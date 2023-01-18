package Entorno;

import Gramatica.GramaticaParser;

import java.util.ArrayList;

public class Simbolo {
    public String tipo_data;
    public Object valor;
    public String identificador;
    public TipoSimbolo tipo_entorno;
    public ArrayList<Integer> dimensiones;
    /**Generado para C3D**/
    public int pos_inStack;
    public enum TipoSimbolo
    {
        Variable,
        Funcion,
        Subrutina,
        Parametros,
        Nativo,
        Arreglo,
        C3D
    }

    public Simbolo(String id, String type_data, Object value, TipoSimbolo type_environment, int pos_inStack) {
        this.identificador = id;
        this.tipo_data = type_data;
        this.valor = value;
        this.tipo_entorno = type_environment;
        this.pos_inStack = pos_inStack;
    }

    public Simbolo(String id, String type_data, Object value, ArrayList<Integer> dimentions, TipoSimbolo type_environment) {
        this.identificador = id;
        this.tipo_data = type_data;
        this.valor = value;
        this.dimensiones = dimentions;
        this.tipo_entorno = type_environment;
    }

    public Simbolo(TipoSimbolo type_symbol, Object temp_value, String type_data){
        this.identificador = "";
        this.tipo_entorno = type_symbol;
        this.valor = temp_value;
        this.tipo_data = type_data;
        this.pos_inStack = -1;
    }

    @Override
    public String toString(){

        return "<TR><TD>" + this.identificador + "</TD>" +
                "<TD>" + this.tipo_data + "</TD>" +
                "<TD>" + this.tipo_entorno + "</TD>" +
                "<TD>" + this.dimensiones + "</TD>" +
                "<TD>" + this.pos_inStack + "</TD></TR>\n";
    }
}
