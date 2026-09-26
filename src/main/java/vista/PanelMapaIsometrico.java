package vista;

import modelo.Camino;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import modelo.GrafoCampus;
import java.util.function.Consumer;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Vista "mapa ilustrado" del campus: usa una imagen isométrica de fondo y
 * dibuja encima los pines de cada edificio en coordenadas calibradas.
 *
 * IMPORTANTE: las coordenadas de POSICIONES están calibradas para la imagen
 * original de 1678 x 937 px (src/main/resources/mapa_campus.png). Si se
 * reemplaza la imagen por otra, hay que recalibrar este mapa de posiciones.
 */
public class PanelMapaIsometrico extends JPanel {

    private static final String RECURSO_MAPA = "/mapa_campus.png";
    private static final int ANCHO_ORIGINAL = 1679;
    private static final int ALTO_ORIGINAL = 937;

    private BufferedImage imagenMapa;
    private final Map<String, Point> posiciones = new HashMap<>();
    /** Tramos que no se dibujan en línea recta, sino siguiendo el camino real. */
    private final Map<String, java.util.List<Point>> trazadosEspeciales = new HashMap<>();
    /** Edificios agregados desde la aplicación: la imagen de fondo no los tiene dibujados. */
    private final Map<String, String> edificiosAgregados = new java.util.LinkedHashMap<>();

    private List<String> rutaActual;
    private List<Camino> tramosActuales;
    private String seleccionOrigen;
    private String seleccionDestino;

    private double zoom = 1.0;

    // ---- Estilo visual (coherente con la Vista Grafo y el Resumen de la Ruta) ----
    private static final Color ORO = UIColores.ACENTO;
    private static final Color INICIO = new Color(0x0B, 0x7A, 0x55);
    private GrafoCampus grafo;              // datos para la ficha y el resumen
    private String hoverEdificio;           // marcador bajo el mouse
    private Point posMouse;
    private double ultimaEscala = 1;        // transformación del último dibujo (para el mouse)
    private int ultimoOffsetX, ultimoOffsetY;

    /** Datos del campus para mostrar nombres, lugares y conexiones. */
    public void setGrafo(GrafoCampus grafo) { this.grafo = grafo; }
    private Consumer<Point> alUbicar;
    private static final double ZOOM_MIN = 0.6;
    private static final double ZOOM_MAX = 2.5;

