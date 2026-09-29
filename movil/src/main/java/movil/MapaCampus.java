package movil;

import controlador.CampusControlador;
import modelo.Edificio;
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
import org.teavm.jso.dom.html.HTMLImageElement;
import org.teavm.jso.dom.html.TextRectangle;
import persistencia.RepositorioEstado;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Mapa ilustrado del campus dibujado en un canvas: la imagen de fondo, la ruta
 * calculada siguiendo los caminos reales y un pin por edificio.
 *
 * Se maneja con los dedos: arrastrar para moverse, pellizcar para acercar o alejar,
 * y tocar un pin para ver el edificio. En el modo "ubicar" (panel de administración)
 * un toque devuelve la posición elegida sobre la imagen.
 */
final class MapaCampus {

    /** Evento de puntero (dedo, mouse o lápiz): agrega el identificador del puntero. */
    interface EventoPuntero extends MouseEvent {
        @JSProperty
        int getPointerId();
    }

    @JSBody(params = {"el", "id"}, script = "try { el.setPointerCapture(id); } catch (e) {}")
    private static native void capturarPuntero(HTMLElement el, int id);

    private static final String VINOTINTO = "#800020";
    private static final String VINOTINTO_OSCURO = "#5A0016";
    private static final String DORADO = "#D4AF37";
    private static final String VERDE = "#0B7A55";
    private static final double ZOOM_MAX = 3.0;
    private static final double RADIO_PIN = 13;

    private final CampusControlador controlador;
    private final HTMLElement contenedor = Dom.el("div", "mapa-lienzo");
    private final HTMLCanvasElement canvas = (HTMLCanvasElement) Dom.el("canvas", null);
    private final CanvasRenderingContext2D ctx;
    private final HTMLImageElement imagen = (HTMLImageElement) Dom.el("img", null);
    private boolean imagenLista;

    private double escala = 0.5, offX, offY;
    private double anchoCss = 1, altoCss = 1;
    private boolean vistaInicial = true;
    private boolean pendienteDibujo;

    private List<String> ruta = new ArrayList<>();
    private String seleccionado;
    private Consumer<String> alTocarEdificio = id -> { };
    private Consumer<int[]> alUbicar;
    private int[] marcador;

    // Gestos
    private final Map<Integer, double[]> punteros = new HashMap<>();
    private double inicioX, inicioY, escalaInicio, distInicio, medioX0, medioY0, offX0, offY0;
    private boolean arrastrando, huboPellizco;

    MapaCampus(CampusControlador controlador) {
        this.controlador = controlador;
        ctx = (CanvasRenderingContext2D) canvas.getContext("2d");
        contenedor.appendChild(canvas);

        imagen.listenLoad(e -> {
            imagenLista = true;
            redibujar();
        });
        imagen.setSrc("mapa_campus.jpg");

        canvas.addEventListener("pointerdown", (EventListener<Event>) e -> alBajar((EventoPuntero) e));
        canvas.addEventListener("pointermove", (EventListener<Event>) e -> alMover((EventoPuntero) e));
        canvas.addEventListener("pointerup", (EventListener<Event>) e -> alSubir((EventoPuntero) e, true));
        canvas.addEventListener("pointercancel", (EventListener<Event>) e -> alSubir((EventoPuntero) e, false));
        canvas.addEventListener("wheel", (EventListener<Event>) e -> alRueda((WheelEvent) e));
        Window.current().addEventListener("resize", (EventListener<Event>) e -> activar());
    }

    HTMLElement getElemento() {
        return contenedor;
    }

    void setAlTocarEdificio(Consumer<String> accion) {
        this.alTocarEdificio = accion;
    }

    /** Muestra una ruta (lista de ids en orden). Lista vacía para quitarla. */
    void setRuta(List<String> ruta) {
        this.ruta = ruta == null ? new ArrayList<>() : new ArrayList<>(ruta);
        redibujar();
    }

    void setSeleccionado(String id) {
        this.seleccionado = id;
        redibujar();
    }

    /** Modo ubicar: el siguiente toque entrega la posición sobre la imagen. */
    void pedirUbicacion(Consumer<int[]> alUbicar) {
        this.alUbicar = alUbicar;
        this.marcador = null;
        redibujar();
    }

    void cancelarUbicacion() {
        this.alUbicar = null;
        this.marcador = null;
        redibujar();
    }

    /** Se llama cuando el mapa se hace visible o cambia el tamaño de la pantalla. */
    void activar() {
        TextRectangle r = contenedor.getBoundingClientRect();
        if (r.getWidth() <= 0 || r.getHeight() <= 0) return;
        double dpr = Window.current().getDevicePixelRatio();
        anchoCss = r.getWidth();
        altoCss = r.getHeight();
        canvas.setWidth((int) Math.round(anchoCss * dpr));
        canvas.setHeight((int) Math.round(altoCss * dpr));
        if (vistaInicial) {
            vistaInicial = false;
            if (ruta.size() > 1) enfocarRuta(); else vistaGeneral();
        }
        limitar();
        redibujar();
    }

