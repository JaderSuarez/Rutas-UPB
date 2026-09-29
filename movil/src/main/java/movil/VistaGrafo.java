package movil;

import controlador.CampusControlador;
import modelo.Camino;
import org.teavm.jso.core.JSArray;
import org.teavm.jso.core.JSNumber;
import persistencia.RepositorioEstado;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Vista técnica del grafo del campus para el celular: la misma que vista.PanelMapa
 * en la app de escritorio (colores, curvas, nodos, ruta, números de paso,
 * distancias sin encimarse y convenciones), dibujada en un canvas táctil.
 *
 * Las posiciones están en el lienzo de referencia del escritorio (836 x 720) y
 * se escalan con el zoom. Los tamaños siguen las mismas fórmulas del escritorio,
 * con mínimos un poco menores para que el grafo completo quepa en un celular.
 */
final class VistaGrafo extends LienzoTactil {

    // Colores de vista.UIColores y vista.PanelMapa
    private static final String ORO = "#D4AF37";
    private static final String PRIMARIO = "#800020";
    private static final String PRIMARIO_OSCURO = "#5A0016";
    private static final String NODO_FONDO = "#F7D9DE";
    private static final String PUNTO_FONDO = "#FDE9C8";
    private static final String PUNTO_BORDE = "#B5862B";
    private static final String PUNTO_TEXTO = "#6B4A10";
    private static final String INICIO_FONDO = "#D1FAE5";
    private static final String INICIO_BORDE = "#0B7A55";
    private static final String DESTINO_FONDO = "#FEF3C7";
    private static final String GRIS_PUNTOS = "#DCE1E8";
    private static final String CAMINO_ACCESIBLE = "rgb(218,41,122)";
    private static final String CAMINO_ESCALERAS = "#000000";
    private static final String CAMINO_BLOQUEADO = "#C0C0C0";
    private static final String TEXTO_OSCURO = "#1E293B";
    private static final String TEXTO_MUTED = "#64748B";
    private static final String FUENTE = "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif";

    private final CampusControlador controlador;
    private List<String> ruta = new ArrayList<>();
    private String seleccionOrigen, seleccionDestino;
    private String nodoTocado;
    private Camino caminoTocado;
    private boolean mostrarDistancias = true;
    private boolean resaltarConexiones = true;
    private boolean leyendaAbierta = true;
    private double[] zonaLeyenda;

    private Consumer<String> alTocarEdificio = id -> { };
    private Consumer<Camino> alTocarCamino = c -> { };

    VistaGrafo(CampusControlador controlador) {
        this.controlador = controlador;
    }

    @Override protected double anchoMundo() { return CoordenadasGrafo.ANCHO + CoordenadasGrafo.MARGEN * 2; }
    @Override protected double altoMundo() { return CoordenadasGrafo.ALTO + CoordenadasGrafo.MARGEN * 2; }
    @Override protected String colorFondo() { return "rgb(245,247,250)"; }

    @Override
    protected double escalaMaxima() {
        return Math.max(1.6, escalaMinima() * 3);   // como en escritorio: hasta 3 veces el tamaño ajustado
    }

    @Override
    protected void vistaInicial() {
        if (altoCss < 520) leyendaAbierta = false;   // celular en horizontal: poco alto disponible
        if (ruta.size() > 1) enfocarRuta(); else vistaGeneral();
    }

    /** Todo el grafo a la vista, debajo de los controles. */
    void vistaGeneral() {
        encuadrar(0, 0, anchoMundo(), altoMundo(), 0);
    }

    /** Acerca el grafo a los edificios de la ruta. */
    void enfocarRuta() {
        if (ruta.size() < 2) return;
        if (!tieneTamano()) {
            reiniciarVistaAlMostrar();
            return;
        }
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
        Map<String, int[]> pos = posiciones();
        for (String id : ruta) {
            int[] p = pos.get(id);
            if (p == null) continue;
            minX = Math.min(minX, p[0]); maxX = Math.max(maxX, p[0]);
            minY = Math.min(minY, p[1]); maxY = Math.max(maxY, p[1]);
        }
        int m = CoordenadasGrafo.MARGEN;
        encuadrar(minX + m, minY + m, maxX + m, maxY + m, 60);
    }

