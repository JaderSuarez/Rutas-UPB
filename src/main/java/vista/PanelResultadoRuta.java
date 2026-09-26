package vista;

import modelo.Camino;
import modelo.ServicioRutas.ResultadoRuta;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.util.List;

/**
 * Tarjeta visual del resultado de una ruta: métricas (distancia, tiempo,
 * accesibilidad) y un "stepper" con la secuencia de edificios, donde cada
 * tramo se dibuja distinto según si tiene escaleras o no.
 */
public class PanelResultadoRuta extends JPanel implements Scrollable {

    private ResultadoRuta resultado;
    private boolean accesibleSolicitado;
    private String mensajeError;

    // Medidas del stepper
    private static final int ALTO_KPI = 74;
    private static final int ALTO_CHIP = 30;
    private static final int SEPARACION_TRAMO = 92;
    private static final int MARGEN = 18;

    public PanelResultadoRuta() {
        setBackground(UIColores.FONDO);
        setPreferredSize(new Dimension(600, 200));
    }

    public void mostrarResultado(ResultadoRuta resultado, boolean accesibleSolicitado) {
        this.resultado = resultado;
        this.accesibleSolicitado = accesibleSolicitado;
        this.mensajeError = null;
        actualizarTamanoPreferido();
        revalidate();
        repaint();
    }

    public void mostrarError(String mensaje) {
        this.resultado = null;
        this.mensajeError = mensaje;
        setPreferredSize(new Dimension(600, 200));
        revalidate();
        repaint();
    }

    public void limpiar() {
        this.resultado = null;
        this.mensajeError = null;
        setPreferredSize(new Dimension(600, 200));
        revalidate();
        repaint();
    }

    /**
     * Recalcula la altura preferida del panel según cuántas filas necesita
     * el stepper para la ruta actual, evitando que las filas de más se
     * corten cuando la ruta tiene muchos edificios. Se usa un ancho de
     * referencia razonable (el ancho actual del panel, o 850px si aún no
     * se conoce) para estimar el número de filas.
     */
    private void actualizarTamanoPreferido() {
        if (resultado == null || resultado.getCaminoEdificios() == null) return;
        int anchoReferencia = getWidth() > 0 ? getWidth() : 850;
        int filas = contarFilasStepper(resultado.getCaminoEdificios(), anchoReferencia);
        int altoNecesario = MARGEN + ALTO_KPI + 34 + filas * (ALTO_CHIP + 46) - 4;
        setPreferredSize(new Dimension(anchoReferencia, Math.max(200, altoNecesario)));
    }

    /** Simula el mismo ajuste de línea que dibujarStepper(), pero solo cuenta filas. */
    private int contarFilasStepper(List<String> edificiosRuta, int anchoDisponible) {
        FontMetrics fm = getFontMetrics(new Font(EstiloUPB.FAMILIA, Font.BOLD, 12));
        int x = MARGEN;
        int filas = 1;
        int limiteDerecho = anchoDisponible - MARGEN;

        for (int i = 0; i < edificiosRuta.size(); i++) {
            int anchoChip = fm.stringWidth(edificiosRuta.get(i)) + 24;
            if (x > MARGEN && x + anchoChip > limiteDerecho) {
                x = MARGEN;
                filas++;
            }
            x += anchoChip;
            if (i < edificiosRuta.size() - 1) {
                if (x + SEPARACION_TRAMO <= limiteDerecho) {
                    x += SEPARACION_TRAMO;
                } else {
                    x = MARGEN + SEPARACION_TRAMO;
                    filas++;
                }
            }
        }
        return filas;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (mensajeError != null) {
            dibujarMensaje(g2, mensajeError, UIColores.ERROR);
            return;
        }
        if (resultado == null) {
            dibujarPasosIniciales(g2);
            return;
        }

        // El ancho real ya lo asigna el JScrollPane (Scrollable) en este punto;
        // si la altura reservada no alcanza para todas las filas del stepper,
        // se corrige aquí y se vuelve a organizar el layout.
        corregirAltoSiHaceFalta();

        dibujarKpis(g2);
        dibujarStepper(g2);
    }

