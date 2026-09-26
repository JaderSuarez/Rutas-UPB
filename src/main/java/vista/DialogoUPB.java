package vista;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.geom.RoundRectangle2D;

/**
 * Ventanas de mensaje del sistema con la paleta institucional. Reemplaza a
 * JOptionPane conservando sus parámetros y sus valores de respuesta
 * (JOptionPane.YES_OPTION, NO_OPTION, CANCEL_OPTION), para que el código que
 * las usa no cambie su lógica.
 */
public final class DialogoUPB {

    private DialogoUPB() { }

    /** Mensaje informativo, de advertencia o de error (mismo orden de parámetros que JOptionPane). */
    public static void mensaje(Component padre, Object mensaje, String titulo, int tipo) {
        mostrar(padre, mensaje, titulo, tipo, false, JOptionPane.DEFAULT_OPTION);
    }

    public static void mensaje(Component padre, Object mensaje) {
        mensaje(padre, mensaje, "Rutas UPB", JOptionPane.INFORMATION_MESSAGE);
    }

    /** Confirmación Sí/No (o Aceptar/Cancelar). Devuelve los mismos valores que JOptionPane. */
    public static int confirmar(Component padre, Object mensaje, String titulo, int opciones, int tipo) {
        return mostrar(padre, mensaje, titulo, tipo, true, opciones);
    }

    public static int confirmar(Component padre, Object mensaje, String titulo, int opciones) {
        return confirmar(padre, mensaje, titulo, opciones, JOptionPane.QUESTION_MESSAGE);
    }

