package com.github.dnvanderwerff.lagrandefinale;

import android.app.Activity;
import android.content.Context;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.TextView;

import com.github.dnvanderwerff.lagrandefinale.particle.CollisionMap;
import com.github.dnvanderwerff.lagrandefinale.particle.ParticleController;
import com.github.dnvanderwerff.lagrandefinale.util.DirectionExtractor;
import com.github.dnvanderwerff.lagrandefinale.util.StepDetector;
import com.github.dnvanderwerff.lagrandefinale.view.CompassView;
import com.github.dnvanderwerff.lagrandefinale.view.MapView;

import java.util.Timer;
import java.util.TimerTask;

public class MapActivity extends Activity {
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

    /* Variables */
    private int degreeNorth, degreeMe;
    private float radianNorth, radianMe;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        degreeView = (TextView) findViewById(R.id.currentDegrees);
        surfaceView = (TextView) findViewById(R.id.particleSurface);
        cellView = (TextView) findViewById(R.id.cellText);

        collisionMap = new CollisionMap(CollisionMap.LSHAPE);
        particleController = new ParticleController(collisionMap);
        particleController.initialize(1000);
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

        /* Timer */
        timer.scheduleAtFixedRate(new updateCompassTask(),1000, 30);
    }

    public void doStep(View view) {
        // Get direction
        double directionRadians = radianNorth;

        // Move particles
        particleController.move(directionRadians);

        // Draw
        mapView.update();

        // Show surface
        cellView.setText(particleController.getActiveCell());
        //surfaceView.setText(String.format("Surface: %.1f m\u00B2, %.1f%%", particleController.getSurface(), particleController.getSurfaceFraction() * 100));
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
            switch (msg.what) {
                case 1:
                    degreeView.setText(degreeNorth + "d");
                    compass.update(radianNorth, radianMe);
                    surfaceView.setText("I am " + stepDetector.getState().toString());
                    break;
                case StepDetector.STEP_HANDLER_ID:
                    doStep(null);
                    break;
            }
        }
    };

    /* Register listeners */
    protected void onResume() {
        super.onResume();
        directionExtractor.registerListeners(mSensorManager);
        stepDetector.registerListeners(mSensorManager);
    }

    /* Unregister listeners */
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(directionExtractor);
        mSensorManager.unregisterListener(stepDetector);
    }
}
