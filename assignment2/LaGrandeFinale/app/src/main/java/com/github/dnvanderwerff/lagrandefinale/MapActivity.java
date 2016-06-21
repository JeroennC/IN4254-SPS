package com.github.dnvanderwerff.lagrandefinale;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.Message;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.github.dnvanderwerff.lagrandefinale.particle.CollisionMap;
import com.github.dnvanderwerff.lagrandefinale.particle.ParticleController;
import com.github.dnvanderwerff.lagrandefinale.util.DirectionExtractor;
import com.github.dnvanderwerff.lagrandefinale.util.NormalDistribution;
import com.github.dnvanderwerff.lagrandefinale.util.StepDetector;
import com.github.dnvanderwerff.lagrandefinale.util.WifiIntel;
import com.github.dnvanderwerff.lagrandefinale.view.CompassView;
import com.github.dnvanderwerff.lagrandefinale.view.MapView;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.Timer;
import java.util.TimerTask;

/**
 * This activity is the heart of the application, which is the only activity the user should have to use
 * This activity
 *      -   displays the map of the 9th floor (or any other map that is created in code)
 *      -   connects all sensors and inputs to the logic behind the map
 *      -   provides all the user interface functionalities
 */
public class MapActivity extends Activity {
    public final static String MAP_TYPE_MSG = "com.github.dnvanderwerff.lagrandefinale.MAP_TYPE_MSG";
    public final static int PREDICTION_HANDLER_ID = 555;
    public final static int PERM_REQ_INTERNET = 1337;
    private final int PARTICLE_COUNT = 10000;
    private final static int offsetDegreesBuildingMap =  -30;
    private final static float offsetRadianBuildingMap = (float)(Math.toRadians(offsetDegreesBuildingMap));
    private final Activity act = this;

    private final double
                r90 = Math.PI / 2,
                r45 = r90 / 2,
                r180 = Math.PI,
                r360 = Math.PI * 2;
    private final double rBound = r90 * 4/9; // 40 deg

    private CollisionMap collisionMap;
    private ParticleController particleController;
    private MapView mapView;
    private CompassView compass;
    private TextView degreeView, surfaceView, cellView;

    private Timer timer = new Timer();

    /* Sensor stuff */
    private SensorManager mSensorManager;
    private DirectionExtractor directionExtractor;
    private StepDetector stepDetector;
    private WifiIntel wifiIntel;
    private Thread wifiThread;

    /* Variables */
    private int degreeNorth, degreeMe;
    private float radianNorth, radianMe;
    private NormalDistribution ndDirection;
    public boolean storeWifi = false;                       // Boolean indicates whether access point information should be stored to the cloud service
    public int[] storeCells = {};                           // Contains the cell numbers to store in

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        degreeView = (TextView) findViewById(R.id.currentDegrees);
        surfaceView = (TextView) findViewById(R.id.particleSurface);
        cellView = (TextView) findViewById(R.id.cellText);
        ndDirection = new NormalDistribution(0,0);

        Intent intent = getIntent();
        int mapType = intent.getIntExtra(MAP_TYPE_MSG, CollisionMap.FLOOR9);

        collisionMap = new CollisionMap(mapType);
        particleController = new ParticleController(collisionMap);
        particleController.initialize(PARTICLE_COUNT);
        // Show surface
        surfaceView.setText(String.format("Surface: %.1f m\u00B2, %.1f%%", particleController.getSurface(), particleController.getSurfaceFraction() * 100));

        // Get compass
        compass = (CompassView) findViewById(R.id.compass);

        // Initialize map view
        mapView = (MapView) findViewById(R.id.mapview);
        mapView.initialize(collisionMap, particleController);

        // Get sensors
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        directionExtractor = new DirectionExtractor(mSensorManager);
        stepDetector = new StepDetector(mSensorManager, mHandler);
        wifiIntel = new WifiIntel(this, mHandler, PREDICTION_HANDLER_ID);
        wifiThread = new Thread() {
            public void run() {
                wifiIntel.start();
            }
        };

