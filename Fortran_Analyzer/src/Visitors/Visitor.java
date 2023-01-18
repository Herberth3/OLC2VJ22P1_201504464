package Visitors;

import ControlCiclos.Cycle;
import ControlCiclos.Exit;
import Entorno.*;
import Entorno.Simbolo.*;
import EntornoType.Funcion;
import EntornoType.Subrutina;
import Gramatica.*;

import java.util.*;

public class Visitor extends GramaticaBaseVisitor<Object> {

    public String output = "";
    public ArrayList<Excepcion> errorList = new ArrayList<Excepcion>();
    public Stack<Entorno> pilaEnt = new Stack<Entorno>();

    /**Generado para C3D**/
    public Entorno padre;

    /**
     * El contructor de Visitor recibe como parametro un nuevo entorno
     * El primer entorno dentro de la pila tiene como padre a null
     */
    public Visitor(Entorno ent) {
        this.pilaEnt.push(ent);
        /**Generado para C3D**/
        this.padre = ent;

        /*
        * FUNCIONES NATIVAS - SIZE
        * Se ingreso la funcion al inicializar el constructor para que el Simbolo ya exista en el Entorno global
        * */
        Entorno ent_actual = pilaEnt.peek();

        // Se crea un objeto tipo Funcion que contendra la informacion para ejecutar al momento de la llamada
        Funcion size = new Funcion("size", "tamano", null, null, null);
        // Se crea un nuevo Simbolo tipo Funcion para almacenarla en el entorno actual para luego pueda ser llamada
        ent_actual.addSimbolo("size" + TipoSimbolo.Funcion.name(), new Simbolo("size", "FUNCION", size, TipoSimbolo.Funcion, ent_actual.lastPosicion));
        /**Generado para C3D**/
        ent_actual.lastPosicion++;
    }

    /**
     * functionSize() : funcion nativa que genera el tamaño del arreglo que se le pasa por parametro
     * */
    private Object functionSize(GramaticaParser.List_parameterContext ctx, int fila, int columna){

        if (ctx == null) return new Excepcion(fila, columna, "Se esperaba un arreglo como parametro", Excepcion.TypeError.Semantico);
        if (ctx.expr().size() > 1) return new Excepcion(fila, columna, "Se esperaba solo un parametro", Excepcion.TypeError.Semantico);

        Object array = visit(ctx.expr(0));

        if (array instanceof Excepcion) return array;
        if (!(array instanceof ArrayList<?>)) return new Excepcion(fila, columna, "Se esperaba un arreglo como parametro", Excepcion.TypeError.Semantico);

        Entorno ent_actual = pilaEnt.peek();
        String arrayName = ctx.expr(0).getText();
        Simbolo arraySimbolo = ent_actual.getSimbolo(arrayName + TipoSimbolo.Variable.name());

        if (arraySimbolo == null) return new Excepcion(fila, columna, "El arreglo no existe", Excepcion.TypeError.Semantico);
        if (arraySimbolo.tipo_entorno != TipoSimbolo.Arreglo) return new Excepcion(fila, columna, "Se esperaba un arreglo como parametro", Excepcion.TypeError.Semantico);

        int tamano = 1;
        for (int dimension : arraySimbolo.dimensiones)
            tamano = tamano * dimension;

        return tamano;
    }

    /************************** I N S T R U C C I O N E S ********************************/
    public Object visitStart(GramaticaParser.StartContext ctx) {
        return visit(ctx.instructions());
    }

    /************************** S U B R U T I N A *************************************/
    public Object visitSubrutina(GramaticaParser.SubrutinaContext ctx){
        // Se verifica que los identificadores sean iguales
        if (ctx.idi.getText().equals(ctx.ide.getText())){
            Entorno ent_actual = pilaEnt.peek();

            if (!ent_actual.tabla_actual.containsKey((ctx.idi.getText() + TipoSimbolo.Subrutina.name()).toUpperCase())){
                // Se crea un array list para almacenar los parametros
                ArrayList<Simbolo> parametros = new ArrayList<Simbolo>();

                // Se verifica que vengan parametros
                if (ctx.list_parameter() != null){
                    /**Generado para C3D, posicion simulada en la tabla simbolos, porque aun no se ingresa el simbolo al Entorno**/
                    int posicion = -1;
                    // Se recorre cada parametro y se almacena como un nuevo Simbolo en el array parametros
                    for (GramaticaParser.ExprContext param : ctx.list_parameter().expr())
                        parametros.add(new Simbolo(param.getText(), "", null, TipoSimbolo.Parametros, posicion++));
                }

                // Se crea un objeto tipo Subrutina que contendra la informacion para ejecutar al momento del call
                Subrutina auxSubrutina = new Subrutina(ctx.idi.getText(), parametros, ctx.sentences(), ctx.list_decla_param());
                // Se crea un nuevo Simbolo tipo Subrutina para almacenarla en el entorno actual para luego pueda ser llamada
                ent_actual.addSimbolo(ctx.idi.getText() + TipoSimbolo.Subrutina.name(), new Simbolo(ctx.idi.getText(), "SUBRUTINA", auxSubrutina, TipoSimbolo.Subrutina, ent_actual.lastPosicion));
                /**Generado para C3D**/
                ent_actual.lastPosicion++;

                return true;

            }
            // Si el ID de la Subrutina ya existe se crea una Excepcion y no se guarda la subrutina
            errorList.add(new Excepcion(ctx.idi.getLine(), ctx.idi.getCharPositionInLine(), "La Subrutina " + ctx.idi.getText() + " ya existe", Excepcion.TypeError.Semantico));

            return true;
        }
        // Si el identificador inicial no coincide con el final se crea una Excepcion y no se guarda la subrutina
        errorList.add(new Excepcion(ctx.idi.getLine(), ctx.idi.getCharPositionInLine(), "Identificadores en Subrutina no coinciden", Excepcion.TypeError.Semantico));

        return true;
    }

