package modelo;

import java.nio.charset.StandardCharsets;

/**
 * Cifrado de contraseñas mediante SHA-256 (clase Sha256, escrita en Java puro),
 * sin necesidad de librerías externas. Da el mismo resultado que el SHA-256 del
 * JDK (java.security) y además funciona en la versión para celular.
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
        byte[] resumen = Sha256.resumen(contrasena.getBytes(StandardCharsets.UTF_8));

        StringBuilder sb = new StringBuilder(PREFIJO);
        for (byte b : resumen) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
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
