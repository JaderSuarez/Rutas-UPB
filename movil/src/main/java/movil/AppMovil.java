package movil;

import controlador.CampusControlador;
import excepcion.RutaNoEncontradaException;
import modelo.ServicioRutas.ResultadoRuta;
import modelo.Usuario;
import org.teavm.jso.dom.html.HTMLElement;

import java.util.function.Consumer;

/**
 * Versión para celular de Rutas UPB.
 *
 * Está escrita en Java y se traduce a JavaScript con TeaVM, así que corre en el
 * navegador del celular sin instalar nada. Usa la misma lógica de la app de
 * escritorio (CampusControlador, GrafoCampus, Dijkstra, EstimadorTiempo,
 * BuscadorLugares, repositorios): solo cambia la interfaz, pensada para pantallas
 * táctiles y verticales.
 */
public class AppMovil {

    final CampusControlador controlador;
    private final HTMLElement raiz;

    private Usuario usuario;          // null si entró como invitado
    private HTMLElement contenido;
    private HTMLElement barraNavegacion;

    // Estado compartido entre pantallas
    String origen, destino;
    boolean evitarEscaleras;
    ResultadoRuta resultado;

    MapaCampus mapa;
    private PantallaRuta pantallaRuta;
    private PantallaBuscar pantallaBuscar;
    private PantallaMapa pantallaMapa;
    private PantallaAdmin pantallaAdmin;

    public static void main(String[] args) {
        PersistenciaWeb.restaurar();
        new AppMovil().mostrarAcceso();
    }

    AppMovil() {
        controlador = new CampusControlador();
        PersistenciaWeb.guardar();   // la primera vez crea la cuenta de administrador por defecto
        raiz = Dom.doc().getElementById("app");
        HTMLElement cargando = Dom.doc().getElementById("cargando");
        if (cargando != null) cargando.getParentNode().removeChild(cargando);
    }

    /** Guarda en el navegador los cambios hechos con el controlador. */
    void persistir() {
        PersistenciaWeb.guardar();
    }

    // ==================== Sesión ====================

    void mostrarAcceso() {
        Dom.vaciar(raiz);
        raiz.setClassName("app app-acceso");
        raiz.appendChild(new PantallaAcceso(this).getElemento());
    }

    /** Entra a la app. {@code u} es null para el modo invitado. */
    void entrar(Usuario u) {
        usuario = u;
        origen = null;
        destino = null;
        resultado = null;
        construirInterfaz();
        mostrar("ruta");
    }

    void cerrarSesion() {
        controlador.cerrarSesion();
        usuario = null;
        mostrarAcceso();
    }

    boolean esAdministrador() {
        return usuario != null && usuario.esAdministrador();
    }

    // ==================== Estructura y navegación ====================

    private void construirInterfaz() {
        Dom.vaciar(raiz);
        raiz.setClassName("app");

        HTMLElement cabecera = Dom.el("header", "cabecera");
        HTMLElement marca = Dom.el("div", "marca");
        marca.appendChild(Dom.texto("span", "marca-escudo", "UPB"));
        HTMLElement titulos = Dom.el("div", null);
        titulos.appendChild(Dom.texto("div", "marca-titulo", "Rutas UPB"));
        titulos.appendChild(Dom.texto("div", "marca-sub",
                usuario == null ? "Invitado"
                        : usuario.getNombre().equalsIgnoreCase(usuario.getRol().getEtiqueta()) ? usuario.getNombre()
                        : usuario.getNombre() + " · " + usuario.getRol().getEtiqueta()));
        marca.appendChild(titulos);
        cabecera.appendChild(marca);
        HTMLElement salir = Dom.boton("", "btn-icono", this::cerrarSesion);
        salir.appendChild(Dom.icono(Dom.ICONO_SALIR));
        salir.setAttribute("aria-label", "Cerrar sesión");
        cabecera.appendChild(salir);

        contenido = Dom.el("main", "contenido");
        barraNavegacion = Dom.el("nav", "navegacion");

        mapa = new MapaCampus(controlador);
        pantallaRuta = new PantallaRuta(this);
        pantallaBuscar = new PantallaBuscar(this);
        pantallaMapa = new PantallaMapa(this);
        pantallaAdmin = esAdministrador() ? new PantallaAdmin(this) : null;

        agregarPestana("ruta", "Ruta", Dom.ICONO_RUTA);
        agregarPestana("buscar", "Buscar", Dom.ICONO_BUSCAR);
        agregarPestana("mapa", "Mapa", Dom.ICONO_MAPA);
        if (pantallaAdmin != null) agregarPestana("admin", "Admin", Dom.ICONO_ADMIN);

        Dom.agregar(raiz, cabecera, contenido, barraNavegacion);
    }

