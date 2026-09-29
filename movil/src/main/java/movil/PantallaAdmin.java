package movil;

import controlador.CampusControlador;
import modelo.Camino;
import modelo.EstimadorTiempo;
import modelo.RegistroBloqueo;
import org.teavm.jso.dom.html.HTMLElement;
import org.teavm.jso.dom.html.HTMLInputElement;
import org.teavm.jso.dom.html.HTMLSelectElement;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Panel de administración para el celular: bloquear caminos, agregar puntos al
 * campus, ajustar las velocidades de caminata y ver el historial de bloqueos.
 * Usa las mismas operaciones del controlador que el panel de escritorio.
 */
final class PantallaAdmin {

    private final AppMovil app;
    private final CampusControlador c;
    private final HTMLElement elemento = Dom.el("div", "pantalla");
    private final HTMLElement pestanas = Dom.el("div", "segmentos");
    private final HTMLElement cuerpo = Dom.el("div", null);
    private String seccion = "caminos";

    // Formulario de punto nuevo (se conserva mientras se ubica en el mapa)
    private final HTMLInputElement txtId = Dom.campo("text", "Ej.: N");
    private final HTMLInputElement txtNombre = Dom.campo("text", "Ej.: Punto Nuevo");
    private final List<FilaConexion> conexiones = new ArrayList<>();
    private int[] ubicacion;

    private static final class FilaConexion {
        final HTMLSelectElement destino = Dom.lista("campo");
        final HTMLInputElement distancia = Dom.campo("number", "Metros");
        final HTMLInputElement escaleras = (HTMLInputElement) Dom.el("input", null);
    }

    PantallaAdmin(AppMovil app) {
        this.app = app;
        this.c = app.controlador;
        elemento.appendChild(pestanas);
        elemento.appendChild(cuerpo);
        txtId.setAttribute("maxlength", "12");
    }

    HTMLElement getElemento() {
        return elemento;
    }

    void alMostrar() {
        Dom.vaciar(pestanas);
        String[][] secciones = {{"caminos", "Caminos"}, {"punto", "Nuevo punto"},
                {"velocidad", "Velocidad"}, {"historial", "Historial"}};
        for (String[] s : secciones) {
            HTMLElement b = Dom.boton(s[1], "segmento" + (s[0].equals(seccion) ? " activo" : ""), () -> {
                seccion = s[0];
                alMostrar();
            });
            pestanas.appendChild(b);
        }
        Dom.vaciar(cuerpo);
        switch (seccion) {
            case "punto": mostrarNuevoPunto(); break;
            case "velocidad": mostrarVelocidades(); break;
            case "historial": mostrarHistorial(); break;
            default: mostrarCaminos();
        }
    }

    // ==================== Bloqueo de caminos ====================

    /** Cada camino una sola vez (el grafo guarda los dos sentidos). */
    private List<Camino> caminosUnicos() {
        List<Camino> lista = new ArrayList<>();
        Set<String> vistos = new HashSet<>();
        for (String id : c.getGrafo().getEdificios().keySet()) {
            for (Camino k : c.getGrafo().getAdyacentes(id)) {
                if (vistos.add(CoordenadasMapa.clave(k.getOrigenId(), k.getDestinoId()))) lista.add(k);
            }
        }
        return lista;
    }

