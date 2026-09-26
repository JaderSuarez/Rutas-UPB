package vista;

import modelo.Camino;
import modelo.GrafoCampus;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * Contenedor del mapa del campus con dos vistas intercambiables:
 *  - "Vista Mapa": imagen isométrica del campus con pines calibrados.
 *  - "Vista Grafo": representación técnica del grafo (edificios, aristas y distancias).
 *
 * Ambas comparten la misma información: la selección de origen/destino y la
 * ruta calculada se reenvían a las dos vistas.
 */
public class PanelMapaDoble extends JPanel {

    private static final String VISTA_MAPA = "mapa";
    private static final String VISTA_GRAFO = "grafo";

    private final PanelMapa panelGrafo;
    private final PanelMapaIsometrico panelIsometrico;
    private final JPanel contenedor;
    private final CardLayout cardLayout;
    private JPanel panelZoom;
    private boolean vistaMapaActiva = true;
    private JButton botonVistaMapa;
    private JButton botonVistaGrafo;
    private JCheckBox chkDistancias;
    private JCheckBox chkConexiones;

    public PanelMapaDoble(GrafoCampus grafo) {
        setLayout(new BorderLayout());
        setBackground(UIColores.FONDO);

        panelGrafo = new PanelMapa(grafo);
        panelIsometrico = new PanelMapaIsometrico();
        panelIsometrico.setGrafo(grafo);

        cardLayout = new CardLayout();
        contenedor = new JPanel(cardLayout);
        contenedor.add(panelIsometrico, VISTA_MAPA);
        contenedor.add(panelGrafo, VISTA_GRAFO);

        // ---------- Barra superior: selector de vista + zoom ----------
        JPanel barra = new JPanel(new BorderLayout());
        barra.setBackground(UIColores.FONDO);
        barra.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));

        JButton btnVistaMapa = new JButton("Vista Mapa");
        JButton btnVistaGrafo = new JButton("Vista Grafo");
        aplicarEstiloSelector(btnVistaMapa, true);
        aplicarEstiloSelector(btnVistaGrafo, false);

        JPanel selector = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        selector.setBackground(UIColores.FONDO);
        selector.add(btnVistaMapa);
        selector.add(btnVistaGrafo);

        JButton btnMas = new JButton("+");
        JButton btnMenos = new JButton("−");
        JButton btnReset = new JButton("Restablecer");
        for (JButton b : new JButton[]{btnMas, btnMenos, btnReset}) {
            BotonUPB.aplicar(b, BotonUPB.Estilo.SECUNDARIO);
            b.setFocusPainted(false);
            b.setFont(new Font(EstiloUPB.FAMILIA, Font.BOLD, 12));
        }
        btnMas.setToolTipText("Acercar");
        btnMenos.setToolTipText("Alejar");
        btnReset.setToolTipText("Restablecer zoom");

        // El zoom se aplica a la vista que esté activa en ese momento
        btnMas.addActionListener(e -> {
            if (vistaMapaActiva) panelIsometrico.acercar();
            else panelGrafo.acercar();
        });
        btnMenos.addActionListener(e -> {
            if (vistaMapaActiva) panelIsometrico.alejar();
            else panelGrafo.alejar();
        });
        btnReset.addActionListener(e -> {
            if (vistaMapaActiva) panelIsometrico.restablecerZoom();
            else panelGrafo.restablecerZoom();
        });

        panelZoom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        panelZoom.setBackground(UIColores.FONDO);
        // Interruptor para mostrar u ocultar la distancia de cada arista (solo en la Vista Grafo)
        chkDistancias = new JCheckBox("Distancias", true);
        chkDistancias.setOpaque(false);
        chkDistancias.setFont(new Font(EstiloUPB.FAMILIA, Font.BOLD, 12));
        chkDistancias.setToolTipText("Mostrar u ocultar la distancia de cada camino");
        EstiloUPB.comoInterruptor(chkDistancias);
        chkDistancias.addActionListener(e -> panelGrafo.setMostrarDistancias(chkDistancias.isSelected()));
        chkDistancias.setVisible(false);
        panelZoom.add(chkDistancias);
        // Interruptor: resaltar las conexiones del edificio bajo el mouse (solo en la Vista Grafo)
        chkConexiones = new JCheckBox("Resaltar conexiones", true);
        chkConexiones.setOpaque(false);
        chkConexiones.setFont(new Font(EstiloUPB.FAMILIA, Font.BOLD, 12));
        chkConexiones.setToolTipText("Al pasar el mouse por un edificio, resaltar sus caminos");
        EstiloUPB.comoInterruptor(chkConexiones);
        chkConexiones.addActionListener(e -> panelGrafo.setResaltarConexiones(chkConexiones.isSelected()));
        chkConexiones.setVisible(false);
        panelZoom.add(Box.createHorizontalStrut(6));
        panelZoom.add(chkConexiones);
        panelZoom.add(Box.createHorizontalStrut(10));
        panelZoom.add(new JLabel("Zoom:"));
        panelZoom.add(btnMenos);
        panelZoom.add(btnMas);
        panelZoom.add(btnReset);

        this.botonVistaMapa = btnVistaMapa;
        this.botonVistaGrafo = btnVistaGrafo;

        btnVistaMapa.addActionListener(e -> mostrarVistaMapa());
        btnVistaGrafo.addActionListener(e -> mostrarVistaGrafo());

        barra.add(selector, BorderLayout.WEST);
        barra.add(panelZoom, BorderLayout.EAST);

        add(barra, BorderLayout.NORTH);
        add(contenedor, BorderLayout.CENTER);
    }

    private void aplicarEstiloSelector(JButton boton, boolean activo) {
        boton.setFont(new Font(EstiloUPB.FAMILIA, Font.BOLD, 12));
        boton.setFocusPainted(false);
        boton.setOpaque(true);
        boton.setContentAreaFilled(true);
        if (activo) {
            boton.setBackground(UIColores.PRIMARIO);
            boton.setForeground(UIColores.TEXTO_CLARO);
            boton.setBorder(BorderFactory.createEmptyBorder(7, 16, 7, 16));
        } else {
            boton.setBackground(UIColores.TARJETA);
            boton.setForeground(UIColores.PRIMARIO);
            boton.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(UIColores.PRIMARIO, 1),
                    BorderFactory.createEmptyBorder(6, 15, 6, 15)));
        }
        BotonUPB.aplicar(boton, activo ? BotonUPB.Estilo.PRINCIPAL : BotonUPB.Estilo.SECUNDARIO);
    }

    public void mostrarVistaMapa() {
        cardLayout.show(contenedor, VISTA_MAPA);
        vistaMapaActiva = true;
        if (chkDistancias != null) chkDistancias.setVisible(false);
        if (chkConexiones != null) chkConexiones.setVisible(false);
        aplicarEstiloSelector(botonVistaMapa, true);
        aplicarEstiloSelector(botonVistaGrafo, false);
    }

    public void mostrarVistaGrafo() {
        cardLayout.show(contenedor, VISTA_GRAFO);
        vistaMapaActiva = false;
        if (chkDistancias != null) chkDistancias.setVisible(true);
        if (chkConexiones != null) chkConexiones.setVisible(true);
        aplicarEstiloSelector(botonVistaMapa, false);
        aplicarEstiloSelector(botonVistaGrafo, true);
    }

    // ---------------- Modo de ubicación (RF-08) ----------------

    /** Muestra la Vista Grafo y pide al administrador que ubique el edificio. */
    public void pedirUbicacionEnGrafo(java.util.function.Consumer<java.awt.Point> alUbicar) {
        mostrarVistaGrafo();
        panelGrafo.pedirUbicacion(alUbicar);
    }

    /** Muestra la Vista Mapa y pide al administrador que ubique el edificio. */
    public void pedirUbicacionEnMapa(java.util.function.Consumer<java.awt.Point> alUbicar) {
        mostrarVistaMapa();
        panelIsometrico.pedirUbicacion(alUbicar);
    }

    public void cancelarUbicacion() {
        panelGrafo.cancelarUbicacion();
        panelIsometrico.cancelarUbicacion();
    }

    /** Registra en ambas vistas la posición de un edificio nuevo. */
    public void registrarPosicionEdificio(String id, int xGrafo, int yGrafo, int xMapa, int yMapa) {
        registrarPosicionEdificio(id, xGrafo, yGrafo, xMapa, yMapa, id);
    }

    public void registrarPosicionEdificio(String id, int xGrafo, int yGrafo,
                                          int xMapa, int yMapa, String nombreVisible) {
        panelGrafo.registrarPosicion(id, xGrafo, yGrafo);
        panelIsometrico.registrarPosicion(id, xMapa, yMapa, nombreVisible);
    }

    /** Quita un edificio de las dos vistas: deja de mostrarse por completo. */
    public void quitarPosicionEdificio(String id) {
        panelGrafo.quitarPosicion(id);
        panelIsometrico.quitarPosicion(id);
    }

    // ---------------- API que usa VentanaPrincipal ----------------
    public void setRutaDestacada(List<String> ruta, List<Camino> tramos) {
        panelGrafo.setRutaDestacada(ruta);
        panelIsometrico.setRutaDestacada(ruta, tramos);
    }

    public void setSeleccion(String origen, String destino) {
        panelGrafo.setSeleccion(origen, destino);
        panelIsometrico.setSeleccion(origen, destino);
    }

    @Override
    public void repaint() {
        super.repaint();
        if (panelGrafo != null) panelGrafo.repaint();
        if (panelIsometrico != null) panelIsometrico.repaint();
    }
}
