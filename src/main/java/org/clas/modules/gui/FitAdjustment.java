/*
 * This code was developed by Hamza Atac for the ALERT COLLOBORATION
 * to perform calibrations for the DC detector.
 *
 */

package org.clas.modules.gui;

import org.jlab.groot.data.GraphErrors;
import org.jlab.groot.data.H1F;
import org.jlab.groot.fitter.DataFitter;
import org.jlab.groot.math.F1D;
import org.jlab.groot.graphics.EmbeddedCanvas;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class FitAdjustment {

    public static void showT0AdjustmentDialog(int layer, int wire, H1F histogram, F1D fitF,
                                              GraphErrors layerGraph, DefaultTableModel tableModel) {
        JFrame fitFrame = new JFrame("Adjust Fit for L" + layer + " Wire " + wire);
        fitFrame.setSize(700, 500);
        fitFrame.setLayout(new BorderLayout());

        EmbeddedCanvas fitCanvas = new EmbeddedCanvas();
        fitCanvas.divide(1, 1);
        fitCanvas.cd(0);
        fitCanvas.draw(histogram);
        fitCanvas.draw(fitF, "same");

        JPanel controls = new JPanel(new GridLayout(3, 3));

        JTextField xMinField = new JTextField(String.valueOf(fitF.getMin()));
        JTextField xMaxField = new JTextField(String.valueOf(fitF.getMax()));
        controls.add(new JLabel("Fit Min:"));
        controls.add(xMinField);
        controls.add(new JLabel("Fit Max:"));
        controls.add(xMaxField);

        // 5 parameters => p0...p4
        JTextField[] paramFields = new JTextField[5];
        for (int i = 0; i < 5; i++) {
            paramFields[i] = new JTextField(String.valueOf(fitF.getParameter(i)));
            controls.add(new JLabel("P" + i + ":"));
            controls.add(paramFields[i]);
        }

        JButton refitButton = new JButton("Refit");
        controls.add(refitButton);

        // Use SwingWorker to offload the heavy fitting computation.
        refitButton.addActionListener(e -> {
            // Disable the button to prevent repeated clicks.
            refitButton.setEnabled(false);
            SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() throws Exception {
                    try {
                        double newXMin = Double.parseDouble(xMinField.getText());
                        double newXMax = Double.parseDouble(xMaxField.getText());
                        fitF.setRange(newXMin, newXMax);
                        for (int p = 0; p < 5; p++) {
                            double param = Double.parseDouble(paramFields[p].getText());
                            fitF.setParameter(p, param);
                        }
                        // Perform the heavy fit.
                        DataFitter.fit(fitF, histogram, "Q");
                    } catch (NumberFormatException ex) {
                        throw ex;
                    }
                    return null;
                }

                @Override
                protected void done() {
                    try {
                        // Propagate exceptions if any.
                        get();
                        double newT0 = fitF.getParameter(0) / fitF.getParameter(1);
                        double newT0Err = Math.sqrt(
                                Math.pow(fitF.parameter(0).error() / fitF.getParameter(1), 2) +
                                        Math.pow(fitF.getParameter(0) * fitF.parameter(1).error() /
                                                Math.pow(fitF.getParameter(1), 2), 2)
                        );
                        double chi2 = fitF.getChiSquare();
                        double ndf = fitF.getNDF();
                        double chi2ndf = (ndf > 0) ? (chi2 / ndf) : 0.0;

                        // Update the graph point for the given wire.
                        for (int iPt = 0; iPt < layerGraph.getDataSize(0); iPt++) {
                            if ((int) layerGraph.getDataX(iPt) == wire) {
                                layerGraph.setPoint(iPt, wire, newT0);
                                layerGraph.setError(iPt, 0, newT0Err);
                                break;
                            }
                        }
                        // Update the table model.
                        for (int rr = 0; rr < tableModel.getRowCount(); rr++) {
                            if ((int) tableModel.getValueAt(rr, 0) == wire) {
                                tableModel.setValueAt(String.format("%.4f", newT0), rr, 1);
                                tableModel.setValueAt(String.format("%.4f", newT0Err), rr, 2);
                                tableModel.setValueAt(String.format("%.2f", chi2ndf), rr, 3);
                                break;
                            }
                        }
                        fitCanvas.repaint();
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(fitFrame,
                                "Invalid input detected. Please enter valid numbers.",
                                "Input Error", JOptionPane.ERROR_MESSAGE);
                    } finally {
                        refitButton.setEnabled(true);
                    }
                }
            };
            worker.execute();
        });

        fitFrame.add(fitCanvas, BorderLayout.CENTER);
        fitFrame.add(controls, BorderLayout.SOUTH);
        fitFrame.setVisible(true);
        fitFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    }
}
