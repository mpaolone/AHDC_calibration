package org.clas.modules.calibration;

import org.clas.modules.config.Config;
import org.clas.modules.utils.FileUtils;
import org.jlab.groot.data.GraphErrors;
import org.jlab.groot.data.H1F;
import org.jlab.groot.data.H2F;
import org.jlab.groot.fitter.DataFitter;
import org.jlab.groot.graphics.EmbeddedCanvas;
import org.jlab.groot.math.F1D;
import org.jlab.io.hipo.HipoDataBank;
import org.jlab.io.hipo.HipoDataEvent;
import org.jlab.io.hipo.HipoDataSource;

import javax.swing.*;
import java.awt.*;
import org.jlab.groot.graphics.*;

public class T2DCalibration {

    // Use the input file from Config.
    private static final String inputFile = Config.Hipo_FILE;
    private static final double TIME_START = 40.0;
    private static final double TIME_END = 140.0;
    private static final double TIME_INTERVAL = 20.0;

    private static F1D fitgrdoca;


    /**
     * Creates and returns the main T2D panel with calibration plots and buttons.
     */
    public static JPanel createT2DPanel() {
        // Define colors for buttons.
        Color lightBlue = new Color(128, 200, 255);
        Color customOrange = new Color(255, 180, 100);

        // Create histograms and graphs.
        H1F hadc = new H1F("hadc", "hadc", 100, 0, 250.0);
        H1F hdoca = new H1F("hdoca", "hdoca", 100, 0., 0.4);
        H2F htdoca = new H2F("htdoca", "htdoca", 100, 0, 5000, 100, 0., 0.4);
        GraphErrors grdoca = new GraphErrors();

        CalibrationUtils cbutils = new CalibrationUtils(0);

        // Open the HIPO file and fill the histograms.
        HipoDataSource reader = new HipoDataSource();
        reader.open(inputFile);
        int nevents = reader.getSize();
        for (int i = 0; i < nevents; i++) {
            HipoDataEvent event = (HipoDataEvent) reader.gotoEvent(i);
            if (event.hasBank("AHDC:Hits")) {
                HipoDataBank hitBank = (HipoDataBank) event.getBank("AHDC::Hits");
                int rows = hitBank.rows();
                for (int loop = 0; loop < rows; loop++) {
                    int layer = (int)hitBank.getByte("layer",loop);
                    int component = hitBank.getInt("wire",loop);
                    int slayer = (int)hitBank.getByte("superlayer",loop);
                    layer += 10*slayer;
                    //time is now t0 subtracted time in Hits bank
                    //float time = hitBank.getFloat("time", loop) - (float)cbutils.getT0off(layer,component);
                    float time = hitBank.getFloat("time", loop);
                    float doca = hitBank.getInt("trackDoca", loop);
                    hadc.fill(time);
                    hdoca.fill(doca);
                    htdoca.fill(time, doca);
                }
            }
        }

        // Fit a polynomial function to hdoca.
        F1D Fpol2 = new F1D("FitFunc", "[p0]+[p1]*x+[p2]*x*x", 0.7, 2);
        DataFitter.fit(Fpol2, hdoca, "Q");
        Fpol2.setLineColor(2);

        // Fill grdoca graph using slices of htdoca.
        for (int i = 0; i < 100; i++) {
            double hmean = htdoca.sliceX(i).getMean();
            double hmeanE = htdoca.sliceX(i).getRMS();
            double xx = htdoca.getDataX(i);
            grdoca.addPoint(xx, hmean, 0., hmeanE);
        }
        int xDataSize = grdoca.getDataSize(0);
        double xMin = grdoca.getDataX(0);
        double xMax = grdoca.getDataX(xDataSize - 1);

        // Create and fit doca vs time function.
        fitgrdoca = new F1D("fitgrdoca",
                "[p3]*x*x*x+[p2]*x*x+[p1]*x+[p0]",
                xMin, xMax);
        fitgrdoca.setParameter(0, 0.1);
        fitgrdoca.setParameter(1, 0.1);
        fitgrdoca.setParameter(2, 0.1);
        fitgrdoca.setParameter(3, 0.1);
        fitgrdoca.setLineColor(2);
        fitgrdoca.setLineWidth(3);
        DataFitter.fit(fitgrdoca, grdoca, "Q");

        // Retrieve previous T2D fit parameters from CCDB.

        double[] ccdbFitParams = cbutils.GetT2DFitParamsFromCCDB();
        F1D previous_t2dfit = new F1D("previous_t2dfit",
                "[p3]*x*x*x+[p2]*x*x+[p1]*x+[p0]",
                xMin, xMax);
        previous_t2dfit.setParameter(0, ccdbFitParams[0]);
        previous_t2dfit.setParameter(1, ccdbFitParams[1]);
        previous_t2dfit.setParameter(2, ccdbFitParams[2]);
        previous_t2dfit.setParameter(3, ccdbFitParams[3]);
        previous_t2dfit.setLineColor(4);
        previous_t2dfit.setLineWidth(3);

        // Create the main canvas divided into 2x2 panels.
        EmbeddedCanvas t2dCanvas = new EmbeddedCanvas();
        t2dCanvas.divide(2, 2);
        t2dCanvas.cd(0); t2dCanvas.draw(htdoca);
        t2dCanvas.cd(1); t2dCanvas.draw(grdoca); t2dCanvas.draw(previous_t2dfit, "same");
        t2dCanvas.cd(2); t2dCanvas.draw(fitgrdoca); t2dCanvas.draw(previous_t2dfit, "same");

        // Instead of drawing an empty placeholder, compute the residuals.
        H1F hresidual = new H1F("hresidual", "Residuals", 100, -3, 3);
        // Re-open the file to compute residuals.
        reader.open(inputFile);
        for (int i = 0; i < nevents; i++) {
            HipoDataEvent event = (HipoDataEvent) reader.gotoEvent(i);
            if (event.hasBank("AHDC::Hits")) {
                HipoDataBank hitBank = (HipoDataBank) event.getBank("AHDC::Hits");
                int rows = hitBank.rows();
                for (int loop = 0; loop < rows; loop++) {
                    int layer = (int)hitBank.getByte("layer",loop);
                    int component = hitBank.getInt("wire",loop);
                    int slayer = (int)hitBank.getByte("superlayer",loop);
                    layer += 10*slayer;
                    //time is now t0 subtracted time in Hits bank
                    float time = hitBank.getFloat("time", loop);
                    //float time = hitBank.getFloat("time", loop) - (float)cbutils.getT0off(layer,component);
                    float doca = hitBank.getInt("trackDoca", loop);
                    // Evaluate the grdoca fit function.
                    double pedfit = fitgrdoca.evaluate(time);
                    double residual = doca - pedfit;
                    hresidual.fill(residual);
                }
            }
        }
        // Fit a Gaussian to the residual histogram.
        F1D gaussianFit1 = new F1D("gaussianFit1", "[amp]*exp(-0.5*((x-[mean])/[sigma])^2)", -3, 3);
        gaussianFit1.setParameter(0, hresidual.getMax());
        gaussianFit1.setParameter(1, hresidual.getMean());
        gaussianFit1.setParameter(2, hresidual.getRMS());
        gaussianFit1.setLineColor(4);
        gaussianFit1.setLineWidth(3);
        DataFitter.fit(gaussianFit1, hresidual, "Q");
        // Draw the residual histogram and its Gaussian fit on pad 3.
        t2dCanvas.cd(3);
        t2dCanvas.draw(hresidual);
        t2dCanvas.draw(gaussianFit1, "same");
        gaussianFit1.setOptStat(1100);

        // Build the main panel.
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(t2dCanvas, BorderLayout.CENTER);

        // Create an extra canvas for residuals if needed during fit adjustment.
        EmbeddedCanvas residualsCanvas = new EmbeddedCanvas();

        // "Adjust grdoca Fit" button.
        JButton refitGrdocaButton = new JButton("Adjust grdoca Fit");
        refitGrdocaButton.setOpaque(true);
        refitGrdocaButton.setBorderPainted(false);
        refitGrdocaButton.setBackground(lightBlue);
        refitGrdocaButton.setForeground(Color.BLACK);
        refitGrdocaButton.addActionListener(e -> {
            openGrdocaFitGUI(grdoca, fitgrdoca, t2dCanvas, residualsCanvas, reader, nevents);
        });

        // "Save T2D Parameters to File" button.
        JButton saveToFileButton = new JButton("Save T2D Parameters to File");
        saveToFileButton.setOpaque(true);
        saveToFileButton.setBorderPainted(false);
        saveToFileButton.setBackground(customOrange);
        saveToFileButton.setForeground(Color.BLACK);
        saveToFileButton.addActionListener(e -> {
            FileUtils.saveT2DToFile(fitgrdoca);
        });

        // Panel to hold the two buttons.
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(refitGrdocaButton);
        buttonPanel.add(saveToFileButton);
        panel.add(buttonPanel, BorderLayout.NORTH);

        return panel;
    }