    /**
     * Se implementa el control de ciclos Exit y Cycle
     * */
    public Object visitCall_subrutina(GramaticaParser.Call_subrutinaContext ctx){
        Entorno ent_actual = pilaEnt.peek();

        Simbolo simSubrutina = ent_actual.getSimbolo(ctx.IDEN().getText() + TipoSimbolo.Subrutina.name());

        // Se verifica que la subrutina exista
        if (simSubrutina == null)
            return new Excepcion(ctx.IDEN().getSymbol().getLine(), ctx.IDEN().getSymbol().getCharPositionInLine(), "La Subrutina " + ctx.IDEN().getText() + " no existe", Excepcion.TypeError.Semantico);

        // Se crea un nuevo Entorno para la Subrutina
        Entorno ent_subr = new Entorno(ent_actual);
        // Se extrae la instancia de la Subrutina almacenada cuando se declaro
        Subrutina auxSubrutina = (Subrutina) simSubrutina.valor;

        // Se verifica que vengan parametros
        if (ctx.list_parameter() != null){

            if (auxSubrutina.list_param.size() == ctx.list_parameter().expr().size() && auxSubrutina.list_param.size() == auxSubrutina.listDecla_param.getChildCount()){
                /*
                * Si tanto la lista de parametros en la declaracion, como en la llamada, son del mismo tamaño
                * Se recorren los parametros para asignarse a un nuevo Simbolo
                */
                for (int i = 0; i < ctx.list_parameter().expr().size(); i++) {
                    /*
                    * Se supone que tanto los parametros como la declaracion de estos en la Subrutina vengan en orden
                    * Ejemplo Parametros: SubrutinaName(var1, var2, var3)
                    * Ejemplo Declaraciones: Integer intent in ::var1 - Integer intent in ::var2 - Character intent in :: var3
                    * Pero igual se verifica que sean llamados igual
                    * */
                    if (auxSubrutina.list_param.get(i).identificador.equalsIgnoreCase(auxSubrutina.listDecla_param.decla_parameter(i).IDEN().getText())){

                        Object exp = visit(ctx.list_parameter().expr().get(i));
                        /*
                         * Validar que la expresion no sea una Excepcion, si lo es, salta a la siguiente iteracion
                         * Y la Excepcion se almacena en la lista de Excepciones, no se retorna, pues quiza falten parametros en cola
                         * Si es una Excepcion el Simbolo de la expresion se ignora y no se almacena en el entorno actual
                         * */
                        if (exp instanceof Excepcion) {
                            errorList.add((Excepcion) exp);
                            continue;
                        }

                        // Tipo de dato declarado en la subrutina
                        String tipo_datoDecla = auxSubrutina.listDecla_param.decla_parameter(i).type().getText().toUpperCase();
                        // Verificar si el Tipo de dato del parametro de la llamada coincide con el de la declaracion en la Subrutina

                        if (exp instanceof Integer){
                            if (!tipo_datoDecla.equals("INTEGER")){
                                errorList.add(new Excepcion(ctx.IDEN().getSymbol().getLine(), ctx.IDEN().getSymbol().getCharPositionInLine(), "Parametro " + exp + " no valido en peticion de Integer", Excepcion.TypeError.Semantico));
                                continue;
                            }
                        } else if (exp instanceof Double) {
                            if (!tipo_datoDecla.equals("REAL")){
                                errorList.add(new Excepcion(ctx.IDEN().getSymbol().getLine(), ctx.IDEN().getSymbol().getCharPositionInLine(), "Parametro " + exp + " no valido en peticion de Real", Excepcion.TypeError.Semantico));
                                continue;
                            }
                        } else if (exp instanceof Character) {
                            if (!tipo_datoDecla.equals("CHARACTER")){
                                errorList.add(new Excepcion(ctx.IDEN().getSymbol().getLine(), ctx.IDEN().getSymbol().getCharPositionInLine(), "Parametro " + exp + " no valido en peticion de Character", Excepcion.TypeError.Semantico));
                                continue;
                            }
                        } else if (exp instanceof Boolean) {
                            if (!tipo_datoDecla.equals("LOGICAL")){
                                errorList.add(new Excepcion(ctx.IDEN().getSymbol().getLine(), ctx.IDEN().getSymbol().getCharPositionInLine(), "Parametro " + exp + " no valido en peticion de Logical", Excepcion.TypeError.Semantico));
                                continue;
                            }
                        } else if (exp instanceof ArrayList<?>){
                            /*
                             * Si la expresion es igual a un arreglo, quiere decir que es una asignacion directa entre arreglos
                             * Se hacen las validaciones necesarias como: saber si IDEN(Parametro) y IDEN(Declaracion) son arreglos ambos
                             * Si los dos tienen las mismas dimensiones y si son del mismo tipo
                             * */

                            // Se verifica que la declaracion del parametro sea para un arreglo
                            if (auxSubrutina.listDecla_param.decla_parameter(i).list_parameter() == null){
                                errorList.add(new Excepcion(ctx.IDEN().getSymbol().getLine(), ctx.IDEN().getSymbol().getCharPositionInLine(), "La declaracion para " + auxSubrutina.listDecla_param.decla_parameter(i).IDEN().getText() + " no es un arreglo", Excepcion.TypeError.Semantico));
                                continue;
                            }
                            // Si la expresion produjo un arreglo, quiere decir que en EXPR hayo un identificador y de este se busco el simbolo
                            Simbolo auxSimbolo = ent_actual.getSimbolo(ctx.list_parameter().expr().get(i).getText() + TipoSimbolo.Variable.name());
                            if (auxSimbolo.tipo_entorno != TipoSimbolo.Arreglo){
                                errorList.add(new Excepcion(ctx.IDEN().getSymbol().getLine(), ctx.IDEN().getSymbol().getCharPositionInLine(), "La variable " + ctx.IDEN().getText() + " no es un arreglo", Excepcion.TypeError.Semantico));
                                continue;
                            }
                            // Comparar dimensiones
                            Object list_parameter = this.getDimInteger(auxSubrutina.listDecla_param.decla_parameter(i).list_parameter(), ctx.IDEN().getSymbol().getLine(), ctx.IDEN().getSymbol().getCharPositionInLine());

                            if (list_parameter instanceof Excepcion) {
                                errorList.add((Excepcion) list_parameter);
                                continue;
                            }
                            if (!auxSimbolo.dimensiones.equals(list_parameter)){
                                errorList.add(new Excepcion(ctx.IDEN().getSymbol().getLine(), ctx.IDEN().getSymbol().getCharPositionInLine(), "Los arreglo " + auxSubrutina.listDecla_param.decla_parameter(i).IDEN().getText() + " y " + auxSimbolo.identificador + " no tienen las mismas dimensiones", Excepcion.TypeError.Semantico));
                                continue;
                            }
                            // Comparar tipo de datos
                            if (!(auxSimbolo.tipo_data.equalsIgnoreCase(tipo_datoDecla))){
                                errorList.add(new Excepcion(ctx.IDEN().getSymbol().getLine(), ctx.IDEN().getSymbol().getCharPositionInLine(), "Los arreglo " + auxSubrutina.listDecla_param.decla_parameter(i).IDEN().getText() + " y " + auxSimbolo.identificador + " no son del mismo tipo de datos", Excepcion.TypeError.Semantico));
                                continue;
                            }

                            // Se hace una copia del arreglo para pasarlo por valor y no por referencia
                            ArrayList<Object> array = (ArrayList<Object>) exp;
                            ArrayList<Object> arrayCopy = new ArrayList<>();
                            // Validacion solo para arreglos de 2 dimension
                            if (auxSubrutina.listDecla_param.decla_parameter(i).list_parameter().expr().size() > 1){

                                for (Object arr : array){
                                    ArrayList<Object>  dim1 = (ArrayList<Object>) arr;
                                    ArrayList<Object> copy = (ArrayList<Object>) dim1.clone();
                                    arrayCopy.add(copy);
                                }
                            } else {
                                // Si el arreglo es solo de 1 dimension, solo se copia
                                arrayCopy = (ArrayList<Object>) array.clone();
                            }

                            // Se crea un nuevo Simbolo con los datos del parametro arreglo a declarar y se guarda en el entorno de la funcion
                            Simbolo paramArreglo = new Simbolo(auxSubrutina.list_param.get(i).identificador, tipo_datoDecla, arrayCopy, (ArrayList<Integer>) list_parameter, TipoSimbolo.Arreglo);

                            // Se almacena en el entorno de la Subrutina el Simbolo del nuevo parametro ya como una variante
                            ent_subr.addSimbolo(auxSubrutina.list_param.get(i).identificador + TipoSimbolo.Variable.name(), paramArreglo);

                            continue;
                        }

                        // Se crea un nuevo Simbolo con los datos del parametro a declarar y se guarda en el entorno de la subrutina
                        Simbolo nuevo = new Simbolo(auxSubrutina.list_param.get(i).identificador, tipo_datoDecla, exp, TipoSimbolo.Parametros, auxSubrutina.list_param.get(i).pos_inStack);

                        // Se almacena en el entorno de la Subrutina el Simbolo del nuevo parametro ya como una variante
                        ent_subr.addSimbolo(auxSubrutina.list_param.get(i).identificador + TipoSimbolo.Variable.name(), nuevo);

                    } else {
                        errorList.add(new Excepcion(ctx.IDEN().getSymbol().getLine(), ctx.IDEN().getSymbol().getCharPositionInLine(), "Parametro " + auxSubrutina.list_param.get(i).identificador + " no declarado en Subrutina", Excepcion.TypeError.Semantico));
                    }

                }
                /**Generado para C3D Se actualiza lastPosicion, pues se traian parametros que aun no se habian ingresado al entorno**/
                ent_subr.lastPosicion = auxSubrutina.list_param.size();
            } else return new Excepcion(ctx.IDEN().getSymbol().getLine(), ctx.IDEN().getSymbol().getCharPositionInLine(), "Cantidad de parametros en Subrutina " + ctx.IDEN().getText() + " no coinciden", Excepcion.TypeError.Semantico);

        } else {
            // Se verifica que los parametros de la Subrutina sean igual a 0, pues en la llamada no hay parametros
            if (auxSubrutina.list_param.size() != 0 || auxSubrutina.listDecla_param.getChildCount() != 0){
                return new Excepcion(ctx.IDEN().getSymbol().getLine(), ctx.IDEN().getSymbol().getCharPositionInLine(), "Cantidad de parametros en Subrutina " + ctx.IDEN().getText() + " no coinciden", Excepcion.TypeError.Semantico);
            }
        }
        // Se ingresa el Entorno de la Subrutina a la pila para que se pueda usar mientras valida parametros
        pilaEnt.push(ent_subr);
        // Si existen o no existen parametros, se ejecutan las Sentencias dentro de la Subrutina
        for (GramaticaParser.SentencesContext stcs : auxSubrutina.list_sentences){

            // Aca se terminan de almacenar las Excepciones de sentencias
            Object sentence = visitSentences(stcs);
            if (sentence instanceof Excepcion) errorList.add((Excepcion) sentence);
            // Implementacion del control de ciclos EXIT, se guarda el error y se siguen ejecutando las sentencias
            if (sentence instanceof Exit) errorList.add(new Excepcion(((Exit) sentence).fila, ((Exit) sentence).columna, "Implementar Control Exit solo en ciclos", Excepcion.TypeError.Semantico));
            // Implementacion del control de ciclos CYCLE, se guarda el error y se siguen ejecutando las sentencias
            if (sentence instanceof Cycle) errorList.add(new Excepcion(((Cycle) sentence).fila, ((Cycle) sentence).columna, "Implementar Control Cycle solo en ciclos", Excepcion.TypeError.Semantico));
        }
        /**Generado para C3D Se captura el entorno de la subrutina en donde fueron guardados los datos de sus sentencias**/
        auxSubrutina.ent_actual = pilaEnt.peek();
        // Cuando se haya ejecutado la Subrutina se elimina su entorno que esta en la pila, para que no quede almacenado.
        pilaEnt.pop();

        return null;
    }

    /************************** F U N C I O N *************************************/
    public Object visitFuncion(GramaticaParser.FuncionContext ctx){
        // Se verifica que los identificadores sean iguales
        if (ctx.idi.getText().equals(ctx.ide.getText())){
            Entorno ent_actual = pilaEnt.peek();

            if (!ent_actual.tabla_actual.containsKey((ctx.idi.getText() + TipoSimbolo.Funcion.name()).toUpperCase())){

                if (ctx.sentences().isEmpty()){
                    // Debe haber al menos 1 declaracion para la variable a retornar
                    errorList.add(new Excepcion(ctx.idi.getLine(), ctx.idi.getCharPositionInLine(), "La Funcion " + ctx.idi.getText() + " no tiene un valor de retorno declarado", Excepcion.TypeError.Sintactico));
                    return true;
                }
                boolean isDeclaRetorno = false;
                // Se recorren las sentencias de la funcion para saber si la variable de retorno esta declarada, de lo contrario error
                for (int i = 0; i < ctx.sentences().size(); i++) {
                    if (ctx.sentences().get(i).declaration() != null && !isDeclaRetorno){
                        for (int j = 0; j < ctx.sentences().get(i).declaration().decla_asign().size(); j++) {
                            if (ctx.sentences().get(i).declaration().decla_asign(j).IDEN().getText().equalsIgnoreCase(ctx.idr.getText())){
                                isDeclaRetorno = true;
                                break;
                            }
                        }
                    } else break;
                }

                // Se corrobora si encontro la declaracion
                if (!isDeclaRetorno){
                    errorList.add(new Excepcion(ctx.idr.getLine(), ctx.idr.getCharPositionInLine(), "La Funcion " + ctx.idi.getText() + " no tiene un valor de retorno declarado", Excepcion.TypeError.Sintactico));
                    return true;
                }

                // Se crea un array list para almacenar los parametros
                ArrayList<Simbolo> parametros = new ArrayList<Simbolo>();

                // Se verifica que vengan parametros
                if (ctx.list_parameter() != null){
                    try {
                        /**Generado para C3D, posicion simulada en la tabla simbolos, porque aun no se ingresa el simbolo al Entorno**/
                        int posicion = -1;
                        // Se recorre cada parametro y se almacena como un nuevo Simbolo en el array parametros
                        for (GramaticaParser.ExprContext param : ctx.list_parameter().expr()){
                            // Se verifica que la variable de retorno no se use tambien como parametro
                            if (param.getText().equalsIgnoreCase(ctx.idr.getText())){
                                errorList.add(new Excepcion(ctx.idr.getLine(), ctx.idr.getCharPositionInLine(), "La variable de retorno " + ctx.idr.getText() + " no puede usarse como parametro", Excepcion.TypeError.Semantico));
                                return true;
                            }
                            parametros.add(new Simbolo(param.getText(), "", null, TipoSimbolo.Parametros, posicion++));
                        }
                    }catch (Exception e){
                        errorList.add(new Excepcion(ctx.idi.getLine(), ctx.idi.getCharPositionInLine(), "Los parametros solo pueden ser identificadores", Excepcion.TypeError.Semantico));
                        return true;
                    }
                }

                // Se crea un objeto tipo Funcion que contendra la informacion para ejecutar al momento de la llamada
                Funcion auxFuncion = new Funcion(ctx.idi.getText(), ctx.idr.getText(), parametros, ctx.sentences(), ctx.list_decla_param());
                // Se crea un nuevo Simbolo tipo Funcion para almacenarla en el entorno actual para luego pueda ser llamada
                ent_actual.addSimbolo(ctx.idi.getText() + TipoSimbolo.Funcion.name(), new Simbolo(ctx.idi.getText(), "FUNCION", auxFuncion, TipoSimbolo.Funcion, ent_actual.lastPosicion));
                /**Generado para C3D**/
                ent_actual.lastPosicion++;

                return true;

            }
            // Si el ID de la Funcion ya existe se crea una Excepcion y no se guarda la Funcion
            errorList.add(new Excepcion(ctx.idi.getLine(), ctx.idi.getCharPositionInLine(), "La Funcion " + ctx.idi.getText() + " ya existe", Excepcion.TypeError.Semantico));

            return true;
        }
        // Si el identificador inicial no coincide con el final se crea una Excepcion y no se guarda la funcion
        errorList.add(new Excepcion(ctx.idi.getLine(), ctx.idi.getCharPositionInLine(), "Identificadores en Funcion no coinciden", Excepcion.TypeError.Semantico));

        return true;
    }

