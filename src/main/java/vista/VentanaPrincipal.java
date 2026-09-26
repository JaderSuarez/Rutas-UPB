package vista;

import controlador.CampusControlador;
import excepcion.RutaNoEncontradaException;
import modelo.ServicioRutas.ResultadoRuta;

import javax.swing.*;
import java.awt.*;
import java.util.Map;
import java.util.Set;

public class VentanaPrincipal extends JFrame {
    private final CampusControlador controlador;
    private final String usuario;
    private final boolean esAdmin;

    private JComboBox<String> cbOrigen;
    private JComboBox<String> cbDestino;
    private JCheckBox chkEvitarEscaleras;
    private JPanel panelDetalle;
    private JTextField txtBusquedaLugar;
    private PanelMapaDoble panelMapa;
    private PanelResultadoRuta panelResultado;
    private JButton btnCalcular;
    private JLabel lblAvisoRuta;
    private JPopupMenu popupSugerencias;
    private JPanel panelResultadosBusqueda;
    private JButton btnLimpiar;
    // Ruta vigente (la que se está mostrando), para poder recalcularla automáticamente
    private java.util.List<String> rutaVigente;
    private String origenVigente, destinoVigente;
    private boolean accesibleVigente;
    private JButton btnExportar;
    private String ultimoDetalleRuta;

    public VentanaPrincipal(CampusControlador controlador, String usuario, boolean esAdmin) {
        this.controlador = controlador;
        this.usuario = usuario;
        this.esAdmin = esAdmin;

        setTitle("Sistema de Rutas Óptimas UPB - [" + usuario + "]");
        setSize(900, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        initUI();
    }

    private void initUI() {
        setLayout(new BorderLayout());

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(UIColores.PRIMARIO);
        header.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));

        JLabel lblTitle = EstiloUPB.tituloNitido("Detección de Ruta Más Corta - UPB");
        lblTitle.setFont(new Font(EstiloUPB.FAMILIA, Font.BOLD, 16));
        lblTitle.setForeground(UIColores.TEXTO_CLARO);

        JPanel leftHeader = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        leftHeader.setOpaque(false);
        leftHeader.add(new LogoUPB(34));
        leftHeader.add(lblTitle);

        JLabel lblUser = new JLabel(usuario, new EstiloUPB.IconoUsuario(UIColores.TEXTO_CLARO, 24), SwingConstants.LEFT);
        lblUser.setForeground(UIColores.TEXTO_CLARO);
        lblUser.setFont(new Font(EstiloUPB.FAMILIA, Font.BOLD, 13));
        lblUser.setIconTextGap(8);
        lblUser.setToolTipText(esAdmin ? "Sesión iniciada como administrador" : "Sesión actual");

        JButton btnLogout = BotonUPB.crear("Cerrar Sesión", BotonUPB.Estilo.CABECERA);
        btnLogout.addActionListener(e -> {
            this.dispose();
            new VentanaLogin(controlador).setVisible(true);
        });

        JButton btnAyuda = BotonUPB.crear("? Ayuda", BotonUPB.Estilo.ACENTO_CABECERA);
        btnAyuda.setToolTipText("Ver cómo usar el sistema");
        btnAyuda.addActionListener(e -> new VentanaAyuda(this, esAdmin).setVisible(true));

        JPanel rightHeader = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        rightHeader.setOpaque(false);
        rightHeader.add(lblUser);
        if (esAdmin) {
            rightHeader.add(EstiloUPB.etiqueta("Admin", UIColores.ACENTO, UIColores.PRIMARIO_OSCURO));
            rightHeader.add(Box.createHorizontalStrut(6));
        }
        rightHeader.add(btnAyuda);
        rightHeader.add(btnLogout);

        header.add(leftHeader, BorderLayout.WEST);
        header.add(rightHeader, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        JTabbedPane tabbedPane = new JTabbedPane();
        EstiloUPB.estilizarPestanas(tabbedPane);

        tabbedPane.addTab("Buscar Ruta y Lugares", new EstiloUPB.IconoPestana(EstiloUPB.TipoIcono.BUSCAR), crearPanelRutas());

        panelMapa = new PanelMapaDoble(controlador.getGrafo());
        // Restaura la ubicación de los edificios agregados en sesiones anteriores
        for (persistencia.RepositorioEstado.EdificioPersonalizado e : controlador.getEdificiosPersonalizados()) {
            panelMapa.registrarPosicionEdificio(e.id, e.xGrafo, e.yGrafo, e.xMapa, e.yMapa, e.nombre);
        }
        tabbedPane.addTab("Mapa del Campus", new EstiloUPB.IconoPestana(EstiloUPB.TipoIcono.MAPA), panelMapa);

        if (esAdmin) {
            tabbedPane.addTab("Administración", new EstiloUPB.IconoPestana(EstiloUPB.TipoIcono.ADMIN), new PanelAdministracion(
                    controlador, usuario,
                    () -> {
                        // Al cambiar el grafo: refrescar las listas de puntos
                        // y volver a dibujar el mapa, sin reiniciar la sesión.
                        refrescarPuntosDisponibles();
                        panelMapa.repaint();
                        // Si el cambio afecta la ruta que se está mostrando, recalcularla
                        verificarRutaVigente();
                    },
                    panelMapa, tabbedPane));
        }

        add(tabbedPane, BorderLayout.CENTER);
    }

