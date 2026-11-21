package Consola;

import javax.swing.SwingUtilities;

public class Main {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            VentanaConsola ventana = new VentanaConsola();
            ventana.setVisible(true);
            ventana.enfocarEntrada();
        });
    }
}
