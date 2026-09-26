package modelo;

import java.util.*;

/**
 * Grafo no dirigido y ponderado del campus. Cada edificio es un vértice, y
 * cada arista un camino transitable con su distancia.
 */
public class GrafoCampus {
    private final Map<String, Edificio> edificios;
    private final Map<String, List<Camino>> adyacencias;

    public GrafoCampus() {
        this.edificios = new LinkedHashMap<>();
        this.adyacencias = new HashMap<>();
    }

    public void agregarEdificio(Edificio edificio) {
        if (edificio == null) {
            throw new IllegalArgumentException("No se puede agregar un edificio nulo al grafo.");
        }
        edificios.put(edificio.getId(), edificio);
        adyacencias.putIfAbsent(edificio.getId(), new ArrayList<>());
    }

    /**
     * Conecta dos edificios existentes. Valida los identificadores para que un
     * error de escritura se reporte con un mensaje claro en vez de terminar
     * en un NullPointerException.
     */
    public void agregarCamino(String origenId, String destinoId, double distancia, boolean tieneEscaleras) {
        validarExiste(origenId);
        validarExiste(destinoId);

        if (origenId.equals(destinoId)) {
            throw new IllegalArgumentException(
                    "Un camino no puede conectar un edificio consigo mismo: " + origenId);
        }
        if (distancia <= 0) {
            throw new IllegalArgumentException(
                    "La distancia del camino " + origenId + " - " + destinoId + " debe ser mayor que cero.");
        }
        if (existeCamino(origenId, destinoId)) {
            throw new IllegalArgumentException(
                    "El camino " + origenId + " - " + destinoId + " ya está registrado en el grafo.");
        }

        adyacencias.get(origenId).add(new Camino(origenId, destinoId, distancia, tieneEscaleras));
        adyacencias.get(destinoId).add(new Camino(destinoId, origenId, distancia, tieneEscaleras));
    }

    /** true si ya hay un camino registrado entre esos dos edificios. */
    public boolean existeCamino(String origenId, String destinoId) {
        List<Camino> lista = adyacencias.get(origenId);
        if (lista == null) return false;
        for (Camino c : lista) {
            if (c.getDestinoId().equals(destinoId)) return true;
        }
        return false;
    }

    public boolean existeEdificio(String id) {
        return id != null && edificios.containsKey(id);
    }

    private void validarExiste(String id) {
        if (!existeEdificio(id)) {
            throw new IllegalArgumentException(
                    "El edificio o punto \"" + id + "\" no existe en el grafo del campus.");
        }
    }

    public Map<String, Edificio> getEdificios() {
        return edificios;
    }

    public List<Camino> getAdyacentes(String idEdificio) {
        return adyacencias.getOrDefault(idEdificio, new ArrayList<>());
    }

    /**
     * Bloquea o desbloquea un camino en los dos sentidos.
     * Valida que el tramo exista antes de modificarlo.
     */
    public void bloquearCamino(String idOrigen, String idDestino, boolean bloquear) {
        validarExiste(idOrigen);
        validarExiste(idDestino);
        if (!existeCamino(idOrigen, idDestino)) {
            throw new IllegalArgumentException(
                    "No existe un camino entre \"" + idOrigen + "\" y \"" + idDestino + "\".");
        }

        for (Camino c : adyacencias.get(idOrigen)) {
            if (c.getDestinoId().equals(idDestino)) c.setBloqueado(bloquear);
        }
        for (Camino c : adyacencias.get(idDestino)) {
            if (c.getDestinoId().equals(idOrigen)) c.setBloqueado(bloquear);
        }
    }

    /**
     * Elimina por completo un camino del grafo, en los dos sentidos.
     * A diferencia de bloquearlo, el tramo deja de existir en la estructura.
     */
    public void eliminarCamino(String idOrigen, String idDestino) {
        validarExiste(idOrigen);
        validarExiste(idDestino);
        if (!existeCamino(idOrigen, idDestino)) {
            throw new IllegalArgumentException(
                    "No existe un camino entre \"" + idOrigen + "\" y \"" + idDestino + "\".");
        }
        adyacencias.get(idOrigen).removeIf(c -> c.getDestinoId().equals(idDestino));
        adyacencias.get(idDestino).removeIf(c -> c.getDestinoId().equals(idOrigen));
    }

    /**
     * Quita un edificio del grafo. Debe haberse desconectado previamente: si
     * aún conserva caminos, se rechaza la operación para no dejar aristas
     * huérfanas.
     */
    public void eliminarEdificio(String id) {
        validarExiste(id);
        if (!getAdyacentes(id).isEmpty()) {
            throw new IllegalArgumentException(
                    "El edificio \"" + id + "\" todavía tiene caminos conectados.");
        }
        edificios.remove(id);
        adyacencias.remove(id);
    }

    // ==================== Análisis de conectividad ====================

    /**
     * Edificios que quedarían incomunicados si se bloqueara o se eliminara el
     * camino indicado (en ambos casos deja de poder usarse, así que el efecto
     * sobre la conectividad es el mismo). Simula la operación sin aplicarla:
     * recorre el grafo desde un punto de referencia y devuelve los edificios a
     * los que ya no se podría llegar.
     *
     * @return lista de identificadores aislados (vacía si la operación es segura)
     */
    public List<String> edificiosQueQuedarianAislados(String idOrigen, String idDestino) {
        if (!existeCamino(idOrigen, idDestino)) {
            return new ArrayList<>();
        }

        // Punto de partida del recorrido: cualquier edificio distinto de los
        // dos extremos del tramo, para poder comprobar si siguen alcanzables.
        String raiz = null;
        for (String id : edificios.keySet()) {
            if (!id.equals(idOrigen) && !id.equals(idDestino)) {
                raiz = id;
                break;
            }
        }
        if (raiz == null) return new ArrayList<>();

        Set<String> alcanzables = recorrer(raiz, idOrigen, idDestino);

        List<String> aislados = new ArrayList<>();
        for (String id : edificios.keySet()) {
            if (!alcanzables.contains(id)) aislados.add(id);
        }
        return aislados;
    }

    /**
     * Recorrido en anchura sobre los caminos disponibles, ignorando además el
     * tramo (ignorarA - ignorarB) para simular su bloqueo o eliminación.
     */
    private Set<String> recorrer(String raiz, String ignorarA, String ignorarB) {
        Set<String> visitados = new HashSet<>();
        Deque<String> pendientes = new ArrayDeque<>();
        visitados.add(raiz);
        pendientes.add(raiz);

        while (!pendientes.isEmpty()) {
            String actual = pendientes.poll();
            for (Camino c : getAdyacentes(actual)) {
                if (c.isBloqueado()) continue;

                boolean esTramoIgnorado =
                        (actual.equals(ignorarA) && c.getDestinoId().equals(ignorarB)) ||
                        (actual.equals(ignorarB) && c.getDestinoId().equals(ignorarA));
                if (esTramoIgnorado) continue;

                String vecino = c.getDestinoId();
                if (visitados.add(vecino)) {
                    pendientes.add(vecino);
                }
            }
        }
        return visitados;
    }
}
