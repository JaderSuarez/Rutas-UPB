package modelo;

/** Cuenta de acceso al sistema. */
public class Usuario {

    /** Perfiles disponibles en el sistema. */
    public enum Rol {
        ESTUDIANTE("Estudiante"),
        EMPLEADO("Empleado"),
        ADMINISTRADOR("Administrador");

        private final String etiqueta;

        Rol(String etiqueta) { this.etiqueta = etiqueta; }

        public String getEtiqueta() { return etiqueta; }

        public static Rol desdeTexto(String texto) {
            if (texto == null) return ESTUDIANTE;
            for (Rol r : values()) {
                if (r.name().equalsIgnoreCase(texto.trim())
                        || r.etiqueta.equalsIgnoreCase(texto.trim())) {
                    return r;
                }
            }
            return ESTUDIANTE;
        }
    }

    private final String nombre;
    private final String correo;
    private String contrasena;
    private final Rol rol;

    public Usuario(String nombre, String correo, String contrasena, Rol rol) {
        this.nombre = nombre;
        this.correo = correo;
        this.contrasena = contrasena;
        this.rol = rol;
    }

    public String getNombre() { return nombre; }
    public String getCorreo() { return correo; }
    public String getContrasena() { return contrasena; }
    public Rol getRol() { return rol; }

    public void setContrasena(String contrasena) { this.contrasena = contrasena; }

    public boolean esAdministrador() { return rol == Rol.ADMINISTRADOR; }

    @Override
    public String toString() {
        return nombre + " (" + rol.getEtiqueta() + ")";
    }
}