        // Internet permissions
        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            // Request permission
            ActivityCompat.requestPermissions(this,
                    new String[] {Manifest.permission.INTERNET
                    }
                    , PERM_REQ_INTERNET);
        } else {
            wifiThread.start();
        }

        /* Timer */
        timer.scheduleAtFixedRate(new updateCompassTask(),1000, 30);
    }

    public void doStep(View view) {
        // Get direction
        double directionRadians = radianNorth + offsetRadianBuildingMap;
        // Make sure it is in [0, 2pi]
        directionRadians = (directionRadians + 2 * Math.PI) % (2 * Math.PI);

        // Lock direction to nearest cardinal (from the buildings perspective)
        double diff;
        if (directionRadians < r45 || directionRadians > r360 - r45) {
            diff = directionRadians < r45 ? directionRadians : directionRadians - r360;
            directionRadians = 0;
        } else if (directionRadians < r90 + r45) {
            diff = directionRadians - r90;
            directionRadians = r90;
        } else if (directionRadians < r180 + r45) {
            diff = directionRadians - r180;
            directionRadians = r180;
        } else {
            diff = directionRadians - (r180 + r90);
            directionRadians = r180 + r90;
        }

        // Special cases for when the direction is near 45 deg off a cardinal?
        if (Math.abs(diff) > rBound) {
            if (diff > 0) {
                directionRadians += r45;
            } else {
                directionRadians -= r45;
            }
        }

        // Use standard deviation based on difference with cardinal
        ndDirection.setStandardDev(diff * .75);

        stepDetector.pauseSensor();

        // Move particles
        particleController.move(directionRadians, ndDirection);

        stepDetector.resumeSensor();

        // Draw
        mapView.update();

        // If cells < 2, set storing on, otherwise set off
        if (particleController.getDominantCells().size() > 0 && particleController.getDominantCells().size() <= 3
                && particleController.getSurfaceFraction() < 0.05) {
            // Only do things if switching state
            storeWifi = true;
            storeCells = new int[particleController.getDominantCells().size()];
            int i = 0;
            for (Integer cell : particleController.getDominantCells())
                storeCells[i++] = cell;
            cellView.setText("Storing for cells " + particleController.getActiveCell());
        } else {
            if (storeWifi) { // Only do things if switching state
                storeWifi = false;
                storeCells = new int[0];
                cellView.setText("Not storing");
            }
        }

        // Show surface
        //surfaceView.setText(String.format("Surface: %.1f m\u00B2, %.1f%%", particleController.getSurface(), particleController.getSurfaceFraction() * 100));
    }

    public void requestPrediction(View view) {
        wifiIntel.requestPrediction();
    }

    public void requestReset(View view) {
        wifiIntel.requestReset();
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

    /* Handler to update parts of the application */
    public Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    //degreeView.setText(degreeNorth + "d");
                    compass.update(radianNorth, radianMe);
                    degreeView.setText(String.format("c: %.2f\nt: %d", stepDetector.getCorrelation(), stepDetector.getOptimalTimeWindow()));
                    surfaceView.setText("I am " + stepDetector.getState().toString());
                    break;
                case StepDetector.STEP_HANDLER_ID:
                    doStep(null);
                    break;
                case PREDICTION_HANDLER_ID:
                    String result = (String)msg.obj;
                    if (!result.isEmpty()) {
                        // Read JSON list
                        double[] cellDist = new double[21];
                        Log.d("PredictResult", result);
                        try {
                            boolean isFilled = false;
                            JSONArray jsonArray = new JSONArray(result);
                            for (int i = 0; i < jsonArray.length(); i++) {
                                cellDist[i] = jsonArray.getDouble(i);
                                if (cellDist[i] > 0)
                                    isFilled = true;
                            }
                            if (isFilled) {
                                particleController.initialize(PARTICLE_COUNT, cellDist);
                                mapView.update();
                            }
                            break;
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }

                    Toast.makeText(act, "Could not initialize prediction", Toast.LENGTH_LONG).show();
                    break;
            }
        }
    };

    /* Register listeners */
    protected void onResume() {
        super.onResume();
        directionExtractor.registerListeners(mSensorManager);
        stepDetector.registerListeners(mSensorManager);
        wifiIntel.onResume();
    }

    /* Unregister listeners */
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(directionExtractor);
        mSensorManager.unregisterListener(stepDetector);
        wifiIntel.onPause();
    }

    // Permissions callback
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERM_REQ_INTERNET: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    wifiThread.start();
                } else {
                    // Well damnit :(
                    closeGracefully("You won't let me internet!");
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