    /**
     * Opens a dialog to adjust the grdoca fit.
     * Heavy computation is offloaded using a SwingWorker.
     */
    private static void openGrdocaFitGUI(GraphErrors grdoca, F1D fitgrdoca, EmbeddedCanvas t2dCanvas,
                                         EmbeddedCanvas residualsCanvas, HipoDataSource reader, int nevents) {
        JFrame fitFrame = new JFrame("Adjust grdoca Fit");
        fitFrame.setSize(700, 500);
        fitFrame.setLayout(new BorderLayout());
        fitFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        EmbeddedCanvas fitCanvas = new EmbeddedCanvas();
        fitCanvas.divide(1, 1);
        fitCanvas.cd(0);
        fitCanvas.draw(grdoca);
        fitCanvas.draw(fitgrdoca, "same");

        JPanel controls = new JPanel(new GridLayout(3, 3));
        JTextField xMinField = new JTextField(String.valueOf(fitgrdoca.getMin()));
        JTextField xMaxField = new JTextField(String.valueOf(fitgrdoca.getMax()));
        controls.add(new JLabel("Fit Min:"));
        controls.add(xMinField);
        controls.add(new JLabel("Fit Max:"));
        controls.add(xMaxField);

        // Create text fields for the 4 parameters.
        JTextField[] paramFields = new JTextField[4];
        for (int i = 0; i < 4; i++) {
            paramFields[i] = new JTextField(String.valueOf(fitgrdoca.getParameter(i)));
            controls.add(new JLabel("P" + i + ":"));
            controls.add(paramFields[i]);
        }

        JButton refitButton = new JButton("Refit");
        refitButton.setOpaque(true);
        refitButton.setBorderPainted(false);
        refitButton.setBackground(new Color(128, 200, 255));
        refitButton.setForeground(Color.BLACK);
        controls.add(refitButton);

        // Offload heavy computation using SwingWorker.
        refitButton.addActionListener(e -> {
            refitButton.setEnabled(false);
            SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() throws Exception {
                    double newXMin = Double.parseDouble(xMinField.getText());
                    double newXMax = Double.parseDouble(xMaxField.getText());
                    fitgrdoca.setRange(newXMin, newXMax);
                    for (int p = 0; p < 4; p++) {
                        double param = Double.parseDouble(paramFields[p].getText());
                        fitgrdoca.setParameter(p, param);
                    }
                    DataFitter.fit(fitgrdoca, grdoca, "Q");
                    return null;
                }
                @Override
                protected void done() {
                    try {
                        get(); // Propagate exceptions.
                        fitCanvas.repaint();
                        t2dCanvas.repaint();
                        residualsCanvas.repaint();
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(fitFrame,
                                "Error during fit adjustment: " + ex.getMessage(),
                                "Error", JOptionPane.ERROR_MESSAGE);
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
    }

    /**
     * Creates and returns a panel that displays residual histograms and their fit.
     */
    public static JPanel createResidualsPanel() {
        int numBins = (int) ((TIME_END - TIME_START) / TIME_INTERVAL);
        H1F[] residualHistograms = new H1F[numBins];
        for (int i = 0; i < numBins; i++) {
            double start = TIME_START + i * TIME_INTERVAL;
            double end = start + TIME_INTERVAL;
            residualHistograms[i] = new H1F("Residuals " + i,
                    String.format("Residuals [%d-%d] ns", (int) start, (int) end),
                    100, -3, 3);
        }
        H1F hresidual = new H1F("residuals", "Residuals", 100, -3, 3);
        CalibrationUtils cbutils = new CalibrationUtils(0);
        HipoDataSource reader = new HipoDataSource();
        reader.open(inputFile);
        int nevents = reader.getSize();
        for (int i = 0; i < nevents; i++) {
            HipoDataEvent event = (HipoDataEvent) reader.gotoEvent(i);
            if (event.hasBank("AHDC::Hits")) {
                HipoDataBank hitBank = (HipoDataBank) event.getBank("AHDC::Hits");
                int rows = hitBank.rows();
                for (int loop = 0; loop < rows; loop++) {
                    int layer = (int)hitBank.getByte("layer",loop);
                    int component = hitBank.getInt("wire",loop);
                    int slayer = (int)hitBank.getByte("superlayer",loop);
                    layer += 10*slayer;
                    //time is now T0 subtracted hit time in Hits bank
                    float time = hitBank.getFloat("time", loop);
                    //float time = hitBank.getFloat("time", loop) - (float)cbutils.getT0off(layer,component);
                    float doca = hitBank.getInt("trackDoca", loop);
                    // Evaluate the grdoca fit function.
                    double pedfit = fitgrdoca.evaluate(time);
                    double residual = doca - pedfit;
                    hresidual.fill(residual);
                    int binIndex = (int) ((time - TIME_START) / TIME_INTERVAL);
                    if (binIndex >= 0 && binIndex < numBins) {
                        residualHistograms[binIndex].fill(residual);
                    }
                }
            }
        }
        // Fit a Gaussian to the overall residual histogram.
        F1D gaussianFit = new F1D("gaussianFit", "[amp]*exp(-0.5*((x-[mean])/[sigma])^2)", -3, 3);
        gaussianFit.setParameter(0, hresidual.getMax());
        gaussianFit.setParameter(1, hresidual.getMean());
        gaussianFit.setParameter(2, hresidual.getRMS());
        gaussianFit.setLineColor(4);
        gaussianFit.setLineWidth(3);
        DataFitter.fit(gaussianFit, hresidual, "Q");
        gaussianFit.setOptStat(1100);

        // Create a canvas and draw the overall residual histogram and binned residuals.
        EmbeddedCanvas residualsCanvas = new EmbeddedCanvas();
        residualsCanvas.divide(2, 3);
        residualsCanvas.cd(0);
        residualsCanvas.draw(hresidual);
        residualsCanvas.draw(gaussianFit, "same");
        for (int i = 0; i < 5; i++) {
            residualsCanvas.cd(i + 1);
            residualsCanvas.draw(residualHistograms[i]);
        }
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(residualsCanvas, BorderLayout.CENTER);
        return panel;
    }
}
