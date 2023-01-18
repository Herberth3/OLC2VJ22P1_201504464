import Gramatica.GramaticaLexer;
import Gramatica.GramaticaParser;
import Visitors.Visitor;
import Visitors.VisitorC3D;
import org.antlr.v4.gui.TreeViewer;
import org.antlr.v4.runtime.*;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.*;
import java.util.ArrayList;

import Entorno.*;

import static org.antlr.v4.runtime.CharStreams.fromString;

public class Compilador_GUI extends JFrame {
    private JTextArea textEditor;
    private JTextArea textConsola;
    private JTextArea textC3D;
    private JScrollPane ScrollPane1;
    private JScrollPane ScrollPane2;
    private JScrollPane ScrollPane3;
    private JPanel panel1;
    private JButton btn_abrir;
    private JButton btn_ejecutar;
    private JButton btn_errores;
    private JButton btn_c3d;
    private JButton btn_simbolos;

    NumeracionLinea lineaNumeradaEditor;
    NumeracionLinea lineaNumeradaConsola;
    NumeracionLinea lineaNumeradaC3D;

    private ArrayList<Excepcion> errorListP = new ArrayList<Excepcion>();
    /**Generado para C3D - SEGUNDA PASADA**/
    private VisitorC3D visit3d;
    private boolean showC3D = false, showSimbolos = false;