    void setRuta(List<String> ruta) {
        this.ruta = ruta == null ? new ArrayList<>() : new ArrayList<>(ruta);
        claveEtiquetas = null;
        redibujar();
    }

    void setSeleccion(String origen, String destino) {
        this.seleccionOrigen = origen;
        this.seleccionDestino = destino;
        redibujar();
    }

    void setMostrarDistancias(boolean mostrar) { mostrarDistancias = mostrar; redibujar(); }
    void setResaltarConexiones(boolean resaltar) { resaltarConexiones = resaltar; redibujar(); }
    boolean isMostrarDistancias() { return mostrarDistancias; }
    boolean isResaltarConexiones() { return resaltarConexiones; }

    void setAlTocarEdificio(Consumer<String> accion) { alTocarEdificio = accion; }
    void setAlTocarCamino(Consumer<Camino> accion) { alTocarCamino = accion; }

    /** Quita el resaltado del edificio o camino tocado. */
    void limpiarSeleccion() {
        nodoTocado = null;
        caminoTocado = null;
        redibujar();
    }

    /** El grafo pudo cambiar (bloqueos, puntos nuevos): recalcular las distancias. */
    void grafoCambiado() {
        claveEtiquetas = null;
        redibujar();
    }

    // ==================== Geometría ====================

    /** Posiciones del escritorio más las de los puntos agregados por el administrador. */
    private Map<String, int[]> posiciones() {
        Map<String, int[]> todas = new LinkedHashMap<>();
        for (String id : controlador.getGrafo().getEdificios().keySet()) {
            int[] p = CoordenadasGrafo.POSICIONES.get(id);
            if (p != null) todas.put(id, p);
        }
        for (RepositorioEstado.EdificioPersonalizado e : controlador.getEdificiosPersonalizados()) {
            if (controlador.getGrafo().existeEdificio(e.id)) todas.put(e.id, new int[]{e.xGrafo, e.yGrafo});
        }
        return todas;
    }

    /**
     * Punto en pantalla SIN el desplazamiento (offX, offY): así las distancias ya
     * ubicadas siguen sirviendo al arrastrar, y solo se recalculan al hacer zoom.
     */
    private double[] local(double baseX, double baseY) {
        return new double[]{(baseX + CoordenadasGrafo.MARGEN) * escala, (baseY + CoordenadasGrafo.MARGEN) * escala};
    }

    private double[] local(String id, Map<String, int[]> pos) {
        int[] p = pos.get(id);
        return p == null ? null : local(p[0], p[1]);
    }

    private double tam(double valor, double minimo) {
        return Math.max(minimo, valor * escala);
    }

    private static boolean esPuntoDeReferencia(String id) {
        return id.length() > 1;
    }

    private static String textoNodo(String id) {
        return id.startsWith("Porteria") ? id.replace("Porteria", "Portería") : id;
    }

    private String fuenteNodo() {
        return "bold " + Math.round(tam(13, 8)) + "px " + FUENTE;
    }

    /** Caja del nodo {x, y, ancho, alto} en coordenadas locales; círculo si ancho == alto y no es referencia. */
    private double[] cajaNodo(String id, double[] p, double extra) {
        double r = tam(19, 10) + extra;
        if (!esPuntoDeReferencia(id)) return new double[]{p[0] - r, p[1] - r, r * 2, r * 2};
        ctx.setFont(fuenteNodo());
        double ancho = ctx.measureText(textoNodo(id)).getWidth() + tam(22, 12) + extra * 2;
        double alto = r * 1.6;
        return new double[]{p[0] - ancho / 2, p[1] - alto / 2, ancho, alto};
    }

    private static boolean esTramo(String idA, String idB, String uno, String otro) {
        return (uno.equals(idA) && otro.equals(idB)) || (otro.equals(idA) && uno.equals(idB));
    }