    /************************** M A I N   P R O G R A M ********************************/
    /**
     * Se implementa el control de ciclos Exit y Cycle
     * */
    public Object visitBlock_program(GramaticaParser.Block_programContext ctx){
        // Se verifica que los identificadores sean iguales
        if (ctx.idi.getText().equals(ctx.ide.getText())){
            Entorno ent_actual = pilaEnt.peek();
            // Se crea un nuevo Entorno para Program
            Entorno ent_program = new Entorno(ent_actual);
            /**Generado para C3D**/
            ent_actual.siguiente = ent_program;

            // Se almacena el nuevo entorno en la pila para que las Sentencias puedan utilizar el entorno de Program
            pilaEnt.push(ent_program);
            // Se ejecutan las Sentencias dentro de Program
            for (GramaticaParser.SentencesContext stcs: ctx.sentences()){

                // Aca se terminan de almacenar las Excepciones de sentencias en Program
                Object sentence = visitSentences(stcs);
                if (sentence instanceof Excepcion) errorList.add((Excepcion) sentence);
                // Implementacion del control de ciclos EXIT, se guarda el error y se siguen ejecutando las sentencias
                if (sentence instanceof Exit) errorList.add(new Excepcion(((Exit) sentence).fila, ((Exit) sentence).columna, "Implementar Control Exit solo en ciclos", Excepcion.TypeError.Semantico));
                // Implementacion del control de ciclos CYCLE, se guarda el error y se siguen ejecutando las sentencias
                if (sentence instanceof Cycle) errorList.add(new Excepcion(((Cycle) sentence).fila, ((Cycle) sentence).columna, "Implementar Control Cycle solo en ciclos", Excepcion.TypeError.Semantico));
            }
            // Cuando se haya ejecutado Program se elimina su entorno que esta en la pila, para que no quede almacenado.
            pilaEnt.pop();

            return true;
        }
        // Si el identificador inicial no coincide con el final se crea una Excepcion y no se ejecuta el bloque Program
        errorList.add(new Excepcion(ctx.idi.getLine(), ctx.idi.getCharPositionInLine(), "Identificadores en Program no coinciden", Excepcion.TypeError.Semantico));

        return true;
    }

    /************************** I M P R E S I O N   P R I N T *************************************/
    public Object visitPrint(GramaticaParser.PrintContext ctx) {
        for (int i = 0; i < ctx.print_parameter().size(); i++) {

            Object print = visit(ctx.print_parameter(i));
            if (print instanceof Excepcion) errorList.add((Excepcion) print);
            else output += print.toString();
        }
        output += "\n";
        return true;
    }

    public Object visitPrintexp(GramaticaParser.PrintexpContext ctx) {

        Object expresion = visit(ctx.expr());
        if (expresion instanceof Excepcion) return expresion;

        if (expresion instanceof Boolean) {
            if ((boolean) expresion) return "T";
            return "F";
        }

        return expresion.toString();
    }

    public String visitPrintstr(GramaticaParser.PrintstrContext ctx) {
        String cadena = String.valueOf(ctx.str.getText());
        cadena = cadena.replace("\"", "");
        cadena = cadena.replace("\'", "");

        return cadena;
    }

    /************************** D E C L A R A C I O N   D E   V A R I A B L E S *******************/
    public Object visitDeclaration(GramaticaParser.DeclarationContext ctx) {
        Entorno ent_actual = pilaEnt.peek();

        for (int i = 0; i < ctx.decla_asign().size(); i++) {
            String var_name = ctx.decla_asign(i).IDEN().getText();

            if (!ent_actual.tabla_actual.containsKey((var_name + TipoSimbolo.Variable.name()).toUpperCase())) {
                Object expresion = null;
                String tipo_dato = visitType(ctx.type()).toString().toUpperCase();

                if (ctx.decla_asign(i).expr() != null) {
                    expresion = visit(ctx.decla_asign(i).expr());

                    /*
                    * Validar que la expresion no sea una Excepcion, si lo es, salta a la siguiente iteracion
                    * Y la Excepcion se almacena en la lista de Excepciones, no se retorna, pues quiza falten declaraciones en cola
                    * */
                    if (expresion instanceof Excepcion) {
                        errorList.add((Excepcion) expresion);
                        continue;
                    }

                    if (tipo_dato.equals("INTEGER")){
                        if (!(expresion instanceof Integer)){
                            errorList.add(new Excepcion(ctx.decla_asign(i).IDEN().getSymbol().getLine(), ctx.decla_asign(i).IDEN().getSymbol().getCharPositionInLine(), "Expresion " + expresion + " no valida para tipo Integer", Excepcion.TypeError.Semantico));
                            continue;
                        }
                    } else if (tipo_dato.equals("REAL")) {
                        if (!(expresion instanceof Double)) {
                            errorList.add(new Excepcion(ctx.decla_asign(i).IDEN().getSymbol().getLine(), ctx.decla_asign(i).IDEN().getSymbol().getCharPositionInLine(), "Expresion " + expresion + " no valida para tipo Real", Excepcion.TypeError.Semantico));
                            continue;
                        }
                    } else if (tipo_dato.equals("CHARACTER")) {
                        if (!(expresion instanceof Character)) {
                            errorList.add(new Excepcion(ctx.decla_asign(i).IDEN().getSymbol().getLine(), ctx.decla_asign(i).IDEN().getSymbol().getCharPositionInLine(), "Expresion " + expresion + " no valida para tipo Character", Excepcion.TypeError.Semantico));
                            continue;
                        }
                    } else if (tipo_dato.equals("LOGICAL")) {
                        if (!(expresion instanceof Boolean)) {
                            errorList.add(new Excepcion(ctx.decla_asign(i).IDEN().getSymbol().getLine(), ctx.decla_asign(i).IDEN().getSymbol().getCharPositionInLine(), "Expresion " + expresion + " no valida para tipo de Logical", Excepcion.TypeError.Semantico));
                            continue;
                        }
                    }

                } else {
                    if (tipo_dato.equals("INTEGER")) {
                        expresion = 0;
                    } else if (tipo_dato.equals("REAL")) {
                        expresion = 0.00000000;
                    } else if (tipo_dato.equals("CHARACTER")) {
                        expresion = "";
                    } else if (tipo_dato.equals("LOGICAL")) {
                        expresion = false;
                    } else if (tipo_dato.equals("COMPLEX")) {
                        expresion = "(9.192517926E-43,0.00000000)";
                    }

                }
                // Si no hay errores, se genera un nuevo SIMBOLO con los datos de la nueva variable
                Simbolo nuevo = new Simbolo(var_name, tipo_dato, expresion, TipoSimbolo.Variable, ent_actual.lastPosicion);
                /**Generado para C3D**/
                ent_actual.lastPosicion++;
                // Se agrega el nuevo SIMBOLO al entorno actual
                ent_actual.addSimbolo((var_name + TipoSimbolo.Variable.name()), nuevo);

            } else
                errorList.add(new Excepcion(ctx.decla_asign(i).IDEN().getSymbol().getLine(), ctx.decla_asign(i).IDEN().getSymbol().getCharPositionInLine(), "La variable " + var_name + " ya existe en el entorno actual", Excepcion.TypeError.Semantico));
        }

        return true;
    }

    /**
     * getSimbolo(): Recorre la tabla actual y las tablas padres; devuelve null si no encontro el Simbolo
     * updateSimbolo(): Recorre la tabla actual y las tablas padres; devuelve false si el tipo de dato no es el mismo
     * ************************ A S I G N A C I O N   D E   V A R I A B L E S *********************/
    public Object visitAssignment(GramaticaParser.AssignmentContext ctx) {
        Entorno ent_actual = pilaEnt.peek();
        Simbolo auxSimbolo = ent_actual.getSimbolo(ctx.IDEN().getText() + TipoSimbolo.Variable.name());

        if (auxSimbolo == null)
            return new Excepcion(ctx.IDEN().getSymbol().getLine(), ctx.IDEN().getSymbol().getCharPositionInLine(), "La variable " + ctx.IDEN().getText() + " no existe", Excepcion.TypeError.Semantico);

        Object expresion = visit(ctx.expr());
        if (expresion instanceof Excepcion) return expresion;

        String tipo_dato = "";

        if (expresion instanceof Integer) {
            tipo_dato = "INTEGER";
        } else if (expresion instanceof Double) {
            tipo_dato = "REAL";
        } else if (expresion instanceof Character) {
            tipo_dato = "CHARACTER";
        } else if (expresion instanceof Boolean) {
            tipo_dato = "LOGICAL";
        } else if (expresion instanceof ArrayList<?>){
            /*
            * Si la expresion es igual a un arreglo, quiere decir que es posible que sea una asignacion directa entre arreglos
            * Se hacen las validaciones necesarias como: saber si IDEN y EXPR son arreglos ambos
            * Si los dos tienen las mismas dimensiones y si son del mismo tipo
            * */
            if (auxSimbolo.tipo_entorno != TipoSimbolo.Arreglo)
                return new Excepcion(ctx.IDEN().getSymbol().getLine(), ctx.IDEN().getSymbol().getCharPositionInLine(), "La variable " + ctx.IDEN().getText() + " no es un arreglo", Excepcion.TypeError.Semantico);
            // Si la expresion produjo un arreglo, quiere decir que en EXPR hayo un identificador y de este se busco el simbolo
            Simbolo simNameExpr = ent_actual.getSimbolo(ctx.expr().getText() + TipoSimbolo.Variable.name());
            // Comparar dimensiones
            if (!auxSimbolo.dimensiones.equals(simNameExpr.dimensiones))
                return new Excepcion(ctx.IDEN().getSymbol().getLine(), ctx.IDEN().getSymbol().getCharPositionInLine(), "Los arreglo " + ctx.IDEN().getText() + " y " + simNameExpr.identificador + " no tienen las mismas dimensiones", Excepcion.TypeError.Semantico);

            // Comparar dimensiones
            if (!(auxSimbolo.tipo_data.equalsIgnoreCase(simNameExpr.tipo_data)))
                return new Excepcion(ctx.IDEN().getSymbol().getLine(), ctx.IDEN().getSymbol().getCharPositionInLine(), "Los arreglo " + ctx.IDEN().getText() + " y " + simNameExpr.identificador + " no son del mismo tipo de datos", Excepcion.TypeError.Semantico);

            tipo_dato = auxSimbolo.tipo_data;
        }

        Boolean verificar = ent_actual.updateSimbolo(ctx.IDEN().getText() + TipoSimbolo.Variable.name(), tipo_dato, expresion);

        if (!verificar)
            return new Excepcion(ctx.IDEN().getSymbol().getLine(), ctx.IDEN().getSymbol().getCharPositionInLine(), "Tipo de dato no aceptado en variable " + ctx.IDEN().getText(), Excepcion.TypeError.Semantico);

        return true;
    }

