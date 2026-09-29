package movil;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Posición de cada edificio sobre la imagen del mapa (mapa_campus.jpg, 1679 x 937)
 * y los tramos que siguen el camino peatonal real en vez de una línea recta.
 *
 * Son los mismos valores calibrados de vista.PanelMapaIsometrico (la app de
 * escritorio). La prueba CoordenadasMapaTest verifica que sigan siendo iguales,
 * así que si se recalibra el mapa de escritorio, la prueba avisa.
 */
final class CoordenadasMapa {

    static final int ANCHO = 1679;
    static final int ALTO = 937;

    /** id del edificio -> {x, y} en la imagen. */
    static final Map<String, int[]> POSICIONES = new HashMap<>();
    /**
     * Puntos que en la imagen ya son un letrero (no un pin): id -> {x, y, ancho, alto}
     * del letrero. En vez de un pin se resalta el contorno del letrero, como en escritorio.
     */
    static final Map<String, int[]> ROTULOS = new HashMap<>();
    /** clave sin orden del tramo -> lista de puntos {x, y}. */
    static final Map<String, List<int[]>> TRAZADOS = new HashMap<>();

    static {
        POSICIONES.put("M", new int[]{333, 338});
        POSICIONES.put("K", new int[]{1102, 175});
        POSICIONES.put("L", new int[]{1474, 251});
        POSICIONES.put("I", new int[]{1307, 303});
        POSICIONES.put("H", new int[]{1401, 403});
        POSICIONES.put("E", new int[]{1221, 421});
        POSICIONES.put("F", new int[]{1297, 462});
        POSICIONES.put("G", new int[]{1362, 504});
        POSICIONES.put("D", new int[]{986, 485});
        POSICIONES.put("J", new int[]{1058, 599});
        POSICIONES.put("A", new int[]{593, 613});
        POSICIONES.put("B", new int[]{694, 622});
        POSICIONES.put("C", new int[]{806, 612});
        POSICIONES.put("Templo", new int[]{308, 634});
        POSICIONES.put("CAF", new int[]{775, 514});
        POSICIONES.put("Porteria 2", new int[]{673, 321});
        POSICIONES.put("Porteria 1", new int[]{704, 799});

        ROTULOS.put("CAF", new int[]{712, 488, 128, 55});
        ROTULOS.put("Porteria 2", new int[]{565, 300, 220, 50});
        ROTULOS.put("Porteria 1", new int[]{610, 765, 190, 48});

        TRAZADOS.put(clave("M", "Porteria 1"), Arrays.asList(
                new int[]{332, 339}, new int[]{205, 430}, new int[]{120, 560}, new int[]{95, 660},
                new int[]{110, 760}, new int[]{185, 838}, new int[]{340, 868}, new int[]{530, 852},
                new int[]{704, 799}));
        TRAZADOS.put(clave("K", "Porteria 2"), Arrays.asList(
                new int[]{1102, 176}, new int[]{1060, 300}, new int[]{980, 420}, new int[]{870, 430},
                new int[]{760, 390}, new int[]{690, 340}, new int[]{673, 321}));
        TRAZADOS.put(clave("M", "Templo"), Arrays.asList(
                new int[]{332, 339}, new int[]{250, 410}, new int[]{195, 500}, new int[]{180, 580},
                new int[]{215, 625}, new int[]{280, 640}, new int[]{309, 635}));
    }

    private CoordenadasMapa() { }

    /** Clave sin orden, para que el tramo sirva en los dos sentidos. */
    static String clave(String a, String b) {
        return (a.compareTo(b) <= 0) ? a + "||" + b : b + "||" + a;
    }
}
