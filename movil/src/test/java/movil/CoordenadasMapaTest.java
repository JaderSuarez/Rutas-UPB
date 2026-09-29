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
    void posicionesDelGrafoIgualesAlEscritorio() throws Exception {
        System.setProperty("java.awt.headless", "true");
        vista.PanelMapa panel = new vista.PanelMapa(repositorio.CampusRepositorio.cargarGrafo());
        Map<String, Point> escritorio = campo(panel, vista.PanelMapa.class, "posicionesBase");
        assertEquals(escritorio.keySet(), CoordenadasGrafo.POSICIONES.keySet());
        for (Map.Entry<String, Point> e : escritorio.entrySet()) {
            int[] movil = CoordenadasGrafo.POSICIONES.get(e.getKey());
            assertEquals(e.getValue().x, movil[0], e.getKey());
            assertEquals(e.getValue().y, movil[1], e.getKey());
        }
    }

    @Test
    void conversionDelMapaAlGrafo() {
        for (Map.Entry<String, int[]> e : CoordenadasMapa.POSICIONES.entrySet()) {
            int[] m = e.getValue(), real = CoordenadasGrafo.POSICIONES.get(e.getKey());
            // Sobre un edificio conocido, cae exactamente en ese edificio del grafo
            assertArrayEquals(real, CoordenadasGrafo.desdeMapa(m[0], m[1]), e.getKey());
            // Muy cerca de él en el mapa, queda también cerca en el grafo
            int[] cerca = CoordenadasGrafo.desdeMapa(m[0] + 12, m[1] + 8);
            assertTrue(Math.hypot(cerca[0] - real[0], cerca[1] - real[1]) < 30, e.getKey());
        }
        // Entre C (806,612) y J (1058,599) en el mapa -> entre C (546,473) y J (684,473) en el grafo
        int[] medio = CoordenadasGrafo.desdeMapa(932, 605);
        assertTrue(medio[0] > 546 && medio[0] < 684 && Math.abs(medio[1] - 473) < 60, medio[0] + "," + medio[1]);
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