    // ==================== Encuadre y zoom ====================

    private double escalaMinima() {
        return Math.min(anchoCss / CoordenadasMapa.ANCHO, altoCss / CoordenadasMapa.ALTO);
    }

    /** Vista general: el mapa llena toda la pantalla (en vertical se recorre a lo ancho con el dedo). */
    void vistaGeneral() {
        escala = Math.max(anchoCss / CoordenadasMapa.ANCHO, altoCss / CoordenadasMapa.ALTO);
        centrarEn(CoordenadasMapa.ANCHO / 2.0, CoordenadasMapa.ALTO / 2.0);
    }

    /** Acerca el mapa para que la ruta completa quede a la vista. */
    void enfocarRuta() {
        if (ruta.size() < 2 || anchoCss <= 1) {
            vistaInicial = anchoCss <= 1;
            return;
        }
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
        for (int i = 0; i < ruta.size() - 1; i++) {
            for (int[] p : puntosDelTramo(ruta.get(i), ruta.get(i + 1))) {
                minX = Math.min(minX, p[0]); maxX = Math.max(maxX, p[0]);
                minY = Math.min(minY, p[1]); maxY = Math.max(maxY, p[1]);
            }
        }
        double margen = 60;
        double ancho = Math.max(1, maxX - minX + margen * 2), alto = Math.max(1, maxY - minY + margen * 2);
        escala = Math.min(ZOOM_MAX, Math.max(escalaMinima(), Math.min(anchoCss / ancho, altoCss / alto)));
        centrarEn((minX + maxX) / 2, (minY + maxY) / 2);
        redibujar();
    }

    void acercar(double factor) {
        zoomEn(anchoCss / 2, altoCss / 2, escala * factor);
    }

    private void centrarEn(double xImg, double yImg) {
        offX = anchoCss / 2 - xImg * escala;
        offY = altoCss / 2 - yImg * escala;
        limitar();
    }

    private void zoomEn(double xPantalla, double yPantalla, double nuevaEscala) {
        nuevaEscala = Math.max(escalaMinima(), Math.min(ZOOM_MAX, nuevaEscala));
        double xImg = (xPantalla - offX) / escala, yImg = (yPantalla - offY) / escala;
        escala = nuevaEscala;
        offX = xPantalla - xImg * escala;
        offY = yPantalla - yImg * escala;
        limitar();
        redibujar();
    }

