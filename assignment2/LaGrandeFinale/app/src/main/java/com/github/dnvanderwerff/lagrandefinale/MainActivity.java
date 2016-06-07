package com.github.dnvanderwerff.lagrandefinale;


import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import com.github.dnvanderwerff.lagrandefinale.particle.CollisionMap;


public class MainActivity extends AppCompatActivity {

    public static double length = 1.80; // Initialize length of user throughout app

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void startDistance(View view) {
        Intent intent = new Intent(this, DistanceActivity.class);
        startActivity(intent);
    }

    public void startMagneto(View view) {
        Intent intent = new Intent(this, MagneticMeasurerActivity.class);
        startActivity(intent);
    }

    public void startParticleMapLShape(View view) {
        Intent intent = new Intent(this, MapActivity.class);
        intent.putExtra(MapActivity.MAP_TYPE_MSG, CollisionMap.LSHAPE);
        startActivity(intent);
    }

    public void startParticleMapFloor9(View view) {
        Intent intent = new Intent(this, MapActivity.class);
        intent.putExtra(MapActivity.MAP_TYPE_MSG, CollisionMap.FLOOR9);
        startActivity(intent);
    }

    public void startAccelMeas(View view) {
        Intent intent = new Intent(this, AccelerationMeasure.class);
        startActivity(intent);
    }

    public void startLength(View view) {
        Intent intent = new Intent(this, LengthActivity.class);
        startActivity(intent);
    }
}
