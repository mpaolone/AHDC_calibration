package org.clas.modules;

import org.clas.modules.gui.*;
import javax.swing.SwingUtilities;

public class AppLauncher {
    public static void main(String[] args) {
        // Launch the main GUI on the Event Dispatch Thread.
        SwingUtilities.invokeLater(() -> {
            new ALERTDCCalibFrame();
        });
    }
}
