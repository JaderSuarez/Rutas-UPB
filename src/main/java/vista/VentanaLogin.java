package vista;

import controlador.CampusControlador;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

public class VentanaLogin extends JFrame {
    private final CampusControlador controlador;
    private JTextField txtEmail;
    private JPasswordField txtPassword;
    private JToggleButton btnMostrarPass;
    private JLabel lblError;

    private static final char CARACTER_OCULTO = '\u2022';
    private static final Color CREMA = new Color(0xFD, 0xF9, 0xF0);

    public VentanaLogin(CampusControlador controlador) {
        this.controlador = controlador;
        setTitle("Rutas UPB - Inicio de Sesión");
        setSize(1000, 660);
        setMinimumSize(new Dimension(900, 620));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(new Color(0xEC, 0xEC, 0xEC));
        initUI();
    }

    private void initUI() {
        setLayout(new GridBagLayout());
        GridBagConstraints centro = new GridBagConstraints();
        centro.gridx = 0;
        centro.gridy = 0;
        add(crearTarjeta(), centro);
    }

    /** Tarjeta central elevada con el formulario. */
    private JPanel crearTarjeta() {
        JPanel tarjeta = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0, 0, 0, 28));
                g2.fill(new RoundRectangle2D.Double(4, 6, getWidth() - 8, getHeight() - 8, 24, 24));
                g2.setColor(CREMA);
                g2.fill(new RoundRectangle2D.Double(0, 0, getWidth() - 10, getHeight() - 12, 24, 24));
                g2.dispose();
            }
        };
        tarjeta.setOpaque(false);
        // Los componentes quedan dentro de la tarjeta crema (fuera del área de la sombra)
        tarjeta.setBorder(BorderFactory.createEmptyBorder(0, 0, 12, 10));
        tarjeta.setPreferredSize(new Dimension(520, 600));

        tarjeta.add(crearEncabezado(), BorderLayout.NORTH);
        tarjeta.add(crearCuerpo(), BorderLayout.CENTER);
        tarjeta.add(crearPie(), BorderLayout.SOUTH);
        return tarjeta;
    }

    /** Franja vinotinto con el logo y el nombre del sistema. */
    private JPanel crearEncabezado() {
        JPanel header = EstiloUPB.encabezadoRedondeado(new GridBagLayout());
        header.setBackground(UIColores.PRIMARIO);
        header.setPreferredSize(new Dimension(510, 108));
        header.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 10));

        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0;
        gc.gridy = 0;
        gc.gridheight = 2;
        gc.insets = new Insets(0, 26, 0, 14);
        header.add(new LogoUPB(60), gc);

        JLabel lblTitulo = EstiloUPB.tituloNitido("Sistema de Rutas Óptimas UPB");
        lblTitulo.setFont(new Font(EstiloUPB.FAMILIA, Font.BOLD, 19));
        lblTitulo.setForeground(UIColores.TEXTO_CLARO);

        JLabel lblSub = new JLabel("UPB Seccional Bucaramanga");
        lblSub.setFont(new Font(EstiloUPB.FAMILIA, Font.ITALIC, 13));
        lblSub.setForeground(new Color(0xE9, 0xD9, 0xC6));

        gc.gridx = 1;
        gc.gridheight = 1;
        gc.anchor = GridBagConstraints.WEST;
        gc.insets = new Insets(22, 0, 0, 20);
        header.add(lblTitulo, gc);

        gc.gridy = 1;
        gc.insets = new Insets(2, 0, 22, 20);
        header.add(lblSub, gc);

        return header;
    }

    private JPanel crearCuerpo() {
        JPanel cuerpo = new JPanel();
        cuerpo.setOpaque(false);
        cuerpo.setLayout(new BoxLayout(cuerpo, BoxLayout.Y_AXIS));
        cuerpo.setBorder(BorderFactory.createEmptyBorder(30, 40, 10, 50));

        JLabel lblIniciar = new JLabel("Iniciar Sesión");
        lblIniciar.setFont(new Font(EstiloUPB.FAMILIA, Font.BOLD, 21));
        lblIniciar.setForeground(UIColores.PRIMARIO);
        lblIniciar.setAlignmentX(Component.LEFT_ALIGNMENT);
        lblIniciar.setHorizontalAlignment(SwingConstants.CENTER);
        lblIniciar.setMaximumSize(new Dimension(Integer.MAX_VALUE, lblIniciar.getPreferredSize().height));
        cuerpo.add(lblIniciar);
        cuerpo.add(Box.createVerticalStrut(14));

        // ---------- Correo ----------
        cuerpo.add(etiquetaCampo("Correo institucional"));
        cuerpo.add(Box.createVerticalStrut(5));
        txtEmail = new JTextField("nombre.apellido@upb.edu.co");
        cuerpo.add(campoConIcono(txtEmail, "\u2709"));
        cuerpo.add(Box.createVerticalStrut(14));

        // ---------- Contraseña ----------
        cuerpo.add(etiquetaCampo("Contraseña"));
        cuerpo.add(Box.createVerticalStrut(5));
        txtPassword = new JPasswordField("12345");
        txtPassword.setEchoChar(CARACTER_OCULTO);

        btnMostrarPass = new JToggleButton("Mostrar");
        btnMostrarPass.setFont(new Font(EstiloUPB.FAMILIA, Font.PLAIN, 11));
        btnMostrarPass.setFocusPainted(false);
        BotonUPB.aplicar(btnMostrarPass, BotonUPB.Estilo.SECUNDARIO);
        btnMostrarPass.setBorder(BorderFactory.createEmptyBorder(3, 10, 3, 10));
        btnMostrarPass.addActionListener(e -> {
            if (btnMostrarPass.isSelected()) {
                txtPassword.setEchoChar((char) 0);
                btnMostrarPass.setText("Ocultar");
            } else {
                txtPassword.setEchoChar(CARACTER_OCULTO);
                btnMostrarPass.setText("Mostrar");
            }
        });

        JPanel filaPass = campoConIcono(txtPassword, "\u26BF");
        filaPass.add(btnMostrarPass, BorderLayout.EAST);
        cuerpo.add(filaPass);

        // ---------- Mensaje de error (altura reservada) ----------
        lblError = new JLabel(" ");
        lblError.setFont(new Font(EstiloUPB.FAMILIA, Font.PLAIN, 12));
        lblError.setForeground(UIColores.ERROR);
        lblError.setAlignmentX(Component.LEFT_ALIGNMENT);
        lblError.setPreferredSize(new Dimension(400, 20));
        lblError.setMinimumSize(new Dimension(400, 20));
        lblError.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
        cuerpo.add(Box.createVerticalStrut(6));
        cuerpo.add(lblError);
        cuerpo.add(Box.createVerticalStrut(6));

        // ---------- Botón principal ----------
        JButton btnLogin = botonPrimario("Iniciar Sesión");
        btnLogin.addActionListener(e -> autenticar());
        // Enter confirma el inicio de sesión desde cualquier campo
        getRootPane().setDefaultButton(btnLogin);
        cuerpo.add(btnLogin);
        cuerpo.add(Box.createVerticalStrut(10));

        // ---------- Enlaces ----------
        JPanel enlaces = new JPanel(new BorderLayout());
        enlaces.setOpaque(false);
        enlaces.setAlignmentX(Component.LEFT_ALIGNMENT);
        enlaces.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));

        JLabel lnkOlvido = enlace("¿Olvidaste tu contraseña?");
        lnkOlvido.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                mostrarError("Comunícate con la oficina de sistemas para restablecer tu contraseña.");
                lblError.setForeground(UIColores.TEXTO_MUTED);
            }
        });

        JLabel lnkRegistro = enlace("Regístrate aquí");
        lnkRegistro.setHorizontalAlignment(SwingConstants.RIGHT);
        lnkRegistro.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                new VentanaRegistro(controlador).setVisible(true);
            }
        });

        enlaces.add(lnkOlvido, BorderLayout.WEST);
        enlaces.add(lnkRegistro, BorderLayout.EAST);
        cuerpo.add(enlaces);
        cuerpo.add(Box.createVerticalStrut(16));

        // ---------- Separador ----------
        JPanel separador = new JPanel(new GridBagLayout());
        separador.setOpaque(false);
        separador.setAlignmentX(Component.LEFT_ALIGNMENT);
        separador.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
        JSeparator sepIzq = new JSeparator();
        JSeparator sepDer = new JSeparator();
        JLabel lblO = new JLabel("o continúa como");
        lblO.setFont(new Font(EstiloUPB.FAMILIA, Font.PLAIN, 12));
        lblO.setForeground(UIColores.TEXTO_MUTED);
        GridBagConstraints gs = new GridBagConstraints();
        gs.gridy = 0;
        gs.fill = GridBagConstraints.HORIZONTAL;
        gs.weightx = 1;
        gs.gridx = 0;
        separador.add(sepIzq, gs);
        gs.gridx = 1;
        gs.weightx = 0;
        gs.insets = new Insets(0, 10, 0, 10);
        separador.add(lblO, gs);
        gs.gridx = 2;
        gs.weightx = 1;
        gs.insets = new Insets(0, 0, 0, 0);
        separador.add(sepDer, gs);
        cuerpo.add(separador);
        cuerpo.add(Box.createVerticalStrut(12));

        // ---------- Accesos alternos ----------
        JButton btnGuest = botonContorno("Entrar como Invitado", UIColores.ACENTO, new Color(0x8C, 0x6B, 0x2E));
        btnGuest.addActionListener(e -> {
            controlador.iniciarSesionInvitado();
            abrirPrincipal("Invitado", false);
        });
        cuerpo.add(btnGuest);
        cuerpo.add(Box.createVerticalStrut(8));

        JButton btnAdmin = botonContorno("Acceso administrador", UIColores.PRIMARIO, UIColores.PRIMARIO);
        btnAdmin.setToolTipText("Abrir la pantalla de acceso para administradores");
        btnAdmin.addActionListener(e -> abrirAccesoAdministrativo());
        cuerpo.add(btnAdmin);

        return cuerpo;
    }

    private JPanel crearPie() {
        JPanel pie = new JPanel(new FlowLayout(FlowLayout.CENTER));
        pie.setOpaque(false);
        pie.setBorder(BorderFactory.createEmptyBorder(0, 0, 22, 10));
        JLabel lbl = new JLabel("UPB Seccional Bucaramanga · Proyecto de Aula");
        lbl.setFont(new Font(EstiloUPB.FAMILIA, Font.PLAIN, 11));
        lbl.setForeground(UIColores.TEXTO_MUTED);
        pie.add(lbl);
        return pie;
    }

    // ---------------- Componentes auxiliares ----------------
    private JLabel etiquetaCampo(String texto) {
        JLabel l = new JLabel(texto);
        l.setFont(new Font(EstiloUPB.FAMILIA, Font.BOLD, 12));
        l.setForeground(UIColores.TEXTO_OSCURO);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    /** Campo de texto con un icono a la izquierda, dentro de un marco claro. */
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

        campo.getDocument().addDocumentListener(new LimpiarErrorAlEscribir());
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

    private JButton botonContorno(String texto, Color colorBorde, Color colorTexto) {
        JButton b = new JButton(texto);
        b.setFont(new Font(EstiloUPB.FAMILIA, Font.BOLD, 13));
        b.setBackground(Color.WHITE);
        b.setForeground(colorTexto);
        b.setFocusPainted(false);
        b.setOpaque(true);
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(colorBorde, 2),
                BorderFactory.createEmptyBorder(9, 10, 9, 10)));
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        b.setAlignmentX(Component.LEFT_ALIGNMENT);
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        BotonUPB.aplicar(b, colorBorde.equals(UIColores.ACENTO) ? BotonUPB.Estilo.ACENTO : BotonUPB.Estilo.SECUNDARIO);
        return b;
    }

    private JLabel enlace(String texto) {
        JLabel l = new JLabel("<html><u>" + texto + "</u></html>");
        l.setFont(new Font(EstiloUPB.FAMILIA, Font.PLAIN, 12));
        l.setForeground(new Color(0x8C, 0x6B, 0x2E));
        l.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return l;
    }

    // ---------------- Lógica ----------------
    private void mostrarError(String mensaje) {
        lblError.setForeground(UIColores.ERROR);
        lblError.setText(mensaje);
    }

    private void limpiarError() {
        lblError.setText(" ");
    }

    private class LimpiarErrorAlEscribir implements javax.swing.event.DocumentListener {
        public void insertUpdate(javax.swing.event.DocumentEvent e) { limpiarError(); }
        public void removeUpdate(javax.swing.event.DocumentEvent e) { limpiarError(); }
        public void changedUpdate(javax.swing.event.DocumentEvent e) { limpiarError(); }
    }

    private void autenticar() {
        String email = txtEmail.getText().trim();
        String pass = new String(txtPassword.getPassword());

        if (email.isEmpty() || pass.isEmpty()) {
            mostrarError("Por favor ingrese correo y contraseña.");
            return;
        }
        if (!email.contains("@")) {
            mostrarError("Ingrese un correo válido.");
            return;
        }

        modelo.Usuario usuario = controlador.autenticar(email, pass);
        if (usuario == null) {
            mostrarError("Correo o contraseña incorrectos.");
            return;
        }
        if (usuario.esAdministrador()) {
            // Las cuentas administrativas entran por su propia pantalla
            mostrarError("Esta es una cuenta de administrador: ingresa por \"Acceso administrador\".");
            return;
        }
        abrirPrincipal(usuario.getNombre(), false);
    }

    /** Abre la pantalla exclusiva de acceso administrativo. */
    private void abrirAccesoAdministrativo() {
        this.dispose();
        SwingUtilities.invokeLater(() -> new VentanaLoginAdmin(controlador).setVisible(true));
    }

    private void abrirPrincipal(String usuario, boolean esAdmin) {
        this.dispose();
        SwingUtilities.invokeLater(() -> new VentanaPrincipal(controlador, usuario, esAdmin).setVisible(true));
    }
}
