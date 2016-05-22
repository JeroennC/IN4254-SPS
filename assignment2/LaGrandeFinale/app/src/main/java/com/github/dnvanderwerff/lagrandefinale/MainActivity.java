package com.github.dnvanderwerff.lagrandefinale;


import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;


public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

    }

    public void startDistance(View view) {
        Intent intent = new Intent(this, DistanceActivity.class);
        startActivity(intent);
    }

    public void startMagnet(View view) {
        //Intent intent = new Intent(this, );
        //startActivity(intent);
    }


}
