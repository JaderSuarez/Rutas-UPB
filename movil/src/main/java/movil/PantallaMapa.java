package movil;

import modelo.Edificio;
import modelo.Lugar;
import modelo.ServicioRutas.ResultadoRuta;
import org.teavm.jso.browser.Window;
import org.teavm.jso.dom.html.HTMLElement;

import java.util.List;
import java.util.function.Consumer;

/** Mapa del campus a pantalla completa, con la ruta y la ficha del edificio tocado. */
final class PantallaMapa {

    private final AppMovil app;
    private final HTMLElement elemento = Dom.el("div", "pantalla-mapa");
    private final HTMLElement resumen = Dom.el("div", "mapa-resumen");
    private final HTMLElement ficha = Dom.el("div", "ficha");
    private final HTMLElement avisoUbicar = Dom.el("div", "mapa-ubicar");
    private Consumer<int[]> alUbicar;
    private int[] puntoElegido;

    PantallaMapa(AppMovil app) {
        this.app = app;
        elemento.appendChild(app.mapa.getElemento());

        HTMLElement controles = Dom.el("div", "mapa-controles");
        controles.appendChild(Dom.boton("+", "btn-mapa", () -> app.mapa.acercar(1.4)));
        controles.appendChild(Dom.boton("−", "btn-mapa", () -> app.mapa.acercar(1 / 1.4)));
        controles.appendChild(Dom.boton("⤢", "btn-mapa", () -> {
            if (app.resultado != null) app.mapa.enfocarRuta(); else app.mapa.vistaGeneral();
            app.mapa.redibujar();
        }));
        elemento.appendChild(controles);
        elemento.appendChild(resumen);
        elemento.appendChild(ficha);
        elemento.appendChild(avisoUbicar);

        app.mapa.setAlTocarEdificio(this::mostrarFicha);
    }

    HTMLElement getElemento() {
        return elemento;
    }

    void alMostrar() {
        actualizarResumen();
        mostrarFicha(null);
        // El tamaño del mapa se conoce cuando ya está en pantalla
        Window.setTimeout(app.mapa::activar, 0);
    }

    private void actualizarResumen() {
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
            actualizarResumen();
        }));
    }

    private void mostrarFicha(String id) {
        app.mapa.setSeleccionado(id);
        Dom.vaciar(ficha);
        if (id == null || alUbicar != null) {
            ficha.getClassList().remove("visible");
            return;
        }
        Edificio e = app.controlador.getGrafo().getEdificios().get(id);
        HTMLElement cabeza = Dom.el("div", "ficha-cabeza");
        cabeza.appendChild(Dom.texto("span", "ficha-pin", MapaCampus.etiquetaCorta(id)));
        cabeza.appendChild(Dom.texto("h3", null, e == null ? id : e.getNombre()));
        cabeza.appendChild(Dom.boton("✕", "btn-icono btn-cerrar", () -> mostrarFicha(null)));
        ficha.appendChild(cabeza);

        if (e != null && !e.getLugares().isEmpty()) {
            HTMLElement lugares = Dom.el("ul", "ficha-lugares");
            for (Lugar l : e.getLugares()) {
                HTMLElement li = Dom.el("li", null);
                li.appendChild(Dom.texto("span", null, l.getNombre()));
                li.appendChild(Dom.texto("small", null, l.getCategoria()));
                lugares.appendChild(li);
            }
            ficha.appendChild(lugares);
        }
        HTMLElement acciones = Dom.el("div", "ficha-acciones");
        acciones.appendChild(Dom.boton("Salir de aquí", "btn btn-contorno", () -> app.usarComo(id, true)));
        acciones.appendChild(Dom.boton("Ir aquí", "btn btn-primario", () -> app.usarComo(id, false)));
        ficha.appendChild(acciones);
        ficha.getClassList().add("visible");
    }

    // ==================== Modo ubicar (panel de administración) ====================

    void iniciarUbicacion(Consumer<int[]> alElegir, Runnable alCancelar) {
        alUbicar = alElegir;
        puntoElegido = null;
        actualizarResumen();
        mostrarFicha(null);

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
        app.mapa.cancelarUbicacion();
    }
}
