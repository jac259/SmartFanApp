package com.dt11.student16.smartfan;

import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.transition.Fade;
import android.transition.Slide;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public class MainActivity extends AppCompatActivity implements AsyncResponse {

    private static final String TAG = "MainActivity";

    private Context context = this;

    private String getOpURL;
    private String getTempURL;
    private String postPowerURL;

    HttpRequests http;

    boolean powerState;

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
                return true;

            case R.id.actionRefresh:
                getRequest(getOpURL);
                getRequest(getTempURL);
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
        setContentView(R.layout.activity_main);

        getOpURL = getString(R.string.url).concat(getString(R.string.getOp));
        getTempURL = getString(R.string.url).concat(getString(R.string.getTemp));
        postPowerURL = getString(R.string.url).concat(getString(R.string.postPower));

        getRequest(getOpURL);
        getRequest(getTempURL);

        Button btnManual = (Button) findViewById(R.id.btnManual);
        Button btnSchedule = (Button) findViewById(R.id.btnSchedule);
        Button btnOneTemp = (Button) findViewById(R.id.btnOneTemp);
        Button btnTwoTemp = (Button) findViewById(R.id.btnTwoTemp);
        final FrameLayout layoutPower = (FrameLayout) findViewById(R.id.layoutPower);
        final ImageButton imgBtnPower = (ImageButton) findViewById(R.id.imgBtnPower);
        //final ImageButton imgBtnRefresh = (ImageButton) findViewById(R.id.imgBtnRefresh);

        btnManual.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                launchManual();
            }
        });

        btnSchedule.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                launchSchedule();
            }
        });

        btnOneTemp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                launchOneTemp();
            }
        });

        btnTwoTemp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                launchTwoTemp();
            }
        });

        layoutPower.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                powerState = !powerState;
                postPowerRequest();
                imgBtnPower.setColorFilter(ContextCompat.getColor(context, powerState ? R.color.colorAccent : R.color.colorPrimaryLight));
                imgBtnPower.refreshDrawableState();
            }
        });

//        imgBtnRefresh.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                getRequest(getOpURL);
//                getRequest(getTempURL);
//            }
//        });

        imgBtnPower.setColorFilter(ContextCompat.getColor(context, powerState ? R.color.colorAccent : R.color.colorPrimaryLight));
        imgBtnPower.refreshDrawableState();
    }

    @Override
    public void processFinish(List<String> result) { parseJSON(result.get(result.size()-1)); }

    private void launchManual() {
        Intent intent = new Intent(this, ManualActivity.class);
        //startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle());
        startActivity(intent);
    }

    private void launchSchedule() {
        Intent intent = new Intent(this, ScheduleActivity.class);
        //startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle());
        startActivity(intent);
    }

    private void launchOneTemp() {
        Intent intent = new Intent(this, OneTempActivity.class);
        //startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle());
        startActivity(intent);
    }

    private void launchTwoTemp() {
        Intent intent = new Intent(this, TwoTempActivity.class);
        //startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle());
        startActivity(intent);
    }

    private void parseJSON(String jsonString) {
        if(!isJSONValid(jsonString))
            return;

        JSONObject json;
        try {
            json = (new JSONObject(jsonString)).getJSONObject("data");

            if (json.has(getString(R.string.main_temp))) {
                TextView textTemperature = (TextView) findViewById(R.id.textTemperature);

                String tempString = json.getString(getString(R.string.main_temp));

                tempString = tempString.contains(".") ?
                        tempString.substring(0, tempString.indexOf('.')).concat(getString(R.string.degreesFahr)) :
                        tempString.concat(getString(R.string.degreesFahr));

                textTemperature.setText(tempString);
            }
            else if (json.has(getString(R.string.main_power)) && json.has(getString(R.string.main_mode))) {
                ImageButton imgBtnPower = (ImageButton) findViewById(R.id.imgBtnPower);
                TextView textMode = (TextView) findViewById(R.id.textMode);

                powerState = Boolean.parseBoolean(json.getString(getString(R.string.main_power)));
                imgBtnPower.setColorFilter(ContextCompat.getColor(context, powerState ? R.color.colorAccent : R.color.colorPrimaryLight));
                imgBtnPower.refreshDrawableState();

                String mode = json.getString(getString(R.string.main_mode));
                if (mode.equals(getString(R.string.manualMode)) || mode.equals(getString(R.string.scheduleMode)))
                    textMode.setText(mode);
                else if(mode.equals(getString(R.string.oneTempMode)))
                    textMode.setText(getString(R.string.oneTempButton));
                else if(mode.equals(getString(R.string.twoTempMode)))
                    textMode.setText(getString(R.string.twoTempButton));
                else
                    throw new Exception(getString(R.string.exMode));
            }
        }
        catch (Exception ex) {
            Log.e(TAG, ex.toString());
        }
    }

    public boolean isJSONValid(String test) {
        try { new JSONObject(test); }
        catch (JSONException ex) {
            try { new JSONArray(test); }
            catch (JSONException ex1) { return false; }
        }
        return true;
    }

    private void getRequest(String url) {

        http = new HttpRequests(this);
        http.delegate = this;
        http.onPreExecute(false);
        http.execute(url);
    }

    private void postPowerRequest() {

        http = new HttpRequests(this);
        http.delegate = this;
        http.onPreExecute(true, powerState);
        http.execute(postPowerURL);
    }

}
