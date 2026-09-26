package vista;

import javax.swing.*;
import javax.swing.border.AbstractBorder;
import javax.swing.border.Border;
import javax.swing.plaf.LayerUI;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicGraphicsUtils;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.function.Function;

/**
 * Estilos visuales compartidos por las ventanas (complemento de BotonUPB):
 * tablas con filas alternadas y resaltado, etiquetas de color, candado,
 * pestañas, secciones tipo tarjeta, listas desplegables planas, interruptor
 * y texto guía en campos de texto.
 */
public final class EstiloUPB {

    // =================================================================== Tipografía
    /**
     * Familia tipográfica de toda la interfaz: Inter (licencia SIL OFL, incluida en
     * src/main/resources/fuentes). Si no se puede cargar, se usa "SansSerif".
     * Debe declararse antes que cualquier otra fuente de esta clase.
     */
    public static final String FAMILIA = registrarInter();

    private static String registrarInter() {
        String[] archivos = {"Inter-Regular.ttf", "Inter-Bold.ttf", "Inter-Italic.ttf", "Inter-BoldItalic.ttf"};
        try {
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            for (String a : archivos) {
                try (java.io.InputStream in = EstiloUPB.class.getResourceAsStream("/fuentes/" + a)) {
                    if (in == null) return "SansSerif";
                    ge.registerFont(Font.createFont(Font.TRUETYPE_FONT, in));
                }
            }
            return "Inter";
        } catch (Exception e) {
            return "SansSerif";
        }
    }

    /** Aplica la tipografía a todos los componentes de Swing (llamar antes de abrir ventanas). */
    public static void instalarFuenteGlobal() {
        javax.swing.plaf.FontUIResource base = new javax.swing.plaf.FontUIResource(FAMILIA, Font.PLAIN, 12);
        java.util.List<Object> claves = new java.util.ArrayList<>(UIManager.getDefaults().keySet());
        for (Object k : claves) {
            if (UIManager.get(k) instanceof javax.swing.plaf.FontUIResource) UIManager.put(k, base);
        }
        System.setProperty("awt.useSystemAAFontSettings", "on");
    }

    public static final Color FILA_ALTERNA = new Color(0xF7, 0xF3, 0xF4);
    public static final Color FILA_HOVER = new Color(0xF3, 0xE4, 0xE8);
    public static final Color FUCSIA_FONDO = new Color(0xFB, 0xE3, 0xEE);
    public static final Color FUCSIA_TEXTO = new Color(0xA8, 0x15, 0x5A);
    public static final Color GRIS_OSCURO = new Color(0x33, 0x41, 0x55);
    private static final Font NORMAL = new Font(FAMILIA, Font.PLAIN, 12);
    private static final Font NEGRITA = new Font(FAMILIA, Font.BOLD, 12);

    private EstiloUPB() { }

    // =================================================================== Tablas
    /** Fondo de una fila: seleccionada, bajo el mouse o alternada (cebra). */
    public static Color fondoFila(JTable t, int fila, boolean seleccionada) {
        if (seleccionada) return t.getSelectionBackground();
        Object hover = t.getClientProperty("filaHover");
        if (hover instanceof Integer && (Integer) hover == fila) return FILA_HOVER;
        return fila % 2 == 0 ? Color.WHITE : FILA_ALTERNA;
    }

    /** Filas alternadas, resaltado al pasar el mouse y renderizador por defecto con relleno. */
    public static void estilizarFilas(JTable t) {
        t.setDefaultRenderer(Object.class, new RenderizadorBase(SwingConstants.LEFT));
        t.setDefaultRenderer(Number.class, new RenderizadorBase(SwingConstants.CENTER));
        t.setDefaultRenderer(Integer.class, new RenderizadorBase(SwingConstants.CENTER));
        t.setFillsViewportHeight(true);
        t.setBackground(Color.WHITE);
        if (t.getClientProperty("hoverInstalado") == null) {
            t.putClientProperty("hoverInstalado", Boolean.TRUE);
            MouseAdapter m = new MouseAdapter() {
                @Override public void mouseMoved(MouseEvent e) {
                    int f = t.rowAtPoint(e.getPoint());
                    if (!Integer.valueOf(f).equals(t.getClientProperty("filaHover"))) {
                        t.putClientProperty("filaHover", f);
                        t.repaint();
                    }
                }
                @Override public void mouseExited(MouseEvent e) {
                    t.putClientProperty("filaHover", -1);
                    t.repaint();
                }
            };
            t.addMouseMotionListener(m);
            t.addMouseListener(m);
        }
        JTableHeader h = t.getTableHeader();
        h.setToolTipText("Haz clic en un encabezado para ordenar");
    }

