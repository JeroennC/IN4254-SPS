package com.github.dnvanderwerff.lagrandefinale.util;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Danielle on 26-5-16.
 */


/* AutoCorrelation class. Upon creation of an AutoCorrelation object for a certain accelerator
 * signal, this class immediately determines the corresponding state (STILL or
 * WALKING) and if necessary the period of walking. All based on normalized auto-correlation
 * as described by http://research.microsoft.com/pubs/166309/com273-chintalapudi.pdf.
 */
public class AutoCorrelation {

    public static final int tMin = 20, tMax = 50; // TODO pas deze waarden aan naar iets logisch
    public static final double WALKING_THRESHOLD = 0.7;
    private List<Double> accData;   // Accelerator signal
    public State currentState;      // Activity state of user
    public int optPeriod;           // Equals step periodicity of user if the user is walking, 0 otherwise.
    public double autocorr;

    public enum State {
        STILL,WALKING
    }

    /* Class to contain the result of norm. auto-correlation: max value of psi on the interval
     * tMin to tMax and the corresponding period.
     */
    private class Result {

        public int period;
        public double max;

        public Result(int p, double m) {
            this.period = p;
            this.max = m;
        }
    }

    /* Constructor */
    public AutoCorrelation(List<Double> accData) {
        this.accData = accData;
        this.optPeriod = 30;
        this.currentState = State.STILL;
        this.autocorr = 0;

        // Set state of user for given accData window
        setState();
    }

    public void addData(double data) {
        this.accData.add(data);
        if (accData.size() > 2 * tMax + 2) {
            this.accData.remove(0);
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
            Result res = maxNormAutoCorrelation(0, tMin, tMax);
            this.autocorr = res.max;
            if (res.max > WALKING_THRESHOLD) {
                this.currentState = State.WALKING;
                this.optPeriod = res.period;
            } else {
                this.currentState = State.STILL;
            }
            Log.d("autocorr_res", String.format("optPeriod " + this.optPeriod + ", AutoCorr %.2f", res.max));

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

        X /= (tau * sd(m, tau) * sd(m + tau, tau));
        Log.d("X",String.format("%.2f",X));
        return X;
    }

    /* Compute the maximum normalized auto-correlation psi(m) for lags between tauMin and tauMax
     * for sample m. Corresponding period is returned as well. */
    public Result maxNormAutoCorrelation(int m, int tauMin, int tauMax) {

        double max = 0;
        int optPeriod = 0;

        for (int t = tauMin; t <= tauMax; t++) {
            double x = X(m, t);
            if (x > max) {
                max = x;
                optPeriod = t;
            }
        }
        Log.d("maxNorm", String.format("Max %.2f optPeriod " + optPeriod,max));
        return new Result(optPeriod, max);
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
