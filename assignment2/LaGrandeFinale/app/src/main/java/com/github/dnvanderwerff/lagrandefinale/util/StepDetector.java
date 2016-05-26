package com.github.dnvanderwerff.lagrandefinale.util;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Jeroen on 23/05/2016.
 */
public class StepDetector implements SensorEventListener {
    private static final float ALPHA = 0.8f;
    public static final int STEP_HANDLER_ID = 3333;

    public enum State {
        STILL,WALKING
    }

    /* Sensor */
    private Sensor accelerometer;

    /* Variables */
    private Handler stepHandler;
    private State currentState = State.STILL;
    private float[] accelVals;
    List<Double> accMagnitude;      // List of acc magnitudes within one time window
    long TimeWindow = 700;                              // Time window in ms, can be adapted
    long endOfWindow; // Set current endOfWindow

    public State getState() {
        return currentState;
    }

    public StepDetector(SensorManager sensorManager, Handler handler) {

        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        if (accelerometer == null)
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        accMagnitude = new ArrayList<>();

        endOfWindow = System.currentTimeMillis() + TimeWindow;

        stepHandler = handler;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        // Get x y z values of the accelerator
        accelVals = lowPass(event.values.clone(), accelVals, ALPHA);
        double x = (double) accelVals[0];
        double y = (double) accelVals[1];
        double z = (double) accelVals[2];

        accMagnitude.add(Math.sqrt(x*x + y*y + z*z));

        // Time window has elapsed
        if (System.currentTimeMillis() > endOfWindow) {
            // Calculate standard deviation
            double sd = sd(accMagnitude);

            if (sd <= 0.2) {
                // Change state to standing still
                currentState = State.STILL;
            } else if ((new AutoCorrelation(accMagnitude)).currentState.equals(AutoCorrelation.State.WALKING)) {
                // Change state to walking
                currentState = State.WALKING;
            }

            // Do step
            if (stepHandler != null && currentState == State.WALKING)
                stepHandler.obtainMessage(StepDetector.STEP_HANDLER_ID).sendToTarget();

            accMagnitude.clear();
            endOfWindow = System.currentTimeMillis() + TimeWindow;
        }
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
