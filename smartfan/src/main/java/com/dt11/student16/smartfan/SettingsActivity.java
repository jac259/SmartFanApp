package com.dt11.student16.smartfan;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class SettingsActivity extends AppCompatActivity {

    private SharedPreferences sharedPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Hook up return button
        Button btnReturn = (Button) findViewById(R.id.btnReturn);
        btnReturn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                launchMain();
            }
        });

        Button btnUpdate = (Button) findViewById(R.id.btnUpdate);
        btnUpdate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveSettings();
            }
        });

        sharedPref = this.getSharedPreferences(getString(R.string.PREF_NAME), Context.MODE_PRIVATE);
        String ipAddress = sharedPref.getString(getString(R.string.PK_IP), "N/A");
        String ipPort = sharedPref.getString(getString(R.string.PK_Port), "N/A");
        EditText textIP = (EditText) findViewById(R.id.textIP);
        EditText textPort = (EditText) findViewById(R.id.textPort);

        if(!ipAddress.equals("N/A"))
            textIP.setText(ipAddress);

        if(!ipPort.equals("N/A"))
            textPort.setText(ipPort);

    }

    private void launchMain() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        overridePendingTransition(0,0);
    }

    private void saveSettings() {
        if(!dataValidation())
            return;

        String editIP = ((EditText) findViewById(R.id.textIP)).getText().toString();
        String storedIP = sharedPref.getString(getString(R.string.PK_IP), "N/A");

        String editPort = ((EditText) findViewById(R.id.textPort)).getText().toString();
        String storedPort = sharedPref.getString(getString(R.string.PK_Port), "N/A");

        SharedPreferences.Editor editor = sharedPref.edit();
        boolean edit = false;

        if(!editIP.equals(storedIP)) {
            editor.putString(getString(R.string.PK_IP), editIP);
            edit = true;
        }

        if(!editPort.equals(storedPort)) {
            editor.putString(getString(R.string.PK_Port), editPort);
            edit = true;
        }

        if(edit)
            editor.apply();
    }

    private boolean dataValidation() {
        try {
            String ipString = ((EditText) findViewById(R.id.textIP)).getText().toString();
            String portString = ((EditText) findViewById(R.id.textPort)).getText().toString();

            boolean validIP = HttpRequests.validate(ipString);
            Integer int2 = HttpRequests.tryParseInt(portString);

            if (!validIP || int2 == null)
                throw new Exception();

            return true;
        } catch (Exception e) {
            Toast.makeText(this, getString(R.string.settingsDataValid), Toast.LENGTH_SHORT).show();
            return false;
        }
    }
}
