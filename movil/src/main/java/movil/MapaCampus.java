package movil;

import controlador.CampusControlador;
import modelo.Edificio;
import org.teavm.jso.dom.html.HTMLImageElement;
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
 * Se maneja con los dedos (ver LienzoTactil): arrastrar, pellizcar y tocar un pin
 * para ver el edificio. En el modo "ubicar" (panel de administración) un toque
 * devuelve la posición elegida sobre la imagen.
 */
final class MapaCampus extends LienzoTactil {

    private static final String VINOTINTO = "#800020";
    private static final String VINOTINTO_OSCURO = "#5A0016";
    private static final String DORADO = "#D4AF37";
    private static final String VERDE = "#0B7A55";
    private static final double RADIO_PIN = 13;

    private final CampusControlador controlador;
    private final HTMLImageElement imagen = (HTMLImageElement) Dom.el("img", null);
    private boolean imagenLista;

    private List<String> ruta = new ArrayList<>();
    private String seleccionado;
    private Consumer<String> alTocarEdificio = id -> { };
    private Consumer<int[]> alUbicar;
    private int[] marcador;

    MapaCampus(CampusControlador controlador) {
        this.controlador = controlador;
        imagen.listenLoad(e -> {
            imagenLista = true;
            redibujar();
        });
        imagen.setSrc("mapa_campus.jpg");
    }

    @Override protected double anchoMundo() { return CoordenadasMapa.ANCHO; }
    @Override protected double altoMundo() { return CoordenadasMapa.ALTO; }

    @Override
    protected void vistaInicial() {
        if (ruta.size() > 1) enfocarRuta(); else vistaGeneral();
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

    /** Vista general: el mapa llena toda la pantalla (en vertical se recorre a lo ancho con el dedo). */
    void vistaGeneral() {
        escala = Math.max(anchoCss / CoordenadasMapa.ANCHO, altoCss / CoordenadasMapa.ALTO);
        centrarEn(CoordenadasMapa.ANCHO / 2.0, CoordenadasMapa.ALTO / 2.0);
    }

    /** Acerca el mapa para que la ruta completa quede a la vista. */
    void enfocarRuta() {
        if (ruta.size() < 2) return;
        if (!tieneTamano()) {
            reiniciarVistaAlMostrar();
            return;
        }
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
        for (int i = 0; i < ruta.size() - 1; i++) {
            for (int[] p : puntosDelTramo(ruta.get(i), ruta.get(i + 1))) {
                minX = Math.min(minX, p[0]); maxX = Math.max(maxX, p[0]);
                minY = Math.min(minY, p[1]); maxY = Math.max(maxY, p[1]);
            }
        }
        encuadrar(minX, minY, maxX, maxY, 60);
    }

    @Override
    protected void alTocar(double x, double y) {
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

    @Override
    protected void dibujarContenido() {
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
        rectRedondeado(x, y, w, h, radio);
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
