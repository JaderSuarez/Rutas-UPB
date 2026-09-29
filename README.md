# Rutas UPB 🗺️

Aplicación de escritorio en **Java (Swing)** para encontrar la mejor ruta entre los lugares del campus de la UPB. Calcula caminos con el **algoritmo de Dijkstra**, estima el tiempo de recorrido, permite buscar lugares y tiene un panel de administración para bloquear caminos.

[![Compilar y publicar](https://github.com/JaderSuarez/Rutas-UPB/actions/workflows/build.yml/badge.svg)](https://github.com/JaderSuarez/Rutas-UPB/actions/workflows/build.yml)
[![Probar en el navegador](https://github.com/codespaces/badge.svg)](https://codespaces.new/JaderSuarez/Rutas-UPB)

---

## 📷 Escanea y pruébalo

| 📱 Abrir la app (celular o PC) | ⬇️ Descargar el JAR (PC) | 💻 Ver el código |
|:---:|:---:|:---:|
| <img src="docs/qr-version-web.png" width="200" alt="QR versión web"> | <img src="docs/qr-descarga-jar.png" width="200" alt="QR descarga del JAR"> | <img src="docs/qr-rutas-upb.png" width="200" alt="QR repositorio"> |
| [jadersuarez.github.io/Rutas-UPB](https://jadersuarez.github.io/Rutas-UPB/) | [RutasUPB.jar](https://github.com/JaderSuarez/Rutas-UPB/releases/latest/download/RutasUPB.jar) | [github.com/JaderSuarez/Rutas-UPB](https://github.com/JaderSuarez/Rutas-UPB) |

---

## ▶️ Probarlo ahora

### 📱 En el celular: versión móvil
Escanea el QR o abre **https://jadersuarez.github.io/Rutas-UPB/** desde el celular: se abre automáticamente la **versión para celular** (también en [/movil/](https://jadersuarez.github.io/Rutas-UPB/movil/)). Carga en segundos y está pensada para pantallas táctiles:

- **Ruta:** origen, destino y opción *evitar escaleras*; distancia, tiempo y recorrido paso a paso.
- **Buscar:** lugares del campus (cafeterías, oficinas, laboratorios…) sin importar tildes.
- **Mapa:** igual que en PC, con **Vista Mapa** (la ilustración) y **Vista Grafo** (edificios, caminos con y sin escaleras, bloqueos, distancias y convenciones). Arrastra para moverte, pellizca para hacer zoom y toca un edificio o un camino para ver su información.
- **Iniciar sesión / registro** y **panel de administración** (bloquear caminos, agregar puntos, velocidades, historial).

Está escrita en **Java** (carpeta `movil/`) y se traduce a JavaScript con [TeaVM](https://teavm.org/). Reutiliza la misma lógica de la app de escritorio: `CampusControlador`, `GrafoCampus`, Dijkstra, `EstimadorTiempo`, `BuscadorLugares` y los repositorios. Los datos (usuarios, bloqueos, puntos nuevos) se guardan en el navegador de cada celular.

### 💻 En el navegador de un PC
En un computador, el mismo enlace abre la **aplicación de escritorio completa** (Swing) dentro del navegador gracias a [CheerpJ](https://cheerpj.com/). La primera carga tarda unos segundos. Desde un celular se puede forzar con [`?escritorio`](https://jadersuarez.github.io/Rutas-UPB/?escritorio).

### Opción 1: descargar y ejecutar en un PC
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

movil/src/main/java/movil   → versión para celular (Java → JavaScript con TeaVM)
├── AppMovil          → punto de entrada y navegación por pestañas
├── PantallaAcceso    → inicio de sesión, registro y acceso administrador
├── PantallaRuta / PantallaBuscar / PantallaMapa / PantallaAdmin
├── LienzoTactil     → canvas con arrastre, pellizco y toque (base de las dos vistas)
├── MapaCampus        → Vista Mapa: ilustración con pines y ruta
├── VistaGrafo        → Vista Grafo: la misma de vista.PanelMapa, adaptada al celular
└── PersistenciaWeb   → guarda los archivos de datos en el navegador (localStorage)
```

Conceptos aplicados:
- **Grafos y Dijkstra** (`GrafoCampus`, `EstrategiaDijkstra`)
- **Patrón Strategy** para el cálculo de rutas (`EstrategiaRuta`)
- **Patrón MVC**
- **Persistencia en archivos** sin librerías externas
- **Contraseñas cifradas** (`Seguridad`)
- **Pruebas unitarias con JUnit 5** (pruebas en `src/test/java` y `movil/src/test/java`)

## ✅ Pruebas
```bash
mvn test                     # app de escritorio y lógica compartida
mvn -f movil/pom.xml package # compila la versión para celular y prueba que el mapa coincide con el de escritorio
```
Cada vez que se sube código, GitHub Actions compila el proyecto y ejecuta las pruebas automáticamente.

## 🚀 Publicar una nueva versión (para el autor)
Crea una Release con un tag que empiece por `v` (por ejemplo `v1.0`) desde **Releases → Draft a new release**. GitHub Actions compilará el proyecto y adjuntará `RutasUPB.jar` automáticamente.

---

Tipografía [Inter](https://rsms.me/inter/) incluida bajo licencia SIL Open Font License (ver `src/main/resources/fuentes/Inter-LICENSE.txt`).
