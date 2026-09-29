package movil;

import org.junit.jupiter.api.Test;
import vista.PanelMapaIsometrico;

import java.awt.Point;
import java.awt.Rectangle;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Las coordenadas del mapa de la versión móvil son una copia de las calibradas en
 * la app de escritorio (vista.PanelMapaIsometrico). Si alguien recalibra el mapa de
 * escritorio, esta prueba falla para recordar actualizar también CoordenadasMapa.
 */
class CoordenadasMapaTest {

    @SuppressWarnings("unchecked")
    private static <T> T campo(Object objeto, Class<?> clase, String nombre) throws Exception {
        Field f = clase.getDeclaredField(nombre);
        f.setAccessible(true);
        return (T) f.get(objeto);
    }

    @Test
    void posicionesIgualesAlEscritorio() throws Exception {
        System.setProperty("java.awt.headless", "true");
        PanelMapaIsometrico panel = new PanelMapaIsometrico();
        Map<String, Point> escritorio = campo(panel, PanelMapaIsometrico.class, "posiciones");
        assertEquals(escritorio.keySet(), CoordenadasMapa.POSICIONES.keySet());
        for (Map.Entry<String, Point> e : escritorio.entrySet()) {
            int[] movil = CoordenadasMapa.POSICIONES.get(e.getKey());
            assertEquals(e.getValue().x, movil[0], e.getKey());
            assertEquals(e.getValue().y, movil[1], e.getKey());
        }
    }

    @Test
    void trazadosIgualesAlEscritorio() throws Exception {
        System.setProperty("java.awt.headless", "true");
        PanelMapaIsometrico panel = new PanelMapaIsometrico();
        Map<String, List<Point>> escritorio = campo(panel, PanelMapaIsometrico.class, "trazadosEspeciales");
        assertEquals(escritorio.keySet(), CoordenadasMapa.TRAZADOS.keySet());
        for (Map.Entry<String, List<Point>> e : escritorio.entrySet()) {
            List<int[]> movil = CoordenadasMapa.TRAZADOS.get(e.getKey());
            assertEquals(e.getValue().size(), movil.size(), e.getKey());
            for (int i = 0; i < movil.size(); i++) {
                assertEquals(e.getValue().get(i).x, movil.get(i)[0]);
                assertEquals(e.getValue().get(i).y, movil.get(i)[1]);
            }
        }
    }

    @Test
    void rotulosIgualesAlEscritorio() throws Exception {
        Map<String, Rectangle> escritorio = campo(null, PanelMapaIsometrico.class, "ROTULOS");
        for (Map.Entry<String, int[]> e : CoordenadasMapa.ROTULOS.entrySet()) {
            Rectangle r = escritorio.get(e.getKey());
            assertNotNull(r, e.getKey());
            assertArrayEquals(new int[]{r.x, r.y, r.width, r.height}, e.getValue(), e.getKey());
        }
    }
}
