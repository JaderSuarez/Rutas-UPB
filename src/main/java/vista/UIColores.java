package vista;

import java.awt.Color;

/**
 * Paleta de colores centralizada del Sistema de Rutas Óptimas UPB.
 * Todas las clases de vista deben tomar sus colores de aquí en vez de
 * declarar valores sueltos con "new Color(...)", para que un cambio de
 * paleta se haga en un solo lugar.
 */
public class UIColores {

    // ---------- Colores institucionales ----------
    public static final Color PRIMARIO = new Color(0x80, 0x00, 0x20);        // Vinotinto UPB
    public static final Color PRIMARIO_OSCURO = new Color(0x5A, 0x00, 0x16); // Vinotinto (hover/oscuro)
    public static final Color ACENTO = new Color(0xD4, 0xAF, 0x37);          // Dorado

    // ---------- Estados ----------
    public static final Color EXITO = new Color(0x10, 0xB9, 0x81);           // Verde - disponible
    public static final Color EXITO_FONDO = new Color(0xD1, 0xFA, 0xE5);
    public static final Color ERROR = new Color(0xEF, 0x44, 0x44);           // Rojo - bloqueado / error
    public static final Color ERROR_FONDO = new Color(0xFE, 0xE2, 0xE2);

    // ---------- Fondo y superficies ----------
    public static final Color FONDO = new Color(0xF8, 0xFA, 0xFC);
    public static final Color TARJETA = Color.WHITE;
    public static final Color BORDE = new Color(0xE2, 0xE8, 0xF0);

    // ---------- Texto ----------
    public static final Color TEXTO_CLARO = Color.WHITE;
    public static final Color TEXTO_OSCURO = new Color(0x1E, 0x29, 0x3B);
    public static final Color TEXTO_MUTED = new Color(0x64, 0x74, 0x8B);

    // ---------- Mapa del grafo (PanelMapa - Vista Grafo) ----------
    public static final Color MAPA_FONDO = new Color(245, 247, 250);
    public static final Color CAMINO_ACCESIBLE = new Color(218, 41, 122);    // Fucsia - sin escaleras
    public static final Color CAMINO_ESCALERAS = Color.BLACK;               // Con escaleras
    public static final Color CAMINO_BLOQUEADO = Color.LIGHT_GRAY;          // Bloqueado (punteado)
    public static final Color RUTA_RESALTADA = new Color(0xFF, 0x8C, 0x00);      // Ruta calculada (naranjado)
    public static final Color EDIFICIO_EN_RUTA = new Color(0xD3, 0x1A, 0x1A);        // Edificio que pertenece a la ruta (rojo fuerte)
    public static final Color EDIFICIO_NORMAL = new Color(30, 40, 60);          // Edificio fuera de la ruta
    public static final Color EDIFICIO_BORDE = Color.WHITE;
    public static final Color EDIFICIO_TEXTO = Color.BLACK;
}
