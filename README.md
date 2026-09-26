# Rutas UPB 🗺️

Aplicación de escritorio en **Java (Swing)** para encontrar la mejor ruta entre los lugares del campus de la UPB. Calcula caminos con el **algoritmo de Dijkstra**, estima el tiempo de recorrido, permite buscar lugares y tiene un panel de administración para bloquear caminos.

[![Compilar y publicar](https://github.com/JaderSuarez/Rutas-UPB/actions/workflows/build.yml/badge.svg)](https://github.com/JaderSuarez/Rutas-UPB/actions/workflows/build.yml)
[![Probar en el navegador](https://github.com/codespaces/badge.svg)](https://codespaces.new/JaderSuarez/Rutas-UPB)

---

## ▶️ Probarlo ahora

### Opción 1: descargar y ejecutar (recomendada)
1. Instala **Java 17 o superior** ([Adoptium](https://adoptium.net/)).
2. Descarga **`RutasUPB.jar`** desde la sección [**Releases**](https://github.com/JaderSuarez/Rutas-UPB/releases/latest).
3. Haz doble clic en el archivo, o ejecuta:
   ```bash
   java -jar RutasUPB.jar
   ```

### Opción 2: en el navegador, sin instalar nada (GitHub Codespaces)
1. Pulsa el botón **"Open in GitHub Codespaces"** de arriba (requiere cuenta de GitHub).
2. Espera a que el entorno se construya (unos minutos la primera vez).
3. En la pestaña **Ports**, abre el puerto **6080** (Escritorio) → contraseña **`vscode`**.
4. La aplicación se abre sola en ese escritorio. Si no aparece, en la terminal ejecuta:
   ```bash
   DISPLAY=:1 java -jar target/RutasUPB.jar
   ```

### Opción 3: desde el código fuente
```bash
git clone https://github.com/JaderSuarez/Rutas-UPB.git
cd Rutas-UPB
mvn package          # compila y ejecuta las pruebas
java -jar target/RutasUPB.jar
```
También se puede abrir directamente en **NetBeans**, **IntelliJ** o **VS Code** como proyecto Maven.

### Usuario de prueba
| Rol | Correo | Contraseña |
|---|---|---|
| Administrador | `admin@upb.edu.co` | `admin123` |

Los estudiantes pueden crear su propia cuenta desde la pantalla de registro.

---

## 🧱 Arquitectura (MVC)

```
src/main/java
├── com/mycompany/rutasupb/Main.java   → punto de entrada
├── modelo/        → grafo del campus, Dijkstra, estimador de tiempo, búsqueda, usuarios
├── vista/         → ventanas y paneles Swing (login, mapa, resultados, administración)
├── controlador/   → CampusControlador: conecta la vista con el modelo
├── repositorio/   → datos del campus (edificios, lugares, caminos)
├── persistencia/  → guardado en archivos de texto (carpeta datos/)
└── excepcion/     → RutaNoEncontradaException
```

Conceptos aplicados:
- **Grafos y Dijkstra** (`GrafoCampus`, `EstrategiaDijkstra`)
- **Patrón Strategy** para el cálculo de rutas (`EstrategiaRuta`)
- **Patrón MVC**
- **Persistencia en archivos** sin librerías externas
- **Contraseñas cifradas** (`Seguridad`)
- **Pruebas unitarias con JUnit 5** (27 pruebas en `src/test/java`)

## ✅ Pruebas
```bash
mvn test
```
Cada vez que se sube código, GitHub Actions compila el proyecto y ejecuta las pruebas automáticamente.

## 🚀 Publicar una nueva versión (para el autor)
Crea una Release con un tag que empiece por `v` (por ejemplo `v1.0`) desde **Releases → Draft a new release**. GitHub Actions compilará el proyecto y adjuntará `RutasUPB.jar` automáticamente.

---

Tipografía [Inter](https://rsms.me/inter/) incluida bajo licencia SIL Open Font License (ver `src/main/resources/fuentes/Inter-LICENSE.txt`).