    private void mostrarCaminos() {
        List<Camino> caminos = caminosUnicos();
        int bloqueados = 0;
        for (Camino k : caminos) if (k.isBloqueado()) bloqueados++;

        HTMLElement kpis = Dom.el("div", "cifras");
        kpis.appendChild(cifra(String.valueOf(caminos.size()), "Caminos"));
        kpis.appendChild(cifra(String.valueOf(caminos.size() - bloqueados), "Disponibles"));
        kpis.appendChild(cifra(String.valueOf(bloqueados), "Bloqueados"));
        cuerpo.appendChild(kpis);
        cuerpo.appendChild(Dom.texto("p", "texto-suave",
                "Toca Bloquear para cerrar un camino (obras, eventos…). Las rutas lo evitarán."));

        for (Camino k : caminos) {
            HTMLElement fila = Dom.el("div", "tarjeta camino" + (k.isBloqueado() ? " camino-bloqueado" : ""));
            HTMLElement info = Dom.el("div", "camino-info");
            info.appendChild(Dom.texto("strong", null, k.getOrigenId() + "  ↔  " + k.getDestinoId()));
            info.appendChild(Dom.texto("span", "texto-suave", String.format("%.0f m", k.getDistancia())
                    + (k.isTieneEscaleras() ? " · con escaleras" : " · sin escaleras")
                    + (k.isBloqueado() ? " · BLOQUEADO" : "")));
            fila.appendChild(info);
            fila.appendChild(Dom.boton(k.isBloqueado() ? "Desbloquear" : "Bloquear",
                    k.isBloqueado() ? "btn btn-exito btn-chico" : "btn btn-peligro btn-chico",
                    () -> alternarBloqueo(k)));
            cuerpo.appendChild(fila);
        }
    }

    private void alternarBloqueo(Camino k) {
        boolean bloquear = !k.isBloqueado();
        Runnable aplicar = () -> {
            try {
                c.solicitarBloqueo(k.getOrigenId(), k.getDestinoId(), bloquear);
                app.alCambiarGrafo();
                Dom.aviso((bloquear ? "Bloqueado: " : "Desbloqueado: ")
                        + k.getOrigenId() + " - " + k.getDestinoId(), true);
            } catch (IllegalArgumentException ex) {
                Dom.aviso(ex.getMessage(), false);
            }
            alMostrar();
        };
        if (bloquear) {
            // Igual que en escritorio: advertir si el tramo es la única vía hacia algún punto
            List<String> aislados = c.consultarAislamientoPorBloqueo(k.getOrigenId(), k.getDestinoId());
            if (!aislados.isEmpty()) {
                Dom.confirmar("El bloqueo deja puntos incomunicados",
                        "Si bloqueas " + k.getOrigenId() + " - " + k.getDestinoId()
                        + ", estos puntos quedarían sin ninguna vía de acceso: " + String.join(", ", aislados)
                        + ". No se podrá calcular ninguna ruta hacia ellos.",
                        "Bloquear igual", true, aplicar);
                return;
            }
        }
        aplicar.run();
    }

    // ==================== Nuevo punto ====================

