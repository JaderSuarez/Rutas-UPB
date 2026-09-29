package movil;

import modelo.Camino;
import modelo.Edificio;
import modelo.Lugar;
import modelo.ServicioRutas.ResultadoRuta;
import org.teavm.jso.browser.Window;
import org.teavm.jso.dom.html.HTMLElement;

import java.util.List;
import java.util.function.Consumer;

/**
 * Pestaña Mapa: como vista.PanelMapaDoble en escritorio, con dos vistas del campus
 * ("Vista Mapa", la ilustración, y "Vista Grafo", la representación técnica),
 * la ruta calculada y la ficha del edificio o camino tocado.
 */
final class PantallaMapa {

    private final AppMovil app;
    private final HTMLElement elemento = Dom.el("div", "pantalla-mapa");
    private final HTMLElement lienzo = Dom.el("div", "mapa-lienzo-contenedor");
    private final HTMLElement barra = Dom.el("div", "mapa-barra");
    private final HTMLElement selector = Dom.el("div", "selector-vista");
    private final HTMLElement opcionesGrafo = Dom.el("div", "opciones-grafo");
    private final HTMLElement resumen = Dom.el("div", "mapa-resumen");
    private final HTMLElement ficha = Dom.el("div", "ficha");
    private final HTMLElement avisoUbicar = Dom.el("div", "mapa-ubicar");
    private boolean vistaGrafo;
    private Consumer<int[]> alUbicar;
    private int[] puntoElegido;

    PantallaMapa(AppMovil app) {
        this.app = app;
        elemento.appendChild(lienzo);

        barra.appendChild(selector);
        barra.appendChild(opcionesGrafo);
        barra.appendChild(resumen);
        elemento.appendChild(barra);

        HTMLElement controles = Dom.el("div", "mapa-controles");
        controles.appendChild(Dom.boton("+", "btn-mapa", () -> actual().acercar(1.4)));
        controles.appendChild(Dom.boton("−", "btn-mapa", () -> actual().acercar(1 / 1.4)));
        controles.appendChild(Dom.boton("⤢", "btn-mapa", this::restablecer));
        elemento.appendChild(controles);
        elemento.appendChild(ficha);
        elemento.appendChild(avisoUbicar);

        app.mapa.setAlTocarEdificio(this::mostrarFicha);
        app.grafo.setAlTocarEdificio(this::mostrarFicha);
        app.grafo.setAlTocarCamino(this::mostrarFichaCamino);
    }

    HTMLElement getElemento() {
        return elemento;
    }

    private LienzoTactil actual() {
        return vistaGrafo ? app.grafo : app.mapa;
    }

    /** Como "Restablecer" en escritorio: encuadra la ruta, o todo si no hay ruta. */
    private void restablecer() {
        if (vistaGrafo) {
            if (app.resultado != null) app.grafo.enfocarRuta(); else app.grafo.vistaGeneral();
        } else {
            if (app.resultado != null) app.mapa.enfocarRuta(); else app.mapa.vistaGeneral();
            app.mapa.redibujar();
        }
    }

    void alMostrar() {
        app.grafo.setSeleccion(app.origen, app.destino);
        mostrarVista(vistaGrafo && alUbicar == null);
    }

    private void mostrarVista(boolean grafo) {
        vistaGrafo = grafo;
        Dom.vaciar(lienzo);
        lienzo.appendChild(actual().getElemento());

        Dom.vaciar(selector);
        selector.appendChild(Dom.boton("Vista Mapa", "segmento" + (grafo ? "" : " activo"), () -> mostrarVista(false)));
        selector.appendChild(Dom.boton("Vista Grafo", "segmento" + (grafo ? " activo" : ""), () -> mostrarVista(true)));
        if (alUbicar != null) selector.getClassList().add("oculto"); else selector.getClassList().remove("oculto");

        Dom.vaciar(opcionesGrafo);
        if (grafo) {
            opcionesGrafo.appendChild(interruptor("Distancias", app.grafo.isMostrarDistancias(),
                    v -> app.grafo.setMostrarDistancias(v)));
            opcionesGrafo.appendChild(interruptor("Resaltar conexiones", app.grafo.isResaltarConexiones(),
                    v -> app.grafo.setResaltarConexiones(v)));
            opcionesGrafo.getClassList().remove("oculto");
        } else {
            opcionesGrafo.getClassList().add("oculto");
        }
        actualizarResumen();
        mostrarFicha(null);
        // El tamaño del lienzo se conoce cuando ya está en pantalla
        Window.setTimeout(() -> {
            actual().setMargenSuperior(barra.getOffsetHeight() + 16);
            actual().activar();
        }, 0);
    }

    private HTMLElement interruptor(String texto, boolean activo, Consumer<Boolean> alCambiar) {
        HTMLElement chip = Dom.boton(texto, "chip-opcion" + (activo ? " activo" : ""), () -> { });
        chip.listenClick(e -> {
            boolean nuevo = !chip.getClassList().contains("activo");
            if (nuevo) chip.getClassList().add("activo"); else chip.getClassList().remove("activo");
            alCambiar.accept(nuevo);
        });
        return chip;
    }

    /** Vuelve a dibujar tras cambios de ruta o del grafo. */
    void refrescar() {
        app.grafo.setSeleccion(app.origen, app.destino);
        app.grafo.grafoCambiado();
        app.mapa.redibujar();
    }