    // ------------------------------------------------------------------
    private static int mostrar(Component padre, Object mensaje, String titulo, int tipo,
                               boolean esConfirmacion, int opciones) {
        Window ventana = padre == null ? null : (padre instanceof Window ? (Window) padre : SwingUtilities.getWindowAncestor(padre));
        JDialog d = new JDialog(ventana, titulo, Dialog.ModalityType.APPLICATION_MODAL);
        final int[] respuesta = {esConfirmacion
                ? (opciones == JOptionPane.OK_CANCEL_OPTION ? JOptionPane.CANCEL_OPTION : JOptionPane.NO_OPTION)
                : JOptionPane.CLOSED_OPTION};

        JPanel cuerpo = new JPanel(new BorderLayout(16, 12));
        cuerpo.setBackground(Color.WHITE);
        cuerpo.setBorder(BorderFactory.createEmptyBorder(20, 22, 16, 22));

        JLabel icono = new JLabel(new IconoAviso(tipo));
        icono.setVerticalAlignment(SwingConstants.TOP);
        cuerpo.add(icono, BorderLayout.WEST);

        JPanel textos = new JPanel();
        textos.setOpaque(false);
        textos.setLayout(new BoxLayout(textos, BoxLayout.Y_AXIS));
        JLabel lblTitulo = new JLabel(titulo);
        lblTitulo.setFont(new Font(EstiloUPB.FAMILIA, Font.BOLD, 15));
        lblTitulo.setForeground(UIColores.TEXTO_OSCURO);
        lblTitulo.setAlignmentX(Component.LEFT_ALIGNMENT);
        textos.add(lblTitulo);
        textos.add(Box.createVerticalStrut(8));
        if (mensaje instanceof Component) {
            ((JComponent) mensaje).setAlignmentX(Component.LEFT_ALIGNMENT);
            textos.add((Component) mensaje);
        } else {
            JLabel lblMensaje = new JLabel(aHtml(String.valueOf(mensaje)));
            lblMensaje.setFont(new Font(EstiloUPB.FAMILIA, Font.PLAIN, 13));
            lblMensaje.setForeground(new Color(0x33, 0x41, 0x55));
            lblMensaje.setAlignmentX(Component.LEFT_ALIGNMENT);
            textos.add(lblMensaje);
        }
        cuerpo.add(textos, BorderLayout.CENTER);

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        botones.setOpaque(false);
        JButton porDefecto;
        if (esConfirmacion) {
            boolean okCancel = opciones == JOptionPane.OK_CANCEL_OPTION;
            boolean delicado = tipo == JOptionPane.WARNING_MESSAGE || tipo == JOptionPane.ERROR_MESSAGE;
            JButton no = BotonUPB.crear(okCancel ? "Cancelar" : "No", BotonUPB.Estilo.SECUNDARIO);
            JButton si = BotonUPB.crear(okCancel ? "Aceptar" : "Sí, continuar",
                    delicado ? BotonUPB.Estilo.PELIGRO : BotonUPB.Estilo.PRINCIPAL);
            si.addActionListener(e -> { respuesta[0] = JOptionPane.YES_OPTION; d.dispose(); });
            no.addActionListener(e -> d.dispose());
            botones.add(no);
            botones.add(si);
            // En acciones delicadas, Enter no confirma por accidente
            porDefecto = delicado ? no : si;
        } else {
            JButton ok = BotonUPB.crear("Entendido", BotonUPB.Estilo.PRINCIPAL);
            ok.addActionListener(e -> { respuesta[0] = JOptionPane.OK_OPTION; d.dispose(); });
            botones.add(ok);
            porDefecto = ok;
        }
        for (Component b : botones.getComponents()) {
            ((JComponent) b).setBorder(BorderFactory.createEmptyBorder(8, 18, 8, 18));
        }

        JPanel contenido = new JPanel(new BorderLayout());
        contenido.setBackground(Color.WHITE);
        JPanel franja = new JPanel();
        franja.setBackground(colorDe(tipo));
        franja.setPreferredSize(new Dimension(10, 4));
        contenido.add(franja, BorderLayout.NORTH);
        contenido.add(cuerpo, BorderLayout.CENTER);
        JPanel pie = new JPanel(new BorderLayout());
        pie.setBackground(new Color(0xF8, 0xFA, 0xFC));
        pie.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(0xE2, 0xE8, 0xF0)),
                BorderFactory.createEmptyBorder(12, 22, 12, 22)));
        pie.add(botones, BorderLayout.EAST);
        contenido.add(pie, BorderLayout.SOUTH);

        d.setContentPane(contenido);
        d.getRootPane().setDefaultButton(porDefecto);
        d.getRootPane().registerKeyboardAction(e -> d.dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        d.setResizable(false);
        d.pack();
        d.setMinimumSize(new Dimension(380, d.getHeight()));
        d.setLocationRelativeTo(ventana);
        SwingUtilities.invokeLater(porDefecto::requestFocusInWindow);
        d.setVisible(true);
        return respuesta[0];
    }

    /** Convierte el texto (con saltos de línea y viñetas) a HTML con ancho máximo. */
    private static String aHtml(String texto) {
        String esc = texto.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\n", "<br>");
        return "<html><div style='width:330px'>" + esc + "</div></html>";
    }

    static Color colorDe(int tipo) {
        switch (tipo) {
            case JOptionPane.ERROR_MESSAGE:   return new Color(0xDC, 0x26, 0x26);
            case JOptionPane.WARNING_MESSAGE: return new Color(0xD9, 0x77, 0x06);
            case JOptionPane.QUESTION_MESSAGE:return UIColores.PRIMARIO;
            default:                          return UIColores.PRIMARIO;
        }
    }

    /** Ícono circular dibujado según el tipo de mensaje (i, !, ?, ×). */
    static final class IconoAviso implements Icon {
        private final int tipo;
        IconoAviso(int tipo) { this.tipo = tipo; }
        @Override public int getIconWidth() { return 40; }
        @Override public int getIconHeight() { return 40; }
        @Override public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            Color col = colorDe(tipo);
            g2.setColor(new Color(col.getRed(), col.getGreen(), col.getBlue(), 32));
            g2.fillOval(x, y, 40, 40);
            g2.setColor(col);
            g2.fillOval(x + 7, y + 7, 26, 26);
            g2.setColor(Color.WHITE);
            if (tipo == JOptionPane.ERROR_MESSAGE) {
                g2.setStroke(new BasicStroke(2.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawLine(x + 15, y + 15, x + 25, y + 25);
                g2.drawLine(x + 25, y + 15, x + 15, y + 25);
            } else {
                String s = tipo == JOptionPane.WARNING_MESSAGE ? "!" : tipo == JOptionPane.QUESTION_MESSAGE ? "?" : "i";
                g2.setFont(new Font(EstiloUPB.FAMILIA, Font.BOLD, 16));
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(s, x + 20 - fm.stringWidth(s) / 2, y + 20 + fm.getAscent() / 2 - 2);
            }
            g2.dispose();
        }
    }
}