    /************************** T I P O S   D E   D A T O S *************************************/
    public String visitType(GramaticaParser.TypeContext ctx) {
        return ctx.getText();
    }

    /************************** D E C L A R A C I O N   D E   A R R E G L O S *********************/
    public Object visitArraydimdecla(GramaticaParser.ArraydimdeclaContext ctx){
        // array_statement : type ',' 'dimension' '(' list_parameter ')' '::' IDEN

        String tipo_dato = visitType(ctx.type()).toUpperCase();
        int fila = ctx.IDEN().getSymbol().getLine();
        int columna = ctx.IDEN().getSymbol().getCharPositionInLine();
        Object list_parameter = this.getDimInteger(ctx.list_parameter(), fila, columna);

        if (list_parameter instanceof Excepcion) return list_parameter;

        Object newArray = this.createArreglo(tipo_dato, ctx.IDEN().getText(), (ArrayList<Integer>) list_parameter, fila, columna);

        if (newArray instanceof Excepcion) return newArray;

        return true;
    }

    public Object visitArraydecla(GramaticaParser.ArraydeclaContext ctx){
        // array_statement : type '::' IDEN '(' list_parameter ')'

        String tipo_dato = visitType(ctx.type()).toUpperCase();
        int fila = ctx.IDEN().getSymbol().getLine();
        int columna = ctx.IDEN().getSymbol().getCharPositionInLine();
        Object list_parameter = this.getDimInteger(ctx.list_parameter(), fila, columna);

        if (list_parameter instanceof Excepcion) return list_parameter;

        Object newArray = this.createArreglo(tipo_dato, ctx.IDEN().getText(), (ArrayList<Integer>) list_parameter, fila, columna);

        if (newArray instanceof Excepcion) return newArray;

        return true;
    }

    /**
     * getDimInteger() : retorna una nueva lista con las dimensiones Integer que daran el tamano y dimensiones que tendra el arreglo
     * No se uso la lista ctx.list_parameter que proporciona Antlr, pues en el metodo createDimension(), se deben extraer elementos y hacer copias del array
     * */
    private Object getDimInteger(GramaticaParser.List_parameterContext list_dimensiones, int fila, int columna){
        ArrayList<Integer> dimensiones = new ArrayList<>();
        Object dimension = null;

        // Se extraen los valores que representan a las dimensiones en un nuevo arrayList, asi se trabaja con el nuevo y no con el que proporciona antlr
        for (GramaticaParser.ExprContext param : list_dimensiones.expr()){
            dimension = visit(param);

            if (dimension instanceof Excepcion)
                return dimension;
            if (!(dimension instanceof Integer))
                return new Excepcion(fila, columna, "Dato: " + dimension.toString() + " no permitido para dimension", Excepcion.TypeError.Semantico);
            if(dimension.equals(0))
                return new Excepcion(fila, columna, "Dimension " + dimension.toString() + " no permitida, inicie en 1", Excepcion.TypeError.Semantico);

            dimensiones.add((int)dimension);
        }

        return dimensiones;
    }

    /**
     * createArreglo() : funcion para hacer validaciones en la creacion de un arreglo con las dimensiones establecidas pero vacias
     * ArrayList dimensiones : almacena los Integer que estableceran las dimensiones
     * createArreglo() : se utilizo para los dos tipos de declaracion de arreglos
     * */
    private Object createArreglo(String type_data, String id, ArrayList<Integer> dimensiones, int fila, int columna){
        // *NOTA* Se restringe la cantidad de dimensiones, pues para este Proyecto solo se permiten de 2D
        if (dimensiones.size() > 2)
            return new Excepcion(fila, columna, "Los arreglos como: " + id + " solo pueden tener maximo 2 dimensiones", Excepcion.TypeError.Sintactico);

        Entorno ent_actual = pilaEnt.peek();
        // Verificar que el nombre de la variable no haya sido utilizada
        if (!ent_actual.tabla_actual.containsKey((id + TipoSimbolo.Variable.name()).toUpperCase())){

            // Se crea una copia de dimensiones porque la original se usa en createDimension() y ahi se remueven todos los datos
            ArrayList<Integer> dimensionsCopy = (ArrayList<Integer>) dimensiones.clone();

            // Crear dimensiones vacias
            Object array = this.createDimension(dimensiones);
            if (array instanceof Excepcion)
                return array;

            // Si no hay errores, se genera un nuevo SIMBOLO con un array dimensionado pero vacio
            Simbolo nuevo = new Simbolo(id, type_data, array, dimensionsCopy, TipoSimbolo.Arreglo);
            // Se agrega el nuevo SIMBOLO al entorno actual
            ent_actual.addSimbolo((id + TipoSimbolo.Variable.name()), nuevo);

            return true;
        }
        return new Excepcion(fila, columna, "La variable " + id + " ya existe en el entorno actual", Excepcion.TypeError.Semantico);
    }

    /**
     * createDimension() : funcion recursiva que permite retornar un nuevo ArrayList de ArrayList llenados con null
     * desde este nuevo ArrayList se trabajaran las dimensiones al momento de hacer asignaciones o peticiones de valores en alguna posicion
     * Se hacen clones de 'dimensiones' para pasar la lista por valor y no por referencia
     * */
    private Object createDimension(ArrayList<Integer> dimensiones){

        if (dimensiones.size() == 0)
            return null;

        int dimension = dimensiones.remove(0);
        ArrayList<Object> arrayReturn = new ArrayList<>();

        int contador = 0;
        while (contador < dimension){
            ArrayList<Integer> arrayCopy = (ArrayList<Integer>) dimensiones.clone();
            arrayReturn.add(this.createDimension(arrayCopy));
            contador += 1;
        }
        return arrayReturn;

    }

    /************************** A S I G N A C I O N   D E   A R R E G L O S   1D *********************/
    public Object visitArraylistassig(GramaticaParser.ArraylistassigContext ctx){
        // array_assignment    : IDEN '=' '(' '/' list_parameter '/' ')'

        Entorno ent_actual = pilaEnt.peek();
        Simbolo auxSimbolo = ent_actual.getSimbolo(ctx.IDEN().getText() + TipoSimbolo.Variable.name());

        if (auxSimbolo == null)
            return new Excepcion(ctx.IDEN().getSymbol().getLine(), ctx.IDEN().getSymbol().getCharPositionInLine(), "El arreglo " + ctx.IDEN().getText() + " no existe", Excepcion.TypeError.Semantico);

        if (auxSimbolo.tipo_entorno != TipoSimbolo.Arreglo)
            return new Excepcion(ctx.IDEN().getSymbol().getLine(), ctx.IDEN().getSymbol().getCharPositionInLine(), "La variable " + ctx.IDEN().getText() + " no es un arreglo", Excepcion.TypeError.Semantico);

        // En este tipo de asignacion solo es para arreglos de 1 dimension
        if (auxSimbolo.dimensiones.size() > 1)
            return new Excepcion(ctx.IDEN().getSymbol().getLine(), ctx.IDEN().getSymbol().getCharPositionInLine(), "El arreglo " + ctx.IDEN().getText() + " es de mayor dimension", Excepcion.TypeError.Sintactico);

        // Se sabe que este arreglo es de solo una dimension, por lo que el tamaño se puede obtener del primer atributo numero de auxSimbolo.dimensiones
        if (auxSimbolo.dimensiones.get(0) != ctx.list_parameter().expr().size())
            return new Excepcion(ctx.IDEN().getSymbol().getLine(), ctx.IDEN().getSymbol().getCharPositionInLine(), "El tamaño del arreglo " + ctx.IDEN().getText() + " es " + auxSimbolo.dimensiones.get(0), Excepcion.TypeError.Semantico);

        // Se crea un Array (solo por ser de 1D) para almacenar todos los datos nuevos
        ArrayList<Object> nuevoArray = new ArrayList<>();
        for (GramaticaParser.ExprContext exp : ctx.list_parameter().expr()){
            Object expresion = visit(exp);
            if (expresion instanceof Excepcion) return expresion;

            String tipo_exp = "";

            if (expresion instanceof Integer) {
                tipo_exp = "INTEGER";
            } else if (expresion instanceof Double) {
                tipo_exp = "REAL";
            } else if (expresion instanceof Character) {
                tipo_exp = "CHARACTER";
            } else return new Excepcion(ctx.IDEN().getSymbol().getLine(), ctx.IDEN().getSymbol().getCharPositionInLine(), "Dato: " + expresion.toString() + " no permitido en este arreglo", Excepcion.TypeError.Semantico);

            // Se verifica que el tipo_data del nuevo valor coincide con el tipo_data del arreglo a modificar
            if (!auxSimbolo.tipo_data.equalsIgnoreCase(tipo_exp))
                return new Excepcion(ctx.IDEN().getSymbol().getLine(), ctx.IDEN().getSymbol().getCharPositionInLine(), "Dato: " + expresion.toString() + " no permitido en este arreglo", Excepcion.TypeError.Semantico);

            nuevoArray.add(expresion);
        }

        // Como solo es de 1D, es factible solo actualizar el objeto Array en el Simbolo
        boolean verificar = ent_actual.updateSimbolo(ctx.IDEN().getText() + TipoSimbolo.Variable.name(), auxSimbolo.tipo_data, nuevoArray);

        if (!verificar)
            return new Excepcion(ctx.IDEN().getSymbol().getLine(), ctx.IDEN().getSymbol().getCharPositionInLine(), "Tipo de dato no aceptado en arreglo " + ctx.IDEN().getText(), Excepcion.TypeError.Semantico);

        return true;
    }

    public Object visitArrayposassig(GramaticaParser.ArrayposassigContext ctx){
        // array_assignment    : IDEN '[' list_parameter ']' '=' expr

        // *NOTA* Se restringe la cantidad de dimensiones, pues esta es una asignacion para arreglos de 1D
        if (ctx.list_parameter().expr().size() > 1)
            return new Excepcion(ctx.IDEN().getSymbol().getLine(), ctx.IDEN().getSymbol().getCharPositionInLine(), "Los arreglos como: " + ctx.IDEN().getText() + " no pueden asignarse en este entorno", Excepcion.TypeError.Sintactico);

        int fila = ctx.IDEN().getSymbol().getLine();
        int columna = ctx.IDEN().getSymbol().getCharPositionInLine();
        Object list_parameter = this.getDimInteger(ctx.list_parameter(), fila, columna);

        if (list_parameter instanceof Excepcion) return list_parameter;

        return this.initArrayAssignment(ctx.IDEN().getText(), (ArrayList<Integer>) list_parameter, ctx.expr(), fila, columna);
    }