    private void actualizarResumen() {
        Window.setTimeout(() -> actual().setMargenSuperior(barra.getOffsetHeight() + 16), 0);
        Dom.vaciar(resumen);
        ResultadoRuta r = app.resultado;
        if (r == null || alUbicar != null) {
            resumen.getClassList().add("oculto");
            return;
        }
        resumen.getClassList().remove("oculto");
        List<String> c = r.getCaminoEdificios();
        HTMLElement texto = Dom.el("div", null);
        texto.appendChild(Dom.texto("strong", null, MapaCampus.etiquetaCorta(c.get(0)) + " → "
                + MapaCampus.etiquetaCorta(c.get(c.size() - 1))));
        texto.appendChild(Dom.texto("span", null, String.format("  %.0f m · ", r.getDistanciaTotal())
                + r.getTiempoEstimadoFormateado()));
        resumen.appendChild(texto);
        resumen.appendChild(Dom.boton("Quitar", "btn btn-texto btn-chico", () -> {
            app.resultado = null;
            app.mapa.setRuta(null);
            app.grafo.setRuta(null);
            actualizarResumen();
        }));
    }

    private void mostrarFicha(String id) {
        app.mapa.setSeleccionado(id);
        if (id == null) app.grafo.limpiarSeleccion();
        Dom.vaciar(ficha);
        if (id == null || alUbicar != null) {
            ficha.getClassList().remove("visible");
            return;
        }
        Edificio e = app.controlador.getGrafo().getEdificios().get(id);
        HTMLElement cabeza = Dom.el("div", "ficha-cabeza");
        cabeza.appendChild(Dom.texto("span", "ficha-pin", MapaCampus.etiquetaCorta(id)));
        HTMLElement titulos = Dom.el("div", "ficha-titulos");
        titulos.appendChild(Dom.texto("h3", null, e == null ? id : e.getNombre()));
        int conexiones = app.controlador.getGrafo().getAdyacentes(id).size();
        int lugares = e == null ? 0 : e.getLugares().size();
        titulos.appendChild(Dom.texto("small", "texto-suave", conexiones + (conexiones == 1 ? " conexión" : " conexiones")
                + "  ·  " + lugares + (lugares == 1 ? " lugar" : " lugares")));
        cabeza.appendChild(titulos);
        cabeza.appendChild(Dom.boton("✕", "btn-icono btn-cerrar", () -> mostrarFicha(null)));
        ficha.appendChild(cabeza);

        if (e != null && !e.getLugares().isEmpty()) {
            HTMLElement lista = Dom.el("ul", "ficha-lugares");
            for (Lugar l : e.getLugares()) {
                HTMLElement li = Dom.el("li", null);
                li.appendChild(Dom.texto("span", null, l.getNombre()));
                li.appendChild(Dom.texto("small", null, l.getCategoria()));
                lista.appendChild(li);
            }
            ficha.appendChild(lista);
        }
        HTMLElement acciones = Dom.el("div", "ficha-acciones");
        acciones.appendChild(Dom.boton("Salir de aquí", "btn btn-contorno", () -> app.usarComo(id, true)));
        acciones.appendChild(Dom.boton("Ir aquí", "btn btn-primario", () -> app.usarComo(id, false)));
        ficha.appendChild(acciones);
        ficha.getClassList().add("visible");
    }

    /** Ficha de un camino tocado en la vista de grafo (en escritorio aparece al pasar el mouse). */
    private void mostrarFichaCamino(Camino c) {
        Dom.vaciar(ficha);
        HTMLElement cabeza = Dom.el("div", "ficha-cabeza");
        HTMLElement titulos = Dom.el("div", "ficha-titulos");
        titulos.appendChild(Dom.texto("h3", null, MapaCampus.nombre(app.controlador, c.getOrigenId())
                + "  –  " + MapaCampus.nombre(app.controlador, c.getDestinoId())));
        titulos.appendChild(Dom.texto("small", "texto-suave", String.format("%.0f m", c.getDistancia())
                + "  ·  " + (c.isTieneEscaleras() ? "con escaleras" : "sin escaleras")
                + "  ·  " + (c.isBloqueado() ? "bloqueado" : "disponible")));
        cabeza.appendChild(titulos);
        cabeza.appendChild(Dom.boton("✕", "btn-icono btn-cerrar", () -> mostrarFicha(null)));
        ficha.appendChild(cabeza);
        ficha.getClassList().add("visible");
    }

    // ==================== Modo ubicar (panel de administración) ====================

    void iniciarUbicacion(Consumer<int[]> alElegir, Runnable alCancelar) {
        alUbicar = alElegir;
        puntoElegido = null;
        mostrarVista(false);   // el punto se ubica sobre el mapa ilustrado

        Dom.vaciar(avisoUbicar);
        avisoUbicar.appendChild(Dom.texto("p", null, "Toca el mapa donde está el punto nuevo."));
        HTMLElement listo = Dom.boton("Usar esta ubicación", "btn btn-primario btn-chico", () -> {
            if (puntoElegido == null) {
                Dom.aviso("Primero toca un lugar del mapa.", false);
                return;
            }
            Consumer<int[]> accion = alUbicar;
            terminarUbicacion();
            accion.accept(puntoElegido);
        });
        HTMLElement acciones = Dom.el("div", "fila-botones");
        acciones.appendChild(Dom.boton("Cancelar", "btn btn-contorno btn-chico", () -> {
            terminarUbicacion();
            alCancelar.run();
        }));
        acciones.appendChild(listo);
        avisoUbicar.appendChild(acciones);
        avisoUbicar.getClassList().add("visible");

        app.mapa.pedirUbicacion(p -> puntoElegido = p);
    }

    /** Si la persona sale del mapa a mitad de ubicar un punto, se cancela ese modo. */
    void cancelarUbicacionSiActiva() {
        if (alUbicar != null) terminarUbicacion();
    }

    private void terminarUbicacion() {
        alUbicar = null;
        avisoUbicar.getClassList().remove("visible");
        selector.getClassList().remove("oculto");
        app.mapa.cancelarUbicacion();
    }
}
