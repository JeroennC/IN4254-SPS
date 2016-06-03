package com.github.dnvanderwerff.lagrandefinale.util;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;

/**
 * Created by Jeroen on 03/06/2016.
 */
public class AutoCorrelation2 {
    private static final int msPerSample = 20;
    private static final int smoothIntervalTail = 2;
    private static final int windowSize = 30;
    LinkedList<Double> smoothMagnitudeHistory;
    LinkedList<Double> magnitudeHistory;

    private int optimalTimeWindow; // in ms
    private int certaintySampleWindow; // in samples
    private int listSize;
    private int offsetR, offsetL;
    private double correlation;

    public double getCorrelation() {
        return correlation;
    }

    public AutoCorrelation2() {
        optimalTimeWindow = 600;
        certaintySampleWindow = 5;
        setListSize();

        correlation = 0;

        // Peak offsets used
        offsetL = 15;
        offsetR = 15;

        smoothMagnitudeHistory = new LinkedList<>();
        magnitudeHistory = new LinkedList<>();
    }

    public void addMagnitude(double magnitude) {
        magnitudeHistory.add(magnitude);
        if (magnitudeHistory.size() > smoothIntervalTail * 2 + 1)
            magnitudeHistory.removeFirst();

        // Moving average
        smoothMagnitudeHistory.add(getAverage(magnitudeHistory));

        // Reduce smoothed list size
        while (smoothMagnitudeHistory.size() > listSize)
            smoothMagnitudeHistory.removeFirst();

        // If peak found in a useful position (some lag perhaps), calculate autocorrelation from last two peaks
        // TODO find peaks
        int peak1 = 15, peak2 = 45;
        if (true) // new peaks found
            calculateAutocorrelation(peak1, peak2);
    }

    // Peak 1 should come before peak 2
    private void calculateAutocorrelation(int peak1, int peak2) {
        //int peak1 = 5;
        //int peak2 = 15;

        // Separate out f and g
        double f[] = new double[windowSize];
        double g[] = new double[windowSize];

        // Get starting and ending indices
        int fstart = peak1 - offsetL < 0 ? 0 : peak1 - offsetL;
        int fend = peak1 + offsetR > smoothMagnitudeHistory.size() ? smoothMagnitudeHistory.size() : peak1 + offsetR;
        int gstart = peak2 - offsetL < 0 ? 0 : peak2 - offsetL;
        int gend = peak2 + offsetR > smoothMagnitudeHistory.size() ? smoothMagnitudeHistory.size() : peak2 + offsetR;

        // Iterate through the list and extract values to f and g
        ListIterator<Double> it = smoothMagnitudeHistory.listIterator(fstart);
        int index = fstart;
        double val;
        while(it.hasNext()) {
            val = it.next();
            if (index < fend)
                f[index - fstart] = val;
            if (index >= gstart && index < gend)
                g[index - gstart] = val;
            index++;
        }

        // Calculate the means and standard deviations
        double fmean = mean(f);
        double gmean = mean(g);
        double fsd = sd(f, fmean);
        double gsd = sd(g, gmean);

        // Turn f and g into f' and g' by subtracting the mean
        // NOTE: f and g are equal length
        for (int i = 0; i < f.length; i++) {
            f[i] = f[i] - fmean;
            g[i] = g[i] - gmean;
        }

        // Find the optimal autocorrelation
        correlation = getOptimalAutocorrelation(f,g, fsd, gsd);

        // Get corresponding timewindow
        int samplesBetween = (peak2 - peak1) // time between peaks
                - (g.length - optimalI - 1);

        optimalTimeWindow = samplesBetween * 20; // Samples times 20 ms
        setListSize();
    }

    int optimalI = -1;
    /* Shift g over f and try to find the optimal autocorrelation */
    private double getOptimalAutocorrelation(double f[], double g[], double fsd, double gsd) {
        double optimalCorrelation = Double.MIN_VALUE;
        double val;
        // Calculate start and end indices
        int startIndex = optimalI < certaintySampleWindow ? 0 : optimalI - certaintySampleWindow;
        int endIndex = optimalI == -1 || optimalI + certaintySampleWindow > g.length * 2 - 1 ? g.length * 2 - 1 : optimalI + certaintySampleWindow;

        // TODO check if this for loop can be decreased if the optimal time window is known
        for (int i = startIndex; i < endIndex; i++) {
            val = getAutocorrelation(f, g, fsd, gsd
                    ,-g.length + i + 1 < 0 ? 0 :-g.length + i + 1
                    , g.length - i - 1 < 0 ? 0 : g.length - i - 1);
            if (val > optimalCorrelation) {
                optimalCorrelation = val;
                optimalI = i;
            }
        }

        return optimalCorrelation;
    }

    /* Calculate autocorrelation with f and g, with gi and fi above eachother. Either gi or fi should alwayss be 0 */
    private double getAutocorrelation(double f[], double g[], double fsd, double gsd, int fi, int gi) {
        double result = 0;
        int overlap = fi == 0 ? g.length - gi : f.length - fi;

        for (int i = 0; i < overlap; i++) {
            result += f[fi + i] * g[gi + i];
        }

        // Divide by the standard deviations
        result /= fsd * gsd;

        // Normalize and return
        return result / overlap;
    }

    private double getAverage(LinkedList<Double> list) {
        ListIterator<Double> it = list.listIterator();
        double val = 0;
        while (it.hasNext()) {
            val += it.next();
        }

        return val / list.size();
    }

    /* Compute mean of an array */
    private double mean (double a[]) {
        double mean = 0;

        for (int i = 0; i < a.length; i++) {
            mean += a[i];
        }
        mean /= a.length;

        return mean;
    }

    /* Compute standard deviation of an array */
    private double sd (double a[], double mean){
        double sum = 0;

        // Compute stdev
        for (int i = 0; i < a.length; i++)
            sum += Math.pow((a[i] - mean), 2);

        return Math.sqrt( sum / a.length );
    }

    private void setListSize() {
        listSize = 3 * optimalTimeWindow / msPerSample;
    }
}
