package persistencia;

import modelo.Camino;
import modelo.EstimadorTiempo;
import modelo.GrafoCampus;
import modelo.RegistroBloqueo;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Guarda y recupera el estado configurable del sistema en datos/estado.txt:
 * los caminos bloqueados, las velocidades de caminata, los edificios y caminos
 * agregados por el administrador, y el historial de bloqueos.
 *
 * Formatos por línea:
 *   VELOCIDAD|plano|escaleras
 *   BLOQUEO|origen|destino
 *   EDIFICIO|id|nombre|xGrafo|yGrafo|xMapa|yMapa
 *   CAMINO|origen|destino|distancia|tieneEscaleras
 *   HISTORIAL|fechaISO|usuario|BLOQUEO/DESBLOQUEO|origen|destino
 */
public class RepositorioEstado {

    private static final String ARCHIVO = "estado.txt";
    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final int MAX_HISTORIAL = 200;

    private final ArchivoDatos archivo = new ArchivoDatos(ARCHIVO);
    private final List<RegistroBloqueo> historial = new ArrayList<>();

    /** Edificio agregado por el administrador, con su ubicación en las dos vistas. */
    public static class EdificioPersonalizado {
        public final String id;
        public final String nombre;
        public final int xGrafo, yGrafo, xMapa, yMapa;

        public EdificioPersonalizado(String id, String nombre,
                                     int xGrafo, int yGrafo, int xMapa, int yMapa) {
            this.id = id; this.nombre = nombre;
            this.xGrafo = xGrafo; this.yGrafo = yGrafo;
            this.xMapa = xMapa; this.yMapa = yMapa;
        }
    }

    private final List<EdificioPersonalizado> edificiosPersonalizados = new ArrayList<>();
    private final List<String[]> caminosPersonalizados = new ArrayList<>();
    private final Set<String> bloqueos = new HashSet<>();
    /** clave del tramo -> {origen, destino, distancia, escaleras} */
    private final Map<String, String[]> caminosEliminados = new java.util.LinkedHashMap<>();
    private double velocidadPlano = -1;
    private double velocidadEscaleras = -1;

    public RepositorioEstado() {
        cargarDesdeArchivo();
    }

    private void cargarDesdeArchivo() {
        for (String[] c : archivo.leer()) {
            if (c.length == 0) continue;
            switch (c[0]) {
                case "VELOCIDAD":
                    if (c.length >= 3) {
                        velocidadPlano = parsear(c[1], -1);
                        velocidadEscaleras = parsear(c[2], -1);
                    }
                    break;
                case "BLOQUEO":
                    if (c.length >= 3) bloqueos.add(clave(c[1], c[2]));
                    break;
                case "ELIMINADO":
                    if (c.length >= 5) {
                        caminosEliminados.put(clave(c[1], c[2]),
                                new String[]{c[1], c[2], c[3], c[4]});
                    } else if (c.length >= 3) {
                        // Formato antiguo, sin los datos del tramo
                        caminosEliminados.put(clave(c[1], c[2]),
                                new String[]{c[1], c[2], "1", "false"});
                    }
                    break;
                case "EDIFICIO":
                    if (c.length >= 7) {
                        edificiosPersonalizados.add(new EdificioPersonalizado(
                                c[1], c[2],
                                (int) parsear(c[3], 0), (int) parsear(c[4], 0),
                                (int) parsear(c[5], 0), (int) parsear(c[6], 0)));
                    }
                    break;
                case "CAMINO":
                    if (c.length >= 5) caminosPersonalizados.add(new String[]{c[1], c[2], c[3], c[4]});
                    break;
                case "HISTORIAL":
                    if (c.length >= 6) {
                        try {
                            historial.add(new RegistroBloqueo(
                                    LocalDateTime.parse(c[1], ISO), c[2],
                                    c[4], c[5], "BLOQUEO".equals(c[3])));
                        } catch (Exception ignored) { }
                    }
                    break;
                default:
                    break;
            }
        }
    }

    private double parsear(String texto, double porDefecto) {
        try { return Double.parseDouble(texto.trim()); }
        catch (Exception e) { return porDefecto; }
    }

    /** Clave sin orden: el grafo es no dirigido. */
    private String clave(String a, String b) {
        return (a.compareTo(b) <= 0) ? a + "||" + b : b + "||" + a;
    }

    // ---------------- Aplicar el estado guardado ----------------

    /**
     * Aplica sobre el grafo recién cargado los edificios y caminos agregados
     * por el administrador y los bloqueos que estaban vigentes.
     */
    public void aplicarA(GrafoCampus grafo) {
        // 1. Edificios personalizados
        for (EdificioPersonalizado e : edificiosPersonalizados) {
            if (!grafo.existeEdificio(e.id)) {
                grafo.agregarEdificio(new modelo.Edificio(e.id, e.nombre));
            }
        }
        // 2. Caminos personalizados
        for (String[] c : caminosPersonalizados) {
            try {
                if (!grafo.existeCamino(c[0], c[1])) {
                    grafo.agregarCamino(c[0], c[1], parsear(c[2], 1), Boolean.parseBoolean(c[3]));
                }
            } catch (IllegalArgumentException ignored) { }
        }
        // 3. Caminos eliminados por el administrador
        for (String[] datos : caminosEliminados.values()) {
            try {
                grafo.eliminarCamino(datos[0], datos[1]);
            } catch (IllegalArgumentException ignored) { }
        }

        // 4. Bloqueos vigentes
        for (String clave : bloqueos) {
            String[] partes = clave.split("\\|\\|");
            if (partes.length == 2) {
                try {
                    grafo.bloquearCamino(partes[0], partes[1], true);
                } catch (IllegalArgumentException ignored) { }
            }
        }
        // 5. Velocidades configuradas
        if (velocidadPlano > 0) EstimadorTiempo.setVelocidadPlano(velocidadPlano);
        if (velocidadEscaleras > 0) EstimadorTiempo.setVelocidadEscaleras(velocidadEscaleras);
    }

