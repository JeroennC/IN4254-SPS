package com.github.dnvanderwerff.lagrandefinale;

import android.Manifest;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.SensorManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;

import com.github.dnvanderwerff.lagrandefinale.util.DirectionExtractor;
import com.github.dnvanderwerff.lagrandefinale.util.StepDetector;
import com.github.dnvanderwerff.lagrandefinale.view.CompassView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * This Activity provides simple storing of the degree north
 *      -   Cell selection
 *      -   Direction selection
 *      -   Start/Stop recording
 */
public class NovaMagnetoActivity extends AppCompatActivity {
    public final static int PERM_REQ_EXTWRITE = 1;
    public final static int PERM_REQ_LOC = 2;
    private final AppCompatActivity act = this;
    private final static int offsetDegreesBuildingMap =  -30;
    private final static float offsetRadianBuildingMap = (float)(Math.toRadians(offsetDegreesBuildingMap));

    /* Class variables */
    private DirectionExtractor directionExtractor;
    private boolean recording;
    private SensorManager mSensorManager;
    private Timer timer = new Timer();
    private boolean canWrite = false;

    private int degreeNorth = offsetDegreesBuildingMap + 90, degreeMe;
    private float radianNorth = (float)(offsetRadianBuildingMap + 0.5 * Math.PI), radianMe;

    /* Layout Variables */
    private CompassView compassView;
    private Button button;
    private Spinner cellSelector;
    private RadioGroup directionSelector;


    /* File variables */
    private File file;
    private FileOutputStream fileStream;
    private File wifiFile;
    private FileOutputStream wifiFileStream;


    /* Wifi */
    private WifiManager mWifiManager;

    private final BroadcastReceiver mWifiScanReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent intent) {
            if (intent.getAction().equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
                if (!recording) return;
                String cell = cellSelector.getSelectedItem().toString();
                List<ScanResult> mScanResults = mWifiManager.getScanResults();
                List<String> handledIds = new ArrayList<>();
                String data = "";
                for (ScanResult sr : mScanResults) {
                    // Don't add duplicate MAC addresses
                    if (handledIds.contains(sr.BSSID)) continue;

                    data += sr.timestamp + "\t" + cell + "\t" + sr.BSSID + "\t" + sr.level + "\n";
                    handledIds.add(sr.BSSID);
                }

                // Write to file
                try {
                    wifiFileStream.write(data.getBytes());
                    wifiFileStream.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nova_magneto);

        // Get views
        compassView = (CompassView) findViewById(R.id.compass);
        button = (Button) findViewById(R.id.recordButton);
        cellSelector = (Spinner) findViewById(R.id.cellSelector);
        ArrayAdapter<Integer> adapter = new ArrayAdapter<Integer>(this
                , android.R.layout.simple_spinner_dropdown_item
                , new Integer[]{1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18}
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        cellSelector.setAdapter(adapter);
        directionSelector = (RadioGroup) findViewById(R.id.radioButtons);
        directionSelector.check(R.id.buttonUp);

        // Sensor Manager
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        // Use direction extractor without gyroscope? Or maybe just with
        directionExtractor = new DirectionExtractor(mSensorManager);

        // Get file location
        File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        file = new File(path, "novaMagneto.dat");
        wifiFile = new File(path, "novaMagnetoWifi.dat");

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

        int permissionCheckWifi = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
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

        /* Timer */
        timer.scheduleAtFixedRate(new updateCompassTask(),1000, 30);
    }

    public void toggleRecording(View view) {
        recording = !recording;
        if (recording) {
            button.setText("Stop");
        } else {
            button.setText("Start");
        }
    }

    private void startWifi() {
        mWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);

        // Frequently scan Wifi networks
        Timer t = new Timer();

        class ScanTask extends TimerTask {
            @Override
            public void run() {
                mWifiManager.startScan();
            }
        }

        // Scan every second
        t.schedule(new ScanTask(), 1000, 5000);

        if (mWifiManager != null) {
            registerReceiver(mWifiScanReceiver, new IntentFilter(mWifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        }
    }

    /* Class updating compass */
    class updateCompassTask extends TimerTask {
        @Override
        public void run() {
            //is now always constant degreeNorth = directionExtractor.getDegreeNorth();
            //is now always constant radianNorth = directionExtractor.getRadianNorth();
            degreeMe = 360 - (directionExtractor.getDegreeMe() + offsetDegreesBuildingMap);

            radianMe = 2 * (float)Math.PI - (directionExtractor.getRadianMe() + offsetRadianBuildingMap);
            radianMe = (2 * (float)Math.PI + radianMe) % (2 * (float)Math.PI);
            mHandler.obtainMessage(1).sendToTarget();
        }
    }

    class storeDirectionTask extends TimerTask {
        @Override
        public void run() {
            if (recording)
                StoreDegree();
        }
    }

    /* Handler to update UI */
    public Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    compassView.update(radianNorth, radianMe);
                    break;
            }
        }
    };

    /* File tings */
    /* Store the current degree with cell in file */
    public void StoreDegree() {
        if (!canWrite) return;

        String cell = cellSelector.getSelectedItem().toString();
        String direction = ( (RadioButton) findViewById(directionSelector.getCheckedRadioButtonId()) ).getText().toString();
        String input = cell + "\t" + direction + "\t" + String.format("%.4f", radianMe) + "\n";
        //Log.d("outputsample", input);

        try {
            fileStream.write(input.getBytes());
            fileStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /* Register listeners */
    protected void onResume() {
        super.onResume();
        directionExtractor.registerListeners(mSensorManager);
        registerReceiver(mWifiScanReceiver, new IntentFilter(mWifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
    }

    /* Unregister listeners */
    protected void onPause() {
        super.onPause();
        if (recording)
            toggleRecording(null);
        mSensorManager.unregisterListener(directionExtractor);
        unregisterReceiver(mWifiScanReceiver);
    }

    protected void onStop() {
        super.onStop();
        if (recording)
            toggleRecording(null);
    }

    // Opens file to write to
    private void openFile() {
        try {
            fileStream = new FileOutputStream(file, true);
            fileStream.write(("Opened file at " + System.currentTimeMillis() + "\n").getBytes());
            fileStream.flush();
            wifiFileStream = new FileOutputStream(wifiFile, true);
            wifiFileStream.write(("Opened file at " + System.currentTimeMillis() + "\n").getBytes());
            wifiFileStream.flush();
            Log.i("openFile", "Data Saved to " + file.getAbsolutePath());
        } catch (IOException e) {
            Log.e("SAVE DATA", "Could not write file " + e.getMessage());
            closeGracefully("Error opening file");
            return;
        }

        canWrite = true;
        timer.scheduleAtFixedRate(new storeDirectionTask(),1000, 50);
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
