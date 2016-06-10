package com.github.dnvanderwerff.lagrandefinale.util;

import android.os.Handler;
import android.util.Log;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

/**
 * Created by Jeroen on 03/06/2016.
 */
public class AutoCorrelation2 {
    private static final int msPerSample = 20;                  // Amount of ms elapsed per sample
    private static final int smoothIntervalTail = 2;            // Parameter used for sizing magnitudeHistory
    private static final int windowSize = 30;                   // Nr of sample per window (window in which we search for a peak)
    private static final int minTimeWindow = 400, maxTimeWindow = 1000;
    private static final int minSampleWindow = minTimeWindow / msPerSample, maxSampleWindow = maxTimeWindow / msPerSample;
    private static final int minListSize = 80;
    // Peak offsets used
    private static final int offsetR = 15, offsetL = 15;        // Nr of samples from peak center
    private static final int peakWindow = offsetR + offsetL;    // Nr of samples used around one peak
    private static final int minInterpeakSize = minSampleWindow;
    LinkedList<Double> smoothMagnitudeHistory;                  // List of smoothed acc magnitude values
    LinkedList<Double> magnitudeHistory;                        // List of acc magnitude values
    List<Integer> indices;                                      // Indices of peaks within smoothMagnitudeHistory

    private int optimalSamplesBetween;
    private int optimalTimeWindow;                  // in ms
    private int certaintySampleWindow;              // in samples
    private int listSize = 0;
    private double correlation;
    private int lastPeak = - minInterpeakSize + 2;  // Parameter used for detectPeaks, keeps track of last peak index
    private Handler stepHandler;                    // Handler to send steps

    public double getCorrelation() {
        return correlation;
    }

    public int getOptimalTimeWindow() { return optimalTimeWindow; }

    public AutoCorrelation2(Handler handler) {
        optimalTimeWindow = 600;
        optimalSamplesBetween = optimalTimeWindow / msPerSample;
        certaintySampleWindow = 5;
        setListSize();

        stepHandler = handler;

        correlation = 0;

        smoothMagnitudeHistory = new LinkedList<>();
        magnitudeHistory = new LinkedList<>();
    }

    private int counter = 0;
    private int c2 = 0;

    public void addMagnitude(double magnitude) {

        // TODO wtf bruhh waarom werkt dit niet D=

        // Adjust lastPeak to compensate for the shift of smoothMagnitudeHistory
        counter++;

        magnitudeHistory.add(magnitude);
        if (magnitudeHistory.size() > smoothIntervalTail * 2 + 1)
            magnitudeHistory.removeFirst();

        // Moving average
        smoothMagnitudeHistory.add(getAverage(magnitudeHistory));

        // Reduce smoothed list size
        while (smoothMagnitudeHistory.size() > listSize) {
            smoothMagnitudeHistory.removeFirst();
            lastPeak--;
            if (lastPeak < - minInterpeakSize + 1) lastPeak = - minInterpeakSize +1;
        }

        int newPeak = detectPeak(lastPeak,smoothMagnitudeHistory);
        Log.d("Smooth data" , counter + "," + String.format("%.2f",smoothMagnitudeHistory.getLast()));
        if (newPeak != -1) {

            // Check if peaks are not too far apart and lastPeak not too big
            if (newPeak - lastPeak <= maxSampleWindow && lastPeak >= offsetL) {
                // Two peaks have been found close to each other, compute their autocorrelation
                if (calculateAutocorrelation(lastPeak, newPeak)
                        && stepHandler != null) {
                    stepHandler.obtainMessage(StepDetector.STEP_HANDLER_ID).sendToTarget();
                    Log.d("autocorr",counter + "," + String.format("%.2f", correlation));
                }
            }

            // Separate out f and g
            double f[] = new double[peakWindow];

            // Get starting and ending indices
            int fstart = newPeak - offsetL < 0 ? 0 : newPeak - offsetL;
            int fend = newPeak + offsetR > smoothMagnitudeHistory.size() ? smoothMagnitudeHistory.size() : newPeak + offsetR;

            // Iterate through the list and extract values to f and g
            ListIterator<Double> it = smoothMagnitudeHistory.listIterator(fstart);
            int index = fstart;
            double val;
            while(it.hasNext()) {
                val = it.next();
                if (index < fend)
                    f[index - fstart] = val;
                index++;
            }

            // Calculate the means and standard deviations
            double fmean = mean(f);
            double fsd = sd(f, fmean);

            // Don't use STILL data as it gives very bad things!
            if (fsd >= StepDetector.STANDARD_DEV_WALKING_THRESHOLD) {
                lastPeak = newPeak;
            }

        }



        // Only compute peaks if enough acc data has been obtained so far
        /*if (smoothMagnitudeHistory.size() == listSize && counter++ == windowSize / 3) {
            counter = 0;
            // If peak found in a useful position (some lag perhaps), calculate autocorrelation from last two peaks
            indices = findPeaks(smoothMagnitudeHistory);

            int size = indices.size();
            if (size >= 2) {// new peaks found
                calculateAutocorrelation(indices.get(size - 2), indices.get(size - 1));
            }
        }*/
    }

