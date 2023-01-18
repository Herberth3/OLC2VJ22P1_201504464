package Visitors;

import Codigo3D.CodigoTresD;
import ControlCiclos.Cycle;
import ControlCiclos.Exit;
import Entorno.*;
import Entorno.Simbolo.*;
import EntornoType.Subrutina;
import Gramatica.GramaticaBaseVisitor;
import Gramatica.GramaticaParser;

import java.util.Stack;

public class VisitorC3D extends GramaticaBaseVisitor<Object> {

    public Stack<Entorno> pilaEnt;
    public CodigoTresD c3d = new CodigoTresD();
    Entorno padre;

    public VisitorC3D(Entorno ent, Stack<Entorno> pilaEnt) {
        this.pilaEnt = pilaEnt;
        this.padre = ent;
    }

    public Object visitStart(GramaticaParser.StartContext ctx) {
        return visit(ctx.instructions());
    }

    public Object visitSubrutina(GramaticaParser.SubrutinaContext ctx){
        Entorno ent_actual = pilaEnt.peek();
        Simbolo sim = ent_actual.getSimbolo((ctx.idi.getText() + TipoSimbolo.Subrutina.name()).toUpperCase());
        if (sim != null)
        {
            Subrutina subr = (Subrutina) sim.valor;
            c3d.writeC3D.add("void " + subr.nombre.toLowerCase() + "()\n{");
            // El ent_actual de Subrutina, solamente se logra capturar si hay una llamada al metodo
            // Si no hay una llamada al metodo en la primera pasada, solo se imprime un metodo vacio
            if (subr.ent_actual != null){
                // Cada subrutina tiene un atributo llamado ent_actual, en donde fueron almacenados todos los datos de sus sentencias en la primera pasada
                pilaEnt.push(subr.ent_actual);
                // Si existen o no existen parametros, se ejecutan las Sentencias dentro de la Subrutina
                // Solo para mostrarlas en el C3D, y esten listas dentro de un metodo void listo para ser llamado
                for (GramaticaParser.SentencesContext stcs : subr.list_sentences)
                    visitSentences(stcs);

                pilaEnt.pop();
            }
            c3d.writeC3D.add("return;\n}\n");
        }

        return true;
    }

    public Object visitCall_subrutina(GramaticaParser.Call_subrutinaContext ctx){
        Entorno ent_actual = pilaEnt.peek();
        Simbolo simSubrutina = ent_actual.getSimbolo(ctx.IDEN().getText() + TipoSimbolo.Subrutina.name());

        if (simSubrutina != null)
        {
            Subrutina subr = (Subrutina) simSubrutina.valor;
            Entorno entCall = subr.ent_actual;
            if (subr.list_param.size() == ctx.list_parameter().expr().size() && subr.list_param.size() == subr.listDecla_param.getChildCount())
            {
                for (int i = 0; i < ctx.list_parameter().expr().size(); i++)
                {
                    Simbolo expresion = (Simbolo) visit(ctx.list_parameter().expr().get(i));
                    // RECUPERAR LA DIRECCIÓN DE MEMORIA EN DONDE ESTÁ ALMACENADA CADA VARIABLE
                    c3d.writeC3D.add(c3d.generateTemporal() + "=" + entCall.getPreviousSizes() + " + " + subr.list_param.get(i).pos_inStack + ";");
                    c3d.writeC3D.add("P = " + c3d.lastTemporal() + ";");
                    c3d.writeC3D.add("STACK[(int)P] = " + expresion.valor + ";");
                }

                c3d.writeC3D.add(subr.nombre.toLowerCase() + "();");
            }
        }
        return true;
    }

    public Object visitBlock_program(GramaticaParser.Block_programContext ctx){
        Entorno ent_block = pilaEnt.peek().siguiente;
        pilaEnt.push(ent_block);

        c3d.writeC3D.add("int main()\n{\n\n");
        for (GramaticaParser.SentencesContext stcs: ctx.sentences())
            visitSentences(stcs);

        //pilaEnt.pop();
        c3d.writeC3D.add("\nreturn 0;\n}");
        return true;
    }