    private void mostrarNuevoPunto() {
        HTMLElement t = Dom.el("section", "tarjeta");
        t.appendChild(Dom.texto("h2", null, "Registrar nuevo punto en el campus"));
        t.appendChild(Dom.grupo("Identificador", txtId));
        t.appendChild(Dom.grupo("Nombre", txtNombre));

        HTMLElement ubic = Dom.el("div", "ubicacion");
        ubic.appendChild(Dom.texto("span", ubicacion == null ? "texto-suave" : "etiqueta etiqueta-ok",
                ubicacion == null ? "Sin ubicar en el mapa" : "Ubicado en el mapa ✓"));
        ubic.appendChild(Dom.boton(ubicacion == null ? "Ubicar en el mapa" : "Cambiar ubicación",
                "btn btn-contorno btn-chico", () -> app.pedirUbicacion(p -> ubicacion = p)));
        t.appendChild(ubic);

        t.appendChild(Dom.texto("h3", "subtitulo", "Conexiones con otros puntos del campus"));
        if (conexiones.isEmpty()) agregarConexion();
        for (FilaConexion f : conexiones) t.appendChild(filaConexion(f));
        t.appendChild(Dom.boton("+ Agregar conexión", "btn btn-texto", () -> {
            agregarConexion();
            alMostrar();
        }));
        t.appendChild(Dom.boton("Agregar punto al grafo", "btn btn-primario btn-bloque", this::agregarPunto));
        cuerpo.appendChild(t);

        List<modelo.Edificio> agregados = new ArrayList<>();
        for (persistencia.RepositorioEstado.EdificioPersonalizado e : c.getEdificiosPersonalizados()) {
            modelo.Edificio ed = c.getGrafo().getEdificios().get(e.id);
            if (ed != null) agregados.add(ed);
        }
        if (!agregados.isEmpty()) {
            HTMLElement lista = Dom.el("section", "tarjeta");
            lista.appendChild(Dom.texto("h2", null, "Puntos agregados"));
            for (modelo.Edificio ed : agregados) {
                HTMLElement fila = Dom.el("div", "camino");
                HTMLElement info = Dom.el("div", "camino-info");
                info.appendChild(Dom.texto("strong", null, ed.getId() + " · " + ed.getNombre()));
                info.appendChild(Dom.texto("span", "texto-suave", c.getGrafo().getAdyacentes(ed.getId()).size() + " conexiones"));
                fila.appendChild(info);
                fila.appendChild(Dom.boton("Eliminar", "btn btn-peligro btn-chico", () ->
                        Dom.confirmar("Eliminar punto", "¿Eliminar " + ed.getNombre() + " y todos sus caminos?",
                                "Eliminar", true, () -> {
                                    try {
                                        c.eliminarEdificio(ed.getId());
                                        if (ed.getId().equals(app.origen)) app.origen = null;
                                        if (ed.getId().equals(app.destino)) app.destino = null;
                                        app.alCambiarGrafo();
                                        Dom.aviso("Punto eliminado.", true);
                                    } catch (IllegalArgumentException ex) {
                                        Dom.aviso(ex.getMessage(), false);
                                    }
                                    alMostrar();
                                })));
                lista.appendChild(fila);
            }
            cuerpo.appendChild(lista);
        }
    }

    private void agregarConexion() {
        FilaConexion f = new FilaConexion();
        f.escaleras.setType("checkbox");
        f.distancia.setAttribute("min", "1");
        f.distancia.setAttribute("inputmode", "numeric");
        Dom.opcion(f.destino, "", "Punto…");
        for (String id : c.getGrafo().getEdificios().keySet()) Dom.opcion(f.destino, id, id);
        conexiones.add(f);
    }

    private HTMLElement filaConexion(FilaConexion f) {
        HTMLElement fila = Dom.el("div", "conexion");
        fila.appendChild(f.destino);
        fila.appendChild(f.distancia);
        HTMLElement esc = Dom.el("label", "check");
        esc.appendChild(f.escaleras);
        esc.appendChild(Dom.texto("span", null, "Escaleras"));
        fila.appendChild(esc);
        fila.appendChild(Dom.boton("✕", "btn-icono btn-cerrar", () -> {
            conexiones.remove(f);
            alMostrar();
        }));
        return fila;
    }

    private void agregarPunto() {
        String id = txtId.getValue().trim();
        String nombre = txtNombre.getValue().trim();
        if (id.isEmpty()) { Dom.aviso("Escribe el identificador del punto.", false); return; }
        if (ubicacion == null) { Dom.aviso("Ubica el punto en el mapa antes de agregarlo.", false); return; }

        List<CampusControlador.ConexionNueva> nuevas = new ArrayList<>();
        for (FilaConexion f : conexiones) {
            String destino = f.destino.getValue();
            if (destino == null || destino.isEmpty()) { Dom.aviso("Selecciona el punto de cada conexión.", false); return; }
            double metros;
            try {
                metros = Double.parseDouble(f.distancia.getValue().trim());
            } catch (NumberFormatException ex) {
                Dom.aviso("Escribe la distancia (en metros) de cada conexión.", false);
                return;
            }
            nuevas.add(new CampusControlador.ConexionNueva(destino, metros, f.escaleras.isChecked()));
        }
        try {
            // En el celular no hay vista de grafo: se usa la misma posición del mapa como referencia
            c.agregarEdificio(id, nombre, ubicacion[0] / 2, ubicacion[1] / 2, ubicacion[0], ubicacion[1], nuevas);
        } catch (IllegalArgumentException ex) {
            Dom.aviso(ex.getMessage(), false);
            return;
        }
        app.alCambiarGrafo();
        Dom.aviso("Punto \"" + id + "\" agregado con " + nuevas.size()
                + (nuevas.size() == 1 ? " conexión." : " conexiones."), true);
        txtId.setValue("");
        txtNombre.setValue("");
        ubicacion = null;
        conexiones.clear();
        alMostrar();
    }