    /* Returns index of highest peak after lastPeak within smoothMagnitudes */
    private int detectPeak(int lastPeak, LinkedList<Double> smoothMagnitudes) {
        // Find start and end indices
        int startIndex = lastPeak + minInterpeakSize;
        if (startIndex + 1 > smoothMagnitudes.size()) return -1;

        int endIndex = smoothMagnitudes.size() - offsetR;

        // TODO  threshold? -> adapt max below

        int i = 1;
        double max  = 2.0;
        int peakLoc = -1;
        Iterator<Double> it = smoothMagnitudes.listIterator(startIndex-1);
        double previous = it.next();
        double current = it.next();
        double next;

        while (it.hasNext() && startIndex + i < endIndex ) {

            next = it.next();

            // Check if value is a peak
            if (previous < current && next < current) {

                // Check if it is the highest peak in the time window
                if (current > max) {
                    max = current;
                    peakLoc = startIndex + i;
                }
            }
            if (counter >= 185 && counter <= 209) {
                Log.d("whathappen","" + i + "," + String.format("%.2f,%.2f,%.2f,%.2f,%d", max, previous, current, next, peakLoc));
            }
            i++;
            previous = current;
            current = next;
        }

        Log.d("Peak detected" , counter + "," + peakLoc + "," + String.format("%.2f", max) +"," + startIndex + "," + endIndex );
        return peakLoc;

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
            if ((peakLoc + delta) >= a.size()) {
                return indices;
            }

            i = peakLoc + delta;                                  // Minimal possible index for new peak
            endWindow = Math.min(i + windowSize, a.size() - 1);   // Maximum possible index for new peak

        }
        return indices;
    }




    // Peak 1 should come before peak 2
    private boolean calculateAutocorrelation(int peak1, int peak2) {
        int samplesBetweenPeaks = peak2 - peak1;
        // Separate out f and g
        double f[] = new double[peakWindow];
        double g[] = new double[peakWindow];

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

        // Don't use STILL data as it gives very bad things!
        if (fsd < StepDetector.STANDARD_DEV_WALKING_THRESHOLD || gsd < StepDetector.STANDARD_DEV_WALKING_THRESHOLD ) {
            return false;
        }

        // Turn f and g into f' and g' by subtracting the mean
        // NOTE: f and g are equal length
        for (int i = 0; i < f.length; i++) {
            f[i] = f[i] - fmean;
            g[i] = g[i] - gmean;
        }

        // Find the optimal autocorrelation
        double new_correlation = getOptimalAutocorrelation(f, g, fsd, gsd, samplesBetweenPeaks);

        if (new_correlation == 0)
            return false;

        correlation = new_correlation;

        // Get corresponding timewindow if correlation is high enough
        if (correlation >= StepDetector.CORRELATION_WALKING_THRESHOLD) {
            optimalSamplesBetween = samplesBetweenPeaks - peakWindow + optimalI + 1;
            optimalTimeWindow = optimalSamplesBetween * 20; // Samples times 20 ms
            Log.d("OPTIMALTIMEWINDOW", counter + "," + optimalTimeWindow);
            setListSize();
            return true;
        }

        return false;
    }

    int optimalI = -1;
    /* Shift g over f and try to find the optimal autocorrelation */
    private double getOptimalAutocorrelation(double f[], double g[], double fsd, double gsd, int samplesBetweenPeaks) {
        double optimalCorrelation = 0;
        double val;

        // Calculate i for optimalSamplesBetween
        int optI = optimalSamplesBetween - samplesBetweenPeaks + peakWindow - 1;
        // Start and end of certainty window
        int startIndex, endIndex;
        if (optimalI == -1) {
            // First time, take full bounds
            startIndex = 0;
            endIndex = windowSize * 2 - 1;
        } else {
            startIndex = optI < certaintySampleWindow ? 0 : optI - certaintySampleWindow;
            endIndex = optI + certaintySampleWindow > peakWindow * 2 - 1 ? peakWindow * 2 - 1 : optI + certaintySampleWindow;
        }

        // Bound the indices by the minSampleWindow, maxSampleWindow
        int minI = minSampleWindow - samplesBetweenPeaks + peakWindow - 1;
        int maxI = maxSampleWindow - samplesBetweenPeaks + peakWindow - 1;
        startIndex = startIndex < minI ? minI : startIndex;
        endIndex   = endIndex   > maxI ? maxI : endIndex;


        double corrSum = 0;
        for (int i = startIndex; i < endIndex; i++) {
            val = getAutocorrelation(f, g, fsd, gsd
                    ,-peakWindow + i + 1 < 0 ? 0 :-peakWindow + i + 1   // fi
                    , peakWindow - i - 1 < 0 ? 0 : peakWindow - i - 1); // gi
            corrSum += Math.abs(val);
            if (val > optimalCorrelation) {
                optimalCorrelation = val;
                optimalI = i;
            }
        }
        Log.d("autocorr functie", counter + "," + String.format(".2f", optimalCorrelation));
        return optimalCorrelation;
    }

    /* Calculate autocorrelation with f and g, with gi and fi above eachother. Either gi or fi should alwayss be 0 */
    private double getAutocorrelation(double f[], double g[], double fsd, double gsd, int fi, int gi) {
        double result = 0;
        int overlap = fi == 0 ? g.length - gi : f.length - fi;

        double sumf = 0;
        double sumg = 0;
        double meanf = 0;
        double meang = 0;

        // Compute stdev

        for (int i = 0; i < overlap; i++) {
            result += f[fi + i] * g[gi + i];
            meanf += f[fi+i];
            meang += g[gi+i];
        }
        meanf /= overlap;
        meang /= overlap;

        for (int i = 0; i < overlap; i++) {
            sumf += Math.pow((f[fi+i] - meanf),2);
            sumg += Math.pow((g[gi+i] - meang),2);
        }

        fsd = sumf /overlap;
        gsd = sumg /overlap;

        // Divide by the standard deviations
        result /= Math.sqrt(fsd * gsd);

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

    /* Set nr of samples needed to calculate autocorrelation */
    private void setListSize() {
        listSize = 5 * optimalTimeWindow / msPerSample;
        if (listSize < minListSize)
            listSize = minListSize;
    }
}
