package excepcion;

public class RutaNoEncontradaException extends Exception {
    public RutaNoEncontradaException(String mensaje) {
        super(mensaje);
    }
}