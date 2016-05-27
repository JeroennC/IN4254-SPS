package com.github.dnvanderwerff.lagrandefinale;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class AccelerationMeasure extends AppCompatActivity implements SensorEventListener {

    public final static int PERM_REQ_EXTWRITE = 1;
    private static final float ALPHA = 0.8f;
    private final AppCompatActivity act = this;
    private float[] accelVals;
    private boolean canWrite = false;
    private TextView samplesView;

    /* Sensor */
    private SensorManager mSensorManager;
    private Sensor accelerometer;

    /* File stuff */
    private File file;
    private FileOutputStream fileStream;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_acceleration_measure);

        mSensorManager =(SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        if (accelerometer == null)
            accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        samplesView = (TextView) findViewById(R.id.samplesWritten);
        samplesView.setText("0 samples");

        // Get file location
        File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        file = new File(path, "accelerometer2705.dat");

        // Write file permissions
        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            // Request permission
            ActivityCompat.requestPermissions(this,
                    new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE
                    }
                    , PERM_REQ_EXTWRITE);
        } else {
            openFile();
        }
    }


    private long sampleNo;
    @Override
    public void onSensorChanged(SensorEvent event) {
        // Get x y z values of the accelerator
        accelVals = lowPass(event.values.clone(), accelVals, ALPHA);
        double x = (double) accelVals[0];
        double y = (double) accelVals[1];
        double z = (double) accelVals[2];

        double magnitude = Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2) + Math.pow(z, 2));
        if (canWrite) {
            try {
                fileStream.write((sampleNo + "|" + event.timestamp + "|" + String.format("%.2f", magnitude) + "|" + String.format("%.2f", x) + "|" + String.format("%.2f", y) + "|" + String.format("%.2f", z) + "\n").getBytes());
                fileStream.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
            sampleNo++;
            samplesView.setText(sampleNo + " samples");
        }
    }

    // Permissions callback
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERM_REQ_EXTWRITE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openFile();
                } else {
                    // Well damnit :(
                    closeGracefully("No file writing permissions");
                }
                return;
            }
        }
    }

    // Opens file to write to
    private void openFile() {
        try {
            fileStream = new FileOutputStream(file, true);
            Log.i("openFile", "Data Saved to " + file.getAbsolutePath());
        } catch (IOException e) {
            Log.e("SAVE DATA", "Could not write file " + e.getMessage());
            closeGracefully("Error opening file");
            return;
        }

        canWrite = true;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) { }

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