    public Compilador_GUI(String title) {
        super(title);

        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setContentPane(panel1);
        this.pack();

        lineaNumeradaEditor = new NumeracionLinea(textEditor);
        ScrollPane1.setRowHeaderView(lineaNumeradaEditor);
        lineaNumeradaConsola = new NumeracionLinea(textConsola);
        ScrollPane2.setRowHeaderView(lineaNumeradaConsola);
        lineaNumeradaC3D = new NumeracionLinea(textC3D);
        ScrollPane3.setRowHeaderView(lineaNumeradaC3D);

        // ACCION DEL BOTON PARA ABRIR UN ARCHIVO DE EXTENSION .F90
        btn_abrir.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser chooser = new JFileChooser();
                FileNameExtensionFilter filter = new FileNameExtensionFilter(
                        "F90 Text", "f90");
                chooser.setFileFilter(filter);
                int returnVal = chooser.showOpenDialog(null);
                if(returnVal == JFileChooser.APPROVE_OPTION) {
                    File archivo = chooser.getSelectedFile();
                    try {
                        FileReader fr = new FileReader(archivo);
                        BufferedReader br = new BufferedReader(fr);
                        String cadena_texto = "";
                        String linea_texto = "";
                        while (((linea_texto = br.readLine()) != null)){
                            cadena_texto += linea_texto + "\n";
                        }

                        textEditor.setText(cadena_texto);
                        textConsola.setText("");
                        JOptionPane.showMessageDialog(null, "Archivo abierto exitosamente!");
                    }catch (Exception s){
                        JOptionPane.showMessageDialog(null, "No se pudo abrir el archivo!", "alert", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        });

        // ACCION DEL BOTON EJECUTAR PARA INICIAR EL ANALISIS E INTERPRETACION
        btn_ejecutar.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                if (!textEditor.getText().isEmpty()){
                    String input = textEditor.getText();
                    textConsola.setText("");
                    // Se vacia la lista de errores para generar nuevos en otra ejecucion
                    errorListP.clear();
                    // Se limpia la consola de codigo en 3 direcciones
                    textC3D.setText("");

                    CharStream cs = fromString(input);

                    GramaticaLexer lexico = new GramaticaLexer(cs);
                    // Obtener errores lexicos de la gramatica
                    lexico.removeErrorListeners();
                    lexico.addErrorListener(new BaseErrorListener(){
                        @Override
                        public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e)
                        {
                            errorListP.add(new Excepcion(line, charPositionInLine, msg, Excepcion.TypeError.Lexico));
                        }
                    });

                    CommonTokenStream tokens = new CommonTokenStream(lexico);
                    GramaticaParser sintactico = new GramaticaParser(tokens);
                    // Obtener errores sintacticos de la gramatica
                    sintactico.removeErrorListeners();
                    sintactico.addErrorListener(new BaseErrorListener() {
                        @Override
                        public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e)
                        {
                            // Se remueve en cada descripcion del error Sintactico el char <,> pues da problema en la etiqueta de la tabla en Graphviz
                            String descripcion = msg.replace("<","");
                            descripcion = descripcion.replace(">","");

                            errorListP.add(new Excepcion(line, charPositionInLine, descripcion, Excepcion.TypeError.Sintactico));
                        }
                    });

                    GramaticaParser.StartContext startCtx = sintactico.start();

                    Visitor visitor = new Visitor(new Entorno(null));
                    visitor.visit(startCtx);
                    // Adjuntar los errores semanticos recopilados en la ejecucion del visitor
                    errorListP.addAll(visitor.errorList);

                    System.out.println(visitor.output);
                    textConsola.setText(visitor.output);

                    /**Generado para C3D - SEGUNDA PASADA**/
                    showC3D = true;
                    showSimbolos = true;
                    // Notificacion si existen errores
                    if (errorListP.isEmpty()) {
                        JOptionPane.showMessageDialog(null, "Process completed successfully");

                        List<String> rulesName = Arrays.asList(sintactico.getRuleNames());
                        TreeViewer viewr = new TreeViewer(rulesName, startCtx);
                        viewr.open();
                        /**Generado para C3D - SEGUNDA PASADA**/
                        visit3d = new VisitorC3D(visitor.padre, visitor.pilaEnt);
                        visit3d.visit(startCtx);
                    }
                    else JOptionPane.showMessageDialog(null, "Errores detectados, presione el boton Errores para verlos", "alert", JOptionPane.ERROR_MESSAGE);

                } else {
                    textConsola.setText("");
                    JOptionPane.showMessageDialog(null, "Editor vacio", "alert", JOptionPane.WARNING_MESSAGE);
                }
            }
        });

        // ACCION DEL BOTON ERRORES PARA GENERAR UN ARCHIVO .DOT Y EL REPORTE EN .PDF
        btn_errores.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                if (!errorListP.isEmpty()){
                    // Encabezado de la lista
                    String reportError = "digraph E { node [shape=none] tabla [label=<<TABLE>\n"
                            + "<TR><TD border=\"3\" bgcolor=\"green\">FILA</TD>" +
                            "<TD border=\"3\" bgcolor=\"green\">COLUMNA</TD>" +
                            "<TD border=\"3\" bgcolor=\"green\">DESCRIPCION</TD>" +
                            "<TD border=\"3\" bgcolor=\"green\">TIPO DE ERROR</TD></TR>\n"
                            ;
                    for (Excepcion err : errorListP)
                        reportError += err.toString();
                    reportError += "</TABLE>>]; }";

                    FileWriter file = null;
                    try {
                        file = new FileWriter("errores.dot");
                        file.write(reportError);
                        file.close();

                        Runtime.getRuntime().exec("dot -Tpdf errores.dot -o errores.pdf");
                        JOptionPane.showMessageDialog(null, "Archivo errores.pdf generado exitosamente!");
                    } catch (IOException ex) {
                        JOptionPane.showMessageDialog(null, "No se pudo crear el archivo errores.pdf!", "alert", JOptionPane.ERROR_MESSAGE);
                    }
                }else JOptionPane.showMessageDialog(null, "No hay errores", "attention", JOptionPane.INFORMATION_MESSAGE);
            }
        });
        btn_c3d.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                /**Generado para C3D - SEGUNDA PASADA**/
                if (errorListP.isEmpty()){
                    if (showC3D){
                        //System.out.println(visit3d.c3d.getHeader());
                        //System.out.println(ln);

                        textC3D.setText(visit3d.c3d.getHeader() + "\n");
                        for (String ln : visit3d.c3d.writeC3D)
                            textC3D.append(ln + "\n");

                        showC3D = false;
                    }
                } else JOptionPane.showMessageDialog(null, "Solucione los errores para poder continuar", "attention", JOptionPane.INFORMATION_MESSAGE);

            }
        });
        btn_simbolos.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (showSimbolos){

                    // Encabezado de la lista
                    String reportSimbolos = "digraph E { node [shape=none] tabla [label=<<TABLE>\n"
                            + "<TR><TD border=\"3\" bgcolor=\"yellow\">IDENTIFICADOR</TD>" +
                            "<TD border=\"3\" bgcolor=\"yellow\">TIPO DE DATO</TD>" +
                            "<TD border=\"3\" bgcolor=\"yellow\">ROL</TD>" +
                            "<TD border=\"3\" bgcolor=\"yellow\">DIMENSIONES</TD>" +
                            "<TD border=\"3\" bgcolor=\"yellow\">APUNTADOR</TD></TR>\n"
                            ;
                    for (Entorno ent : visit3d.pilaEnt)
                        for (Simbolo sim : ent.tabla_actual.values())
                            reportSimbolos += sim.toString();

                    reportSimbolos += "</TABLE>>]; }";

                    showSimbolos = false;
                    FileWriter file;
                    try {
                        file = new FileWriter("simbolos.dot");
                        file.write(reportSimbolos);
                        file.close();

                        Runtime.getRuntime().exec("dot -Tpdf simbolos.dot -o simbolos.pdf");
                        JOptionPane.showMessageDialog(null, "Archivo simbolos.pdf generado exitosamente!");
                    } catch (IOException ex) {
                        JOptionPane.showMessageDialog(null, "No se pudo crear el archivo simbolos.pdf!", "alert", JOptionPane.ERROR_MESSAGE);
                    }

                }else JOptionPane.showMessageDialog(null, "Ejecute para generar reporte", "attention", JOptionPane.INFORMATION_MESSAGE);
            }
        });
    }

    public static void main(String[] args){
        JFrame frame = new Compilador_GUI("Compilador");
        frame.setVisible(true);
    }
}
