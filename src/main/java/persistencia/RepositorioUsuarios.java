package persistencia;

import modelo.Seguridad;
import modelo.Usuario;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Guarda y recupera las cuentas de usuario del archivo datos/usuarios.txt.
 *
 * Formato de cada línea:  USUARIO|nombre|correo|contraseña|rol
 */
public class RepositorioUsuarios {

    private static final String ARCHIVO = "usuarios.txt";
    private static final String CLAVE = "USUARIO";

    private final ArchivoDatos archivo = new ArchivoDatos(ARCHIVO);
    private final Map<String, Usuario> usuarios = new LinkedHashMap<>();

    public RepositorioUsuarios() {
        cargar();
    }

    private void cargar() {
        usuarios.clear();

        for (String[] campos : archivo.leer()) {
            if (campos.length >= 5 && CLAVE.equals(campos[0])) {
                Usuario u = new Usuario(campos[1], campos[2], campos[3],
                        Usuario.Rol.desdeTexto(campos[4]));
                usuarios.put(clave(u.getCorreo()), u);
            }
        }

        // Si no hay archivo previo (primera ejecución), se crea la cuenta de
        // administrador por defecto para no dejar el sistema sin acceso.
        if (usuarios.isEmpty()) {
            Usuario admin = new Usuario("Administrador", "admin@upb.edu.co",
                    Seguridad.cifrar("admin123"), Usuario.Rol.ADMINISTRADOR);
            usuarios.put(clave(admin.getCorreo()), admin);
            guardar();
        }
    }

    private String clave(String correo) {
        return correo == null ? "" : correo.trim().toLowerCase();
    }

    public boolean guardar() {
        List<String[]> registros = new ArrayList<>();
        for (Usuario u : usuarios.values()) {
            registros.add(new String[]{CLAVE, u.getNombre(), u.getCorreo(),
                    u.getContrasena(), u.getRol().name()});
        }
        return archivo.escribir(registros,
                "Cuentas de usuario del Sistema de Rutas Óptimas UPB\n"
              + "Formato: USUARIO|nombre|correo|contraseña cifrada (SHA-256)|rol\n"
              + "Roles disponibles: ESTUDIANTE, EMPLEADO, ADMINISTRADOR");
    }

    public boolean existeCorreo(String correo) {
        return usuarios.containsKey(clave(correo));
    }

    /** Registra una cuenta nueva. Devuelve false si el correo ya estaba en uso. */
    public boolean registrar(Usuario usuario) {
        if (usuario == null || existeCorreo(usuario.getCorreo())) {
            return false;
        }
        // La contraseña se guarda siempre cifrada, nunca en texto plano
        if (!Seguridad.estaCifrada(usuario.getContrasena())) {
            usuario.setContrasena(Seguridad.cifrar(usuario.getContrasena()));
        }
        usuarios.put(clave(usuario.getCorreo()), usuario);
        return guardar();
    }

    /** Devuelve el usuario si el correo y la contraseña coinciden; si no, null. */
    public Usuario autenticar(String correo, String contrasena) {
        Usuario u = usuarios.get(clave(correo));
        if (u == null || !Seguridad.coincide(contrasena, u.getContrasena())) {
            return null;
        }
        // Si la cuenta venía de una versión anterior sin cifrar, se actualiza
        // al esquema seguro aprovechando que aquí se conoce la contraseña.
        if (!Seguridad.estaCifrada(u.getContrasena())) {
            u.setContrasena(Seguridad.cifrar(contrasena));
            guardar();
        }
        return u;
    }

    public Usuario buscarPorCorreo(String correo) {
        return usuarios.get(clave(correo));
    }

    /** Cambia la contraseña de una cuenta existente y la guarda en disco. */
    public boolean cambiarContrasena(String correo, String actual, String nueva) {
        Usuario u = autenticar(correo, actual);
        if (u == null) return false;
        u.setContrasena(Seguridad.cifrar(nueva));
        return guardar();
    }

    public List<Usuario> listar() {
        return new ArrayList<>(usuarios.values());
    }

    public int cantidad() {
        return usuarios.size();
    }

    public String getRutaArchivo() {
        return archivo.getRuta();
    }
}
