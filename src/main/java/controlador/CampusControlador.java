package controlador;

import excepcion.RutaNoEncontradaException;
import modelo.*;
import modelo.ServicioRutas.ResultadoRuta;
import persistencia.RepositorioEstado;
import persistencia.RepositorioUsuarios;
import repositorio.CampusRepositorio;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CampusControlador {

    private final GrafoCampus grafo;
    private final ServicioRutas servicioRutas;
    private final RepositorioUsuarios repositorioUsuarios;
    private final RepositorioEstado repositorioEstado;

    /** Usuario que tiene la sesión abierta (null si es invitado). */
    private Usuario usuarioActivo;

    public CampusControlador() {
        this.grafo = CampusRepositorio.cargarGrafo();
        this.servicioRutas = new ServicioRutas(new EstrategiaDijkstra());
        this.repositorioUsuarios = new RepositorioUsuarios();
        this.repositorioEstado = new RepositorioEstado();

        // Restaura los cambios guardados de ejecuciones anteriores:
        // edificios y caminos agregados, bloqueos vigentes y velocidades.
        this.repositorioEstado.aplicarA(this.grafo);
    }

    public GrafoCampus getGrafo() {
        return this.grafo;
    }

    // ==================== Rutas y lugares ====================
    public ResultadoRuta solicitarRuta(String origen, String destino, boolean evitarEscaleras)
            throws RutaNoEncontradaException {
        return servicioRutas.calcularRuta(grafo, origen, destino, evitarEscaleras);
    }

    public Map<String, Set<String>> solicitarBusquedaLugar(String termino) {
        return BuscadorLugares.buscarLugar(grafo, termino);
    }

    // ==================== Sesión y usuarios ====================
    public Usuario autenticar(String correo, String contrasena) {
        Usuario u = repositorioUsuarios.autenticar(correo, contrasena);
        if (u != null) usuarioActivo = u;
        return u;
    }

    public boolean registrarUsuario(String nombre, String correo, String contrasena, Usuario.Rol rol) {
        return repositorioUsuarios.registrar(new Usuario(nombre, correo, contrasena, rol));
    }

    public boolean existeCorreo(String correo) {
        return repositorioUsuarios.existeCorreo(correo);
    }

    public boolean cambiarContrasena(String correo, String actual, String nueva) {
        return repositorioUsuarios.cambiarContrasena(correo, actual, nueva);
    }

    public Usuario getUsuarioActivo() {
        return usuarioActivo;
    }

    public void iniciarSesionInvitado() {
        usuarioActivo = null;
    }

    public void cerrarSesion() {
        usuarioActivo = null;
    }

    public int cantidadUsuarios() {
        return repositorioUsuarios.cantidad();
    }

    public List<Usuario> listarUsuarios() {
        return repositorioUsuarios.listar();
    }

    /** Nombre a mostrar del usuario que realiza las acciones administrativas. */
    private String nombreUsuarioActual() {
        return usuarioActivo != null ? usuarioActivo.getCorreo() : "invitado";
    }

    // ==================== Bloqueos ====================
    public void solicitarBloqueo(String idOrigen, String idDestino, boolean bloquear) {
        grafo.bloquearCamino(idOrigen, idDestino, bloquear);
        repositorioEstado.registrarBloqueo(nombreUsuarioActual(), idOrigen, idDestino, bloquear);
        repositorioEstado.guardar();
    }

    /**
     * Consulta, sin aplicar cambios, qué edificios quedarían sin ninguna forma
     * de acceso si se bloqueara el tramo indicado.
     */
    public List<String> consultarAislamientoPorBloqueo(String idOrigen, String idDestino) {
        return grafo.edificiosQueQuedarianAislados(idOrigen, idDestino);
    }

    /**
     * Elimina definitivamente un camino del grafo (RF-05: "eliminar conexiones").
     * A diferencia del bloqueo, el tramo desaparece de la estructura y no
     * vuelve a aparecer al reiniciar la aplicación.
     */
    public void eliminarCamino(String idOrigen, String idDestino) {
        // Se consultan los datos del tramo antes de quitarlo, para poder
        // restaurarlo después tal como estaba.
        double distancia = 0;
        boolean escaleras = false;
        for (Camino c : grafo.getAdyacentes(idOrigen)) {
            if (c.getDestinoId().equals(idDestino)) {
                distancia = c.getDistancia();
                escaleras = c.isTieneEscaleras();
                break;
            }
        }

        grafo.eliminarCamino(idOrigen, idDestino);
        repositorioEstado.registrarEliminacionCamino(idOrigen, idDestino, distancia, escaleras);
        repositorioEstado.guardar();
    }

    /** Vuelve a crear un camino que había sido eliminado, con sus datos originales. */
    public void restaurarCamino(String idOrigen, String idDestino, double distancia, boolean escaleras) {
        grafo.agregarCamino(idOrigen, idDestino, distancia, escaleras);
        repositorioEstado.registrarRestauracionCamino(idOrigen, idDestino);
        repositorioEstado.guardar();
    }

    /** Caminos eliminados: {origen, destino, distancia, escaleras}. */
    public List<String[]> getCaminosEliminados() {
        return repositorioEstado.getCaminosEliminados();
    }

    /**
     * Elimina un edificio agregado desde la aplicación, junto con todos sus
     * caminos. Solo se permite con edificios personalizados, no con los que
     * vienen en el mapa original del campus.
     */
    public void eliminarEdificio(String id) {
        if (!grafo.existeEdificio(id)) {
            throw new IllegalArgumentException("El edificio \"" + id + "\" no existe.");
        }
        if (!esEdificioPersonalizado(id)) {
            throw new IllegalArgumentException(
                    "Solo se pueden eliminar los edificios agregados desde la aplicación.");
        }

        // Quitar primero todos sus caminos
        List<String> vecinos = new ArrayList<>();
        for (Camino c : grafo.getAdyacentes(id)) {
            vecinos.add(c.getDestinoId());
        }
        for (String vecino : vecinos) {
            grafo.eliminarCamino(id, vecino);
        }

        grafo.eliminarEdificio(id);
        repositorioEstado.registrarEliminacionEdificio(id);
        repositorioEstado.guardar();
    }

    /** true si el edificio fue agregado desde la aplicación (no viene del mapa base). */
    public boolean esEdificioPersonalizado(String id) {
        for (RepositorioEstado.EdificioPersonalizado e : repositorioEstado.getEdificiosPersonalizados()) {
            if (e.id.equals(id)) return true;
        }
        return false;
    }

    /** Edificios que quedarían incomunicados si se elimina el tramo indicado. */
    public List<String> consultarAislamientoPorEliminacion(String idOrigen, String idDestino) {
        return grafo.edificiosQueQuedarianAislados(idOrigen, idDestino);
    }

    public List<RegistroBloqueo> getHistorialBloqueos() {
        return repositorioEstado.getHistorial();
    }

    // ==================== Gestión del grafo (RF-08) ====================

    /** Una conexión que se creará junto con un edificio nuevo. */
    public static class ConexionNueva {
        public final String destino;
        public final double distancia;
        public final boolean tieneEscaleras;

        public ConexionNueva(String destino, double distancia, boolean tieneEscaleras) {
            this.destino = destino;
            this.distancia = distancia;
            this.tieneEscaleras = tieneEscaleras;
        }
    }

    /**
     * Agrega un edificio nuevo al campus con su ubicación en las dos vistas del
     * mapa, y lo conecta con uno o varios edificios existentes.
     *
     * La operación es todo o nada: primero se valida cada conexión por
     * separado (que el destino exista, que no se repita, que la distancia sea
     * válida) antes de tocar el grafo. Si aun así algo fallara a mitad de
     * camino, se deshace lo que ya se hubiera aplicado, de modo que el
     * edificio quede completo con todas sus conexiones o no llegue a existir.
     */
    public void agregarEdificio(String id, String nombre,
                                int xGrafo, int yGrafo, int xMapa, int yMapa,
                                List<ConexionNueva> conexiones) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("El identificador del edificio no puede estar vacío.");
        }
        if (conexiones == null || conexiones.isEmpty()) {
            throw new IllegalArgumentException("Agrega al menos una conexión con otro edificio.");
        }
        String idLimpio = id.trim();
        if (grafo.existeEdificio(idLimpio)) {
            throw new IllegalArgumentException("Ya existe un edificio con el identificador \"" + idLimpio + "\".");
        }

        // Validar todas las conexiones antes de crear nada: así, un solo dato
        // inválido se detecta sin dejar el edificio a medio construir.
        Set<String> destinosVistos = new java.util.HashSet<>();
        for (ConexionNueva c : conexiones) {
            if (c.destino == null || c.destino.trim().isEmpty()) {
                throw new IllegalArgumentException("Selecciona el edificio de cada conexión.");
            }
            if (c.destino.equals(idLimpio)) {
                throw new IllegalArgumentException("Un edificio no puede conectarse consigo mismo.");
            }
            if (!grafo.existeEdificio(c.destino)) {
                throw new IllegalArgumentException("El edificio \"" + c.destino + "\" no existe.");
            }
            if (!destinosVistos.add(c.destino)) {
                throw new IllegalArgumentException(
                        "Hay dos conexiones repetidas hacia \"" + c.destino + "\". Quita una de ellas.");
            }
            if (c.distancia <= 0) {
                throw new IllegalArgumentException("La distancia de cada conexión debe ser mayor que cero.");
            }
        }

        // Con todo validado, se aplica al grafo. Si algo inesperado fallara a
        // mitad de camino, se deshace exactamente lo que ya se hubiera creado.
        grafo.agregarEdificio(new Edificio(idLimpio, nombre == null || nombre.trim().isEmpty()
                ? "Edificio " + idLimpio : nombre.trim()));

        List<ConexionNueva> aplicadas = new ArrayList<>();
        try {
            for (ConexionNueva c : conexiones) {
                grafo.agregarCamino(idLimpio, c.destino, c.distancia, c.tieneEscaleras);
                aplicadas.add(c);
            }
        } catch (RuntimeException fallo) {
            for (ConexionNueva c : aplicadas) {
                try {
                    grafo.eliminarCamino(idLimpio, c.destino);
                } catch (RuntimeException ignorado) { /* ya no está: no hay nada que deshacer */ }
            }
            try {
                grafo.eliminarEdificio(idLimpio);
            } catch (RuntimeException ignorado) { /* no debería ocurrir, pero no debe ocultar el error real */ }
            throw fallo;
        }

        // Solo si el grafo quedó completo se registra el cambio en disco.
        repositorioEstado.registrarEdificio(new RepositorioEstado.EdificioPersonalizado(
                idLimpio, nombre, xGrafo, yGrafo, xMapa, yMapa));
        for (ConexionNueva c : conexiones) {
            repositorioEstado.registrarCamino(idLimpio, c.destino, c.distancia, c.tieneEscaleras);
        }
        repositorioEstado.guardar();
    }

    /** Conecta dos edificios que ya existen. */
    public void agregarCamino(String origen, String destino, double distancia, boolean tieneEscaleras) {
        grafo.agregarCamino(origen, destino, distancia, tieneEscaleras);
        repositorioEstado.registrarCamino(origen, destino, distancia, tieneEscaleras);
        repositorioEstado.guardar();
    }

    public List<RepositorioEstado.EdificioPersonalizado> getEdificiosPersonalizados() {
        return repositorioEstado.getEdificiosPersonalizados();
    }

    // ==================== Configuración ====================
    public void guardarVelocidades() {
        repositorioEstado.guardar();
    }

    public String getRutaArchivoEstado() {
        return repositorioEstado.getRutaArchivo();
    }

    public String getRutaArchivoUsuarios() {
        return repositorioUsuarios.getRutaArchivo();
    }
}