    /************************** A S I G N A C I O N   D E   A R R E G L O S   2D *********************/
    public Object visitArray_assignment2d(GramaticaParser.Array_assignment2dContext ctx){
        // array_assignment2d  : IDEN '[' list_parameter ']' '=' expr

        // *NOTA* Se restringe la cantidad de dimensiones, pues para este Proyecto solo se permiten de 2D
        if (ctx.list_parameter().expr().size() > 2)
            return new Excepcion(ctx.IDEN().getSymbol().getLine(), ctx.IDEN().getSymbol().getCharPositionInLine(), "Los arreglos como: " + ctx.IDEN().getText() + " solo pueden tener maximo 2 dimensiones", Excepcion.TypeError.Sintactico);

        int fila = ctx.IDEN().getSymbol().getLine();
        int columna = ctx.IDEN().getSymbol().getCharPositionInLine();
        Object list_parameter = this.getDimInteger(ctx.list_parameter(), fila, columna);

        if (list_parameter instanceof Excepcion) return list_parameter;

        return this.initArrayAssignment(ctx.IDEN().getText(), (ArrayList<Integer>) list_parameter, ctx.expr(), fila, columna);
    }

    /**
     * initArrayAssignment() : hace las validaciones necesarias para confirmar que se manipula un arreglo
     * Este metodo es utilizado tanto en la asignacion de 1 dimension como en la de 2 mas dimensiones
     * */
    private Object initArrayAssignment(String id, ArrayList<Integer> dimensiones, GramaticaParser.ExprContext exp, int fila, int columna){
        Entorno ent_actual = pilaEnt.peek();
        Simbolo auxSimbolo = ent_actual.getSimbolo(id + TipoSimbolo.Variable.name());

        if (auxSimbolo == null)
            return new Excepcion(fila, columna, "El arreglo " + id + " no existe", Excepcion.TypeError.Semantico);

        if (auxSimbolo.tipo_entorno != TipoSimbolo.Arreglo)
            return new Excepcion(fila, columna, "El variable " + id + " no es un arreglo", Excepcion.TypeError.Semantico);

        Object expresion = visit(exp);
        if (expresion instanceof Excepcion) return expresion;

        String tipo_dato = "";

        if (expresion instanceof Integer) {
            tipo_dato = "INTEGER";
        } else if (expresion instanceof Double) {
            tipo_dato = "REAL";
        } else if (expresion instanceof Character) {
            tipo_dato = "CHARACTER";
        } else return new Excepcion(fila, columna, "Dato: " + expresion.toString() + " no permitido en este arreglo", Excepcion.TypeError.Semantico);

        if (!auxSimbolo.tipo_data.equalsIgnoreCase(tipo_dato))
            return new Excepcion(fila, columna, "Dato: " + expresion.toString() + " no permitido en este arreglo", Excepcion.TypeError.Semantico);

        return this.modifyDimension(dimensiones, (ArrayList<Object>) auxSimbolo.valor, expresion, fila, columna);
    }

    /**
     * modifyDimension() : metodo recursivo que navega en todos los arreglos contenidos en aux_array segun las dimensiones otorgadas
     * Dentro del trycatch antes de la ultima recursividad se valida que en el array aux_array ya contenga un dato o nuevamente un arreglo
     * pues si ya se tiene el dato, este dara error si se pasa como parametro en el lugar de un arreglo
     * */
    private Object modifyDimension(ArrayList<Integer> dimensiones, ArrayList<Object> aux_array, Object value, int fila, int columna) {
        if (dimensiones.isEmpty()) {
            if (aux_array instanceof ArrayList<Object>)
                return new Excepcion(fila, columna, "El arreglo es de mayor dimension", Excepcion.TypeError.Semantico);

            return value;
        }

        if (!(aux_array instanceof ArrayList<Object>))
            return new Excepcion(fila, columna, "Dimension no establecida", Excepcion.TypeError.Semantico);

        int dimension = dimensiones.remove(0);
        // Se le resta 1 a dimension por cuestion de que los arreglos en Fortran empiezan en 1 y no en 0
        // Pero java si permite la posicion en 0
        dimension = dimension -1;

        try {
            Object valor;
            // Esta validacion permite verificar que la siguiente dimension sea un arreglo antes de enviarse como parametro
            // Pues el parametro establecido recibe un Array, y si en la siguiente dimension ya se encuentra el dato, este no coincide con el parametro esperado
            if (!(aux_array.get(dimension) instanceof ArrayList<?>)){
                if (!dimensiones.isEmpty())
                    return new Excepcion(fila, columna, "El arreglo es de menor dimension", Excepcion.TypeError.Semantico);

                valor = value;

            } else valor = this.modifyDimension(dimensiones, (ArrayList<Object>) aux_array.get(dimension), value, fila, columna);

            if (valor instanceof Excepcion) return valor;

            if (valor != null)
                aux_array.set(dimension, valor);

        } catch (Exception e){
            return new Excepcion(fila, columna, "Dimension no establecida", Excepcion.TypeError.Semantico);
        }

        return null;
    }

    /************************** S E N T E N C I A   IF, IF ELSE, ELSE ***********************************/
    /**
     * Se implementa el control de ciclos EXIT y CYCLE
     * */
    public Object visitIf_statement(GramaticaParser.If_statementContext ctx){

        Entorno ent_actual = pilaEnt.peek();
        // Expresion condicional del IF
        Object condicion = visit(ctx.expr());
        if (condicion instanceof Excepcion) return condicion;

        if (condicion instanceof Boolean){

            if ((boolean)condicion){
                // Se crea un nuevo Entorno para el IF
                Entorno ent_if = new Entorno(ent_actual);

                // Se ingresa el nuevo entorno a la pila para que se utilice en las sentencias que este posee
                pilaEnt.push(ent_if);
                for (int i = 0; i < ctx.stmt_if.getChildCount(); i++) {

                    Object respond = visit(ctx.stmt_if.getChild(i));
                    if (respond instanceof Excepcion) errorList.add((Excepcion) respond);
                    // Implementacion del control de ciclos EXIT, se retorna 'respond' para que lo valide en el entorno padre
                    if (respond instanceof Exit){
                        pilaEnt.pop();
                        return respond;
                    }
                    // Implementacion del control de ciclos CYCLE, se retorna 'respond' para que lo valide en el entorno padre
                    if (respond instanceof Cycle){
                        pilaEnt.pop();
                        return respond;
                    }
                }
                // Cuando se hayan ejecutado las sentencias del IF se elimina su entorno que esta en la pila, para que no quede almacenado.
                pilaEnt.pop();
            } else {
                if (ctx.else_if_stmt() != null){

                    for (GramaticaParser.Else_if_stmtContext else_if : ctx.else_if_stmt()){
                        Object respond = visit(else_if);
                        if (respond instanceof Excepcion) return respond;
                        // Implementacion del control de ciclos EXIT, se retorna 'respond' para que lo valide en el entorno padre
                        if (respond instanceof Exit) return respond;

                        // Si respond es TRUE quiere decir que entro en uno de los ELSE IF y ya se ejecutaron las sentencias, por lo tanto aqui termina
                        if (respond instanceof Boolean){
                            if ((boolean)respond) return true;
                        }
                    }
                }

                if (ctx.stmt_else != null) {
                    // Se crea un nuevo Entorno para el ELSE
                    Entorno ent_else = new Entorno(ent_actual);

                    // Se ingresa el nuevo entorno a la pila para que se utilice en las sentencias que este posee
                    pilaEnt.push(ent_else);
                    for (int i = 0; i < ctx.stmt_else.getChildCount(); i++) {

                        Object respond = visit(ctx.stmt_else.getChild(i));
                        if (respond instanceof Excepcion) errorList.add((Excepcion) respond);
                        // Implementacion del control de ciclos EXIT, se retorna 'respond' para que lo valide en el entorno padre
                        if (respond instanceof Exit){
                            pilaEnt.pop();
                            return respond;
                        }
                    }
                    // Cuando se hayan ejecutado las sentencias del ELSE se elimina su entorno que esta en la pila, para que no quede almacenado.
                    pilaEnt.pop();
                }
            }

        } else return new Excepcion(ctx.expr().getStart().getLine(), ctx.expr().getStart().getCharPositionInLine(), "La condicion no es de tipo logical", Excepcion.TypeError.Semantico);

        return true;
    }

    public Object visitElse_if_stmt(GramaticaParser.Else_if_stmtContext ctx){
        Entorno ent_actual = pilaEnt.peek();
        // Expresion condicional del ELSE IF
        Object condicion = visit(ctx.expr());
        if (condicion instanceof Excepcion) return condicion;

        if (condicion instanceof Boolean){

            if ((boolean)condicion){
                // Se crea un nuevo Entorno para el ELSE IF
                Entorno ent_else_if = new Entorno(ent_actual);

                // Se ingresa el nuevo entorno a la pila para que se utilice en las sentencias que este posee
                pilaEnt.push(ent_else_if);
                for (GramaticaParser.SentencesContext stmt : ctx.sentences()){
                    Object respond = visitSentences(stmt);
                    if (respond instanceof Excepcion) errorList.add((Excepcion) respond);
                    // Implementacion del control de ciclos EXIT, se retorna 'respond' para que lo valide en el entorno padre
                    if (respond instanceof Exit){
                        pilaEnt.pop();
                        return respond;
                    }
                    // Implementacion del control de ciclos CYCLE, se retorna 'respond' para que lo valide en el entorno padre
                    if (respond instanceof Cycle){
                        pilaEnt.pop();
                        return respond;
                    }
                }
                // Cuando se hayan ejecutado las sentencias del ELSE IF se elimina su entorno que esta en la pila, para que no quede almacenado.
                pilaEnt.pop();
                return true;

            } else return false;

        } else return new Excepcion(ctx.expr().getStart().getLine(), ctx.expr().getStart().getCharPositionInLine(), "La condicion no es de tipo logical", Excepcion.TypeError.Semantico);

    }

