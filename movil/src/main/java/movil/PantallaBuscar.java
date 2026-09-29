package movil;

import modelo.Edificio;
import modelo.Lugar;
import org.teavm.jso.dom.html.HTMLElement;
import org.teavm.jso.dom.html.HTMLInputElement;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/** Buscar lugares del campus (cafeterías, oficinas, laboratorios…) con BuscadorLugares. */
final class PantallaBuscar {

    private static final String[] SUGERENCIAS = {"Cafetería", "Rectoría", "Gimnasio", "Laboratorio",
            "Banco", "Auditorio", "Psicología", "Sala de docentes"};

    private final AppMovil app;
    private final HTMLElement elemento = Dom.el("div", "pantalla");
    private final HTMLInputElement campo = Dom.campo("search", "Busca un lugar: cafetería, banco…");
    private final HTMLElement resultados = Dom.el("div", "resultados");

    PantallaBuscar(AppMovil app) {
        this.app = app;
        HTMLElement caja = Dom.el("div", "buscador");
        caja.appendChild(Dom.icono(Dom.ICONO_BUSCAR));
        caja.appendChild(campo);
        elemento.appendChild(caja);
        elemento.appendChild(resultados);
        campo.addEventListener("input", e -> buscar());
    }

    HTMLElement getElemento() {
        return elemento;
    }

    void alMostrar() {
        buscar();
    }

    private void buscar() {
        Dom.vaciar(resultados);
        String termino = campo.getValue().trim();
        if (termino.isEmpty()) {
            resultados.appendChild(Dom.texto("p", "texto-suave", "Prueba con:"));
            HTMLElement chips = Dom.el("div", "chips");
            for (String s : SUGERENCIAS) {
                chips.appendChild(Dom.boton(s, "chip", () -> {
                    campo.setValue(s);
                    buscar();
                }));
            }
            resultados.appendChild(chips);
            return;
        }

        // La misma búsqueda de la app de escritorio: por palabras y sin importar tildes
        Map<String, Set<String>> encontrados = new TreeMap<>(app.controlador.solicitarBusquedaLugar(termino));
        if (encontrados.isEmpty()) {
            resultados.appendChild(Dom.texto("p", "vacio", "No se encontraron lugares para \"" + termino + "\"."));
            return;
        }
        resultados.appendChild(Dom.texto("p", "texto-suave", encontrados.size()
                + (encontrados.size() == 1 ? " lugar encontrado" : " lugares encontrados")));

        for (Map.Entry<String, Set<String>> e : encontrados.entrySet()) {
            List<String> edificios = new ArrayList<>(e.getValue());
            java.util.Collections.sort(edificios);
            for (String id : edificios) {
                HTMLElement item = Dom.el("div", "tarjeta lugar");
                HTMLElement info = Dom.el("div", "lugar-info");
                info.appendChild(Dom.texto("strong", null, e.getKey()));
                info.appendChild(Dom.texto("span", "texto-suave",
                        categoria(id, e.getKey()) + " · " + MapaCampus.nombre(app.controlador, id)));
                item.appendChild(info);
                HTMLElement acciones = Dom.el("div", "lugar-acciones");
                acciones.appendChild(Dom.boton("Ir aquí", "btn btn-primario btn-chico", () -> app.usarComo(id, false)));
                acciones.appendChild(Dom.boton("Salir de aquí", "btn btn-contorno btn-chico", () -> app.usarComo(id, true)));
                item.appendChild(acciones);
                resultados.appendChild(item);
            }
        }
    }

    private String categoria(String idEdificio, String nombreLugar) {
        Edificio ed = app.controlador.getGrafo().getEdificios().get(idEdificio);
        if (ed != null) {
            for (Lugar l : ed.getLugares()) {
                if (l.getNombre().equals(nombreLugar)) return l.getCategoria();
            }
        }
        return "Lugar";
    }
}
