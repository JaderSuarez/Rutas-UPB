package vista;

import javax.swing.*;
import java.awt.*;

/**
 * Ventana de ayuda (RF-14): explica de forma sencilla cómo usar la aplicación,
 * pensada especialmente para estudiantes nuevos y visitantes. La sección de
 * administración solo se muestra cuando el usuario tiene rol Administrador.
 */
public class VentanaAyuda extends JDialog {

    public VentanaAyuda(Frame propietario, boolean esAdmin) {
        super(propietario, "Ayuda - Sistema de Rutas Óptimas UPB", true);
        setSize(640, 580);
        setLocationRelativeTo(propietario);
        setLayout(new BorderLayout());

        // ---------- Cabecera ----------
        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
        header.setBackground(UIColores.PRIMARIO);
        header.add(new LogoUPB(30));
        JLabel titulo = new JLabel("<html><b style='font-size:13px'>¿Cómo usar el sistema?</b><br>"
                + "<i>Guía rápida para moverte por el campus</i></html>");
        titulo.setForeground(UIColores.TEXTO_CLARO);
        header.add(titulo);
        add(header, BorderLayout.NORTH);

        // ---------- Contenido ----------
        JEditorPane contenido = new JEditorPane("text/html", construirTexto(esAdmin));
        contenido.setEditable(false);
        contenido.setCaretPosition(0);
        contenido.setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));
        JScrollPane scroll = new JScrollPane(contenido);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        add(scroll, BorderLayout.CENTER);

        // ---------- Pie ----------
        JButton btnCerrar = BotonUPB.crear("Entendido", BotonUPB.Estilo.PRINCIPAL);
        btnCerrar.setBorder(BorderFactory.createEmptyBorder(7, 18, 7, 18));
        btnCerrar.addActionListener(e -> dispose());
        JPanel pie = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        pie.setBackground(UIColores.FONDO);
        pie.add(btnCerrar);
        add(pie, BorderLayout.SOUTH);
    }

    private String construirTexto(boolean esAdmin) {
        String h = "<h3 style='color:#800020; margin-bottom:2px'>";
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body style='font-family:SansSerif; font-size:11px; color:#1E293B'>");

        sb.append(h).append("1. ¿Qué hace el sistema?</h3>")
          .append("Calcula el camino más corto entre dos puntos del campus de la UPB Seccional Bucaramanga ")
          .append("(edificios, porterías, cafetería y templo) y te muestra la distancia, el tiempo estimado ")
          .append("y el recorrido en el mapa.");

        sb.append(h).append("2. Buscar un lugar</h3>")
          .append("Si sabes a qué lugar vas (por ejemplo <i>biblioteca</i> o <i>auditorio</i>) pero no en qué edificio está, ")
          .append("escríbelo en el campo <b>Lugar</b> y presiona <b>Buscar Lugar</b>. Puedes escribir sin tildes y con ")
          .append("varias palabras en cualquier orden. Verás las coincidencias con su edificio: presiona <b>Ir aquí</b> para ")
          .append("usarlo como destino o <b>Salir de aquí</b> para usarlo como origen.");

        sb.append(h).append("3. Calcular una ruta</h3>")
          .append("Elige el <b>Punto Origen</b> y el <b>Punto Destino</b> en las listas desplegables y presiona ")
          .append("<b>Calcular Ruta Más Corta</b>. El botón <b>Invertir</b> intercambia origen y destino, y los botones ")
          .append("<b>Estoy en: Portería 1 / Portería 2</b> fijan tu origen con un clic. ")
          .append("El origen y el destino deben ser puntos distintos.");

        sb.append(h).append("4. Ruta accesible (sin escaleras)</h3>")
          .append("Activa el interruptor <b>Evitar escaleras</b> antes de calcular para obtener una ruta que use solo caminos ")
          .append("sin escaleras. Si no existe una ruta accesible entre los dos puntos, el sistema te lo avisará.");

        sb.append(h).append("5. Leer el resultado</h3>")
          .append("En <b>Resumen de la Ruta</b> verás la distancia total, el tiempo estimado y los tramos del recorrido. ")
          .append("Si quieres más información, presiona <b>Mostrar detalle técnico de la ruta</b> para ver la secuencia ")
          .append("completa de puntos; con el mismo botón lo vuelves a ocultar.");

        sb.append(h).append("6. Ver la ruta en el mapa</h3>")
          .append("Abre la pestaña <b>Mapa del Campus</b>. La ruta calculada aparece resaltada. Puedes alternar entre ")
          .append("<b>Vista Mapa</b> y <b>Vista Grafo</b> y usar los botones de zoom. En la vista de grafo, las líneas ")
          .append("<b style='color:#DA297A'>fucsia</b> no tienen escaleras, las <b>negras</b> sí tienen, y las grises ")
          .append("punteadas están bloqueadas temporalmente.");

        sb.append(h).append("7. Exportar la ruta</h3>")
          .append("Después de calcular una ruta, el botón <b>Exportar ruta</b> guarda las instrucciones en un archivo de texto.");

        if (esAdmin) {
            sb.append(h).append("8. Administración</h3>")
              .append("En la pestaña <b>Administración</b> puedes: bloquear o desbloquear caminos con el interruptor de cada ")
              .append("tramo (<b>Bloqueos de Caminos</b>); registrar edificios nuevos con sus conexiones y eliminar los que ")
              .append("agregaste (<b>Edificios y Caminos</b>); consultar las estadísticas del campus (<b>Reportes</b>); y ajustar ")
              .append("las velocidades de caminata o cambiar tu contraseña (<b>Configuración</b>).");
        }

        sb.append(h).append(esAdmin ? "9." : "8.").append(" Cerrar sesión</h3>")
          .append("Usa el botón <b>Cerrar Sesión</b> de la parte superior para salir y volver a la pantalla de inicio.");

        sb.append("</body></html>");
        return sb.toString();
    }
}
