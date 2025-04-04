/*
 * This code was developed by Hamza Atac for the ALERT COLLOBORATION
 * to perform calibrations for the DC detector.
 *
 */
package org.clas.modules.calibration;

import org.jlab.detector.calib.utils.CalibrationConstants;
import org.jlab.detector.calib.utils.DatabaseConstantProvider;

public class CalibrationUtils {
    private CalibrationConstants calibConstsT0 = null;
    private CalibrationConstants calibConstsT2D = null;
    public DatabaseConstantProvider provider = null;
    public int runNumber = 0;
    public CalibrationUtils(int runNumber) {
        this.runNumber = runNumber;
        provider = new DatabaseConstantProvider(runNumber, "default");
        this.calibConstsT0 = provider.readConstants("/calibration/alert/ahdc/time_offsets");
        this.calibConstsT2D = provider.readConstants("/calibration/alert/ahdc/time_to_distance");
    }
    public double[] GetT2DFitParamsFromCCDB() {
        double[] pars = new double[4];

        //provider.loadTable("/calibration/alert/ahdc/time_to_distance");
        for (int i = 0; i < 4; i++) {
            //pars[i] = provider.getDouble("/calibration/alert/ahdc/time_to_distance/p" + i, 0);
            pars[i] = calibConstsT2D.getDoubleValue("p"+i, 0,0,0);
        }
        provider.disconnect();
        return pars;
    }
    public double getT0off(int layer, int component){
        return calibConstsT0.getDoubleValue("t0",0,layer,component);
    }
}
