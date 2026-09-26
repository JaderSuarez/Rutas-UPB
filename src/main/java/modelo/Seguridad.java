package modelo;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Cifrado de contraseñas mediante SHA-256, disponible en el propio JDK
 * (java.security), sin necesidad de librerías externas.
 *
 * Las contraseñas no se guardan nunca en texto plano: se almacena su resumen
 * (hash) y, al iniciar sesión, se compara el resumen de lo que escribe el
 * usuario con el que está guardado.
 */
public class Seguridad {

    private static final String PREFIJO = "sha256:";

    /** Devuelve el resumen de la contraseña, listo para guardarse. */
    public static String cifrar(String contrasena) {
        if (contrasena == null) contrasena = "";
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] resumen = md.digest(contrasena.getBytes(StandardCharsets.UTF_8));

            StringBuilder sb = new StringBuilder(PREFIJO);
            for (byte b : resumen) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 forma parte de la plataforma Java, así que no debería ocurrir.
            throw new IllegalStateException("El algoritmo SHA-256 no está disponible.", e);
        }
    }

    /** true si el valor guardado ya está cifrado con este esquema. */
    public static boolean estaCifrada(String valorGuardado) {
        return valorGuardado != null && valorGuardado.startsWith(PREFIJO);
    }

    /**
     * Comprueba una contraseña contra el valor almacenado.
     *
     * Si el valor guardado todavía estuviera en texto plano (por ejemplo, de
     * una versión anterior del sistema), se compara directamente para no dejar
     * fuera a esa cuenta; al iniciar sesión correctamente, el repositorio la
     * vuelve a guardar ya cifrada.
     */
    public static boolean coincide(String contrasenaIngresada, String valorGuardado) {
        if (valorGuardado == null) return false;
        if (estaCifrada(valorGuardado)) {
            return cifrar(contrasenaIngresada).equals(valorGuardado);
        }
        return valorGuardado.equals(contrasenaIngresada);
    }
}