    private void corregirAltoSiHaceFalta() {
        if (resultado == null || resultado.getCaminoEdificios() == null || getWidth() <= 0) return;
        int filas = contarFilasStepper(resultado.getCaminoEdificios(), getWidth());
        int altoNecesario = MARGEN + ALTO_KPI + 34 + filas * (ALTO_CHIP + 46) - 4;
        Dimension actual = getPreferredSize();
        if (altoNecesario > actual.height) {
            setPreferredSize(new Dimension(actual.width, altoNecesario));
            SwingUtilities.invokeLater(this::revalidate);
        }
    }

    private void dibujarMensaje(Graphics2D g2, String mensaje, Color color) {
        g2.setFont(new Font(EstiloUPB.FAMILIA, Font.ITALIC, 13));
        g2.setColor(color);
        FontMetrics fm = g2.getFontMetrics();
        int x = Math.max(MARGEN, (getWidth() - fm.stringWidth(mensaje)) / 2);
        g2.drawString(mensaje, x, getHeight() / 2);
    }

    // ---------------- Tarjetas de métricas ----------------
    private void dibujarKpis(Graphics2D g2) {
        int anchoDisponible = getWidth() - MARGEN * 2;
        int anchoTarjeta = (anchoDisponible - 20) / 3;
        int y = MARGEN;

        boolean rutaSinEscaleras = !resultado.tieneAlgunTramoConEscaleras();

        dibujarTarjetaKpi(g2, MARGEN, y, anchoTarjeta, "Distancia total",
                String.format("%.0f m", resultado.getDistanciaTotal()), UIColores.PRIMARIO);

        dibujarTarjetaKpi(g2, MARGEN + anchoTarjeta + 10, y, anchoTarjeta, "Tiempo estimado",
                resultado.getTiempoEstimadoFormateado(), UIColores.PRIMARIO);

        String textoAccesible;
        Color colorAccesible;
        if (rutaSinEscaleras) {
            textoAccesible = "Sin escaleras";
            colorAccesible = UIColores.EXITO;
        } else {
            textoAccesible = "Tiene escaleras";
            colorAccesible = UIColores.PRIMARIO_OSCURO;
        }
        dibujarTarjetaKpi(g2, MARGEN + (anchoTarjeta + 10) * 2, y, anchoTarjeta,
                accesibleSolicitado ? "Modo accesible: activado" : "Modo accesible: apagado",
                textoAccesible, colorAccesible);
    }

    private void dibujarTarjetaKpi(Graphics2D g2, int x, int y, int ancho, String etiqueta, String valor, Color colorValor) {
        g2.setColor(UIColores.TARJETA);
        g2.fill(new RoundRectangle2D.Double(x, y, ancho, ALTO_KPI, 12, 12));
        g2.setColor(UIColores.BORDE);
        g2.draw(new RoundRectangle2D.Double(x, y, ancho, ALTO_KPI, 12, 12));

        g2.setFont(new Font(EstiloUPB.FAMILIA, Font.PLAIN, 11));
        g2.setColor(UIColores.TEXTO_MUTED);
        g2.drawString(recortar(g2, etiqueta, ancho - 20), x + 12, y + 22);

        g2.setFont(new Font(EstiloUPB.FAMILIA, Font.BOLD, 19));
        g2.setColor(colorValor);
        g2.drawString(recortar(g2, valor, ancho - 20), x + 12, y + 52);
    }

    // ---------------- Stepper del recorrido ----------------
    private void dibujarStepper(Graphics2D g2) {
        List<String> edificiosRuta = resultado.getCaminoEdificios();
        List<Camino> tramos = resultado.getTramos();
        if (edificiosRuta == null || edificiosRuta.isEmpty()) return;

        g2.setFont(new Font(EstiloUPB.FAMILIA, Font.BOLD, 12));
        FontMetrics fm = g2.getFontMetrics();

        int x = MARGEN;
        int y = MARGEN + ALTO_KPI + 34;
        int limiteDerecho = getWidth() - MARGEN;

        for (int i = 0; i < edificiosRuta.size(); i++) {
            String nombre = edificiosRuta.get(i);
            int anchoChip = fm.stringWidth(nombre) + 24;

            // Salto de línea si el siguiente chip no cabe
            if (x > MARGEN && x + anchoChip > limiteDerecho) {
                x = MARGEN;
                y += ALTO_CHIP + 46;
            }

            dibujarChip(g2, x, y, anchoChip, nombre, i == 0, i == edificiosRuta.size() - 1);
            x += anchoChip;

            // Conector hacia el siguiente edificio
            if (i < edificiosRuta.size() - 1) {
                Camino tramo = (tramos != null && i < tramos.size()) ? tramos.get(i) : null;
                boolean cabeConector = x + SEPARACION_TRAMO <= limiteDerecho;
                if (cabeConector) {
                    dibujarConector(g2, x, y + ALTO_CHIP / 2, SEPARACION_TRAMO, tramo);
                    x += SEPARACION_TRAMO;
                } else {
                    // El conector no cabe: se dibuja al inicio de la siguiente línea
                    x = MARGEN;
                    y += ALTO_CHIP + 46;
                    dibujarConector(g2, x, y + ALTO_CHIP / 2, SEPARACION_TRAMO, tramo);
                    x += SEPARACION_TRAMO;
                }
            }
        }

        dibujarLeyenda(g2, MARGEN, y + ALTO_CHIP + 28);
    }

