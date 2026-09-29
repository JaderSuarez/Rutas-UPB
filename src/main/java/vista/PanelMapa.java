package vista;

import java.awt.geom.*;

import modelo.Camino;
import modelo.GrafoCampus;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.Path2D;
import java.util.function.Consumer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Vista técnica del grafo del campus: edificios, aristas y distancias.
 *
 * Las posiciones se guardan en un lienzo de referencia (ANCHO_BASE x ALTO_BASE)
 * y se escalan al tamaño real del panel en cada repintado, de modo que el grafo
 * siempre se ve completo. Además admite zoom, centrado en la ruta marcada.
 */
public class PanelMapa extends JPanel {

    private static final int ANCHO_BASE = 836;
    private static final int ALTO_BASE = 720;
    private static final int MARGEN = 46;

    private static final double ZOOM_MIN = 1.0;
    private static final double ZOOM_MAX = 3.0;

    private final GrafoCampus grafo;
    private List<String> rutaActual;
    private final Map<String, Point> posicionesBase;
    private String seleccionOrigen;
    private String seleccionDestino;
    private double zoom = 1.0;
    private Consumer<Point> alUbicar;

    public PanelMapa(GrafoCampus grafo) {
        this.grafo = grafo;
        this.posicionesBase = new HashMap<>();
        this.setBackground(UIColores.MAPA_FONDO);
        inicializarCoordenadas();

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) { actualizarHover(e.getPoint()); }
        });
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent e) { actualizarHover(null); }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (alUbicar == null && zonaLeyenda != null && zonaLeyenda.contains(e.getPoint())) {
                    leyendaAbierta = !leyendaAbierta;   // minimizar o abrir las convenciones
                    repaint();
                    return;
                }
                if (alUbicar != null) {
                    Consumer<Point> destino = alUbicar;
                    alUbicar = null;
                    setCursor(Cursor.getDefaultCursor());
                    destino.accept(aCoordenadaBase(e.getPoint()));
                    repaint();
                }
            }
        });
    }

    /**
     * Activa el modo de ubicación: el siguiente clic sobre el panel entrega la
     * posición elegida, ya convertida al lienzo de referencia del grafo.
     */
    public void pedirUbicacion(Consumer<Point> alUbicar) {
        this.alUbicar = alUbicar;
        setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
        repaint();
    }

    public void cancelarUbicacion() {
        this.alUbicar = null;
        setCursor(Cursor.getDefaultCursor());
        repaint();
    }

    /** Convierte un punto de pantalla a coordenadas del lienzo de referencia. */
    private Point aCoordenadaBase(Point pantalla) {
        double esc = escalaActual();
        return new Point((int) ((pantalla.x - offsetX()) / esc),
                         (int) ((pantalla.y - offsetY()) / esc));
    }

    /** Registra la posición de un edificio agregado por el administrador. */
    public void registrarPosicion(String id, int baseX, int baseY) {
        posicionesBase.put(id, new Point(baseX, baseY));
        repaint();
    }

    /** Olvida por completo la posición de un edificio (al eliminarlo). */
    public void quitarPosicion(String id) {
        posicionesBase.remove(id);
        if (id.equals(seleccionOrigen)) seleccionOrigen = null;
        if (id.equals(seleccionDestino)) seleccionDestino = null;
        repaint();
    }

    private void inicializarCoordenadas() {
        // Coordenadas sobre el lienzo de referencia, tomadas de la imagen del
        // grafo elaborada por el equipo.
        posicionesBase.put("M", new Point(70, 304));
        posicionesBase.put("Porteria 2", new Point(228, 145));
        posicionesBase.put("K", new Point(479, 94));
        posicionesBase.put("L", new Point(674, 57));
        posicionesBase.put("I", new Point(602, 129));
        posicionesBase.put("H", new Point(725, 231));
        posicionesBase.put("E", new Point(665, 266));
        posicionesBase.put("F", new Point(725, 294));
        posicionesBase.put("G", new Point(766, 354));
        posicionesBase.put("D", new Point(541, 293));
        posicionesBase.put("CAF", new Point(412, 370));
        posicionesBase.put("A", new Point(310, 473));
        posicionesBase.put("B", new Point(428, 498));
        posicionesBase.put("Templo", new Point(208, 554));
        posicionesBase.put("C", new Point(546, 473));
        posicionesBase.put("J", new Point(684, 473));
        posicionesBase.put("Porteria 1", new Point(619, 626));
    }

    public void setRutaDestacada(List<String> ruta) {
        this.rutaActual = ruta;
        repaint();
    }

    /** Resalta el origen y el destino elegidos, incluso antes de calcular la ruta. */
    public void setSeleccion(String origen, String destino) {
        this.seleccionOrigen = origen;
        this.seleccionDestino = destino;
        repaint();
    }

    // ---------------- Zoom ----------------
    public void acercar() {
        zoom = Math.min(ZOOM_MAX, zoom + 0.25);
        repaint();
    }

    public void alejar() {
        zoom = Math.max(ZOOM_MIN, zoom - 0.25);
        repaint();
    }

    public void restablecerZoom() {
        zoom = 1.0;
        repaint();
    }

    // ---------------- Escalado ----------------
    private double escalaActual() {
        double dispX = Math.max(1, getWidth() - MARGEN * 2);
        double dispY = Math.max(1, getHeight() - MARGEN * 2);
        return Math.min(dispX / ANCHO_BASE, dispY / ALTO_BASE) * zoom;
    }

    /**
     * Punto del lienzo de referencia sobre el que se centra el zoom: el centro
     * de la ruta marcada, o de la selección origen/destino. Si no hay nada
     * marcado, el centro del lienzo.
     */
    private Point focoBase() {
        java.util.List<Point> refs = new java.util.ArrayList<>();
        if (rutaActual != null && !rutaActual.isEmpty()) {
            for (String id : rutaActual) {
                Point p = posicionesBase.get(id);
                if (p != null) refs.add(p);
            }
        } else {
            Point o = seleccionOrigen == null ? null : posicionesBase.get(seleccionOrigen);
            Point d = seleccionDestino == null ? null : posicionesBase.get(seleccionDestino);
            if (o != null) refs.add(o);
            if (d != null) refs.add(d);
        }
        if (refs.isEmpty()) {
            return new Point(ANCHO_BASE / 2, ALTO_BASE / 2);
        }
        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
        for (Point p : refs) {
            minX = Math.min(minX, p.x); maxX = Math.max(maxX, p.x);
            minY = Math.min(minY, p.y); maxY = Math.max(maxY, p.y);
        }
        return new Point((minX + maxX) / 2, (minY + maxY) / 2);
    }

    private int offsetX() {
        double esc = escalaActual();
        int anchoDibujo = (int) (ANCHO_BASE * esc);
        if (anchoDibujo <= getWidth()) {
            return (getWidth() - anchoDibujo) / 2;
        }
        int deseado = (int) (getWidth() / 2.0 - focoBase().x * esc);
        return Math.min(0, Math.max(getWidth() - anchoDibujo, deseado));
    }

    private int offsetY() {
        double esc = escalaActual();
        int altoDibujo = (int) (ALTO_BASE * esc);
        if (altoDibujo <= getHeight()) {
            return (getHeight() - altoDibujo) / 2;
        }
        int deseado = (int) (getHeight() / 2.0 - focoBase().y * esc);
        return Math.min(0, Math.max(getHeight() - altoDibujo, deseado));
    }

    /** Convierte una coordenada del lienzo de referencia a coordenada de pantalla. */
    private Point aPantalla(String id) {
        Point base = posicionesBase.get(id);
        if (base == null) return null;
        double esc = escalaActual();
        return new Point((int) (base.x * esc) + offsetX(), (int) (base.y * esc) + offsetY());
    }

    private Point aPantalla(int baseX, int baseY) {
        double esc = escalaActual();
        return new Point((int) (baseX * esc) + offsetX(), (int) (baseY * esc) + offsetY());
    }

    private int escalar(int valor) {
        return Math.max(1, (int) Math.round(valor * escalaActual()));
    }

    // ==================== Estilo visual del grafo ====================
    private static final Color ORO = UIColores.ACENTO;                        // ruta calculada
    private static final Color NODO_FONDO = new Color(0xF7, 0xD9, 0xDE);
    private static final Color PUNTO_FONDO = new Color(0xFD, 0xE9, 0xC8);    // porterías, CAF, templo
    private static final Color PUNTO_BORDE = new Color(0xB5, 0x86, 0x2B);
    private static final Color PUNTO_TEXTO = new Color(0x6B, 0x4A, 0x10);
    private static final Color INICIO_FONDO = new Color(0xD1, 0xFA, 0xE5);
    private static final Color INICIO_BORDE = new Color(0x0B, 0x7A, 0x55);
    private static final Color DESTINO_FONDO = new Color(0xFE, 0xF3, 0xC7);
    private static final Color GRIS_PUNTOS = new Color(0xDC, 0xE1, 0xE8);

    private boolean mostrarDistancias = true;
    private boolean resaltarConexiones = true;

    /** Con el mouse sobre un edificio: resaltar (o no) sus conexiones. La ficha se muestra igual. */
    public void setResaltarConexiones(boolean resaltar) {
        this.resaltarConexiones = resaltar;
        repaint();
    }
    private String hoverEdificio;      // edificio bajo el mouse
    private Camino hoverCamino;        // camino bajo el mouse
    private Point posMouse;

    /** Muestra u oculta la distancia sobre cada arista. */
    public void setMostrarDistancias(boolean mostrar) {
        this.mostrarDistancias = mostrar;
        repaint();
    }

    /** Porterías, cafetería y templo se dibujan como rectángulos; los edificios, como círculos. */
    private boolean esPuntoDeReferencia(String id) {
        return id.length() > 1;
    }

    private String textoNodo(String id) {
        return id.startsWith("Porteria") ? id.replace("Porteria", "Portería") : id;
    }

    /** Figura del nodo en pantalla (círculo o rectángulo redondeado). */
    private Shape formaNodo(String id, Point p, Graphics2D g2, int extra) {
        int r = Math.max(13, escalar(19)) + extra;
        if (!esPuntoDeReferencia(id)) {
            return new Ellipse2D.Double(p.x - r, p.y - r, r * 2.0, r * 2.0);
        }
        g2.setFont(fuenteNodo());
        int ancho = g2.getFontMetrics().stringWidth(textoNodo(id)) + escalar(22) + extra * 2;
        int alto = (int) (r * 1.6);
        return new RoundRectangle2D.Double(p.x - ancho / 2.0, p.y - alto / 2.0, ancho, alto, alto * 0.6, alto * 0.6);
    }

    private Font fuenteNodo() {
        return new Font(EstiloUPB.FAMILIA, Font.BOLD, Math.max(10, (int) Math.round(13 * escalaActual())));
    }

    /** Figura (recta o curva) de la arista en pantalla. */
    private Shape formaArista(String idA, String idB, Point pA, Point pB) {
        Point[] c = controlesCurva(idA, idB, pA, pB);
        if (c == null) return new Line2D.Double(pA, pB);
        Path2D curva = new Path2D.Double();
        curva.moveTo(c[0].x, c[0].y);
        curva.curveTo(c[1].x, c[1].y, c[2].x, c[2].y, c[3].x, c[3].y);
        return curva;
    }

    /** Punto de la arista en la fracción t (0 = inicio, 1 = fin), sobre la curva si es curva. */
    private Point puntoEn(String idA, String idB, Point pA, Point pB, double t) {
        Point[] c = controlesCurva(idA, idB, pA, pB);
        if (c == null) return new Point((int) (pA.x + (pB.x - pA.x) * t), (int) (pA.y + (pB.y - pA.y) * t));
        double u = 1 - t;
        double a = u * u * u, b = 3 * u * u * t, d = 3 * u * t * t, e = t * t * t;
        return new Point((int) (a * c[0].x + b * c[1].x + d * c[2].x + e * c[3].x),
                         (int) (a * c[0].y + b * c[1].y + d * c[2].y + e * c[3].y));
    }

    /**
     * Puntos de las tres aristas curvas {desde, control1, control2, hasta}, o null si es recta.
     * Se trazan como curvas para que no atraviesen otros edificios del grafo.
     */
    private Point[] controlesCurva(String idA, String idB, Point pA, Point pB) {
        if (esTramo(idA, idB, "M", "Porteria 1")) {
            boolean m = "M".equals(idA);
            return new Point[]{m ? pA : pB, aPantalla(-40, 520), aPantalla(210, 715), m ? pB : pA};
        } else if (esTramo(idA, idB, "M", "Templo")) {
            boolean m = "M".equals(idA);
            return new Point[]{m ? pA : pB, aPantalla(55, 430), aPantalla(105, 545), m ? pB : pA};
        } else if (esTramo(idA, idB, "K", "Porteria 2")) {
            boolean k = "K".equals(idA);
            return new Point[]{k ? pA : pB, aPantalla(520, 260), aPantalla(330, 205), k ? pB : pA};
        }
        return null;
    }

    /** Caminos únicos (cada arista una sola vez). */
    /** Posición ya calculada de la etiqueta de distancia de un camino. */
    private static final class Etiqueta {
        final Camino camino; final Rectangle zona; final Font fuente;
        Etiqueta(Camino camino, Rectangle zona, Font fuente) { this.camino = camino; this.zona = zona; this.fuente = fuente; }
    }

    private java.util.List<Object> claveEtiquetas;
    private java.util.List<Etiqueta> etiquetas = new java.util.ArrayList<>();

    /** Todo lo que influye en dónde van las etiquetas de distancia. */
    private java.util.List<Object> claveEtiquetas(java.util.List<Camino> caminos, double esc) {
        java.util.List<Object> clave = new java.util.ArrayList<>();
        clave.add(getWidth()); clave.add(getHeight()); clave.add(esc);
        for (String id : posicionesBase.keySet()) { clave.add(id); clave.add(aPantalla(id)); clave.add(textoNodo(id)); }
        for (Camino c : caminos) { clave.add(c.getOrigenId()); clave.add(c.getDestinoId()); clave.add(c.getDistancia()); }
        clave.add(rutaActual == null ? null : new java.util.ArrayList<>(rutaActual));
        return clave;
    }

    /**
     * Ubica la etiqueta de distancia de cada camino sobre su propia arista o pegada a ella,
     * sin tapar nodos, otras etiquetas ni la línea de OTRA arista. Si no hay buen lugar,
     * usa una fuente menor. Primero la ruta; luego las aristas más cortas.
     */
    private java.util.List<Etiqueta> calcularEtiquetas(Graphics2D g2, java.util.List<Camino> caminos,
                                                        Font fNormal, Font fPequena) {
        java.util.List<Etiqueta> resultado = new java.util.ArrayList<>();
        java.util.List<Rectangle> ocupados = new java.util.ArrayList<>();
        for (String id : posicionesBase.keySet()) {
            Point p = aPantalla(id);
            if (p != null) {
                Rectangle r = formaNodo(id, p, g2, 2).getBounds();
                r.grow(2, 2);
                ocupados.add(r);
            }
        }
        // Trazo (delgado) de cada arista en pantalla, para saber si una etiqueta pisa otra línea
        java.util.Map<Camino, Shape> trazos = new java.util.HashMap<>();
        Stroke trazoFino = new BasicStroke(2f);
        for (Camino c : caminos) {
            Point pA = aPantalla(c.getOrigenId()), pB = aPantalla(c.getDestinoId());
            if (pA != null && pB != null)
                trazos.put(c, trazoFino.createStrokedShape(formaArista(c.getOrigenId(), c.getDestinoId(), pA, pB)));
        }
        java.util.List<Camino> orden = new java.util.ArrayList<>(caminos);
        orden.removeIf(c -> !trazos.containsKey(c));
        orden.sort((x, y) -> {
            int r = Boolean.compare(!esAristaDeRuta(x.getOrigenId(), x.getDestinoId()),
                                    !esAristaDeRuta(y.getOrigenId(), y.getDestinoId()));
            if (r != 0) return r;
            return Double.compare(aPantalla(x.getOrigenId()).distance(aPantalla(x.getDestinoId())),
                                  aPantalla(y.getOrigenId()).distance(aPantalla(y.getDestinoId())));
        });
        double[] posiciones = {0.5, 0.4, 0.6, 0.32, 0.68, 0.25, 0.75};
        for (Camino c : orden) {
            String a = c.getOrigenId(), b = c.getDestinoId();
            Point pA = aPantalla(a), pB = aPantalla(b);
            String txt = String.format("%.0f m", c.getDistancia());
            double dx = pB.x - pA.x, dy = pB.y - pA.y, largo = Math.max(1, Math.hypot(dx, dy));

            Rectangle elegido = null;
            Font fuenteElegida = fNormal;
            double mejor = Double.MAX_VALUE;
            for (Font f : new Font[]{fNormal, fPequena}) {
                FontMetrics fmx = g2.getFontMetrics(f);
                int w = fmx.stringWidth(txt) + (f == fNormal ? 10 : 7), h = fmx.getHeight() + (f == fNormal ? 2 : 0);
                // lado 0: sobre la línea; ±1: pegada a un costado; ±2: un poco más separada
                // (solo para aristas muy cortas, donde pegada taparía un nodo)
                for (int lado : new int[]{0, 1, -1, 2, -2}) {
                    for (double t : posiciones) {
                        Point m = puntoEn(a, b, pA, pB, t);
                        if (lado != 0) {
                            double sep = Math.abs(lado) == 1 ? h / 2.0 : h * 1.25;
                            int signo = Integer.signum(lado);
                            m = new Point((int) Math.round(m.x - dy / largo * sep * signo),
                                          (int) Math.round(m.y + dx / largo * sep * signo));
                        }
                        Rectangle r = new Rectangle(m.x - w / 2, m.y - h / 2, w, h);
                        double puntaje = 0;
                        for (Rectangle o : ocupados) {   // tapar un nodo u otra etiqueta es lo peor
                            Rectangle inter = o.intersection(r);
                            if (!inter.isEmpty()) puntaje += 6.0 * inter.width * inter.height;
                        }
                        for (java.util.Map.Entry<Camino, Shape> e : trazos.entrySet()) {
                            if (e.getKey() != c && e.getValue().intersects(r)) puntaje += 600;   // pisa otra arista
                        }
                        puntaje += (lado == 0 ? 0 : Math.abs(lado) == 1 ? 70 : 140) + (f == fPequena ? 40 : 0) + Math.abs(t - 0.5) * 40;
                        if (puntaje < mejor) { mejor = puntaje; elegido = r; fuenteElegida = f; }
                    }
                }
                if (mejor < 40) break;   // con la fuente normal ya hay un buen lugar sobre la línea
            }
            ocupados.add(new Rectangle(elegido.x - 2, elegido.y - 1, elegido.width + 4, elegido.height + 2));
            resultado.add(new Etiqueta(c, elegido, fuenteElegida));
        }
        return resultado;
    }

    private java.util.List<Camino> caminosUnicos() {
        java.util.List<Camino> lista = new java.util.ArrayList<>();
        java.util.Set<String> vistos = new java.util.HashSet<>();
        for (String id : grafo.getEdificios().keySet()) {
            for (Camino c : grafo.getAdyacentes(id)) {
                if (vistos.add(claveArista(c.getOrigenId(), c.getDestinoId()))) lista.add(c);
            }
        }
        return lista;
    }

    private boolean esAristaDeRuta(String a, String b) {
        if (rutaActual == null) return false;
        for (int i = 0; i < rutaActual.size() - 1; i++) {
            if (esTramo(a, b, rutaActual.get(i), rutaActual.get(i + 1))) return true;
        }
        return false;
    }

    /** Detecta qué edificio o camino hay bajo el mouse. */
    private void actualizarHover(Point m) {
        String nodo = null;
        Camino camino = null;
        if (m != null && alUbicar == null) {
            Graphics2D g2 = (Graphics2D) getGraphics();
            for (String id : posicionesBase.keySet()) {
                Point p = aPantalla(id);
                if (p != null && g2 != null && formaNodo(id, p, g2, 4).contains(m)) { nodo = id; break; }
            }
            if (g2 != null) g2.dispose();
            if (nodo == null) {
                Stroke zona = new BasicStroke(9f);
                for (Camino c : caminosUnicos()) {
                    Point pA = aPantalla(c.getOrigenId()), pB = aPantalla(c.getDestinoId());
                    if (pA == null || pB == null) continue;
                    if (zona.createStrokedShape(formaArista(c.getOrigenId(), c.getDestinoId(), pA, pB)).contains(m)) {
                        camino = c; break;
                    }
                }
            }
        }
        boolean cambio = !java.util.Objects.equals(nodo, hoverEdificio) || camino != hoverCamino;
        hoverEdificio = nodo;
        hoverCamino = camino;
        posMouse = m;
        setCursor(alUbicar != null ? Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR)
                : (nodo != null || camino != null) ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                : Cursor.getDefaultCursor());
        if (cambio || nodo != null || camino != null) repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        Composite normal = g2.getComposite();
        double esc = escalaActual();
        float grosor = (float) Math.max(1.6, 2.4 * esc);

        // 0. Fondo de puntos
        g2.setColor(GRIS_PUNTOS);
        for (int x = 9; x < getWidth(); x += 18)
            for (int y = 9; y < getHeight(); y += 18)
                g2.fillOval(x - 1, y - 1, 2, 2);

        // Qué se resalta: el vecindario del edificio bajo el mouse o, si no, la ruta calculada
        boolean hayRuta = rutaActual != null && rutaActual.size() > 1;
        java.util.Set<String> nodosFoco = new java.util.HashSet<>();
        String foco = resaltarConexiones ? hoverEdificio : null;   // edificio cuyas conexiones se resaltan
        if (foco != null) {
            nodosFoco.add(foco);
            for (Camino c : grafo.getAdyacentes(foco)) nodosFoco.add(c.getDestinoId());
        } else if (hayRuta) {
            nodosFoco.addAll(rutaActual);
        }
        boolean hayFoco = !nodosFoco.isEmpty();
        float alfaFuera = foco != null ? 0.15f : 0.32f;

        java.util.List<Camino> caminos = caminosUnicos();

        // 1. Aristas
        for (Camino c : caminos) {
            String a = c.getOrigenId(), b = c.getDestinoId();
            Point pA = aPantalla(a), pB = aPantalla(b);
            if (pA == null || pB == null) continue;
            boolean enFoco = !hayFoco
                    || (foco != null ? (a.equals(foco) || b.equals(foco)) : esAristaDeRuta(a, b));
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, enFoco ? 1f : alfaFuera));
            float w = (c == hoverCamino) ? grosor * 2f : grosor;
            if (c.isBloqueado()) {
                g2.setColor(UIColores.CAMINO_BLOQUEADO);
                g2.setStroke(new BasicStroke(w, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0,
                        new float[]{6 * (float) esc + 2, 5 * (float) esc + 2}, 0));
            } else {
                g2.setColor(c.isTieneEscaleras() ? UIColores.CAMINO_ESCALERAS : UIColores.CAMINO_ACCESIBLE);
                g2.setStroke(new BasicStroke(w, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            }
            g2.draw(formaArista(a, b, pA, pB));
        }
        g2.setComposite(normal);

        // 2. Ruta calculada en dorado
        if (hayRuta) {
            g2.setColor(ORO);
            g2.setStroke(new BasicStroke((float) Math.max(4, 7 * esc), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            for (int i = 0; i < rutaActual.size() - 1; i++) {
                String a = rutaActual.get(i), b = rutaActual.get(i + 1);
                Point p1 = aPantalla(a), p2 = aPantalla(b);
                if (p1 != null && p2 != null) g2.draw(formaArista(a, b, p1, p2));
            }
        }

        // 3. Distancias sobre las aristas. Cada etiqueta se ubica sobre su propia arista o
        //    pegada a ella, sin tapar nodos, otras etiquetas ni la línea de OTRA arista (así no
        //    parece pertenecer a un camino vecino). Si no hay buen lugar, usa una fuente menor.
        //    Primero la ruta; luego las aristas más cortas, que tienen menos opciones.
        if (mostrarDistancias) {
            Font fNormal = new Font(EstiloUPB.FAMILIA, Font.PLAIN, Math.max(9, (int) Math.round(11 * esc)));
            Font fPequena = new Font(EstiloUPB.FAMILIA, Font.PLAIN, Math.max(8, (int) Math.round(9 * esc)));
            // La ubicación de las etiquetas solo cambia con el tamaño, el zoom, la ruta o los
            // caminos; se guarda y se recalcula únicamente cuando alguno de ellos cambia
            // (el panel se repinta con cada movimiento del mouse).
            java.util.List<Object> clave = claveEtiquetas(caminos, esc);
            if (!clave.equals(claveEtiquetas)) {
                etiquetas = calcularEtiquetas(g2, caminos, fNormal, fPequena);
                claveEtiquetas = clave;
            }
            for (Etiqueta et : etiquetas) {
                Camino c = et.camino;
                String a = c.getOrigenId(), b = c.getDestinoId();
                boolean deRuta = esAristaDeRuta(a, b);
                String txt = String.format("%.0f m", c.getDistancia());
                Rectangle elegido = et.zona;
                Font fuenteElegida = et.fuente;

                boolean enFoco = !hayFoco
                        || (foco != null ? (a.equals(foco) || b.equals(foco)) : deRuta);
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, enFoco ? 1f : alfaFuera));
                g2.setFont(fuenteElegida);
                FontMetrics fm = g2.getFontMetrics();
                int h = elegido.height;
                RoundRectangle2D pill = new RoundRectangle2D.Double(elegido.x, elegido.y, elegido.width, h, h, h);
                g2.setColor(Color.WHITE);
                g2.fill(pill);
                g2.setColor(deRuta && hayRuta ? ORO : new Color(0xCB, 0xD5, 0xE1));
                g2.setStroke(new BasicStroke(deRuta && hayRuta ? 1.6f : 1f));
                g2.draw(pill);
                g2.setColor(UIColores.TEXTO_OSCURO);
                g2.drawString(txt, elegido.x + (elegido.width - fm.stringWidth(txt)) / 2,
                        elegido.y + (h + fm.getAscent() - fm.getDescent()) / 2);
            }
            g2.setComposite(normal);
        }

        // 4. Edificios y puntos de referencia
        Font fNodo = fuenteNodo();
        for (String id : posicionesBase.keySet()) {
            Point p = aPantalla(id);
            if (p == null) continue;
            boolean esInicio = hayRuta ? id.equals(rutaActual.get(0)) : id.equals(seleccionOrigen);
            boolean esFin = hayRuta ? id.equals(rutaActual.get(rutaActual.size() - 1)) : id.equals(seleccionDestino);
            boolean deRuta = hayRuta && rutaActual.contains(id);
            boolean enFoco = !hayFoco || nodosFoco.contains(id);
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, enFoco ? 1f : Math.min(1f, alfaFuera + 0.2f)));

            Color fondo, borde, texto;
            if (esInicio) { fondo = INICIO_FONDO; borde = INICIO_BORDE; texto = INICIO_BORDE; }
            else if (esFin) { fondo = DESTINO_FONDO; borde = PUNTO_BORDE; texto = PUNTO_TEXTO; }
            else if (esPuntoDeReferencia(id)) { fondo = PUNTO_FONDO; borde = PUNTO_BORDE; texto = PUNTO_TEXTO; }
            else { fondo = NODO_FONDO; borde = UIColores.PRIMARIO; texto = UIColores.PRIMARIO_OSCURO; }

            boolean destacado = deRuta || esInicio || esFin || id.equals(hoverEdificio);
            Shape forma = formaNodo(id, p, g2, destacado ? 2 : 0);
            g2.setColor(fondo);
            g2.fill(forma);
            g2.setColor(borde);
            g2.setStroke(new BasicStroke(destacado ? (float) Math.max(2.4, 3 * esc) : (float) Math.max(1.4, 1.8 * esc)));
            g2.draw(forma);

            g2.setFont(fNodo);
            FontMetrics fm = g2.getFontMetrics();
            String t = textoNodo(id);
            g2.setColor(texto);
            g2.drawString(t, p.x - fm.stringWidth(t) / 2, p.y + fm.getAscent() / 2 - 2);

            // Rótulo de origen/destino cuando aún no hay ruta calculada
            if (!hayRuta && (esInicio || esFin)) {
                g2.setFont(new Font(EstiloUPB.FAMILIA, Font.BOLD, Math.max(9, (int) Math.round(10 * esc))));
                String r = esInicio ? "Origen" : "Destino";
                FontMetrics f2 = g2.getFontMetrics();
                Rectangle rb = forma.getBounds();
                g2.setColor(borde);
                g2.drawString(r, p.x - f2.stringWidth(r) / 2, rb.y + rb.height + f2.getAscent() + 2);
            }
        }
        g2.setComposite(normal);

        // 5. Número de paso sobre cada edificio de la ruta
        if (hayRuta) {
            Font fPaso = new Font(EstiloUPB.FAMILIA, Font.BOLD, Math.max(9, (int) Math.round(11 * esc)));
            g2.setFont(fPaso);
            FontMetrics fm = g2.getFontMetrics();
            int d = Math.max(16, (int) Math.round(20 * esc));
            for (int i = 0; i < rutaActual.size(); i++) {
                Point p = aPantalla(rutaActual.get(i));
                if (p == null) continue;
                Rectangle rb = formaNodo(rutaActual.get(i), p, g2, 2).getBounds();
                int cx = rb.x + (esPuntoDeReferencia(rutaActual.get(i)) ? 2 : 0), cy = rb.y;
                g2.setColor(Color.WHITE);
                g2.fillOval(cx - d / 2 - 2, cy - d / 2 - 2, d + 4, d + 4);
                g2.setColor(UIColores.PRIMARIO);
                g2.fillOval(cx - d / 2, cy - d / 2, d, d);
                g2.setColor(Color.WHITE);
                String n = String.valueOf(i + 1);
                g2.drawString(n, cx - fm.stringWidth(n) / 2, cy + fm.getAscent() / 2 - 1);
                g2.setFont(fPaso);
            }
        }

        // 6. Leyenda flotante
        dibujarLeyenda(g2);

        // 7. Ficha del elemento bajo el mouse
        if (posMouse != null && (hoverEdificio != null || hoverCamino != null)) {
            String titulo, detalle;
            if (hoverEdificio != null) {
                modelo.Edificio e = grafo.getEdificios().get(hoverEdificio);
                titulo = e != null ? e.getNombre() : hoverEdificio;
                int con = grafo.getAdyacentes(hoverEdificio).size();
                int lug = e != null ? e.getLugares().size() : 0;
                detalle = con + (con == 1 ? " conexión" : " conexiones") + "  ·  " + lug + (lug == 1 ? " lugar" : " lugares");
            } else {
                titulo = textoNodo(hoverCamino.getOrigenId()) + "  –  " + textoNodo(hoverCamino.getDestinoId());
                detalle = String.format("%.0f m", hoverCamino.getDistancia()) + "  ·  "
                        + (hoverCamino.isTieneEscaleras() ? "con escaleras" : "sin escaleras") + "  ·  "
                        + (hoverCamino.isBloqueado() ? "bloqueado" : "disponible");
            }
            dibujarFicha(g2, titulo, detalle, posMouse);
        }

        // 8. Aviso del modo de ubicación
        if (alUbicar != null) {
            String aviso = "Haz clic en el punto donde quedará el nuevo edificio";
            g2.setFont(new Font(EstiloUPB.FAMILIA, Font.BOLD, 13));
            FontMetrics fm = g2.getFontMetrics();
            int ancho = fm.stringWidth(aviso) + 24;
            int x = (getWidth() - ancho) / 2;
            g2.setColor(UIColores.PRIMARIO);
            g2.fillRoundRect(x, 10, ancho, 30, 14, 14);
            g2.setColor(UIColores.TEXTO_CLARO);
            g2.drawString(aviso, x + 12, 30);
        }
    }

    /** Tarjeta de convenciones en la esquina superior izquierda. */
    /** Convenciones abiertas o minimizadas (clic en su título). */
    private boolean leyendaAbierta = true;
    private Rectangle zonaLeyenda;

    private void dibujarLeyenda(Graphics2D g2) {
        int x = 12, y = 12, w = 176, h = 158;
        if (!leyendaAbierta) {
            zonaLeyenda = EstiloUPB.dibujarLeyendaPlegada(g2, x, y);
            return;
        }
        zonaLeyenda = new Rectangle(x, y, w, 28);
        g2.setColor(new Color(0, 0, 0, 18));
        g2.fillRoundRect(x + 2, y + 3, w, h, 14, 14);
        g2.setColor(Color.WHITE);
        g2.fillRoundRect(x, y, w, h, 14, 14);
        g2.setColor(new Color(0xD5, 0xDC, 0xE6));
        g2.setStroke(new BasicStroke(1f));
        g2.drawRoundRect(x, y, w, h, 14, 14);
        g2.setFont(new Font(EstiloUPB.FAMILIA, Font.BOLD, 12));
        g2.setColor(UIColores.TEXTO_OSCURO);
        g2.drawString("Convenciones", x + 12, y + 20);
        EstiloUPB.dibujarFlechaPlegar(g2, x + w - 16, y + 16, true);
        g2.setFont(new Font(EstiloUPB.FAMILIA, Font.PLAIN, 11));
        String[] txt = {"Sin escaleras", "Con escaleras", "Bloqueado", "Ruta calculada"};
        Color[] col = {UIColores.CAMINO_ACCESIBLE, UIColores.CAMINO_ESCALERAS, UIColores.CAMINO_BLOQUEADO, ORO};
        for (int i = 0; i < txt.length; i++) {
            int ly = y + 38 + i * 18;
            g2.setColor(col[i]);
            if (i == 2) {
                g2.setStroke(new BasicStroke(2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0, new float[]{5, 4}, 0));
            } else {
                g2.setStroke(new BasicStroke(i == 3 ? 5f : 2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            }
            g2.drawLine(x + 12, ly, x + 40, ly);
            g2.setColor(UIColores.TEXTO_MUTED);
            g2.drawString(txt[i], x + 50, ly + 4);
        }
        int ly = y + 38 + 4 * 18 + 2;
        g2.setStroke(new BasicStroke(1.6f));
        g2.setColor(NODO_FONDO); g2.fillOval(x + 18, ly - 7, 14, 14);
        g2.setColor(UIColores.PRIMARIO); g2.drawOval(x + 18, ly - 7, 14, 14);
        g2.setColor(UIColores.TEXTO_MUTED); g2.drawString("Edificio", x + 50, ly + 4);
        ly += 18;
        g2.setColor(PUNTO_FONDO); g2.fillRoundRect(x + 12, ly - 6, 28, 12, 8, 8);
        g2.setColor(PUNTO_BORDE); g2.drawRoundRect(x + 12, ly - 6, 28, 12, 8, 8);
        g2.setColor(UIColores.TEXTO_MUTED); g2.drawString("Portería, CAF, templo", x + 50, ly + 4);
    }

    /** Ficha flotante con el detalle del edificio o camino bajo el mouse. */
    private void dibujarFicha(Graphics2D g2, String titulo, String detalle, Point m) {
        Font fT = new Font(EstiloUPB.FAMILIA, Font.BOLD, 12), fD = new Font(EstiloUPB.FAMILIA, Font.PLAIN, 11);
        int w = Math.max(g2.getFontMetrics(fT).stringWidth(titulo), g2.getFontMetrics(fD).stringWidth(detalle)) + 24;
        int h = 46;
        int x = m.x + 16, y = m.y + 14;
        if (x + w > getWidth() - 6) x = m.x - w - 12;
        if (y + h > getHeight() - 6) y = m.y - h - 10;
        g2.setColor(new Color(0, 0, 0, 30));
        g2.fillRoundRect(x + 2, y + 3, w, h, 12, 12);
        g2.setColor(Color.WHITE);
        g2.fillRoundRect(x, y, w, h, 12, 12);
        g2.setColor(UIColores.PRIMARIO);
        g2.fillRoundRect(x, y + 8, 4, h - 16, 3, 3);
        g2.setColor(new Color(0xD5, 0xDC, 0xE6));
        g2.setStroke(new BasicStroke(1f));
        g2.drawRoundRect(x, y, w, h, 12, 12);
        g2.setFont(fT);
        g2.setColor(UIColores.TEXTO_OSCURO);
        g2.drawString(titulo, x + 14, y + 19);
        g2.setFont(fD);
        g2.setColor(UIColores.TEXTO_MUTED);
        g2.drawString(detalle, x + 14, y + 36);
    }

    /** Identificador sin orden de una arista, para no dibujarla dos veces. */
    private String claveArista(String a, String b) {
        return (a.compareTo(b) <= 0) ? a + "||" + b : b + "||" + a;
    }

    /** true si el par de edificios corresponde al tramo indicado, en cualquier orden. */
    private boolean esTramo(String idA, String idB, String uno, String otro) {
        return (uno.equals(idA) && otro.equals(idB)) || (otro.equals(idA) && uno.equals(idB));
    }
}
