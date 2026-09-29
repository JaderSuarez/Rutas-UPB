package movil;

import org.teavm.jso.dom.html.HTMLDocument;
import org.teavm.jso.dom.html.HTMLElement;
import org.teavm.jso.dom.html.HTMLInputElement;
import org.teavm.jso.dom.html.HTMLSelectElement;
import org.teavm.jso.dom.xml.Node;

/**
 * Utilidades pequeñas para construir la interfaz con elementos HTML desde Java.
 * Toda la app móvil se arma con estos métodos, sin escribir JavaScript a mano.
 */
final class Dom {

    private Dom() { }

    static HTMLDocument doc() {
        return HTMLDocument.current();
    }

    /** Crea un elemento con una o varias clases CSS (separadas por espacio). */
    static HTMLElement el(String etiqueta, String clases) {
        HTMLElement e = doc().createElement(etiqueta);
        if (clases != null && !clases.isEmpty()) e.setClassName(clases);
        return e;
    }

    /** Crea un elemento con texto. */
    static HTMLElement texto(String etiqueta, String clases, String texto) {
        HTMLElement e = el(etiqueta, clases);
        e.setTextContent(texto);
        return e;
    }

    static HTMLElement boton(String texto, String clases, Runnable accion) {
        HTMLElement b = texto("button", clases, texto);
        b.setAttribute("type", "button");
        b.listenClick(ev -> accion.run());
        return b;
    }

    static HTMLInputElement campo(String tipo, String placeholder) {
        HTMLInputElement i = (HTMLInputElement) el("input", "campo");
        i.setType(tipo);
        if (placeholder != null) i.setPlaceholder(placeholder);
        return i;
    }

    static HTMLSelectElement lista(String clases) {
        return (HTMLSelectElement) el("select", clases);
    }

    static void opcion(HTMLSelectElement lista, String valor, String texto) {
        HTMLElement o = texto("option", null, texto);
        o.setAttribute("value", valor);
        lista.appendChild(o);
    }

    /** Etiqueta encima de un control, al estilo de un formulario móvil. */
    static HTMLElement grupo(String etiqueta, HTMLElement control) {
        HTMLElement g = el("label", "grupo");
        g.appendChild(texto("span", "grupo-etiqueta", etiqueta));
        g.appendChild(control);
        return g;
    }

    static void vaciar(Node n) {
        while (n.getFirstChild() != null) n.removeChild(n.getFirstChild());
    }

    static HTMLElement agregar(HTMLElement padre, HTMLElement... hijos) {
        for (HTMLElement h : hijos) padre.appendChild(h);
        return padre;
    }

    /** Icono SVG de trazo sencillo (24x24). */
    static HTMLElement icono(String trazo) {
        HTMLElement s = el("span", "icono");
        s.setInnerHTML("<svg viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"2\" "
                + "stroke-linecap=\"round\" stroke-linejoin=\"round\">" + trazo + "</svg>");
        return s;
    }

    static final String ICONO_RUTA = "<circle cx=\"6\" cy=\"19\" r=\"2.5\"/><circle cx=\"18\" cy=\"5\" r=\"2.5\"/>"
            + "<path d=\"M8.5 19H16a3.5 3.5 0 0 0 0-7H8a3.5 3.5 0 0 1 0-7h7.5\"/>";
    static final String ICONO_BUSCAR = "<circle cx=\"11\" cy=\"11\" r=\"7\"/><path d=\"m20 20-3.5-3.5\"/>";
    static final String ICONO_MAPA = "<path d=\"M9 4 3 6v14l6-2 6 2 6-2V4l-6 2-6-2z\"/><path d=\"M9 4v14M15 6v14\"/>";
    static final String ICONO_ADMIN = "<path d=\"M12 3 4 6v6c0 5 3.5 8 8 9 4.5-1 8-4 8-9V6l-8-3z\"/><path d=\"m9 12 2 2 4-4\"/>";
    static final String ICONO_SALIR = "<path d=\"M15 4h3a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2h-3\"/><path d=\"M10 17l-5-5 5-5M5 12h11\"/>";
    static final String ICONO_CAMBIAR = "<path d=\"M7 4v16M7 20l-3-3M7 20l3-3M17 20V4M17 4l-3 3M17 4l3 3\"/>";

    /**
     * Ventana de confirmación propia (más clara en el celular que la del navegador).
     * Llama a {@code siAcepta} solo si la persona confirma.
     */
    static void confirmar(String titulo, String mensaje, String textoAceptar, boolean peligro, Runnable siAcepta) {
        HTMLElement fondo = el("div", "modal-fondo");
        HTMLElement caja = el("div", "modal");
        caja.appendChild(texto("h3", null, titulo));
        HTMLElement cuerpo = texto("p", "modal-texto", mensaje);
        caja.appendChild(cuerpo);
        HTMLElement acciones = el("div", "modal-acciones");
        acciones.appendChild(boton("Cancelar", "btn btn-contorno", () -> doc().getBody().removeChild(fondo)));
        acciones.appendChild(boton(textoAceptar, peligro ? "btn btn-peligro" : "btn btn-primario", () -> {
            doc().getBody().removeChild(fondo);
            siAcepta.run();
        }));
        caja.appendChild(acciones);
        fondo.appendChild(caja);
        doc().getBody().appendChild(fondo);
    }

    /** Mensaje breve que aparece abajo y desaparece solo. */
    static void aviso(String mensaje, boolean exito) {
        HTMLElement t = texto("div", exito ? "toast toast-ok" : "toast toast-error", mensaje);
        doc().getBody().appendChild(t);
        org.teavm.jso.browser.Window.setTimeout(() -> t.getClassList().add("visible"), 20);
        org.teavm.jso.browser.Window.setTimeout(() -> {
            t.getClassList().remove("visible");
            org.teavm.jso.browser.Window.setTimeout(() -> {
                if (t.getParentNode() != null) t.getParentNode().removeChild(t);
            }, 400);
        }, 2600);
    }
}
