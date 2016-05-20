package com.github.dnvanderwerff.lagrandefinale;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import java.util.List;

public class DistanceActivity extends AppCompatActivity {

    /* Accelerator stuff */
    private SensorManager mSensorManager;
    private Sensor accelerator;
    private float[] accelVals;
    private boolean started = false;

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
            mSensorManager.registerListener((SensorEventListener) this, accelerator, SensorManager.SENSOR_DELAY_GAME);
        }
    }

    /* Register listeners */
    @Override
    protected void onResume() {
        super.onResume();
        if (!started) {
            return;
        } else {
            mSensorManager.registerListener((SensorEventListener) this, accelerator, SensorManager.SENSOR_DELAY_GAME);
        }
    }

    /* Unregister listeners */
    @Override
    protected void onPause() {
        super.onPause();
        if (!started) {
            return;
        } else {
            mSensorManager.unregisterListener((SensorEventListener) this);
        }
    }

    List<double> accMagnitude = 
    long endOfWindow = System.currentTimeMillis(); // set current endOfWindow
    double accMagnitude = 0;
    public void onSensorChanged(SensorEvent event) {

        long t = System.currentTimeMillis();               // current time

        long TimeWindow = 400; // 400 ms as time window, can be adapted
        long subTimeWindow = TimeWindow / 10; // dit is een random gok... pas nog aan

        //measure stdev,
        // Get x y and z values of the accelerator
        //accMagnitude += Math.sqrt(Math.abs(x) + Math.abs(y) + Math.abs(z)); // welk type wil ik dit hebben?

        if (t > endOfWindow) {

            // set new time window, compute stdev


        }




        // TODO doe hier wat nodig is om correcte feature te krijgen
        /*if (t > endOfWindow){
            // TimeWindow has elapsed, find min-max feature and start new window

            double min = Double.MAX_VALUE;
            double max = Double.MIN_VALUE;

            // Extract min-max feature from data
            for (Feature f: features) {
                if (f.value > max) max = f.value;
                if (f.value < min) min = f.value;
            }

            // Now find the label corresponding to the found minmax feature.
            String label = accClassifier.classify(max - min);
            accLabels[accIt++] = label;

            features.clear();
            endOfWindow = t + subTimeWindow;
        }

        if (accIt >= accMaxIt) {
            // Find most used label
            int walkCount = 0, stillCount = 0;
            for (String str : accLabels) {
                if (str.equals("walk"))
                    walkCount++;
                else
                    stillCount++;
            }
            String label = walkCount > stillCount ? "walk" : "still";
            acctext.setText(label + " " + counter++);

            accIt = 0;*/
    }

    /* Use a low-pass filter to avoid random high values casued by noise. Taken from https://www.built.io/blog/2013/05/applying-low-pass-filter-to-android-sensors-readings/ */
    // TODO hier kunnen we vast nog wel wat mee om standard deviation nauwkeuriger te bepalen
    /*private float[] lowPass(float[] input, float[] output) {
        if (output == null) return input;
        for (int i = 0; i < input.length; i++) {
            output[i] = output[i] + ALPHA * (input[i] - output[i]);
        }
        return output;
    }*/


    // TODO close gracefully function
}