    private void dibujarChip(Graphics2D g2, int x, int y, int ancho, String texto, boolean esInicio, boolean esFin) {
        Color fondo = UIColores.ERROR_FONDO;
        Color borde = UIColores.PRIMARIO;
        if (esInicio) {
            fondo = UIColores.EXITO_FONDO;
            borde = UIColores.EXITO;
        } else if (esFin) {
            fondo = new Color(0xFD, 0xE9, 0xC0);
            borde = UIColores.ACENTO;
        }

        g2.setColor(fondo);
        g2.fill(new RoundRectangle2D.Double(x, y, ancho, ALTO_CHIP, ALTO_CHIP, ALTO_CHIP));
        g2.setColor(borde);
        g2.setStroke(new BasicStroke(1.6f));
        g2.draw(new RoundRectangle2D.Double(x, y, ancho, ALTO_CHIP, ALTO_CHIP, ALTO_CHIP));

        g2.setFont(new Font(EstiloUPB.FAMILIA, Font.BOLD, 12));
        FontMetrics fm = g2.getFontMetrics();
        g2.setColor(UIColores.PRIMARIO_OSCURO);
        g2.drawString(texto, x + (ancho - fm.stringWidth(texto)) / 2, y + ALTO_CHIP / 2 + 4);

        // Etiqueta inicio / fin
        if (esInicio || esFin) {
            g2.setFont(new Font(EstiloUPB.FAMILIA, Font.PLAIN, 10));
            g2.setColor(UIColores.TEXTO_MUTED);
            String etiqueta = esInicio ? "inicio" : "destino";
            FontMetrics fm2 = g2.getFontMetrics();
            g2.drawString(etiqueta, x + (ancho - fm2.stringWidth(etiqueta)) / 2, y - 6);
        }
    }

    private void dibujarConector(Graphics2D g2, int x, int yCentro, int largo, Camino tramo) {
        boolean tieneEscaleras = tramo != null && tramo.isTieneEscaleras();

        if (tieneEscaleras) {
            g2.setColor(UIColores.CAMINO_ESCALERAS);
            g2.setStroke(new BasicStroke(2.4f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                    10f, new float[]{7f, 5f}, 0f));
        } else {
            g2.setColor(UIColores.CAMINO_ACCESIBLE);
            g2.setStroke(new BasicStroke(2.6f));
        }
        g2.drawLine(x + 4, yCentro, x + largo - 10, yCentro);

        // Punta de flecha
        g2.setStroke(new BasicStroke(2.2f));
        int xf = x + largo - 8;
        g2.drawLine(xf - 7, yCentro - 5, xf, yCentro);
        g2.drawLine(xf - 7, yCentro + 5, xf, yCentro);

        // Distancia del tramo y marca de escaleras
        if (tramo != null) {
            g2.setFont(new Font(EstiloUPB.FAMILIA, Font.PLAIN, 10));
            FontMetrics fm = g2.getFontMetrics();
            String txt = String.format("%.0f m", tramo.getDistancia());
            g2.setColor(UIColores.TEXTO_MUTED);
            g2.drawString(txt, x + (largo - fm.stringWidth(txt)) / 2, yCentro - 8);

            if (tieneEscaleras) {
                String marca = "escaleras";
                g2.setColor(UIColores.CAMINO_ESCALERAS);
                g2.drawString(marca, x + (largo - fm.stringWidth(marca)) / 2, yCentro + 18);
            }
        }
    }

