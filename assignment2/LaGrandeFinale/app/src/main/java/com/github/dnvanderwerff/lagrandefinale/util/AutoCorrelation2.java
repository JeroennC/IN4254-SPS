package com.github.dnvanderwerff.lagrandefinale.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

/**
 * Created by Jeroen on 03/06/2016.
 */
public class AutoCorrelation2 {
    private static final int msPerSample = 20;
    private static final int smoothIntervalTail = 2;
    private static final int windowSize = 30;
    private static final int minInterpeakSize = 10;
    LinkedList<Double> smoothMagnitudeHistory;
    LinkedList<Double> magnitudeHistory;
    List<Integer> indices; // Indices of peaks within smoothMagnitudeHistory

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
        synchronized (this) {
            while (smoothMagnitudeHistory.size() > listSize)
                smoothMagnitudeHistory.removeFirst();
        }

        // Only compute peaks if enough acc data has been obtained so far
        if (smoothMagnitudeHistory.size() == listSize) {

            // If peak found in a useful position (some lag perhaps), calculate autocorrelation from last two peaks
            indices = findPeaks(smoothMagnitudeHistory);

            int size = indices.size();
            if (size >= 2) {// new peaks found
                calculateAutocorrelation(indices.get(size - 2), indices.get(size - 1));
            }
        }

        //int peak1 = 15, peak2 = 42;
        //if (true) {
        //    calculateAutocorrelation(peak1,peak2);
        //}
    }

    /* Return indices of peaks found in a LinkedList */
    private List<Integer> findPeaks(LinkedList<Double> a) {

        double max = 0;

        List<Integer> indices = new ArrayList<Integer>();
        if (windowSize > (a.size() - 1)) { // Not enough data has been collected so far
            return indices;
        }
        int endWindow = windowSize;
        int delta = minInterpeakSize;
        int peakLoc = 0;
        boolean found = false;

        // Check whole signal for peaks
        for (int i = 1; i < (a.size() - 1); ) {

            // Find peak in particular time window
            for (int j = i; j < endWindow; j++) {

                double value = a.get(j);

                // Check if value is a peak
                if (a.get(j-1) < value && a.get(j+1) < value) {

                    // Check if it is the highest peak in the time window
                    if (value > max) {
                        found = true;
                        max = value;
                        peakLoc = j;
                    }
                }
            }

            // If a peak was found within the time window, add to results, else, look in next window
            if (found) {
                indices.add(peakLoc);
                found = false;
            } else {
                peakLoc = endWindow - delta;
            }

            max = 0;

            // If end of signal has been reached, returned the found peaks
            if ((peakLoc + delta + windowSize + 2) >= a.size()) {
                return indices;
            }

            i = peakLoc + delta;     // Minimal possible index for new peak
            endWindow = i + windowSize;   // Maximum possible index for new peak

        }
        return indices;
    }




    // Peak 1 should come before peak 2
    private void calculateAutocorrelation(int peak1, int peak2) {

        // Separate out f and g
        double f[] = new double[windowSize];
        double g[] = new double[windowSize];

        // TODO  ik geloof dat start en eind indices hier niet in alle situaties evenver uit elkaar liggen voor f en g
        // Get starting and ending indices
        int fstart = peak1 - offsetL < 0 ? 0 : peak1 - offsetL;
        int fend = peak1 + offsetR >= smoothMagnitudeHistory.size() ? smoothMagnitudeHistory.size() - 1 : peak1 + offsetR;
        int gstart = peak2 - offsetL < 0 ? 0 : peak2 - offsetL;
        int gend = peak2 + offsetR >= smoothMagnitudeHistory.size() ? smoothMagnitudeHistory.size() - 1 : peak2 + offsetR;

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
        correlation = getOptimalAutocorrelation(f, g, fsd, gsd);

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
