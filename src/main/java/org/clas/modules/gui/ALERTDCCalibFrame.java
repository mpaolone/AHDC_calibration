/*
 * This code was developed by Hamza Atac for the ALERT COLLOBORATION
 * to perform calibrations for the DC detector.
 *
 */
package org.clas.modules.gui;

import org.clas.modules.calibration.*;

import javax.swing.*;

public class ALERTDCCalibFrame extends JFrame {

    public ALERTDCCalibFrame() {
        super("ALERT DC Calibration");
        setSize(1200, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Create the main tabbed pane.
        JTabbedPane tabbedPane = new JTabbedPane();

        // Create the T0 panel (from calibration code).
        JTabbedPane t0Panel = T0Calibration.createT0Panel();
        tabbedPane.addTab("T0", t0Panel);

        // Create the T2D panel.
        JPanel t2dPanel = T2DCalibration.createT2DPanel();
        tabbedPane.addTab("T2D", t2dPanel);

        // Create a panel for residuals (if separated from T2D).
        JPanel residualsPanel = T2DCalibration.createResidualsPanel();
        tabbedPane.addTab("Residuals", residualsPanel);

        add(tabbedPane);
        setVisible(true);
    }
}
