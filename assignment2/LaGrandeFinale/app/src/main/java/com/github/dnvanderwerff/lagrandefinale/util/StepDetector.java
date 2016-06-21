package com.github.dnvanderwerff.lagrandefinale.util;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.util.Log;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 *  Combines the standard deviation check and the autocorrelation check to detect steps being taken. When a step is detected, tells the MapActivity to do a step.
 */
public class StepDetector implements SensorEventListener {
    private static final float ALPHA = 0.8f;
    public static final int STEP_HANDLER_ID = 3333;
    public static final double STANDARD_DEV_WALKING_THRESHOLD = 0.2;
    public static final double CORRELATION_WALKING_THRESHOLD = 0.4;

    public enum State {
        STILL,WALKING
    }

    /* Sensor */
    private Sensor accelerometer;

    /* Autocorrelation */
    private AutoCorrelation corr;

    /* Variables */
    private Handler stepHandler;
    private State currentState = State.STILL;
    private float[] accelVals;
    List<Double> accMagnitude;      // List of acc magnitudes within one time window
    List<Double> valueBuffer;       // Buffer of magnitudes while paused
    long TimeWindow = 600;          // Time window in ms, can be adapted
    int sampleWindow = 30;
    long endOfWindow;               // Set current endOfWindow
    int sampleCount;
    private boolean paused = false;

    public State getState() {
        return currentState;
    }

    public double getCorrelation() {

        if (currentState.equals(State.STILL)) {
            return 0.0;
        }
        return corr.getAutoCorrelation();
    }

    public int getOptimalTimeWindow() { return corr.optPeriod; }

    public StepDetector(SensorManager sensorManager, Handler handler) {

        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        if (accelerometer == null)
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        accMagnitude = new ArrayList<>();

        endOfWindow = System.currentTimeMillis() + TimeWindow;
        sampleCount = 0;

        stepHandler = handler;

        corr = new AutoCorrelation(new ArrayList<Double>());

    }

    public void pauseSensor() {
        paused = true;
        valueBuffer = new LinkedList<>();
    }

    public void resumeSensor() {
        paused = false;
        Iterator<Double> it = valueBuffer.iterator();
        while (it.hasNext())
            handleValue(it.next());
    }

    private void handleValue(double magnitude) {
        sampleCount++;
        accMagnitude.add(magnitude);
        corr.addData(magnitude);


        // Time window has elapsed
        if (sampleCount >= sampleWindow) {
            sampleCount = 0;
            // Calculate standard deviation
            double sd = sd(accMagnitude);

            if (sd <= STANDARD_DEV_WALKING_THRESHOLD) {
                // Change state to standing still
                currentState = State.STILL;
            } else if (corr.getAutoCorrelation() > CORRELATION_WALKING_THRESHOLD) {
                // Change state to walking
                currentState = State.WALKING;
                TimeWindow = corr.optPeriod * 20
                        / 2 // if 2 steps
                ; // Convert from nr of samples to ms
                sampleWindow = corr.optPeriod
                        / 2 // if 2 steps
                ;
            }

            // Do step
            if (stepHandler != null && currentState == State.WALKING)
                stepHandler.obtainMessage(StepDetector.STEP_HANDLER_ID).sendToTarget();

            accMagnitude.clear();
            endOfWindow = System.currentTimeMillis() + TimeWindow;
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        // Get x y z values of the accelerator
        accelVals = lowPass(event.values.clone(), accelVals, ALPHA);
        double magnitude = Math.sqrt(Math.pow(accelVals[0], 2) + Math.pow(accelVals[1], 2) + Math.pow(accelVals[2], 2));

        if (!paused)
            handleValue(magnitude);
        else
            valueBuffer.add(magnitude);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) { }

    public void registerListeners(SensorManager sensorManager) {
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
    }

    /* Use a low-pass filter to avoid random high values casued by noise. Taken from https://www.built.io/blog/2013/05/applying-low-pass-filter-to-android-sensors-readings/ with slight adaptation */
    private float[] lowPass(float[] input, float[] output, float alpha) {
        if (output == null) return input;
        for (int i = 0; i < input.length; i++) {
            output[i] = alpha * output[i] + (1 - alpha) * input[i];
        }
        return output;
    }

    /* Compute standard deviation of a list */
    private double sd (List<Double> a){
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
}