    public Object visitPrint(GramaticaParser.PrintContext ctx){

        for (GramaticaParser.Print_parameterContext param : ctx.print_parameter())
            visit(param);
        return true;
    }
    public Object visitPrintexp(GramaticaParser.PrintexpContext ctx){
        Entorno ent_actual = pilaEnt.peek();
        Simbolo expresion = (Simbolo) visit(ctx.expr());

        /*
         * CASOS PARA PODER HACER UN PRINT Y UTILIZAR LOS METODO CREADOS GENERICAMENTE
         * 1. IMPRIMIR UNA VARIABLE DE TIPO CHAR Y STRING -> IDCHAR
         * 2. IMPRIMIR UNA VARIABLE DE TIPO NUMÉRICA (ACA TAMBIEN EL BOOLEAN 1,0) -> IDFLOAT
         * 3. IMPRIMIR UN TOKEN DE TIPO NUMÉRICO (ACA TAMBIEN EL BOOLEAN 1,0) -> FLOAT
         * 4. IMPRIMIR UN TOKEN DE TIPO CHAR Y STRING -> CHAR
         * */

        Simbolo sim = null;
        if (expresion.tipo_data.toUpperCase().equals("IDCHAR") || expresion.tipo_data.toUpperCase().equals("IDFLOAT"))
            sim = ent_actual.getSimbolo(expresion.identificador + TipoSimbolo.Variable.name());

        // Si se va a imprimir un tipo IDCHAR o IDFLOAT, se debe generar de nuevo su posicion y la asignacion al puntero P
        // Pues una impresion puede llamar a esa variable mucho despues de que fue declarada
        switch (expresion.tipo_data)
        {
            case "IDCHAR":
                if (sim != null)
                {
                    c3d.writeC3D.add(c3d.generateTemporal() + " = " + ent_actual.getPreviousSizes() + " + " + sim.pos_inStack + ";");
                    c3d.writeC3D.add("P = " + c3d.lastTemporal() + ";");
                    c3d.writeC3D.add("imprimir_variable();");
                }
                break;
            case "IDFLOAT":
                if (sim != null)
                {
                    c3d.writeC3D.add(c3d.generateTemporal() + " = " + ent_actual.getPreviousSizes() + " + " + sim.pos_inStack + ";");
                    c3d.writeC3D.add("P = " + c3d.lastTemporal() + ";");
                    c3d.writeC3D.add("imprimir_var_int();");
                }
                break;
            case "FLOAT":
                c3d.writeC3D.add("printf(\"%f\", " + expresion.valor + ");");
                break;
            case "CHAR":
                c3d.writeC3D.add("P = " + expresion.valor + ";");
                c3d.writeC3D.add("imprimir_string();");
                break;
        }
        return true;
    }
    public Object visitPrintstr(GramaticaParser.PrintstrContext ctx){
        // Cadena String o Char escrito directo en el Print, se captura la cadena y se almacena caracter por caracter en el HEAP
        Simbolo sim3d = new Simbolo(TipoSimbolo.C3D, c3d.generateTemporal(), "CHAR");
        c3d.writeC3D.add(sim3d.valor + " = H;");

        for (char i : String.valueOf(ctx.getText()).toCharArray())
        {
            c3d.writeC3D.add("HEAP[(int)H] = " + (int)i + ";");
            c3d.writeC3D.add("H = H + 1;");
        }
        c3d.writeC3D.add("HEAP[(int)H] = -1;");
        c3d.writeC3D.add("H = H + 1;");
        // Terminado el almacenaje de los caracteres se envia a imprimir otorgando la direccion del HEAP a la P->puntero del STACK
        c3d.writeC3D.add("P = " + sim3d.valor + ";");
        c3d.writeC3D.add("imprimir_string();");

        return true;
    }
    public Object visitDeclaration(GramaticaParser.DeclarationContext ctx){
        Entorno ent_actual = pilaEnt.peek();

        for (GramaticaParser.Decla_asignContext decla : ctx.decla_asign()){

            if (ent_actual.tabla_actual.containsKey((decla.IDEN().getText() + TipoSimbolo.Variable.name()).toUpperCase())){
                Simbolo sim = ent_actual.tabla_actual.get((decla.IDEN().getText() + TipoSimbolo.Variable.name()).toUpperCase());
                String tipo_dato = visitType(ctx.type()).toString().toUpperCase();
                // valorLocal es el Simbolo que contendra el valor a guardar en STACK o HEAP
                Simbolo valorLocal = null;

                // Por cada declaracion se verifica que este o no asignada una expresion
                if (decla.expr() != null) {
                    valorLocal = (Simbolo) visit(decla.expr());

                } else {
                    if (tipo_dato.equals("INTEGER")) {
                        Simbolo sim3d = new Simbolo(TipoSimbolo.C3D, c3d.generateTemporal(), "FLOAT");
                        c3d.writeC3D.add("/* Declaracion de un int */");
                        c3d.writeC3D.add(sim3d.valor + " = " + 0 + ";");
                        valorLocal = sim3d;
                    } else if (tipo_dato.equals("REAL")) {
                        Simbolo sim3d = new Simbolo(TipoSimbolo.C3D, c3d.generateTemporal(), "FLOAT");
                        c3d.writeC3D.add("/* Declaracion de un real */");
                        c3d.writeC3D.add(sim3d.valor + " = " + 0.00000000 + ";");
                        valorLocal = sim3d;
                    } else if (tipo_dato.equals("CHARACTER")) {
                        // Para los datos tipo cadena, en el temporal se almacena la direccion del HEAP
                        Simbolo sim3d = new Simbolo(TipoSimbolo.C3D, c3d.generateTemporal(), "CHAR");
                        c3d.writeC3D.add("/* Declaracion de un character */");
                        c3d.writeC3D.add(sim3d.valor + " = H;");

                        for (char i : String.valueOf("''").toCharArray())
                        {
                            c3d.writeC3D.add("HEAP[(int)H] = " + (int)i + ";");
                            c3d.writeC3D.add("H = H + 1;");
                        }
                        c3d.writeC3D.add("HEAP[(int)H] = -1;");
                        c3d.writeC3D.add("H = H + 1;");
                        valorLocal = sim3d;
                    } else if (tipo_dato.equals("COMPLEX")) {
                        // Para los datos tipo cadena, en el temporal se almacena la direccion del HEAP
                        Simbolo sim3d = new Simbolo(TipoSimbolo.C3D, c3d.generateTemporal(), "CHAR");
                        c3d.writeC3D.add("/* Declaracion de un complex */");
                        c3d.writeC3D.add(sim3d.valor + " = H;");

                        for (char i : String.valueOf("(9.19251792643,0.00000000)").toCharArray())
                        {
                            c3d.writeC3D.add("HEAP[(int)H] = " + (int)i + ";");
                            c3d.writeC3D.add("H = H + 1;");
                        }
                        c3d.writeC3D.add("HEAP[(int)H] = -1;");
                        c3d.writeC3D.add("H = H + 1;");
                        valorLocal = sim3d;
                    } else if (tipo_dato.equals("LOGICAL")) {
                        Simbolo sim3d = new Simbolo(TipoSimbolo.C3D, c3d.generateTemporal(), "FLOAT");
                        c3d.writeC3D.add("/* Declaracion de una logical */");
                        c3d.writeC3D.add(sim3d.valor + " = " + 0 + ";");
                        valorLocal = sim3d;
                    }

                }
                /*
                * Generar c3d de las expresiones previo (int :: var1 = 58*7/3+9)
                * t1 = 58 * 7;
                * t2 = t1 / 3;
                * t3 = t2 + 9;
                *
                * t4 = 12 + 2; (t1 = sizePadres + pos_inStack)
                * P = t4; (P : puntero del Stack en C, ahi se almacena el valor)
                * STACK[P] = t3; (int:5, boleano:0,1, char:32)
                * */
                c3d.writeC3D.add(c3d.generateTemporal() + " = " + ent_actual.getPreviousSizes() + " + " + sim.pos_inStack + ";");
                c3d.writeC3D.add("P = " + c3d.lastTemporal() + ";");
                c3d.writeC3D.add("STACK[(int)P] = " + ((Simbolo) valorLocal).valor + ";");
            }
        }

        return true;
    }