    /** Las tres aristas curvas del escritorio {desde, control1, control2, hasta}, o null si es recta. */
    private double[][] controlesCurva(String idA, String idB, double[] pA, double[] pB) {
        if (esTramo(idA, idB, "M", "Porteria 1")) {
            boolean m = "M".equals(idA);
            return new double[][]{m ? pA : pB, local(-40, 520), local(210, 715), m ? pB : pA};
        } else if (esTramo(idA, idB, "M", "Templo")) {
            boolean m = "M".equals(idA);
            return new double[][]{m ? pA : pB, local(55, 430), local(105, 545), m ? pB : pA};
        } else if (esTramo(idA, idB, "K", "Porteria 2")) {
            boolean k = "K".equals(idA);
            return new double[][]{k ? pA : pB, local(520, 260), local(330, 205), k ? pB : pA};
        }
        return null;
    }

    private double[] puntoEn(String idA, String idB, double[] pA, double[] pB, double t) {
        double[][] c = controlesCurva(idA, idB, pA, pB);
        if (c == null) return new double[]{pA[0] + (pB[0] - pA[0]) * t, pA[1] + (pB[1] - pA[1]) * t};
        double u = 1 - t;
        double a = u * u * u, b = 3 * u * u * t, d = 3 * u * t * t, e = t * t * t;
        return new double[]{a * c[0][0] + b * c[1][0] + d * c[2][0] + e * c[3][0],
                            a * c[0][1] + b * c[1][1] + d * c[2][1] + e * c[3][1]};
    }

    /** La arista como línea quebrada (las curvas se aproximan con 24 tramos). */
    private List<double[]> trazo(String a, String b, double[] pA, double[] pB) {
        List<double[]> puntos = new ArrayList<>();
        if (controlesCurva(a, b, pA, pB) == null) {
            puntos.add(pA);
            puntos.add(pB);
        } else {
            for (int i = 0; i <= 24; i++) puntos.add(puntoEn(a, b, pA, pB, i / 24.0));
        }
        return puntos;
    }

    private void trazarArista(String a, String b, double[] pA, double[] pB) {
        double[][] c = controlesCurva(a, b, pA, pB);
        ctx.beginPath();
        ctx.moveTo(pA[0] + offX, pA[1] + offY);
        if (c == null) {
            ctx.lineTo(pB[0] + offX, pB[1] + offY);
        } else {
            ctx.moveTo(c[0][0] + offX, c[0][1] + offY);
            ctx.bezierCurveTo(c[1][0] + offX, c[1][1] + offY, c[2][0] + offX, c[2][1] + offY,
                    c[3][0] + offX, c[3][1] + offY);
        }
    }

    private List<Camino> caminosUnicos() {
        List<Camino> lista = new ArrayList<>();
        Set<String> vistos = new HashSet<>();
        for (String id : controlador.getGrafo().getEdificios().keySet()) {
            for (Camino c : controlador.getGrafo().getAdyacentes(id)) {
                if (vistos.add(CoordenadasMapa.clave(c.getOrigenId(), c.getDestinoId()))) lista.add(c);
            }
        }
        return lista;
    }

    private boolean esAristaDeRuta(String a, String b) {
        for (int i = 0; i < ruta.size() - 1; i++) {
            if (esTramo(a, b, ruta.get(i), ruta.get(i + 1))) return true;
        }
        return false;
    }

    // ==================== Distancias sin encimarse ====================

    private static final class Etiqueta {
        final Camino camino; final double[] zona; final boolean pequena;
        Etiqueta(Camino camino, double[] zona, boolean pequena) {
            this.camino = camino; this.zona = zona; this.pequena = pequena;
        }
    }

    private List<Object> claveEtiquetas;
    private List<Etiqueta> etiquetas = new ArrayList<>();

    private List<Object> claveEtiquetas(List<Camino> caminos, Map<String, int[]> pos) {
        List<Object> clave = new ArrayList<>();
        clave.add(escala);
        for (Map.Entry<String, int[]> e : pos.entrySet()) { clave.add(e.getKey()); clave.add(e.getValue()[0]); clave.add(e.getValue()[1]); }
        for (Camino c : caminos) { clave.add(c.getOrigenId()); clave.add(c.getDestinoId()); clave.add(c.getDistancia()); }
        clave.add(new ArrayList<>(ruta));
        return clave;
    }

    private static double areaInterseccion(double[] a, double[] b) {
        double w = Math.min(a[0] + a[2], b[0] + b[2]) - Math.max(a[0], b[0]);
        double h = Math.min(a[1] + a[3], b[1] + b[3]) - Math.max(a[1], b[1]);
        return w > 0 && h > 0 ? w * h : 0;
    }

