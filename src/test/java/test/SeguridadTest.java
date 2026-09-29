package test;

import modelo.BuscadorLugares;
import modelo.Seguridad;
import modelo.Sha256;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.Normalizer;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Seguridad y BuscadorLugares ya no dependen de MessageDigest ni de Normalizer
 * (no existen en la versión para celular). Estas pruebas comprueban que el
 * resultado es exactamente el mismo que con las clases del JDK.
 */
class SeguridadTest {

    @Test
    void sha256CoincideConElDelJdk() throws Exception {
        MessageDigest jdk = MessageDigest.getInstance("SHA-256");
        Random azar = new Random(42);
        // Incluye los tamaños límite del relleno (55, 56, 63, 64 bytes) y mensajes largos
        for (int largo = 0; largo <= 300; largo++) {
            byte[] datos = new byte[largo];
            azar.nextBytes(datos);
            assertArrayEquals(jdk.digest(datos), Sha256.resumen(datos), "largo " + largo);
        }
    }

    @Test
    void cifrarDaElMismoTextoQueAntes() throws Exception {
        for (String clave : new String[]{"admin123", "", "contraseña", "Ñandú-2026"}) {
            byte[] r = MessageDigest.getInstance("SHA-256").digest(clave.getBytes(StandardCharsets.UTF_8));
            StringBuilder esperado = new StringBuilder("sha256:");
            for (byte b : r) esperado.append(String.format("%02x", b));
            assertEquals(esperado.toString(), Seguridad.cifrar(clave));
        }
        assertTrue(Seguridad.coincide("admin123", Seguridad.cifrar("admin123")));
        assertFalse(Seguridad.coincide("otra", Seguridad.cifrar("admin123")));
    }

    @Test
    void normalizarQuitaTildesIgualQueNormalizer() {
        String[] textos = {"Cafetería", "Cámara Gesell", "Pádel", "Portería Ñ", "Económía ÜÖ", "Café Ç",
                "Música", "Diseño", "ÁÉÍÓÚ àèìòù âêîôû äëïöü ãõ å ý ÿ"};
        for (String t : textos) {
            String jdk = Normalizer.normalize(t, Normalizer.Form.NFD)
                    .replaceAll("\\p{InCombiningDiacriticalMarks}+", "").toLowerCase().trim();
            assertEquals(jdk, BuscadorLugares.normalizar(t), t);
        }
    }
}
