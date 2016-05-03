package com.github.jeroennc.in4524_sps.fissa;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class FissaActivity extends AppCompatActivity {
    public final static int PERM_REQ_EXTWRITE = 1;
    public final static int PERM_REQ_LOC = 2;
    private final static float ALPHA = 0.25f;
    private final AppCompatActivity act = this;

    /* Training var */
    boolean trainWifi;
    private boolean started = false;
    TextView textView;
    int writes = 0;

    /* File stuff */
    File trainingFile;
    FileOutputStream trainingFileStream;

    /* Accelerator stuff */
    private SensorManager mSensorManager;
    private Sensor accelerator;
    private float[] accelVals;

    /* Wifi */
    private WifiManager mWifiManager;

    private final BroadcastReceiver mWifiScanReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent intent) {
            if (intent.getAction().equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
                if (!started) return;
                List<ScanResult> mScanResults = mWifiManager.getScanResults();
                List<String> handledIds = new ArrayList<>();
                String data = "";
                for (ScanResult sr : mScanResults) {
                    // Don't add duplicate MAC addresses
                    if (handledIds.contains(sr.BSSID)) continue;

                    data += sr.timestamp + "|" + sr.BSSID + "|" + sr.level + "\n";
                    handledIds.add(sr.BSSID);
                    // sla ook levels op
                }
            }
        }
    };


    // Dus de handleIds zijn de resultaten van de wifi meting? Ja
    // sorteer lijst op level -> pak eerste drie. ( per ap is training data een lijst met levels)
    // per AP -> kijk level, gebruik dit om te classifyen. -> print ook label

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_training);

        // Get textview
        textView = (TextView) findViewById(R.id.testtext); // pas dit ID nog aan
        textView.setTextSize(40);
        textView.setText("Testing..");

        //Classifier classifier = new Classifier(7);
        //classifier.trainsClassifier(features list van training set);

        //same voor wifi


        // If necessary, location permissions
        int permissionCheckWifi = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);
        if (permissionCheckWifi != PackageManager.PERMISSION_GRANTED) {
            // Request permission
            ActivityCompat.requestPermissions(this,
                    new String[] {Manifest.permission.ACCESS_WIFI_STATE
                            , Manifest.permission.CHANGE_WIFI_STATE
                            , Manifest.permission.ACCESS_COARSE_LOCATION
                            , Manifest.permission.ACCESS_FINE_LOCATION
                    }
                    , PERM_REQ_LOC);
        } else {
            startWifi();
        }

        // Start accelerator business
        startAccelerator();

    }

    private void startAccelerator() {
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerator = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);

        // See if all can be started
        attemptStart();
    }

    private void startWifi() {
        mWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);

        // Frequently scan Wifi networks
        Timer t = new Timer();

        class ScanTask extends TimerTask {
            @Override
            public void run() {
                if (started) {
                    mWifiManager.startScan();
                }
            }
        }

        // Scan every second
        t.schedule(new ScanTask(), 1000, 5000);

        // See if all can be started
        attemptStart();
    }

    /* Opens the file stream */
    private void openFile() {
        try {
            trainingFileStream = new FileOutputStream(trainingFile, true);
            Log.i("openFile", "Data Saved to " + trainingFile.getAbsolutePath());
        } catch (IOException e) {
            Log.e("SAVE DATA", "Could not write file " + e.getMessage());
            closeGracefully("Error opening file");
            return;
        }

        // See if all can be started
        attemptStart();
    }

    /* Starts recording data if possible */
    private void attemptStart() {
        if (started) {
            return;
        } else {
            started = true;
            registerReceiver(mWifiScanReceiver, new IntentFilter(mWifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
            if (accelerator != null) {
                mSensorManager.registerListener(this, accelerator, SensorManager.SENSOR_DELAY_GAME);
            }
        }
    }

    /* Register listeners */
    protected void onResume() {
        super.onResume();
        if (!started) {
            return;
        } else {
            registerReceiver(mWifiScanReceiver, new IntentFilter(mWifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
            mSensorManager.registerListener(this, accelerator, SensorManager.SENSOR_DELAY_GAME);
        }
    }

    /* Unregister listeners */
    protected void onPause() {
        super.onPause();
        if (!started) {
            return;
        } else {
            unregisterReceiver(mWifiScanReceiver);
            mSensorManager.unregisterListener(this);
        }
    }

    /* Close file */
    protected void onStop() {
        super.onStop();
        if (trainingFileStream != null) {
            try {
                trainingFileStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        accelVals = lowPass(event.values.clone(), accelVals);
        String xValue = String.format("%.2f", accelVals[0]);
        String yValue = String.format("%.2f", accelVals[1]);
        String zValue = String.format("%.2f", accelVals[2]);
        long ts = System.currentTimeMillis();

        //voor 1 time windows (600 ms) sla min max features op -> gebruik classifier
        //        label printen op scherm


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
            case PERM_REQ_LOC: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startWifi();
                } else {
                    // Well damnit :(
                    closeGracefully("You won't let me WiFi!");
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
