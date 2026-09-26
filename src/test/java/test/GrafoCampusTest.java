package test;

import modelo.Camino;
import modelo.GrafoCampus;
import modelo.Edificio;
import repositorio.CampusRepositorio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class GrafoCampusTest {

    private GrafoCampus grafo;

    @BeforeEach
    public void setUp() {
        grafo = CampusRepositorio.cargarGrafo();
    }

    // ---------- Validaciones ----------
    @Test
    public void testNoPermiteCaminoHaciaEdificioInexistente() {
        assertThrows(IllegalArgumentException.class,
                () -> grafo.agregarCamino("A", "EDIFICIO_QUE_NO_EXISTE", 20, false));
    }

    @Test
    public void testNoPermiteCaminoDuplicado() {
        // A - B ya existe en el grafo del campus
        assertThrows(IllegalArgumentException.class,
                () -> grafo.agregarCamino("A", "B", 14, false));
    }

    @Test
    public void testNoPermiteCaminoDeUnEdificioHaciaSiMismo() {
        assertThrows(IllegalArgumentException.class,
                () -> grafo.agregarCamino("A", "A", 10, false));
    }

    @Test
    public void testNoPermiteDistanciaInvalida() {
        GrafoCampus g = new GrafoCampus();
        g.agregarEdificio(new Edificio("X", "Edificio X"));
        g.agregarEdificio(new Edificio("Y", "Edificio Y"));
        assertThrows(IllegalArgumentException.class, () -> g.agregarCamino("X", "Y", 0, false));
        assertThrows(IllegalArgumentException.class, () -> g.agregarCamino("X", "Y", -5, false));
    }

    @Test
    public void testNoPermiteBloquearTramoInexistente() {
        // M y L no están conectados directamente
        assertThrows(IllegalArgumentException.class,
                () -> grafo.bloquearCamino("M", "L", true));
    }

    // ---------- Estructura ----------
    @Test
    public void testCaminoSeRegistraEnLosDosSentidos() {
        GrafoCampus g = new GrafoCampus();
        g.agregarEdificio(new Edificio("X", "Edificio X"));
        g.agregarEdificio(new Edificio("Y", "Edificio Y"));
        g.agregarCamino("X", "Y", 30, true);

        assertTrue(g.existeCamino("X", "Y"));
        assertTrue(g.existeCamino("Y", "X"), "El grafo es no dirigido: debe existir en ambos sentidos");
    }

    // ---------- Bloqueos ----------
    @Test
    public void testBloqueoAfectaLosDosSentidos() {
        grafo.bloquearCamino("A", "B", true);

        assertTrue(estaBloqueado(grafo.getAdyacentes("A"), "B"));
        assertTrue(estaBloqueado(grafo.getAdyacentes("B"), "A"),
                "Al bloquear un tramo debe quedar bloqueado en ambos sentidos");
    }

    @Test
    public void testDesbloqueoRestauraElTramo() {
        grafo.bloquearCamino("A", "B", true);
        grafo.bloquearCamino("A", "B", false);

        assertFalse(estaBloqueado(grafo.getAdyacentes("A"), "B"));
        assertFalse(estaBloqueado(grafo.getAdyacentes("B"), "A"));
    }

    // ---------- Conectividad ----------
    @Test
    public void testTramoNoCriticoNoAislaNingunEdificio() {
        // D - C es una de varias conexiones: bloquearlo no debe aislar nada
        List<String> aislados = grafo.edificiosQueQuedarianAislados("D", "C");
        assertTrue(aislados.isEmpty(), "Un tramo con alternativas no debe aislar edificios");
    }

    @Test
    public void testDetectaEdificioQueQuedariaAislado() {
        // M tiene tres accesos: Portería 1, Portería 2 y Templo.
        // Bloqueando los dos primeros, bloquear el último deja a M incomunicado.
        grafo.bloquearCamino("M", "Porteria 2", true);
        grafo.bloquearCamino("M", "Templo", true);

        List<String> aislados = grafo.edificiosQueQuedarianAislados("M", "Porteria 1");
        assertTrue(aislados.contains("M"),
                "Debe advertir que el edificio M quedaría sin ninguna vía de acceso");
    }

    private boolean estaBloqueado(List<Camino> caminos, String destino) {
        for (Camino c : caminos) {
            if (c.getDestinoId().equals(destino)) return c.isBloqueado();
        }
        fail("No se encontró el camino hacia " + destino);
        return false;
    }
}