    public String visitType(GramaticaParser.TypeContext ctx) {
        return ctx.getText();
    }
    public Object visitOpexpr(GramaticaParser.OpexprContext ctx){
        Simbolo izq = (Simbolo)visit(ctx.left);
        Simbolo der = (Simbolo)visit(ctx.right);
        String operacion = ctx.op.getText();

        Simbolo sim3d = new Simbolo(TipoSimbolo.C3D, c3d.generateTemporal(), "FLOAT");
        c3d.writeC3D.add(sim3d.valor + " = " + izq.valor + operacion + der.valor + ";");
        return  sim3d;

    }
    public Object visitOpexprrel(GramaticaParser.OpexprrelContext ctx){
        Simbolo izq = (Simbolo)visit(ctx.left);
        Simbolo der = (Simbolo)visit(ctx.right);
        String operacion = ctx.op.getText();
        String operador = "==";

        if (operacion.equals("==") || operacion.equals(".eq.")) {
            operador = "==";
        } else if (operacion.equals("/=") || operacion.equals(".ne.")) {
            operador = "!=";
        } else if (operacion.equals(">") || operacion.equals(".gt.")) {
            operador = ">";
        } else if (operacion.equals("<") || operacion.equals(".lt.")) {
            operador = "<";
        } else if (operacion.equals(">=") || operacion.equals(".ge.")) {
            operador = ">=";
        } else if (operacion.equals("<=") || operacion.equals(".le.")) {
            operador = "<=";
        }

        Simbolo sim3d = new Simbolo(TipoSimbolo.C3D, c3d.generateTemporal(), "FLOAT");
        c3d.writeC3D.add(sim3d.valor + " = " + izq.valor + operador + der.valor + ";");
        return  sim3d;
    }

