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
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class MagneticMeasurerActivity extends AppCompatActivity implements SensorEventListener {
    public final static int PERM_REQ_EXTWRITE = 1;
    private final AppCompatActivity act = this;
    private boolean canWrite = false;

    /* GUI vars */
    private TextView degreesView;
    private EditText cellEdit;

    /* File stuff */
    private File magnFile;
    private FileOutputStream magnFileStream;

    /* Sensor stuff */
    private SensorManager mSensorManager;
    private Sensor orientation;
    private float currentDegree;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_magnetic_measurer);

        // Set GUI refs
        degreesView = (TextView) findViewById(R.id.currentDegrees);
        cellEdit = (EditText) findViewById(R.id.cell);

        // Get magnetometer
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        orientation = mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);

        // Get file location
        File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        magnFile = new File(path, "magneto.dat");

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

    /* Store the current degree with cell in file */
    public void StoreDegree(View view) {
        String cell = cellEdit.getText().toString();
        String input = cell + "\t" + String.format("%.1f", currentDegree) + "\n";
        try {
            magnFileStream.write(input.getBytes());
            magnFileStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Sensor listener (http://www.codingforandroid.com/2011/01/using-orientation-sensors-simple.html)
    @Override
    public void onSensorChanged(SensorEvent event) {
        float degree = Math.round(event.values[0]);

        degreesView.setText(Float.toString(degree) + " degrees");
        currentDegree = degree;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    /* Register listeners */
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, orientation, SensorManager.SENSOR_DELAY_GAME);
    }

    /* Unregister listeners */
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }

    // Opens file to write to
    private void openFile() {
        try {
            magnFileStream = new FileOutputStream(magnFile, true);
            Log.i("openFile", "Data Saved to " + magnFile.getAbsolutePath());
        } catch (IOException e) {
            Log.e("SAVE DATA", "Could not write file " + e.getMessage());
            closeGracefully("Error opening file");
            return;
        }

        canWrite = true;
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
