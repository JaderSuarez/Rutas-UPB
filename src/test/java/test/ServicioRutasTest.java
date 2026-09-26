package test;

import excepcion.RutaNoEncontradaException;
import modelo.GrafoCampus;
import modelo.ServicioRutas;
import modelo.ServicioRutas.ResultadoRuta;
import repositorio.CampusRepositorio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ServicioRutasTest {

    private GrafoCampus grafo;
    private ServicioRutas servicioRutas;

    @BeforeEach
    public void setUp() {
        grafo = CampusRepositorio.cargarGrafo();
        servicioRutas = new ServicioRutas();
    }

    @Test
    public void testRutaInexistente() {
        assertThrows(RutaNoEncontradaException.class, () -> {
            servicioRutas.calcularRuta(grafo, "EdificioInexistente", "A", false);
        });
    }

    @Test
    public void testRutaAccesible() throws RutaNoEncontradaException {
        ResultadoRuta resultado = servicioRutas.calcularRuta(grafo, "D", "J", true);
        assertNotNull(resultado);
        assertTrue(resultado.getDistanciaTotal() > 0);
        assertFalse(resultado.getCaminoEdificios().isEmpty());
    }

    @Test
    public void testBloqueoCaminoExcepcion() {
        grafo.bloquearCamino("M", "Porteria 2", true);
        grafo.bloquearCamino("M", "Porteria 1", true);
        grafo.bloquearCamino("M", "Templo", true); // M también se conecta con el Templo (130 m)

        assertThrows(RutaNoEncontradaException.class, () -> {
            servicioRutas.calcularRuta(grafo, "M", "A", false);
        });
    }
}