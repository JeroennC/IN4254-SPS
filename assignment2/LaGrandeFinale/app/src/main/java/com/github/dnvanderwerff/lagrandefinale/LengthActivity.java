package com.github.dnvanderwerff.lagrandefinale;


import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;


public class LengthActivity extends AppCompatActivity {

    private final AppCompatActivity act = this;

    /* GUI vars */
    private TextView textView;
    private EditText lengthText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_length);

        textView = (TextView) findViewById(R.id.textView);
        lengthText = (EditText) findViewById(R.id.lengthText);

        textView.setText("Enter your height in meters. Current height set to " + MainActivity.length + "." );
    }


    /* Set length if appropriate value has been provided */
    public void saveLength(View view) {
        double len = Double.parseDouble(lengthText.getText().toString());
        if (len < 2.50 && len > 0.50) {
            MainActivity.length = len;
        }
        textView.setText("Enter your height in meters. Current height set to " + MainActivity.length + "." );
    }
}