    /** Evita que el mapa se salga de la pantalla. */
    private void limitar() {
        escala = Math.max(escalaMinima(), Math.min(ZOOM_MAX, escala));
        double w = CoordenadasMapa.ANCHO * escala, h = CoordenadasMapa.ALTO * escala;
        offX = w <= anchoCss ? (anchoCss - w) / 2 : Math.min(0, Math.max(anchoCss - w, offX));
        offY = h <= altoCss ? (altoCss - h) / 2 : Math.min(0, Math.max(altoCss - h, offY));
    }

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
            iniciarPellizco();
        }
    }

    private void iniciarPellizco() {
        List<double[]> ps = new ArrayList<>(punteros.values());
        double[] a = ps.get(0), b = ps.get(1);
        distInicio = Math.max(1, Math.hypot(a[0] - b[0], a[1] - b[1]));
        medioX0 = (a[0] + b[0]) / 2; medioY0 = (a[1] + b[1]) / 2;
        escalaInicio = escala; offX0 = offX; offY0 = offY;
        huboPellizco = true;
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
            double xImg = (medioX0 - offX0) / escalaInicio, yImg = (medioY0 - offY0) / escalaInicio;
            escala = Math.max(escalaMinima(), Math.min(ZOOM_MAX, escalaInicio * dist / distInicio));
            offX = medioX - xImg * escala;
            offY = medioY - yImg * escala;
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

    private void alTocar(double x, double y) {
        if (alUbicar != null) {
            int xImg = (int) Math.round((x - offX) / escala), yImg = (int) Math.round((y - offY) / escala);
            if (xImg < 0 || yImg < 0 || xImg > CoordenadasMapa.ANCHO || yImg > CoordenadasMapa.ALTO) return;
            marcador = new int[]{xImg, yImg};
            redibujar();
            alUbicar.accept(marcador);
            return;
        }
        String encontrado = null;
        double mejor = RADIO_PIN + 12;
        for (Map.Entry<String, int[]> e : posiciones().entrySet()) {
            int[] r = CoordenadasMapa.ROTULOS.get(e.getKey());
            if (r != null && x >= aPantallaX(r[0]) - 6 && x <= aPantallaX(r[0] + r[2]) + 6
                    && y >= aPantallaY(r[1]) - 6 && y <= aPantallaY(r[1] + r[3]) + 6) {
                encontrado = e.getKey();
                break;
            }
            double d = Math.hypot(aPantallaX(e.getValue()[0]) - x, aPantallaY(e.getValue()[1]) - y);
            if (d < mejor) { mejor = d; encontrado = e.getKey(); }
        }
        alTocarEdificio.accept(encontrado);
    }

    // ==================== Datos ====================

    /** Posiciones de todos los edificios: los del mapa base y los agregados por el administrador. */
    private Map<String, int[]> posiciones() {
        Map<String, int[]> todas = new HashMap<>();
        for (String id : controlador.getGrafo().getEdificios().keySet()) {
            int[] p = CoordenadasMapa.POSICIONES.get(id);
            if (p != null) todas.put(id, p);
        }
        for (RepositorioEstado.EdificioPersonalizado e : controlador.getEdificiosPersonalizados()) {
            if (controlador.getGrafo().existeEdificio(e.id)) todas.put(e.id, new int[]{e.xMapa, e.yMapa});
        }
        return todas;
    }

    /** Puntos del tramo en coordenadas de la imagen, en el sentido del recorrido. */
    private List<int[]> puntosDelTramo(String a, String b) {
        Map<String, int[]> pos = posiciones();
        List<int[]> especial = CoordenadasMapa.TRAZADOS.get(CoordenadasMapa.clave(a, b));
        List<int[]> puntos = new ArrayList<>();
        if (especial != null) {
            puntos.addAll(especial);
            int[] inicio = pos.get(a);
            if (inicio != null) {
                int[] primero = puntos.get(0), ultimo = puntos.get(puntos.size() - 1);
                if (Math.hypot(ultimo[0] - inicio[0], ultimo[1] - inicio[1])
                        < Math.hypot(primero[0] - inicio[0], primero[1] - inicio[1])) {
                    Collections.reverse(puntos);
                }
            }
        } else if (pos.get(a) != null && pos.get(b) != null) {
            puntos.add(pos.get(a));
            puntos.add(pos.get(b));
        }
        return puntos;
    }

    private double aPantallaX(double x) { return x * escala + offX; }
    private double aPantallaY(double y) { return y * escala + offY; }

    /** Texto corto dentro del pin. */
    static String etiquetaCorta(String id) {
        switch (id) {
            case "Porteria 1": return "P1";
            case "Porteria 2": return "P2";
            case "Templo": return "T";
            default: return id.length() <= 3 ? id : id.substring(0, 3);
        }
    }

    // ==================== Dibujo ====================

    void redibujar() {
        if (pendienteDibujo) return;
        pendienteDibujo = true;
        Window.requestAnimationFrame(t -> {
            pendienteDibujo = false;
            dibujar();
        });
    }

    private void dibujar() {
        double dpr = Window.current().getDevicePixelRatio();
        ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
        ctx.setFillStyle("#E9EEF3");
        ctx.fillRect(0, 0, anchoCss, altoCss);
        if (!imagenLista) {
            ctx.setFillStyle("#64748B");
            ctx.setFont("15px system-ui, sans-serif");
            ctx.fillText("Cargando mapa…", 20, 30);
            return;
        }
        ctx.drawImage(imagen, offX, offY, CoordenadasMapa.ANCHO * escala, CoordenadasMapa.ALTO * escala);

        boolean hayRuta = ruta.size() > 1;
        if (hayRuta) {
            // Velo claro para que la ruta resalte sobre la ilustración
            ctx.setFillStyle("rgba(255,255,255,0.32)");
            ctx.fillRect(offX, offY, CoordenadasMapa.ANCHO * escala, CoordenadasMapa.ALTO * escala);
            dibujarRuta();
        }
        dibujarPines(hayRuta);
        if (marcador != null) dibujarMarcador();
    }

    private void dibujarRuta() {
        ctx.setLineCap("round");
        ctx.setLineJoin("round");
        String[] colores = {VINOTINTO_OSCURO, DORADO};
        double[] anchos = {Math.max(8, 11 * escala), Math.max(4.5, 6.5 * escala)};
        for (int capa = 0; capa < 2; capa++) {
            ctx.beginPath();
            for (int i = 0; i < ruta.size() - 1; i++) {
                List<int[]> puntos = puntosDelTramo(ruta.get(i), ruta.get(i + 1));
                for (int j = 0; j < puntos.size(); j++) {
                    double x = aPantallaX(puntos.get(j)[0]), y = aPantallaY(puntos.get(j)[1]);
                    if (i == 0 && j == 0) ctx.moveTo(x, y); else ctx.lineTo(x, y);
                }
            }
            ctx.setStrokeStyle(colores[capa]);
            ctx.setLineWidth(anchos[capa]);
            ctx.stroke();
        }
    }

    private void dibujarPines(boolean hayRuta) {
        Map<String, int[]> pos = posiciones();
        String origen = hayRuta ? ruta.get(0) : null;
        String destino = hayRuta ? ruta.get(ruta.size() - 1) : null;
        ctx.setTextAlign("center");
        ctx.setTextBaseline("middle");
        // Primero los que no están en la ruta, para que los de la ruta queden encima
        for (int pasada = 0; pasada < 2; pasada++) {
            for (Map.Entry<String, int[]> e : pos.entrySet()) {
                String id = e.getKey();
                boolean enRuta = hayRuta && ruta.contains(id);
                if ((pasada == 1) != (enRuta || id.equals(seleccionado))) continue;

                double x = aPantallaX(e.getValue()[0]), y = aPantallaY(e.getValue()[1]);
                if (x < -30 || y < -30 || x > anchoCss + 30 || y > altoCss + 30) continue;
                boolean esSel = id.equals(seleccionado);
                int[] rotulo = CoordenadasMapa.ROTULOS.get(id);
                if (rotulo != null) {
                    dibujarRotulo(rotulo, id.equals(origen) ? VERDE : id.equals(destino) ? DORADO
                            : enRuta ? DORADO : null, esSel);
                    continue;
                }
                double r = esSel ? RADIO_PIN + 4 : RADIO_PIN;

                String relleno = VINOTINTO, texto = "#FFFFFF", borde = "#FFFFFF";
                if (id.equals(origen)) relleno = VERDE;
                else if (id.equals(destino)) { relleno = DORADO; texto = "#3B2A00"; }
                else if (enRuta) borde = DORADO;

                ctx.setGlobalAlpha(hayRuta && !enRuta && !esSel ? 0.6 : 1);
                ctx.setShadowColor("rgba(0,0,0,0.35)");
                ctx.setShadowBlur(6);
                ctx.beginPath();
                ctx.arc(x, y, r, 0, Math.PI * 2);
                ctx.setFillStyle(relleno);
                ctx.fill();
                ctx.setShadowBlur(0);
                ctx.setLineWidth(esSel || enRuta ? 3 : 2);
                ctx.setStrokeStyle(esSel ? "#1E293B" : borde);
                ctx.stroke();

                String etiqueta = etiquetaCorta(id);
                ctx.setFillStyle(texto);
                ctx.setFont("bold " + (etiqueta.length() > 2 ? 9 : 12) + "px system-ui, sans-serif");
                ctx.fillText(etiqueta, x, y + 0.5);
            }
        }
        ctx.setGlobalAlpha(1);
    }

    /** Contorno redondeado alrededor de un letrero de la imagen (porterías, CAF). */
    private void dibujarRotulo(int[] r, String colorRuta, boolean seleccionado) {
        double x = aPantallaX(r[0]) - 3, y = aPantallaY(r[1]) - 3;
        double w = r[2] * escala + 6, h = r[3] * escala + 6, radio = Math.min(10, h / 2);
        // El letrero se vuelve a pintar encima: la línea de la ruta "pasa por debajo"
        ctx.setGlobalAlpha(1);
        ctx.drawImage(imagen, r[0], r[1], r[2], r[3], aPantallaX(r[0]), aPantallaY(r[1]), r[2] * escala, r[3] * escala);
        ctx.beginPath();
        ctx.moveTo(x + radio, y);
        ctx.arcTo(x + w, y, x + w, y + h, radio);
        ctx.arcTo(x + w, y + h, x, y + h, radio);
        ctx.arcTo(x, y + h, x, y, radio);
        ctx.arcTo(x, y, x + w, y, radio);
        ctx.closePath();
        ctx.setGlobalAlpha(1);
        if (colorRuta != null || seleccionado) {
            ctx.setLineWidth(seleccionado ? 4 : 3.5);
            ctx.setStrokeStyle(seleccionado ? "#1E293B" : colorRuta);
        } else {
            ctx.setLineWidth(2);
            ctx.setStrokeStyle("rgba(255,255,255,0.9)");
        }
        ctx.stroke();
    }

    private void dibujarMarcador() {
        double x = aPantallaX(marcador[0]), y = aPantallaY(marcador[1]);
        ctx.setStrokeStyle("#EF4444");
        ctx.setLineWidth(3);
        ctx.beginPath();
        ctx.arc(x, y, 14, 0, Math.PI * 2);
        ctx.moveTo(x - 22, y); ctx.lineTo(x + 22, y);
        ctx.moveTo(x, y - 22); ctx.lineTo(x, y + 22);
        ctx.stroke();
    }

    /** Nombre legible de un edificio (para las pantallas). */
    static String nombre(CampusControlador c, String id) {
        Edificio e = c.getGrafo().getEdificios().get(id);
        return e == null ? id : e.getNombre();
    }
}
