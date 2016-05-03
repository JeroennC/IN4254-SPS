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
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.os.SystemClock;
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class FissaActivity extends AppCompatActivity implements SensorEventListener{
    public final static int PERM_REQ_EXTREAD = 3;
    public final static int PERM_REQ_LOC = 2;
    private final static float ALPHA = 0.25f;
    private final AppCompatActivity act = this;
    private final static int KVALUE = 7;

    private class WifiCollection {
        Map<String, Classifier> classifiers;
        Map<String, List<Feature>> features;

        WifiCollection() {
            classifiers = new HashMap<>();
            features = new HashMap<>();
        }

        Classifier getClassifier(String ap_mac) {
            Classifier c = classifiers.get(ap_mac);
            if (c == null) {
                c = new Classifier(KVALUE);
                List<Feature> featureList = features.get(ap_mac);
                if (features != null)
                    c.trainClassifier(featureList);
                classifiers.put(ap_mac, c);
            }
            return c;
        }

        void addValue(String ap_mac, Feature feat) {
            // Check if list is already present
            List<Feature> ap_data = features.get(ap_mac);
            if (ap_data == null) {
                ap_data = new ArrayList<Feature>();
                features.put(ap_mac, ap_data);
            }
            // Add value
            ap_data.add(feat);
        }
    }

    /* Training var */
    TextView wifitext;
    TextView acctext;

    /* Classifier stuff */
    private Classifier accClassifier;
    private boolean started = false;
    private WifiCollection wifiClassifiers;

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
        acctext = (TextView) findViewById(R.id.acctext); // pas dit ID nog aan
        acctext.setTextSize(40);
        acctext.setText("Testing acc");

        wifitext = (TextView) findViewById(R.id.wifitext); // pas dit ID nog aan
        wifitext.setTextSize(40);
        wifitext.setText("Testing wifi");

        // Classifier start
        accClassifier = new Classifier(KVALUE);
        wifiClassifiers = new WifiCollection();

        // File Permissions
        int permissionCheck = ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            // Request permission
            ActivityCompat.requestPermissions(this,
                    new String[] {Manifest.permission.READ_EXTERNAL_STORAGE
                    }
                    , PERM_REQ_EXTREAD);
        } else {
            readClassifierFiles();
        }

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

    /* Reads the classifiers */
    private void readClassifierFiles() {
        // Read accelerator input
        File accfile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "accelerator.dat");

        List<Feature> accFeatures = new ArrayList<>();
        BufferedReader input;
        try {
            input = new BufferedReader(new InputStreamReader(new FileInputStream(accfile)));

            String line;
            double val;
            String[] inf;
            while ((line = input.readLine()) != null) {
                inf = line.split("\\|");
                val = Double.parseDouble(inf[0]);
                accFeatures.add(new Feature(val, inf[1]));
            }
            input.close();
        } catch (IOException e) {
            closeGracefully("Could not read accelerator.dat");
            e.printStackTrace();
            return;
        }

        accClassifier.trainClassifier(accFeatures);

        // Read WiFi input
        File wififile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "wifi.dat");

        Map<String, List<Feature>> wifiData = new HashMap<>();
        try {
            input = new BufferedReader(new InputStreamReader(new FileInputStream(wififile)));

            String line;
            double val;
            String[] inf;
            while ((line = input.readLine()) != null) {
                inf = line.split("\\|");
                val = Double.parseDouble(inf[1]);
                wifiClassifiers.addValue(inf[0], new Feature(val, inf[2]));
            }
            input.close();
        } catch (IOException e) {
            closeGracefully("Could not read wifi.dat");
            e.printStackTrace();
            return;
        }

        // Try to start now
        attemptStart();
    }

    /* Starts recording data if possible */
    private void attemptStart() {
        if (started) return;
        if (mWifiManager != null && accelerator != null) {
            started = true;
            registerReceiver(mWifiScanReceiver, new IntentFilter(mWifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
            mSensorManager.registerListener(this, accelerator, SensorManager.SENSOR_DELAY_GAME);
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
    long timeWindow = 600; // Time window in msec
    long endOfWindow = System.currentTimeMillis() + timeWindow; // Time of end of window
    List<Feature> features = new ArrayList<Feature>();  // List of features to store within one window

    @Override
    public void onSensorChanged(SensorEvent event) {

        long t = System.currentTimeMillis();               // current time

        if (t < endOfWindow) {
            // TimeWindow has not yet elapsed

            // store and extract features
            accelVals = lowPass(event.values.clone(), accelVals);
            double xValue = accelVals[0];
            double yValue = accelVals[1];
            double zValue = accelVals[2];

            Feature f = new Feature(Math.abs(xValue) + Math.abs(yValue) + Math.abs(zValue),
                                    "undefined");
            features.add(f);

        } else {
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
            acctext.setText(label);

            features = new ArrayList<Feature>();
            endOfWindow = System.currentTimeMillis() + timeWindow;
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

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERM_REQ_EXTREAD: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    readClassifierFiles();
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
