package com.github.dnvanderwerff.lagrandefinale;

import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

import com.jjoe64.graphview.*;
import com.jjoe64.graphview.series.*;

public class DistanceActivity extends AppCompatActivity implements SensorEventListener {

    /* Accelerator stuff */
    private SensorManager mSensorManager;
    private Sensor accelerator;
    private float[] accelVals;
    private boolean started = false;
    private final static float ALPHA = 0.25f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_distance);

        // Start accelerator business
        startAccelerator();
    }

    private void startAccelerator() {
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerator = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);

        // See if all can be started
        attemptStart();
    }

    /* Starts recording data if possible */
    private void attemptStart() {
        if (started) return;
        if (accelerator != null) {
            started = true;
            mSensorManager.registerListener(this, accelerator, SensorManager.SENSOR_DELAY_FASTEST);
        }
    }

    /* Register listeners */
    protected void onResume() {
        super.onResume();
        if (!started) return;
        mSensorManager.registerListener(this, accelerator, SensorManager.SENSOR_DELAY_FASTEST);
    }

    /* Unregister listeners */
    protected void onPause() {
        super.onPause();
        if (!started) return;
        mSensorManager.unregisterListener(this);
    }

    boolean walking = false;
    boolean standing = false;

    /* Start collecting data (stdevs) for walking */
    public void setWalking(View view) {
        accMagnitude.clear();
        standing = false;
        walking = true;
    }

    /* Start collecting data (stdevs) for standing still */
    public void setStanding(View view) {
        accMagnitude.clear();
        walking = false;
        standing = true;
    }

    /* Stop collecting stdev values */
    public void stopMeasurements(View view) {
        walking = false;
        standing = false;
    }

    // Values can be adapted
    int nrBins = 15; // nr of bins
    double binSize = 0.1;

    /* Show the collected values of the acc data standard deviation */
    public void showMeasurements(View view) {
        walking = false;
        standing = false;

        if (stdevStanding.isEmpty() || stdevWalking.isEmpty()) return;

        double[] yStanding = new double[nrBins]; // Defaults to array of zeroes
        double[] yWalking = new double[nrBins];
        double[] x = new double[nrBins];

        // Fill x with values to be displayed (i.e. bin centres)
        x[0] = 0.5 * binSize;
        for (int i = 1; i < nrBins; i++) {
            x[i] = x[i-1] + binSize;
        }

        List<Double> walking = new ArrayList<>(stdevWalking);
        List<Double> standing = new ArrayList<>(stdevStanding);
        // TODO: of verwijder ik zo ook daadwerkelijk stdevWalking etc?

        // Compute histograms
        for (int i = 0; i < nrBins; i++ ) {
            // Walking
            int j = 0;
            while (j < walking.size()) {
                if ((walking.get(j) >= x[i] - 0.5 * binSize) &&
                        (walking.get(j) < x[i] + 0.5 * binSize)) {
                    yWalking[i]++;
                    walking.remove(j);
                }
                j++;
            }

            // Standing still
            j = 0;
            while (j < standing.size()) {
                if ((standing.get(j) >= x[i] - 0.5 * binSize) &&
                        (standing.get(j) < x[i] + 0.5 * binSize)) {
                    yStanding[i]++;
                    standing.remove(j);
                }
                j++;
            }
        }

        // Normalize the histogram
        for (int i = 0; i < nrBins; i++) {
            yWalking[i] /= (double) stdevWalking.size();
            yStanding[i] /= (double) stdevStanding.size();
        }

        // Plot (x, yWalking) and (x, yStanding)
        GraphView graph = (GraphView) findViewById(R.id.graph);
        DataPoint[] dataWalking = new DataPoint[] {};
        DataPoint[] dataStanding = new DataPoint[] {};

        LineGraphSeries<DataPoint> seriesWalking = new LineGraphSeries<>(dataWalking);
        LineGraphSeries<DataPoint> seriesStanding = new LineGraphSeries<>(dataStanding);

        for (int i = 0; i < nrBins; i++) {
            seriesWalking.appendData(new DataPoint(x[i], yWalking[i]), true, nrBins);
            seriesStanding.appendData(new DataPoint(x[i], yStanding[i]), true, nrBins);
        }

        graph.removeAllSeries();;
        seriesWalking.setColor(Color.GREEN);
        graph.addSeries(seriesWalking);
        seriesStanding.setColor(Color.BLUE);
        graph.addSeries(seriesStanding);

    }

    /* Compute standard deviation of a list */
    public double sd (List<Double> a){
        double sum = 0;
        double mean = 0;

        // Compute mean
        for (double i : a)
            mean += i;
        mean = mean / a.size();

        // Compute stdev
        for (double i : a)
            sum += Math.pow((i - mean), 2);

        return Math.sqrt( sum / a.size() );
    }

    List<Double> accMagnitude = new ArrayList<>();      // List of acc magnitudes within one time window
    List<Double> stdevWalking = new ArrayList<>();      // List of stdevs per time window for walking
    List<Double> stdevStanding = new ArrayList<>();     // List of stdevs per time windows for standing still

    long TimeWindow = 200;                              // Time window in ms, can be adapted
    long endOfWindow = System.currentTimeMillis() + TimeWindow; // Set current endOfWindow

    /* On sensor changed  */
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (!walking && !standing) return;

        // Get x y z values of the accelerator
        accelVals = lowPass(event.values.clone(), accelVals);
        double x = (double) accelVals[0];
        double y = (double) accelVals[1];
        double z = (double) accelVals[2];

        accMagnitude.add(Math.sqrt(x*x + y*y + z*z));

        // Time window has elapsed
        if (System.currentTimeMillis() > endOfWindow) {

            // Add stdev to correct data list
            if (walking) {
                stdevWalking.add(sd(accMagnitude));
            } else if (standing) {
                stdevStanding.add(sd(accMagnitude));
            }

            accMagnitude.clear();
            endOfWindow = System.currentTimeMillis() + TimeWindow;
        }
    }

    /* Use a low-pass filter to avoid random high values casued by noise. Taken from https://www.built.io/blog/2013/05/applying-low-pass-filter-to-android-sensors-readings/ */
    private float[] lowPass(float[] input, float[] output) {
        if (output == null) return input;
        for (int i = 0; i < input.length; i++) {
            output[i] = output[i] + ALPHA * (input[i] - output[i]);
        }
        return output;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Not needed
    }

    // TODO close gracefully function
}
