package com.github.jeroennc.in4524_sps.fissa;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class Main extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button train = (Button) findViewById(R.id.btn_train);
        Button fissa = (Button) findViewById(R.id.btn_fissa);
    }

    public void startTrain(View view) {
        Intent intent = new Intent(this, PretrainingActivity.class);
        startActivity(intent);
    }

    public void startFissa(View view) {
        Intent intent = new Intent(this, FissaActivity.class);
        startActivity(intent);
    }
}
