/*
 * This code was developed by Hamza Atac for the ALERT COLLOBORATION
 * to perform calibrations for the DC detector.
 *
 */
package org.clas.modules.calibration;

import org.jlab.detector.calib.utils.DatabaseConstantProvider;

public class CalibrationUtils {
    public static double[] GetT2DFitParamsFromCCDB(int runNumber) {
        double[] pars = new double[4];
        DatabaseConstantProvider provider = new DatabaseConstantProvider(runNumber, "default");
        provider.loadTable("/calibration/alert/t2d_fit_params");
        for (int i = 0; i < 4; i++) {
            pars[i] = provider.getDouble("/calibration/alert/t2d_fit_params/p" + i, 0);
        }
        provider.disconnect();
        return pars;
    }
}