    /** Compara textos como "14 m" o "110 m" por su valor numérico (para ordenar). */
    public static java.util.Comparator<Object> comparadorNumerico() {
        return (a, b) -> Double.compare(numero(a), numero(b));
    }

    private static double numero(Object o) {
        try {
            return Double.parseDouble(String.valueOf(o).replaceAll("[^0-9.,]", "").replace(',', '.'));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** Centra el contenido de una columna (números, distancias). */
    public static void centrarColumna(JTable t, int columnaVista) {
        t.getColumnModel().getColumn(columnaVista).setCellRenderer(new RenderizadorBase(SwingConstants.CENTER));
    }

    /** Tabla que muestra un mensaje centrado cuando no tiene filas. */
    public static JTable tablaConMensajeVacio(TableModel modelo, String mensaje) {
        JTable t = new JTable(modelo) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (getRowCount() == 0) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                    g2.setFont(new Font(FAMILIA, Font.ITALIC, 12));
                    g2.setColor(UIColores.TEXTO_MUTED);
                    FontMetrics fm = g2.getFontMetrics();
                    int x = (getWidth() - fm.stringWidth(mensaje)) / 2;
                    int y = Math.max(fm.getAscent() + 14, getVisibleRect().height / 2);
                    g2.drawString(mensaje, x, y);
                    g2.dispose();
                }
            }
        };
        t.setFillsViewportHeight(true);
        return t;
    }

    /** Renderizador de texto con el fondo de cebra y relleno interior. */
    public static class RenderizadorBase extends DefaultTableCellRenderer {
        private final int alineacion;
        public RenderizadorBase(int alineacion) { this.alineacion = alineacion; }
        @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foco, int f, int c) {
            JLabel l = (JLabel) super.getTableCellRendererComponent(t, v, sel, false, f, c);
            l.setHorizontalAlignment(alineacion);
            l.setBackground(fondoFila(t, f, sel));
            l.setForeground(UIColores.TEXTO_OSCURO);
            l.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
            return l;
        }
    }

    /** "Sí"/"No" de la columna Escaleras como etiqueta: negra "Con escaleras" o fucsia "Accesible". */
    public static class RenderizadorEscaleras extends DefaultTableCellRenderer {
        @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foco, int f, int c) {
            boolean escaleras = "Sí".equals(String.valueOf(v));
            JPanel celda = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 7));
            celda.setBackground(fondoFila(t, f, sel));
            celda.add(etiqueta(escaleras ? "Con escaleras" : "Accesible",
                    escaleras ? new Color(0xE2, 0xE8, 0xF0) : FUCSIA_FONDO,
                    escaleras ? GRIS_OSCURO : FUCSIA_TEXTO));
            return celda;
        }
    }

    /** Etiqueta redondeada ("pastilla") de color. */
    public static JLabel etiqueta(String texto, Color fondo, Color colorTexto) {
        JLabel l = new JLabel(texto) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), getHeight(), getHeight()));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        l.setOpaque(false);
        l.setBackground(fondo);
        l.setForeground(colorTexto);
        l.setFont(new Font(FAMILIA, Font.BOLD, 11));
        l.setBorder(BorderFactory.createEmptyBorder(3, 10, 3, 10));
        return l;
    }

    // =================================================================== Íconos
    /** Insignia con el identificador del edificio (círculo o pastilla, como en el grafo). */
    public static class IconoInsignia implements Icon {
        private final String texto;
        public IconoInsignia(String texto) { this.texto = texto; }
        private int ancho(Component c) {
            FontMetrics fm = c.getFontMetrics(NEGRITA);
            return Math.max(22, fm.stringWidth(texto) + 12);
        }
        @Override public int getIconWidth() { return texto.length() <= 2 ? 22 : texto.length() * 7 + 12; }
        @Override public int getIconHeight() { return 22; }
        @Override public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int w = Math.min(ancho(c), getIconWidth());
            g2.setColor(new Color(0xF7, 0xD9, 0xDE));
            g2.fill(new RoundRectangle2D.Double(x, y, w, 22, 22, 22));
            g2.setColor(UIColores.PRIMARIO);
            g2.setStroke(new BasicStroke(1.3f));
            g2.draw(new RoundRectangle2D.Double(x + 0.6, y + 0.6, w - 1.2, 20.8, 22, 22));
            g2.setFont(NEGRITA);
            FontMetrics fm = g2.getFontMetrics();
            g2.setColor(UIColores.PRIMARIO_OSCURO);
            g2.drawString(texto, x + (w - fm.stringWidth(texto)) / 2, y + 11 + fm.getAscent() / 2 - 2);
            g2.dispose();
        }
    }

    /** Candado pequeño dibujado (no depende de fuentes de emoji). */
    public static class IconoCandado implements Icon {
        private final Color color;
        public IconoCandado(Color color) { this.color = color; }
        @Override public int getIconWidth() { return 12; }
        @Override public int getIconHeight() { return 14; }
        @Override public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(1.6f));
            g2.drawArc(x + 2, y, 8, 10, 0, 180);
            g2.drawLine(x + 2, y + 5, x + 2, y + 6);
            g2.drawLine(x + 10, y + 5, x + 10, y + 6);
            g2.fill(new RoundRectangle2D.Double(x, y + 6, 12, 8, 3, 3));
            g2.dispose();
        }
    }

    /** Interruptor tipo switch (encendido / apagado) para JCheckBox. */
    public static class IconoInterruptor implements Icon {
        private final boolean encendido;
        public IconoInterruptor(boolean encendido) { this.encendido = encendido; }
        @Override public int getIconWidth() { return 38; }
        @Override public int getIconHeight() { return 20; }
        @Override public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            boolean activo = c == null || c.isEnabled();
            g2.setColor(!activo ? new Color(0xE2, 0xE8, 0xF0) : encendido ? UIColores.CAMINO_ACCESIBLE : new Color(0xCB, 0xD5, 0xE1));
            g2.fill(new RoundRectangle2D.Double(x, y, 38, 20, 20, 20));
            g2.setColor(Color.WHITE);
            g2.fillOval(encendido ? x + 20 : x + 3, y + 3, 14, 14);
            g2.dispose();
        }
    }

    /** Convierte una casilla de verificación en un interruptor, sin cambiar su lógica. */
    public static void comoInterruptor(JCheckBox cb) {
        cb.setIcon(new IconoInterruptor(false));
        cb.setSelectedIcon(new IconoInterruptor(true));
        cb.setRolloverIcon(new IconoInterruptor(false));
        cb.setRolloverSelectedIcon(new IconoInterruptor(true));
        cb.setPressedIcon(new IconoInterruptor(true));
        cb.setIconTextGap(8);
        cb.setFocusPainted(false);
        cb.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    // =================================================================== Secciones
    /** Borde de sección tipo tarjeta: contorno redondeado suave y título con acento vinotinto. */
    public static Border bordeSeccion(String titulo) {
        return new BordeSeccion(titulo);
    }

    private static final class BordeSeccion extends AbstractBorder {
        private final String titulo;
        BordeSeccion(String titulo) { this.titulo = titulo; }
        @Override public Insets getBorderInsets(Component c) { return new Insets(25, 10, 4, 10); }
        @Override public Insets getBorderInsets(Component c, Insets i) {
            i.top = 25; i.left = 10; i.bottom = 4; i.right = 10; return i;
        }
        @Override public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setColor(new Color(0xD5, 0xDC, 0xE6));
            g2.draw(new RoundRectangle2D.Double(x + 0.5, y + 0.5, w - 1.5, h - 1.5, 14, 14));
            g2.setColor(UIColores.PRIMARIO);
            g2.fillRoundRect(x + 11, y + 6, 4, 14, 3, 3);
            g2.setFont(new Font(FAMILIA, Font.BOLD, 13));
            g2.setColor(UIColores.TEXTO_OSCURO);
            g2.drawString(titulo, x + 21, y + 18);
            g2.dispose();
        }
    }

    // =================================================================== Pestañas
    /** Pestañas planas: la activa en blanco con texto y línea vinotinto. */
    public static void estilizarPestanas(JTabbedPane tp) {
        tp.setFont(new Font(FAMILIA, Font.BOLD, 12));
        tp.setUI(new BasicTabbedPaneUI() {
            @Override protected void installDefaults() {
                super.installDefaults();
                tabInsets = new Insets(7, 16, 7, 16);
                selectedTabPadInsets = new Insets(0, 0, 0, 0);
                contentBorderInsets = new Insets(1, 0, 0, 0);
                tabAreaInsets = new Insets(4, 6, 0, 6);
                textIconGap = 7;
            }
            @Override protected void paintTabBackground(Graphics g, int p, int i, int x, int y, int w, int h, boolean sel) {
                g.setColor(sel ? Color.WHITE : new Color(0xEE, 0xF1, 0xF5));
                g.fillRect(x, y, w, h);
            }
            @Override protected void paintTabBorder(Graphics g, int p, int i, int x, int y, int w, int h, boolean sel) {
                if (sel) {
                    g.setColor(UIColores.PRIMARIO);
                    g.fillRect(x, y + h - 3, w, 3);
                } else {
                    g.setColor(new Color(0xD5, 0xDC, 0xE6));
                    g.drawLine(x + w - 1, y + 5, x + w - 1, y + h - 5);
                }
            }
            @Override protected void paintText(Graphics g, int p, Font font, FontMetrics fm, int i, String title,
                                               Rectangle r, boolean sel) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2.setFont(font);
                g2.setColor(sel ? UIColores.PRIMARIO : UIColores.TEXTO_MUTED);
                BasicGraphicsUtils.drawStringUnderlineCharAt(tabPane, g2, title, -1, r.x, r.y + fm.getAscent());
            }
            @Override protected void paintFocusIndicator(Graphics g, int p, Rectangle[] r, int i, Rectangle ir, Rectangle tr, boolean sel) { }
            @Override protected void paintIcon(Graphics g, int p, int i, Icon icono, Rectangle r, boolean sel) {
                if (icono instanceof IconoPestana) {
                    ((IconoPestana) icono).pintar(g, r.x, r.y, sel ? UIColores.PRIMARIO : UIColores.TEXTO_MUTED);
                } else {
                    super.paintIcon(g, p, i, icono, r, sel);
                }
            }
            @Override protected void paintContentBorder(Graphics g, int p, int sel) {
                Rectangle area = new Rectangle(0, calculateTabAreaHeight(p, runCount, maxTabHeight), tabPane.getWidth(), 1);
                g.setColor(new Color(0xD5, 0xDC, 0xE6));
                g.fillRect(area.x, area.y, area.width, 1);
            }
        });
    }

    // =================================================================== Listas desplegables
    /** Lista desplegable plana con borde suave; opcionalmente con texto a mostrar por elemento. */
    public static <T> void estilizarCombo(JComboBox<T> cb, Function<Object, String> textoVisible) {
        cb.setUI(new BasicComboBoxUI() {
            @Override protected JButton createArrowButton() {
                JButton b = new JButton() {
                    @Override protected void paintComponent(Graphics g) {
                        Graphics2D g2 = (Graphics2D) g.create();
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        g2.setColor(UIColores.PRIMARIO);
                        g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                        int cx = getWidth() / 2, cy = getHeight() / 2;
                        g2.drawPolyline(new int[]{cx - 5, cx, cx + 5}, new int[]{cy - 2, cy + 3, cy - 2}, 3);
                        g2.dispose();
                    }
                };
                b.setBorder(BorderFactory.createEmptyBorder());
                b.setContentAreaFilled(false);
                b.setFocusPainted(false);
                b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                return b;
            }
            @Override public void paintCurrentValueBackground(Graphics g, Rectangle b, boolean foco) {
                g.setColor(Color.WHITE);
                g.fillRect(b.x, b.y, b.width, b.height);
            }
        });
        cb.setBackground(Color.WHITE);
        cb.setFont(NORMAL);
        cb.setBorder(new BordeCampo(new Color(0xCB, 0xD5, 0xE1), null, 3, 6, 3, 4, 10));
        cb.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        cb.setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> l, Object v, int i, boolean sel, boolean foco) {
                String txt = v == null ? "" : (textoVisible != null ? textoVisible.apply(v) : String.valueOf(v));
                JLabel lb = (JLabel) super.getListCellRendererComponent(l, txt, i, sel, false);
                lb.setBorder(BorderFactory.createEmptyBorder(5, 8, 5, 8));
                if (sel) { lb.setBackground(FILA_HOVER); lb.setForeground(UIColores.PRIMARIO_OSCURO); }
                return lb;
            }
        });
    }

    /** Borde redondeado de una línea, usado en listas desplegables y campos. */
    public static final class BordeRedondeado extends AbstractBorder {
        private final Color color;
        public BordeRedondeado(Color color) { this.color = color; }
        @Override public Insets getBorderInsets(Component c) { return new Insets(3, 6, 3, 4); }
        @Override public Insets getBorderInsets(Component c, Insets i) { i.set(3, 6, 3, 4); return i; }
        @Override public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.draw(new RoundRectangle2D.Double(x + 0.5, y + 0.5, w - 1, h - 1, 10, 10));
            g2.dispose();
        }
    }

    // =================================================================== Texto guía
    /** Envuelve un campo de texto para mostrar un texto guía mientras está vacío. */
    public static JLayer<JTextField> conTextoGuia(JTextField campo, String guia) {
        campo.setToolTipText(guia);
        return new JLayer<>(campo, new LayerUI<JTextField>() {
            @Override public void paint(Graphics g, JComponent c) {
                super.paint(g, c);
                if (campo.getText().isEmpty()) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                    g2.setFont(campo.getFont().deriveFont(Font.ITALIC));
                    g2.setColor(new Color(0x94, 0xA3, 0xB8));
                    Insets in = campo.getInsets();
                    FontMetrics fm = g2.getFontMetrics();
                    g2.drawString(guia, in.left + 2, (c.getHeight() - fm.getHeight()) / 2 + fm.getAscent());
                    g2.dispose();
                }
            }
        });
    }
    // =================================================================== Bordes curvos
    /**
     * Borde redondeado para campos, filas y tarjetas: dibuja el contorno curvo y
     * "pinta" las esquinas exteriores con el color del fondo que hay detrás, para que
     * el relleno cuadrado del componente no asome por fuera de la curva.
     * fondoExterno = null toma el fondo del primer contenedor opaco.
     */
    public static final class BordeCampo extends AbstractBorder {
        private final Color linea, fondoExterno;
        private final int arriba, izq, abajo, der, arco;
        public BordeCampo(Color linea, Color fondoExterno, int arriba, int izq, int abajo, int der) {
            this(linea, fondoExterno, arriba, izq, abajo, der, 12);
        }
        public BordeCampo(Color linea, Color fondoExterno, int arriba, int izq, int abajo, int der, int arco) {
            this.linea = linea; this.fondoExterno = fondoExterno;
            this.arriba = arriba; this.izq = izq; this.abajo = abajo; this.der = der; this.arco = arco;
        }
        @Override public Insets getBorderInsets(Component c) { return new Insets(arriba, izq, abajo, der); }
        @Override public Insets getBorderInsets(Component c, Insets i) { i.set(arriba, izq, abajo, der); return i; }
        @Override public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            RoundRectangle2D forma = new RoundRectangle2D.Double(x + 0.5, y + 0.5, w - 1, h - 1, arco, arco);
            java.awt.geom.Area esquinas = new java.awt.geom.Area(new Rectangle(x, y, w, h));
            esquinas.subtract(new java.awt.geom.Area(forma));
            g2.setColor(fondoExterno != null ? fondoExterno : fondoDetras(c));
            g2.fill(esquinas);
            g2.setColor(linea);
            g2.draw(forma);
            g2.dispose();
        }
    }

    /** Color del primer contenedor opaco detrás del componente. */
    public static Color fondoDetras(Component c) {
        Container p = c.getParent();
        while (p != null) {
            if (p.isOpaque() && p.getBackground() != null) return p.getBackground();
            p = p.getParent();
        }
        return UIColores.FONDO;
    }

    /** Aplica bordes curvos a un campo de texto (o contraseña) con relleno interior. */
    public static void redondearCampo(JComponent campo) {
        campo.setBorder(new BordeCampo(new Color(0xCB, 0xD5, 0xE1), null, 5, 9, 5, 9));
    }

    /** Panel de desplazamiento cuya tabla (encabezado incluido) tiene esquinas curvas. */
    public static JScrollPane scrollRedondeado(Component vista) {
        return new JScrollPane(vista) {
            @Override protected void paintChildren(Graphics g) {
                super.paintChildren(g);
                Insets in = getInsets();
                int x = in.left, y = in.top, w = getWidth() - in.left - in.right, h = getHeight() - in.top - in.bottom;
                if (w <= 0 || h <= 0) return;
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                RoundRectangle2D forma = new RoundRectangle2D.Double(x, y, w, h, 14, 14);
                java.awt.geom.Area esquinas = new java.awt.geom.Area(new Rectangle(x, y, w, h));
                esquinas.subtract(new java.awt.geom.Area(forma));
                g2.setColor(getBackground());
                g2.fill(esquinas);
                g2.setColor(new Color(0xD5, 0xDC, 0xE6));
                g2.draw(new RoundRectangle2D.Double(x + 0.5, y + 0.5, w - 1, h - 1, 14, 14));
                g2.dispose();
            }
        };
    }

    /** Panel de encabezado con las esquinas superiores redondeadas (usa su color de fondo). */
    public static JPanel encabezadoRedondeado(LayoutManager layout) {
        JPanel p = new JPanel(layout) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight() + 24, 24, 24));
                g2.dispose();
            }
        };
        p.setOpaque(false);
        return p;
    }

    /**
     * Etiqueta de título con dibujo de texto de precisión (métricas fraccionarias y
     * suavizado): evita los espacios irregulares entre letras que aparecen en Windows.
     */
    public static JLabel tituloNitido(String texto) {
        return new JLabel(texto) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                super.paintComponent(g);
            }
        };
    }

    // =================================================================== Íconos del itinerario
    /** Círculo vinotinto con el número del paso. */
    public static class IconoPaso implements Icon {
        private final String numero;
        public IconoPaso(int numero) { this.numero = String.valueOf(numero); }
        @Override public int getIconWidth() { return 26; }
        @Override public int getIconHeight() { return 26; }
        @Override public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setColor(UIColores.PRIMARIO);
            g2.fillOval(x, y, 26, 26);
            g2.setColor(Color.WHITE);
            g2.setFont(NEGRITA);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(numero, x + (26 - fm.stringWidth(numero)) / 2, y + 13 + fm.getAscent() / 2 - 2);
            g2.dispose();
        }
    }

    /** Círculo verde con una marca de llegada. */
    public static class IconoLlegada implements Icon {
        @Override public int getIconWidth() { return 26; }
        @Override public int getIconHeight() { return 26; }
        @Override public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(UIColores.EXITO_FONDO);
            g2.fillOval(x, y, 26, 26);
            g2.setColor(new Color(0x0B, 0x7A, 0x55));
            g2.setStroke(new BasicStroke(2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawPolyline(new int[]{x + 7, x + 11, x + 19}, new int[]{y + 13, y + 18, y + 8}, 3);
            g2.dispose();
        }
    }
    /** Círculo ámbar con signo de exclamación, para avisos suaves. */
    public static class IconoAlerta implements Icon {
        @Override public int getIconWidth() { return 16; }
        @Override public int getIconHeight() { return 16; }
        @Override public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(0xD9, 0x77, 0x06));
            g2.fillOval(x, y, 16, 16);
            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine(x + 8, y + 4, x + 8, y + 9);
            g2.fillOval(x + 7, y + 11, 3, 3);
            g2.dispose();
        }
    }
    // =================================================================== Convenciones plegables
    /** Flecha pequeña: hacia abajo (abierta) o hacia la derecha (cerrada). */
    public static void dibujarFlechaPlegar(Graphics2D g2, int cx, int cy, boolean abierta) {
        g2.setColor(UIColores.TEXTO_MUTED);
        g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        if (abierta) g2.drawPolyline(new int[]{cx - 4, cx, cx + 4}, new int[]{cy - 2, cy + 2, cy - 2}, 3);
        else g2.drawPolyline(new int[]{cx - 2, cx + 2, cx - 2}, new int[]{cy - 4, cy, cy + 4}, 3);
    }

    /** Convenciones minimizadas: pastilla "Convenciones" con flecha. Devuelve su zona de clic. */
    public static Rectangle dibujarLeyendaPlegada(Graphics2D g2, int x, int y) {
        g2.setFont(new Font(FAMILIA, Font.BOLD, 12));
        FontMetrics fm = g2.getFontMetrics();
        int w = fm.stringWidth("Convenciones") + 40, h = 28;
        g2.setColor(new Color(0, 0, 0, 30));
        g2.fillRoundRect(x + 2, y + 3, w, h, h, h);
        g2.setColor(Color.WHITE);
        g2.fillRoundRect(x, y, w, h, h, h);
        g2.setColor(new Color(0xD5, 0xDC, 0xE6));
        g2.setStroke(new BasicStroke(1f));
        g2.drawRoundRect(x, y, w, h, h, h);
        g2.setColor(UIColores.TEXTO_OSCURO);
        g2.drawString("Convenciones", x + 14, y + (h + fm.getAscent()) / 2 - 2);
        dibujarFlechaPlegar(g2, x + w - 16, y + h / 2, false);
        return new Rectangle(x, y, w, h);
    }
    // =================================================================== Íconos de interfaz
    /** Avatar de usuario: círculo con cabeza y hombros (en el color indicado). */
    public static class IconoUsuario implements Icon {
        private final Color color; private final int t;
        public IconoUsuario(Color color, int tamano) { this.color = color; this.t = tamano; }
        @Override public int getIconWidth() { return t; }
        @Override public int getIconHeight() { return t; }
        @Override public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            float borde = Math.max(1.5f, t / 13f);
            g2.setStroke(new BasicStroke(borde));
            g2.draw(new java.awt.geom.Ellipse2D.Double(x + borde / 2, y + borde / 2, t - borde, t - borde));
            java.awt.geom.Area area = new java.awt.geom.Area(new java.awt.geom.Ellipse2D.Double(x + t * 0.335, y + t * 0.18, t * 0.33, t * 0.33));
            java.awt.geom.Ellipse2D hombros = new java.awt.geom.Ellipse2D.Double(x + t * 0.2, y + t * 0.56, t * 0.6, t * 0.5);
            java.awt.geom.Area cuerpo = new java.awt.geom.Area(hombros);
            cuerpo.intersect(new java.awt.geom.Area(new java.awt.geom.Ellipse2D.Double(x + borde * 1.6, y + borde * 1.6, t - borde * 3.2, t - borde * 3.2)));
            area.add(cuerpo);
            g2.fill(area);
            g2.dispose();
        }
    }

    public enum TipoIcono { BUSCAR, MAPA, ADMIN, BLOQUEOS, EDIFICIOS, REPORTES, CONFIGURACION }

    /** Ícono vectorial de pestaña; su color lo decide la pestaña (activa o no). */
    public static class IconoPestana implements Icon {
        private final TipoIcono tipo;
        public IconoPestana(TipoIcono tipo) { this.tipo = tipo; }
        @Override public int getIconWidth() { return 16; }
        @Override public int getIconHeight() { return 16; }
        @Override public void paintIcon(Component c, Graphics g, int x, int y) { pintar(g, x, y, UIColores.TEXTO_MUTED); }

        public void pintar(Graphics g, int x, int y, Color color) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(1.7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.translate(x, y);
            switch (tipo) {
                case BUSCAR:
                    g2.drawOval(1, 1, 10, 10);
                    g2.drawLine(10, 10, 14, 14);
                    break;
                case MAPA: {
                    java.awt.geom.Path2D pin = new java.awt.geom.Path2D.Double();
                    pin.moveTo(8, 15);
                    pin.curveTo(3, 9.5, 2.5, 7, 2.5, 5.8);
                    pin.curveTo(2.5, 2.7, 5, 0.8, 8, 0.8);
                    pin.curveTo(11, 0.8, 13.5, 2.7, 13.5, 5.8);
                    pin.curveTo(13.5, 7, 13, 9.5, 8, 15);
                    g2.draw(pin);
                    g2.drawOval(6, 4, 4, 4);
                    break;
                }
                case ADMIN: {
                    java.awt.geom.Path2D esc = new java.awt.geom.Path2D.Double();
                    esc.moveTo(8, 1); esc.lineTo(14, 3.2); esc.lineTo(14, 7.5);
                    esc.curveTo(14, 11.2, 11.4, 13.6, 8, 15);
                    esc.curveTo(4.6, 13.6, 2, 11.2, 2, 7.5);
                    esc.lineTo(2, 3.2); esc.closePath();
                    g2.draw(esc);
                    g2.drawPolyline(new int[]{5, 7, 11}, new int[]{8, 10, 6}, 3);
                    break;
                }
                case BLOQUEOS:
                    g2.drawRoundRect(2, 7, 12, 8, 3, 3);
                    g2.drawArc(4, 1, 8, 10, 0, 180);
                    g2.drawLine(4, 6, 4, 7); g2.drawLine(12, 6, 12, 7);
                    g2.fillOval(7, 9, 2, 3);
                    break;
                case EDIFICIOS:
                    g2.drawRect(3, 2, 10, 13);
                    for (int fy = 4; fy <= 10; fy += 3) { g2.fillRect(5, fy, 2, 2); g2.fillRect(9, fy, 2, 2); }
                    g2.drawLine(7, 15, 7, 13); g2.drawLine(9, 15, 9, 13);
                    break;
                case REPORTES:
                    g2.drawLine(1, 15, 15, 15);
                    g2.fillRoundRect(2, 9, 3, 5, 1, 1);
                    g2.fillRoundRect(7, 5, 3, 9, 1, 1);
                    g2.fillRoundRect(12, 2, 3, 12, 1, 1);
                    break;
                case CONFIGURACION: {
                    g2.drawOval(5, 5, 6, 6);
                    for (int k = 0; k < 8; k++) {
                        double a = Math.PI / 4 * k;
                        g2.draw(new java.awt.geom.Line2D.Double(8 + Math.cos(a) * 5.2, 8 + Math.sin(a) * 5.2,
                                8 + Math.cos(a) * 7.2, 8 + Math.sin(a) * 7.2));
                    }
                    break;
                }
            }
            g2.dispose();
        }
    }
    /** Dos flechas (arriba y abajo) para el botón "Invertir"; no depende de la fuente. */
    public static class IconoInvertir implements Icon {
        private final Color color;
        public IconoInvertir(Color color) { this.color = color; }
        @Override public int getIconWidth() { return 12; }
        @Override public int getIconHeight() { return 14; }
        @Override public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(c != null && !c.isEnabled() ? UIColores.TEXTO_MUTED : color);
            g2.setStroke(new BasicStroke(1.7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine(x + 3, y + 1, x + 3, y + 13);
            g2.drawPolyline(new int[]{x, x + 3, x + 6}, new int[]{y + 4, y + 1, y + 4}, 3);
            g2.drawLine(x + 9, y + 1, x + 9, y + 13);
            g2.drawPolyline(new int[]{x + 6, x + 9, x + 12}, new int[]{y + 10, y + 13, y + 10}, 3);
            g2.dispose();
        }
    }
    /**
     * Ícono vectorial de un campo de texto a partir del símbolo que se usaba antes
     * (✉ correo, ⚿ contraseña, ☺ nombre). Se dibuja, así no depende de la fuente.
     */
    public static Icon iconoCampo(String simbolo) {
        final int tipo = "\u2709".equals(simbolo) ? 0 : "\u26BF".equals(simbolo) ? 1 : 2;
        if (tipo == 2) return new IconoUsuario(UIColores.PRIMARIO, 17);
        return new Icon() {
            @Override public int getIconWidth() { return 17; }
            @Override public int getIconHeight() { return 17; }
            @Override public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(UIColores.PRIMARIO);
                g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                if (tipo == 0) {          // sobre
                    g2.drawRoundRect(x + 1, y + 3, 15, 11, 3, 3);
                    g2.drawPolyline(new int[]{x + 2, x + 8, x + 15}, new int[]{y + 5, y + 10, y + 5}, 3);
                } else {                  // candado
                    g2.drawRoundRect(x + 2, y + 7, 13, 9, 3, 3);
                    g2.drawArc(x + 4, y + 1, 9, 11, 0, 180);
                    g2.drawLine(x + 4, y + 6, x + 4, y + 7);
                    g2.drawLine(x + 13, y + 6, x + 13, y + 7);
                    g2.fillOval(x + 7, y + 10, 3, 3);
                }
                g2.dispose();
            }
        };
    }
}