    // ==================== Velocidades ====================

    private void mostrarVelocidades() {
        HTMLElement t = Dom.el("section", "tarjeta");
        t.appendChild(Dom.texto("h2", null, "Velocidad de caminata"));
        t.appendChild(Dom.texto("p", "texto-suave",
                "Se usan para estimar el tiempo de cada ruta, tramo por tramo."));
        HTMLInputElement plano = Dom.campo("number", null);
        HTMLInputElement escaleras = Dom.campo("number", null);
        plano.setValue(formato(EstimadorTiempo.getVelocidadPlano()));
        escaleras.setValue(formato(EstimadorTiempo.getVelocidadEscaleras()));
        t.appendChild(Dom.grupo("Tramos planos (metros por minuto)", plano));
        t.appendChild(Dom.grupo("Tramos con escaleras (metros por minuto)", escaleras));
        t.appendChild(Dom.boton("Guardar", "btn btn-primario btn-bloque", () -> {
            try {
                double p = Double.parseDouble(plano.getValue().trim());
                double e = Double.parseDouble(escaleras.getValue().trim());
                if (p <= 0 || e <= 0) throw new NumberFormatException();
                EstimadorTiempo.setVelocidadPlano(p);
                EstimadorTiempo.setVelocidadEscaleras(e);
                c.guardarVelocidades();
                app.alCambiarGrafo();
                Dom.aviso("Velocidades guardadas.", true);
            } catch (NumberFormatException ex) {
                Dom.aviso("Escribe velocidades mayores que cero.", false);
            }
        }));
        t.appendChild(Dom.boton("Restaurar valores por defecto", "btn btn-texto btn-bloque", () -> {
            EstimadorTiempo.restaurarValoresPorDefecto();
            c.guardarVelocidades();
            app.alCambiarGrafo();
            alMostrar();
            Dom.aviso("Se restauraron 23 y 17 m/min.", true);
        }));
        cuerpo.appendChild(t);
    }

    private static String formato(double v) {
        return v == Math.floor(v) ? String.valueOf((long) v) : String.valueOf(v);
    }

    // ==================== Historial ====================

    private void mostrarHistorial() {
        List<RegistroBloqueo> historial = c.getHistorialBloqueos();
        if (historial.isEmpty()) {
            cuerpo.appendChild(Dom.texto("p", "vacio", "Todavía no hay bloqueos ni desbloqueos registrados."));
            return;
        }
        for (RegistroBloqueo r : historial) {
            HTMLElement fila = Dom.el("div", "tarjeta camino");
            HTMLElement info = Dom.el("div", "camino-info");
            info.appendChild(Dom.texto("strong", null, r.getAccionLegible() + " " + r.getTramo()));
            info.appendChild(Dom.texto("span", "texto-suave", r.getFechaLegible() + " · " + r.getUsuario()));
            fila.appendChild(info);
            fila.appendChild(Dom.texto("span", r.esBloqueo() ? "etiqueta etiqueta-aviso" : "etiqueta etiqueta-ok",
                    r.esBloqueo() ? "Bloqueo" : "Desbloqueo"));
            cuerpo.appendChild(fila);
        }
    }

    private static HTMLElement cifra(String valor, String etiqueta) {
        HTMLElement e = Dom.el("div", "cifra");
        e.appendChild(Dom.texto("strong", null, valor));
        e.appendChild(Dom.texto("span", null, etiqueta));
        return e;
    }
}
