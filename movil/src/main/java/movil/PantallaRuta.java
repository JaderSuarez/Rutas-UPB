package movil;

import modelo.Camino;
import modelo.ServicioRutas.ResultadoRuta;
import org.teavm.jso.dom.html.HTMLElement;
import org.teavm.jso.dom.html.HTMLInputElement;
import org.teavm.jso.dom.html.HTMLSelectElement;

import java.util.List;

/** Elegir origen y destino, calcular la ruta más corta y ver el recorrido paso a paso. */
final class PantallaRuta {

    private final AppMovil app;
    private final HTMLElement elemento = Dom.el("div", "pantalla");
    private final HTMLSelectElement listaOrigen = Dom.lista("campo");
    private final HTMLSelectElement listaDestino = Dom.lista("campo");
    private final HTMLInputElement evitar = (HTMLInputElement) Dom.el("input", null);
    private final HTMLElement mensaje = Dom.el("p", "mensaje-error");
    private final HTMLElement zonaResultado = Dom.el("div", null);

    PantallaRuta(AppMovil app) {
        this.app = app;

        HTMLElement tarjeta = Dom.el("section", "tarjeta");
        tarjeta.appendChild(Dom.texto("h2", null, "¿A dónde vas?"));

        HTMLElement filas = Dom.el("div", "origen-destino");
        HTMLElement columna = Dom.el("div", "od-campos");
        columna.appendChild(Dom.grupo("Desde", listaOrigen));
        columna.appendChild(Dom.grupo("Hasta", listaDestino));
        filas.appendChild(columna);
        HTMLElement intercambiar = Dom.boton("", "btn-icono btn-intercambiar", () -> {
            String o = app.origen;
            app.origen = app.destino;
            app.destino = o;
            seleccionarActuales();
        });
        intercambiar.setAttribute("aria-label", "Intercambiar origen y destino");
        intercambiar.appendChild(Dom.icono(Dom.ICONO_CAMBIAR));
        filas.appendChild(intercambiar);
        tarjeta.appendChild(filas);

        evitar.setType("checkbox");
        HTMLElement interruptor = Dom.el("label", "interruptor");
        interruptor.appendChild(evitar);
        interruptor.appendChild(Dom.el("span", "interruptor-pista"));
        HTMLElement textoInt = Dom.el("span", "interruptor-texto");
        textoInt.appendChild(Dom.texto("strong", null, "Evitar escaleras"));
        textoInt.appendChild(Dom.texto("small", null, "Ruta accesible, solo caminos sin escaleras"));
        interruptor.appendChild(textoInt);
        tarjeta.appendChild(interruptor);

        tarjeta.appendChild(mensaje);
        tarjeta.appendChild(Dom.boton("Calcular ruta", "btn btn-primario btn-bloque", this::calcular));
        elemento.appendChild(tarjeta);
        elemento.appendChild(zonaResultado);

        listaOrigen.addEventListener("change", e -> app.origen = valor(listaOrigen));
        listaDestino.addEventListener("change", e -> app.destino = valor(listaDestino));
        evitar.addEventListener("change", e -> app.evitarEscaleras = evitar.isChecked());
        recargarListas();
    }

    HTMLElement getElemento() {
        return elemento;
    }

    void alMostrar() {
        seleccionarActuales();
        evitar.setChecked(app.evitarEscaleras);
        mensaje.setTextContent("");
        mostrarResultado();
    }

    /** Vuelve a llenar las listas (por ejemplo, tras agregar un punto nuevo). */
    void recargarListas() {
        for (HTMLSelectElement l : new HTMLSelectElement[]{listaOrigen, listaDestino}) {
            Dom.vaciar(l);
            Dom.opcion(l, "", "Selecciona un punto…");
            for (String id : app.controlador.getGrafo().getEdificios().keySet()) {
                Dom.opcion(l, id, MapaCampus.nombre(app.controlador, id));
            }
        }
        seleccionarActuales();
    }

    private void seleccionarActuales() {
        listaOrigen.setValue(app.origen == null ? "" : app.origen);
        listaDestino.setValue(app.destino == null ? "" : app.destino);
    }

    private static String valor(HTMLSelectElement l) {
        return l.getValue() == null || l.getValue().isEmpty() ? null : l.getValue();
    }

    private void calcular() {
        app.origen = valor(listaOrigen);
        app.destino = valor(listaDestino);
        app.evitarEscaleras = evitar.isChecked();
        String error = app.calcularRuta();
        mensaje.setTextContent(error == null ? "" : error);
        mostrarResultado();
        if (error == null) zonaResultado.scrollIntoView();
    }

    private void mostrarResultado() {
        Dom.vaciar(zonaResultado);
        ResultadoRuta r = app.resultado;
        if (r == null) return;

        HTMLElement tarjeta = Dom.el("section", "tarjeta resultado");
        List<String> camino = r.getCaminoEdificios();
        tarjeta.appendChild(Dom.texto("h2", null,
                MapaCampus.nombre(app.controlador, camino.get(0)) + "  →  "
                + MapaCampus.nombre(app.controlador, camino.get(camino.size() - 1))));

        HTMLElement datos = Dom.el("div", "cifras");
        datos.appendChild(cifra(String.format("%.0f m", r.getDistanciaTotal()), "Distancia"));
        datos.appendChild(cifra(r.getTiempoEstimadoFormateado(), "Tiempo a pie"));
        datos.appendChild(cifra(String.valueOf(r.getTramos().size()), r.getTramos().size() == 1 ? "Tramo" : "Tramos"));
        tarjeta.appendChild(datos);

        tarjeta.appendChild(Dom.texto("p", r.tieneAlgunTramoConEscaleras() ? "etiqueta etiqueta-aviso" : "etiqueta etiqueta-ok",
                r.tieneAlgunTramoConEscaleras() ? "Incluye tramos con escaleras" : "Ruta sin escaleras"));

        HTMLElement pasos = Dom.el("ol", "pasos");
        for (Camino c : r.getTramos()) {
            HTMLElement paso = Dom.el("li", null);
            paso.appendChild(Dom.texto("span", "paso-tramo",
                    MapaCampus.nombre(app.controlador, c.getOrigenId()) + " → "
                    + MapaCampus.nombre(app.controlador, c.getDestinoId())));
            paso.appendChild(Dom.texto("span", "paso-detalle", String.format("%.0f m", c.getDistancia())
                    + (c.isTieneEscaleras() ? " · con escaleras" : " · plano")));
            pasos.appendChild(paso);
        }
        tarjeta.appendChild(pasos);
        tarjeta.appendChild(Dom.boton("Ver en el mapa", "btn btn-dorado btn-bloque", app::verRutaEnMapa));
        zonaResultado.appendChild(tarjeta);
    }

    private static HTMLElement cifra(String valor, String etiqueta) {
        HTMLElement c = Dom.el("div", "cifra");
        c.appendChild(Dom.texto("strong", null, valor));
        c.appendChild(Dom.texto("span", null, etiqueta));
        return c;
    }
}
