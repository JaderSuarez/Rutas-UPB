package movil;

import java.util.HashMap;
import java.util.Map;

/**
 * Posición de cada edificio en la vista de grafo, sobre el lienzo de referencia de
 * 836 x 720. Son los mismos valores de vista.PanelMapa (la app de escritorio);
 * CoordenadasMapaTest verifica que sigan siendo iguales.
 */
final class CoordenadasGrafo {

    static final int ANCHO = 836;
    static final int ALTO = 720;
    static final int MARGEN = 46;

    /** id del edificio -> {x, y} en el lienzo del grafo. */
    static final Map<String, int[]> POSICIONES = new HashMap<>();

    static {
        POSICIONES.put("M", new int[]{70, 304});
        POSICIONES.put("Porteria 2", new int[]{228, 145});
        POSICIONES.put("K", new int[]{479, 94});
        POSICIONES.put("L", new int[]{674, 57});
        POSICIONES.put("I", new int[]{602, 129});
        POSICIONES.put("H", new int[]{725, 231});
        POSICIONES.put("E", new int[]{665, 266});
        POSICIONES.put("F", new int[]{725, 294});
        POSICIONES.put("G", new int[]{766, 354});
        POSICIONES.put("D", new int[]{541, 293});
        POSICIONES.put("CAF", new int[]{412, 370});
        POSICIONES.put("A", new int[]{310, 473});
        POSICIONES.put("B", new int[]{428, 498});
        POSICIONES.put("Templo", new int[]{208, 554});
        POSICIONES.put("C", new int[]{546, 473});
        POSICIONES.put("J", new int[]{684, 473});
        POSICIONES.put("Porteria 1", new int[]{619, 626});
    }

    private CoordenadasGrafo() { }

    /**
     * Convierte una posición del mapa ilustrado a la vista de grafo. Se usa para los
     * puntos agregados desde el celular, que solo se ubican sobre el mapa.
     *
     * Primero un ajuste afín por mínimos cuadrados (grafo = a·x + b·y + c) sobre los
     * edificios que están en ambas vistas; luego se corrige con el error de los
     * edificios cercanos (pesado por la distancia), de modo que un punto ubicado
     * junto a un edificio en el mapa quede también junto a él en el grafo.
     */
    static int[] desdeMapa(int xMapa, int yMapa) {
        java.util.List<double[]> mapa = new java.util.ArrayList<>(), grafo = new java.util.ArrayList<>();
        for (Map.Entry<String, int[]> e : POSICIONES.entrySet()) {
            int[] m = CoordenadasMapa.POSICIONES.get(e.getKey());
            if (m == null) continue;
            mapa.add(new double[]{m[0], m[1]});
            grafo.add(new double[]{e.getValue()[0], e.getValue()[1]});
        }
        double[] cx = afin(mapa, grafo, 0), cy = afin(mapa, grafo, 1);
        double x = cx[0] * xMapa + cx[1] * yMapa + cx[2];
        double y = cy[0] * xMapa + cy[1] * yMapa + cy[2];

        double sumaPeso = 0, corrX = 0, corrY = 0;
        for (int i = 0; i < mapa.size(); i++) {
            double[] m = mapa.get(i), g = grafo.get(i);
            double d2 = (m[0] - xMapa) * (m[0] - xMapa) + (m[1] - yMapa) * (m[1] - yMapa);
            if (d2 < 1e-9) return new int[]{(int) g[0], (int) g[1]};
            double peso = 1 / (d2 * d2);   // los edificios cercanos pesan mucho más
            corrX += peso * (g[0] - (cx[0] * m[0] + cx[1] * m[1] + cx[2]));
            corrY += peso * (g[1] - (cy[0] * m[0] + cy[1] * m[1] + cy[2]));
            sumaPeso += peso;
        }
        x += corrX / sumaPeso;
        y += corrY / sumaPeso;
        return new int[]{(int) Math.round(Math.max(0, Math.min(ANCHO, x))),
                         (int) Math.round(Math.max(0, Math.min(ALTO, y)))};
    }

    /** Coeficientes {a, b, c} de grafo[eje] = a·x + b·y + c (ecuaciones normales 3x3). */
    private static double[] afin(java.util.List<double[]> mapa, java.util.List<double[]> grafo, int eje) {
        double[][] m = new double[3][4];
        for (int i = 0; i < mapa.size(); i++) {
            double[] f = {mapa.get(i)[0], mapa.get(i)[1], 1};
            for (int r = 0; r < 3; r++) {
                for (int c = 0; c < 3; c++) m[r][c] += f[r] * f[c];
                m[r][3] += f[r] * grafo.get(i)[eje];
            }
        }
        for (int col = 0; col < 3; col++) {   // eliminación de Gauss con pivoteo
            int piv = col;
            for (int r = col + 1; r < 3; r++) if (Math.abs(m[r][col]) > Math.abs(m[piv][col])) piv = r;
            double[] t = m[col]; m[col] = m[piv]; m[piv] = t;
            for (int r = 0; r < 3; r++) {
                if (r == col) continue;
                double factor = m[r][col] / m[col][col];
                for (int c = col; c < 4; c++) m[r][c] -= factor * m[col][c];
            }
        }
        return new double[]{m[0][3] / m[0][0], m[1][3] / m[1][1], m[2][3] / m[2][2]};
    }
}