    /** true si algún tramo de la línea (con 1 px de grosor a cada lado) toca el rectángulo. */
    private static boolean lineaTocaRect(List<double[]> linea, double[] r) {
        double x0 = r[0] - 1, y0 = r[1] - 1, x1 = r[0] + r[2] + 1, y1 = r[1] + r[3] + 1;
        for (int i = 0; i < linea.size() - 1; i++) {
            if (segmentoTocaRect(linea.get(i), linea.get(i + 1), x0, y0, x1, y1)) return true;
        }
        return false;
    }

    /** Recorte de Liang-Barsky: ¿el segmento pasa por el rectángulo? */
    private static boolean segmentoTocaRect(double[] a, double[] b, double x0, double y0, double x1, double y1) {
        double dx = b[0] - a[0], dy = b[1] - a[1];
        double[] p = {-dx, dx, -dy, dy};
        double[] q = {a[0] - x0, x1 - a[0], a[1] - y0, y1 - a[1]};
        double t0 = 0, t1 = 1;
        for (int i = 0; i < 4; i++) {
            if (p[i] == 0) {
                if (q[i] < 0) return false;
            } else {
                double t = q[i] / p[i];
                if (p[i] < 0) t0 = Math.max(t0, t); else t1 = Math.min(t1, t);
                if (t0 > t1) return false;
            }
        }
        return true;
    }

    /**
     * Misma regla que el escritorio: cada distancia va sobre su arista o pegada a ella,
     * sin tapar nodos, otras distancias ni la línea de OTRA arista; si no hay buen
     * lugar, con letra más pequeña. Primero la ruta; luego las aristas más cortas.
     */
    private List<Etiqueta> calcularEtiquetas(List<Camino> caminos, Map<String, int[]> pos,
                                             String fNormal, String fPequena, double altoNormal, double altoPequena) {
        List<Etiqueta> resultado = new ArrayList<>();
        List<double[]> ocupados = new ArrayList<>();
        for (String id : pos.keySet()) {
            double[] c = cajaNodo(id, local(id, pos), 2);
            ocupados.add(new double[]{c[0] - 2, c[1] - 2, c[2] + 4, c[3] + 4});
        }
        Map<Camino, List<double[]>> trazos = new HashMap<>();
        for (Camino c : caminos) {
            double[] pA = local(c.getOrigenId(), pos), pB = local(c.getDestinoId(), pos);
            if (pA != null && pB != null) trazos.put(c, trazo(c.getOrigenId(), c.getDestinoId(), pA, pB));
        }
        List<Camino> orden = new ArrayList<>(caminos);
        orden.removeIf(c -> !trazos.containsKey(c));
        orden.sort((x, y) -> {
            int r = Boolean.compare(!esAristaDeRuta(x.getOrigenId(), x.getDestinoId()),
                                    !esAristaDeRuta(y.getOrigenId(), y.getDestinoId()));
            if (r != 0) return r;
            return Double.compare(largo(x, pos), largo(y, pos));
        });
        double[] posicionesT = {0.5, 0.4, 0.6, 0.32, 0.68, 0.25, 0.75};
        for (Camino c : orden) {
            String a = c.getOrigenId(), b = c.getDestinoId();
            double[] pA = local(a, pos), pB = local(b, pos);
            String txt = String.format("%.0f m", c.getDistancia());
            double dx = pB[0] - pA[0], dy = pB[1] - pA[1], largo = Math.max(1, Math.hypot(dx, dy));

            double[] elegido = null;
            boolean pequena = false;
            double mejor = Double.MAX_VALUE;
            for (int f = 0; f < 2; f++) {
                ctx.setFont(f == 0 ? fNormal : fPequena);
                double w = ctx.measureText(txt).getWidth() + (f == 0 ? 10 : 7);
                double h = f == 0 ? altoNormal + 2 : altoPequena;
                for (int lado : new int[]{0, 1, -1, 2, -2}) {
                    for (double t : posicionesT) {
                        double[] m = puntoEn(a, b, pA, pB, t);
                        if (lado != 0) {
                            double sep = Math.abs(lado) == 1 ? h / 2.0 : h * 1.25;
                            int signo = Integer.signum(lado);
                            m = new double[]{m[0] - dy / largo * sep * signo, m[1] + dx / largo * sep * signo};
                        }
                        double[] r = {Math.round(m[0] - w / 2), Math.round(m[1] - h / 2), w, h};
                        double puntaje = 0;
                        for (double[] o : ocupados) puntaje += 6.0 * areaInterseccion(o, r);
                        for (Map.Entry<Camino, List<double[]>> e : trazos.entrySet()) {
                            if (e.getKey() != c && lineaTocaRect(e.getValue(), r)) puntaje += 600;
                        }
                        puntaje += (lado == 0 ? 0 : Math.abs(lado) == 1 ? 70 : 140) + (f == 1 ? 40 : 0) + Math.abs(t - 0.5) * 40;
                        if (puntaje < mejor) { mejor = puntaje; elegido = r; pequena = f == 1; }
                    }
                }
                if (mejor < 40) break;
            }
            ocupados.add(new double[]{elegido[0] - 2, elegido[1] - 1, elegido[2] + 4, elegido[3] + 2});
            resultado.add(new Etiqueta(c, elegido, pequena));
        }
        return resultado;
    }

