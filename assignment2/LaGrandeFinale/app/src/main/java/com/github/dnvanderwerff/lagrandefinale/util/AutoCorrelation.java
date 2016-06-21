package com.github.dnvanderwerff.lagrandefinale.util;

import android.util.Log;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

/*
 *  This class performs normalized auto-correlation as explained in Zee: Zero-Effort Crowdsourcing for Indoor Localization (http://research.microsoft.com/pubs/166309/com273-chintalapudi.pdf)
 *  It takes the magnitude of the acceleration, smoothes this and performs autocorrelation on this smoothed data.
 */
public class AutoCorrelation {
    //private static final int tMin = 20, tMax =50; // 1 stap
    private static final int tMin = 40, tMax = 100; // 2 stappen
    private static final int smoothIntervalTail = 2;            // Parameter used for smoothing accelerator data
    public static final double WALKING_THRESHOLD = 0.7;
    private static final int tWindowTailSize = 20; // Distance from optimal tau that is checked
    private List<Double> accData;   // Smoothed accelerator signal
    private List<Double> calcData;  // Data that is used to calculate the auto correlation
    private LinkedList<Double> rawAccData;   // Raw accelerator signal
    public State currentState;      // Activity state of user
    public int optPeriod;           // Equals step periodicity of user if the user is walking, 0 otherwise.
    public double autocorr;
    private double maxMagnitudeThreshold; // Is a threshold for the maximum magnitude, to ignore big sudden values
    public int lowT = tMin, highT = tMax;
    private double[] storedData; // Contains the last walk signal


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
    public AutoCorrelation(List<Double> accData) {
        this.accData = accData;
        this.calcData = new ArrayList<>();
        this.rawAccData = new LinkedList<>();
        this.optPeriod = 30;
        this.currentState = State.STILL;
        this.autocorr = 0;
        this.maxMagnitudeThreshold = Double.MAX_VALUE;

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
            Result res = maxNormAutoCorrelation(lowT, highT);
            this.autocorr = res.max;
            if (res.max > WALKING_THRESHOLD) {
                this.currentState = State.WALKING;
                this.optPeriod = res.period;
                this.lowT  = this.optPeriod - tWindowTailSize < tMin ? tMin : this.optPeriod - tWindowTailSize;
                this.highT = this.optPeriod + tWindowTailSize > tMax ? tMax : this.optPeriod + tWindowTailSize;
                // Update max magnitude
                this.maxMagnitudeThreshold = res.maxMagnitude * 4; // 4 is just a choice.. maybe needs to be adapted
                // Store the most recent walk signal
                storeSignal();
            } else {
                this.currentState = State.STILL;
            }
            Log.d("autocorr_res", String.format("optPeriod " + this.optPeriod + ", AutoCorr %.2f", res.max));

        }
    }

    /* Compute normalized auto-correlation X(m,tau) of accData for sample m and lag tau */
    public double X(int m, int tau) {
        double X = 0;
        double mu1 = mean(calcData, m, tau);
        double mu2 = mean(calcData, m+tau, tau);


        for (int k = 0; k < tau; k++) {
            X += (calcData.get(m+k) - mu1) * (calcData.get(m+k+tau) - mu2);
        }

        double sd1 = sd(calcData, m, tau);
        double sd2 = sd(calcData, m + tau, tau);

        // Check if samples have high enough standard deviation
        if (sd1 < StepDetector.STANDARD_DEV_WALKING_THRESHOLD || sd2 < StepDetector.STANDARD_DEV_WALKING_THRESHOLD)
            return -1;

        X /= (tau * sd1 * sd2);
        return X;
    }

    /* Compute the maximum normalized auto-correlation psi(m) for lags between tauMin and tauMax
     * for sample m. Corresponding period is returned as well. */
    public Result maxNormAutoCorrelation(int tauMin, int tauMax) {

        double max = -1;
        int optPeriod = 0;
        int size = this.accData.size();
        int currentM;

        // The mean of the left half
        double sum = 0;
        int cnt = 0;
        for (int i = size - 1 - tauMax * 2; i < size - tauMax; i++, cnt++) {
            sum += accData.get(i);
        }
        double mu = sum / cnt;

        // Check the max value being used, and the standard deviation of the left half
        double maxMagnitude = Double.MIN_VALUE;
        double val;
        sum = 0;
        for (int i = size - 1 - tauMax * 2; i < size; i++) {
            val = accData.get(i);
            calcData.add(val);
            maxMagnitude = val > maxMagnitude ? val : maxMagnitude;
            // Get stdev of left half
            if (i < size - tauMax)
                sum += Math.pow(val - mu, 2);
        }

        // If above threshold never mind
        if (maxMagnitude > maxMagnitudeThreshold)
            return new Result(0, -1, maxMagnitude);

        // If the left half standard deviation is too low, replace by the stored step if available
        double sd = Math.sqrt(sum / cnt);

        if (storedData != null && sd < StepDetector.STANDARD_DEV_WALKING_THRESHOLD) {
            Log.d("TEST", "Using stored data");
            for (int i = 0; i < highT; i++)
                calcData.set(i, storedData[i]);
        }

        // Recalculate currentM for calcData
        currentM = calcData.size() - 1 - tauMin * 2;

        for (int t = tauMin; t <= tauMax; t++, currentM -= 2) {
            double x = X(currentM, t);
            if (x > max) {
                max = x;
                optPeriod = t;
            }

        }
        Log.d("maxNorm", String.format("Max %.2f optPeriod " + optPeriod,max));
        return new Result(optPeriod, max, maxMagnitude);
    }


    /* Compute standard deviation of samples k to k + l-1 of a list */
    private double sd (List<Double> a, int k, int l){
        double sum = 0;
        double mean = mean(a, k, l);

        // Compute stdev
        for (int i = k; i < (k + l); i++) {
            sum += Math.pow((a.get(i) - mean), 2);
        }

        return Math.sqrt(sum / (l - 1)); // l- 1 instead of l since it is a sample, not a population
    }

    /* Compute mean of of samples k to k + l-1 of a list */
    private double mean (List<Double> a, int k, int l) {
        double mean = 0;

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

    // Store the last highT amount of samples in the stored signal array;
    private void storeSignal() {
        int listOffset = accData.size() - 1 - highT; // Exactly highT values from startI to end

        storedData = new double[highT];

        for (int i = 0; i < highT; i++) {
            storedData[i] = accData.get(i + listOffset);
        }
        Log.d("TEST", "Stored signal data");
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
