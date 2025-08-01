/*
 * This code was developed by Hamza Atac for the ALERT COLLOBORATION
 * to perform calibrations for the DC detector.
 *
 */
package org.clas.modules.gui;

import org.clas.modules.calibration.*;
import org.clas.modules.config.Config;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

public class ALERTDCCalibFrame extends JFrame {



    public ALERTDCCalibFrame() {
        super("ALERT DC Calibration");
        setSize(1200, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Create the main tabbed pane.
        JTabbedPane tabbedPane = new JTabbedPane();


        JButton openButton = new JButton("Load File");
        JPanel buttonPanel = new JPanel();

        //add(buttonPanel);
        // buttonPanel.add(openButton);

        JTabbedPane loadFilePanel = new JTabbedPane();
        tabbedPane.add(buttonPanel);
        buttonPanel.add(openButton);
        add(tabbedPane);
        setVisible(true);


        openButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setDialogTitle("Open File");
                fileChooser.setCurrentDirectory(new File(System.getProperty("user.home")));
                FileNameExtensionFilter filter = new FileNameExtensionFilter("Hipo Files", "hipo", "HIPO");
                fileChooser.setFileFilter(filter);
                int result = fileChooser.showOpenDialog(null);
                if (result == JFileChooser.APPROVE_OPTION) {
                    File selectedFile = fileChooser.getSelectedFile();
                    Config.Hipo_FILE = selectedFile.getAbsolutePath();
                    System.out.println("Selected file: " + selectedFile.getAbsolutePath());
                    // Create the T0 panel (from calibration code).
                    tabbedPane.remove(buttonPanel);

                    JTabbedPane t0Panel = T0Calibration.createT0Panel();
                    tabbedPane.addTab("T0", t0Panel);

                    // Create the T2D panel.
                    //JPanel t2dPanel = T2DCalibration.createT2DPanel();
                    //tabbedPane.addTab("T2D", t2dPanel);

                    // Create a panel for residuals (if separated from T2D).
                   // JPanel residualsPanel = T2DCalibration.createResidualsPanel();
                   // tabbedPane.addTab("Residuals", residualsPanel);

                    tabbedPane.revalidate();

                }
            }
        });
    }
}
