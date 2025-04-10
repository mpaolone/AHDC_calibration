/*
 * This code was developed by Hamza Atac for the ALERT COLLOBORATION
 * to perform calibrations for the DC detector.
 *
 */
package org.clas.modules.utils;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

public class FileUtils {

    public static void saveT2DToFile(org.jlab.groot.math.F1D fitgrdoca) {
        double p0 = fitgrdoca.getParameter(0);
        double p1 = fitgrdoca.getParameter(1);
        double p2 = fitgrdoca.getParameter(2);
        double p3 = fitgrdoca.getParameter(3);

        String filePath = "./t2d_parameters.txt";
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, true))) {
            writer.write(String.format("p0: %.4f, p1: %.4f, p2: %.4f, p3: %.4f\n", p0, p1, p2, p3));
            JOptionPane.showMessageDialog(null,
                    "T2D parameters written to" +filePath+ "file!",
                    "Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null,
                    "Error writing to file:\n" + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    public static void saveT0TableToFile(Map<Integer,Object[][]> allTables){

        TreeMap<Integer,Object[][]> sorted = new TreeMap<>(allTables);
        String fileName = "T0table.txt";
        //final Integer[] nwires = {47, 56, 56, 72, 72, 87, 87, 99};
        //String[] layers = {"11", "21", "22", "31", "32", "41","42", "51"};
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
            writer.write("# Sector Layer Component t0 dt0 extra1 extra2 chi2ndf\n");
            sorted.forEach((sector, table) -> {
                for(int i = 0; i < table.length; i++){
                    try {
                        writer.write("1  " + sector + "  " + table[i][0] + "  " + table[i][1]
                                + "  " + table[i][2] + "  0.0  0.0  " + table[i][3] + "\n");
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


    }

    public static void saveT0TableToFile(DefaultTableModel tableModel, int layer) {
        String fileName = "layer_" + layer + "_T0table.txt";
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
            writer.write("# Wire   T0(ns)   T0Err(ns)   Chi2/NDF");
            writer.newLine();
            int rowCount = tableModel.getRowCount();
            for (int r = 0; r < rowCount; r++) {
                int wire = (int) tableModel.getValueAt(r, 0);
                String t0 = (String) tableModel.getValueAt(r, 1);
                String t0Err = (String) tableModel.getValueAt(r, 2);
                String chi2 = (String) tableModel.getValueAt(r, 3);
                writer.write(String.format("%4d  %8s  %8s  %8s", wire, t0, t0Err, chi2));
                writer.newLine();
            }
            writer.flush();
            JOptionPane.showMessageDialog(null,
                    "Saved layer " + layer + " T0 table to file:\n" + fileName,
                    "Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(null,
                    "Error writing T0 table to file:\n" + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }
}

