package movil;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSProperty;
import org.teavm.jso.browser.Window;
import org.teavm.jso.canvas.CanvasRenderingContext2D;
import org.teavm.jso.dom.events.Event;
import org.teavm.jso.dom.events.EventListener;
import org.teavm.jso.dom.events.MouseEvent;
import org.teavm.jso.dom.events.WheelEvent;
import org.teavm.jso.dom.html.HTMLCanvasElement;
import org.teavm.jso.dom.html.HTMLElement;
import org.teavm.jso.dom.html.TextRectangle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Lienzo (canvas) que se maneja con los dedos: arrastrar para moverse, pellizcar
 * para acercar o alejar y tocar para elegir. Lo comparten el mapa ilustrado
 * (MapaCampus) y la vista de grafo (VistaGrafo); cada uno define qué dibuja.
 *
 * El contenido se describe en coordenadas propias ("mundo") de tamaño
 * anchoMundo() x altoMundo(); aquí se lleva la escala y el desplazamiento.
 */
abstract class LienzoTactil {

    /** Evento de puntero (dedo, mouse o lápiz): agrega el identificador del puntero. */
    interface EventoPuntero extends MouseEvent {
        @JSProperty
        int getPointerId();
    }

    @JSBody(params = {"el", "id"}, script = "try { el.setPointerCapture(id); } catch (e) {}")
    private static native void capturarPuntero(HTMLElement el, int id);

    protected final HTMLElement contenedor = Dom.el("div", "mapa-lienzo");
    protected final HTMLCanvasElement canvas = (HTMLCanvasElement) Dom.el("canvas", null);
    protected final CanvasRenderingContext2D ctx;

    protected double escala = 0.5, offX, offY;
    protected double anchoCss = 1, altoCss = 1;
    /** Alto (px) tapado arriba por los controles flotantes: los encuadres lo evitan. */
    private double margenSuperior;
    private boolean vistaInicialPendiente = true;
    private boolean pendienteDibujo;

    private final Map<Integer, double[]> punteros = new HashMap<>();
    private double inicioX, inicioY, escalaInicio, distInicio, medioX0, medioY0, offX0, offY0;
    private boolean arrastrando, huboPellizco;

    LienzoTactil() {
        ctx = (CanvasRenderingContext2D) canvas.getContext("2d");
        contenedor.appendChild(canvas);
        canvas.addEventListener("pointerdown", (EventListener<Event>) e -> alBajar((EventoPuntero) e));
        canvas.addEventListener("pointermove", (EventListener<Event>) e -> alMover((EventoPuntero) e));
        canvas.addEventListener("pointerup", (EventListener<Event>) e -> alSubir((EventoPuntero) e, true));
        canvas.addEventListener("pointercancel", (EventListener<Event>) e -> alSubir((EventoPuntero) e, false));
        canvas.addEventListener("wheel", (EventListener<Event>) e -> alRueda((WheelEvent) e));
        Window.current().addEventListener("resize", (EventListener<Event>) e -> activar());
    }

    // ==================== Lo que define cada vista ====================

    protected abstract double anchoMundo();
    protected abstract double altoMundo();
    protected abstract void dibujarContenido();
    /** Toque (sin arrastre) en la posición de pantalla indicada. */
    protected abstract void alTocar(double x, double y);
    /** Encuadre al mostrarse por primera vez. */
    protected abstract void vistaInicial();

    protected double escalaMaxima() {
        return 3.0;
    }

    protected String colorFondo() {
        return "#E9EEF3";
    }

    HTMLElement getElemento() {
        return contenedor;
    }

    /** Se llama cuando el lienzo se hace visible o cambia el tamaño de la pantalla. */
    void activar() {
        TextRectangle r = contenedor.getBoundingClientRect();
        if (r.getWidth() <= 0 || r.getHeight() <= 0) return;
        double dpr = Window.current().getDevicePixelRatio();
        anchoCss = r.getWidth();
        altoCss = r.getHeight();
        canvas.setWidth((int) Math.round(anchoCss * dpr));
        canvas.setHeight((int) Math.round(altoCss * dpr));
        if (vistaInicialPendiente) {
            vistaInicialPendiente = false;
            vistaInicial();
        }
        limitar();
        redibujar();
    }

    /** true si el lienzo ya conoce su tamaño en pantalla. */
    protected boolean tieneTamano() {
        return anchoCss > 1;
    }

    /** Pide volver a aplicar la vista inicial la próxima vez que se muestre. */
    protected void reiniciarVistaAlMostrar() {
        vistaInicialPendiente = true;
    }

    // ==================== Encuadre y zoom ====================

    protected double escalaMinima() {
        return Math.min(anchoCss / anchoMundo(), altoCss / altoMundo());
    }

    void acercar(double factor) {
        zoomEn(anchoCss / 2, altoCss / 2, escala * factor);
    }

    protected void centrarEn(double xMundo, double yMundo) {
        offX = anchoCss / 2 - xMundo * escala;
        offY = altoCss / 2 - yMundo * escala;
        limitar();
    }

    void setMargenSuperior(double px) {
        margenSuperior = Math.max(0, px);
    }

    /** Encuadra el rectángulo indicado (coordenadas del mundo) con un margen, debajo de los controles. */
    protected void encuadrar(double minX, double minY, double maxX, double maxY, double margen) {
        double arriba = Math.min(margenSuperior, altoCss * 0.5);
        double altoLibre = Math.max(1, altoCss - arriba);
        double ancho = Math.max(1, maxX - minX + margen * 2), alto = Math.max(1, maxY - minY + margen * 2);
        escala = Math.min(escalaMaxima(), Math.max(escalaMinima(), Math.min(anchoCss / ancho, altoLibre / alto)));
        offX = anchoCss / 2 - (minX + maxX) / 2 * escala;
        offY = arriba + altoLibre / 2 - (minY + maxY) / 2 * escala;
        limitar();
        redibujar();
    }