    public Object visitOpexprlog(GramaticaParser.OpexprlogContext ctx){
        Simbolo izq = (Simbolo)visit(ctx.left);
        Simbolo der = (Simbolo)visit(ctx.right);
        String operacion = ctx.op.getText();

        if (operacion.equalsIgnoreCase(".and."))
            return c3d.getAnd(izq.valor.toString(), der.valor.toString());
        else if (operacion.equalsIgnoreCase(".or.")) {
            return c3d.getOr(izq.valor.toString(), der.valor.toString());
        }
        return true;
    }
    public Simbolo visitParenexpr(GramaticaParser.ParenexprContext ctx) {
        return (Simbolo) visit(ctx.expr());
    }
    public Simbolo visitAtomexpr(GramaticaParser.AtomexprContext ctx){
        Simbolo sim3d = new Simbolo(TipoSimbolo.C3D, c3d.generateTemporal(), "FLOAT");
        c3d.writeC3D.add(sim3d.valor + " = " + ctx.getText() + ";");
        return sim3d;
    }
    public Simbolo visitCharexpr(GramaticaParser.CharexprContext ctx){

        Simbolo sim3d = new Simbolo(TipoSimbolo.C3D, c3d.generateTemporal(), "CHAR");
        c3d.writeC3D.add(sim3d.valor + " = H;");

        for (char i : String.valueOf(ctx.getText()).toCharArray())
        {
            c3d.writeC3D.add("HEAP[(int)H] = " + (int)i + ";");
            c3d.writeC3D.add("H = H + 1;");
        }
        c3d.writeC3D.add("HEAP[(int)H] = -1;");
        c3d.writeC3D.add("H = H + 1;");
        return sim3d;
    }
    public Simbolo visitIdexpr(GramaticaParser.IdexprContext ctx){
        Entorno ent_actual = pilaEnt.peek();
        Simbolo auxSimbolo = ent_actual.getSimbolo(ctx.IDEN().getText() + TipoSimbolo.Variable.name());

        Simbolo sim3d = new Simbolo(TipoSimbolo.C3D, c3d.generateTemporal(), auxSimbolo.tipo_data.toUpperCase().equals("CHARACTER") ? "IDCHAR" : "IDFLOAT");

        // Se hace el manejo de los tipo complex, solo si se hace una declaracion y piden su valor, no hay operaciones con es tipo de dato
        // Complex, para este proyecto se maneja como una cadena de String
        if (auxSimbolo.tipo_data.equalsIgnoreCase("COMPLEX"))
            sim3d.tipo_data = "IDCHAR";
        sim3d.identificador = ctx.IDEN().getText();

        /*
        * t0 = 12 + 4;
        * P = t0;
        * t1 = STACK[(int)P];
        * */
        // En el caso de ID's hay que ir a buscar su valor a memoria STACK o HEAP
        c3d.writeC3D.add(c3d.generateTemporal() + " = " + ent_actual.getPreviousSizes() + " + " + auxSimbolo.pos_inStack + ";");
        c3d.writeC3D.add("P = " + c3d.lastTemporal() + ";");
        c3d.writeC3D.add(sim3d.valor + "= STACK[(int)P];");
        return sim3d;
    }

    public Object visitCmplxexpr(GramaticaParser.CmplxexprContext ctx){
        Simbolo sim3d = new Simbolo(TipoSimbolo.C3D, c3d.generateTemporal(), "CHAR");
        c3d.writeC3D.add(sim3d.valor + " = H;");

        for (char i : String.valueOf(ctx.getText()).toCharArray())
        {
            c3d.writeC3D.add("HEAP[(int)H] = " + (int)i + ";");
            c3d.writeC3D.add("H = H + 1;");
        }
        c3d.writeC3D.add("HEAP[(int)H] = -1;");
        c3d.writeC3D.add("H = H + 1;");
        return sim3d;
    }

    public Simbolo visitBoleanexpr(GramaticaParser.BoleanexprContext ctx){
        Simbolo sim3d = new Simbolo(TipoSimbolo.C3D, c3d.generateTemporal(), "FLOAT");
        String valor = ctx.getText();
        boolean bool = valor.equals(".true.");
        c3d.writeC3D.add("/* Declaracion de una logical */");
        c3d.writeC3D.add(sim3d.valor + " = " + (bool ? "1" : "0") + ";");
        return sim3d;
    }
}