    private JPanel crearPanelRutas() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        panel.setBackground(UIColores.FONDO);

        // ---------- Búsqueda por lugar con autocompletado ----------
        JPanel searchPanel = new JPanel(new BorderLayout(0, 4));
        searchPanel.setBorder(EstiloUPB.bordeSeccion("Búsqueda Directa por Lugar / Dependencia"));
        searchPanel.setBackground(UIColores.FONDO);
        JPanel filaBusqueda = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filaBusqueda.setOpaque(false);
        // Resultados de la búsqueda como tarjetas (se muestran solo al buscar)
        panelResultadosBusqueda = new JPanel();
        panelResultadosBusqueda.setOpaque(false);
        panelResultadosBusqueda.setLayout(new BoxLayout(panelResultadosBusqueda, BoxLayout.Y_AXIS));
        panelResultadosBusqueda.setVisible(false);
        searchPanel.add(filaBusqueda, BorderLayout.NORTH);
        searchPanel.add(panelResultadosBusqueda, BorderLayout.CENTER);
        txtBusquedaLugar = new JTextField(28);
        EstiloUPB.redondearCampo(txtBusquedaLugar);
        JButton btnBuscarLugar = BotonUPB.crear("Buscar Lugar", BotonUPB.Estilo.SECUNDARIO);

        filaBusqueda.add(new JLabel("Lugar:"));
        filaBusqueda.add(EstiloUPB.conTextoGuia(txtBusquedaLugar, "Ej.: biblioteca, auditorio, cafetería"));
        filaBusqueda.add(btnBuscarLugar);

        btnBuscarLugar.addActionListener(e -> ejecutarBusquedaLugar());
        txtBusquedaLugar.addActionListener(e -> ejecutarBusquedaLugar()); // buscar también con Enter

