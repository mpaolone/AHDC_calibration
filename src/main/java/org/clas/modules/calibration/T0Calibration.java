/*
 * This code was developed by Hamza Atac for the ALERT COLLOBORATION
 * to perform calibrations for the DC detector.
 *
 */

package org.clas.modules.calibration;

import org.jlab.groot.data.GraphErrors;
import org.jlab.groot.data.H1F;
import org.jlab.groot.fitter.DataFitter;
import org.jlab.groot.math.F1D;
import org.jlab.io.hipo.HipoDataBank;
import org.jlab.io.hipo.HipoDataEvent;
import org.jlab.io.hipo.HipoDataSource;
import org.clas.modules.gui.FitAdjustment;
import org.clas.modules.gui.ColumnColors;
import org.jlab.groot.graphics.EmbeddedCanvas;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import org.clas.modules.config.Config;
import org.clas.modules.utils.FileUtils;

public class T0Calibration {

    // Define expected layers and colors (adjust as needed)
    private static final java.util.List<Integer> expectedLayers = Arrays.asList(11, 21, 22, 31, 32, 41, 42, 51);
    private static final Color lightBlue = new Color(128, 200, 255);
    private static final Color customOrange = new Color(255, 180, 100);
    // Call hipo file from config
    private static final String inputFile = Config.Hipo_FILE;

