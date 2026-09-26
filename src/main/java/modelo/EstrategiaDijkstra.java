package modelo;

import excepcion.RutaNoEncontradaException;
import modelo.ServicioRutas.ResultadoRuta;
import java.util.*;

public class EstrategiaDijkstra implements EstrategiaRuta {

    @Override
    public ResultadoRuta calcularRuta(GrafoCampus grafo, String idOrigen, String idDestino, boolean evitarEscaleras)
            throws RutaNoEncontradaException {

        if (!grafo.getEdificios().containsKey(idOrigen) || !grafo.getEdificios().containsKey(idDestino)) {
            throw new RutaNoEncontradaException("El edificio de origen o destino no existe en el campus.");
        }

        Map<String, Double> distancias = new HashMap<>();
        Map<String, String> predecesores = new HashMap<>();
        Map<String, Camino> caminoUsado = new HashMap<>();
        PriorityQueue<EdificioDistancia> colaPrioridad = new PriorityQueue<>(Comparator.comparingDouble(n -> n.distancia));

        for (String idEdificio : grafo.getEdificios().keySet()) {
            distancias.put(idEdificio, Double.MAX_VALUE);
        }

        distancias.put(idOrigen, 0.0);
        colaPrioridad.add(new EdificioDistancia(idOrigen, 0.0));

        while (!colaPrioridad.isEmpty()) {
            EdificioDistancia actual = colaPrioridad.poll();
            String u = actual.idEdificio;

            if (u.equals(idDestino)) break;

            if (actual.distancia > distancias.get(u)) continue;

            for (Camino camino : grafo.getAdyacentes(u)) {
                if (camino.isBloqueado()) continue;
                if (evitarEscaleras && camino.isTieneEscaleras()) continue;

                String v = camino.getDestinoId();
                double peso = camino.getDistancia();

                if (distancias.get(u) + peso < distancias.get(v)) {
                    distancias.put(v, distancias.get(u) + peso);
                    predecesores.put(v, u);
                    caminoUsado.put(v, camino);
                    colaPrioridad.add(new EdificioDistancia(v, distancias.get(v)));
                }
            }
        }

        if (distancias.get(idDestino) == Double.MAX_VALUE) {
            throw new RutaNoEncontradaException("No existe un camino accesible/disponible entre " + idOrigen + " y " + idDestino);
        }

        LinkedList<String> camino = new LinkedList<>();
        LinkedList<Camino> tramos = new LinkedList<>();
        String paso = idDestino;
        while (paso != null) {
            camino.addFirst(paso);
            Camino tramo = caminoUsado.get(paso);
            if (tramo != null) {
                tramos.addFirst(tramo);
            }
            paso = predecesores.get(paso);
        }

        return new ResultadoRuta(camino, tramos, distancias.get(idDestino));
    }

    private static class EdificioDistancia {
        String idEdificio;
        double distancia;

        EdificioDistancia(String idEdificio, double distancia) {
            this.idEdificio = idEdificio;
            this.distancia = distancia;
        }
    }
}