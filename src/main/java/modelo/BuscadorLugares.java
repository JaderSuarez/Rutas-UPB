package modelo;

import java.text.Normalizer;
import java.util.*;

public class BuscadorLugares {

    /** Quita tildes/diacríticos para que la búsqueda no dependa de acentos. */
    private static String normalizar(String texto) {
        String sinTildes = Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return sinTildes.toLowerCase().trim();
    }

    /**
     * Busca lugares por nombre o categoría. La comparación se hace por
     * palabras: todas las palabras escritas deben aparecer en el texto, sin
     * importar el orden ni las palabras intermedias. Así "cafeteria k"
     * encuentra "Cafetería Edificio K", y "sauna" encuentra "Sauna".
     */
    public static Map<String, Set<String>> buscarLugar(GrafoCampus grafo, String termino) {
        Map<String, Set<String>> resultados = new HashMap<>();
        if (termino == null || termino.trim().isEmpty()) {
            return resultados;
        }

        String[] palabras = normalizar(termino).split("\\s+");

        for (Edificio edificio : grafo.getEdificios().values()) {
            for (Lugar lugar : edificio.getLugares()) {
                String nombre = normalizar(lugar.getNombre());
                String categoria = normalizar(lugar.getCategoria());
                // También se considera el identificador del edificio, para que
                // "cafeteria k" funcione incluso si el lugar no lleva la letra.
                String contexto = nombre + " " + categoria + " " + normalizar(edificio.getId());

                if (contieneTodas(contexto, palabras)) {
                    resultados.putIfAbsent(lugar.getNombre(), new HashSet<>());
                    resultados.get(lugar.getNombre()).add(edificio.getId());
                }
            }
        }
        return resultados;
    }

    private static boolean contieneTodas(String texto, String[] palabras) {
        for (String palabra : palabras) {
            if (!palabra.isEmpty() && !texto.contains(palabra)) {
                return false;
            }
        }
        return true;
    }
}