    /************************** S E N T E N C I A   D O ***********************************/
    /**
     * Se implementa el control de ciclos EXIT y CYCLE
     * */
    public Object visitDo_statement(GramaticaParser.Do_statementContext ctx){
        // do_statement    : 'do' assignment ',' e1=expr ',' e2=expr do_sentences* 'end' 'do'
        Entorno ent_actual = pilaEnt.peek();
        // Se corrobora que el dato de la asignacion sea Integer
        Object datoAsignacion = visit(ctx.assignment().expr());
        if (datoAsignacion instanceof Excepcion) return datoAsignacion;
        if (!(datoAsignacion instanceof Integer)) return new Excepcion(ctx.assignment().expr().getStart().getLine(), ctx.assignment().expr().getStart().getCharPositionInLine(), "Asignacion debe ser dato tipo Integer", Excepcion.TypeError.Semantico);

        // Se realiza la asignacion
        Object asignacion = visitAssignment(ctx.assignment());
        if (asignacion instanceof Excepcion) return asignacion;

        // Variable en la que se realizo la asignacion
        Simbolo simAsignacion = ent_actual.getSimbolo(ctx.assignment().IDEN().getText() + TipoSimbolo.Variable.name());
        if (simAsignacion == null) return new Excepcion(ctx.assignment().IDEN().getSymbol().getLine(), ctx.assignment().IDEN().getSymbol().getCharPositionInLine(), "La variable "+ ctx.assignment().IDEN().getText() +" no existe", Excepcion.TypeError.Semantico);

        // Se obtiene la segunda expresion la cual indica hasta donde itera el ciclo
        Object alcance = visit(ctx.e1);
        if (alcance instanceof Excepcion) return alcance;
        if (!(alcance instanceof Integer)) return new Excepcion(ctx.e1.getStart().getLine(), ctx.e1.getStart().getCharPositionInLine(), "Alcance debe ser dato tipo Integer", Excepcion.TypeError.Semantico);

        int indice = (int)simAsignacion.valor;
        int size = (int)alcance;
        int paso = 1;

        if (ctx.e2 != null){
            Object pasoIterador = visit(ctx.e2);
            if (pasoIterador instanceof Excepcion) return alcance;
            if (!(pasoIterador instanceof Integer)) return new Excepcion(ctx.e2.getStart().getLine(), ctx.e2.getStart().getCharPositionInLine(), "Paso de iteracion debe ser dato tipo Integer", Excepcion.TypeError.Semantico);

            paso = (int) pasoIterador;
        }

        for (int i = indice; i <= size; i= i + paso) {

            Boolean verificar = ent_actual.updateSimbolo(ctx.assignment().IDEN().getText() + TipoSimbolo.Variable.name(), "INTEGER", i);

            if (!verificar)
                return new Excepcion(ctx.assignment().IDEN().getSymbol().getLine(), ctx.assignment().IDEN().getSymbol().getCharPositionInLine(), "Tipo de dato no aceptado en variable " + ctx.assignment().IDEN().getText(), Excepcion.TypeError.Semantico);

            // Se crea un nuevo Entorno para el DO
            Entorno ent_do = new Entorno(ent_actual);
            // Se ingresa el nuevo entorno a la pila para que se utilice en las sentencias que este posee en cada pasada
            pilaEnt.push(ent_do);

            for (GramaticaParser.SentencesContext stmt : ctx.sentences()){
                Object respond = visitSentences(stmt);
                if (respond instanceof Excepcion) errorList.add((Excepcion) respond);
                // Implementacion del control de ciclos EXIT
                if (respond instanceof Exit){
                    pilaEnt.pop();
                    return true;
                }
                // Implementacion del control de ciclos CYCLE
                if (respond instanceof Cycle) break;
            }
            // Cuando se hayan ejecutado las sentencias del DO se elimina su entorno que esta en la pila, para que no quede almacenado.
            pilaEnt.pop();

            Simbolo newIndex = ent_actual.getSimbolo(ctx.assignment().IDEN().getText() + TipoSimbolo.Variable.name());
            if (newIndex == null) return new Excepcion(ctx.assignment().IDEN().getSymbol().getLine(), ctx.assignment().IDEN().getSymbol().getCharPositionInLine(), "La variable "+ ctx.assignment().IDEN().getText() +" no existe", Excepcion.TypeError.Semantico);

            i = (int)newIndex.valor;
        }

        return true;
    }

    /************************** S E N T E N C I A   D O   W H I L E ***********************************/
    /**
     * Se implementa el control de ciclos EXIT y CYCLE
     * */
    public Object visitDowhile_statement(GramaticaParser.Dowhile_statementContext ctx){
        // dowhile_statement   : 'do' 'while' '(' expr ')' do_sentences* 'end' 'do'
        Entorno ent_actual = pilaEnt.peek();

        while (true){

            // Expresion condicional del DO WHILE
            Object condicion = visit(ctx.expr());
            if (condicion instanceof Excepcion) return condicion;

            if (condicion instanceof Boolean){

                if ((boolean) condicion){
                    // Se crea un nuevo Entorno para el DO WHILE
                    Entorno ent_dowhile = new Entorno(ent_actual);
                    // Se ingresa el nuevo entorno a la pila para que se utilice en las sentencias que este posee
                    pilaEnt.push(ent_dowhile);

                    for (GramaticaParser.SentencesContext stmt : ctx.sentences()){

                        Object responds = visitSentences(stmt);
                        if (responds instanceof Excepcion) errorList.add((Excepcion) responds);
                        // Implementacion del control de ciclos EXIT
                        if (responds instanceof Exit){
                            pilaEnt.pop();
                            return true;
                        }
                        // Implementacion del control de ciclos CYCLE
                        if (responds instanceof Cycle){
                            break;
                        }

                    }
                    // Cuando se hayan ejecutado las sentencias del DO WHILE se elimina su entorno que esta en la pila, para que no quede almacenado.
                    pilaEnt.pop();
                } else break;

            } else return new Excepcion(ctx.expr().getStart().getLine(), ctx.expr().getStart().getCharPositionInLine(), "La condicion no es de tipo logical", Excepcion.TypeError.Semantico);
        }

        return true;
    }

    /************************** C O N T R O L   D E   C I C L O S   E X I T ****************************/
    public Object visitExit_control(GramaticaParser.Exit_controlContext ctx){
        // exit_control : 'exit'
        return new Exit(ctx.getStart().getLine(), ctx.getStart().getCharPositionInLine());
    }

    /************************** C O N T R O L   D E   C I C L O S   C Y C L E ****************************/
    public Object visitCycle_control(GramaticaParser.Cycle_controlContext ctx){
        // exit_control : 'exit'
        return new Cycle(ctx.getStart().getLine(), ctx.getStart().getCharPositionInLine());
    }

    /************************** E X P R E S I O N E S *************************************/
    public Object visitOpunaria(GramaticaParser.OpunariaContext ctx) {
        Object exp = visit(ctx.right);
        if (exp instanceof Excepcion) return exp;

        String operacion = ctx.op.getText();

        if (exp instanceof Integer) {
            int der = (int) exp;

            if (operacion.equals("-")) return -der;

        } else if (exp instanceof Double) {
            double der = (double) exp;

            if (operacion.equals("-")) return -der;

        } else if (exp instanceof Boolean) {
            boolean der = (boolean) exp;

            if (operacion.equals(".not.")) return !der;
        }
        return new Excepcion(ctx.op.getLine(), ctx.op.getCharPositionInLine(), "Operacion unaria con " + operacion + " no valida", Excepcion.TypeError.Semantico);
    }

    public Object visitOpexpr(GramaticaParser.OpexprContext ctx) {
        Object exp1 = visit(ctx.left);
        if (exp1 instanceof Excepcion) return exp1;

        Object exp2 = visit(ctx.right);
        if (exp2 instanceof Excepcion) return exp2;

        String operacion = ctx.op.getText();

        if (exp1 instanceof Integer && exp2 instanceof Integer) {
            int izq = (int) exp1;
            int der = (int) exp2;

            if (operacion.equals("**")) {

                return (int) Math.pow(izq, der);
            }

            switch (operacion.charAt(0)) {
                case '*':
                    return izq * der;
                case '/':
                    try {
                        return (int) izq / der;
                    } catch (Exception e){
                        return new Excepcion(ctx.op.getLine(), ctx.op.getCharPositionInLine(), "Operacion con " + operacion + " no valida", Excepcion.TypeError.Semantico);
                    }
                case '+':
                    return izq + der;
                case '-':
                    return izq - der;
                default:
                    return new Excepcion(ctx.op.getLine(), ctx.op.getCharPositionInLine(), "Operacion con " + operacion + " no valida", Excepcion.TypeError.Semantico);
            }
        } else if (exp1 instanceof Double && exp2 instanceof Integer
                || exp1 instanceof Integer && exp2 instanceof Double
                || exp1 instanceof Double && exp2 instanceof Double) {
            double izq = (double) exp1;
            double der = (double) exp2;

            if (operacion.equals("**")) {

                return Math.pow(izq, der);
            }

            switch (operacion.charAt(0)) {
                case '*':
                    return izq * der;
                case '/':
                    try {
                        return izq / der;
                    } catch (Exception e){
                        return new Excepcion(ctx.op.getLine(), ctx.op.getCharPositionInLine(), "Operacion con " + operacion + " no valida", Excepcion.TypeError.Semantico);
                    }
                case '+':
                    return izq + der;
                case '-':
                    return izq - der;
                default:
                    return new Excepcion(ctx.op.getLine(), ctx.op.getCharPositionInLine(), "Operacion con " + operacion + " no valida", Excepcion.TypeError.Semantico);
            }
        }

        return new Excepcion(ctx.op.getLine(), ctx.op.getCharPositionInLine(), "Operacion Aritmetica " + operacion + " con tipo de dato no permitido", Excepcion.TypeError.Semantico);
    }

    public Object visitOpexprrel(GramaticaParser.OpexprrelContext ctx) {
        Object exp1 = visit(ctx.left);
        if (exp1 instanceof Excepcion) return exp1;

        Object exp2 = visit(ctx.right);
        if (exp2 instanceof Excepcion) return exp2;

        String operacion = ctx.op.getText();

        if (exp1 instanceof Integer && exp2 instanceof Integer) {
            int izq = (int) exp1;
            int der = (int) exp2;

            if (operacion.equals("==") || operacion.equals(".eq.")) {
                return izq == der;
            } else if (operacion.equals("/=") || operacion.equals(".ne.")) {
                return izq != der;
            } else if (operacion.equals(">") || operacion.equals(".gt.")) {
                return izq > der;
            } else if (operacion.equals("<") || operacion.equals(".lt.")) {
                return izq < der;
            } else if (operacion.equals(">=") || operacion.equals(".ge.")) {
                return izq >= der;
            } else if (operacion.equals("<=") || operacion.equals(".le.")) {
                return izq <= der;
            }
        } else if (exp1 instanceof Double && exp2 instanceof Integer
                || exp1 instanceof Integer && exp2 instanceof Double
                || exp1 instanceof Double && exp2 instanceof Double) {
            double izq = (double) exp1;
            double der = (double) exp2;

            if (operacion.equals("==") || operacion.equals(".eq.")) {
                return izq == der;
            } else if (operacion.equals("/=") || operacion.equals(".ne.")) {
                return izq != der;
            } else if (operacion.equals(">") || operacion.equals(".gt.")) {
                return izq > der;
            } else if (operacion.equals("<") || operacion.equals(".lt.")) {
                return izq < der;
            } else if (operacion.equals(">=") || operacion.equals(".ge.")) {
                return izq >= der;
            } else if (operacion.equals("<=") || operacion.equals(".le.")) {
                return izq <= der;
            }
        } else if (exp1 instanceof Boolean && exp2 instanceof Boolean
                || exp1 instanceof Character && exp2 instanceof Character) {

            if (operacion.equals("==") || operacion.equals(".eq.")) {
                return exp1 == exp2;
            } else if (operacion.equals("/=") || operacion.equals(".ne.")) {
                return exp1 != exp2;
            }
        }

        return new Excepcion(ctx.op.ope.getLine(), ctx.op.ope.getCharPositionInLine(), "Operacion Relacional " + operacion + " con tipo de dato no permitido", Excepcion.TypeError.Semantico);
    }

