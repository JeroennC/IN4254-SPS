package com.github.dnvanderwerff.lagrandefinale;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Color;
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
import android.view.View;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.jjoe64.graphview.*;
import com.jjoe64.graphview.series.*;


/**
 * Measures the accelerometer data while walking/standing (based on user input)
 * Collects this information to display a chart of the separation of magnitudes
 */
public class DistanceActivity extends AppCompatActivity implements SensorEventListener {

    public final static int PERM_REQ_EXTWRITE = 1;
    private final static float ALPHA = 0.25f;
    private final AppCompatActivity act = this;
    private float[] accelVals;
    private boolean canWrite = false;
    private boolean started = false;

    /* Sensor stuff */
    private SensorManager mSensorManager;
    private Sensor accelerator;

    /* File stuff */
    private File file;
    private FileOutputStream fileStream;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_distance);

        // Start accelerator business
        startAccelerator();

        // Get file location
        File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        file = new File(path, "stdev.dat");

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

    /* Histogram stuff, values can be adapted */
    int nrBins = 30; // Initial nr of bins
    double maxValWalking = 0;
    double binSize = 0.05;

    /* Show the collected values of the acc data standard deviation */
    public void showMeasurements(View view) {
        walking = false;
        standing = false;

        if (stdevStanding.isEmpty() || stdevWalking.isEmpty()) return;

        // Compute max values in stdevWalking array
        maxValWalking = 0;
        for (double i : stdevWalking) {
            if (i > maxValWalking) {
                maxValWalking = i;
            }
        }

        int standingSamples = stdevStanding.size();
        int walkingSamples = stdevWalking.size();

        // Set nr of bins for histogram
        nrBins = (int) Math.ceil(maxValWalking / binSize);

        double[] yStanding = new double[nrBins]; // Defaults to array of zeroes
        double[] yWalking = new double[nrBins];
        double[] x = new double[nrBins];

        // Fill x with values to be displayed (i.e. bin centres)
        x[0] = 0.5 * binSize;
        for (int i = 1; i < nrBins; i++) {
            x[i] = x[i - 1] + binSize;
        }

        // Compute histograms
        for (double i : stdevWalking) {
            int index = (int) Math.floor(i / binSize); // Bin nr where stdevWalking belongs to
            yWalking[index]++;
        }

        for (double i : stdevStanding) {
            int index = (int) Math.floor(i / binSize); // Bin nr where stdevStanding belongs to
            yStanding[index]++;
        }

        // Normalize the histograms
        for (int i = 0; i < nrBins; i++) {
            yWalking[i] /= (double) stdevWalking.size();
            yStanding[i] /= (double) stdevStanding.size();
        }

        // Show optimum value of sigma
        double sig = x[min(yWalking, yStanding)];

        // Compute accuracy
        double still_belowthresh = 0;
        double still_abovethresh = 0;

        for (int i = 0; i < nrBins; i++) {
            if (x[i] < sig) {
                still_belowthresh += yStanding[i];
            } else {
                still_abovethresh += yStanding[i];
            }
        }

        TextView sigma = (TextView) findViewById(R.id.sigma);
        String test = "Sig: " + sig + " - Below: " + still_belowthresh + " - Above: " + still_abovethresh + " - Nr samples walk: " + walkingSamples + " - still: " + standingSamples + ".";
        sigma.setText(test);

        // Plot (x, yWalking) and (x, yStanding)
        GraphView graph = (GraphView) findViewById(R.id.graph);
        DataPoint[] dataWalking = new DataPoint[]{};
        DataPoint[] dataStanding = new DataPoint[]{};

        LineGraphSeries<DataPoint> seriesWalking = new LineGraphSeries<>(dataWalking);
        LineGraphSeries<DataPoint> seriesStanding = new LineGraphSeries<>(dataStanding);

        for (int i = 0; i < nrBins; i++) {
            seriesWalking.appendData(new DataPoint(x[i], yWalking[i]), true, nrBins);
            seriesStanding.appendData(new DataPoint(x[i], yStanding[i]), true, nrBins);
        }

        graph.removeAllSeries();
        seriesWalking.setColor(Color.GREEN);
        graph.addSeries(seriesWalking);
        seriesStanding.setColor(Color.BLUE);
        graph.addSeries(seriesStanding);
    }

    /* Adds two equally sized arrays and returns the index of the minimum value of the resulting array */
    private int min(double[] a, double[] b) {
        double min = Integer.MAX_VALUE;
        int index = 0;

        for (int i = 0; i < a.length; i++) { // TODO maybe adjust boundaries to be completely sure that actual value is returned, and not values on the far right side of plot for example
            if (a[i] + b[i] < min) {
                min = a[i] + b[i];
                index = i;
            }
        }
        return index;
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

    long TimeWindow = 700;                              // Time window in ms, can be adapted
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

            if (canWrite) {
                try {
                    if (walking) {
                        fileStream.write(("W|" + String.format("%.2f", sd(accMagnitude)) + "\n").getBytes());
                    } else if (standing) {
                        fileStream.write(("S|" + String.format("%.2f", sd(accMagnitude)) + "\n").getBytes());
                    }
                    fileStream.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
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
