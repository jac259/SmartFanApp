package com.dt11.student16.smartfan;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;

import com.edmodo.rangebar.RangeBar;

import org.json.JSONObject;

import java.util.List;

public class TwoTempActivity extends AppCompatActivity implements AsyncResponse {

    private static final String TAG = "TwoTempActivity";

    private String getURL;
    private String postURL;
    private String getOpURL;
    private String postOpURL;

    HttpRequests http;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_two_temp);

        // Update title bar
        try {
            getSupportActionBar().setTitle(getString(R.string.twoTempTitle));
        }
        catch (Exception ex) {
            Log.e(TAG, ex.toString());
        }

        // Load URLs
        getURL = getString(R.string.url).concat(getString(R.string.getTwoTemp));
        postURL = getString(R.string.url).concat(getString(R.string.postTwoTemp));
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
    public void processFinish(List<String> result) {
        parseJSON(result.get(result.size()-1));
    }

    private void parseJSON(String jsonString) {
        if(jsonString.equals(getString(R.string.success)))
            return;

        JSONObject json;

        RangeBar rangeSpeed = (RangeBar) findViewById(R.id.rangeSpeed);
        EditText editLowText = (EditText) findViewById(R.id.editLowTemp);
        EditText editHighText = (EditText) findViewById(R.id.editHighTemp);

        try {
            json = (new JSONObject(jsonString)).getJSONObject("data");

            // set speeds
            Integer lowSpeed = Integer.parseInt(json.getString(getString(R.string.two_temp_low_speed)));
            Integer highSpeed = Integer.parseInt(json.getString(getString(R.string.two_temp_high_speed)));
            rangeSpeed.setThumbIndices(lowSpeed, highSpeed);

            // set temps
            editLowText.setText(json.getString(getString(R.string.two_temp_low_temp)));
            editHighText.setText(json.getString(getString(R.string.two_temp_high_temp)));
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

        Integer lowSpeed = ((RangeBar) findViewById(R.id.rangeSpeed)).getLeftIndex();
        Integer highSpeed = ((RangeBar) findViewById(R.id.rangeSpeed)).getRightIndex();
        String lowTemp = ((EditText) findViewById(R.id.editLowTemp)).getText().toString();
        String highTemp = ((EditText) findViewById(R.id.editHighTemp)).getText().toString();

        http = new HttpRequests(this);
        http.delegate = this;
        http.onPreExecute(true, lowSpeed.toString(), lowTemp, highSpeed.toString(), highTemp);
        http.execute(postURL);
    }

    private void setMode() {

        String mode = getString(R.string.twoTempMode);

        http = new HttpRequests(this);
        http.delegate = this;
        http.onPreExecute(true, mode);
        http.execute(postOpURL);
    }
}
