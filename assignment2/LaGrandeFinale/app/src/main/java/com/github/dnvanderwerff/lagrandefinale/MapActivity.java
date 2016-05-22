package com.github.dnvanderwerff.lagrandefinale;

import android.content.Context;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.github.dnvanderwerff.lagrandefinale.particle.CollisionMap;
import com.github.dnvanderwerff.lagrandefinale.particle.ParticleController;
import com.github.dnvanderwerff.lagrandefinale.util.DirectionExtractor;
import com.github.dnvanderwerff.lagrandefinale.view.CompassView;
import com.github.dnvanderwerff.lagrandefinale.view.MapView;

import java.util.Timer;
import java.util.TimerTask;

public class MapActivity extends AppCompatActivity {
    private CollisionMap collisionMap;
    private ParticleController particleController;
    private MapView mapView;
    private CompassView compass;
    private TextView degreeView;

    private Timer timer = new Timer();

    /* Sensor stuff */
    private SensorManager mSensorManager;
    private DirectionExtractor directionExtractor;

    /* Variables */
    private int degreeNorth, degreeMe;
    private float radianNorth, radianMe;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        collisionMap = new CollisionMap(CollisionMap.LSHAPE);
        particleController = new ParticleController(collisionMap);
        particleController.initialize(1000);

        degreeView = (TextView) findViewById(R.id.currentDegrees);

        // Get compass
        compass = (CompassView) findViewById(R.id.compass);

        // Initialize map view
        mapView = (MapView) findViewById(R.id.mapview);
        mapView.initialize(collisionMap, particleController);

        // Get sensors
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        directionExtractor = new DirectionExtractor(mSensorManager);

        /* Timer */
        timer.scheduleAtFixedRate(new updateCompassTask(),1000, 30);
    }

    public void doStep(View view) {
        // Get direction
        double directionDegrees = degreeNorth;

        // Move particles
        particleController.move(directionDegrees);

        // Draw
        mapView.update();
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
            degreeView.setText(degreeNorth + "d");
            compass.update(radianNorth, radianMe);
        }
    };/* Register listeners */
    protected void onResume() {
        super.onResume();
        directionExtractor.registerListeners(mSensorManager);
    }

    /* Unregister listeners */
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(directionExtractor);
    }
}
