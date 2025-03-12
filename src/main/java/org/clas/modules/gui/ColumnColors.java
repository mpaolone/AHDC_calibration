/*
 * This code was developed by Hamza Atac for the ALERT COLLOBORATION
 * to perform calibrations for the DC detector.
 *
 */
package org.clas.modules.gui;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

public class ColumnColors extends DefaultTableCellRenderer {

    private static final Color[] PASTEL_COLORS = new Color[]{
            new Color(239, 246, 255),  // Soft Blue
            new Color(255, 244, 244)   // Soft Pink
    };

    @Override
    public Component getTableCellRendererComponent(
            JTable table, Object value, boolean isSelected,
            boolean hasFocus, int row, int column) {

        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        if (!isSelected) {
            Color pastel = PASTEL_COLORS[column % PASTEL_COLORS.length];
            setBackground(pastel);
        } else {
            setBackground(table.getSelectionBackground());
        }
        return this;
    }
}
