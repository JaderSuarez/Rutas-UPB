package vista;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;

/**
 * Insignia institucional simplificada para el proyecto: cruz roja de fondo,
 * escudo dorado y campo oscuro con las letras UPB. Se dibuja con Graphics2D
 * para no depender de archivos de imagen externos.
 */
public class LogoUPB extends JComponent {

    private static final Color ROJO = new Color(0xD8, 0x1E, 0x1E);
    private static final Color DORADO = new Color(0xF5, 0xC2, 0x1A);
    private static final Color OSCURO = new Color(0x1A, 0x1A, 0x1A);

    private final int lado;

    public LogoUPB(int lado) {
        this.lado = lado;
        setPreferredSize(new Dimension(lado, lado));
        setMinimumSize(new Dimension(lado, lado));
        setOpaque(false);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        double s = lado / 100.0;   // factor de escala sobre un lienzo base de 100x100
        g2.translate((getWidth() - lado) / 2.0, (getHeight() - lado) / 2.0);

        // ---- Cruz roja con brazos en forma de trompeta ----
        g2.setColor(ROJO);
        g2.fill(brazo(50 * s, 50 * s, 0, s));                 // arriba
        g2.fill(brazo(50 * s, 50 * s, Math.PI, s));           // abajo
        g2.fill(brazo(50 * s, 50 * s, -Math.PI / 2, s));      // izquierda
        g2.fill(brazo(50 * s, 50 * s, Math.PI / 2, s));       // derecha

        // ---- Escudo dorado ----
        g2.setColor(DORADO);
        g2.fill(new RoundRectangle2D.Double(24 * s, 18 * s, 52 * s, 62 * s, 12 * s, 12 * s));

        // ---- Campo oscuro interior ----
        g2.setColor(OSCURO);
        g2.fill(new RoundRectangle2D.Double(31 * s, 25 * s, 38 * s, 46 * s, 7 * s, 7 * s));

        // ---- Letras: A  Ω arriba, UPB abajo ----
        g2.setColor(DORADO);
        int tamAO = Math.max(6, (int) (13 * s));
        g2.setFont(new Font("Serif", Font.BOLD, tamAO));
        FontMetrics fmAO = g2.getFontMetrics();
        String alfaOmega = "A\u03A9";
        g2.drawString(alfaOmega, (float) (50 * s - fmAO.stringWidth(alfaOmega) / 2.0), (float) (40 * s));

        int tamUPB = Math.max(7, (int) (16 * s));
        g2.setFont(new Font("Serif", Font.BOLD, tamUPB));
        FontMetrics fmUPB = g2.getFontMetrics();
        String upb = "UPB";
        g2.drawString(upb, (float) (50 * s - fmUPB.stringWidth(upb) / 2.0), (float) (64 * s));

        g2.dispose();
    }

    /** Brazo de la cruz: trapecio que se ensancha hacia el exterior. */
    private Path2D brazo(double cx, double cy, double rotacion, double s) {
        Path2D p = new Path2D.Double();
        p.moveTo(-5 * s, -30 * s);
        p.lineTo(5 * s, -30 * s);
        p.lineTo(22 * s, -48 * s);
        p.lineTo(-22 * s, -48 * s);
        p.closePath();

        java.awt.geom.AffineTransform t = new java.awt.geom.AffineTransform();
        t.translate(cx, cy);
        t.rotate(rotacion);
        return new Path2D.Double(t.createTransformedShape(p));
    }
}
