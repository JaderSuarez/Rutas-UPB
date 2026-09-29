package movil;

import org.teavm.jso.browser.Storage;
import persistencia.ArchivoDatos;

import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * La app de escritorio guarda usuarios y estado en archivos de texto
 * (datos/usuarios.txt y datos/estado.txt) mediante ArchivoDatos. En el navegador
 * esos archivos viven solo en memoria, así que aquí se copian al almacenamiento
 * local del navegador (localStorage) para que no se pierdan al cerrar la página.
 *
 * Así la app móvil reutiliza sin cambios los repositorios y el controlador de
 * escritorio: solo se agrega este paso de restaurar al abrir y guardar tras cada cambio.
 */
final class PersistenciaWeb {

    private static final String[] ARCHIVOS = {"usuarios.txt", "estado.txt"};
    private static final String PREFIJO = "rutasupb.movil/";

    private PersistenciaWeb() { }

    private static Storage almacen() {
        return Storage.getLocalStorage();
    }

    /** Antes de crear el controlador: devuelve a los archivos lo guardado en el navegador. */
    static void restaurar() {
        Storage s = almacen();
        if (s == null) return;
        for (String nombre : ARCHIVOS) {
            String contenido = s.getItem(PREFIJO + nombre);
            if (contenido == null) continue;
            File archivo = new File(new File(ArchivoDatos.CARPETA), nombre);
            archivo.getParentFile().mkdirs();
            try (Writer w = new OutputStreamWriter(new FileOutputStream(archivo), StandardCharsets.UTF_8)) {
                w.write(contenido);
            } catch (IOException e) {
                System.err.println("No se pudo restaurar " + nombre + ": " + e.getMessage());
            }
        }
    }

    /** Después de cada cambio: copia los archivos al almacenamiento del navegador. */
    static void guardar() {
        Storage s = almacen();
        if (s == null) return;
        for (String nombre : ARCHIVOS) {
            File archivo = new File(new File(ArchivoDatos.CARPETA), nombre);
            if (!archivo.exists()) continue;
            StringBuilder sb = new StringBuilder();
            try (Reader r = new InputStreamReader(new FileInputStream(archivo), StandardCharsets.UTF_8)) {
                char[] buffer = new char[4096];
                int leidos;
                while ((leidos = r.read(buffer)) > 0) sb.append(buffer, 0, leidos);
                s.setItem(PREFIJO + nombre, sb.toString());
            } catch (IOException e) {
                System.err.println("No se pudo guardar " + nombre + ": " + e.getMessage());
            }
        }
    }
}
