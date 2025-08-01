package org.clas.modules.calibration;

import org.jlab.groot.data.GraphErrors;
import org.jlab.groot.data.H1F;
import org.jlab.groot.data.H2F;
import org.jlab.groot.fitter.ParallelSliceFitter;
import org.jlab.groot.math.F1D;


public class T0Fit2D {
    public F1D polyfit = new F1D("f","[a]*pow(x,4) + [b]*pow(x,3) + [c]*pow(x,2) + [d]*x + [e]",125,500);
    public F1D polyfitDerivative = new F1D("df","4.0*[a]*pow(x,3) + 3.0*[b]*pow(x,2) + 2.0*[c]*x + [d]",125,500);

    public GraphErrors get2Dgraph(H2F input_hist){
        ParallelSliceFitter psf = new ParallelSliceFitter(input_hist);
        psf.fitSlicesX();
        GraphErrors gr = psf.getMeanSlices();
        return gr;
    }
}