    public static JTabbedPane createT0Panel() {
        JTabbedPane layeredT0Tabs = new JTabbedPane();

        // Data storage for layered T0 calibration.
        Map<Integer, Map<Integer, H1F>> layerHistograms = new HashMap<>();
        Map<Integer, Map<Integer, F1D>> layerFits = new HashMap<>();
        Map<Integer, GraphErrors> layerGraphs = new HashMap<>();

        // Open data source and fill histograms.
        HipoDataSource reader = new HipoDataSource();
        reader.open(inputFile);
        int nevents = reader.getSize();
        for (int i = 0; i < nevents; i++) {
            HipoDataEvent event = (HipoDataEvent) reader.gotoEvent(i);
            if (event.hasBank("AHDC::adc")) {
                HipoDataBank bank = (HipoDataBank) event.getBank("AHDC::adc");
                int rows = bank.rows();
                for (int loop = 0; loop < rows; loop++) {
                    float time = bank.getFloat("time", loop);
                    int layer = bank.getInt("layer", loop);
                    int component = bank.getInt("component", loop);
                    // Only store T0 data for expected layers.
                    if (!expectedLayers.contains(layer)) continue;
                    layerHistograms.computeIfAbsent(layer, k -> new HashMap<>());
                    layerHistograms.get(layer).computeIfAbsent(component,
                            k -> new H1F("Layer " + layer + " Component " + component,
                                    "Time for Wire " + component,
                                    100, 0, 5000));
                    layerHistograms.get(layer).get(component).fill(time);
                }
            }
        }

        // For each expected layer, build a tab.
        for (int layer : expectedLayers) {
            Map<Integer, H1F> histMap = layerHistograms.getOrDefault(layer, new HashMap<>());
            Map<Integer, F1D> layerFitMap = new HashMap<>();
            layerFits.put(layer, layerFitMap);

            GraphErrors layerGraph = new GraphErrors("Layer " + layer + " T0 vs Wire");
            layerGraph.setTitleX("Wire");
            layerGraph.setTitleY("T0 (ns)");
            layerGraphs.put(layer, layerGraph);

            // Build table data for each wire.
            String[] colNames = {"Wire", "T0 (ns)", "T0 Error (ns)", "Chi²/NDF"};
            Object[][] tableData = new Object[histMap.size()][4];
            int rowIndex = 0;
            for (Map.Entry<Integer, H1F> entry : histMap.entrySet()) {
                int wire = entry.getKey();
                H1F hist = entry.getValue();

                double mean = hist.getMean();
                double rms = hist.getRMS();
                double xMin = Math.max(0, mean - 2 * rms);
                double xMax = Math.min(hist.getXaxis().max(), mean + 3 * rms);

                F1D fitF = new F1D("T0Fit",
                        "(1./(1.+exp([p0]-[p1]*x))*exp([p2]-[p3]*x))+[p4]",
                        xMin, xMax);
                // Set initial parameters (example values).
                fitF.setParameter(0, -52);
                fitF.setParameter(1, -0.04);
                fitF.setParameter(2, -15);
                fitF.setParameter(3, -0.018);
                fitF.setParameter(4, 0.898);

                DataFitter.fit(fitF, hist, "Q");

                double T0 = fitF.getParameter(0) / fitF.getParameter(1);
                double T0Err = Math.sqrt(
                        Math.pow(fitF.parameter(0).error() / fitF.getParameter(1), 2) +
                                Math.pow(fitF.getParameter(0) * fitF.parameter(1).error() /
                                        Math.pow(fitF.getParameter(1), 2), 2)
                );
                double chi2 = fitF.getChiSquare();
                double ndf = fitF.getNDF();
                double chi2ndf = (ndf > 0) ? (chi2 / ndf) : 0.0;

                layerFitMap.put(wire, fitF);
                layerGraph.addPoint(wire, T0, 0, T0Err);

                tableData[rowIndex][0] = wire;
                tableData[rowIndex][1] = String.format("%.4f", T0);
                tableData[rowIndex][2] = String.format("%.4f", T0Err);
                tableData[rowIndex][3] = String.format("%.2f", chi2ndf);
                rowIndex++;
            }

            DefaultTableModel layerTableModel = new DefaultTableModel(tableData, colNames) {
                @Override
                public boolean isCellEditable(int row, int col) {
                    return false;
                }
            };
            JTable layerTable = new JTable(layerTableModel);

            // Set custom pastel renderer.
            ColumnColors pastelRenderer = new ColumnColors();
            for (int c = 0; c < layerTable.getColumnCount(); c++) {
                layerTable.getColumnModel().getColumn(c).setCellRenderer(pastelRenderer);
            }

            // Double-click to open the fit adjustment dialog.
            layerTable.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    if (e.getClickCount() == 2) {
                        int selRow = layerTable.getSelectedRow();
                        if (selRow != -1) {
                            int wire = (int) layerTable.getValueAt(selRow, 0);
                            FitAdjustment.showT0AdjustmentDialog(layer, wire, histMap.get(wire),
                                    layerFitMap.get(wire), layerGraph, layerTableModel);
                        }
                    }
                }
            });

            JScrollPane layerScrollPane = new JScrollPane(layerTable);
            EmbeddedCanvas layerCanvas = new EmbeddedCanvas();
            layerCanvas.divide(1, 1);
            layerCanvas.cd(0);
            layerCanvas.draw(layerGraph);

            JButton adjustFitBtn = new JButton("Adjust Fit");
            adjustFitBtn.setOpaque(true);
            adjustFitBtn.setBorderPainted(false);
            adjustFitBtn.setBackground(lightBlue);
            adjustFitBtn.setForeground(Color.BLACK);
            adjustFitBtn.addActionListener(e -> {
                int selRow = layerTable.getSelectedRow();
                if (selRow != -1) {
                    int wire = (int) layerTable.getValueAt(selRow, 0);
                    FitAdjustment.showT0AdjustmentDialog(layer, wire, histMap.get(wire),
                            layerFitMap.get(wire), layerGraph, layerTableModel);
                }
            });

            JButton saveTableBtn = new JButton("Save T0 Table");
            saveTableBtn.setOpaque(true);
            saveTableBtn.setBorderPainted(false);
            saveTableBtn.setBackground(customOrange);
            saveTableBtn.setForeground(Color.BLACK);
            saveTableBtn.addActionListener(e -> {
                FileUtils.saveT0TableToFile(layerTableModel, layer);
            });


            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            buttonPanel.add(adjustFitBtn);
            buttonPanel.add(saveTableBtn);

            JPanel layerPanel = new JPanel(new BorderLayout());
            layerPanel.add(layerCanvas, BorderLayout.CENTER);
            layerPanel.add(layerScrollPane, BorderLayout.SOUTH);
            layerPanel.add(buttonPanel, BorderLayout.NORTH);

            layeredT0Tabs.addTab("Layer " + layer, layerPanel);
        }
        return layeredT0Tabs;
    }
}