    private double largo(Camino c, Map<String, int[]> pos) {
        double[] a = local(c.getOrigenId(), pos), b = local(c.getDestinoId(), pos);
        return Math.hypot(a[0] - b[0], a[1] - b[1]);
    }

    // ==================== Toques ====================

    @Override
    protected void alTocar(double x, double y) {
        if (zonaLeyenda != null && x >= zonaLeyenda[0] && x <= zonaLeyenda[0] + zonaLeyenda[2]
                && y >= zonaLeyenda[1] && y <= zonaLeyenda[1] + zonaLeyenda[3]) {
            leyendaAbierta = !leyendaAbierta;
            redibujar();
            return;
        }
        Map<String, int[]> pos = posiciones();
        double lx = x - offX, ly = y - offY;
        for (String id : pos.keySet()) {
            double[] c = cajaNodo(id, local(id, pos), 8);
            if (lx >= c[0] && lx <= c[0] + c[2] && ly >= c[1] && ly <= c[1] + c[3]) {
                nodoTocado = id;
                caminoTocado = null;
                redibujar();
                alTocarEdificio.accept(id);
                return;
            }
        }
        Camino mejor = null;
        double distMejor = 14;   // tolerancia para el dedo
        for (Camino c : caminosUnicos()) {
            double[] pA = local(c.getOrigenId(), pos), pB = local(c.getDestinoId(), pos);
            if (pA == null || pB == null) continue;
            List<double[]> t = trazo(c.getOrigenId(), c.getDestinoId(), pA, pB);
            for (int i = 0; i < t.size() - 1; i++) {
                double d = distanciaASegmento(lx, ly, t.get(i), t.get(i + 1));
                if (d < distMejor) { distMejor = d; mejor = c; }
            }
        }
        nodoTocado = null;
        caminoTocado = mejor;
        redibujar();
        if (mejor != null) alTocarCamino.accept(mejor); else alTocarEdificio.accept(null);
    }

    private static double distanciaASegmento(double px, double py, double[] a, double[] b) {
        double dx = b[0] - a[0], dy = b[1] - a[1];
        double l2 = dx * dx + dy * dy;
        double t = l2 == 0 ? 0 : Math.max(0, Math.min(1, ((px - a[0]) * dx + (py - a[1]) * dy) / l2));
        return Math.hypot(px - (a[0] + t * dx), py - (a[1] + t * dy));
    }

    // ==================== Dibujo ====================

