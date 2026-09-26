package persistencia;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Lectura y escritura de archivos de datos en un formato de texto propio,
 * sin librerías externas.
 *
 * Cada línea tiene la forma:   CLAVE|campo1|campo2|...
 * Las líneas vacías y las que empiezan por '#' se tratan como comentarios.
 *
 * Los archivos se guardan en la carpeta "datos" junto al programa; si no
 * existe, se crea automáticamente la primera vez que se guarda algo.
 */
public class ArchivoDatos {

    public static final String CARPETA = "datos";
    private static final String SEPARADOR = "|";

    private final File archivo;

    public ArchivoDatos(String nombreArchivo) {
        File carpeta = new File(CARPETA);
        this.archivo = new File(carpeta, nombreArchivo);
    }

    public boolean existe() {
        return archivo.exists();
    }

    public String getRuta() {
        return archivo.getPath();
    }

    /** Lee todas las líneas útiles, ya separadas en campos. */
    public List<String[]> leer() {
        List<String[]> registros = new ArrayList<>();
        if (!archivo.exists()) {
            return registros;
        }
        try (BufferedReader lector = new BufferedReader(
                new InputStreamReader(new FileInputStream(archivo), StandardCharsets.UTF_8))) {

            String linea;
            while ((linea = lector.readLine()) != null) {
                linea = linea.trim();
                if (linea.isEmpty() || linea.startsWith("#")) continue;
                registros.add(linea.split("\\" + SEPARADOR, -1));
            }
        } catch (IOException e) {
            System.err.println("No se pudo leer " + archivo.getPath() + ": " + e.getMessage());
        }
        return registros;
    }

    /**
     * Escribe todas las líneas, reemplazando el contenido anterior.
     * Devuelve true si la operación fue exitosa.
     */
    public boolean escribir(List<String[]> registros, String encabezado) {
        File carpeta = archivo.getParentFile();
        if (carpeta != null && !carpeta.exists() && !carpeta.mkdirs()) {
            System.err.println("No se pudo crear la carpeta de datos: " + carpeta.getPath());
            return false;
        }

        try (BufferedWriter escritor = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(archivo), StandardCharsets.UTF_8))) {

            if (encabezado != null) {
                for (String linea : encabezado.split("\n")) {
                    escritor.write("# " + linea);
                    escritor.newLine();
                }
                escritor.newLine();
            }
            for (String[] campos : registros) {
                escritor.write(unir(campos));
                escritor.newLine();
            }
            return true;
        } catch (IOException e) {
            System.err.println("No se pudo guardar " + archivo.getPath() + ": " + e.getMessage());
            return false;
        }
    }

    /** Une los campos con el separador, limpiando caracteres que lo romperían. */
    private String unir(String[] campos) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < campos.length; i++) {
            if (i > 0) sb.append(SEPARADOR);
            sb.append(limpiar(campos[i]));
        }
        return sb.toString();
    }

    /** Evita que un campo contenga el separador o saltos de línea. */
    public static String limpiar(String valor) {
        if (valor == null) return "";
        return valor.replace(SEPARADOR, "/").replace("\n", " ").replace("\r", " ").trim();
    }
}
