package com.dt11.student16.smartfan;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Toast;

import com.edmodo.rangebar.RangeBar;

import org.json.JSONObject;

import java.util.List;

public class OneTempActivity extends AppCompatActivity implements AsyncResponse {

    private static final String TAG = "OneTempActivity";

    private String getURL;
    private String postURL;
    private String getOpURL;
    private String postOpURL;

    private SharedPreferences sharedPref;

    HttpRequests http;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_one_temp);

        // Update title bar
        try {
            getSupportActionBar().setTitle(getString(R.string.oneTempTitle));
        }
        catch (Exception ex) {
            Log.e(TAG, ex.toString());
        }

        sharedPref = this.getSharedPreferences(getString(R.string.PREF_NAME), Context.MODE_PRIVATE);

        String urlBase = "http://".concat(sharedPref.getString(getString(R.string.PK_IP), "N/A")).concat(":")
                .concat(sharedPref.getString(getString(R.string.PK_Port), "N/A")).concat("/");

        if(urlBase.contains("N/A"))
            Toast.makeText(this, "Please enter an IP address and port number via the Settings menu.", Toast.LENGTH_LONG).show();

        // Load URLs
        getURL = urlBase.concat(getString(R.string.getOneTemp));
        postURL = urlBase.concat(getString(R.string.postOneTemp));
        getOpURL = urlBase.concat(getString(R.string.getOp));
        postOpURL = urlBase.concat(getString(R.string.postOp));
//        getURL = getString(R.string.url).concat(getString(R.string.getOneTemp));
//        postURL = getString(R.string.url).concat(getString(R.string.postOneTemp));
//        getOpURL = getString(R.string.url).concat(getString(R.string.getOp));
//        postOpURL = getString(R.string.url).concat(getString(R.string.postOp));

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

        RadioButton CW = (RadioButton) findViewById(R.id.rdoCW);
        RadioButton CCW = (RadioButton) findViewById(R.id.rdoCCW);
        RangeBar rangeSpeed = (RangeBar) findViewById(R.id.rangeSpeed);
        EditText editLowText = (EditText) findViewById(R.id.editLowTemp);
        EditText editHighText = (EditText) findViewById(R.id.editHighTemp);

        try {
            json = (new JSONObject(jsonString)).getJSONObject("data");

            // set direction
            String direction = json.getString(getString(R.string.one_temp_direction));
            if (direction.equals(getString(R.string.clockwise)))
                CW.setChecked(true);
            else
                CCW.setChecked(true);

            // set speeds
            Integer lowSpeed = Integer.parseInt(json.getString(getString(R.string.one_temp_low_speed)));
            Integer highSpeed = Integer.parseInt(json.getString(getString(R.string.one_temp_high_speed)));
            rangeSpeed.setThumbIndices(lowSpeed, highSpeed);

            // set temps
            editLowText.setText(json.getString(getString(R.string.one_temp_low_temp)));
            editHighText.setText(json.getString(getString(R.string.one_temp_high_temp)));
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
        String direction = getString(R.string.clockwise);
        Integer lowSpeed = ((RangeBar) findViewById(R.id.rangeSpeed)).getLeftIndex();
        Integer highSpeed = ((RangeBar) findViewById(R.id.rangeSpeed)).getRightIndex();
        String lowTemp = ((EditText) findViewById(R.id.editLowTemp)).getText().toString();
        String highTemp = ((EditText) findViewById(R.id.editHighTemp)).getText().toString();

        if (!forward)
            direction = getString(R.string.counterclockwise);

        http = new HttpRequests(this);
        http.delegate = this;
        http.onPreExecute(true, direction, lowSpeed.toString(), lowTemp, highSpeed.toString(), highTemp);
        http.execute(postURL);
    }

    private void setMode() {

        String mode = getString(R.string.oneTempMode);

        http = new HttpRequests(this);
        http.delegate = this;
        http.onPreExecute(true, mode);
        http.execute(postOpURL);
    }
}
