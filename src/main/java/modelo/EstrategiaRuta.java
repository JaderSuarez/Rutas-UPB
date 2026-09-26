package modelo;

import excepcion.RutaNoEncontradaException;
import modelo.ServicioRutas.ResultadoRuta;

public interface EstrategiaRuta {
    ResultadoRuta calcularRuta(GrafoCampus grafo, String idOrigen, String idDestino, boolean evitarEscaleras)
            throws RutaNoEncontradaException;
}