    // ---------------- Registrar cambios ----------------

    public void registrarBloqueo(String usuario, String origen, String destino, boolean bloquear) {
        if (bloquear) bloqueos.add(clave(origen, destino));
        else bloqueos.remove(clave(origen, destino));

        historial.add(new RegistroBloqueo(LocalDateTime.now(), usuario, origen, destino, bloquear));
        if (historial.size() > MAX_HISTORIAL) {
            historial.subList(0, historial.size() - MAX_HISTORIAL).clear();
        }
    }

    public void registrarEdificio(EdificioPersonalizado edificio) {
        edificiosPersonalizados.add(edificio);
    }

    public void registrarCamino(String origen, String destino, double distancia, boolean escaleras) {
        caminosPersonalizados.add(new String[]{origen, destino,
                String.valueOf(distancia), String.valueOf(escaleras)});
    }

    /**
     * Deja constancia de que un camino fue eliminado: se quita de los caminos
     * agregados (si venía de ahí) y de la lista de bloqueos, y se anota como
     * eliminado para no volver a crearlo al reiniciar.
     */
    /**
     * Deja constancia de que un camino fue eliminado, conservando su distancia
     * y si tenía escaleras, de modo que pueda restaurarse más adelante.
     */
    public void registrarEliminacionCamino(String origen, String destino,
                                           double distancia, boolean escaleras) {
        String clave = clave(origen, destino);
        bloqueos.remove(clave);
        caminosPersonalizados.removeIf(c -> clave(c[0], c[1]).equals(clave));
        caminosEliminados.put(clave, new String[]{origen, destino,
                String.valueOf(distancia), String.valueOf(escaleras)});
    }

    /** Quita el tramo de la lista de eliminados (al restaurarlo). */
    public void registrarRestauracionCamino(String origen, String destino) {
        caminosEliminados.remove(clave(origen, destino));
    }

    /** Datos de los caminos eliminados: {origen, destino, distancia, escaleras}. */
    public List<String[]> getCaminosEliminados() {
        return new ArrayList<>(caminosEliminados.values());
    }

    /** Elimina del estado guardado un edificio agregado y todo lo suyo. */
    public void registrarEliminacionEdificio(String id) {
        edificiosPersonalizados.removeIf(e -> e.id.equals(id));
        caminosPersonalizados.removeIf(c -> c[0].equals(id) || c[1].equals(id));
        bloqueos.removeIf(k -> {
            String[] p = k.split("\\|\\|");
            return p.length == 2 && (p[0].equals(id) || p[1].equals(id));
        });
        caminosEliminados.entrySet().removeIf(e ->
                e.getValue()[0].equals(id) || e.getValue()[1].equals(id));
    }

    /** Historial más reciente primero. */
    public List<RegistroBloqueo> getHistorial() {
        List<RegistroBloqueo> copia = new ArrayList<>(historial);
        java.util.Collections.reverse(copia);
        return copia;
    }

    public List<EdificioPersonalizado> getEdificiosPersonalizados() {
        return new ArrayList<>(edificiosPersonalizados);
    }

    // ---------------- Guardar ----------------

    public boolean guardar() {
        List<String[]> registros = new ArrayList<>();

        registros.add(new String[]{"VELOCIDAD",
                String.valueOf(EstimadorTiempo.getVelocidadPlano()),
                String.valueOf(EstimadorTiempo.getVelocidadEscaleras())});

        for (EdificioPersonalizado e : edificiosPersonalizados) {
            registros.add(new String[]{"EDIFICIO", e.id, e.nombre,
                    String.valueOf(e.xGrafo), String.valueOf(e.yGrafo),
                    String.valueOf(e.xMapa), String.valueOf(e.yMapa)});
        }
        for (String[] c : caminosPersonalizados) {
            registros.add(new String[]{"CAMINO", c[0], c[1], c[2], c[3]});
        }
        for (String clave : bloqueos) {
            String[] partes = clave.split("\\|\\|");
            if (partes.length == 2) {
                registros.add(new String[]{"BLOQUEO", partes[0], partes[1]});
            }
        }
        for (String[] datos : caminosEliminados.values()) {
            registros.add(new String[]{"ELIMINADO", datos[0], datos[1], datos[2], datos[3]});
        }
        for (RegistroBloqueo r : historial) {
            registros.add(new String[]{"HISTORIAL", r.getFecha().format(ISO), r.getUsuario(),
                    r.esBloqueo() ? "BLOQUEO" : "DESBLOQUEO", r.getOrigenId(), r.getDestinoId()});
        }

        return archivo.escribir(registros,
                "Estado del Sistema de Rutas Óptimas UPB\n"
              + "Este archivo se genera automáticamente: no es necesario editarlo a mano.\n"
              + "VELOCIDAD|plano|escaleras   ·   BLOQUEO|origen|destino\n"
              + "ELIMINADO|origen|destino (camino retirado del grafo)\n"
              + "EDIFICIO|id|nombre|xGrafo|yGrafo|xMapa|yMapa\n"
              + "CAMINO|origen|destino|distancia|escaleras\n"
              + "ELIMINADO|origen|destino|distancia|escaleras\n"
              + "HISTORIAL|fecha|usuario|acción|origen|destino");
    }

    public String getRutaArchivo() {
        return archivo.getRuta();
    }
}