    protected void zoomEn(double xPantalla, double yPantalla, double nuevaEscala) {
        nuevaEscala = Math.max(escalaMinima(), Math.min(escalaMaxima(), nuevaEscala));
        double xM = (xPantalla - offX) / escala, yM = (yPantalla - offY) / escala;
        escala = nuevaEscala;
        offX = xPantalla - xM * escala;
        offY = yPantalla - yM * escala;
        limitar();
        redibujar();
    }

    /** Evita que el contenido se salga de la pantalla. */
    protected void limitar() {
        escala = Math.max(escalaMinima(), Math.min(escalaMaxima(), escala));
        double w = anchoMundo() * escala, h = altoMundo() * escala;
        offX = w <= anchoCss ? (anchoCss - w) / 2 : Math.min(0, Math.max(anchoCss - w, offX));
        offY = h <= altoCss ? (altoCss - h) / 2 : Math.min(0, Math.max(altoCss - h, offY));
    }

    protected double aPantallaX(double x) { return x * escala + offX; }
    protected double aPantallaY(double y) { return y * escala + offY; }
    protected double aMundoX(double x) { return (x - offX) / escala; }
    protected double aMundoY(double y) { return (y - offY) / escala; }

    // ==================== Gestos ====================

    private double[] posicionLocal(MouseEvent e) {
        TextRectangle r = canvas.getBoundingClientRect();
        return new double[]{e.getClientX() - r.getLeft(), e.getClientY() - r.getTop()};
    }

    private void alBajar(EventoPuntero e) {
        e.preventDefault();
        capturarPuntero(canvas, e.getPointerId());
        double[] p = posicionLocal(e);
        punteros.put(e.getPointerId(), p);
        if (punteros.size() == 1) {
            inicioX = p[0]; inicioY = p[1];
            offX0 = offX; offY0 = offY;
            arrastrando = false;
            huboPellizco = false;
        } else if (punteros.size() == 2) {
            List<double[]> ps = new ArrayList<>(punteros.values());
            double[] a = ps.get(0), b = ps.get(1);
            distInicio = Math.max(1, Math.hypot(a[0] - b[0], a[1] - b[1]));
            medioX0 = (a[0] + b[0]) / 2; medioY0 = (a[1] + b[1]) / 2;
            escalaInicio = escala; offX0 = offX; offY0 = offY;
            huboPellizco = true;
        }
    }

    private void alMover(EventoPuntero e) {
        if (!punteros.containsKey(e.getPointerId())) return;
        e.preventDefault();
        double[] p = posicionLocal(e);
        punteros.put(e.getPointerId(), p);

        if (punteros.size() >= 2) {
            List<double[]> ps = new ArrayList<>(punteros.values());
            double[] a = ps.get(0), b = ps.get(1);
            double dist = Math.max(1, Math.hypot(a[0] - b[0], a[1] - b[1]));
            double medioX = (a[0] + b[0]) / 2, medioY = (a[1] + b[1]) / 2;
            double xM = (medioX0 - offX0) / escalaInicio, yM = (medioY0 - offY0) / escalaInicio;
            escala = Math.max(escalaMinima(), Math.min(escalaMaxima(), escalaInicio * dist / distInicio));
            offX = medioX - xM * escala;
            offY = medioY - yM * escala;
            limitar();
            redibujar();
        } else if (!huboPellizco) {
            double dx = p[0] - inicioX, dy = p[1] - inicioY;
            if (!arrastrando && Math.hypot(dx, dy) > 8) arrastrando = true;
            if (arrastrando) {
                offX = offX0 + dx;
                offY = offY0 + dy;
                limitar();
                redibujar();
            }
        }
    }

    private void alSubir(EventoPuntero e, boolean valido) {
        if (!punteros.containsKey(e.getPointerId())) return;
        double[] p = posicionLocal(e);
        punteros.remove(e.getPointerId());
        if (punteros.size() == 1) {
            // Queda un dedo: sigue como arrastre desde donde está
            double[] resto = punteros.values().iterator().next();
            inicioX = resto[0]; inicioY = resto[1];
            offX0 = offX; offY0 = offY;
            arrastrando = true;
            return;
        }
        if (punteros.isEmpty() && valido && !arrastrando && !huboPellizco) {
            alTocar(p[0], p[1]);
        }
    }

    private void alRueda(WheelEvent e) {
        e.preventDefault();
        double[] p = posicionLocal(e);
        zoomEn(p[0], p[1], escala * (e.getDeltaY() < 0 ? 1.15 : 1 / 1.15));
    }

    // ==================== Dibujo ====================

    void redibujar() {
        if (pendienteDibujo) return;
        pendienteDibujo = true;
        Window.requestAnimationFrame(t -> {
            pendienteDibujo = false;
            double dpr = Window.current().getDevicePixelRatio();
            ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
            ctx.setGlobalAlpha(1);
            ctx.setFillStyle(colorFondo());
            ctx.fillRect(0, 0, anchoCss, altoCss);
            dibujarContenido();
        });
    }

    /** Traza un rectángulo redondeado (sin rellenar ni delinear). */
    protected void rectRedondeado(double x, double y, double w, double h, double radio) {
        radio = Math.min(radio, Math.min(w, h) / 2);
        ctx.beginPath();
        ctx.moveTo(x + radio, y);
        ctx.arcTo(x + w, y, x + w, y + h, radio);
        ctx.arcTo(x + w, y + h, x, y + h, radio);
        ctx.arcTo(x, y + h, x, y, radio);
        ctx.arcTo(x, y, x + w, y, radio);
        ctx.closePath();
    }
}
