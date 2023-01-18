package Entorno;

public class Excepcion {
    public int fila;
    public int columna;
    public String descripcion;
    public TypeError tipo;

    public enum TypeError{
        Lexico,
        Sintactico,
        Semantico
    }

    public Excepcion(int fila, int columna, String descripcion, TypeError tipo) {
        this.fila = fila;
        this.columna = columna;
        this.descripcion = descripcion;
        this.tipo = tipo;
    }

    @Override
    public String toString(){
        // Se remueve en cada descripcion de Excepcion el char <,> pues da problema en la etiqueta de la tabla en Graphviz
        String descripcion = this.descripcion.replace("<","");
        descripcion = descripcion.replace(">","");

        return "<TR><TD>" + this.fila + "</TD>" +
                "<TD>" + this.columna + "</TD>" +
                "<TD>" + descripcion + "</TD>" +
                "<TD>" + this.tipo + "</TD></TR>\n";
    }
}
