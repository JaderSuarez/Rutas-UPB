package modelo;

import excepcion.RutaNoEncontradaException;
import java.util.List;

public class ServicioRutas {

    private EstrategiaRuta estrategia;

    public ServicioRutas() {
        this.estrategia = new EstrategiaDijkstra();
    }

    public ServicioRutas(EstrategiaRuta estrategia) {
        this.estrategia = estrategia;
    }

    public void setEstrategia(EstrategiaRuta estrategia) {
        this.estrategia = estrategia;
    }

    public ResultadoRuta calcularRuta(GrafoCampus grafo, String idOrigen, String idDestino, boolean evitarEscaleras)
            throws RutaNoEncontradaException {
        return estrategia.calcularRuta(grafo, idOrigen, idDestino, evitarEscaleras);
    }

    public static class ResultadoRuta {
        private final List<String> caminoEdificios;
        private final List<Camino> tramos;
        private final double distanciaTotal;

        public ResultadoRuta(List<String> caminoEdificios, List<Camino> tramos, double distanciaTotal) {
            this.caminoEdificios = caminoEdificios;
            this.tramos = tramos;
            this.distanciaTotal = distanciaTotal;
        }

        public List<String> getCaminoEdificios() {
            return caminoEdificios;
        }

        /** Aristas (Camino) recorridas en orden, cada una con su distancia y si tiene escaleras. */
        public List<Camino> getTramos() {
            return tramos;
        }

        public double getDistanciaTotal() {
            return distanciaTotal;
        }

        /** Tiempo estimado en minutos (decimal), calculado tramo por tramo. */
        public double getTiempoEstimadoMinutos() {
            return EstimadorTiempo.calcularMinutos(tramos);
        }

        /** Tiempo estimado listo para mostrar en pantalla, ej. "3 min 24 s". */
        public String getTiempoEstimadoFormateado() {
            return EstimadorTiempo.formatear(getTiempoEstimadoMinutos());
        }

        /** true si al menos un tramo de la ruta tiene escaleras. */
        public boolean tieneAlgunTramoConEscaleras() {
            if (tramos == null) return false;
            for (Camino c : tramos) {
                if (c.isTieneEscaleras()) return true;
            }
            return false;
        }
    }
}