package vista;

import controlador.CampusControlador;
import modelo.Camino;
import modelo.EstimadorTiempo;
import modelo.Lugar;
import modelo.Edificio;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Panel del rol Administrador. Agrupa tres secciones:
 *  - Bloqueos de Caminos: tarjetas de resumen + tabla con todas las aristas.
 *  - Reportes: estado actual del grafo y de los bloqueos.
 *  - Configuración: perfil, velocidades de caminata y cambio de contraseña.
 */
public class PanelAdministracion extends JPanel {

    private final CampusControlador controlador;
    private final String usuario;
    private final Runnable alCambiarGrafo;
    private final PanelMapaDoble panelMapa;
    private final JTabbedPane pestanasPrincipales;

    private JTable tablaCaminos;
    private ModeloTablaCaminos modeloTabla;
    private JPanel panelKpis;
    private JEditorPane areaReportes;

    private static final int COL_ESTADO = 4;
    private static final int COL_ACCION = 5;
    private static final int COL_DETALLE = 6;

    public PanelAdministracion(CampusControlador controlador, String usuario, Runnable alCambiarGrafo,
                               PanelMapaDoble panelMapa, JTabbedPane pestanasPrincipales) {
        this.controlador = controlador;
        this.usuario = usuario;
        this.alCambiarGrafo = alCambiarGrafo;
        this.panelMapa = panelMapa;
        this.pestanasPrincipales = pestanasPrincipales;

        setLayout(new BorderLayout());
        setBackground(UIColores.FONDO);

        JTabbedPane sub = new JTabbedPane();
        EstiloUPB.estilizarPestanas(sub);
        sub.setFont(new Font(EstiloUPB.FAMILIA, Font.BOLD, 12));
        sub.addTab("Bloqueos de Caminos", new EstiloUPB.IconoPestana(EstiloUPB.TipoIcono.BLOQUEOS), crearPanelBloqueos());
        sub.addTab("Edificios y Caminos", new EstiloUPB.IconoPestana(EstiloUPB.TipoIcono.EDIFICIOS), crearPanelEdificios());
        sub.addTab("Reportes", new EstiloUPB.IconoPestana(EstiloUPB.TipoIcono.REPORTES), crearPanelReportes());
        sub.addTab("Configuración", new EstiloUPB.IconoPestana(EstiloUPB.TipoIcono.CONFIGURACION), crearPanelConfiguracion());
        sub.addChangeListener(e -> {
            if (sub.getSelectedIndex() == 2) refrescarReportes();
        });

        add(sub, BorderLayout.CENTER);
    }

