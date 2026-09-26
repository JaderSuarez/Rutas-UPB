package vista;

import controlador.CampusControlador;
import modelo.Usuario;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

/**
 * Acceso exclusivo para cuentas con rol de administrador. Se abre desde el
 * botón "Acceso administrador" de la pantalla de inicio de sesión normal y solo deja
 * entrar a quienes tengan permisos de administración.
 */
public class VentanaLoginAdmin extends JFrame {

    private static final char CARACTER_OCULTO = '\u2022';
    private static final Color CREMA = new Color(0xFD, 0xF9, 0xF0);

    private final CampusControlador controlador;
    private JTextField txtCorreo;
    private JPasswordField txtClave;
    private JLabel lblError;

    public VentanaLoginAdmin(CampusControlador controlador) {
        this.controlador = controlador;
        setTitle("Rutas UPB - Acceso Administrativo");
        setSize(1000, 660);
        setMinimumSize(new Dimension(900, 620));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(new Color(0xE4, 0xE4, 0xE4));
        setLayout(new GridBagLayout());

        GridBagConstraints centro = new GridBagConstraints();
        centro.gridx = 0;
        centro.gridy = 0;
        add(crearTarjeta(), centro);
    }

    private JPanel crearTarjeta() {
        JPanel tarjeta = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0, 0, 0, 32));
                g2.fill(new RoundRectangle2D.Double(4, 6, getWidth() - 8, getHeight() - 8, 24, 24));
                g2.setColor(CREMA);
                g2.fill(new RoundRectangle2D.Double(0, 0, getWidth() - 10, getHeight() - 12, 24, 24));
                g2.dispose();
            }
        };
        tarjeta.setOpaque(false);
        // Los componentes quedan dentro de la tarjeta crema (fuera del área de la sombra)
        tarjeta.setBorder(BorderFactory.createEmptyBorder(0, 0, 12, 10));
        tarjeta.setPreferredSize(new Dimension(520, 540));

        tarjeta.add(crearEncabezado(), BorderLayout.NORTH);
        tarjeta.add(crearCuerpo(), BorderLayout.CENTER);
        return tarjeta;
    }

    /** Encabezado más oscuro, para diferenciarlo del ingreso de usuarios. */
    private JPanel crearEncabezado() {
        JPanel header = EstiloUPB.encabezadoRedondeado(new GridBagLayout());
        header.setBackground(UIColores.PRIMARIO_OSCURO);
        header.setPreferredSize(new Dimension(510, 112));
        header.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 10));

        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0;
        gc.gridy = 0;
        gc.gridheight = 2;
        gc.insets = new Insets(0, 26, 0, 14);
        header.add(new LogoUPB(58), gc);

        JLabel lblTitulo = EstiloUPB.tituloNitido("Acceso Administrativo");
        lblTitulo.setFont(new Font(EstiloUPB.FAMILIA, Font.BOLD, 20));
        lblTitulo.setForeground(UIColores.TEXTO_CLARO);

        JLabel lblSub = new JLabel("Gestión del sistema de rutas · UPB Seccional Bucaramanga");
        lblSub.setFont(new Font(EstiloUPB.FAMILIA, Font.ITALIC, 12));
        lblSub.setForeground(new Color(0xE9, 0xD9, 0xC6));

        gc.gridx = 1;
        gc.gridheight = 1;
        gc.anchor = GridBagConstraints.WEST;
        gc.insets = new Insets(24, 0, 0, 20);
        header.add(lblTitulo, gc);

        gc.gridy = 1;
        gc.insets = new Insets(2, 0, 24, 20);
        header.add(lblSub, gc);

        return header;
    }

    private JPanel crearCuerpo() {
        JPanel cuerpo = new JPanel();
        cuerpo.setOpaque(false);
        cuerpo.setLayout(new BoxLayout(cuerpo, BoxLayout.Y_AXIS));
        cuerpo.setBorder(BorderFactory.createEmptyBorder(28, 40, 20, 50));

        JLabel aviso = new JLabel("<html><div style='text-align:center'>Esta pantalla es solo para cuentas<br>con permisos de administración.</div></html>");
        aviso.setFont(new Font(EstiloUPB.FAMILIA, Font.PLAIN, 12));
        aviso.setForeground(UIColores.TEXTO_MUTED);
        aviso.setAlignmentX(Component.LEFT_ALIGNMENT);
        aviso.setHorizontalAlignment(SwingConstants.CENTER);
        aviso.setMaximumSize(new Dimension(Integer.MAX_VALUE, aviso.getPreferredSize().height));
        cuerpo.add(aviso);
        cuerpo.add(Box.createVerticalStrut(18));

        cuerpo.add(etiquetaCampo("Correo del administrador"));
        cuerpo.add(Box.createVerticalStrut(5));
        txtCorreo = new JTextField();
        cuerpo.add(campoConIcono(txtCorreo, "\u2709"));
        cuerpo.add(Box.createVerticalStrut(14));

        cuerpo.add(etiquetaCampo("Contraseña"));
        cuerpo.add(Box.createVerticalStrut(5));
        txtClave = new JPasswordField();
        txtClave.setEchoChar(CARACTER_OCULTO);

        JToggleButton btnMostrar = new JToggleButton("Mostrar");
        btnMostrar.setFont(new Font(EstiloUPB.FAMILIA, Font.PLAIN, 11));
        btnMostrar.setFocusPainted(false);
        BotonUPB.aplicar(btnMostrar, BotonUPB.Estilo.SECUNDARIO);
        btnMostrar.setBorder(BorderFactory.createEmptyBorder(3, 10, 3, 10));
        btnMostrar.addActionListener(e -> {
            if (btnMostrar.isSelected()) {
                txtClave.setEchoChar((char) 0);
                btnMostrar.setText("Ocultar");
            } else {
                txtClave.setEchoChar(CARACTER_OCULTO);
                btnMostrar.setText("Mostrar");
            }
        });

        JPanel filaClave = campoConIcono(txtClave, "\u26BF");
        filaClave.add(btnMostrar, BorderLayout.EAST);
        cuerpo.add(filaClave);

        lblError = new JLabel(" ");
        lblError.setFont(new Font(EstiloUPB.FAMILIA, Font.PLAIN, 12));
        lblError.setForeground(UIColores.ERROR);
        lblError.setAlignmentX(Component.LEFT_ALIGNMENT);
        lblError.setPreferredSize(new Dimension(400, 34));
        lblError.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        lblError.setVerticalAlignment(SwingConstants.TOP);
        cuerpo.add(Box.createVerticalStrut(8));
        cuerpo.add(lblError);

        JButton btnEntrar = botonPrimario("Ingresar al panel de administración");
        btnEntrar.addActionListener(e -> autenticar());
        cuerpo.add(btnEntrar);
        cuerpo.add(Box.createVerticalStrut(10));

        JButton btnVolver = botonContorno("Volver al inicio de sesión de usuarios");
        btnVolver.addActionListener(e -> volver());
        cuerpo.add(btnVolver);

        getRootPane().setDefaultButton(btnEntrar);
        getRootPane().registerKeyboardAction(e -> volver(),
                KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);

        return cuerpo;
    }

    // ---------------- Lógica ----------------
    private void autenticar() {
        String correo = txtCorreo.getText().trim();
        String clave = new String(txtClave.getPassword());

        if (correo.isEmpty() || clave.isEmpty()) {
            mostrarError("Ingresa el correo y la contraseña.");
            return;
        }

        Usuario usuario = controlador.autenticar(correo, clave);
        if (usuario == null) {
            mostrarError("Correo o contraseña incorrectos.");
            return;
        }
        if (!usuario.esAdministrador()) {
            mostrarError("Esta cuenta no tiene permisos de administrador. "
                    + "Usa el inicio de sesión de usuarios.");
            return;
        }

        dispose();
        SwingUtilities.invokeLater(() ->
                new VentanaPrincipal(controlador, usuario.getNombre(), true).setVisible(true));
    }

    private void volver() {
        dispose();
        SwingUtilities.invokeLater(() -> new VentanaLogin(controlador).setVisible(true));
    }

    private void mostrarError(String mensaje) {
        lblError.setText("<html>" + mensaje + "</html>");
    }

    private void limpiarError() {
        lblError.setText(" ");
    }

    // ---------------- Componentes auxiliares ----------------
    private JLabel etiquetaCampo(String texto) {
        JLabel l = new JLabel(texto);
        l.setFont(new Font(EstiloUPB.FAMILIA, Font.BOLD, 12));
        l.setForeground(UIColores.TEXTO_OSCURO);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private JPanel campoConIcono(JTextField campo, String icono) {
        JPanel fila = new JPanel(new BorderLayout(8, 0));
        fila.setBackground(Color.WHITE);
        fila.setBorder(new EstiloUPB.BordeCampo(new Color(0xD8, 0xCE, 0xB8), CREMA, 6, 10, 6, 8));
        fila.setAlignmentX(Component.LEFT_ALIGNMENT);
        fila.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));

        JLabel lblIcono = new JLabel(EstiloUPB.iconoCampo(icono));
        lblIcono.setFont(new Font(EstiloUPB.FAMILIA, Font.PLAIN, 16));
        lblIcono.setForeground(UIColores.PRIMARIO);

        campo.setFont(new Font(EstiloUPB.FAMILIA, Font.PLAIN, 14));
        campo.setBorder(BorderFactory.createEmptyBorder());

        fila.add(lblIcono, BorderLayout.WEST);
        fila.add(campo, BorderLayout.CENTER);

        campo.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { limpiarError(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { limpiarError(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { limpiarError(); }
        });
        return fila;
    }

    private JButton botonPrimario(String texto) {
        JButton b = new JButton(texto);
        b.setFont(new Font(EstiloUPB.FAMILIA, Font.BOLD, 14));
        b.setBackground(UIColores.PRIMARIO);
        b.setForeground(UIColores.TEXTO_CLARO);
        b.setFocusPainted(false);
        b.setOpaque(true);
        b.setBorder(BorderFactory.createEmptyBorder(12, 10, 12, 10));
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        b.setAlignmentX(Component.LEFT_ALIGNMENT);
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        BotonUPB.aplicar(b, BotonUPB.Estilo.PRINCIPAL);
        return b;
    }

    private JButton botonContorno(String texto) {
        JButton b = new JButton(texto);
        b.setFont(new Font(EstiloUPB.FAMILIA, Font.PLAIN, 13));
        b.setBackground(Color.WHITE);
        b.setForeground(UIColores.TEXTO_MUTED);
        b.setFocusPainted(false);
        b.setOpaque(true);
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIColores.BORDE, 1),
                BorderFactory.createEmptyBorder(9, 10, 9, 10)));
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        b.setAlignmentX(Component.LEFT_ALIGNMENT);
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        BotonUPB.aplicar(b, BotonUPB.Estilo.SECUNDARIO);
        return b;
    }
}