    @Override
    protected void dibujarContenido() {
        Map<String, int[]> pos = posiciones();
        double esc = escala;
        double grosor = Math.max(1.6, 2.4 * esc);

        // 0. Fondo de puntos
        ctx.setFillStyle(GRIS_PUNTOS);
        for (int x = 9; x < anchoCss; x += 18) {
            for (int y = 9; y < altoCss; y += 18) ctx.fillRect(x - 1, y - 1, 2, 2);
        }

        // Qué se resalta: el vecindario del edificio tocado o, si no, la ruta calculada
        boolean hayRuta = ruta.size() > 1;
        Set<String> nodosFoco = new HashSet<>();
        String foco = resaltarConexiones ? nodoTocado : null;
        if (foco != null) {
            nodosFoco.add(foco);
            for (Camino c : controlador.getGrafo().getAdyacentes(foco)) nodosFoco.add(c.getDestinoId());
        } else if (hayRuta) {
            nodosFoco.addAll(ruta);
        }
        boolean hayFoco = !nodosFoco.isEmpty();
        double alfaFuera = foco != null ? 0.15 : 0.32;
        List<Camino> caminos = caminosUnicos();

        // 1. Aristas
        ctx.setLineCap("round");
        ctx.setLineJoin("round");
        for (Camino c : caminos) {
            String a = c.getOrigenId(), b = c.getDestinoId();
            double[] pA = local(a, pos), pB = local(b, pos);
            if (pA == null || pB == null) continue;
            boolean enFoco = !hayFoco || (foco != null ? (a.equals(foco) || b.equals(foco)) : esAristaDeRuta(a, b));
            ctx.setGlobalAlpha(enFoco ? 1 : alfaFuera);
            ctx.setLineWidth(c == caminoTocado ? grosor * 2 : grosor);
            if (c.isBloqueado()) {
                ctx.setStrokeStyle(CAMINO_BLOQUEADO);
                lineaPunteada(6 * esc + 2, 5 * esc + 2);
            } else {
                ctx.setStrokeStyle(c.isTieneEscaleras() ? CAMINO_ESCALERAS : CAMINO_ACCESIBLE);
            }
            trazarArista(a, b, pA, pB);
            ctx.stroke();
            if (c.isBloqueado()) lineaPunteada(0, 0);
        }
        ctx.setGlobalAlpha(1);

        // 2. Ruta calculada en dorado
        if (hayRuta) {
            ctx.setStrokeStyle(ORO);
            ctx.setLineWidth(Math.max(4, 7 * esc));
            for (int i = 0; i < ruta.size() - 1; i++) {
                double[] p1 = local(ruta.get(i), pos), p2 = local(ruta.get(i + 1), pos);
                if (p1 != null && p2 != null) {
                    trazarArista(ruta.get(i), ruta.get(i + 1), p1, p2);
                    ctx.stroke();
                }
            }
        }

        // 3. Distancias sobre las aristas
        if (mostrarDistancias) {
            double tamNormal = Math.round(tam(11, 8)), tamPequena = Math.round(tam(9, 7));
            String fNormal = tamNormal + "px " + FUENTE, fPequena = tamPequena + "px " + FUENTE;
            List<Object> clave = claveEtiquetas(caminos, pos);
            if (!clave.equals(claveEtiquetas)) {
                etiquetas = calcularEtiquetas(caminos, pos, fNormal, fPequena,
                        Math.round(tamNormal * 1.21), Math.round(tamPequena * 1.21));
                claveEtiquetas = clave;
            }
            ctx.setTextAlign("center");
            ctx.setTextBaseline("middle");
            for (Etiqueta et : etiquetas) {
                String a = et.camino.getOrigenId(), b = et.camino.getDestinoId();
                boolean deRuta = esAristaDeRuta(a, b);
                boolean enFoco = !hayFoco || (foco != null ? (a.equals(foco) || b.equals(foco)) : deRuta);
                ctx.setGlobalAlpha(enFoco ? 1 : alfaFuera);
                double[] z = et.zona;
                double x = z[0] + offX, y = z[1] + offY;
                rectRedondeado(x, y, z[2], z[3], z[3] / 2);
                ctx.setFillStyle("#FFFFFF");
                ctx.fill();
                ctx.setStrokeStyle(deRuta && hayRuta ? ORO : "#CBD5E1");
                ctx.setLineWidth(deRuta && hayRuta ? 1.6 : 1);
                ctx.stroke();
                ctx.setFont(et.pequena ? fPequena : fNormal);
                ctx.setFillStyle(TEXTO_OSCURO);
                ctx.fillText(String.format("%.0f m", et.camino.getDistancia()), x + z[2] / 2, y + z[3] / 2 + 0.5);
            }
            ctx.setGlobalAlpha(1);
        }

        // 4. Edificios y puntos de referencia
        ctx.setTextAlign("center");
        ctx.setTextBaseline("middle");
        for (String id : pos.keySet()) {
            double[] p = local(id, pos);
            boolean esInicio = hayRuta ? id.equals(ruta.get(0)) : id.equals(seleccionOrigen);
            boolean esFin = hayRuta ? id.equals(ruta.get(ruta.size() - 1)) : id.equals(seleccionDestino);
            boolean deRuta = hayRuta && ruta.contains(id);
            boolean enFoco = !hayFoco || nodosFoco.contains(id);
            ctx.setGlobalAlpha(enFoco ? 1 : Math.min(1, alfaFuera + 0.2));

            String fondo, borde, texto;
            if (esInicio) { fondo = INICIO_FONDO; borde = INICIO_BORDE; texto = INICIO_BORDE; }
            else if (esFin) { fondo = DESTINO_FONDO; borde = PUNTO_BORDE; texto = PUNTO_TEXTO; }
            else if (esPuntoDeReferencia(id)) { fondo = PUNTO_FONDO; borde = PUNTO_BORDE; texto = PUNTO_TEXTO; }
            else { fondo = NODO_FONDO; borde = PRIMARIO; texto = PRIMARIO_OSCURO; }

            boolean destacado = deRuta || esInicio || esFin || id.equals(nodoTocado);
            double[] c = cajaNodo(id, p, destacado ? 2 : 0);
            trazarNodo(id, c);
            ctx.setFillStyle(fondo);
            ctx.fill();
            ctx.setStrokeStyle(borde);
            ctx.setLineWidth(destacado ? Math.max(2.4, 3 * esc) : Math.max(1.4, 1.8 * esc));
            ctx.stroke();

            ctx.setFont(fuenteNodo());
            ctx.setFillStyle(texto);
            ctx.fillText(textoNodo(id), p[0] + offX, p[1] + offY + 0.5);

            // Rótulo de origen/destino cuando aún no hay ruta calculada
            if (!hayRuta && (esInicio || esFin)) {
                ctx.setFont("bold " + Math.round(tam(10, 8)) + "px " + FUENTE);
                ctx.setFillStyle(borde);
                ctx.fillText(esInicio ? "Origen" : "Destino", p[0] + offX, c[1] + c[3] + offY + tam(10, 8) * 0.8 + 2);
            }
        }
        ctx.setGlobalAlpha(1);

        // 5. Número de paso sobre cada edificio de la ruta
        if (hayRuta) {
            double d = Math.max(13, Math.round(20 * esc));
            ctx.setFont("bold " + Math.round(tam(11, 8)) + "px " + FUENTE);
            for (int i = 0; i < ruta.size(); i++) {
                double[] p = local(ruta.get(i), pos);
                if (p == null) continue;
                double[] c = cajaNodo(ruta.get(i), p, 2);
                double cx = c[0] + (esPuntoDeReferencia(ruta.get(i)) ? 2 : 0) + offX, cy = c[1] + offY;
                circulo(cx, cy, d / 2 + 2, "#FFFFFF");
                circulo(cx, cy, d / 2, PRIMARIO);
                ctx.setFillStyle("#FFFFFF");
                ctx.fillText(String.valueOf(i + 1), cx, cy + 0.5);
            }
        }

        // 6. Convenciones
        dibujarLeyenda();
    }

