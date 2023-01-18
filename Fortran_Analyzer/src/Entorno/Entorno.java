package Entorno;

import java.util.HashMap;
import java.util.Locale;

public class Entorno {
    public HashMap<String, Simbolo> tabla_actual;
    public Entorno padre;
    /**Generado para C3D**/
    public Entorno siguiente;
    /**Generado para C3D**/
    public int lastPosicion;

    public Entorno(Entorno previous_table) {
        this.padre = previous_table;
        tabla_actual = new HashMap<String, Simbolo>();
        /**Generado para C3D**/
        this.lastPosicion = 0;
    }

    public Object addSimbolo(String nombre, Simbolo nuevo) {

        if (tabla_actual.containsKey(nombre.toUpperCase())) return null;
        tabla_actual.put(nombre.toUpperCase(), nuevo);

        return true;
    }

    public Simbolo getSimbolo(String nombre) {

        for (Entorno ent = this; ent != null; ent = ent.padre) {

            if (ent.tabla_actual.containsKey(nombre.toUpperCase()))
                return ent.tabla_actual.get(nombre.toUpperCase());
        }
        return null;
    }

    public Boolean updateSimbolo(String nombre, String type_data, Object valor){

        for (Entorno ent = this; ent != null; ent = ent.padre) {

            if (ent.tabla_actual.containsKey(nombre.toUpperCase())){
                if (ent.tabla_actual.get(nombre.toUpperCase()).tipo_data.equals(type_data)){
                    ent.tabla_actual.get(nombre.toUpperCase()).valor = valor;

                    return true;
                }
                return false;
            }
        }
        return null;
    }

    /**
     * Suma los tamaños de la tabla_actual en cada Entorno
     * Generado para C3D**/
    public int getPreviousSizes(){
        int size = 0;
        for (Entorno ent = this.padre; ent != null; ent = ent.padre)
            size += ent.tabla_actual.size();

        return size;
    }
}