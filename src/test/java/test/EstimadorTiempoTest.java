package test;

import modelo.Camino;
import modelo.EstimadorTiempo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class EstimadorTiempoTest {

    @AfterEach
    public void restaurar() {
        // Las velocidades son configurables: se dejan como estaban para no
        // afectar a las demás pruebas.
        EstimadorTiempo.restaurarValoresPorDefecto();
    }

    @Test
    public void testRutaVaciaNoTomaTiempo() {
        assertEquals(0.0, EstimadorTiempo.calcularMinutos(null), 0.0001);
        assertEquals(0.0, EstimadorTiempo.calcularMinutos(new ArrayList<>()), 0.0001);
    }

    @Test
    public void testTramoEnPlanoUsaLaVelocidadDePlano() {
        EstimadorTiempo.setVelocidadPlano(30.0);
        List<Camino> tramos = List.of(new Camino("A", "B", 60.0, false));
        // 60 m a 30 m/min = 2 minutos
        assertEquals(2.0, EstimadorTiempo.calcularMinutos(tramos), 0.0001);
    }

    @Test
    public void testTramoConEscalerasEsMasLentoQueEnPlano() {
        List<Camino> plano = List.of(new Camino("A", "B", 100.0, false));
        List<Camino> escaleras = List.of(new Camino("A", "B", 100.0, true));

        assertTrue(EstimadorTiempo.calcularMinutos(escaleras) > EstimadorTiempo.calcularMinutos(plano),
                "A igual distancia, un tramo con escaleras debe tardar más");
    }

    @Test
    public void testSumaElTiempoDeCadaTramoPorSeparado() {
        EstimadorTiempo.setVelocidadPlano(20.0);
        EstimadorTiempo.setVelocidadEscaleras(10.0);

        List<Camino> tramos = List.of(
                new Camino("A", "B", 40.0, false),  // 40/20 = 2 min
                new Camino("B", "C", 30.0, true));  // 30/10 = 3 min

        assertEquals(5.0, EstimadorTiempo.calcularMinutos(tramos), 0.0001);
    }

    @Test
    public void testIgnoraVelocidadesInvalidas() {
        double original = EstimadorTiempo.getVelocidadPlano();
        EstimadorTiempo.setVelocidadPlano(0);
        EstimadorTiempo.setVelocidadPlano(-10);
        assertEquals(original, EstimadorTiempo.getVelocidadPlano(), 0.0001,
                "Una velocidad no positiva no debe aplicarse");
    }

    @Test
    public void testRestaurarValoresPorDefecto() {
        EstimadorTiempo.setVelocidadPlano(99.0);
        EstimadorTiempo.restaurarValoresPorDefecto();
        assertEquals(23.0, EstimadorTiempo.getVelocidadPlano(), 0.0001);
        assertEquals(17.0, EstimadorTiempo.getVelocidadEscaleras(), 0.0001);
    }

    // ---------- Formato ----------
    @Test
    public void testFormatoEnSegundosCuandoEsMenosDeUnMinuto() {
        assertEquals("30 s", EstimadorTiempo.formatear(0.5));
    }

    @Test
    public void testFormatoEnMinutosYSegundos() {
        assertEquals("2 min 30 s", EstimadorTiempo.formatear(2.5));
    }
}
