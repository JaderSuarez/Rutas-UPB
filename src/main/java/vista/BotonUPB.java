package vista;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.plaf.UIResource;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.plaf.basic.BasicGraphicsUtils;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

/**
 * Sistema de botones de la aplicación: todos los botones comparten esquinas
 * redondeadas, cursor de mano y un efecto al pasar el mouse, con colores de la
 * paleta institucional (UIColores). Funciona con JButton y JToggleButton.
 *
 *  PRINCIPAL        relleno vinotinto: la acción clave de cada zona.
 *  SECUNDARIO       contorno vinotinto: acciones de apoyo.
 *  ACENTO           contorno dorado sobre fondo claro (p. ej. "Entrar como Invitado").
 *  ACENTO_CABECERA  contorno dorado sobre la cabecera vinotinto ("Ayuda").
 *  CABECERA         contorno blanco sobre la cabecera vinotinto ("Cerrar Sesión").
 *  PELIGRO          contorno rojo: acciones destructivas ("Eliminar...").
 */
public final class BotonUPB {

    public enum Estilo { PRINCIPAL, SECUNDARIO, ACENTO, ACENTO_CABECERA, CABECERA, PELIGRO }

    private static final int ARCO = 14;           // curvatura de las esquinas
    private static final Color DORADO_TEXTO = new Color(0x8C, 0x6B, 0x2E);
    private static final Color ROJO_BORDE = new Color(0xDC, 0x26, 0x26);
    private static final Color ROJO_TEXTO = new Color(0xB9, 0x1C, 0x1C);

    private BotonUPB() { }

    /** Crea un botón con el estilo indicado. */
    public static JButton crear(String texto, Estilo estilo) {
        JButton b = new JButton(texto);
        aplicar(b, estilo);
        return b;
    }

    /** Aplica (o cambia) el estilo de un botón existente. Se puede llamar varias veces. */
    public static void aplicar(AbstractButton b, Estilo estilo) {
        b.setUI(new EstiloUI(estilo));
        Border borde = b.getBorder();
        if (borde == null || borde instanceof UIResource) {
            b.setBorder(BorderFactory.createEmptyBorder(6, 14, 6, 14));
        }
        if (b.getFont() == null || b.getFont() instanceof UIResource) {
            b.setFont(new Font(EstiloUPB.FAMILIA, Font.BOLD, 12));
        }
        b.setOpaque(false);
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setRolloverEnabled(true);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    // ------------------------------------------------------------------
    private static final class EstiloUI extends BasicButtonUI {
        private final Estilo estilo;

        EstiloUI(Estilo estilo) { this.estilo = estilo; }

        /** {relleno, borde, texto} según el estado del botón. relleno null = transparente. */
        private Color[] colores(AbstractButton b) {
            ButtonModel m = b.getModel();
            boolean activo = b.isEnabled();
            boolean encima = activo && m.isRollover();
            boolean presionado = activo && m.isArmed() && m.isPressed();
            Color blanco = UIColores.TEXTO_CLARO;
            switch (estilo) {
                case PRINCIPAL:
                    if (!activo) return new Color[]{mezcla(UIColores.PRIMARIO, Color.WHITE, 0.55f), null, blanco};
                    Color f = presionado ? oscurecer(UIColores.PRIMARIO_OSCURO)
                            : encima ? UIColores.PRIMARIO_OSCURO : UIColores.PRIMARIO;
                    return new Color[]{f, null, blanco};
                case SECUNDARIO:
                    if (!activo) return new Color[]{UIColores.TARJETA, UIColores.BORDE, UIColores.TEXTO_MUTED};
                    return new Color[]{presionado ? mezcla(UIColores.PRIMARIO, Color.WHITE, 0.18f)
                            : encima ? mezcla(UIColores.PRIMARIO, Color.WHITE, 0.09f) : UIColores.TARJETA,
                            UIColores.PRIMARIO, UIColores.PRIMARIO};
                case ACENTO:
                    if (!activo) return new Color[]{UIColores.TARJETA, UIColores.BORDE, UIColores.TEXTO_MUTED};
                    return new Color[]{presionado ? mezcla(UIColores.ACENTO, Color.WHITE, 0.35f)
                            : encima ? mezcla(UIColores.ACENTO, Color.WHITE, 0.18f) : UIColores.TARJETA,
                            UIColores.ACENTO, DORADO_TEXTO};
                case ACENTO_CABECERA:
                    if (encima || presionado) return new Color[]{UIColores.ACENTO, UIColores.ACENTO, UIColores.PRIMARIO_OSCURO};
                    return new Color[]{null, UIColores.ACENTO, UIColores.ACENTO};
                case CABECERA:
                    if (encima || presionado) return new Color[]{blanco, blanco, UIColores.PRIMARIO};
                    return new Color[]{null, blanco, blanco};
                case PELIGRO:
                default:
                    if (!activo) return new Color[]{UIColores.TARJETA, UIColores.BORDE, UIColores.TEXTO_MUTED};
                    return new Color[]{presionado ? mezcla(ROJO_BORDE, Color.WHITE, 0.18f)
                            : encima ? UIColores.ERROR_FONDO : UIColores.TARJETA,
                            ROJO_BORDE, ROJO_TEXTO};
            }
        }

        @Override
        public void paint(Graphics g, JComponent c) {
            AbstractButton b = (AbstractButton) c;
            Color[] col = colores(b);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            float w = c.getWidth(), h = c.getHeight();
            RoundRectangle2D forma = new RoundRectangle2D.Float(0.75f, 0.75f, w - 1.5f, h - 1.5f, ARCO, ARCO);
            if (col[0] != null) {
                g2.setColor(col[0]);
                g2.fill(forma);
            }
            if (col[1] != null) {
                g2.setColor(col[1]);
                g2.setStroke(new BasicStroke(1.5f));
                g2.draw(forma);
            }
            g2.dispose();
            super.paint(g, c);   // texto e ícono
        }

        @Override
        protected void paintText(Graphics g, AbstractButton b, Rectangle r, String texto) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            FontMetrics fm = g.getFontMetrics(b.getFont());
            g.setColor(colores(b)[2]);
            BasicGraphicsUtils.drawStringUnderlineCharAt(b, g2, texto, b.getDisplayedMnemonicIndex(),
                    r.x + getTextShiftOffset(), r.y + fm.getAscent() + getTextShiftOffset());
        }

        @Override
        protected void paintButtonPressed(Graphics g, AbstractButton b) { /* ya se dibuja en paint */ }

        @Override
        protected void paintFocus(Graphics g, AbstractButton b, Rectangle v, Rectangle t, Rectangle i) { }
    }

    private static Color mezcla(Color a, Color base, float proporcionA) {
        float p = proporcionA, q = 1 - p;
        return new Color(Math.round(a.getRed() * p + base.getRed() * q),
                Math.round(a.getGreen() * p + base.getGreen() * q),
                Math.round(a.getBlue() * p + base.getBlue() * q));
    }

    private static Color oscurecer(Color c) {
        return new Color((int) (c.getRed() * 0.8), (int) (c.getGreen() * 0.8), (int) (c.getBlue() * 0.8));
    }
}