    public Object visitOpexprlog(GramaticaParser.OpexprlogContext ctx) {
        Object exp1 = visit(ctx.left);
        if (exp1 instanceof Excepcion) return exp1;

        Object exp2 = visit(ctx.right);
        if (exp2 instanceof Excepcion) return exp2;

        String operacion = ctx.op.getText();

        if (exp1 instanceof Boolean && exp2 instanceof Boolean) {
            boolean izq = (boolean) exp1;
            boolean der = (boolean) exp2;
            if (operacion.equalsIgnoreCase(".and.")) {
                return izq && der;
            } else if (operacion.equalsIgnoreCase(".or.")) {
                return izq || der;
            }
        }

        return new Excepcion(ctx.op.getLine(), ctx.op.getCharPositionInLine(), "Operacion Logica " + operacion + " con tipo de dato no permitido", Excepcion.TypeError.Semantico);
    }

    public Object visitParenexpr(GramaticaParser.ParenexprContext ctx) {
        return visit(ctx.expr());
    }

    public Integer visitAtomexpr(GramaticaParser.AtomexprContext ctx) {
        return Integer.valueOf(ctx.getText());
    }

    public Double visitDeciexpr(GramaticaParser.DeciexprContext ctx) {
        return Double.valueOf(ctx.getText());
    }

    public Character visitCharexpr(GramaticaParser.CharexprContext ctx) {
        String cadena = String.valueOf(ctx.getText());
        cadena = cadena.replace("\"", "");
        cadena = cadena.replace("\'", "");
        char caracter = cadena.charAt(0);

        return caracter;
    }

    /**
     * getSimbolo(): Recorre la tabla actual y las tablas padres; devuelve null si no encontro el Simbolo
     * */
    public Object visitIdexpr(GramaticaParser.IdexprContext ctx) {
        Entorno ent_actual = pilaEnt.peek();
        Simbolo auxSimbolo = ent_actual.getSimbolo(ctx.IDEN().getText() + TipoSimbolo.Variable.name());

        // Si la variable no existe se retorna una Excepcion
        if (auxSimbolo == null)
            return new Excepcion(ctx.IDEN().getSymbol().getLine(), ctx.IDEN().getSymbol().getCharPositionInLine(), "La variable " + ctx.IDEN().getText() + " no existe", Excepcion.TypeError.Semantico);

        return auxSimbolo.valor;
    }

    public Object visitCmplxexpr(GramaticaParser.CmplxexprContext ctx) {
        return String.valueOf(ctx.getText());
    }

    public Boolean visitBoleanexpr(GramaticaParser.BoleanexprContext ctx) {
        String valor = ctx.getText();
        if (valor.equals(".true.")) return true;
        return false;
    }

    /**
     * Se implementa el control de ciclos Exit y Cycle
     * */
    public Object visitFuncexpr(GramaticaParser.FuncexprContext ctx){
        Entorno ent_actual = pilaEnt.peek();

        Simbolo simFuncion = ent_actual.getSimbolo(ctx.IDEN().getText() + TipoSimbolo.Funcion.name());

        // Se verifica que la funcion exista
        if (simFuncion == null)
            return new Excepcion(ctx.IDEN().getSymbol().getLine(), ctx.IDEN().getSymbol().getCharPositionInLine(), "La Funcion " + ctx.IDEN().getText() + " no existe", Excepcion.TypeError.Semantico);

        /*
        * VALIDACION PARA SABER SI LA FUNCION ES UNA NATIVA
        * Si lo es, se ejecuta su funcion y ya no se sigue con el codigo debajo
        * */
        if (ctx.IDEN().getText().equalsIgnoreCase("SIZE")) return this.functionSize(ctx.list_parameter(), ctx.IDEN().getSymbol().getLine(), ctx.IDEN().getSymbol().getCharPositionInLine());

        // Verificacion que solo en el entorno del metodo PROGRAM donde la funcion fue llamada este declarada su nombre
        if (ent_actual.padre.padre == null){
            if (!ent_actual.tabla_actual.containsKey((ctx.IDEN().getText() + TipoSimbolo.Variable.name()).toUpperCase()))
                return new Excepcion(ctx.IDEN().getSymbol().getLine(), ctx.IDEN().getSymbol().getCharPositionInLine(), "La Funcion " + ctx.IDEN().getText() + " no esta declarada", Excepcion.TypeError.Semantico);
        }

        // Se crea un nuevo Entorno para la Funcion
        Entorno ent_func = new Entorno(ent_actual);
        // Se extrae la instancia de la Funcion almacenada cuando se declaro
        Funcion auxFuncion = (Funcion) simFuncion.valor;

        // Se verifica que vengan parametros
        if (ctx.list_parameter() != null){

            if (auxFuncion.list_param.size() == ctx.list_parameter().expr().size() && auxFuncion.list_param.size() == auxFuncion.listDecla_param.getChildCount()){
                /*
                * Si tanto la lista de parametros en la declaracion, como en la llamada, son del mismo tamaño
                * Se recorren los parametros para asignarse a un nuevo Simbolo
                * */
                for (int i = 0; i < ctx.list_parameter().expr().size(); i++) {
                    /*
                     * Se supone que tanto los parametros como la declaracion de estos en la Funcion vienen en orden
                     * Ejemplo Parametros: FuncionName(var1, var2, var3)
                     * Ejemplo Declaraciones: Integer intent in ::var1 - Integer intent in ::var2 - Character intent in :: var3
                     * Pero igual se verifica que sean llamados igual
                     * */
                    if (auxFuncion.list_param.get(i).identificador.equalsIgnoreCase(auxFuncion.listDecla_param.decla_parameter(i).IDEN().getText())){

                        Object exp = visit(ctx.list_parameter().expr().get(i));
                        /*
                         * Validar que la expresion no sea una Excepcion, si lo es, salta a la siguiente iteracion
                         * Y la Excepcion se almacena en la lista de Excepciones, no se retorna, pues quiza falten parametros en cola
                         * Si es una Excepcion el Simbolo de la expresion se ignora y no se almacena en el entorno actual
                         * */
                        if (exp instanceof Excepcion) {
                            errorList.add((Excepcion) exp);
                            continue;
                        }

                        // Tipo de dato declarado en la Funcion
                        String tipo_datoDecla = auxFuncion.listDecla_param.decla_parameter(i).type().getText().toUpperCase();
                        // Verificar si el Tipo de dato del parametro de la llamada coincide con el de la declaracion en la Funcion

                        if (exp instanceof Integer){
                            if (!tipo_datoDecla.equals("INTEGER")){
                                errorList.add(new Excepcion(ctx.IDEN().getSymbol().getLine(), ctx.IDEN().getSymbol().getCharPositionInLine(), "Parametro " + exp + " no valido en peticion de Integer", Excepcion.TypeError.Semantico));
                                continue;
                            }
                        } else if (exp instanceof Double) {
                            if (!tipo_datoDecla.equals("REAL")){
                                errorList.add(new Excepcion(ctx.IDEN().getSymbol().getLine(), ctx.IDEN().getSymbol().getCharPositionInLine(), "Parametro " + exp + " no valido en peticion de Real", Excepcion.TypeError.Semantico));
                                continue;
                            }
                        } else if (exp instanceof Character) {
                            if (!tipo_datoDecla.equals("CHARACTER")){
                                errorList.add(new Excepcion(ctx.IDEN().getSymbol().getLine(), ctx.IDEN().getSymbol().getCharPositionInLine(), "Parametro " + exp + " no valido en peticion de Character", Excepcion.TypeError.Semantico));
                                continue;
                            }
                        } else if (exp instanceof Boolean) {
                            if (!tipo_datoDecla.equals("LOGICAL")){
                                errorList.add(new Excepcion(ctx.IDEN().getSymbol().getLine(), ctx.IDEN().getSymbol().getCharPositionInLine(), "Parametro " + exp + " no valido en peticion de Logical", Excepcion.TypeError.Semantico));
                                continue;
                            }
                        } else if (exp instanceof ArrayList<?>){
                            /*
                             * Si la expresion es igual a un arreglo, quiere decir que es una asignacion directa entre arreglos
                             * Se hacen las validaciones necesarias como: saber si IDEN(Parametro) y IDEN(Declaracion) son arreglos ambos
                             * Si los dos tienen las mismas dimensiones y si son del mismo tipo
                             * */

                            // Se verifica que la declaracion del parametro sea para un arreglo
                            if (auxFuncion.listDecla_param.decla_parameter(i).list_parameter() == null){
                                errorList.add(new Excepcion(ctx.IDEN().getSymbol().getLine(), ctx.IDEN().getSymbol().getCharPositionInLine(), "La declaracion para " + auxFuncion.listDecla_param.decla_parameter(i).IDEN().getText() + " no es un arreglo", Excepcion.TypeError.Semantico));
                                continue;
                            }
                            // Si la expresion produjo un arreglo, quiere decir que en EXPR hayo un identificador y de este se busco el simbolo
                            Simbolo auxSimbolo = ent_actual.getSimbolo(ctx.list_parameter().expr().get(i).getText() + TipoSimbolo.Variable.name());
                            if (auxSimbolo.tipo_entorno != TipoSimbolo.Arreglo){
                                errorList.add(new Excepcion(ctx.IDEN().getSymbol().getLine(), ctx.IDEN().getSymbol().getCharPositionInLine(), "La variable " + ctx.IDEN().getText() + " no es un arreglo", Excepcion.TypeError.Semantico));
                                continue;
                            }
                            // Comparar dimensiones
                            // Se ingresa a la pila momentaneamente el entorno de la funcion para que pueda buscar variables ya declaradas en ese entorno
                            pilaEnt.push(ent_func);
                            Object list_parameter = this.getDimInteger(auxFuncion.listDecla_param.decla_parameter(i).list_parameter(), ctx.IDEN().getSymbol().getLine(), ctx.IDEN().getSymbol().getCharPositionInLine());
                            pilaEnt.pop();

                            if (list_parameter instanceof Excepcion) {
                                errorList.add((Excepcion) list_parameter);
                                continue;
                            }
                            if (!auxSimbolo.dimensiones.equals(list_parameter)){
                                errorList.add(new Excepcion(ctx.IDEN().getSymbol().getLine(), ctx.IDEN().getSymbol().getCharPositionInLine(), "Los arreglo " + auxFuncion.listDecla_param.decla_parameter(i).IDEN().getText() + " y " + auxSimbolo.identificador + " no tienen las mismas dimensiones", Excepcion.TypeError.Semantico));
                                continue;
                            }
                            // Comparar tipo de datos
                            if (!(auxSimbolo.tipo_data.equalsIgnoreCase(tipo_datoDecla))){
                                errorList.add(new Excepcion(ctx.IDEN().getSymbol().getLine(), ctx.IDEN().getSymbol().getCharPositionInLine(), "Los arreglo " + auxFuncion.listDecla_param.decla_parameter(i).IDEN().getText() + " y " + auxSimbolo.identificador + " no son del mismo tipo de datos", Excepcion.TypeError.Semantico));
                                continue;
                            }

                            // Se hace una copia del arreglo para pasarlo por valor y no por referencia
                            ArrayList<Object> array = (ArrayList<Object>) exp;
                            ArrayList<Object> arrayCopy = new ArrayList<>();
                            // Validacion solo para arreglos de 2 dimension
                            if (auxFuncion.listDecla_param.decla_parameter(i).list_parameter().expr().size() > 1){

                                for (Object arr : array){
                                    ArrayList<Object>  dim1 = (ArrayList<Object>) arr;
                                    ArrayList<Object> copy = (ArrayList<Object>) dim1.clone();
                                    arrayCopy.add(copy);
                                }
                            } else {
                                // Si el arreglo es solo de 1 dimension, solo se copia
                                arrayCopy = (ArrayList<Object>) array.clone();
                            }

                            // Se crea un nuevo Simbolo con los datos del parametro arreglo a declarar y se guarda en el entorno de la funcion
                            Simbolo paramArreglo = new Simbolo(auxFuncion.list_param.get(i).identificador, tipo_datoDecla, arrayCopy, (ArrayList<Integer>) list_parameter, TipoSimbolo.Arreglo);

                            // Se almacena en el entorno de la Funcion el Simbolo del nuevo parametro ya como una variante
                            ent_func.addSimbolo(auxFuncion.list_param.get(i).identificador + TipoSimbolo.Variable.name(), paramArreglo);

                            continue;
                        }

                        // Se crea un nuevo Simbolo con los datos del parametro a declarar y se guarda en el entorno de la funcion
                        Simbolo nuevo = new Simbolo(auxFuncion.list_param.get(i).identificador, tipo_datoDecla, exp, TipoSimbolo.Parametros, auxFuncion.list_param.get(i).pos_inStack);

                        // Se almacena en el entorno de la Funcion el Simbolo del nuevo parametro ya como una variante
                        ent_func.addSimbolo(auxFuncion.list_param.get(i).identificador + TipoSimbolo.Variable.name(), nuevo);
                    } else {
                        errorList.add(new Excepcion(ctx.IDEN().getSymbol().getLine(), ctx.IDEN().getSymbol().getCharPositionInLine(), "Parametro " + auxFuncion.list_param.get(i).identificador + " no declarado en Funcion", Excepcion.TypeError.Semantico));
                    }
                }
                /**Generado para C3D Se actualiza lastPosicion, pues se traian parametros que aun no se habian ingresado al entorno**/
                ent_func.lastPosicion = auxFuncion.list_param.size();
            } else return new Excepcion(ctx.IDEN().getSymbol().getLine(), ctx.IDEN().getSymbol().getCharPositionInLine(), "Cantidad de parametros en Funcion " + ctx.IDEN().getText() + " no coinciden", Excepcion.TypeError.Semantico);

        } else {
            // Se verifica que los parametros de la Funcion sean igual a 0, pues en la llamada no hay parametros
            if (auxFuncion.list_param.size() != 0 || auxFuncion.listDecla_param.getChildCount() != 0){
                return new Excepcion(ctx.IDEN().getSymbol().getLine(), ctx.IDEN().getSymbol().getCharPositionInLine(), "Cantidad de parametros en Funcion " + ctx.IDEN().getText() + " no coinciden", Excepcion.TypeError.Semantico);
            }
        }

        // Se ingresa el Entorno de la Funcion a la pila para que se pueda usar mientras valida parametros
        pilaEnt.push(ent_func);
        // Si existen o no existen parametros, se ejecutan las Sentencias dentro de la Funcion
        for (GramaticaParser.SentencesContext stcs : auxFuncion.list_sentences){

            // Aca se terminan de almacenar las Excepciones de sentencias
            Object sentence = visitSentences(stcs);
            if (sentence instanceof Excepcion) errorList.add((Excepcion) sentence);
            // Implementacion del control de ciclos EXIT, se guarda el error y se siguen ejecutando las sentencias
            if (sentence instanceof Exit) errorList.add(new Excepcion(((Exit) sentence).fila, ((Exit) sentence).columna, "Implementar Control Exit solo en ciclos", Excepcion.TypeError.Semantico));
            // Implementacion del control de ciclos CYCLE, se guarda el error y se siguen ejecutando las sentencias
            if (sentence instanceof Cycle) errorList.add(new Excepcion(((Cycle) sentence).fila, ((Cycle) sentence).columna, "Implementar Control Cycle solo en ciclos", Excepcion.TypeError.Semantico));
        }
        // Cuando se hayan ejecutado las sentencias la Funcion se elimina su entorno que esta en la pila, para que no quede almacenado.
        pilaEnt.pop();

        /*
        * VERIFICACIONES PARA PODER RETORNAR EL VALOR DE LA FUNCION
        * Simbolo del retorno en el Entorno de la Funcion
        * */
        Simbolo simRetorno = ent_func.getSimbolo(auxFuncion.return_name + TipoSimbolo.Variable.name());

        /*
        * Simbolo de la variable declarada en el Entorno padre con el nombre de la llamada a la funcion
        * *NOTA* Ya se verifico que si este declarado, ver al inicio de VisitFuncexpr
        * */
        Simbolo simDeclaFuncion = ent_actual.getSimbolo(ctx.IDEN().getText() + TipoSimbolo.Variable.name());

        // VERIFICAR QUE EL TIPO_DATA DEL RETORNO EN LA FUNCION SEA IGUAL AL TIPO_DATA DE LA DECLARACION DE LA LLAMADA EN EL ENTORNO PADRE
        if (simRetorno.tipo_data.equalsIgnoreCase(simDeclaFuncion.tipo_data)){
            // ESTE ES EL VALOR DE RETORNO DE LA FUNCION
            return simRetorno.valor;
        } else return new Excepcion(ctx.IDEN().getSymbol().getLine(), ctx.IDEN().getSymbol().getCharPositionInLine(), "La Funcion " + ctx.IDEN().getText() + " tiene un tipo de dato diferente declarado al esperado", Excepcion.TypeError.Semantico);

    }

