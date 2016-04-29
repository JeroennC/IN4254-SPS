package com.github.jeroennc.in4524_sps.fissa;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

public class PretrainingActivity extends AppCompatActivity {
    public final static String TRAINING_FILENAME = "com.github.jeroennc.in4524_sps.TRAINING_FILENAME";
    public final static String TRAINING_ISWIFI = "com.github.jeroennc.in4524_sps.TRAINING_ISWIFI";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pretraining);
    }

    public void startTrainingWifi(View view) {
        Intent intent = new Intent(this, TrainingActivity.class);
        EditText input = (EditText) findViewById(R.id.trainfile);
        String filename = input.getText().toString();
        intent.putExtra(TRAINING_FILENAME, filename);
        intent.putExtra(TRAINING_ISWIFI, true);
        startActivity(intent);
    }

    public void startTrainingWalk(View view) {
        Intent intent = new Intent(this, TrainingActivity.class);
        EditText input = (EditText) findViewById(R.id.trainfile);
        String filename = input.getText().toString();
        intent.putExtra(TRAINING_FILENAME, filename);
        intent.putExtra(TRAINING_ISWIFI, false);
        startActivity(intent);
    }
}
