package modelo;

import java.util.List;

/**
 * Calcula el tiempo estimado de una ruta usando velocidades de caminata
 * diferenciadas según el tipo de tramo (plano o con escaleras), en vez de
 * una única velocidad promedio aplicada a la distancia total.
 *
 * Las velocidades tienen valores por defecto razonables y son ajustables
 * en tiempo de ejecución (pensado para conectarse con la pestaña
 * "Configuración" del panel de administración).
 */
public class EstimadorTiempo {

    // Velocidades por defecto, en metros por minuto, ajustadas por el equipo
    // a partir de recorridos reales en el campus. Son más bajas que una
    // caminata en ciudad porque el terreno es montañoso y con desniveles.
    // Se pueden modificar desde la pestaña Configuración del administrador.
    private static double velocidadPlanoMPorMin = 23.0;
    private static double velocidadEscalerasMPorMin = 17.0;

    public static double getVelocidadPlano() {
        return velocidadPlanoMPorMin;
    }

    public static void setVelocidadPlano(double metrosPorMinuto) {
        if (metrosPorMinuto > 0) {
            velocidadPlanoMPorMin = metrosPorMinuto;
        }
    }

    public static double getVelocidadEscaleras() {
        return velocidadEscalerasMPorMin;
    }

    public static void setVelocidadEscaleras(double metrosPorMinuto) {
        if (metrosPorMinuto > 0) {
            velocidadEscalerasMPorMin = metrosPorMinuto;
        }
    }

    /** Restaura las velocidades a sus valores por defecto (23 m/min y 17 m/min). */
    public static void restaurarValoresPorDefecto() {
        velocidadPlanoMPorMin = 23.0;
        velocidadEscalerasMPorMin = 17.0;
    }

    /**
     * Suma el tiempo de cada tramo por separado, usando la velocidad que
     * corresponda según si ese tramo tiene escaleras o no.
     */
    public static double calcularMinutos(List<Camino> tramos) {
        if (tramos == null || tramos.isEmpty()) {
            return 0.0;
        }
        double minutos = 0.0;
        for (Camino tramo : tramos) {
            double velocidad = tramo.isTieneEscaleras() ? velocidadEscalerasMPorMin : velocidadPlanoMPorMin;
            minutos += tramo.getDistancia() / velocidad;
        }
        return minutos;
    }

    /** Formatea minutos decimales de forma legible, ej. "3 min 24 s" o "45 s". */
    public static String formatear(double minutosDecimales) {
        int totalSegundos = (int) Math.round(minutosDecimales * 60);
        int minutos = totalSegundos / 60;
        int segundos = totalSegundos % 60;
        if (minutos <= 0) {
            return segundos + " s";
        }
        return minutos + " min " + segundos + " s";
    }
}
