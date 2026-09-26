package vista;

import controlador.CampusControlador;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

public class VentanaRegistro extends JFrame {

    private final CampusControlador controlador;
    private JComboBox<String> cbRol;

    private static final char CARACTER_OCULTO = '\u2022';
    private static final Color CREMA = new Color(0xFD, 0xF9, 0xF0);

    private JTextField txtNombre;
    private JTextField txtCorreo;
    private JPasswordField txtPass;
    private JLabel lblError;

    public VentanaRegistro(CampusControlador controlador) {
        this.controlador = controlador;
        setTitle("Registro de Usuario UPB");
        // Tamaño fijo: así el mensaje de error no reacomoda la ventana
        setSize(520, 680);
        setMinimumSize(new Dimension(520, 680));
        setResizable(false);
        setLocationRelativeTo(null);
        getContentPane().setBackground(new Color(0xEC, 0xEC, 0xEC));
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
        tarjeta.setPreferredSize(new Dimension(470, 620));

        tarjeta.add(crearEncabezado(), BorderLayout.NORTH);
        tarjeta.add(crearCuerpo(), BorderLayout.CENTER);
        return tarjeta;
    }

    private JPanel crearEncabezado() {
        JPanel header = EstiloUPB.encabezadoRedondeado(new GridBagLayout());
        header.setBackground(UIColores.PRIMARIO);
        header.setPreferredSize(new Dimension(460, 96));
        header.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 10));

        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0;
        gc.gridy = 0;
        gc.gridheight = 2;
        gc.insets = new Insets(0, 24, 0, 14);
        header.add(new LogoUPB(54), gc);

        JLabel lblTitulo = EstiloUPB.tituloNitido("Crear Cuenta");
        lblTitulo.setFont(new Font(EstiloUPB.FAMILIA, Font.BOLD, 19));
        lblTitulo.setForeground(UIColores.TEXTO_CLARO);

        JLabel lblSub = new JLabel("Sistema de Rutas Óptimas UPB");
        lblSub.setFont(new Font(EstiloUPB.FAMILIA, Font.ITALIC, 12));
        lblSub.setForeground(new Color(0xE9, 0xD9, 0xC6));

        gc.gridx = 1;
        gc.gridheight = 1;
        gc.anchor = GridBagConstraints.WEST;
        gc.insets = new Insets(20, 0, 0, 20);
        header.add(lblTitulo, gc);

        gc.gridy = 1;
        gc.insets = new Insets(2, 0, 20, 20);
        header.add(lblSub, gc);

        return header;
    }

    private JPanel crearCuerpo() {
        JPanel cuerpo = new JPanel();
        cuerpo.setOpaque(false);
        cuerpo.setLayout(new BoxLayout(cuerpo, BoxLayout.Y_AXIS));
        cuerpo.setBorder(BorderFactory.createEmptyBorder(26, 36, 20, 46));

        cuerpo.add(etiquetaCampo("Nombre completo"));
        cuerpo.add(Box.createVerticalStrut(5));
        txtNombre = new JTextField();
        cuerpo.add(campoConIcono(txtNombre, "\u263A"));
        cuerpo.add(Box.createVerticalStrut(14));

        cuerpo.add(etiquetaCampo("Correo institucional"));
        cuerpo.add(Box.createVerticalStrut(5));
        txtCorreo = new JTextField();
        cuerpo.add(campoConIcono(txtCorreo, "\u2709"));
        cuerpo.add(Box.createVerticalStrut(14));

        cuerpo.add(etiquetaCampo("Tipo de usuario"));
        cuerpo.add(Box.createVerticalStrut(5));
        cbRol = new JComboBox<>(new String[]{"Estudiante", "Empleado"});
        EstiloUPB.estilizarCombo(cbRol, null);
        cbRol.setBorder(new EstiloUPB.BordeCampo(new Color(0xD8, 0xCE, 0xB8), CREMA, 3, 6, 3, 4));
        cbRol.setFont(new Font(EstiloUPB.FAMILIA, Font.PLAIN, 13));
        cbRol.setBackground(Color.WHITE);
        cbRol.setAlignmentX(Component.LEFT_ALIGNMENT);
        cbRol.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        cuerpo.add(cbRol);
        cuerpo.add(Box.createVerticalStrut(14));

        cuerpo.add(etiquetaCampo("Contraseña"));
        cuerpo.add(Box.createVerticalStrut(5));
        txtPass = new JPasswordField();
        txtPass.setEchoChar(CARACTER_OCULTO);
        JToggleButton btnMostrar = new JToggleButton("Mostrar");
        btnMostrar.setFont(new Font(EstiloUPB.FAMILIA, Font.PLAIN, 11));
        btnMostrar.setFocusPainted(false);
        BotonUPB.aplicar(btnMostrar, BotonUPB.Estilo.SECUNDARIO);
        btnMostrar.setBorder(BorderFactory.createEmptyBorder(3, 10, 3, 10));
        btnMostrar.addActionListener(e -> {
            if (btnMostrar.isSelected()) {
                txtPass.setEchoChar((char) 0);
                btnMostrar.setText("Ocultar");
            } else {
                txtPass.setEchoChar(CARACTER_OCULTO);
                btnMostrar.setText("Mostrar");
            }
        });
        JPanel filaPass = campoConIcono(txtPass, "\u26BF");
        filaPass.add(btnMostrar, BorderLayout.EAST);
        cuerpo.add(filaPass);

        // Espacio reservado para el mensaje: no mueve los botones al aparecer
        lblError = new JLabel(" ");
        lblError.setFont(new Font(EstiloUPB.FAMILIA, Font.PLAIN, 12));
        lblError.setForeground(UIColores.ERROR);
        lblError.setAlignmentX(Component.LEFT_ALIGNMENT);
        lblError.setPreferredSize(new Dimension(380, 34));
        lblError.setMinimumSize(new Dimension(380, 34));
        lblError.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        lblError.setVerticalAlignment(SwingConstants.TOP);
        cuerpo.add(Box.createVerticalStrut(8));
        cuerpo.add(lblError);

        JButton btnRegistrar = botonPrimario("Registrar");
        cuerpo.add(btnRegistrar);
        cuerpo.add(Box.createVerticalStrut(8));

        JButton btnCancelar = botonContorno("Cancelar");
        btnCancelar.addActionListener(e -> dispose());
        cuerpo.add(btnCancelar);

        JLabel nota = new JLabel("Debes usar tu correo UPB (@upb.edu.co)");
        nota.setFont(new Font(EstiloUPB.FAMILIA, Font.ITALIC, 11));
        nota.setForeground(UIColores.TEXTO_MUTED);
        nota.setAlignmentX(Component.LEFT_ALIGNMENT);
        nota.setHorizontalAlignment(SwingConstants.CENTER);
        nota.setMaximumSize(new Dimension(Integer.MAX_VALUE, nota.getPreferredSize().height));
        cuerpo.add(Box.createVerticalStrut(12));
        cuerpo.add(nota);

        btnRegistrar.addActionListener(e -> validarRegistro());

        // Enter registra; Esc cierra la ventana sin registrar
        getRootPane().setDefaultButton(btnRegistrar);
        getRootPane().registerKeyboardAction(e -> dispose(),
                KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);

        return cuerpo;
    }

    private void validarRegistro() {
        String nombre = txtNombre.getText().trim();
        String correo = txtCorreo.getText().trim();
        String pass = new String(txtPass.getPassword());

        if (nombre.isEmpty()) {
            mostrarError("Ingresa tu nombre completo.");
        } else if (!correo.toLowerCase().endsWith("@upb.edu.co")) {
            mostrarError("Debe ser un correo institucional @upb.edu.co");
        } else if (pass.length() < 5) {
            mostrarError("La contraseña debe tener al menos 5 caracteres.");
        } else if (controlador.existeCorreo(correo)) {
            mostrarError("Ese correo ya está registrado. Inicia sesión.");
        } else {
            modelo.Usuario.Rol rol = "Empleado".equals(cbRol.getSelectedItem())
                    ? modelo.Usuario.Rol.EMPLEADO
                    : modelo.Usuario.Rol.ESTUDIANTE;

            if (controlador.registrarUsuario(nombre, correo, pass, rol)) {
                lblError.setForeground(UIColores.EXITO);
                lblError.setText("Cuenta creada. Ya puedes iniciar sesión.");
                Timer t = new Timer(1400, ev -> dispose());
                t.setRepeats(false);
                t.start();
            } else {
                mostrarError("No se pudo guardar la cuenta. Revisa los permisos de la carpeta.");
            }
        }
    }

    private void mostrarError(String mensaje) {
        lblError.setForeground(UIColores.ERROR);
        lblError.setText(mensaje);
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
            public void insertUpdate(javax.swing.event.DocumentEvent e) { lblError.setText(" "); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { lblError.setText(" "); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { lblError.setText(" "); }
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
        b.setBorder(BorderFactory.createEmptyBorder(11, 10, 11, 10));
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        b.setAlignmentX(Component.LEFT_ALIGNMENT);
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
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
