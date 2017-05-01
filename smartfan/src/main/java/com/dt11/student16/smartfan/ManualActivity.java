package com.dt11.student16.smartfan;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.SeekBar;
import android.widget.Toast;

import com.github.anastr.speedviewlib.SpeedView;

import org.json.JSONObject;

import java.util.List;

public class ManualActivity extends AppCompatActivity implements AsyncResponse {

    private static final String TAG = "ManualActivity";

    private String getURL;
    private String postURL;
    private String getOpURL;
    private String postOpURL;

    private SharedPreferences sharedPref;

    HttpRequests http;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.actions, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.actionSettings:
                // User chose the "Settings" item, show the app settings UI...
                launchSettings(getString(R.string.activityManual));
                return true;

            case R.id.actionRefresh:
                getRequest(getURL);
                getRequest(getOpURL);
                return true;

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);
        }
    }

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
        sharedPref = this.getSharedPreferences(getString(R.string.PREF_NAME), Context.MODE_PRIVATE);

        String urlBase = "http://".concat(sharedPref.getString(getString(R.string.PK_IP), "N/A")).concat(":")
                .concat(sharedPref.getString(getString(R.string.PK_Port), "N/A")).concat("/");

        if(urlBase.contains("N/A"))
            Toast.makeText(this, "Please enter an IP address and port number via the Settings menu.", Toast.LENGTH_LONG).show();

        getURL = urlBase.concat(getString(R.string.getManual));
        postURL = urlBase.concat(getString(R.string.postManual));
        getOpURL = urlBase.concat(getString(R.string.getOp));
        postOpURL = urlBase.concat(getString(R.string.postOp));

        // Load fields
        getRequest(getURL);
        getRequest(getOpURL);

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
        if(HttpRequests.checkResponseCode(result))
            parseJSON(result.get(result.size()-1));
        else
            Toast.makeText(this, getString(R.string.connectFailMessage), Toast.LENGTH_LONG).show();
    }

    private void parseJSON(String jsonString) {
        JSONObject json;

        try {
            json = (new JSONObject(jsonString)).getJSONObject("data");

            if (json.has(getString(R.string.RPM))) {
                SpeedView rpmSpeed = (SpeedView) findViewById(R.id.speedView);

                json = (new JSONObject(jsonString)).getJSONObject("data");

                // set RPM
                rpmSpeed.speedTo(Float.parseFloat(json.getString(getString(R.string.RPM))));
            }
            else {
                SeekBar oneSpeed = (SeekBar) findViewById(R.id.seekSpeed);
                RadioButton CW = (RadioButton) findViewById(R.id.rdoCW);
                RadioButton CCW = (RadioButton) findViewById(R.id.rdoCCW);

                // set direction
                String direction = json.getString(getString(R.string.manual_direction));
                if (direction.equals(getString(R.string.clockwise)))
                    CW.setChecked(true);
                else
                    CCW.setChecked(true);

                // set speed
                oneSpeed.setProgress(Math.round(Integer.parseInt(json.getString(getString(R.string.manual_fan_speed)))));
            }


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

    private void launchSettings(String parent) {
        Bundle bundle = new Bundle();
        bundle.putString(getString(R.string.settingsBundleParent), parent);

        Intent intent = new Intent(this, SettingsActivity.class);
        intent.putExtras(bundle);
        startActivity(intent);
    }
}