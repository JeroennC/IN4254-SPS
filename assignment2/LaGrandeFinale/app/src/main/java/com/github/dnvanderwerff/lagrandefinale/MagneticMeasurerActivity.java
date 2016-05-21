package com.github.dnvanderwerff.lagrandefinale;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.hardware.SensorManager;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.github.dnvanderwerff.lagrandefinale.util.DirectionExtractor;
import com.github.dnvanderwerff.lagrandefinale.view.CompassView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

public class MagneticMeasurerActivity extends AppCompatActivity {
    public final static int PERM_REQ_EXTWRITE = 1;
    private final AppCompatActivity act = this;
    private final static float ALPHA = 0.8f;
    private boolean canWrite = false;

    private Timer timer = new Timer();

    /* GUI vars */
    private TextView degreesView;
    private EditText cellEdit;
    private CompassView compass;

    /* File stuff */
    private File magnFile;
    private FileOutputStream magnFileStream;

    /* Sensor stuff */
    private SensorManager mSensorManager;
    private DirectionExtractor directionExtractor;

    /* */
    private int degreeNorth, degreeMe;
    private float radianNorth, radianMe;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_magnetic_measurer);

        // Set GUI refs
        degreesView = (TextView) findViewById(R.id.currentDegrees);
        cellEdit = (EditText) findViewById(R.id.cell);
        compass = (CompassView) findViewById(R.id.compass);

        // Get sensors
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        directionExtractor = new DirectionExtractor(mSensorManager);

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

        /* Timer */
        timer.scheduleAtFixedRate(new updateCompassTask(),1000, 30);
    }

    /* Store the current degree with cell in file */
    public void StoreDegree(View view) {
        if (!canWrite) return;

        String cell = cellEdit.getText().toString();
        String input = cell + "\t" + String.format("%.1f", degreeNorth) + "\n";
        try {
            magnFileStream.write(input.getBytes());
            magnFileStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /* Class updating compass */
    class updateCompassTask extends TimerTask {
        @Override
        public void run() {
            degreeNorth = directionExtractor.getDegreeNorth();
            radianNorth = directionExtractor.getRadianNorth();
            degreeMe = directionExtractor.getDegreeMe();
            radianMe = directionExtractor.getRadianMe();

            mHandler.obtainMessage(1).sendToTarget();
        }
    }

    /* Handler to update UI */
    public Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            degreesView.setText("Degrees North: " + degreeNorth + "\n"
                + "Degrees me: " + degreeMe);
            compass.update(radianNorth, radianMe);
        }
    };

    /* Register listeners */
    protected void onResume() {
        super.onResume();
        directionExtractor.registerListeners(mSensorManager);
    }

    /* Unregister listeners */
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(directionExtractor);
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