    public Object visitArrayexpr(GramaticaParser.ArrayexprContext ctx){
        // ida=IDEN '[' list_parameter ']'

        // *NOTA* Se restringe la cantidad de dimensiones, pues para este Proyecto solo se permiten de 2D
        if (ctx.list_parameter().expr().size() > 2)
            return new Excepcion(ctx.IDEN().getSymbol().getLine(), ctx.IDEN().getSymbol().getCharPositionInLine(), "Los arreglos como: " + ctx.IDEN().getText() + " solo pueden tener maximo 2 dimensiones", Excepcion.TypeError.Sintactico);

        Entorno ent_actual = pilaEnt.peek();
        Simbolo auxSimbolo = ent_actual.getSimbolo(ctx.IDEN().getText() + TipoSimbolo.Variable.name());

        if (auxSimbolo == null)
            return new Excepcion(ctx.IDEN().getSymbol().getLine(), ctx.IDEN().getSymbol().getCharPositionInLine(), "El arreglo " + ctx.IDEN().getText() + " no existe", Excepcion.TypeError.Semantico);

        if (auxSimbolo.tipo_entorno != TipoSimbolo.Arreglo)
            return new Excepcion(ctx.IDEN().getSymbol().getLine(), ctx.IDEN().getSymbol().getCharPositionInLine(), "El variable " + ctx.IDEN().getText() + " no es un arreglo", Excepcion.TypeError.Semantico);

        int fila = ctx.IDEN().getSymbol().getLine();
        int columna = ctx.IDEN().getSymbol().getCharPositionInLine();
        Object list_parameter = this.getDimInteger(ctx.list_parameter(), fila, columna);

        if (list_parameter instanceof Excepcion) return list_parameter;

        Object respond = this.searchDimension((ArrayList<Integer>) list_parameter, (ArrayList<Object>) auxSimbolo.valor, fila, columna);

        if (respond instanceof Excepcion) return respond;

        if (respond instanceof ArrayList<?>)
            return new Excepcion(ctx.IDEN().getSymbol().getLine(), ctx.IDEN().getSymbol().getCharPositionInLine(), "Exceso de dimensiones", Excepcion.TypeError.Semantico);

        return respond;
    }

    /**
     * searchDimension() : funcion que retorna un arreglo si las dimensiones no fueron correctas, o bien un valor almacenado en alguna posicion
     * La funcion es recursiva, el Array dimensiones por defecto se pasan por referencia
     * El Array aux_array en cada pasada envia una nueva dimension (arreglo), adentrandose segun el numero de posicion en cada dimension
     * Si todo resulta correcto, y se encuentra el dato en el ultimo arreglo, se empieza a retornar recursivamente
     * */
    private Object searchDimension(ArrayList<Integer> dimensiones, ArrayList<Object> aux_array, int fila, int columna){

        if (dimensiones.isEmpty()) return aux_array;

        if (!(aux_array instanceof ArrayList<Object>))
            return new Excepcion(fila, columna, "Dimensiones no establecidas", Excepcion.TypeError.Semantico);

        Object data_return;
        int dimension = dimensiones.remove(0);
        // Se le resta 1 a dimension por cuestion de que los arreglos en Fortran empiezan en 1 y no en 0
        // Pero java si permite la posicion en 0
        dimension = dimension -1;

        try {
            /*
            * Si ya no hay dimensiones, se retorna la ultima posicion obtenida en el arreglo
            * Esa ultima posicion puede ser un arreglo, null (si la posicion esta vacia) o el valor almacenado
            * Si se retorna un arreglo quiere decir que las dimensiones establecidas eran menores a las del arreglo declarado
            * */
            if (dimensiones.isEmpty()) return aux_array.get(dimension);

            // Esta validacion permite verificar que la siguiente dimension sea un arreglo antes de enviarse como parametro
            // Pues el parametro establecido recibe un Array, y si en la siguiente dimension ya se encuentra el dato, este no coincide con el parametro esperado
            if (!(aux_array.get(dimension) instanceof ArrayList<?>))
                return new Excepcion(fila, columna, "Arreglo de menor dimension", Excepcion.TypeError.Semantico);

            data_return = this.searchDimension(dimensiones, (ArrayList<Object>) aux_array.get(dimension), fila, columna);
        } catch (Exception e){
            return new Excepcion(fila, columna, "Dimension no establecida", Excepcion.TypeError.Semantico);
        }
        return data_return;
    }
}
