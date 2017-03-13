package com.dt11.student16.smartfan;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.SeekBar;

import org.json.JSONObject;

import java.util.List;

public class ManualActivity extends AppCompatActivity implements AsyncResponse {

    private static final String TAG = "ManualActivity";

    private String getURL;
    private String postURL;
    private String getOpURL;
    private String postOpURL;

    HttpRequests http;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manual);

        // Update title bar
        try {
            getSupportActionBar().setTitle(getString(R.string.manualTitle));
        }
        catch (Exception ex) {
            Log.e(TAG, ex.toString());
        }

        // Load URLs
        getURL = getString(R.string.url).concat(getString(R.string.getManual));
        postURL = getString(R.string.url).concat(getString(R.string.postManual));
        getOpURL = getString(R.string.url).concat(getString(R.string.getOp));
        postOpURL = getString(R.string.url).concat(getString(R.string.postOp));

        // Load fields
        getRequest(getURL);

        // Hook up return button
        Button btnReturn = (Button) findViewById(R.id.btnReturn);
        btnReturn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                launchMain();
            }
        });

        // Hook up update button
        Button btnUpdate = (Button) findViewById(R.id.btnUpdate);
        btnUpdate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                postRequest();
            }
        });

        // Hook up change mode button
        Button btnChangeMode = (Button) findViewById(R.id.btnChangeMode);
        btnChangeMode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setMode();
            }
        });
    }

    private void launchMain() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        overridePendingTransition(0,0);
    }

    @Override
    public void processFinish(List<String> result) { parseJSON(result.get(result.size()-1)); }

    private void parseJSON(String jsonString) {
        JSONObject json;
        SeekBar oneSpeed = (SeekBar) findViewById(R.id.seekSpeed);
        RadioButton CW = (RadioButton) findViewById(R.id.rdoCW);
        RadioButton CCW = (RadioButton) findViewById(R.id.rdoCCW);

        try {
            json = (new JSONObject(jsonString)).getJSONObject("data");

            // set direction
            String direction = json.getString(getString(R.string.manual_direction));
            if (direction.equals(getString(R.string.clockwise)))
                CW.setChecked(true);
            else
                CCW.setChecked(true);

            // set speed
            oneSpeed.setProgress(Math.round(Integer.parseInt(json.getString(getString(R.string.manual_fan_speed)))));
        }
        catch (Exception ex) {
            Log.e(TAG, ex.toString());
        }

    }

    private void getRequest(String url) {

        http = new HttpRequests(this);
        http.delegate = this;
        http.onPreExecute(false);
        http.execute(url);

    }

    private void postRequest() {

        Boolean forward = ((RadioButton) findViewById(R.id.rdoCW)).isChecked();
        Integer oneSpeed = Math.round(((SeekBar) findViewById(R.id.seekSpeed)).getProgress());
        String direction = getString(R.string.clockwise);

        if (!forward)
            direction = getString(R.string.counterclockwise);

        http = new HttpRequests(this);
        http.delegate = this;
        http.onPreExecute(true, direction, oneSpeed.toString());
        http.execute(postURL);
    }

    private void setMode() {

        String mode = getString(R.string.manualMode);

        http = new HttpRequests(this);
        http.delegate = this;
        http.onPreExecute(true, mode);
        http.execute(postOpURL);
    }
}
