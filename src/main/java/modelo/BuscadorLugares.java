package modelo;

import java.util.*;

public class BuscadorLugares {

    // Letras con tilde o diacrítico y su equivalente sin él (misma posición en ambas cadenas).
    // Se usa una tabla en vez de java.text.Normalizer para que la búsqueda también
    // funcione en la versión para celular, donde Normalizer no está disponible.
    private static final String CON_TILDE = "ÁÀÂÄÃÅáàâäãåÉÈÊËéèêëÍÌÎÏíìîïÓÒÔÖÕóòôöõÚÙÛÜúùûüÑñÇçÝýÿ";
    private static final String SIN_TILDE = "AAAAAAaaaaaaEEEEeeeeIIIIiiiiOOOOOoooooUUUUuuuuNnCcYyy";

    /** Quita tildes/diacríticos para que la búsqueda no dependa de acentos. */
    public static String normalizar(String texto) {
        StringBuilder sb = new StringBuilder(texto.length());
        for (int i = 0; i < texto.length(); i++) {
            char ch = texto.charAt(i);
            int pos = CON_TILDE.indexOf(ch);
            sb.append(pos >= 0 ? SIN_TILDE.charAt(pos) : ch);
        }
        return sb.toString().toLowerCase().trim();
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