        popupSugerencias = new JPopupMenu();
        popupSugerencias.setFocusable(false);
        popupSugerencias.setBackground(Color.WHITE);
        popupSugerencias.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xCB, 0xD5, 0xE1)),
                BorderFactory.createEmptyBorder(4, 0, 4, 0)));
        txtBusquedaLugar.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { actualizarSugerencias(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { actualizarSugerencias(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { actualizarSugerencias(); }
        });

        // ---------- Selección de origen y destino ----------
        JPanel routePanel = new JPanel(new GridBagLayout());
        routePanel.setBorder(EstiloUPB.bordeSeccion("Seleccionar Puntos de Ruta"));
        routePanel.setBackground(UIColores.FONDO);
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(4, 8, 4, 8);
        gc.fill = GridBagConstraints.HORIZONTAL;

        cbOrigen = new JComboBox<>();
        cbDestino = new JComboBox<>();
        chkEvitarEscaleras = new JCheckBox("Evitar escaleras (Ruta accesible)");
        chkEvitarEscaleras.setBackground(UIColores.FONDO);
        EstiloUPB.comoInterruptor(chkEvitarEscaleras);
        // Las listas guardan el identificador del edificio, pero muestran su nombre completo
        java.util.function.Function<Object, String> nombreCompleto = id -> {
            modelo.Edificio e = controlador.getGrafo().getEdificios().get(String.valueOf(id));
            return e != null ? e.getNombre() : String.valueOf(id);
        };
        EstiloUPB.estilizarCombo(cbOrigen, nombreCompleto);
        EstiloUPB.estilizarCombo(cbDestino, nombreCompleto);

        JButton btnInvertir = BotonUPB.crear("Invertir", BotonUPB.Estilo.SECUNDARIO);
        btnInvertir.setIcon(new EstiloUPB.IconoInvertir(UIColores.PRIMARIO));
        btnInvertir.setIconTextGap(8);
        btnInvertir.setToolTipText("Intercambiar origen y destino");
        btnInvertir.setFocusPainted(false);
        btnInvertir.addActionListener(e -> invertirOrigenDestino());

        btnCalcular = new JButton("Calcular Ruta Más Corta");
        btnCalcular.setFont(new Font(EstiloUPB.FAMILIA, Font.BOLD, 13));
        btnCalcular.setBackground(UIColores.PRIMARIO);
        btnCalcular.setForeground(UIColores.TEXTO_CLARO);
        btnCalcular.setFocusPainted(false);
        BotonUPB.aplicar(btnCalcular, BotonUPB.Estilo.PRINCIPAL);
        btnCalcular.setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));

        // Aviso suave (franja ámbar redondeada con ícono); sin texto no se dibuja nada
        lblAvisoRuta = new JLabel(" ") {
            @Override protected void paintComponent(Graphics g) {
                if (!getText().trim().isEmpty()) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(new Color(0xFE, 0xF3, 0xC7));
                    g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
                    g2.setColor(new Color(0xF5, 0xD9, 0x8B));
                    g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
                    g2.dispose();
                }
                super.paintComponent(g);
            }
        };
        lblAvisoRuta.setFont(new Font(EstiloUPB.FAMILIA, Font.BOLD, 12));
        lblAvisoRuta.setForeground(new Color(0x92, 0x40, 0x0E));
        lblAvisoRuta.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        lblAvisoRuta.setIconTextGap(8);
        lblAvisoRuta.setVisible(false);   // solo ocupa espacio cuando hay un aviso

        cargarCombos();

        gc.gridx = 0; gc.gridy = 0; gc.weightx = 0;
        routePanel.add(new JLabel("Punto Origen:"), gc);
        gc.gridx = 1; gc.weightx = 1;
        routePanel.add(cbOrigen, gc);
        gc.gridx = 2; gc.weightx = 0; gc.gridheight = 2;
        routePanel.add(btnInvertir, gc);
        gc.gridheight = 1;

        gc.gridx = 0; gc.gridy = 1; gc.weightx = 0;
        routePanel.add(new JLabel("Punto Destino:"), gc);
        gc.gridx = 1; gc.weightx = 1;
        routePanel.add(cbDestino, gc);

        gc.gridx = 0; gc.gridy = 2; gc.gridwidth = 2; gc.weightx = 1;
        routePanel.add(chkEvitarEscaleras, gc);
        gc.gridx = 2; gc.gridwidth = 1; gc.weightx = 0;
        routePanel.add(btnCalcular, gc);

        btnExportar = BotonUPB.crear("Exportar ruta", BotonUPB.Estilo.SECUNDARIO);
        btnExportar.setToolTipText("Guardar las instrucciones de la ruta en un archivo de texto");
        btnExportar.setFocusPainted(false);
        btnExportar.setEnabled(false);
        btnExportar.addActionListener(e -> exportarRuta());

        JPanel estoyEn = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        estoyEn.setOpaque(false);
        JLabel lblEstoy = new JLabel("Estoy en:");
        lblEstoy.setForeground(UIColores.TEXTO_MUTED);
        estoyEn.add(lblEstoy);
        for (String[] acceso : new String[][]{{"Porteria 1", "Portería 1"}, {"Porteria 2", "Portería 2"}}) {
            if (controlador.getGrafo().getEdificios().containsKey(acceso[0])) {
                JButton chip = BotonUPB.crear(acceso[1], BotonUPB.Estilo.ACENTO);
                chip.setFont(new Font(EstiloUPB.FAMILIA, Font.BOLD, 11));
                chip.setBorder(BorderFactory.createEmptyBorder(3, 12, 3, 12));
                chip.setToolTipText("Usar " + acceso[1] + " como punto de origen");
                chip.addActionListener(e -> cbOrigen.setSelectedItem(acceso[0]));
                estoyEn.add(chip);
            }
        }
        btnLimpiar = BotonUPB.crear("Limpiar ruta", BotonUPB.Estilo.SECUNDARIO);
        btnLimpiar.setToolTipText("Quitar la ruta trazada y empezar de nuevo");
        btnLimpiar.setEnabled(false);
        btnLimpiar.addActionListener(e -> limpiarRuta());
        JPanel filaEstoy = new JPanel(new BorderLayout());
        filaEstoy.setOpaque(false);
        filaEstoy.add(estoyEn, BorderLayout.WEST);
        filaEstoy.add(btnLimpiar, BorderLayout.EAST);
        gc.gridx = 0; gc.gridy = 3; gc.gridwidth = 2; gc.weightx = 1;
        routePanel.add(filaEstoy, gc);

        gc.gridx = 2; gc.gridy = 3; gc.gridwidth = 1; gc.weightx = 0;
        routePanel.add(btnExportar, gc);

        gc.gridx = 0; gc.gridy = 4; gc.gridwidth = 3;
        routePanel.add(lblAvisoRuta, gc);

        JPanel topContainer = new JPanel(new BorderLayout());
        topContainer.setBackground(UIColores.FONDO);
        topContainer.add(searchPanel, BorderLayout.NORTH);
        topContainer.add(routePanel, BorderLayout.CENTER);

        // ---------- Resultado: tarjeta visual + detalle en texto ----------
        panelResultado = new PanelResultadoRuta();

        // Respaldo: si una ruta con muchos edificios necesita más alto del
        // disponible, se puede desplazar en vez de quedar cortada.
        JScrollPane scrollResultado = new JScrollPane(panelResultado);
        scrollResultado.setBorder(EstiloUPB.bordeSeccion("Resumen de la Ruta"));
        scrollResultado.setBackground(UIColores.FONDO);
        scrollResultado.getVerticalScrollBar().setUnitIncrement(16);
        scrollResultado.setPreferredSize(new Dimension(600, 260));
        scrollResultado.getViewport().setBackground(UIColores.FONDO); // sin franja gris bajo el resumen

        // Detalle técnico: itinerario paso a paso (se construye en mostrarItinerario)
        panelDetalle = new JPanel();
        panelDetalle.setLayout(new BoxLayout(panelDetalle, BoxLayout.Y_AXIS));
        panelDetalle.setBackground(Color.WHITE);
        panelDetalle.setBorder(BorderFactory.createEmptyBorder(6, 14, 8, 14));
        JScrollPane scroll = new JScrollPane(panelDetalle);
        scroll.getVerticalScrollBar().setUnitIncrement(14);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(BorderFactory.createLineBorder(UIColores.BORDE));
        scroll.setPreferredSize(new Dimension(600, 170));
        scroll.setVisible(false); // oculto por defecto: el resumen es la vista principal

        JPanel resultadoContainer = new JPanel(new BorderLayout(0, 10));
        resultadoContainer.setBackground(UIColores.FONDO);

        // Botón para mostrar u ocultar el detalle técnico. Cuando está oculto,
        // el "Resumen de la Ruta" ocupa todo el espacio disponible.
        JButton btnDetalle = BotonUPB.crear("\u25BA Mostrar detalle técnico de la ruta", BotonUPB.Estilo.SECUNDARIO);
        btnDetalle.setHorizontalAlignment(SwingConstants.LEFT);
        btnDetalle.setFocusPainted(false);
        btnDetalle.setToolTipText("Mostrar u ocultar el detalle técnico de la ruta calculada");
        btnDetalle.addActionListener(e -> {
            boolean mostrar = !scroll.isVisible();
            scroll.setVisible(mostrar);
            btnDetalle.setText(mostrar ? "\u25BC Ocultar detalle técnico de la ruta"
                                       : "\u25BA Mostrar detalle técnico de la ruta");
            resultadoContainer.revalidate();
            resultadoContainer.repaint();
        });

        JPanel detallePanel = new JPanel(new BorderLayout(0, 4));
        detallePanel.setOpaque(false);
        detallePanel.add(btnDetalle, BorderLayout.NORTH);
        detallePanel.add(scroll, BorderLayout.CENTER);

        resultadoContainer.add(scrollResultado, BorderLayout.CENTER);
        resultadoContainer.add(detallePanel, BorderLayout.SOUTH);

        panel.add(topContainer, BorderLayout.NORTH);
        panel.add(resultadoContainer, BorderLayout.CENTER);

        btnCalcular.addActionListener(e -> calcularRuta());

        // Enter calcula la ruta desde cualquier parte de la ventana
        SwingUtilities.invokeLater(() -> {
            if (getRootPane() != null) {
                getRootPane().setDefaultButton(btnCalcular);
            }
        });

        // Validación en vivo: origen igual a destino
        cbOrigen.addActionListener(e -> validarSeleccion());
        cbDestino.addActionListener(e -> validarSeleccion());
        validarSeleccion();

        return panel;
    }

    /** Intercambia los valores seleccionados de origen y destino. */
    private void invertirOrigenDestino() {
        Object origen = cbOrigen.getSelectedItem();
        Object destino = cbDestino.getSelectedItem();
        cbOrigen.setSelectedItem(destino);
        cbDestino.setSelectedItem(origen);
    }

    /** Deshabilita el cálculo y avisa si el origen y el destino son el mismo punto. */
    private void validarSeleccion() {
        Object origen = cbOrigen.getSelectedItem();
        Object destino = cbDestino.getSelectedItem();

        boolean iguales = origen != null && origen.equals(destino);
        btnCalcular.setEnabled(!iguales);

        if (iguales) {
            lblAvisoRuta.setText("El origen y el destino son el mismo punto. Elige un destino distinto para calcular la ruta.");
            lblAvisoRuta.setIcon(new EstiloUPB.IconoAlerta());
            mostrarAviso(true);
        } else {
            lblAvisoRuta.setText(" ");
            lblAvisoRuta.setIcon(null);
            mostrarAviso(false);
        }

        // Resaltar en el mapa los puntos elegidos, aun antes de calcular
        if (panelMapa != null) {
            panelMapa.setSeleccion(origen == null ? null : origen.toString(),
                                   destino == null ? null : destino.toString());
        }
    }

    /** Muestra sugerencias de lugares mientras el usuario escribe. */
    private void actualizarSugerencias() {
        String termino = txtBusquedaLugar.getText().trim();
        popupSugerencias.setVisible(false);
        popupSugerencias.removeAll();

        if (termino.length() < 2) return;

        Map<String, Set<String>> coincidencias = controlador.solicitarBusquedaLugar(termino);
        if (coincidencias.isEmpty()) return;

        int mostrados = 0;
        externo:
        for (Map.Entry<String, Set<String>> entrada : coincidencias.entrySet()) {
            for (String edificio : entrada.getValue()) {
                if (mostrados >= 8) break externo;
                String lugar = entrada.getKey();
                JMenuItem item = new JMenuItem("<html><b>" + lugar + "</b> &nbsp;<font color='#64748B'>· "
                        + nombreDe(edificio) + "</font></html>");
                item.setFont(new Font(EstiloUPB.FAMILIA, Font.PLAIN, 12));
                item.setBackground(Color.WHITE);
                item.setOpaque(true);
                item.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
                item.getModel().addChangeListener(ev ->
                        item.setBackground(item.isArmed() ? EstiloUPB.FILA_HOVER : Color.WHITE));
                item.addActionListener(e -> {
                    txtBusquedaLugar.setText(lugar);
                    popupSugerencias.setVisible(false);
                    mostrarResultadosBusqueda(lugar, Map.of(lugar, Set.of(edificio)));
                });
                popupSugerencias.add(item);
                mostrados++;
            }
        }

        if (popupSugerencias.getComponentCount() > 0 && txtBusquedaLugar.isShowing()) {
            popupSugerencias.show(txtBusquedaLugar, 0, txtBusquedaLugar.getHeight());
            txtBusquedaLugar.requestFocusInWindow();
        }
    }

    private void cargarCombos() {
        cbOrigen.removeAllItems();
        cbDestino.removeAllItems();
        for (String id : controlador.getGrafo().getEdificios().keySet()) {
            cbOrigen.addItem(id);
            cbDestino.addItem(id);
        }
    }

    /**
     * Vuelve a llenar las listas de origen y destino conservando, si es
     * posible, lo que el usuario tenía seleccionado. Se usa cuando el
     * administrador agrega un edificio, para que aparezca de inmediato sin
     * necesidad de cerrar la sesión.
     */
    public void refrescarPuntosDisponibles() {
        Object origenPrevio = cbOrigen.getSelectedItem();
        Object destinoPrevio = cbDestino.getSelectedItem();

        cargarCombos();

        if (origenPrevio != null) cbOrigen.setSelectedItem(origenPrevio);
        if (destinoPrevio != null) cbDestino.setSelectedItem(destinoPrevio);

        validarSeleccion();
    }

    private void ejecutarBusquedaLugar() {
        String termino = txtBusquedaLugar.getText().trim();
        popupSugerencias.setVisible(false);
        if (termino.isEmpty()) {
            ocultarResultadosBusqueda();
            return;
        }
        mostrarResultadosBusqueda(termino, controlador.solicitarBusquedaLugar(termino));
    }

    /** Categoría registrada del lugar dentro del edificio (para su etiqueta). */
    private String categoriaDe(String lugar, String idEdificio) {
        modelo.Edificio e = controlador.getGrafo().getEdificios().get(idEdificio);
        if (e != null) {
            for (modelo.Lugar l : e.getLugares()) {
                if (l.getNombre().equals(lugar)) return l.getCategoria();
            }
        }
        return null;
    }

    private void ocultarResultadosBusqueda() {
        panelResultadosBusqueda.removeAll();
        panelResultadosBusqueda.setVisible(false);
        panelResultadosBusqueda.revalidate();
        panelResultadosBusqueda.repaint();
    }

    /**
     * Muestra las coincidencias como tarjetas, cada una con "Salir de aquí" (origen) e
     * "Ir aquí" (destino): el usuario elige; nada se asigna sin que lo decida.
     */
    private void mostrarResultadosBusqueda(String termino, Map<String, Set<String>> res) {
        panelResultadosBusqueda.removeAll();
        JPanel cabecera = new JPanel(new BorderLayout());
        cabecera.setOpaque(false);
        cabecera.setAlignmentX(Component.LEFT_ALIGNMENT);
        cabecera.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
        int total = 0;
        for (Set<String> eds : res.values()) total += eds.size();
        JLabel lblCab = new JLabel(res.isEmpty()
                ? "No se encontró ningún lugar para «" + termino + "». Prueba con otra palabra, por ejemplo «biblioteca»."
                : total + (total == 1 ? " resultado" : " resultados") + " para «" + termino + "»");
        lblCab.setFont(new Font(EstiloUPB.FAMILIA, res.isEmpty() ? Font.ITALIC : Font.BOLD, 12));
        lblCab.setForeground(UIColores.TEXTO_MUTED);
        JButton cerrar = BotonUPB.crear("Cerrar", BotonUPB.Estilo.SECUNDARIO);
        cerrar.setFont(new Font(EstiloUPB.FAMILIA, Font.BOLD, 11));
        cerrar.setBorder(BorderFactory.createEmptyBorder(2, 10, 2, 10));
        cerrar.addActionListener(e -> ocultarResultadosBusqueda());
        cabecera.add(lblCab, BorderLayout.CENTER);
        cabecera.add(cerrar, BorderLayout.EAST);
        panelResultadosBusqueda.add(cabecera);

        if (!res.isEmpty()) {
            JPanel lista = new JPanel();
            lista.setOpaque(false);
            lista.setLayout(new BoxLayout(lista, BoxLayout.Y_AXIS));
            for (Map.Entry<String, Set<String>> ent : res.entrySet()) {
                for (String idEd : ent.getValue()) {
                    lista.add(tarjetaResultado(ent.getKey(), idEd));
                    lista.add(Box.createVerticalStrut(4));
                }
            }
            JScrollPane sc = new JScrollPane(lista);
            sc.setBorder(BorderFactory.createEmptyBorder());
            sc.setOpaque(false);
            sc.getViewport().setOpaque(false);
            sc.getVerticalScrollBar().setUnitIncrement(12);
            sc.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            int filas = Math.min(total, 3);
            sc.setPreferredSize(new Dimension(100, filas * 40));
            sc.setMaximumSize(new Dimension(Integer.MAX_VALUE, filas * 40));
            sc.setAlignmentX(Component.LEFT_ALIGNMENT);
            panelResultadosBusqueda.add(sc);
        }
        panelResultadosBusqueda.setVisible(true);
        panelResultadosBusqueda.revalidate();
        panelResultadosBusqueda.repaint();
    }

    private JPanel tarjetaResultado(String lugar, String idEdificio) {
        JPanel t = new JPanel(new BorderLayout(10, 0)) {
            @Override public Dimension getMaximumSize() {
                return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
            }
        };
        t.setBackground(Color.WHITE);
        t.setBorder(new EstiloUPB.BordeCampo(new Color(0xD5, 0xDC, 0xE6), null, 4, 12, 4, 6));
        JPanel izq = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 3));
        izq.setOpaque(false);
        JLabel nombre = new JLabel("<html><b>" + lugar + "</b> &nbsp;·&nbsp; " + nombreDe(idEdificio) + "</html>");
        nombre.setFont(new Font(EstiloUPB.FAMILIA, Font.PLAIN, 12));
        nombre.setForeground(UIColores.TEXTO_OSCURO);
        izq.add(nombre);
        String cat = categoriaDe(lugar, idEdificio);
        if (cat != null) {
            izq.add(EstiloUPB.etiqueta(cat, EstiloUPB.FILA_HOVER, UIColores.PRIMARIO_OSCURO));
        }
        JPanel der = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 2));
        der.setOpaque(false);
        JButton salir = BotonUPB.crear("Salir de aquí", BotonUPB.Estilo.SECUNDARIO);
        JButton ir = BotonUPB.crear("Ir aquí", BotonUPB.Estilo.PRINCIPAL);
        for (JButton b : new JButton[]{salir, ir}) {
            b.setFont(new Font(EstiloUPB.FAMILIA, Font.BOLD, 11));
            b.setBorder(BorderFactory.createEmptyBorder(4, 12, 4, 12));
        }
        salir.setToolTipText("Usar " + nombreDe(idEdificio) + " como punto de origen");
        ir.setToolTipText("Usar " + nombreDe(idEdificio) + " como destino");
        salir.addActionListener(e -> { cbOrigen.setSelectedItem(idEdificio); ocultarResultadosBusqueda(); });
        ir.addActionListener(e -> { cbDestino.setSelectedItem(idEdificio); ocultarResultadosBusqueda(); });
        der.add(salir);
        der.add(ir);
        t.add(izq, BorderLayout.CENTER);
        t.add(der, BorderLayout.EAST);
        t.setAlignmentX(Component.LEFT_ALIGNMENT);
        return t;
    }

    /** Guarda las instrucciones de la última ruta calculada en un archivo de texto. */
    private void exportarRuta() {
        if (ultimoDetalleRuta == null || ultimoDetalleRuta.isEmpty()) {
            DialogoUPB.mensaje(this,
                    "Primero calcula una ruta para poder exportarla.",
                    "Sin ruta calculada", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        JFileChooser selector = new JFileChooser();
        selector.setDialogTitle("Guardar instrucciones de la ruta");
        String sugerido = "ruta_" + cbOrigen.getSelectedItem() + "_a_" + cbDestino.getSelectedItem() + ".txt";
        selector.setSelectedFile(new java.io.File(sugerido.replace(" ", "_")));

        if (selector.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        java.io.File destino = selector.getSelectedFile();
        if (!destino.getName().toLowerCase().endsWith(".txt")) {
            destino = new java.io.File(destino.getParentFile(), destino.getName() + ".txt");
        }

        try (java.io.BufferedWriter escritor = new java.io.BufferedWriter(
                new java.io.OutputStreamWriter(new java.io.FileOutputStream(destino),
                        java.nio.charset.StandardCharsets.UTF_8))) {

            escritor.write("SISTEMA DE RUTAS ÓPTIMAS UPB - Seccional Bucaramanga");
            escritor.newLine();
            escritor.write("Generado el " + java.time.LocalDateTime.now()
                    .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
            escritor.newLine();
            escritor.newLine();
            escritor.write(ultimoDetalleRuta);

            DialogoUPB.mensaje(this,
                    "Ruta exportada correctamente en:\n" + destino.getAbsolutePath(),
                    "Exportación completada", JOptionPane.INFORMATION_MESSAGE);

        } catch (java.io.IOException ex) {
            DialogoUPB.mensaje(this,
                    "No se pudo guardar el archivo: " + ex.getMessage(),
                    "Error al exportar", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void calcularRuta() {
        String origen = (String) cbOrigen.getSelectedItem();
        String destino = (String) cbDestino.getSelectedItem();
        boolean evitarEscaleras = chkEvitarEscaleras.isSelected();

        try {
            mostrarRutaCalculada(controlador.solicitarRuta(origen, destino, evitarEscaleras), origen, destino, evitarEscaleras);
        } catch (RutaNoEncontradaException e) {
            limpiarRuta();
            mostrarMensajeDetalle(e.getMessage());
            panelResultado.mostrarError(e.getMessage());
        } catch (Exception e) {
            limpiarRuta();
            mostrarMensajeDetalle("Ocurrió un error al calcular la ruta: " + e.getMessage());
            panelResultado.mostrarError("Ocurrió un error al calcular la ruta.");
        }
    }

    /** Muestra una ruta en el resumen, el detalle técnico y el mapa, y la guarda como vigente. */
    private void mostrarRutaCalculada(ResultadoRuta res, String origen, String destino, boolean evitarEscaleras) {
        ultimoDetalleRuta = mostrarItinerario(res, origen, destino, evitarEscaleras);
        btnExportar.setEnabled(true);
        btnLimpiar.setEnabled(true);
        panelResultado.mostrarResultado(res, evitarEscaleras);
        panelMapa.setRutaDestacada(res.getCaminoEdificios(), res.getTramos());
        rutaVigente = new java.util.ArrayList<>(res.getCaminoEdificios());
        origenVigente = origen;
        destinoVigente = destino;
        accesibleVigente = evitarEscaleras;
    }

    /** Quita la ruta trazada: resumen al estado inicial, detalle vacío y mapa sin ruta. */
    private void limpiarRuta() {
        rutaVigente = null;
        ultimoDetalleRuta = null;
        btnExportar.setEnabled(false);
        btnLimpiar.setEnabled(false);
        panelResultado.limpiar();
        panelMapa.setRutaDestacada(null, null);
        panelDetalle.removeAll();
        panelDetalle.revalidate();
        panelDetalle.repaint();
    }

    /**
     * Tras un cambio en el grafo (bloqueo o eliminación de caminos o edificios): si algún
     * tramo de la ruta vigente dejó de estar disponible, se recalcula automáticamente con el
     * mismo origen, destino y modo accesible. Si ya no hay ruta posible, se limpia y se avisa.
     */
    private void verificarRutaVigente() {
        if (rutaVigente == null || rutaVigente.size() < 2) return;
        String afectado = null;
        String accion = "bloqueó";
        for (int i = 0; i < rutaVigente.size() - 1 && afectado == null; i++) {
            String a = rutaVigente.get(i), b = rutaVigente.get(i + 1);
            modelo.Camino tramo = null;
            if (controlador.getGrafo().getEdificios().containsKey(a)) {
                for (modelo.Camino c : controlador.getGrafo().getAdyacentes(a)) {
                    if (c.getDestinoId().equals(b)) { tramo = c; break; }
                }
            }
            if (tramo == null) { afectado = nombreCorto(a) + " – " + nombreCorto(b); accion = "eliminó"; }
            else if (tramo.isBloqueado()) { afectado = nombreCorto(a) + " – " + nombreCorto(b); }
        }
        if (afectado == null) return;   // la ruta vigente no se vio afectada

        try {
            if (!controlador.getGrafo().getEdificios().containsKey(origenVigente)
                    || !controlador.getGrafo().getEdificios().containsKey(destinoVigente)) {
                throw new RutaNoEncontradaException("El origen o el destino ya no existe.");
            }
            mostrarRutaCalculada(controlador.solicitarRuta(origenVigente, destinoVigente, accesibleVigente),
                    origenVigente, destinoVigente, accesibleVigente);
            avisar("La ruta se recalculó automáticamente porque se " + accion + " el tramo " + afectado + ".");
        } catch (Exception e) {
            String o = nombreDe(origenVigente), d = nombreDe(destinoVigente);
            limpiarRuta();
            panelResultado.mostrarError("Ya no hay una ruta disponible de " + o + " a " + d + ".");
            avisar("Ya no hay ruta disponible de " + o + " a " + d + " porque se " + accion + " el tramo " + afectado + ".");
        }
    }

    private String nombreCorto(String id) {
        return id.startsWith("Porteria") ? id.replace("Porteria", "Portería") : id;
    }

    /** Muestra un aviso en la franja ámbar de la sección de selección de ruta. */
    private void avisar(String texto) {
        lblAvisoRuta.setText(texto);
        lblAvisoRuta.setIcon(new EstiloUPB.IconoAlerta());
        mostrarAviso(true);
    }

    private void mostrarAviso(boolean visible) {
        if (lblAvisoRuta.isVisible() == visible) return;
        lblAvisoRuta.setVisible(visible);
        if (lblAvisoRuta.getParent() != null) {
            lblAvisoRuta.getParent().revalidate();
            lblAvisoRuta.getParent().repaint();
        }
    }

    // ==================== Detalle técnico: itinerario paso a paso ====================
    private String nombreDe(String id) {
        modelo.Edificio e = controlador.getGrafo().getEdificios().get(id);
        return e != null ? e.getNombre() : id;
    }

    /** Fila del itinerario: ocupa todo el ancho pero solo la altura que necesita. */
    private JPanel filaDetalle(boolean conLinea) {
        JPanel f = new JPanel(new BorderLayout(12, 0)) {
            @Override public Dimension getMaximumSize() {
                return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
            }
        };
        f.setOpaque(false);
        f.setAlignmentX(Component.LEFT_ALIGNMENT);
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(conLinea ? 1 : 0, 0, 0, 0, UIColores.BORDE),
                BorderFactory.createEmptyBorder(8, 0, 8, 0)));
        return f;
    }

    /**
     * Muestra el detalle técnico como itinerario (un paso por tramo, con distancia,
     * tiempo y si tiene escaleras) y devuelve la misma información en texto plano
     * para "Exportar ruta".
     */
    private String mostrarItinerario(ResultadoRuta res, String origen, String destino, boolean evitarEscaleras) {
        java.util.List<String> puntos = res.getCaminoEdificios();
        java.util.List<modelo.Camino> tramos = res.getTramos();
        String distTotal = String.format("%.0f m", res.getDistanciaTotal());
        panelDetalle.removeAll();

        JPanel cab = filaDetalle(false);
        JLabel titulo = new JLabel("<html><b>" + nombreDe(origen) + "</b> &nbsp;&#8594;&nbsp; <b>" + nombreDe(destino) + "</b></html>");
        titulo.setFont(new Font(EstiloUPB.FAMILIA, Font.PLAIN, 14));
        titulo.setForeground(UIColores.TEXTO_OSCURO);
        JLabel resumen = new JLabel(tramos.size() + (tramos.size() == 1 ? " tramo" : " tramos") + "  ·  " + distTotal
                + "  ·  " + res.getTiempoEstimadoFormateado() + "  ·  modo accesible: " + (evitarEscaleras ? "activado" : "apagado"));
        resumen.setFont(new Font(EstiloUPB.FAMILIA, Font.PLAIN, 12));
        resumen.setForeground(UIColores.TEXTO_MUTED);
        cab.add(titulo, BorderLayout.WEST);
        cab.add(resumen, BorderLayout.EAST);
        panelDetalle.add(cab);

        StringBuilder txt = new StringBuilder();
        txt.append("Origen:  ").append(nombreDe(origen)).append(" (").append(origen).append(")\n");
        txt.append("Destino: ").append(nombreDe(destino)).append(" (").append(destino).append(")\n");
        txt.append("Ruta accesible (sin escaleras): ").append(evitarEscaleras ? "Sí" : "No").append("\n");
        txt.append("------------------------------------------------\n");
        txt.append("Itinerario:\n");

        double acumulado = 0;
        for (int i = 0; i < tramos.size(); i++) {
            modelo.Camino c = tramos.get(i);
            String desde = nombreDe(puntos.get(i)), hasta = nombreDe(puntos.get(i + 1));
            acumulado += c.getDistancia();
            String tiempo = modelo.EstimadorTiempo.formatear(
                    modelo.EstimadorTiempo.calcularMinutos(java.util.Collections.singletonList(c)));
            String dist = String.format("%.0f m", c.getDistancia());
            boolean escaleras = c.isTieneEscaleras();

            JPanel fila = filaDetalle(true);
            fila.add(new JLabel(new EstiloUPB.IconoPaso(i + 1)), BorderLayout.WEST);
            JPanel centro = new JPanel();
            centro.setOpaque(false);
            centro.setLayout(new BoxLayout(centro, BoxLayout.Y_AXIS));
            JLabel paso = new JLabel("<html>" + (i == 0 ? "Sal de " : "Continúa de ") + "<b>" + desde + "</b>"
                    + (i == 0 ? " hacia " : " a ") + "<b>" + hasta + "</b></html>");
            paso.setFont(new Font(EstiloUPB.FAMILIA, Font.PLAIN, 13));
            paso.setForeground(UIColores.TEXTO_OSCURO);
            JPanel datos = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 2));
            datos.setOpaque(false);
            JLabel lblDatos = new JLabel(dist + "  ·  \u2248 " + tiempo + "   ");
            lblDatos.setFont(new Font(EstiloUPB.FAMILIA, Font.PLAIN, 12));
            lblDatos.setForeground(UIColores.TEXTO_MUTED);
            datos.add(lblDatos);
            datos.add(EstiloUPB.etiqueta(escaleras ? "Con escaleras" : "Accesible",
                    escaleras ? new Color(0xE2, 0xE8, 0xF0) : EstiloUPB.FUCSIA_FONDO,
                    escaleras ? EstiloUPB.GRIS_OSCURO : EstiloUPB.FUCSIA_TEXTO));
            paso.setAlignmentX(Component.LEFT_ALIGNMENT);
            datos.setAlignmentX(Component.LEFT_ALIGNMENT);
            centro.add(paso);
            centro.add(datos);
            fila.add(centro, BorderLayout.CENTER);
            JLabel lblAcum = new JLabel(String.format("%.0f m", acumulado));
            lblAcum.setFont(new Font(EstiloUPB.FAMILIA, Font.PLAIN, 12));
            lblAcum.setForeground(UIColores.TEXTO_MUTED);
            lblAcum.setToolTipText("Distancia acumulada");
            fila.add(lblAcum, BorderLayout.EAST);
            panelDetalle.add(fila);

            txt.append(String.format(" %d. %s%s%s%s: %s, %s (\u2248 %s)%n", i + 1, i == 0 ? "Sal de " : "Continúa de ",
                    desde, i == 0 ? " hacia " : " a ", hasta, dist, escaleras ? "con escaleras" : "sin escaleras", tiempo));
        }

        JPanel fin = filaDetalle(true);
        fin.add(new JLabel(new EstiloUPB.IconoLlegada()), BorderLayout.WEST);
        JLabel llegada = new JLabel("<html>Llegada: <b>" + nombreDe(destino) + "</b></html>");
        llegada.setFont(new Font(EstiloUPB.FAMILIA, Font.PLAIN, 13));
        llegada.setForeground(UIColores.TEXTO_OSCURO);
        fin.add(llegada, BorderLayout.CENTER);
        panelDetalle.add(fin);
        panelDetalle.revalidate();
        panelDetalle.repaint();

        txt.append(" Llegada: ").append(nombreDe(destino)).append("\n");
        txt.append("------------------------------------------------\n");
        txt.append("Distancia total: ").append(distTotal).append("\n");
        txt.append("Tiempo estimado: ").append(res.getTiempoEstimadoFormateado()).append("\n");
        return txt.toString();
    }

    /** Muestra un aviso en el detalle técnico cuando no se pudo calcular la ruta. */
    private void mostrarMensajeDetalle(String mensaje) {
        panelDetalle.removeAll();
        JLabel l = new JLabel(mensaje);
        l.setFont(new Font(EstiloUPB.FAMILIA, Font.BOLD, 12));
        l.setForeground(UIColores.ERROR);
        l.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));
        panelDetalle.add(l);
        panelDetalle.revalidate();
        panelDetalle.repaint();
    }
}