    private void trazarNodo(String id, double[] c) {
        double x = c[0] + offX, y = c[1] + offY;
        if (!esPuntoDeReferencia(id)) {
            ctx.beginPath();
            ctx.arc(x + c[2] / 2, y + c[3] / 2, c[2] / 2, 0, Math.PI * 2);
        } else {
            rectRedondeado(x, y, c[2], c[3], c[3] * 0.3);
        }
    }

    private void circulo(double x, double y, double r, String color) {
        ctx.beginPath();
        ctx.arc(x, y, r, 0, Math.PI * 2);
        ctx.setFillStyle(color);
        ctx.fill();
    }

    private void lineaPunteada(double trazo, double espacio) {
        JSArray<org.teavm.jso.JSObject> patron = JSArray.create();
        if (trazo > 0) {
            patron.push(JSNumber.valueOf(trazo));
            patron.push(JSNumber.valueOf(espacio));
        }
        ctx.setLineDash(patron);
    }

    /** Tarjeta de convenciones (abajo a la izquierda); se abre o minimiza tocándola. */
    private void dibujarLeyenda() {
        double x = 12;
        ctx.setTextAlign("left");
        ctx.setTextBaseline("middle");
        if (!leyendaAbierta) {
            double w = 128, h = 30, y = altoCss - 12 - h;
            zonaLeyenda = new double[]{x, y, w, h};
            sombraTarjeta(x, y, w, h, h / 2);
            ctx.setFont("bold 12px " + FUENTE);
            ctx.setFillStyle(TEXTO_OSCURO);
            ctx.fillText("Convenciones", x + 12, y + h / 2);
            flecha(x + w - 16, y + h / 2, false);
            return;
        }
        double w = 176, h = 158, y = altoCss - 12 - h;
        zonaLeyenda = new double[]{x, y, w, h};
        sombraTarjeta(x, y, w, h, 14);
        ctx.setFont("bold 12px " + FUENTE);
        ctx.setFillStyle(TEXTO_OSCURO);
        ctx.fillText("Convenciones", x + 12, y + 16);
        flecha(x + w - 16, y + 16, true);

        ctx.setFont("11px " + FUENTE);
        String[] txt = {"Sin escaleras", "Con escaleras", "Bloqueado", "Ruta calculada"};
        String[] col = {CAMINO_ACCESIBLE, CAMINO_ESCALERAS, CAMINO_BLOQUEADO, ORO};
        for (int i = 0; i < txt.length; i++) {
            double ly = y + 38 + i * 18;
            ctx.setStrokeStyle(col[i]);
            ctx.setLineWidth(i == 3 ? 5 : 2.4);
            if (i == 2) lineaPunteada(5, 4);
            ctx.beginPath();
            ctx.moveTo(x + 12, ly);
            ctx.lineTo(x + 40, ly);
            ctx.stroke();
            if (i == 2) lineaPunteada(0, 0);
            ctx.setFillStyle(TEXTO_MUTED);
            ctx.fillText(txt[i], x + 50, ly);
        }
        double ly = y + 38 + 4 * 18 + 2;
        ctx.beginPath();
        ctx.arc(x + 25, ly, 7, 0, Math.PI * 2);
        ctx.setFillStyle(NODO_FONDO);
        ctx.fill();
        ctx.setStrokeStyle(PRIMARIO);
        ctx.setLineWidth(1.6);
        ctx.stroke();
        ctx.setFillStyle(TEXTO_MUTED);
        ctx.fillText("Edificio", x + 50, ly);
        ly += 18;
        rectRedondeado(x + 12, ly - 6, 28, 12, 4);
        ctx.setFillStyle(PUNTO_FONDO);
        ctx.fill();
        ctx.setStrokeStyle(PUNTO_BORDE);
        ctx.stroke();
        ctx.setFillStyle(TEXTO_MUTED);
        ctx.fillText("Portería, CAF, templo", x + 50, ly);
    }

    private void sombraTarjeta(double x, double y, double w, double h, double radio) {
        rectRedondeado(x + 2, y + 3, w, h, radio);
        ctx.setFillStyle("rgba(0,0,0,0.07)");
        ctx.fill();
        rectRedondeado(x, y, w, h, radio);
        ctx.setFillStyle("#FFFFFF");
        ctx.fill();
        ctx.setStrokeStyle("#D5DCE6");
        ctx.setLineWidth(1);
        ctx.stroke();
    }

    /** Flecha para abrir (hacia arriba) o minimizar (hacia abajo) las convenciones. */
    private void flecha(double cx, double cy, boolean abierta) {
        ctx.setStrokeStyle(TEXTO_MUTED);
        ctx.setLineWidth(1.8);
        ctx.beginPath();
        if (abierta) {
            ctx.moveTo(cx - 4, cy - 2); ctx.lineTo(cx, cy + 2); ctx.lineTo(cx + 4, cy - 2);
        } else {
            ctx.moveTo(cx - 4, cy + 2); ctx.lineTo(cx, cy - 2); ctx.lineTo(cx + 4, cy + 2);
        }
        ctx.stroke();
    }
}
