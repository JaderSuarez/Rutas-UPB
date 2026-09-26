package test;

import modelo.BuscadorLugares;
import modelo.GrafoCampus;
import repositorio.CampusRepositorio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class BuscadorLugaresTest {

    private GrafoCampus grafo;

    @BeforeEach
    public void setUp() {
        grafo = CampusRepositorio.cargarGrafo();
    }

    @Test
    public void testBusquedaVaciaNoDevuelveResultados() {
        assertTrue(BuscadorLugares.buscarLugar(grafo, "").isEmpty());
        assertTrue(BuscadorLugares.buscarLugar(grafo, "   ").isEmpty());
        assertTrue(BuscadorLugares.buscarLugar(grafo, null).isEmpty());
    }

    @Test
    public void testBusquedaSinTildesEncuentraConTildes() {
        // "cafeteria" sin tilde debe encontrar los lugares escritos "Cafetería"
        Map<String, Set<String>> resultados = BuscadorLugares.buscarLugar(grafo, "cafeteria");
        assertFalse(resultados.isEmpty(), "La búsqueda sin tildes debe encontrar resultados");
    }

    @Test
    public void testBusquedaEsInsensibleAMayusculas() {
        assertEquals(
                BuscadorLugares.buscarLugar(grafo, "BIBLIOTECA").keySet(),
                BuscadorLugares.buscarLugar(grafo, "biblioteca").keySet());
    }

    @Test
    public void testBusquedaPorVariasPalabrasEnCualquierOrden() {
        // Debe encontrar "Cafetería Edificio K" escribiendo solo "cafeteria k"
        Map<String, Set<String>> resultados = BuscadorLugares.buscarLugar(grafo, "cafeteria k");
        assertTrue(resultados.containsKey("Cafetería Edificio K"),
                "Debe encontrar la cafetería del edificio K con dos palabras");
    }

    @Test
    public void testResultadoIndicaElEdificioCorrecto() {
        Map<String, Set<String>> resultados = BuscadorLugares.buscarLugar(grafo, "Sauna");
        assertTrue(resultados.containsKey("Sauna"));
        assertTrue(resultados.get("Sauna").contains("M"),
                "El Sauna debe estar ubicado en el edificio M");
    }

    @Test
    public void testBusquedaSinCoincidenciasDevuelveVacio() {
        assertTrue(BuscadorLugares.buscarLugar(grafo, "piscina olimpica").isEmpty());
    }
}
