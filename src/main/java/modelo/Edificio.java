package modelo;

import java.util.ArrayList;
import java.util.List;

/**
 * Un edificio del campus (o punto de referencia como una portería, el templo
 * o la cafetería): un vértice del grafo, con su identificador, su nombre
 * completo y los lugares o dependencias que contiene.
 */
public class Edificio {
    private final String id;
    private final String nombre;
    private final List<Lugar> lugares;

    public Edificio(String id, String nombre) {
        this.id = id;
        this.nombre = nombre;
        this.lugares = new ArrayList<>();
    }

    public String getId() { return id; }
    public String getNombre() { return nombre; }
    public List<Lugar> getLugares() { return lugares; }

    public void agregarLugar(Lugar lugar) {
        this.lugares.add(lugar);
    }
}