    private void agregarPestana(String id, String titulo, String icono) {
        HTMLElement b = Dom.el("button", "pestana");
        b.setAttribute("type", "button");
        b.setAttribute("data-id", id);
        b.appendChild(Dom.icono(icono));
        b.appendChild(Dom.texto("span", null, titulo));
        b.listenClick(e -> mostrar(id));
        barraNavegacion.appendChild(b);
    }

    void mostrar(String pestana) {
        if (!"mapa".equals(pestana)) pantallaMapa.cancelarUbicacionSiActiva();
        for (int i = 0; i < barraNavegacion.getChildNodes().getLength(); i++) {
            HTMLElement b = (HTMLElement) barraNavegacion.getChildNodes().get(i);
            if (pestana.equals(b.getAttribute("data-id"))) b.getClassList().add("activa");
            else b.getClassList().remove("activa");
        }
        Dom.vaciar(contenido);
        contenido.setClassName("contenido" + ("mapa".equals(pestana) ? " contenido-mapa" : ""));
        switch (pestana) {
            case "buscar":
                contenido.appendChild(pantallaBuscar.getElemento());
                pantallaBuscar.alMostrar();
                break;
            case "mapa":
                contenido.appendChild(pantallaMapa.getElemento());
                pantallaMapa.alMostrar();
                break;
            case "admin":
                contenido.appendChild(pantallaAdmin.getElemento());
                pantallaAdmin.alMostrar();
                break;
            default:
                contenido.appendChild(pantallaRuta.getElemento());
                pantallaRuta.alMostrar();
        }
        contenido.setScrollTop(0);
    }

    // ==================== Rutas ====================

    /** Calcula la ruta con el controlador. Devuelve null si todo salió bien, o el mensaje de error. */
    String calcularRuta() {
        resultado = null;
        mapa.setRuta(null);
        if (origen == null || destino == null) return "Elige el punto de partida y el destino.";
        if (origen.equals(destino)) return "El origen y el destino son el mismo lugar.";
        try {
            resultado = controlador.solicitarRuta(origen, destino, evitarEscaleras);
            mapa.setRuta(resultado.getCaminoEdificios());
            return null;
        } catch (RutaNoEncontradaException e) {
            return evitarEscaleras
                    ? "No hay una ruta sin escaleras disponible. Prueba permitiendo escaleras."
                    : e.getMessage();
        }
    }

    void verRutaEnMapa() {
        mostrar("mapa");
        mapa.enfocarRuta();
    }

    /** Desde el mapa o la búsqueda: fija el origen o el destino y vuelve a la pestaña de ruta. */
    void usarComo(String id, boolean comoOrigen) {
        if (comoOrigen) origen = id; else destino = id;
        if (origen != null && destino != null && !origen.equals(destino)) calcularRuta();
        else { resultado = null; mapa.setRuta(null); }
        mostrar("ruta");
    }

    /** Tras un cambio del administrador (bloqueos, puntos nuevos): la ruta mostrada puede ya no ser válida. */
    void alCambiarGrafo() {
        persistir();
        if (resultado != null) calcularRuta();
        mapa.redibujar();
        pantallaRuta.recargarListas();
    }

    /** El panel de administración pide ubicar un punto: se abre el mapa en modo ubicar. */
    void pedirUbicacion(Consumer<int[]> alElegir) {
        mostrar("mapa");
        pantallaMapa.iniciarUbicacion(punto -> {
            alElegir.accept(punto);
            mostrar("admin");
        }, () -> mostrar("admin"));
    }
}
