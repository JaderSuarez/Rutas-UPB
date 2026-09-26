package modelo;

public class Camino {
    private final String origenId;
    private final String destinoId;
    private final double distancia;
    private final boolean tieneEscaleras;
    private boolean bloqueado;

    public Camino(String origenId, String destinoId, double distancia, boolean tieneEscaleras) {
        this.origenId = origenId;
        this.destinoId = destinoId;
        this.distancia = distancia;
        this.tieneEscaleras = tieneEscaleras;
        this.bloqueado = false;
    }

    public String getOrigenId() { return origenId; }
    public String getDestinoId() { return destinoId; }
    public double getDistancia() { return distancia; }
    public boolean isTieneEscaleras() { return tieneEscaleras; }
    public boolean isBloqueado() { return bloqueado; }
    public void setBloqueado(boolean bloqueado) { this.bloqueado = bloqueado; }
}