    public PanelMapaIsometrico() {
        setBackground(UIColores.MAPA_FONDO);
        cargarImagen();
        calibrarPosiciones();
        calibrarTrazadosEspeciales();

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
                    destino.accept(aCoordenadaImagen(e.getPoint()));
                    repaint();
                }
            }
        });
    }

    /**
     * Activa el modo de ubicación: el siguiente clic entrega la posición
     * elegida en coordenadas de la imagen original del mapa.
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

    /** Convierte un punto de pantalla a coordenadas de la imagen original. */
    private Point aCoordenadaImagen(Point pantalla) {
        double escalaBase = Math.min(getWidth() / (double) ANCHO_ORIGINAL,
                                     getHeight() / (double) ALTO_ORIGINAL);
        double escala = escalaBase * zoom;
        int anchoDibujo = (int) (ANCHO_ORIGINAL * escala);
        int altoDibujo = (int) (ALTO_ORIGINAL * escala);

        Point foco = calcularFoco();
        int offsetX, offsetY;
        if (foco != null && anchoDibujo > getWidth()) {
            int deseado = (int) (getWidth() / 2.0 - foco.x * escala);
            offsetX = Math.min(0, Math.max(getWidth() - anchoDibujo, deseado));
        } else {
            offsetX = Math.max(0, (getWidth() - anchoDibujo) / 2);
        }
        if (foco != null && altoDibujo > getHeight()) {
            int deseado = (int) (getHeight() / 2.0 - foco.y * escala);
            offsetY = Math.min(0, Math.max(getHeight() - altoDibujo, deseado));
        } else {
            offsetY = Math.max(0, (getHeight() - altoDibujo) / 2);
        }

        return new Point((int) ((pantalla.x - offsetX) / escala),
                         (int) ((pantalla.y - offsetY) / escala));
    }

    /** Registra la posición de un edificio agregado por el administrador. */
    public void registrarPosicion(String id, int x, int y) {
        registrarPosicion(id, x, y, id);
    }

    /**
     * Registra un edificio agregado desde la aplicación. Como la imagen del
     * mapa no tiene su pin dibujado, el programa dibuja su propio marcador.
     */
    public void registrarPosicion(String id, int x, int y, String nombreVisible) {
        posiciones.put(id, new Point(x, y));
        edificiosAgregados.put(id, nombreVisible == null || nombreVisible.trim().isEmpty()
                ? id : nombreVisible.trim());
        repaint();
    }

    /** Olvida por completo la posición y el marcador de un edificio (al eliminarlo). */
    public void quitarPosicion(String id) {
        posiciones.remove(id);
        edificiosAgregados.remove(id);
        if (id.equals(seleccionOrigen)) seleccionOrigen = null;
        if (id.equals(seleccionDestino)) seleccionDestino = null;
        repaint();
    }

    private void cargarImagen() {
        try {
            java.io.InputStream in = getClass().getResourceAsStream(RECURSO_MAPA);
            if (in != null) {
                imagenMapa = ImageIO.read(in);
            }
        } catch (Exception e) {
            imagenMapa = null;
        }
    }

    /** Coordenadas del centro de cada pin sobre la imagen original (1678 x 937). */
    private void calibrarPosiciones() {
        posiciones.put("M", new Point(333, 338));
        posiciones.put("K", new Point(1102, 175));
        posiciones.put("L", new Point(1474, 251));
        posiciones.put("I", new Point(1307, 303));
        posiciones.put("H", new Point(1401, 403));
        posiciones.put("E", new Point(1221, 421));
        posiciones.put("F", new Point(1297, 462));
        posiciones.put("G", new Point(1362, 504));
        posiciones.put("D", new Point(986, 485));
        posiciones.put("J", new Point(1058, 599));
        posiciones.put("A", new Point(593, 613));
        posiciones.put("B", new Point(694, 622));
        posiciones.put("C", new Point(806, 612));
        posiciones.put("Templo", new Point(308, 634));
        posiciones.put("CAF", new Point(775, 514));
        posiciones.put("Porteria 2", new Point(673, 321));
        posiciones.put("Porteria 1", new Point(704, 799));
    }

    /**
     * Algunos tramos no se pueden representar con una línea recta porque
     * atravesarían edificios. Aquí se define la secuencia de puntos que sigue
     * el camino peatonal real sobre la imagen, en coordenadas de la imagen
     * original (1679 x 937).
     */
    private void calibrarTrazadosEspeciales() {
        // M - Portería 1: bordea el costado occidental del campus y pasa por
        // debajo del Templo, en vez de cruzar por encima de los edificios.
        trazadosEspeciales.put(clave("M", "Porteria 1"), java.util.List.of(
                new Point(332, 339),
                new Point(205, 430),
                new Point(120, 560),
                new Point(95, 660),
                new Point(110, 760),
                new Point(185, 838),
                new Point(340, 868),
                new Point(530, 852),
                new Point(704, 799)));

        // K - Portería 2: baja bordeando el edificio K y sigue la vía interna
        // hacia el occidente, en vez de cruzar en línea recta sobre la ladera.
        trazadosEspeciales.put(clave("K", "Porteria 2"), java.util.List.of(
                new Point(1102, 176),
                new Point(1060, 300),
                new Point(980, 420),
                new Point(870, 430),
                new Point(760, 390),
                new Point(690, 340),
                new Point(673, 321)));

        // M - Templo: desciende por el costado interior de las canchas, por
        // dentro del recorrido M - Portería 1, de modo que ambos no se crucen.
        trazadosEspeciales.put(clave("M", "Templo"), java.util.List.of(
                new Point(332, 339),
                new Point(250, 410),
                new Point(195, 500),
                new Point(180, 580),
                new Point(215, 625),
                new Point(280, 640),
                new Point(309, 635)));
    }

    /** Clave sin orden, para que el tramo sirva en los dos sentidos. */
    private String clave(String a, String b) {
        return (a.compareTo(b) <= 0) ? a + "||" + b : b + "||" + a;
    }

    /**
     * Puntos en pantalla del tramo entre dos edificios: el camino real si está
     * definido (respetando el sentido del recorrido), o los dos extremos.
     */
    private java.util.List<Point> puntosDelTramo(String idA, String idB,
                                                 double escala, int offsetX, int offsetY) {
        java.util.List<Point> resultado = new java.util.ArrayList<>();
        java.util.List<Point> especial = trazadosEspeciales.get(clave(idA, idB));

        if (especial != null) {
            java.util.List<Point> base = new java.util.ArrayList<>(especial);
            // El trazado se guarda en un sentido; si se recorre al revés, se invierte.
            Point inicio = posiciones.get(idA);
            if (inicio != null && base.size() > 1) {
                Point primero = base.get(0);
                Point ultimo = base.get(base.size() - 1);
                double dIni = distancia(inicio, primero);
                double dFin = distancia(inicio, ultimo);
                if (dFin < dIni) java.util.Collections.reverse(base);
            }
            for (Point p : base) {
                resultado.add(new Point((int) (p.x * escala) + offsetX, (int) (p.y * escala) + offsetY));
            }
        } else {
            Point p1 = aPantalla(idA, escala, offsetX, offsetY);
            Point p2 = aPantalla(idB, escala, offsetX, offsetY);
            if (p1 != null && p2 != null) {
                resultado.add(p1);
                resultado.add(p2);
            }
        }
        return resultado;
    }

    private double distancia(Point a, Point b) {
        return Math.hypot(a.x - b.x, a.y - b.y);
    }

    // ---------------- API pública ----------------
    public void setRutaDestacada(List<String> ruta, List<Camino> tramos) {
        this.rutaActual = ruta;
        this.tramosActuales = tramos;
        repaint();
    }

    public void setSeleccion(String origen, String destino) {
        this.seleccionOrigen = origen;
        this.seleccionDestino = destino;
        repaint();
    }

    public void acercar() {
        zoom = Math.min(ZOOM_MAX, zoom + 0.2);
        revalidate();
        repaint();
    }

    public void alejar() {
        zoom = Math.max(ZOOM_MIN, zoom - 0.2);
        revalidate();
        repaint();
    }

    public void restablecerZoom() {
        zoom = 1.0;
        revalidate();
        repaint();
    }

    // ---------------- Dibujo ----------------
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        if (imagenMapa == null) {
            g2.setColor(UIColores.TEXTO_MUTED);
            g2.setFont(new Font(EstiloUPB.FAMILIA, Font.ITALIC, 13));
            g2.drawString("No se encontró la imagen del mapa (src/main/resources/mapa_campus.png).", 20, 40);
            g2.drawString("Usa la pestaña \"Vista Grafo\" mientras se agrega el recurso.", 20, 62);
            return;
        }

        // Escala base: encajar la imagen en el panel, multiplicada por el zoom
        double escalaBase = Math.min(getWidth() / (double) ANCHO_ORIGINAL,
                                     getHeight() / (double) ALTO_ORIGINAL);
        double escala = escalaBase * zoom;

        int anchoDibujo = (int) (ANCHO_ORIGINAL * escala);
        int altoDibujo = (int) (ALTO_ORIGINAL * escala);

        // El zoom se centra en la ruta marcada (o en la selección de
        // origen/destino) para que al acercar se vea justo el tramo de interés,
        // en vez de una esquina fija del mapa.
        Point focoImagen = calcularFoco();
        int offsetX, offsetY;

        if (focoImagen != null && anchoDibujo > getWidth()) {
            int deseado = (int) (getWidth() / 2.0 - focoImagen.x * escala);
            offsetX = Math.min(0, Math.max(getWidth() - anchoDibujo, deseado));
        } else {
            offsetX = Math.max(0, (getWidth() - anchoDibujo) / 2);
        }

        if (focoImagen != null && altoDibujo > getHeight()) {
            int deseado = (int) (getHeight() / 2.0 - focoImagen.y * escala);
            offsetY = Math.min(0, Math.max(getHeight() - altoDibujo, deseado));
        } else {
            offsetY = Math.max(0, (getHeight() - altoDibujo) / 2);
        }

        g2.drawImage(imagenMapa, offsetX, offsetY, anchoDibujo, altoDibujo, null);
        ultimaEscala = escala;
        ultimoOffsetX = offsetX;
        ultimoOffsetY = offsetY;

        // Modo enfoque: con una ruta calculada, un velo claro atenúa la imagen y los
        // edificios del recorrido se vuelven a dibujar nítidos encima.
        boolean hayRuta = rutaActual != null && rutaActual.size() > 1;
        if (hayRuta) {
            g2.setColor(new Color(255, 255, 255, 100));
            g2.fillRect(offsetX, offsetY, anchoDibujo, altoDibujo);
            Shape clipOriginal = g2.getClip();
            // Cada edificio del recorrido se vuelve a dibujar nítido completo (su contorno
            // calibrado sobre la imagen), con un borde suavizado para que no quede un corte brusco.
            int rAnillo = (int) Math.max(14, 22 * escala * 1.2) - 2;
            for (String id : rutaActual) {
                Point p = aPantalla(id, escala, offsetX, offsetY);
                if (p == null) continue;
                java.awt.geom.Area zona = new java.awt.geom.Area(
                        new java.awt.geom.Ellipse2D.Double(p.x - rAnillo, p.y - rAnillo, rAnillo * 2.0, rAnillo * 2.0));
                Rectangle huella = HUELLAS.get(id);
                Rectangle enPantalla = null;
                if (huella != null) {
                    enPantalla = new Rectangle((int) (huella.x * escala) + offsetX, (int) (huella.y * escala) + offsetY,
                            (int) (huella.width * escala), (int) (huella.height * escala));
                }
                // Borde suave: capas cada vez más grandes y más transparentes
                int[] expansion = {10, 6, 3};
                float[] alfa = {0.30f, 0.45f, 0.65f};
                Composite compOriginal = g2.getComposite();
                for (int k = 0; k < expansion.length; k++) {
                    java.awt.geom.Area capa = new java.awt.geom.Area(zona);
                    if (enPantalla != null) {
                        int e = (int) Math.max(2, expansion[k] * escala * 1.4);
                        capa.add(new java.awt.geom.Area(new RoundRectangle2D.Double(enPantalla.x - e, enPantalla.y - e,
                                enPantalla.width + 2.0 * e, enPantalla.height + 2.0 * e, 40 * escala + e, 40 * escala + e)));
                    }
                    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alfa[k]));
                    g2.setClip(capa);
                    g2.drawImage(imagenMapa, offsetX, offsetY, anchoDibujo, altoDibujo, null);
                }
                g2.setComposite(compOriginal);
                if (enPantalla != null) {
                    zona.add(new java.awt.geom.Area(new RoundRectangle2D.Double(enPantalla.x, enPantalla.y,
                            enPantalla.width, enPantalla.height, 40 * escala, 40 * escala)));
                }
                g2.setClip(zona);
                g2.drawImage(imagenMapa, offsetX, offsetY, anchoDibujo, altoDibujo, null);
                g2.setClip(clipOriginal);
            }
        }

        dibujarRuta(g2, escala, offsetX, offsetY);
        dibujarMarcadores(g2, escala, offsetX, offsetY);
        // Las distancias van al final para que ningún marcador las tape
        dibujarEtiquetasDistancia(g2, escala, offsetX, offsetY);
        dibujarLeyenda(g2);
        if (hayRuta) dibujarResumen(g2);
        dibujarFichaHover(g2, escala, offsetX, offsetY);

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

    /**
     * Punto (en coordenadas de la imagen original) sobre el que se debe centrar
     * el zoom: el centro de la ruta calculada; si no hay ruta, el centro entre
     * el origen y el destino seleccionados. Devuelve null si no hay nada
     * marcado, en cuyo caso el mapa se centra normalmente.
     */
    private Point calcularFoco() {
        java.util.List<Point> referencias = new java.util.ArrayList<>();

        if (rutaActual != null && !rutaActual.isEmpty()) {
            for (String id : rutaActual) {
                Point p = posiciones.get(id);
                if (p != null) referencias.add(p);
            }
        } else {
            Point o = seleccionOrigen == null ? null : posiciones.get(seleccionOrigen);
            Point d = seleccionDestino == null ? null : posiciones.get(seleccionDestino);
            if (o != null) referencias.add(o);
            if (d != null) referencias.add(d);
        }

        if (referencias.isEmpty()) return null;

        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
        for (Point p : referencias) {
            minX = Math.min(minX, p.x);
            maxX = Math.max(maxX, p.x);
            minY = Math.min(minY, p.y);
            maxY = Math.max(maxY, p.y);
        }
        return new Point((minX + maxX) / 2, (minY + maxY) / 2);
    }

    private Point aPantalla(String id, double escala, int offsetX, int offsetY) {
        Point p = posiciones.get(id);
        if (p == null) return null;
        return new Point((int) (p.x * escala) + offsetX, (int) (p.y * escala) + offsetY);
    }

    private void dibujarRuta(Graphics2D g2, double escala, int offsetX, int offsetY) {
        if (rutaActual == null || rutaActual.size() < 2) return;

        for (int i = 0; i < rutaActual.size() - 1; i++) {
            String idA = rutaActual.get(i);
            String idB = rutaActual.get(i + 1);

            java.util.List<Point> puntos = puntosDelTramo(idA, idB, escala, offsetX, offsetY);
            if (puntos.size() < 2) continue;

            // La línea empieza y termina un poco antes del centro de cada pin,
            // para que toque su borde en vez de atravesar la letra que tiene
            // dibujada encima (el pin es parte de la imagen de fondo).
            puntos = recortarExtremos(puntos, RADIO_PIN * escala);

            Camino tramo = (tramosActuales != null && i < tramosActuales.size()) ? tramosActuales.get(i) : null;
            boolean escaleras = tramo != null && tramo.isTieneEscaleras();

            // Contorno blanco para que la línea se despegue de la foto
            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(11f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            dibujarPolilinea(g2, puntos);

            // Ruta en dorado: continua sin escaleras, punteada con escaleras
            g2.setColor(ORO);
            if (escaleras) {
                g2.setStroke(new BasicStroke(5.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                        10f, new float[]{11f, 8f}, 0f));
            } else {
                g2.setStroke(new BasicStroke(5.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            }
            dibujarPolilinea(g2, puntos);

        }
    }

    /**
     * Contorno aproximado de cada edificio en la imagen original (x, y, ancho, alto),
     * calibrado a mano sobre mapa_campus.png. Se usa en el modo enfoque para mostrar
     * nítido el edificio completo cuando la ruta pasa por él. Para los puntos sin
     * edificio propio (CAF y porterías) cubre su rótulo; la C no tiene edificio visible.
     */
    private static final Map<String, Rectangle> HUELLAS = new HashMap<>();
    static {
        HUELLAS.put("K", new Rectangle(1020, 158, 180, 180));
        HUELLAS.put("L", new Rectangle(1435, 240, 118, 150));
        HUELLAS.put("I", new Rectangle(1265, 272, 130, 112));
        HUELLAS.put("H", new Rectangle(1336, 385, 124, 105));
        HUELLAS.put("E", new Rectangle(1160, 410, 106, 130));
        HUELLAS.put("F", new Rectangle(1226, 452, 110, 142));
        HUELLAS.put("G", new Rectangle(1305, 490, 122, 132));
        HUELLAS.put("D", new Rectangle(905, 385, 172, 205));
        HUELLAS.put("J", new Rectangle(800, 620, 305, 190));
        HUELLAS.put("A", new Rectangle(515, 612, 150, 185));
        HUELLAS.put("B", new Rectangle(600, 625, 148, 175));
        HUELLAS.put("M", new Rectangle(205, 300, 410, 245));
        HUELLAS.put("Templo", new Rectangle(205, 610, 205, 205));
        HUELLAS.put("CAF", new Rectangle(712, 485, 128, 60));
        HUELLAS.put("Porteria 2", new Rectangle(565, 300, 220, 55));
        HUELLAS.put("Porteria 1", new Rectangle(610, 760, 190, 100));
    }

    /**
     * Rótulos impresos en la propia imagen (x, y, ancho, alto en la imagen original).
     * Los números de paso y las distancias de la ruta los evitan para no taparlos.
     */
    private static final Map<String, Rectangle> ROTULOS = new HashMap<>();
    static {
        ROTULOS.put("CAF", new Rectangle(712, 488, 128, 55));
        ROTULOS.put("Porteria 2", new Rectangle(565, 300, 220, 50));
        ROTULOS.put("Porteria 1", new Rectangle(610, 765, 190, 48));
        ROTULOS.put("M", new Rectangle(150, 205, 340, 95));
        ROTULOS.put("Templo", new Rectangle(235, 566, 145, 42));
    }

    /** Puntos que en la imagen son un rótulo (no un pin): se resaltan con un contorno del rótulo. */
    private static final java.util.Set<String> ES_ROTULO = java.util.Set.of("CAF", "Porteria 1", "Porteria 2");

    /** Zonas ocupadas en pantalla en el cuadro actual (marcadores, rótulos, chips, números). */
    private final java.util.List<Rectangle> ocupados = new java.util.ArrayList<>();

    private Rectangle aPantalla(Rectangle r, double escala, int offsetX, int offsetY) {
        return new Rectangle((int) (r.x * escala) + offsetX, (int) (r.y * escala) + offsetY,
                (int) Math.ceil(r.width * escala), (int) Math.ceil(r.height * escala));
    }

    private static int solapamiento(Rectangle r, java.util.List<Rectangle> otros) {
        int total = 0;
        for (Rectangle o : otros) {
            Rectangle i = r.intersection(o);
            if (!i.isEmpty()) total += i.width * i.height;
        }
        return total;
    }

    /** Punto de la polilínea en la fracción t (0 = inicio, 1 = fin) de su largo. */
    private Point puntoEnFraccion(java.util.List<Point> pts, double t) {
        double largo = 0;
        for (int i = 0; i < pts.size() - 1; i++) largo += distancia(pts.get(i), pts.get(i + 1));
        double objetivo = largo * t, acum = 0;
        for (int i = 0; i < pts.size() - 1; i++) {
            double seg = distancia(pts.get(i), pts.get(i + 1));
            if (acum + seg >= objetivo && seg > 0) {
                double f = (objetivo - acum) / seg;
                Point a = pts.get(i), b = pts.get(i + 1);
                return new Point((int) (a.x + (b.x - a.x) * f), (int) (a.y + (b.y - a.y) * f));
            }
            acum += seg;
        }
        return pts.get(pts.size() - 1);
    }

    /** Radio aproximado (en píxeles de la imagen original) del pin dibujado en el mapa. */
    private static final double RADIO_PIN = 22.0;

    /**
     * Devuelve una copia de la polilínea con el primer y el último punto
     * desplazados hacia adentro, a lo largo de su segmento, la distancia
     * indicada. En tramos muy cortos el recorte se reduce para que la línea
     * no llegue a desaparecer.
     */
    private java.util.List<Point> recortarExtremos(java.util.List<Point> puntos, double margen) {
        if (puntos.size() < 2 || margen <= 0) return puntos;

        java.util.List<Point> resultado = new java.util.ArrayList<>(puntos);
        double largoTotal = 0;
        for (int i = 0; i < resultado.size() - 1; i++) {
            largoTotal += distancia(resultado.get(i), resultado.get(i + 1));
        }
        // Como mucho se recorta el 35% del largo en cada extremo, para que
        // los tramos muy cortos conserven una línea visible.
        double margenReal = Math.min(margen, largoTotal * 0.35);
        if (margenReal <= 0) return resultado;

        resultado.set(0, moverHaciaAdentro(resultado.get(0), resultado.get(1), margenReal));
        int ult = resultado.size() - 1;
        resultado.set(ult, moverHaciaAdentro(resultado.get(ult), resultado.get(ult - 1), margenReal));
        return resultado;
    }

    /** Mueve el punto "desde" una distancia fija en dirección a "hacia". */
    private Point moverHaciaAdentro(Point desde, Point hacia, double distanciaAMover) {
        double dx = hacia.x - desde.x;
        double dy = hacia.y - desde.y;
        double largo = Math.hypot(dx, dy);
        if (largo == 0) return desde;
        double f = distanciaAMover / largo;
        return new Point((int) (desde.x + dx * f), (int) (desde.y + dy * f));
    }

    /**
     * Dibuja la distancia de cada tramo en el punto medio real del recorrido.
     * Se llama después de los marcadores para que las etiquetas queden encima
     * y siempre se puedan leer.
     */
    private void dibujarEtiquetasDistancia(Graphics2D g2, double escala, int offsetX, int offsetY) {
        if (rutaActual == null || rutaActual.size() < 2 || tramosActuales == null) return;
        g2.setFont(new Font(EstiloUPB.FAMILIA, Font.BOLD, 11));
        FontMetrics fm = g2.getFontMetrics();
        double[] posiciones = {0.5, 0.38, 0.62, 0.28, 0.72, 0.18, 0.82};

        for (int i = 0; i < rutaActual.size() - 1 && i < tramosActuales.size(); i++) {
            java.util.List<Point> puntos = puntosDelTramo(
                    rutaActual.get(i), rutaActual.get(i + 1), escala, offsetX, offsetY);
            if (puntos.size() < 2) continue;
            String txt = String.format("%.0f m", tramosActuales.get(i).getDistancia());
            int w = fm.stringWidth(txt) + 12, h = 18;

            // Primera posición libre a lo largo del tramo; si ninguna lo está, la de menor choque
            Rectangle elegido = null;
            int mejorChoque = Integer.MAX_VALUE;
            buscar:
            for (int desplazamiento : new int[]{0, 1, -1}) {
                for (double t : posiciones) {
                    Point m = puntoEnFraccion(puntos, t);
                    if (desplazamiento != 0) {   // arriba o abajo de la línea (perpendicular al tramo)
                        Point a = puntoEnFraccion(puntos, Math.max(0, t - 0.05)), b = puntoEnFraccion(puntos, Math.min(1, t + 0.05));
                        double dx = b.x - a.x, dy = b.y - a.y, largo = Math.max(1, Math.hypot(dx, dy));
                        m = new Point((int) (m.x - dy / largo * (h + 4) * desplazamiento),
                                      (int) (m.y + dx / largo * (h + 4) * desplazamiento));
                    }
                    Rectangle r = new Rectangle(m.x - w / 2, m.y - h / 2, w, h);
                    int choque = solapamiento(r, ocupados);
                    if (choque < mejorChoque) { mejorChoque = choque; elegido = r; }
                    if (choque == 0) break buscar;
                }
            }
            ocupados.add(new Rectangle(elegido.x - 3, elegido.y - 2, elegido.width + 6, elegido.height + 4));

            RoundRectangle2D pill = new RoundRectangle2D.Double(elegido.x, elegido.y, w, h, 9, 9);
            g2.setColor(new Color(255, 255, 255, 240));
            g2.fill(pill);
            g2.setColor(ORO);
            g2.setStroke(new BasicStroke(1.6f));
            g2.draw(pill);
            g2.setColor(UIColores.TEXTO_OSCURO);
            g2.drawString(txt, elegido.x + 6, elegido.y + (h + fm.getAscent()) / 2 - 2);
        }
    }

    /**
     * Marcador para un edificio que no aparece dibujado en la imagen: una
     * chincheta con el nombre, al estilo de las etiquetas del propio mapa.
     */
    private void dibujarMarcadorPropio(Graphics2D g2, Point p, String identificador, String nombre) {
        int radio = 11;

        // Punta de la chincheta
        g2.setColor(UIColores.PRIMARIO);
        java.awt.geom.Path2D punta = new java.awt.geom.Path2D.Double();
        punta.moveTo(p.x - 6, p.y - 2);
        punta.lineTo(p.x + 6, p.y - 2);
        punta.lineTo(p.x, p.y + 10);
        punta.closePath();
        g2.fill(punta);

        // Círculo superior
        g2.setColor(Color.WHITE);
        g2.fillOval(p.x - radio - 2, p.y - radio * 2 - 4, (radio + 2) * 2, (radio + 2) * 2);
        g2.setColor(UIColores.PRIMARIO);
        g2.fillOval(p.x - radio, p.y - radio * 2 - 2, radio * 2, radio * 2);

        // Identificador dentro del círculo (se ajusta si tiene varios caracteres)
        String texto = identificador == null ? "?" : identificador.trim();
        int tam = texto.length() <= 1 ? 12 : (texto.length() == 2 ? 10 : 8);
        g2.setFont(new Font(EstiloUPB.FAMILIA, Font.BOLD, tam));
        FontMetrics fmI = g2.getFontMetrics();
        g2.setColor(Color.WHITE);
        g2.drawString(texto, p.x - fmI.stringWidth(texto) / 2, p.y - radio + 2);

        // Etiqueta con el nombre completo
        g2.setFont(new Font(EstiloUPB.FAMILIA, Font.BOLD, 11));
        FontMetrics fm = g2.getFontMetrics();
        int ancho = fm.stringWidth(nombre);
        int ex = p.x - ancho / 2;
        int ey = p.y - radio * 2 - 12;

        g2.setColor(UIColores.PRIMARIO);
        g2.fill(new RoundRectangle2D.Double(ex - 8, ey - 14, ancho + 16, 19, 8, 8));
        g2.setColor(Color.WHITE);
        g2.drawString(nombre, ex, ey);
    }

    /** Punto que queda a la mitad de la longitud total de la polilínea. */
    private Point puntoMedio(java.util.List<Point> puntos) {
        double total = 0;
        for (int i = 0; i < puntos.size() - 1; i++) {
            total += distancia(puntos.get(i), puntos.get(i + 1));
        }
        double objetivo = total / 2.0;
        double acumulado = 0;

        for (int i = 0; i < puntos.size() - 1; i++) {
            Point a = puntos.get(i), b = puntos.get(i + 1);
            double d = distancia(a, b);
            if (acumulado + d >= objetivo) {
                double f = d == 0 ? 0 : (objetivo - acumulado) / d;
                return new Point((int) (a.x + (b.x - a.x) * f),
                                 (int) (a.y + (b.y - a.y) * f));
            }
            acumulado += d;
        }
        return puntos.get(puntos.size() / 2);
    }

    private void dibujarPolilinea(Graphics2D g2, java.util.List<Point> puntos) {
        for (int i = 0; i < puntos.size() - 1; i++) {
            Point a = puntos.get(i);
            Point b = puntos.get(i + 1);
            g2.drawLine(a.x, a.y, b.x, b.y);
        }
    }

    private void dibujarMarcadores(Graphics2D g2, double escala, int offsetX, int offsetY) {
        // Marcadores propios de los edificios agregados desde la aplicación
        for (Map.Entry<String, String> agregado : edificiosAgregados.entrySet()) {
            Point p = aPantalla(agregado.getKey(), escala, offsetX, offsetY);
            if (p != null) {
                // Dentro del círculo va el identificador del edificio (por
                // ejemplo "N"), no la inicial del nombre.
                dibujarMarcadorPropio(g2, p, agregado.getKey(), agregado.getValue());
            }
        }

        // Zonas que no se deben tapar: todos los marcadores y los rótulos de la imagen
        ocupados.clear();
        double rPin = Math.max(10, RADIO_PIN * escala * 1.1);
        for (Map.Entry<String, Point> entrada : posiciones.entrySet()) {
            Point p = aPantalla(entrada.getKey(), escala, offsetX, offsetY);
            if (p != null) ocupados.add(new Rectangle((int) (p.x - rPin), (int) (p.y - rPin), (int) (rPin * 2), (int) (rPin * 2)));
        }
        for (Rectangle r : ROTULOS.values()) ocupados.add(aPantalla(r, escala, offsetX, offsetY));

        boolean hayRuta = rutaActual != null && rutaActual.size() > 1;
        int radio = (int) Math.max(14, 22 * escala * 1.2);
        java.util.List<String> conNumero = new java.util.ArrayList<>();

        for (Map.Entry<String, Point> entrada : posiciones.entrySet()) {
            String id = entrada.getKey();
            Point p = aPantalla(id, escala, offsetX, offsetY);
            if (p == null) continue;

            boolean esOrigen = id.equals(seleccionOrigen);
            boolean esDestino = id.equals(seleccionDestino);
            boolean enRuta = rutaActual != null && rutaActual.contains(id);
            if (!esOrigen && !esDestino && !enRuta) continue;

            boolean inicio = hayRuta ? id.equals(rutaActual.get(0)) : esOrigen;
            boolean fin = hayRuta ? id.equals(rutaActual.get(rutaActual.size() - 1)) : esDestino;
            Color color = inicio ? INICIO : fin ? ORO : UIColores.PRIMARIO;

            // Resaltado: anillo alrededor del pin o, si el punto es un rótulo (CAF, porterías),
            // un contorno redondeado alrededor del rótulo completo para no taparlo
            Rectangle zona;
            if (ES_ROTULO.contains(id)) {
                zona = aPantalla(ROTULOS.get(id), escala, offsetX, offsetY);
                // El rótulo se vuelve a dibujar encima de la línea de la ruta: la línea "pasa por debajo"
                if (imagenMapa != null) {
                    Shape clipPrevio = g2.getClip();
                    g2.clip(new RoundRectangle2D.Double(zona.x, zona.y, zona.width, zona.height, 12, 12));
                    g2.drawImage(imagenMapa, offsetX, offsetY, (int) (imagenMapa.getWidth() * escala),
                            (int) (imagenMapa.getHeight() * escala), null);
                    g2.setClip(clipPrevio);
                }
                zona.grow(5, 5);
                RoundRectangle2D contorno = new RoundRectangle2D.Double(zona.x, zona.y, zona.width, zona.height, 16, 16);
                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke(6f));
                g2.draw(contorno);
                g2.setColor(color);
                g2.setStroke(new BasicStroke(3.4f));
                g2.draw(contorno);
            } else {
                zona = new Rectangle(p.x - radio, p.y - radio, radio * 2, radio * 2);
                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke(6f));
                g2.drawOval(zona.x, zona.y, zona.width, zona.height);
                g2.setColor(color);
                g2.setStroke(new BasicStroke(3.4f));
                g2.drawOval(zona.x, zona.y, zona.width, zona.height);
            }
            ocupados.add(new Rectangle(zona.x - 3, zona.y - 3, zona.width + 6, zona.height + 6));

            if (inicio || fin) {
                String etiqueta = inicio ? "Origen" : "Destino";
                g2.setFont(new Font(EstiloUPB.FAMILIA, Font.BOLD, 12));
                FontMetrics fm = g2.getFontMetrics();
                int ancho = fm.stringWidth(etiqueta) + 16;
                int ex = p.x - ancho / 2;
                int ey = zona.y + zona.height + 6;
                g2.setColor(Color.WHITE);
                g2.fill(new RoundRectangle2D.Double(ex - 2, ey - 2, ancho + 4, 24, 14, 14));
                g2.setColor(color);
                g2.fill(new RoundRectangle2D.Double(ex, ey, ancho, 20, 12, 12));
                g2.setColor(inicio ? Color.WHITE : UIColores.PRIMARIO_OSCURO);
                g2.drawString(etiqueta, ex + 8, ey + 15);
                ocupados.add(new Rectangle(ex - 2, ey - 2, ancho + 4, 24));
            }
            if (hayRuta) conNumero.add(id);
        }

        // Número de paso: en la primera esquina libre, por fuera del anillo y del rótulo
        int d = 22;
        g2.setFont(new Font(EstiloUPB.FAMILIA, Font.BOLD, 12));
        FontMetrics fmn = g2.getFontMetrics();
        for (String id : conNumero) {
            Point p = aPantalla(id, escala, offsetX, offsetY);
            Rectangle obst = new Rectangle(p.x - radio, p.y - radio, radio * 2, radio * 2);
            if (ROTULOS.containsKey(id)) {
                Rectangle rot = aPantalla(ROTULOS.get(id), escala, offsetX, offsetY);
                if (ES_ROTULO.contains(id)) rot.grow(5, 5);
                obst = ES_ROTULO.contains(id) ? rot : obst.union(rot);
            }
            int[][] esquinas = {
                    {obst.x - d / 2 - 2, obst.y - d / 2 - 2}, {obst.x + obst.width + d / 2 + 2, obst.y - d / 2 - 2},
                    {obst.x - d / 2 - 2, obst.y + obst.height / 2}, {obst.x + obst.width + d / 2 + 2, obst.y + obst.height / 2},
                    {obst.x - d / 2 - 2, obst.y + obst.height + d / 2 + 2}, {obst.x + obst.width + d / 2 + 2, obst.y + obst.height + d / 2 + 2}};
            int cx = esquinas[0][0], cy = esquinas[0][1];
            int mejor = Integer.MAX_VALUE;
            for (int[] e : esquinas) {
                Rectangle r = new Rectangle(e[0] - d / 2 - 2, e[1] - d / 2 - 2, d + 4, d + 4);
                java.util.List<Rectangle> otros = new java.util.ArrayList<>(ocupados);
                otros.removeIf(o -> o.contains(p));   // su propio marcador no cuenta
                int choque = solapamiento(r, otros);
                if (choque < mejor) { mejor = choque; cx = e[0]; cy = e[1]; }
                if (choque == 0) break;
            }
            g2.setColor(Color.WHITE);
            g2.fillOval(cx - d / 2 - 2, cy - d / 2 - 2, d + 4, d + 4);
            g2.setColor(UIColores.PRIMARIO);
            g2.fillOval(cx - d / 2, cy - d / 2, d, d);
            g2.setColor(Color.WHITE);
            String n = String.valueOf(rutaActual.indexOf(id) + 1);
            g2.drawString(n, cx - fmn.stringWidth(n) / 2, cy + fmn.getAscent() / 2 - 1);
            ocupados.add(new Rectangle(cx - d / 2 - 3, cy - d / 2 - 3, d + 6, d + 6));
        }
    }

    // ==================== Ficha, resumen y leyenda ====================
    /** Detecta el marcador (pin) bajo el mouse. */
    private void actualizarHover(Point m) {
        String encontrado = null;
        if (m != null && alUbicar == null && imagenMapa != null) {
            double radio = Math.max(14, RADIO_PIN * ultimaEscala * 1.3);
            double mejor = Double.MAX_VALUE;
            for (String id : posiciones.keySet()) {
                Point p = aPantalla(id, ultimaEscala, ultimoOffsetX, ultimoOffsetY);
                if (p == null) continue;
                double d = p.distance(m);
                if (d <= radio && d < mejor) { mejor = d; encontrado = id; }
            }
        }
        boolean cambio = !java.util.Objects.equals(encontrado, hoverEdificio);
        hoverEdificio = encontrado;
        posMouse = m;
        setCursor(alUbicar != null ? Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR)
                : encontrado != null ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : Cursor.getDefaultCursor());
        if (cambio || encontrado != null) repaint();
    }

    private String nombreDe(String id) {
        if (edificiosAgregados.containsKey(id)) return edificiosAgregados.get(id);
        if (grafo != null && grafo.getEdificios().get(id) != null) return grafo.getEdificios().get(id).getNombre();
        return id;
    }

    /** Anillo y ficha (nombre, conexiones y lugares) del marcador bajo el mouse. */
    private void dibujarFichaHover(Graphics2D g2, double escala, int offsetX, int offsetY) {
        if (hoverEdificio == null || posMouse == null) return;
        Point p = aPantalla(hoverEdificio, escala, offsetX, offsetY);
        if (p == null) return;
        int radio = (int) Math.max(14, 22 * escala * 1.2);
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(6f));
        g2.drawOval(p.x - radio, p.y - radio, radio * 2, radio * 2);
        g2.setColor(UIColores.PRIMARIO);
        g2.setStroke(new BasicStroke(2.6f));
        g2.drawOval(p.x - radio, p.y - radio, radio * 2, radio * 2);

        String titulo = nombreDe(hoverEdificio);
        String detalle = "";
        if (grafo != null && grafo.getEdificios().get(hoverEdificio) != null) {
            int con = grafo.getAdyacentes(hoverEdificio).size();
            int lug = grafo.getEdificios().get(hoverEdificio).getLugares().size();
            detalle = con + (con == 1 ? " conexión" : " conexiones") + "  ·  " + lug + (lug == 1 ? " lugar" : " lugares");
        }
        tarjeta(g2, titulo, detalle, posMouse.x + 16, posMouse.y + 14, true);
    }

    /** Tarjeta flotante con el resumen de la ruta, en la esquina inferior izquierda. */
    private void dibujarResumen(Graphics2D g2) {
        double metros = 0;
        if (tramosActuales != null) for (Camino c : tramosActuales) metros += c.getDistancia();
        String tiempo = tramosActuales != null
                ? modelo.EstimadorTiempo.formatear(modelo.EstimadorTiempo.calcularMinutos(tramosActuales)) : "";
        int n = rutaActual.size() - 1;
        String titulo = nombreDe(rutaActual.get(0)) + "  \u2192  " + nombreDe(rutaActual.get(rutaActual.size() - 1));
        String detalle = String.format("%.0f m", metros) + "  ·  " + tiempo + "  ·  " + n + (n == 1 ? " tramo" : " tramos");
        tarjeta(g2, titulo, detalle, 12, getHeight() - 12 - 50, false);
    }

    /** Tarjeta blanca curva con acento vinotinto (título y detalle). */
    private void tarjeta(Graphics2D g2, String titulo, String detalle, int x, int y, boolean ajustar) {
        Font fT = new Font(EstiloUPB.FAMILIA, Font.BOLD, 13), fD = new Font(EstiloUPB.FAMILIA, Font.PLAIN, 12);
        int w = Math.max(g2.getFontMetrics(fT).stringWidth(titulo), g2.getFontMetrics(fD).stringWidth(detalle)) + 28;
        int h = 50;
        if (ajustar) {
            if (x + w > getWidth() - 6) x = x - w - 30;
            if (y + h > getHeight() - 6) y = y - h - 26;
        }
        g2.setColor(new Color(0, 0, 0, 40));
        g2.fillRoundRect(x + 2, y + 3, w, h, 14, 14);
        g2.setColor(Color.WHITE);
        g2.fillRoundRect(x, y, w, h, 14, 14);
        g2.setColor(UIColores.PRIMARIO);
        g2.fillRoundRect(x, y + 9, 4, h - 18, 3, 3);
        g2.setColor(new Color(0xD5, 0xDC, 0xE6));
        g2.setStroke(new BasicStroke(1f));
        g2.drawRoundRect(x, y, w, h, 14, 14);
        g2.setFont(fT);
        g2.setColor(UIColores.TEXTO_OSCURO);
        g2.drawString(titulo, x + 16, y + 21);
        g2.setFont(fD);
        g2.setColor(UIColores.TEXTO_MUTED);
        g2.drawString(detalle, x + 16, y + 39);
    }

    /** Leyenda flotante en la esquina superior izquierda. */
    /** Convenciones abiertas o minimizadas (clic en su título). */
    private boolean leyendaAbierta = true;
    private Rectangle zonaLeyenda;

    private void dibujarLeyenda(Graphics2D g2) {
        int x = 12, y = 12, w = 176, h = 132;
        if (!leyendaAbierta) {
            zonaLeyenda = EstiloUPB.dibujarLeyendaPlegada(g2, x, y);
            return;
        }
        zonaLeyenda = new Rectangle(x, y, w, 28);
        g2.setColor(new Color(0, 0, 0, 40));
        g2.fillRoundRect(x + 2, y + 3, w, h, 14, 14);
        g2.setColor(new Color(255, 255, 255, 242));
        g2.fillRoundRect(x, y, w, h, 14, 14);
        g2.setColor(new Color(0xD5, 0xDC, 0xE6));
        g2.setStroke(new BasicStroke(1f));
        g2.drawRoundRect(x, y, w, h, 14, 14);
        g2.setFont(new Font(EstiloUPB.FAMILIA, Font.BOLD, 12));
        g2.setColor(UIColores.TEXTO_OSCURO);
        g2.drawString("Convenciones", x + 12, y + 20);
        EstiloUPB.dibujarFlechaPlegar(g2, x + w - 16, y + 16, true);
        g2.setFont(new Font(EstiloUPB.FAMILIA, Font.PLAIN, 11));
        int ly = y + 38;
        g2.setColor(ORO);
        g2.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawLine(x + 12, ly, x + 40, ly);
        g2.setColor(UIColores.TEXTO_MUTED); g2.drawString("Ruta sin escaleras", x + 50, ly + 4);
        ly += 18;
        g2.setColor(ORO);
        g2.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10f, new float[]{7f, 6f}, 0f));
        g2.drawLine(x + 12, ly, x + 40, ly);
        g2.setColor(UIColores.TEXTO_MUTED); g2.drawString("Tramo con escaleras", x + 50, ly + 4);
        ly += 19;
        g2.setStroke(new BasicStroke(3f));
        g2.setColor(INICIO); g2.drawOval(x + 19, ly - 7, 14, 14);
        g2.setColor(UIColores.TEXTO_MUTED); g2.drawString("Origen", x + 50, ly + 4);
        ly += 19;
        g2.setColor(ORO); g2.drawOval(x + 19, ly - 7, 14, 14);
        g2.setColor(UIColores.TEXTO_MUTED); g2.drawString("Destino", x + 50, ly + 4);
        ly += 19;
        g2.setColor(UIColores.PRIMARIO); g2.fillOval(x + 18, ly - 8, 16, 16);
        g2.setColor(Color.WHITE); g2.setFont(new Font(EstiloUPB.FAMILIA, Font.BOLD, 10)); g2.drawString("1", x + 23, ly + 4);
        g2.setFont(new Font(EstiloUPB.FAMILIA, Font.PLAIN, 11));
        g2.setColor(UIColores.TEXTO_MUTED); g2.drawString("Orden de paso", x + 50, ly + 4);
    }
}
