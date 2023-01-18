package EntornoType;

import Entorno.Simbolo;
import Gramatica.GramaticaParser;

import java.util.ArrayList;
import java.util.List;

public class Funcion {
    public String nombre;
    public String return_name;
    public ArrayList<Simbolo> list_param;
    public List<GramaticaParser.SentencesContext> list_sentences;
    public GramaticaParser.List_decla_paramContext listDecla_param;

    public Funcion(String nombre, String return_name, ArrayList<Simbolo> list_param, List<GramaticaParser.SentencesContext> list_sentences, GramaticaParser.List_decla_paramContext listDecla_param) {
        this.nombre = nombre;
        this.return_name = return_name;
        this.list_param = list_param;
        this.list_sentences = list_sentences;
        this.listDecla_param = listDecla_param;
    }
}
