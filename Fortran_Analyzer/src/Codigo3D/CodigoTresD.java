package Codigo3D;

import Entorno.Simbolo;
import Entorno.Simbolo.*;

import java.util.ArrayList;

public class CodigoTresD {
    public ArrayList<String> writeC3D;
    private int temporal;
    private int label;

    public CodigoTresD() {
        this.writeC3D = new ArrayList<>();
        this.temporal = -1;
        this.label = -1;
    }

    public String generateTemporal(){
        this.temporal++;
        return String.valueOf("t" + this.temporal);
    }

    public String lastTemporal(){
        return String.valueOf("t" + this.temporal);
    }

    public String generateLabel()
    {
        this.label++;
        return  "L" + this.label;
    }

    public Simbolo getAnd(String izq, String der)
    {
        this.writeC3D.add("if (" + izq + " ) goto " + this.generateLabel() + ";\n" +
                "goto " + this.generateLabel() + ";\n" +
                "L" + (this.label - 1) + ": if (" + der + ") goto L" + (this.label + 1) + ";\n" +
                "goto L" + (this.label + 2) + ";\n" +
                this.generateLabel() + ": " + this.generateTemporal() + " = 1;\n" +
                "goto L" + (this.label + 2) + ";\n" +
                "L" + (this.label - 1) + ": " + this.generateLabel() + ": t" + this.temporal + "= 0;\n" +
                this.generateLabel() + ":\n");
        return  new Simbolo(TipoSimbolo.C3D, this.lastTemporal(), "FLOAT");
    }
    public Simbolo getOr(String izq, String der)
    {
        this.writeC3D.add("if (" + izq + " ) goto " + this.generateLabel() + ";\n" +
                "goto " + this.generateLabel() + ";\n" +
                "L" + (this.label) + ": if (" + der + ") goto L" + (this.label - 1) + ";\n" +
                "goto L" + (this.label + 1) + ";\n" +
                "L" + (this.label - 1) + ": " + this.generateTemporal() + " = 1;\n" +
                "goto L" + (this.label + 2) + ";\n" +
                this.generateLabel() + ": " + "t" + this.temporal + "= 0;\n" +
                this.generateLabel() + ":\n");
        return  new Simbolo(TipoSimbolo.C3D, this.lastTemporal(), "FLOAT");
    }

    /**
     * getPrintVars() : retorna una cadena que simula al metodo en C: imprimir_variable()
     * imprimir_variable() : genera un ciclo con etiquetas y saltos para obtener cada caracter que se almaceno en el HEAP y lo imprime
     * los datos guardados en el HEAP fueron de tipo STRING o CHAR
     * */
    private String getPrintVars(){
        String tempStart = this.generateTemporal();
        String labelStart = this.generateLabel();

        return "void imprimir_variable()\n" +
                "{\n" +
                tempStart + " = STACK[(int)P];\n" +
                labelStart + ":\n" +
                this.generateTemporal() + " = HEAP[(int)" + tempStart + "];\n" +
                "if (" + this.lastTemporal() + " != -1) goto L" + (this.label + 1) + ";\n" +
                "goto L" + (this.label + 2) + ";\n" +
                this.generateLabel() + ":\n" +
                "printf(\"%c\", (char)" + this.lastTemporal() + ");\n" +
                tempStart + "=" + tempStart + " + 1;\n" +
                "goto " + labelStart + ";\n" +
                this.generateLabel() + ":\n" +
                "printf(\"%c\\n\", (char)32);\n" +
                "return;\n" +
                "}\n\n";
    }

    private String getPrintStr()
    {
        String tempStart = this.generateTemporal();
        String labelStart = this.generateLabel();
        return "void imprimir_string()\n" +
                "{\n" +
                tempStart + " = P;\n" +
                labelStart + ":\n" +
                this.generateTemporal() + " = HEAP[(int)" + tempStart + "];\n" +
                "if (" + this.lastTemporal() + " != -1) goto L" + (this.label + 1) + ";\n" +
                "goto L" + (this.label + 2) + ";\n" +
                this.generateLabel() + ":\n" +
                "printf(\"%c\", (char)" + this.lastTemporal() + ");\n" +
                tempStart + "=" + tempStart + " + 1;\n" +
                "goto " + labelStart + ";\n" +
                this.generateLabel() + ":\n" +
                "printf(\"%c\\n\", (char)32);\n" +
                "return;\n" +
                "}\n\n";
    }

    private String getPrintVarInt()
    {
        return "void imprimir_var_int()\n{\n" +
                this.generateTemporal() + " = STACK[(int)P];\n" +
                "printf(\"%f\\n\", " + this.lastTemporal() + ");\n" +
                "return;\n}\n\n";
    }

    /**
     * Generar encabezado del lenguaje C
     * El for concatena el listado de temporales: t1, t2, t3, .... ,tn;
     * Generado para C3D**/
    public String getHeader(){
        String prints = this.getPrintVars() + this.getPrintVarInt() + this.getPrintStr();
        String temporales = "";
        for (int i = 0; i <= this.temporal; i++)
            temporales += "t" + String.valueOf(i) + (i < this.temporal ? "," : ";\n");

        return "#include <stdio.h>\n" +
                "double STACK[30101999];\n" +
                "double HEAP[30101999];\n" +
                "double P;\n" +
                "double H;\n" +
                "double " + temporales +
                "\n" + prints;
    }
}
