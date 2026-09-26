package com.mycompany.rutasupb;

import controlador.CampusControlador;
import vista.VentanaLogin;
import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {
        vista.EstiloUPB.instalarFuenteGlobal(); // tipografía Inter en toda la interfaz
        SwingUtilities.invokeLater(() -> {
            CampusControlador controlador = new CampusControlador();
            VentanaLogin login = new VentanaLogin(controlador);
            login.setVisible(true);
        });
    }
}