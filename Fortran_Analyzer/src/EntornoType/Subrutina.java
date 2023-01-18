package EntornoType;

import Entorno.Entorno;
import Entorno.Simbolo;
import Gramatica.GramaticaParser;

import java.util.ArrayList;
import java.util.List;

public class Subrutina {
    public String nombre;
    public ArrayList<Simbolo> list_param;
    public List<GramaticaParser.SentencesContext> list_sentences;
    public GramaticaParser.List_decla_paramContext listDecla_param;
    /**Generado para C3D**/
    public Entorno ent_actual;

    public Subrutina(String nombre, ArrayList<Simbolo> list_param, List<GramaticaParser.SentencesContext> list_sentences, GramaticaParser.List_decla_paramContext listDecla_param) {
        this.nombre = nombre;
        this.list_param = list_param;
        this.list_sentences = list_sentences;
        this.listDecla_param = listDecla_param;
    }
}