    private void dibujarLeyenda(Graphics2D g2, int x, int y) {
        g2.setFont(new Font(EstiloUPB.FAMILIA, Font.PLAIN, 10));

        g2.setColor(UIColores.CAMINO_ACCESIBLE);
        g2.setStroke(new BasicStroke(2.6f));
        g2.drawLine(x, y, x + 24, y);
        g2.setColor(UIColores.TEXTO_MUTED);
        g2.drawString("tramo sin escaleras", x + 30, y + 4);

        int x2 = x + 160;
        g2.setColor(UIColores.CAMINO_ESCALERAS);
        g2.setStroke(new BasicStroke(2.4f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                10f, new float[]{7f, 5f}, 0f));
        g2.drawLine(x2, y, x2 + 24, y);
        g2.setColor(UIColores.TEXTO_MUTED);
        g2.drawString("tramo con escaleras", x2 + 30, y + 4);
    }

    private String recortar(Graphics2D g2, String texto, int anchoMax) {
        FontMetrics fm = g2.getFontMetrics();
        if (fm.stringWidth(texto) <= anchoMax) return texto;
        String resultado = texto;
        while (resultado.length() > 4 && fm.stringWidth(resultado + "...") > anchoMax) {
            resultado = resultado.substring(0, resultado.length() - 1);
        }
        return resultado + "...";
    }

    // ---------------- Scrollable: usar siempre el ancho del visor ----------------
    // Así el panel se ajusta en filas dentro del ancho disponible, en vez de
    // aparecer una barra de desplazamiento horizontal.
    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return getPreferredSize();
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        return true;
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
        // Si sobra espacio, el panel ocupa todo el alto visible (sin barra de desplazamiento)
        return getParent() instanceof JViewport && getParent().getHeight() > getPreferredSize().height;
    }

    @Override
    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
        return 20;
    }

    @Override
    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
        return 100;
    }
    /** Estado inicial guiado: tres pasos numerados para calcular una ruta. */
    private void dibujarPasosIniciales(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        String[][] pasos = {
                {"Elige el origen", "o toca «Estoy en: Portería»"},
                {"Elige el destino", "o busca un lugar arriba"},
                {"Calcula la ruta", "«Calcular Ruta Más Corta»"}};
        Font fTitulo = new Font(EstiloUPB.FAMILIA, Font.BOLD, 14);
        Font fPaso = new Font(EstiloUPB.FAMILIA, Font.BOLD, 13);
        Font fAyuda = new Font(EstiloUPB.FAMILIA, Font.PLAIN, 11);
        int anchoPaso = Math.min(210, Math.max(150, (getWidth() - 80) / 3));
        int total = anchoPaso * 3;
        int x0 = (getWidth() - total) / 2;
        int y0 = Math.max(18, getHeight() / 2 - 48);

        String titulo = "¿A dónde vas hoy?";
        g2.setFont(fTitulo);
        g2.setColor(UIColores.TEXTO_OSCURO);
        g2.drawString(titulo, (getWidth() - g2.getFontMetrics().stringWidth(titulo)) / 2, y0);

        int cy = y0 + 36;
        for (int i = 0; i < 3; i++) {
            int cx = x0 + anchoPaso * i + anchoPaso / 2;
            if (i < 2) {   // flecha hacia el siguiente paso
                g2.setColor(new Color(0xCB, 0xD5, 0xE1));
                g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                int ax = cx + 26, bx = cx + anchoPaso - 26;
                g2.drawLine(ax, cy, bx, cy);
                g2.drawPolyline(new int[]{bx - 6, bx, bx - 6}, new int[]{cy - 5, cy, cy + 5}, 3);
            }
            g2.setColor(UIColores.PRIMARIO);
            g2.fillOval(cx - 16, cy - 16, 32, 32);
            g2.setColor(Color.WHITE);
            g2.setFont(fPaso);
            FontMetrics fm = g2.getFontMetrics();
            String n = String.valueOf(i + 1);
            g2.drawString(n, cx - fm.stringWidth(n) / 2, cy + fm.getAscent() / 2 - 2);
            g2.setColor(UIColores.TEXTO_OSCURO);
            g2.drawString(pasos[i][0], cx - fm.stringWidth(pasos[i][0]) / 2, cy + 36);
            g2.setFont(fAyuda);
            FontMetrics fa = g2.getFontMetrics();
            g2.setColor(UIColores.TEXTO_MUTED);
            g2.drawString(pasos[i][1], cx - fa.stringWidth(pasos[i][1]) / 2, cy + 53);
        }
    }
}
