package modelo;

public class Lugar {
    private final String id;
    private final String nombre;
    private final String categoria;

    public Lugar(String id, String nombre, String categoria) {
        this.id = id;
        this.nombre = nombre;
        this.categoria = categoria;
    }

    public String getId() { return id; }
    public String getNombre() { return nombre; }
    public String getCategoria() { return categoria; }

    @Override
    public String toString() {
        return nombre + " (" + categoria + ")";
    }
}