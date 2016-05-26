package com.github.dnvanderwerff.lagrandefinale.util;

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

    public static final int tMin = 40, tMax = 100; // Voorlopig even waarden uit paper gehaald,
                                                // juiste waarden hangen af van sampling freq van sensor

    private List<Double> accData;   // Accelerator signal
    public State currentState;      // Activity state of user
    public int optPeriod;           // Equals step periodicity of user if the user is walking, 0 otherwise.

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
        this.optPeriod = 0;
        this.currentState = State.STILL;

        // Set state of user for given accData window
        setState();
    }

    /* Set state of user for sample m by checking value of psi(m) */
    public void setState() {

        int m = this.accData.size();

        int walkingCount = 0;
        int optPeriod = 0;

        for (int i = 0; i < (m - 2*tMax); i++) {
            Result res = maxNormAutoCorrelation(i, tMin, tMax);

            if(res.max > 0.7) { // TODO voorlopig gelijk aan 0.7 uit paper, check of dit daadwerkelijk beste waarde is
                walkingCount++;
                optPeriod += res.period;
            }
        }

        // If majority of tested samples indicates WALKING, adjust state and period of user
        if (walkingCount > (m - 2*tMax)/2) {
            this.currentState = State.WALKING;
            this.optPeriod = optPeriod / (m - 2*tMax);
        }
    }

    /* Compute normalized auto-correlation X(m,tau) of accData for sample m and lag tau */
    public double X(int m, int tau) {
        double X = 0;

        for (int k = 0; k < tau; k++) {
            X += (accData.get(m+k) - mean(m, tau)) * (accData.get(m+k+tau) - mean(m+tau, tau));
        }

        X /= tau * sd(m, tau) * sd(m + tau, tau);

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

        return new Result(optPeriod, max);
    }


    /* Compute standard deviation of samples k to l-1 of a list */
    private double sd (int k, int l){
        double sum = 0;
        double mean = mean(k, l);
        List<Double> a = this.accData;

        // Compute stdev
        for (int i = k; i < l; i++) {
            sum += Math.pow((a.get(i) - mean), 2);
        }

        return Math.sqrt( sum / (l - k) );
    }

    /* Compute mean of of samples k to l-1 of a list */
    private double mean (int k, int l) {
        double mean = 0;
        List<Double> a = this.accData;

        for (int i = k; i < l; i++) {
            mean += a.get(i);
        }
        mean /= (l - k);

        return mean;
    }

    // TODO  sample m uit paper, is dat array[m] (dus basically element m+1) of gewoon echt het m'de element? Voor nu is array[m] gebruikt,
    // maar dit zou er dus 1 sample naast kunnen zitten
}