    // ==================== 1. BLOQUEOS ====================
    private JPanel crearPanelBloqueos() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBackground(UIColores.FONDO);
        panel.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));

        panelKpis = new JPanel(new GridLayout(1, 4, 12, 0));
        panelKpis.setBackground(UIColores.FONDO);
        refrescarKpis();
        panel.add(panelKpis, BorderLayout.NORTH);

        modeloTabla = new ModeloTablaCaminos(obtenerCaminosUnicos());
        tablaCaminos = new JTable(modeloTabla);
        estilizarTabla(tablaCaminos);

        tablaCaminos.getColumnModel().getColumn(COL_ESTADO).setCellRenderer(new RenderizadorEstado());
        tablaCaminos.getColumnModel().getColumn(COL_ACCION).setCellRenderer(new RenderizadorInterruptor());
        tablaCaminos.getColumnModel().getColumn(COL_DETALLE).setCellRenderer(new RenderizadorDetalle());
        tablaCaminos.getColumnModel().getColumn(3).setCellRenderer(new EstiloUPB.RenderizadorEscaleras());
        EstiloUPB.centrarColumna(tablaCaminos, 2);
        // Ordenar al hacer clic en el encabezado (la distancia se ordena como número)
        javax.swing.table.TableRowSorter<ModeloTablaCaminos> orden = new javax.swing.table.TableRowSorter<>(modeloTabla);
        orden.setComparator(2, EstiloUPB.comparadorNumerico());
        orden.setSortable(COL_ACCION, false);
        orden.setSortable(COL_DETALLE, false);
        tablaCaminos.setRowSorter(orden);

        tablaCaminos.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int fila = tablaCaminos.rowAtPoint(e.getPoint());
                int col = tablaCaminos.columnAtPoint(e.getPoint());
                if (fila < 0) return;
                if (col == COL_ACCION) {
                    alternarBloqueo(fila);
                } else if (col == COL_DETALLE) {
                    mostrarDetalleTramo(fila);
                }
            }
        });

        JScrollPane scroll = EstiloUPB.scrollRedondeado(tablaCaminos);
        scroll.setBorder(EstiloUPB.bordeSeccion("Gestión de bloqueos de caminos"));
        panel.add(scroll, BorderLayout.CENTER);

        JPanel sur = new JPanel(new BorderLayout(0, 6));
        sur.setBackground(UIColores.FONDO);

        JLabel nota = new JLabel("Haz clic en el interruptor de la columna \"Acción\" para bloquear o "
                + "desbloquear un tramo. Un tramo bloqueado se excluye del cálculo de rutas.");
        nota.setFont(new Font(EstiloUPB.FAMILIA, Font.ITALIC, 11));
        nota.setForeground(UIColores.TEXTO_MUTED);
        sur.add(nota, BorderLayout.NORTH);

        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        acciones.setBackground(UIColores.FONDO);
        JButton btnEliminar = botonPeligro("Eliminar camino seleccionado");
        btnEliminar.setToolTipText("Quita el camino del grafo de forma permanente");
        btnEliminar.addActionListener(e -> eliminarCaminoSeleccionado());
        acciones.add(btnEliminar);

        JLabel notaElim = new JLabel("Eliminar no es lo mismo que bloquear: el camino desaparece del grafo.");
        notaElim.setFont(new Font(EstiloUPB.FAMILIA, Font.ITALIC, 11));
        notaElim.setForeground(UIColores.TEXTO_MUTED);
        acciones.add(notaElim);

        sur.add(acciones, BorderLayout.SOUTH);

        // Tabla de caminos eliminados, con opción de restaurar el que se elija
        tablaEliminados = EstiloUPB.tablaConMensajeVacio(construirModeloEliminados(),
                "No hay caminos eliminados. Los que elimines aparecerán aquí para poder restaurarlos.");
        estilizarTabla(tablaEliminados);
        tablaEliminados.setRowHeight(30);
        configurarTablaEliminados();

        JScrollPane scrollElim = EstiloUPB.scrollRedondeado(tablaEliminados);
        scrollElim.setPreferredSize(new Dimension(200, 120));
        scrollElim.setBorder(EstiloUPB.bordeSeccion("Caminos eliminados"));

        JButton btnRestaurar = botonSecundario("Restaurar camino seleccionado");
        btnRestaurar.addActionListener(e -> restaurarCaminoSeleccionado());

        JPanel panelElim = new JPanel(new BorderLayout(0, 6));
        panelElim.setBackground(UIColores.FONDO);
        panelElim.add(scrollElim, BorderLayout.CENTER);
        JPanel filaRest = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        filaRest.setBackground(UIColores.FONDO);
        filaRest.add(btnRestaurar);
        panelElim.add(filaRest, BorderLayout.SOUTH);

        JPanel contenedorSur = new JPanel(new BorderLayout(0, 10));
        contenedorSur.setBackground(UIColores.FONDO);
        contenedorSur.add(sur, BorderLayout.NORTH);
        contenedorSur.add(panelElim, BorderLayout.CENTER);

        panel.add(contenedorSur, BorderLayout.SOUTH);

        return panel;
    }

    private void alternarBloqueo(int fila) {
        Camino c = modeloTabla.getCaminoEn(tablaCaminos.convertRowIndexToModel(fila));
        boolean bloquearAhora = !c.isBloqueado();

        // Antes de bloquear, advertir si el tramo es la única vía de acceso
        // a uno o más edificios (quedarían incomunicados).
        if (bloquearAhora) {
            List<String> aislados =
                    controlador.consultarAislamientoPorBloqueo(c.getOrigenId(), c.getDestinoId());
            if (!aislados.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                sb.append("Si bloqueas el tramo ").append(c.getOrigenId())
                  .append(" - ").append(c.getDestinoId())
                  .append(", estos puntos del campus quedarían sin ninguna vía de acceso:\n\n");
                for (String id : aislados) {
                    sb.append("   • ").append(id).append("\n");
                }
                sb.append("\nNo se podrá calcular ninguna ruta hacia ellos.\n¿Deseas bloquearlo de todas formas?");

                int respuesta = DialogoUPB.confirmar(this, sb.toString(),
                        "Advertencia: el bloqueo deja puntos incomunicados",
                        JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (respuesta != JOptionPane.YES_OPTION) {
                    return;
                }
            }
        }

        try {
            controlador.solicitarBloqueo(c.getOrigenId(), c.getDestinoId(), bloquearAhora);
        } catch (IllegalArgumentException ex) {
            DialogoUPB.mensaje(this, ex.getMessage(),
                    "No se pudo cambiar el estado del tramo", JOptionPane.ERROR_MESSAGE);
            return;
        }

        modeloTabla.setCaminos(obtenerCaminosUnicos());
        refrescarKpis();
        if (alCambiarGrafo != null) alCambiarGrafo.run();
    }

    /** Elimina del grafo el camino seleccionado en la tabla, previa confirmación. */
    private void eliminarCaminoSeleccionado() {
        int fila = tablaCaminos.getSelectedRow();
        if (fila < 0) {
            DialogoUPB.mensaje(this,
                    "Selecciona primero una fila de la tabla.",
                    "Ningún camino seleccionado", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        Camino c = modeloTabla.getCaminoEn(tablaCaminos.convertRowIndexToModel(fila));

        // Advertir si al quitarlo algún punto queda incomunicado
        List<String> aislados =
                controlador.consultarAislamientoPorBloqueo(c.getOrigenId(), c.getDestinoId());

        StringBuilder sb = new StringBuilder();
        sb.append("¿Eliminar definitivamente el camino ")
          .append(c.getOrigenId()).append(" - ").append(c.getDestinoId())
          .append(" (").append(String.format("%.0f m", c.getDistancia())).append(")?\n\n");
        sb.append("El camino desaparecerá del grafo y no se podrá usar en ninguna ruta.\n");
        if (!aislados.isEmpty()) {
            sb.append("\nAdemás, estos puntos quedarían sin ninguna vía de acceso:\n");
            for (String id : aislados) sb.append("   • ").append(id).append("\n");
        }

        int respuesta = DialogoUPB.confirmar(this, sb.toString(),
                "Confirmar eliminación", JOptionPane.YES_NO_OPTION,
                aislados.isEmpty() ? JOptionPane.QUESTION_MESSAGE : JOptionPane.WARNING_MESSAGE);
        if (respuesta != JOptionPane.YES_OPTION) return;

        try {
            controlador.eliminarCamino(c.getOrigenId(), c.getDestinoId());
            refrescarTablasCaminos();
            if (alCambiarGrafo != null) alCambiarGrafo.run();
        } catch (IllegalArgumentException ex) {
            DialogoUPB.mensaje(this, ex.getMessage(),
                    "No se pudo eliminar", JOptionPane.ERROR_MESSAGE);
        }
    }

    private DefaultTableModel construirModeloEliminados() {
        String[] cols = {"Origen", "Destino", "Distancia", "Escaleras"};
        List<Object[]> filas = new ArrayList<>();
        for (String[] d : controlador.getCaminosEliminados()) {
            filas.add(new Object[]{d[0], d[1],
                    String.format("%.0f m", Double.parseDouble(d[2])),
                    Boolean.parseBoolean(d[3]) ? "Sí" : "No"});
        }
        return new DefaultTableModel(filas.toArray(new Object[0][]), cols) {
            @Override public boolean isCellEditable(int f, int c) { return false; }
        };
    }

    /** Vuelve a crear el camino eliminado que el administrador seleccione. */
    private void restaurarCaminoSeleccionado() {
        int fila = tablaEliminados.getSelectedRow();
        if (fila < 0) {
            DialogoUPB.mensaje(this,
                    "Selecciona el camino que quieres restaurar.",
                    "Ningún camino seleccionado", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        List<String[]> eliminados = controlador.getCaminosEliminados();
        if (fila >= eliminados.size()) return;
        String[] d = eliminados.get(fila);

        try {
            controlador.restaurarCamino(d[0], d[1],
                    Double.parseDouble(d[2]), Boolean.parseBoolean(d[3]));

            refrescarTablasCaminos();
            if (alCambiarGrafo != null) alCambiarGrafo.run();

            DialogoUPB.mensaje(this,
                    "El camino " + d[0] + " - " + d[1] + " volvió a estar disponible.",
                    "Camino restaurado", JOptionPane.INFORMATION_MESSAGE);
        } catch (IllegalArgumentException ex) {
            DialogoUPB.mensaje(this, ex.getMessage(),
                    "No se pudo restaurar", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** Actualiza las dos tablas de caminos y los indicadores. */
    private void refrescarTablasCaminos() {
        modeloTabla.setCaminos(obtenerCaminosUnicos());
        refrescarKpis();
        if (tablaEliminados != null) {
            tablaEliminados.setModel(construirModeloEliminados());
            estilizarTabla(tablaEliminados);
            tablaEliminados.setRowHeight(30);
            configurarTablaEliminados();
        }
        if (tablaEdificios != null) {
            tablaEdificios.setModel(construirModeloEdificios());
            estilizarTabla(tablaEdificios);
            configurarTablaEdificios();
        }
    }

    private void mostrarDetalleTramo(int fila) {
        Camino c = modeloTabla.getCaminoEn(tablaCaminos.convertRowIndexToModel(fila));
        double minutos = c.getDistancia() / (c.isTieneEscaleras()
                ? EstimadorTiempo.getVelocidadEscaleras()
                : EstimadorTiempo.getVelocidadPlano());

        StringBuilder sb = new StringBuilder();
        sb.append("Tramo: ").append(c.getOrigenId()).append("  <->  ").append(c.getDestinoId()).append("\n");
        sb.append("Distancia: ").append(String.format("%.0f m", c.getDistancia())).append("\n");
        sb.append("Tiene escaleras: ").append(c.isTieneEscaleras() ? "Sí" : "No").append("\n");
        sb.append("Estado: ").append(c.isBloqueado() ? "Bloqueado" : "Disponible").append("\n");
        sb.append("Tiempo estimado del tramo: ").append(EstimadorTiempo.formatear(minutos)).append("\n\n");
        sb.append("Lugares en ").append(c.getOrigenId()).append(":\n").append(listarLugares(c.getOrigenId()));
        sb.append("\nLugares en ").append(c.getDestinoId()).append(":\n").append(listarLugares(c.getDestinoId()));

        JTextArea area = new JTextArea(sb.toString());
        area.setEditable(false);
        area.setFont(new Font(EstiloUPB.FAMILIA, Font.PLAIN, 12));
        area.setBackground(UIColores.FONDO);
        JScrollPane sc = new JScrollPane(area);
        sc.setPreferredSize(new Dimension(420, 300));

        DialogoUPB.mensaje(this, sc, "Detalle del tramo", JOptionPane.INFORMATION_MESSAGE);
    }

    private String listarLugares(String idEdificio) {
        Edificio edificio = controlador.getGrafo().getEdificios().get(idEdificio);
        if (edificio == null || edificio.getLugares().isEmpty()) return "   (sin lugares registrados)\n";
        StringBuilder sb = new StringBuilder();
        for (Lugar l : edificio.getLugares()) {
            sb.append("   • ").append(l.getNombre()).append("\n");
        }
        return sb.toString();
    }

    // ---------- Tarjetas de resumen ----------
    private void refrescarKpis() {
        panelKpis.removeAll();
        List<Camino> caminos = obtenerCaminosUnicos();
        int total = caminos.size();
        int bloqueados = 0, conEscaleras = 0;
        for (Camino c : caminos) {
            if (c.isBloqueado()) bloqueados++;
            if (c.isTieneEscaleras()) conEscaleras++;
        }
        int disponibles = total - bloqueados;

        panelKpis.add(tarjetaKpi("Total de caminos", String.valueOf(total), UIColores.PRIMARIO, null));
        panelKpis.add(tarjetaKpi("Caminos disponibles", String.valueOf(disponibles), UIColores.EXITO,
                porcentaje(disponibles, total)));
        panelKpis.add(tarjetaKpi("Caminos bloqueados", String.valueOf(bloqueados), UIColores.ERROR,
                porcentaje(bloqueados, total)));
        panelKpis.add(tarjetaKpi("Con escaleras", String.valueOf(conEscaleras), UIColores.PRIMARIO_OSCURO,
                porcentaje(conEscaleras, total)));

        panelKpis.revalidate();
        panelKpis.repaint();
    }

    private String porcentaje(int parte, int total) {
        if (total == 0) return null;
        return String.format("(%.0f%%)", parte * 100.0 / total);
    }

    private JPanel tarjetaKpi(String etiqueta, String valor, Color colorValor, String extra) {
        JPanel p = new JPanel();
        p.setBackground(UIColores.TARJETA);
        p.setBorder(new EstiloUPB.BordeCampo(UIColores.BORDE, null, 12, 14, 12, 14, 16));
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));

        JLabel lblEtiqueta = new JLabel(etiqueta);
        lblEtiqueta.setFont(new Font(EstiloUPB.FAMILIA, Font.PLAIN, 11));
        lblEtiqueta.setForeground(UIColores.TEXTO_MUTED);
        lblEtiqueta.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel fila = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        fila.setOpaque(false);
        fila.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel lblValor = new JLabel(valor);
        lblValor.setFont(new Font(EstiloUPB.FAMILIA, Font.BOLD, 24));
        lblValor.setForeground(colorValor);
        fila.add(lblValor);
        if (extra != null) {
            JLabel lblExtra = new JLabel(extra);
            lblExtra.setFont(new Font(EstiloUPB.FAMILIA, Font.PLAIN, 11));
            lblExtra.setForeground(UIColores.TEXTO_MUTED);
            fila.add(lblExtra);
        }

        p.add(lblEtiqueta);
        p.add(fila);
        return p;
    }

    // ---------- Utilidades del grafo ----------
    /** El grafo es no dirigido, así que cada arista aparece dos veces: aquí se deduplica. */
    private List<Camino> obtenerCaminosUnicos() {
        List<Camino> resultado = new ArrayList<>();
        Set<String> vistos = new HashSet<>();
        for (String id : controlador.getGrafo().getEdificios().keySet()) {
            for (Camino c : controlador.getGrafo().getAdyacentes(id)) {
                String a = c.getOrigenId(), b = c.getDestinoId();
                String clave = (a.compareTo(b) <= 0) ? a + "||" + b : b + "||" + a;
                if (vistos.add(clave)) resultado.add(c);
            }
        }
        resultado.sort((x, y) -> {
            int cmp = x.getOrigenId().compareTo(y.getOrigenId());
            return cmp != 0 ? cmp : x.getDestinoId().compareTo(y.getDestinoId());
        });
        return resultado;
    }

    private void estilizarTabla(JTable tabla) {
        tabla.setRowHeight(34);
        tabla.setFont(new Font(EstiloUPB.FAMILIA, Font.PLAIN, 12));
        tabla.setShowVerticalLines(false);
        tabla.setGridColor(UIColores.BORDE);
        tabla.setSelectionBackground(UIColores.ERROR_FONDO);
        tabla.setSelectionForeground(UIColores.TEXTO_OSCURO);

        JTableHeader header = tabla.getTableHeader();
        header.setFont(new Font(EstiloUPB.FAMILIA, Font.BOLD, 12));
        header.setBackground(UIColores.PRIMARIO);
        header.setForeground(UIColores.TEXTO_CLARO);
        header.setPreferredSize(new Dimension(0, 34));
        header.setReorderingAllowed(false);
        ((DefaultTableCellRenderer) header.getDefaultRenderer())
                .setHorizontalAlignment(SwingConstants.LEFT);
        EstiloUPB.estilizarFilas(tabla);
    }

    /** Campo de contraseña con su botón "Mostrar"/"Ocultar" a la derecha (igual que en el login). */
    private JPanel conBotonMostrar(JPasswordField campo) {
        char ocultar = campo.getEchoChar();
        JToggleButton btn = new JToggleButton("Mostrar");
        BotonUPB.aplicar(btn, BotonUPB.Estilo.SECUNDARIO);
        btn.setFont(new Font(EstiloUPB.FAMILIA, Font.BOLD, 11));
        btn.setBorder(BorderFactory.createEmptyBorder(4, 12, 4, 12));
        btn.setToolTipText("Mostrar u ocultar la contraseña");
        btn.addActionListener(e -> {
            campo.setEchoChar(btn.isSelected() ? (char) 0 : ocultar);
            btn.setText(btn.isSelected() ? "Ocultar" : "Mostrar");
        });
        JPanel fila = new JPanel(new BorderLayout(6, 0));
        fila.setOpaque(false);
        fila.add(campo, BorderLayout.CENTER);
        fila.add(btn, BorderLayout.EAST);
        return fila;
    }

    private void configurarTablaEliminados() {
        if (tablaEliminados.getColumnCount() >= 4) {
            EstiloUPB.centrarColumna(tablaEliminados, 2);
            tablaEliminados.getColumnModel().getColumn(3).setCellRenderer(new EstiloUPB.RenderizadorEscaleras());
        }
    }

    /**
     * Tabla de edificios: una sola columna "Edificio" (insignia con el identificador y nombre),
     * números centrados, tipo Original (con candado) o Agregado, y orden por encabezado.
     * La columna "Nombre" se oculta solo en la vista: el modelo la conserva.
     */
    private void configurarTablaEdificios() {
        javax.swing.table.TableColumnModel cm = tablaEdificios.getColumnModel();
        if (cm.getColumnCount() == 5) {
            cm.removeColumn(cm.getColumn(1));
        }
        tablaEdificios.setAutoCreateRowSorter(true);
        tablaEdificios.setRowHeight(36);
        int[] anchos = {175, 88, 62, 104};
        for (int k = 0; k < anchos.length; k++) cm.getColumn(k).setPreferredWidth(anchos[k]);
        cm.getColumn(0).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foco, int f, int c) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(t, v, sel, false, f, c);
                String id = String.valueOf(v);
                Object nombre = t.getModel().getValueAt(t.convertRowIndexToModel(f), 1);
                l.setIcon(new EstiloUPB.IconoInsignia(id));
                l.setText(String.valueOf(nombre));
                l.setIconTextGap(10);
                l.setBackground(EstiloUPB.fondoFila(t, f, sel));
                l.setForeground(UIColores.TEXTO_OSCURO);
                l.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
                return l;
            }
        });
        EstiloUPB.centrarColumna(tablaEdificios, 1);
        EstiloUPB.centrarColumna(tablaEdificios, 2);
        cm.getColumn(3).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foco, int f, int c) {
                boolean original = "Original".equals(String.valueOf(v));
                JPanel celda = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
                celda.setBackground(EstiloUPB.fondoFila(t, f, sel));
                JLabel et = EstiloUPB.etiqueta(original ? "Original" : "Agregado",
                        original ? new Color(0xE2, 0xE8, 0xF0) : UIColores.EXITO_FONDO,
                        original ? EstiloUPB.GRIS_OSCURO : new Color(0x0B, 0x7A, 0x55));
                if (original) {
                    et.setIcon(new EstiloUPB.IconoCandado(EstiloUPB.GRIS_OSCURO));
                    et.setIconTextGap(5);
                }
                celda.add(et);
                celda.setToolTipText(original ? "Edificio original del campus: no se puede eliminar"
                                              : "Agregado desde la aplicación: se puede eliminar");
                return celda;
            }
        });
    }

    // ==================== 2. EDIFICIOS Y CAMINOS (RF-08) ====================

    private JTextField txtIdEdificio;
    private JTextField txtNombreEdificio;
    private JPanel panelConexiones;
    private final List<FilaConexion> filasConexion = new ArrayList<>();
    private JLabel lblUbicacionGrafo;
    private JLabel lblUbicacionMapa;
    private JLabel lblAvisoEdificio;
    private Point ubicacionGrafo;
    private Point ubicacionMapa;
    private JTable tablaEdificios;
    private JTable tablaEliminados;

    private JPanel crearPanelEdificios() {
        JPanel panel = new JPanel(new BorderLayout(14, 0));
        panel.setBackground(UIColores.FONDO);
        panel.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));

        // ---------- Formulario ----------
        JPanel form = new JPanel();
        form.setBackground(UIColores.TARJETA);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBorder(BorderFactory.createCompoundBorder(
                EstiloUPB.bordeSeccion("Registrar nuevo punto en el campus"),
                BorderFactory.createEmptyBorder(10, 12, 12, 12)));
        form.setPreferredSize(new Dimension(360, 0));

        txtIdEdificio = new JTextField();
        txtNombreEdificio = new JTextField();
        EstiloUPB.redondearCampo(txtIdEdificio);
        EstiloUPB.redondearCampo(txtNombreEdificio);
        form.add(filaCampo("Identificador:", EstiloUPB.conTextoGuia(txtIdEdificio, "Ej.: N")));
        form.add(filaCampo("Nombre:", EstiloUPB.conTextoGuia(txtNombreEdificio, "Ej.: Punto Nuevo")));
        form.add(Box.createVerticalStrut(6));

        JLabel tituloConexiones = new JLabel("Conexiones con otros puntos del campus");
        tituloConexiones.setFont(new Font(EstiloUPB.FAMILIA, Font.BOLD, 12));
        tituloConexiones.setForeground(UIColores.PRIMARIO);
        tituloConexiones.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(tituloConexiones);
        form.add(Box.createVerticalStrut(6));

        panelConexiones = new JPanel() {
            // Solo ocupa la altura de sus filas: evita el espacio vacío bajo las conexiones
            @Override public Dimension getMaximumSize() {
                return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
            }
        };
        panelConexiones.setLayout(new BoxLayout(panelConexiones, BoxLayout.Y_AXIS));
        panelConexiones.setOpaque(false);
        panelConexiones.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(panelConexiones);
        agregarFilaConexion();

        JButton btnMasConexion = botonSecundario("+ Agregar otra conexión");
        btnMasConexion.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnMasConexion.addActionListener(e -> {
            agregarFilaConexion();
            form.revalidate();
        });
        form.add(Box.createVerticalStrut(4));
        form.add(btnMasConexion);
        form.add(Box.createVerticalStrut(10));

        // ---------- Ubicación por clic ----------
        JLabel tituloUbic = new JLabel("Ubicación en el mapa");
        tituloUbic.setFont(new Font(EstiloUPB.FAMILIA, Font.BOLD, 12));
        tituloUbic.setForeground(UIColores.PRIMARIO);
        tituloUbic.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(tituloUbic);
        form.add(Box.createVerticalStrut(6));

        lblUbicacionGrafo = new JLabel("Vista Grafo: sin ubicar");
        lblUbicacionGrafo.setFont(new Font(EstiloUPB.FAMILIA, Font.PLAIN, 11));
        lblUbicacionGrafo.setForeground(UIColores.TEXTO_MUTED);
        lblUbicacionGrafo.setAlignmentX(Component.LEFT_ALIGNMENT);

        lblUbicacionMapa = new JLabel("Vista Mapa: sin ubicar");
        lblUbicacionMapa.setFont(new Font(EstiloUPB.FAMILIA, Font.PLAIN, 11));
        lblUbicacionMapa.setForeground(UIColores.TEXTO_MUTED);
        lblUbicacionMapa.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton btnUbicarGrafo = botonSecundario("Ubicar en Vista Grafo");
        btnUbicarGrafo.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnUbicarGrafo.addActionListener(e -> pedirUbicacion(false));

        JButton btnUbicarMapa = botonSecundario("Ubicar en Vista Mapa");
        btnUbicarMapa.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnUbicarMapa.addActionListener(e -> pedirUbicacion(true));

        form.add(btnUbicarGrafo);
        form.add(lblUbicacionGrafo);
        form.add(Box.createVerticalStrut(6));
        form.add(btnUbicarMapa);
        form.add(lblUbicacionMapa);
        form.add(Box.createVerticalStrut(12));

        lblAvisoEdificio = new JLabel(" ");
        lblAvisoEdificio.setFont(new Font(EstiloUPB.FAMILIA, Font.BOLD, 12));
        lblAvisoEdificio.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton btnAgregar = botonPrimario("Agregar punto al grafo");
        btnAgregar.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnAgregar.addActionListener(e -> agregarEdificio());

        // Todos los botones del formulario con el mismo ancho
        for (JButton b : new JButton[]{btnMasConexion, btnUbicarGrafo, btnUbicarMapa, btnAgregar}) {
            b.setMaximumSize(new Dimension(Integer.MAX_VALUE, b.getPreferredSize().height));
            b.setHorizontalAlignment(SwingConstants.CENTER);
        }
        form.add(btnAgregar);
        form.add(Box.createVerticalStrut(6));
        form.add(lblAvisoEdificio);
        form.add(Box.createVerticalGlue());

        panel.add(form, BorderLayout.WEST);

        // ---------- Tabla de edificios ----------
        JPanel derecha = new JPanel(new BorderLayout());
        derecha.setBackground(UIColores.FONDO);
        JLabel titulo = new JLabel("Edificios registrados en el grafo");
        titulo.setFont(new Font(EstiloUPB.FAMILIA, Font.BOLD, 14));
        titulo.setForeground(UIColores.PRIMARIO_OSCURO);
        derecha.add(titulo, BorderLayout.NORTH);

        tablaEdificios = new JTable(construirModeloEdificios());
        estilizarTabla(tablaEdificios);
        configurarTablaEdificios();
        JScrollPane scroll = EstiloUPB.scrollRedondeado(tablaEdificios);
        scroll.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        derecha.add(scroll, BorderLayout.CENTER);

        JPanel accionesEdif = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        accionesEdif.setBackground(UIColores.FONDO);
        JButton btnEliminarEdif = botonPeligro("Eliminar edificio seleccionado");
        btnEliminarEdif.setToolTipText("Solo se pueden eliminar los edificios agregados desde la aplicación");
        btnEliminarEdif.addActionListener(e -> eliminarEdificioSeleccionado());
        accionesEdif.add(btnEliminarEdif);

        JLabel notaEdif = new JLabel("Los edificios originales del campus no se pueden eliminar.");
        notaEdif.setFont(new Font(EstiloUPB.FAMILIA, Font.ITALIC, 11));
        notaEdif.setForeground(UIColores.TEXTO_MUTED);
        accionesEdif.add(notaEdif);

        derecha.add(accionesEdif, BorderLayout.SOUTH);

        panel.add(derecha, BorderLayout.CENTER);
        return panel;
    }

    /** Lleva al usuario a la vista indicada y espera el clic de ubicación. */
    private void pedirUbicacion(boolean enMapa) {
        if (panelMapa == null || pestanasPrincipales == null) return;

        // Cambia a la pestaña del mapa para que el administrador pueda hacer clic
        for (int i = 0; i < pestanasPrincipales.getTabCount(); i++) {
            if ("Mapa del Campus".equals(pestanasPrincipales.getTitleAt(i))) {
                pestanasPrincipales.setSelectedIndex(i);
                break;
            }
        }

        if (enMapa) {
            panelMapa.pedirUbicacionEnMapa(punto -> {
                ubicacionMapa = punto;
                lblUbicacionMapa.setText("Vista Mapa: (" + punto.x + ", " + punto.y + ")");
                lblUbicacionMapa.setForeground(UIColores.EXITO);
                volverAAdministracion();
            });
        } else {
            panelMapa.pedirUbicacionEnGrafo(punto -> {
                ubicacionGrafo = punto;
                lblUbicacionGrafo.setText("Vista Grafo: (" + punto.x + ", " + punto.y + ")");
                lblUbicacionGrafo.setForeground(UIColores.EXITO);
                volverAAdministracion();
            });
        }
    }

    private void volverAAdministracion() {
        if (pestanasPrincipales == null) return;
        for (int i = 0; i < pestanasPrincipales.getTabCount(); i++) {
            if ("Administración".equals(pestanasPrincipales.getTitleAt(i))) {
                pestanasPrincipales.setSelectedIndex(i);
                break;
            }
        }
    }

    private void agregarEdificio() {
        String id = txtIdEdificio.getText().trim();
        String nombre = txtNombreEdificio.getText().trim();

        if (id.isEmpty()) {
            avisoEdificio("Escribe el identificador del punto.", false);
            return;
        }
        if (ubicacionGrafo == null || ubicacionMapa == null) {
            avisoEdificio("Ubica el punto en las dos vistas antes de agregarlo.", false);
            return;
        }

        // Reunir y validar todas las conexiones ingresadas
        List<CampusControlador.ConexionNueva> conexiones = new ArrayList<>();
        for (FilaConexion fila : filasConexion) {
            String destino = (String) fila.combo.getSelectedItem();
            double distancia = ((Number) fila.distancia.getValue()).doubleValue();
            if (destino == null) {
                avisoEdificio("Selecciona el punto de cada conexión.", false);
                return;
            }
            conexiones.add(new CampusControlador.ConexionNueva(destino, distancia, fila.escaleras.isSelected()));
        }
        if (conexiones.isEmpty()) {
            avisoEdificio("Agrega al menos una conexión con otro punto.", false);
            return;
        }

        try {
            controlador.agregarEdificio(id, nombre,
                    ubicacionGrafo.x, ubicacionGrafo.y,
                    ubicacionMapa.x, ubicacionMapa.y,
                    conexiones);

            String nombreVisible = nombre.isEmpty() ? ("Punto " + id) : nombre;
            panelMapa.registrarPosicionEdificio(id,
                    ubicacionGrafo.x, ubicacionGrafo.y,
                    ubicacionMapa.x, ubicacionMapa.y, nombreVisible);

            String detalleConexiones = conexiones.size() == 1
                    ? "1 conexión"
                    : conexiones.size() + " conexiones";
            avisoEdificio("Punto \"" + id + "\" agregado con " + detalleConexiones + ".", true);

            // Limpiar el formulario y refrescar todo lo que depende del grafo
            txtIdEdificio.setText("");
            txtNombreEdificio.setText("");
            ubicacionGrafo = null;
            ubicacionMapa = null;
            lblUbicacionGrafo.setText("Vista Grafo: sin ubicar");
            lblUbicacionGrafo.setForeground(UIColores.TEXTO_MUTED);
            lblUbicacionMapa.setText("Vista Mapa: sin ubicar");
            lblUbicacionMapa.setForeground(UIColores.TEXTO_MUTED);

            // Volver a dejar una sola fila de conexión, ya con el nuevo edificio disponible
            panelConexiones.removeAll();
            filasConexion.clear();
            agregarFilaConexion();
            panelConexiones.revalidate();
            panelConexiones.repaint();

            refrescarTablasCaminos();
            if (alCambiarGrafo != null) alCambiarGrafo.run();

        } catch (IllegalArgumentException ex) {
            avisoEdificio(ex.getMessage(), false);
        }
    }

    // ---------------- Filas dinámicas de conexión ----------------

    /** Una fila del formulario: a qué edificio se conecta, distancia y si tiene escaleras. */
    private class FilaConexion {
        final JPanel panel;
        final JComboBox<String> combo;
        final JSpinner distancia;
        final JCheckBox escaleras;

        FilaConexion(JPanel panel, JComboBox<String> combo, JSpinner distancia, JCheckBox escaleras) {
            this.panel = panel;
            this.combo = combo;
            this.distancia = distancia;
            this.escaleras = escaleras;
        }
    }

    /** Agrega una nueva fila para elegir otra conexión del edificio que se está creando. */
    private void agregarFilaConexion() {
        JComboBox<String> combo = new JComboBox<>();
        for (String edificioId : controlador.getGrafo().getEdificios().keySet()) {
            combo.addItem(edificioId);
        }
        combo.setFont(new Font(EstiloUPB.FAMILIA, Font.PLAIN, 12));
        EstiloUPB.estilizarCombo(combo, null);
        combo.setPreferredSize(new Dimension(66, 28));

        JSpinner distancia = new JSpinner(new SpinnerNumberModel(40.0, 1.0, 2000.0, 1.0));
        distancia.setPreferredSize(new Dimension(62, 26));

        JCheckBox escaleras = new JCheckBox("Escaleras");
        escaleras.setOpaque(false);
        escaleras.setFont(new Font(EstiloUPB.FAMILIA, Font.PLAIN, 11));
        EstiloUPB.comoInterruptor(escaleras);
        escaleras.setIconTextGap(5);

        JButton btnQuitar = new JButton("×");
        btnQuitar.setFont(new Font(EstiloUPB.FAMILIA, Font.BOLD, 11));
        btnQuitar.setMargin(new Insets(1, 5, 1, 5));
        btnQuitar.setFocusPainted(false);
        btnQuitar.setToolTipText("Quitar esta conexión");
        BotonUPB.aplicar(btnQuitar, BotonUPB.Estilo.SECUNDARIO);
        btnQuitar.setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));

        JPanel fila = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 3));
        fila.setOpaque(false);
        fila.setAlignmentX(Component.LEFT_ALIGNMENT);
        fila.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        fila.add(combo);
        fila.add(distancia);
        fila.add(new JLabel("m"));
        fila.add(escaleras);
        fila.add(btnQuitar);

        FilaConexion nueva = new FilaConexion(fila, combo, distancia, escaleras);
        filasConexion.add(nueva);
        panelConexiones.add(fila);

        btnQuitar.addActionListener(e -> quitarFilaConexion(nueva));
        actualizarBotonesQuitarConexion();

        panelConexiones.revalidate();
        panelConexiones.repaint();
    }

    /** Quita una fila de conexión, siempre que quede al menos una. */
    private void quitarFilaConexion(FilaConexion fila) {
        if (filasConexion.size() <= 1) return;
        filasConexion.remove(fila);
        panelConexiones.remove(fila.panel);
        actualizarBotonesQuitarConexion();
        panelConexiones.revalidate();
        panelConexiones.repaint();
    }

    /** El botón de quitar solo tiene sentido cuando hay más de una fila. */
    private void actualizarBotonesQuitarConexion() {
        boolean puedeQuitar = filasConexion.size() > 1;
        for (FilaConexion fila : filasConexion) {
            for (Component c : fila.panel.getComponents()) {
                if (c instanceof JButton) {
                    c.setEnabled(puedeQuitar);
                }
            }
        }
    }

    /** Elimina el edificio seleccionado, siempre que lo haya agregado el administrador. */
    private void eliminarEdificioSeleccionado() {
        int fila = tablaEdificios.getSelectedRow();
        if (fila < 0) {
            DialogoUPB.mensaje(this,
                    "Selecciona en la tabla el edificio que quieres eliminar.",
                    "Ningún edificio seleccionado", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        String id = String.valueOf(tablaEdificios.getValueAt(fila, 0));

        if (!controlador.esEdificioPersonalizado(id)) {
            DialogoUPB.mensaje(this,
                    "\"" + id + "\" forma parte del mapa original del campus y no se puede eliminar.\n\n"
                    + "Solo se pueden eliminar los edificios agregados desde esta pantalla.",
                    "Edificio protegido", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int conexiones = controlador.getGrafo().getAdyacentes(id).size();
        int respuesta = DialogoUPB.confirmar(this,
                "¿Eliminar el edificio \"" + id + "\"?\n\n"
                + "Se quitarán también sus " + conexiones + " camino(s) y dejará de aparecer "
                + "en el mapa y en las listas de origen y destino.",
                "Confirmar eliminación", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (respuesta != JOptionPane.YES_OPTION) return;

        try {
            controlador.eliminarEdificio(id);

            // El edificio deja de mostrarse por completo en las dos vistas
            panelMapa.quitarPosicionEdificio(id);

            refrescarTablasCaminos();
            if (alCambiarGrafo != null) alCambiarGrafo.run();

            avisoEdificio("Edificio \"" + id + "\" eliminado.", true);
        } catch (IllegalArgumentException ex) {
            DialogoUPB.mensaje(this, ex.getMessage(),
                    "No se pudo eliminar", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void avisoEdificio(String mensaje, boolean exito) {
        lblAvisoEdificio.setForeground(exito ? UIColores.EXITO : UIColores.ERROR);
        lblAvisoEdificio.setText(mensaje);
    }

    private DefaultTableModel construirModeloEdificios() {
        String[] cols = {"Edificio", "Nombre", "Conexiones", "Lugares", "Tipo"};
        List<Object[]> filas = new ArrayList<>();
        for (Edificio edificio : controlador.getGrafo().getEdificios().values()) {
            filas.add(new Object[]{
                    edificio.getId(), edificio.getNombre(),
                    controlador.getGrafo().getAdyacentes(edificio.getId()).size(),
                    edificio.getLugares().size(),
                    controlador.esEdificioPersonalizado(edificio.getId()) ? "Agregado" : "Original"});
        }
        return new DefaultTableModel(filas.toArray(new Object[0][]), cols) {
            @Override public boolean isCellEditable(int f, int c) { return false; }
            @Override public Class<?> getColumnClass(int c) { return (c == 2 || c == 3) ? Integer.class : String.class; }
        };
    }

    // ==================== 2. REPORTES ====================
    private JPanel crearPanelReportes() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBackground(UIColores.FONDO);
        panel.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));

        areaReportes = new JEditorPane("text/html", "");
        areaReportes.setEditable(false);
        areaReportes.setBackground(UIColores.TARJETA);
        areaReportes.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));

        JScrollPane scroll = new JScrollPane(areaReportes);
        scroll.setBorder(EstiloUPB.bordeSeccion("Estado actual del sistema"));
        panel.add(scroll, BorderLayout.CENTER);

        JButton btnRefrescar = new JButton("Actualizar reporte");
        btnRefrescar.setFont(new Font(EstiloUPB.FAMILIA, Font.BOLD, 12));
        btnRefrescar.setBackground(UIColores.PRIMARIO);
        btnRefrescar.setForeground(UIColores.TEXTO_CLARO);
        btnRefrescar.setFocusPainted(false);
        btnRefrescar.setOpaque(true);
        btnRefrescar.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        BotonUPB.aplicar(btnRefrescar, BotonUPB.Estilo.PRINCIPAL);
        btnRefrescar.addActionListener(e -> refrescarReportes());

        JPanel sur = new JPanel(new FlowLayout(FlowLayout.LEFT));
        sur.setBackground(UIColores.FONDO);
        sur.add(btnRefrescar);
        panel.add(sur, BorderLayout.SOUTH);

        refrescarReportes();
        return panel;
    }

    private void refrescarReportes() {
        if (areaReportes == null) return;

        List<Camino> caminos = obtenerCaminosUnicos();
        int totalCaminos = caminos.size();
        double distanciaTotal = 0;
        int conEscaleras = 0;
        List<Camino> bloqueados = new ArrayList<>();
        for (Camino c : caminos) {
            distanciaTotal += c.getDistancia();
            if (c.isTieneEscaleras()) conEscaleras++;
            if (c.isBloqueado()) bloqueados.add(c);
        }
        int accesibles = totalCaminos - conEscaleras;

        int totalEdificios = controlador.getGrafo().getEdificios().size();
        int totalLugares = 0;
        String edificioTop = "-";
        int maxLugares = -1;
        for (Edificio edificio : controlador.getGrafo().getEdificios().values()) {
            int cant = edificio.getLugares().size();
            totalLugares += cant;
            if (cant > maxLugares) {
                maxLugares = cant;
                edificioTop = edificio.getNombre() + " (" + cant + " lugares)";
            }
        }

        String colorPrimario = "#" + Integer.toHexString(UIColores.PRIMARIO.getRGB()).substring(2);
        String colorError = "#" + Integer.toHexString(UIColores.ERROR.getRGB()).substring(2);
        String colorExito = "#" + Integer.toHexString(UIColores.EXITO.getRGB()).substring(2);

        StringBuilder html = new StringBuilder();
        html.append("<html><body style='font-family:SansSerif; font-size:11px;'>");

        html.append("<h3 style='color:").append(colorPrimario).append("; margin-bottom:4px;'>Caminos bloqueados actualmente</h3>");
        if (bloqueados.isEmpty()) {
            html.append("<p style='color:").append(colorExito).append(";'>")
                .append("Ningún camino se encuentra bloqueado. Toda la red del campus está disponible.</p>");
        } else {
            html.append("<ul>");
            for (Camino c : bloqueados) {
                html.append("<li style='color:").append(colorError).append(";'>")
                    .append(c.getOrigenId()).append(" &harr; ").append(c.getDestinoId())
                    .append(" &nbsp;(").append(String.format("%.0f m", c.getDistancia())).append(")")
                    .append("</li>");
            }
            html.append("</ul>");
        }

        // ---- Caminos eliminados ----
        html.append("<h3 style='color:").append(colorPrimario).append("; margin-bottom:4px;'>Caminos eliminados</h3>");
        java.util.List<String[]> eliminados = controlador.getCaminosEliminados();
        if (eliminados.isEmpty()) {
            html.append("<p>No se ha eliminado ningún camino del grafo.</p>");
        } else {
            html.append("<ul>");
            for (String[] d : eliminados) {
                html.append("<li>").append(d[0]).append(" &harr; ").append(d[1])
                    .append(" &nbsp;(").append(String.format("%.0f m", Double.parseDouble(d[2])))
                    .append(Boolean.parseBoolean(d[3]) ? ", con escaleras" : ", sin escaleras")
                    .append(") — se puede restaurar desde la pestaña de bloqueos</li>");
            }
            html.append("</ul>");
        }

        html.append("<h3 style='color:").append(colorPrimario).append("; margin-bottom:4px;'>Resumen del grafo del campus</h3>");
        html.append("<table cellpadding='3'>");
        html.append(filaHtml("Edificios y puntos registrados", String.valueOf(totalEdificios)));
        html.append(filaHtml("Caminos registrados", String.valueOf(totalCaminos)));
        html.append(filaHtml("Distancia total de la red", String.format("%.0f m", distanciaTotal)));
        html.append(filaHtml("Caminos accesibles (sin escaleras)",
                accesibles + (totalCaminos > 0 ? String.format(" (%.0f%%)", accesibles * 100.0 / totalCaminos) : "")));
        html.append(filaHtml("Caminos con escaleras",
                conEscaleras + (totalCaminos > 0 ? String.format(" (%.0f%%)", conEscaleras * 100.0 / totalCaminos) : "")));
        html.append("</table>");

        html.append("<h3 style='color:").append(colorPrimario).append("; margin-bottom:4px;'>Lugares registrados</h3>");
        html.append("<table cellpadding='3'>");
        html.append(filaHtml("Total de lugares en el sistema", String.valueOf(totalLugares)));
        html.append(filaHtml("Edificio con más lugares", edificioTop));
        html.append("</table>");

        // ---- Historial de bloqueos ----
        html.append("<h3 style='color:").append(colorPrimario).append("; margin-bottom:4px;'>Historial de bloqueos</h3>");
        java.util.List<modelo.RegistroBloqueo> historial = controlador.getHistorialBloqueos();
        if (historial.isEmpty()) {
            html.append("<p>Todavía no se ha registrado ninguna acción de bloqueo.</p>");
        } else {
            html.append("<table cellpadding='4'>");
            html.append("<tr style='background:#EEEEEE;'><td><b>Fecha</b></td><td><b>Usuario</b></td>")
                .append("<td><b>Acción</b></td><td><b>Tramo</b></td></tr>");
            int mostrados = 0;
            for (modelo.RegistroBloqueo r : historial) {
                if (mostrados++ >= 15) break;
                String color = r.esBloqueo() ? colorError : colorExito;
                html.append("<tr><td>").append(r.getFechaLegible()).append("</td>")
                    .append("<td>").append(r.getUsuario()).append("</td>")
                    .append("<td style='color:").append(color).append(";'><b>")
                    .append(r.getAccionLegible()).append("</b></td>")
                    .append("<td>").append(r.getTramo()).append("</td></tr>");
            }
            html.append("</table>");
            if (historial.size() > 15) {
                html.append("<p style='font-size:10px;'>Mostrando los 15 más recientes de ")
                    .append(historial.size()).append(" registros.</p>");
            }
        }

        html.append("<h3 style='color:").append(colorPrimario).append("; margin-bottom:4px;'>Velocidades de cálculo en uso</h3>");
        html.append("<table cellpadding='3'>");
        html.append(filaHtml("Caminata en plano", String.format("%.0f m/min", EstimadorTiempo.getVelocidadPlano())));
        html.append(filaHtml("Caminata en escaleras", String.format("%.0f m/min", EstimadorTiempo.getVelocidadEscaleras())));
        html.append("</table>");

        html.append("<h3 style='color:").append(colorPrimario).append("; margin-bottom:4px;'>Datos guardados</h3>");
        html.append("<table cellpadding='3'>");
        html.append(filaHtml("Cuentas registradas", String.valueOf(controlador.cantidadUsuarios())));
        html.append(filaHtml("Archivo de usuarios", controlador.getRutaArchivoUsuarios()));
        html.append(filaHtml("Archivo de estado", controlador.getRutaArchivoEstado()));
        html.append("</table>");

        html.append("</body></html>");

        areaReportes.setText(html.toString());
        areaReportes.setCaretPosition(0);
    }

    private String filaHtml(String etiqueta, String valor) {
        return "<tr><td>" + etiqueta + ":</td><td><b>" + valor + "</b></td></tr>";
    }

    // ==================== 3. CONFIGURACIÓN ====================
    private JPanel crearPanelConfiguracion() {
        JPanel contenedor = new JPanel();
        contenedor.setBackground(UIColores.FONDO);
        contenedor.setLayout(new BoxLayout(contenedor, BoxLayout.Y_AXIS));
        contenedor.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));

        // ---- Perfil ----
        JPanel perfil = seccion("Mi perfil");
        perfil.add(filaEtiquetaValor("Usuario:", usuario));
        perfil.add(filaEtiquetaValor("Rol:", "Administrador"));
        contenedor.add(perfil);
        contenedor.add(Box.createVerticalStrut(12));

        // ---- Velocidades ----
        JPanel velocidades = seccion("Velocidades de caminata (afectan el tiempo estimado)");

        JSpinner spPlano = new JSpinner(new SpinnerNumberModel(
                EstimadorTiempo.getVelocidadPlano(), 5.0, 150.0, 1.0));
        JSpinner spEscaleras = new JSpinner(new SpinnerNumberModel(
                EstimadorTiempo.getVelocidadEscaleras(), 5.0, 150.0, 1.0));

        JPanel filaPlano = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        filaPlano.setOpaque(false);
        filaPlano.add(etiqueta("Velocidad en plano (m/min):", 210));
        filaPlano.add(spPlano);
        velocidades.add(filaPlano);

        JPanel filaEsc = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        filaEsc.setOpaque(false);
        filaEsc.add(etiqueta("Velocidad en escaleras (m/min):", 210));
        filaEsc.add(spEscaleras);
        velocidades.add(filaEsc);

        JLabel notaVel = new JLabel("Cambiar estos valores afecta todos los tiempos que muestra el sistema.");
        notaVel.setFont(new Font(EstiloUPB.FAMILIA, Font.ITALIC, 11));
        notaVel.setForeground(UIColores.TEXTO_MUTED);
        notaVel.setAlignmentX(Component.LEFT_ALIGNMENT);
        velocidades.add(Box.createVerticalStrut(4));
        velocidades.add(notaVel);

        JLabel avisoVel = new JLabel(" ");
        avisoVel.setFont(new Font(EstiloUPB.FAMILIA, Font.BOLD, 12));
        avisoVel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel filaBotones = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        filaBotones.setOpaque(false);
        JButton btnAplicar = botonPrimario("Aplicar velocidades");
        JButton btnRestaurar = botonSecundario("Restaurar valores por defecto");
        filaBotones.add(btnAplicar);
        filaBotones.add(btnRestaurar);
        velocidades.add(Box.createVerticalStrut(6));
        velocidades.add(filaBotones);
        velocidades.add(avisoVel);

        btnAplicar.addActionListener(e -> {
            EstimadorTiempo.setVelocidadPlano(((Number) spPlano.getValue()).doubleValue());
            EstimadorTiempo.setVelocidadEscaleras(((Number) spEscaleras.getValue()).doubleValue());
            controlador.guardarVelocidades();
            avisoVel.setForeground(UIColores.EXITO);
            avisoVel.setText("Velocidades guardadas. Vuelve a calcular una ruta para ver el nuevo tiempo.");
            refrescarReportes();
        });
        btnRestaurar.addActionListener(e -> {
            EstimadorTiempo.restaurarValoresPorDefecto();
            spPlano.setValue(EstimadorTiempo.getVelocidadPlano());
            spEscaleras.setValue(EstimadorTiempo.getVelocidadEscaleras());
            controlador.guardarVelocidades();
            avisoVel.setForeground(UIColores.EXITO);
            avisoVel.setText("Se restauraron y guardaron los valores por defecto.");
            refrescarReportes();
        });

        contenedor.add(velocidades);
        contenedor.add(Box.createVerticalStrut(12));

        // ---- Cambio de contraseña ----
        JPanel clave = seccion("Cambiar contraseña");

        JPasswordField txtActual = new JPasswordField(16);
        JPasswordField txtNueva = new JPasswordField(16);
        JPasswordField txtConfirmar = new JPasswordField(16);
        for (JPasswordField campo : new JPasswordField[]{txtActual, txtNueva, txtConfirmar}) {
            EstiloUPB.redondearCampo(campo);
        }

        clave.add(filaCampo("Contraseña actual:", conBotonMostrar(txtActual)));
        clave.add(filaCampo("Nueva contraseña:", conBotonMostrar(txtNueva)));
        clave.add(filaCampo("Confirmar nueva:", conBotonMostrar(txtConfirmar)));

        JLabel avisoClave = new JLabel(" ");
        avisoClave.setFont(new Font(EstiloUPB.FAMILIA, Font.BOLD, 12));
        avisoClave.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton btnCambiar = botonPrimario("Actualizar contraseña");
        JPanel filaBtnClave = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        filaBtnClave.setOpaque(false);
        filaBtnClave.add(btnCambiar);
        clave.add(Box.createVerticalStrut(6));
        clave.add(filaBtnClave);
        clave.add(avisoClave);

        JLabel notaClave = new JLabel("El cambio se guarda en el archivo de usuarios del sistema.");
        notaClave.setFont(new Font(EstiloUPB.FAMILIA, Font.ITALIC, 11));
        notaClave.setForeground(UIColores.TEXTO_MUTED);
        notaClave.setAlignmentX(Component.LEFT_ALIGNMENT);
        clave.add(Box.createVerticalStrut(4));
        clave.add(notaClave);

        btnCambiar.addActionListener(e -> {
            String actual = new String(txtActual.getPassword());
            String nueva = new String(txtNueva.getPassword());
            String confirmar = new String(txtConfirmar.getPassword());

            if (actual.isEmpty() || nueva.isEmpty() || confirmar.isEmpty()) {
                avisoClave.setForeground(UIColores.ERROR);
                avisoClave.setText("Completa los tres campos.");
            } else if (nueva.length() < 5) {
                avisoClave.setForeground(UIColores.ERROR);
                avisoClave.setText("La nueva contraseña debe tener al menos 5 caracteres.");
            } else if (!nueva.equals(confirmar)) {
                avisoClave.setForeground(UIColores.ERROR);
                avisoClave.setText("La nueva contraseña y su confirmación no coinciden.");
            } else if (nueva.equals(actual)) {
                avisoClave.setForeground(UIColores.ERROR);
                avisoClave.setText("La nueva contraseña debe ser distinta de la actual.");
            } else {
                modelo.Usuario activo = controlador.getUsuarioActivo();
                if (activo == null) {
                    avisoClave.setForeground(UIColores.ERROR);
                    avisoClave.setText("No hay una sesión con cuenta registrada.");
                } else if (controlador.cambiarContrasena(activo.getCorreo(), actual, nueva)) {
                    avisoClave.setForeground(UIColores.EXITO);
                    avisoClave.setText("Contraseña actualizada y guardada.");
                    txtActual.setText("");
                    txtNueva.setText("");
                    txtConfirmar.setText("");
                } else {
                    avisoClave.setForeground(UIColores.ERROR);
                    avisoClave.setText("La contraseña actual no es correcta.");
                }
            }
        });

        contenedor.add(clave);

        JScrollPane scroll = new JScrollPane(contenedor);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(UIColores.FONDO);
        wrapper.add(scroll, BorderLayout.CENTER);
        return wrapper;
    }

    private JPanel seccion(String titulo) {
        JPanel p = new JPanel();
        p.setBackground(UIColores.TARJETA);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(BorderFactory.createCompoundBorder(
                EstiloUPB.bordeSeccion(titulo),
                BorderFactory.createEmptyBorder(8, 12, 12, 12)));
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        return p;
    }

    private JPanel filaEtiquetaValor(String etiquetaTexto, String valor) {
        JPanel fila = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        fila.setOpaque(false);
        fila.setAlignmentX(Component.LEFT_ALIGNMENT);
        fila.add(etiqueta(etiquetaTexto, 210));
        JLabel lblValor = new JLabel(valor);
        lblValor.setFont(new Font(EstiloUPB.FAMILIA, Font.BOLD, 12));
        lblValor.setForeground(UIColores.TEXTO_OSCURO);
        fila.add(lblValor);
        return fila;
    }

    private JPanel filaCampo(String etiquetaTexto, JComponent campo) {
        JPanel fila = new JPanel(new BorderLayout(8, 0));
        fila.setOpaque(false);
        fila.setAlignmentX(Component.LEFT_ALIGNMENT);
        // Evita que la fila se estire al usarse dentro de un BoxLayout vertical
        fila.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        fila.setPreferredSize(new Dimension(320, 34));

        JLabel lbl = etiqueta(etiquetaTexto, 150);
        fila.add(lbl, BorderLayout.WEST);
        fila.add(campo, BorderLayout.CENTER);
        return fila;
    }

    private JLabel etiqueta(String texto, int ancho) {
        JLabel l = new JLabel(texto);
        l.setFont(new Font(EstiloUPB.FAMILIA, Font.PLAIN, 12));
        l.setForeground(UIColores.TEXTO_MUTED);
        l.setPreferredSize(new Dimension(ancho, 20));
        return l;
    }

    private JButton botonPrimario(String texto) {
        JButton b = new JButton(texto);
        b.setFont(new Font(EstiloUPB.FAMILIA, Font.BOLD, 12));
        b.setBackground(UIColores.PRIMARIO);
        b.setForeground(UIColores.TEXTO_CLARO);
        b.setFocusPainted(false);
        b.setOpaque(true);
        b.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        BotonUPB.aplicar(b, BotonUPB.Estilo.PRINCIPAL);
        return b;
    }

    /** Acción destructiva (eliminar): contorno rojo. */
    private JButton botonPeligro(String texto) {
        JButton b = botonSecundario(texto);
        BotonUPB.aplicar(b, BotonUPB.Estilo.PELIGRO);
        return b;
    }

    private JButton botonSecundario(String texto) {
        JButton b = new JButton(texto);
        b.setFont(new Font(EstiloUPB.FAMILIA, Font.PLAIN, 12));
        b.setBackground(UIColores.TARJETA);
        b.setForeground(UIColores.PRIMARIO);
        b.setFocusPainted(false);
        b.setOpaque(true);
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIColores.PRIMARIO, 1),
                BorderFactory.createEmptyBorder(7, 15, 7, 15)));
        BotonUPB.aplicar(b, BotonUPB.Estilo.SECUNDARIO);
        return b;
    }

    // ==================== Modelo y renderizadores de la tabla ====================
    private static class ModeloTablaCaminos extends AbstractTableModel {
        private final String[] columnas = {"Origen", "Destino", "Distancia", "Escaleras", "Estado", "Acción", "Detalle"};
        private List<Camino> caminos;

        ModeloTablaCaminos(List<Camino> caminos) { this.caminos = caminos; }

        void setCaminos(List<Camino> caminos) {
            this.caminos = caminos;
            fireTableDataChanged();
        }

        Camino getCaminoEn(int fila) { return caminos.get(fila); }

        @Override public int getRowCount() { return caminos.size(); }
        @Override public int getColumnCount() { return columnas.length; }
        @Override public String getColumnName(int col) { return columnas[col]; }
        @Override public boolean isCellEditable(int fila, int col) { return false; }

        @Override
        public Object getValueAt(int fila, int col) {
            Camino c = caminos.get(fila);
            switch (col) {
                case 0: return c.getOrigenId();
                case 1: return c.getDestinoId();
                case 2: return String.format("%.0f m", c.getDistancia());
                case 3: return c.isTieneEscaleras() ? "Sí" : "No";
                case 4: return c.isBloqueado() ? "Bloqueado" : "Disponible";
                case 5: return c.isBloqueado();
                case 6: return "···";
                default: return "";
            }
        }
    }

    /** Pinta el estado como una "pastilla" redondeada verde o roja. */
    private static class RenderizadorEstado extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable tabla, Object valor, boolean sel,
                                                       boolean foco, int fila, int col) {
            String texto = String.valueOf(valor);
            boolean bloqueado = "Bloqueado".equals(texto);
            JPanel contenedor = new JPanel(new GridBagLayout());
            contenedor.setBackground(EstiloUPB.fondoFila(tabla, fila, sel));
            JLabel pastilla = new JLabel(texto) {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(getBackground());
                    g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), getHeight(), getHeight()));
                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            pastilla.setOpaque(false);
            pastilla.setBackground(bloqueado ? UIColores.ERROR_FONDO : UIColores.EXITO_FONDO);
            pastilla.setForeground(bloqueado ? UIColores.ERROR : new Color(0x0B, 0x7A, 0x55));
            pastilla.setFont(new Font(EstiloUPB.FAMILIA, Font.BOLD, 11));
            pastilla.setHorizontalAlignment(SwingConstants.CENTER);
            pastilla.setBorder(BorderFactory.createEmptyBorder(4, 12, 4, 12));
            contenedor.add(pastilla);
            return contenedor;
        }
    }

    /** Pinta un interruptor tipo switch según el estado del tramo. */
    private static class RenderizadorInterruptor extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable tabla, Object valor, boolean sel,
                                                       boolean foco, int fila, int col) {
            boolean bloqueado = Boolean.TRUE.equals(valor);
            final boolean disponible = !bloqueado;
            JPanel p = new JPanel(new GridBagLayout()) {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    int w = 44, h = 22;
                    int x = (getWidth() - w) / 2, y = (getHeight() - h) / 2;
                    g2.setColor(disponible ? UIColores.EXITO : new Color(0xCB, 0xD5, 0xE1));
                    g2.fill(new RoundRectangle2D.Double(x, y, w, h, h, h));
                    int d = h - 6;
                    int knobX = disponible ? x + w - d - 3 : x + 3;
                    g2.setColor(Color.WHITE);
                    g2.fillOval(knobX, y + 3, d, d);
                    g2.dispose();
                }
            };
            p.setBackground(EstiloUPB.fondoFila(tabla, fila, sel));
            p.setToolTipText(disponible ? "Disponible - clic para bloquear" : "Bloqueado - clic para habilitar");
            return p;
        }
    }

    /** Columna de tres puntos para abrir el detalle del tramo. */
    private static class RenderizadorDetalle extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable tabla, Object valor, boolean sel,
                                                       boolean foco, int fila, int col) {
            JLabel l = (JLabel) super.getTableCellRendererComponent(tabla, valor, sel, foco, fila, col);
            l.setHorizontalAlignment(SwingConstants.CENTER);
            l.setFont(new Font(EstiloUPB.FAMILIA, Font.BOLD, 16));
            l.setForeground(UIColores.PRIMARIO);
            l.setToolTipText("Ver detalle del tramo");
            l.setBackground(EstiloUPB.fondoFila(tabla, fila, sel));
            return l;
        }
    }
}
