package op;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

/**
 * Created by Danielle on 26-5-16.
 */


/* AutoCorrelation class. Upon creation of an AutoCorrelation object for a certain accelerator
 * signal, this class immediately determines the corresponding state (STILL or
 * WALKING) and if necessary the period of walking. All based on normalized auto-correlation
 * as described by http://research.microsoft.com/pubs/166309/com273-chintalapudi.pdf.
 */
public class AutoCorrelation {
    //private static final int tMin = 20, tMax =50; // 1 stap
    private static final int tMin = 40, tMax = 100; // 2 stappen
    private static final int smoothIntervalTail = 2;            // Parameter used for smoothing accelerator data
    private static final int tWindowTailSize = 5; // Distance from optimal tau that is checked
    private List<Double> accData;   // Smoothed accelerator signal
    private LinkedList<Double> rawAccData;   // Raw accelerator signal
    public State currentState;      // Activity state of user
    public int optPeriod;           // Equals step periodicity of user if the user is walking, 0 otherwise.
    public double autocorr;
    private double maxMagnitudeThreshold; // Is a threshold for the maximum magnitude, to ignore big sudden values
    public int lowT = tMin, highT = tMax;
    private StepDetector stepDetector;

    public enum State {
        STILL,WALKING
    }

    /* Class to contain the result of norm. auto-correlation: max value of psi on the interval
     * tMin to tMax and the corresponding period.
     */
    private class Result {

        public int period;
        public double max;
        public double maxMagnitude;

        public Result(int p, double m, double magnitude) {
            this.period = p;
            this.max = m;
            this.maxMagnitude = magnitude;
        }
    }

    /* Constructor */
    public AutoCorrelation(StepDetector stepDetector, List<Double> accData) {
        this.accData = accData;
        this.rawAccData = new LinkedList<>();
        this.optPeriod = 30;
        this.currentState = State.STILL;
        this.autocorr = 0;
        this.stepDetector = stepDetector;
        this.maxMagnitudeThreshold = 6;//Double.MAX_VALUE;

        // Set state of user for given accData window
        setState();
    }

    public void addData(double data) {
        rawAccData.add(data);
        if (rawAccData.size() > smoothIntervalTail * 2 + 1)
            rawAccData.removeFirst();

        // Get smooth value
        accData.add(getAverage(rawAccData));

        if (accData.size() > 2 * tMax + 2) {
            accData.remove(0);
        }
    }

    public double getAutoCorrelation() {
        setState();
        return this.autocorr;
    }
    
    /* Set state of user for sample m by checking value of psi(m) */
    public void setState() {
        // Check if enough data has been obtained
        if (accData.size() > 2 * tMax + 1) {
            Result res = maxNormAutoCorrelation(0, lowT, highT);
            this.autocorr = res.max;
            if (res.max > StepDetector.CORRELATION_WALKING_THRESHOLD) {
                this.currentState = State.WALKING;
                this.optPeriod = res.period;
                this.lowT = this.optPeriod - tWindowTailSize < tMin ? tMin : this.optPeriod - tWindowTailSize;
                this.highT = this.optPeriod + tWindowTailSize > tMax ? tMax : this.optPeriod + tWindowTailSize;
                // Update max magnitude
                this.maxMagnitudeThreshold = res.maxMagnitude * 4;
            } else {
                this.currentState = State.STILL;
            }
            //Log.d("autocorr_res", String.format("optPeriod " + this.optPeriod + ", AutoCorr %.2f", res.max));
/*
            if (autocorr > StepDetector.CORRELATION_WALKING_THRESHOLD) {
            	System.out.println(stepDetector.counter + " - " + autocorr + ", " + optPeriod);
            }*/
        }
    }

    /* Compute normalized auto-correlation X(m,tau) of accData for sample m and lag tau */
    public double X(int m, int tau) {
        double X = 0;
        double mu1 = mean(m, tau);
        double mu2 = mean(m+tau, tau);


        for (int k = 0; k < tau; k++) {
            X += (accData.get(m+k) - mu1) * (accData.get(m+k+tau) - mu2);
        }

        double sd1 = sd(m, tau);
        double sd2 = sd(m + tau, tau);

        // Check if samples have high enough standard deviation
        // This is useful for sudden movements
        if (sd1 < StepDetector.STANDARD_DEV_WALKING_THRESHOLD || sd2 < StepDetector.STANDARD_DEV_WALKING_THRESHOLD)
            return -1;

        X /= (tau * sd1 * sd2);
        //Log.d("X",String.format("%.2f",X));
        return X;
    }

    /* Compute the maximum normalized auto-correlation psi(m) for lags between tauMin and tauMax
     * for sample m. Corresponding period is returned as well. */
    public Result maxNormAutoCorrelation(int m, int tauMin, int tauMax) {

        double maxAC = -1;
        int optPeriod = 0;
        int size = this.accData.size();
        int currentM = size - 1 - tauMin * 2;
        
        // Check the max value being used
        double maxMagnitude = Double.MIN_VALUE;
        double val;
        for (int i = size - 1 - tauMax * 2; i < size; i++) {
        	val = accData.get(i);
        	maxMagnitude = val > maxMagnitude ? val : maxMagnitude;
        }
        // If above threshold never mind
        if (maxMagnitude > maxMagnitudeThreshold)
        	return new Result(0, -1, maxMagnitude);

        for (int t = tauMin; t <= tauMax; t++, currentM -= 2) {
            double x = X(currentM, t);
            if (x > maxAC) {
            	maxAC = x;
                optPeriod = t;
            }

        }
        //Log.d("maxNorm", String.format("Max %.2f optPeriod " + optPeriod,max));
        return new Result(optPeriod, maxAC, maxMagnitude);
    }


    /* Compute standard deviation of samples k to k + l-1 of a list */
    private double sd (int k, int l){
        double sum = 0;
        double mean = mean(k, l);
        List<Double> a = this.accData;

        // Compute stdev
        for (int i = k; i < (k + l); i++) {
            sum += Math.pow((a.get(i) - mean), 2);
        }

        return Math.sqrt(sum / (l - 1)); // l- 1 instead of l since it is a sample, not a population
    }

    /* Compute mean of of samples k to k + l-1 of a list */
    private double mean (int k, int l) {
        double mean = 0;
        List<Double> a = this.accData;

        for (int i = k; i < (k + l); i++) {
            mean += a.get(i);
        }
        mean /= (l - 1); // l - 1 instead of l since it is a sample, not a population

        return mean;
    }

    private double getAverage(LinkedList<Double> list) {
        ListIterator<Double> it = list.listIterator();
        double val = 0;
        while (it.hasNext()) {
            val += it.next();
        }

        return val / list.size();
    }


    /*
    private List<Double> findPeaks(List<Double> a) {

        double max = 0;
        List<Double> peaks = new ArrayList<Double>();

        int startWindow = 0;
        int endWindow = startWindow + tMax;
        int delta = tMin;                       // Minimum amount of samples between peaks
        int peakLoc = 0;

        for (int i = 0; i < a.size(); ) {

            for (int j = i; j < endWindow; j++) {
                if (a.get(j) > max) {
                    max = a.get(j);
                    peakLoc = j;

                }
                // max now equals global max within window tMax
            }

            peaks.add(max);
            max = 0;

            if (peakLoc + delta + tMax >= a.size()) return peaks;

            i = peakLoc + delta;     // Minimal possible index for new peak
            endWindow = i + tMax;   // Maximum possible index for new peak


        }

        return peaks;

    }

    */






}
