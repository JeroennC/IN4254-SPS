package com.github.dnvanderwerff.lagrandefinale;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

/**
 * This activity was used as an attempt to measure some form of velocity, to be used for placement offset
 */
public class VelocityActivity extends AppCompatActivity implements SensorEventListener {

    private static final float ALPHA = 0.8f;
    private final AppCompatActivity act = this;

    private TextView textView;

    /* Sensor */
    private SensorManager mSensorManager;
    private Sensor accelerometer;
    private Sensor magnetometer;

    /* Tings */
    private float[] offset;
    private float[] gravVals;
    private float[] magnVals;
    private float[] velocity = new float[3];
    private float[] newVals = new float[3];
    private float[] relativeAcc = new float[4];

    // accelerometer and magnetometer based rotation matrix
    private float[] rotationMatrix = new float[16];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_velocity);

        textView = (TextView) findViewById(R.id.velocityView);

        mSensorManager =(SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
    }

    long lastTimestamp = 0;
    @Override
    public void onSensorChanged(SensorEvent event) {
        float[] vals = event.values.clone();
        switch (event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                gravVals = lowPass(vals, gravVals, ALPHA);
                // High pass the relative acc
                relativeAcc[0] = vals[0] - gravVals[0];
                relativeAcc[1] = vals[1] - gravVals[1];
                relativeAcc[2] = vals[2] - gravVals[2];
                // Low pass the relative acc?
                //relativeAcc = lowPass(newVals, relativeAcc, .7f);
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                magnVals = lowPass(vals, magnVals, ALPHA);
                break;
        }

        if (gravVals == null || magnVals == null)
            return;

        // Rotate to the world axis
        SensorManager.getRotationMatrix(rotationMatrix, null, gravVals, magnVals);

        float[] inv = new float[16];
        float[] earthAcc = new float[4];


        android.opengl.Matrix.invertM(inv, 0, rotationMatrix, 0);
        android.opengl.Matrix.multiplyMV(earthAcc, 0, rotationMatrix, 0, relativeAcc, 0);
        //earthAcc = relativeAcc;
        if (lastTimestamp == 0) {
            offset = earthAcc.clone();
            lastTimestamp = event.timestamp;
            return;
        }

        long time = event.timestamp - lastTimestamp;
        lastTimestamp = event.timestamp;

        for (int i = 0 ; i < 3; i++) {
            velocity[i] += (earthAcc[i] - offset[i]) * (time / 1000000000.f);
        }

        //earthAcc = relativeAcc;

        textView.setText(
                String.format("%.4f", earthAcc[0]) + " \n "
                        + String.format("%.4f", earthAcc[1]) + " \n "
                        + String.format("%.4f", earthAcc[2]) + "\n\n"
                        + String.format("%.4f", velocity[0]) + " \n "
                        + String.format("%.4f", velocity[1]) + " \n "
                        + String.format("%.4f", velocity[2]) + " \n "

        );


    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    /* Use a low-pass filter to avoid random high values casued by noise. Taken from https://www.built.io/blog/2013/05/applying-low-pass-filter-to-android-sensors-readings/ with slight adaptation */
    private float[] lowPass(float[] input, float[] output, float alpha) {
        if (output == null) return input;
        for (int i = 0; i < input.length; i++) {
            output[i] = alpha * output[i] + (1 - alpha) * input[i];
        }
        return output;
    }

    /* Register listeners */
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
        mSensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_GAME);
    }

    /* Unregister listeners */
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }

    /**
     * Closes ever so gracefully
     */
    private void closeGracefully(String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                act.finish();
            }
        });
        builder.setMessage(message);
        AlertDialog dialog = builder.create();
        dialog.show();
    }
}
