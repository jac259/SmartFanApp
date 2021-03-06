package com.dt11.student16.smartfan;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.util.Log;
import android.util.Pair;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static junit.framework.Assert.assertEquals;

public class HttpRequests extends AsyncTask<String, Void, List<String>> {

    private static final String TAG = "HttpRequests";

    public AsyncResponse delegate = null;

    private Boolean Post = false;
    private List<Pair<String, String>> Params = new ArrayList<>();
    private Context context;
    private Resources r;

    private SharedPreferences sharedPref;
    private String GetOP;

    public HttpRequests(Context current) {
        this.context = current;
        r = context.getResources();
    }

    @Override
    protected List<String> doInBackground(String... args) {

        List<String> results = new ArrayList<>();
        for (String s : args) {

            try {
                if (Post)
                    sendPost(s, Params, results);
                else
                    sendGet(s, results);
            } catch (Exception ex) {
                results.add("An error occurred: " + ex.toString());
                Log.e(TAG, ex.toString());
            }

        }
        return results;
    }

    // Get
    protected void onPreExecute(Boolean post) {
        Post = post;
    }

    // Update Mode
    protected void onPreExecute(Boolean post, String mode) {
        Post = post;

        Params.add(Pair.create(r.getString(R.string.mode), mode));
    }

    // Toggle Power
    protected void onPreExecute(Boolean post, Boolean powerState) {
        Post = post;

        Params.add(Pair.create(r.getString(R.string.main_power), powerState ? r.getString(R.string.textTrue) : r.getString(R.string.textFalse)));
    }

    // Manual
    protected void onPreExecute(Boolean post, String direction, String fanspeed) {
        Post = post;

        Params.add(Pair.create(r.getString(R.string.manual_direction), direction));
        Params.add(Pair.create(r.getString(R.string.manual_fan_speed), fanspeed));
    }

    // Schedule - Create/Update
    protected void onPreExecute(Boolean post, Schedule schedule) {
        Post = post;

        Params.add(Pair.create(r.getString(R.string.schedule_id), Integer.toString(schedule.id)));
        Params.add(Pair.create(r.getString(R.string.begin_time), schedule.startTime));
        Params.add(Pair.create(r.getString(R.string.end_time), schedule.endTime));
        Params.add(Pair.create(r.getString(R.string.direction), schedule.direction));
        Params.add(Pair.create(r.getString(R.string.fan_speed), Integer.toString(schedule.speed)));
        Params.add(Pair.create(r.getString(R.string.day), schedule.days));
        Params.add(Pair.create(r.getString(R.string.enabled), schedule.enabled ? r.getString(R.string.yes) : r.getString(R.string.no)));
    }

    // Schedule - Delete
    protected void onPreExecute(Boolean post, Integer schedule_id) {
        Post = post;

        Params.add(Pair.create(r.getString(R.string.schedule_id), Integer.toString(schedule_id)));
    }

    // Schedule - Toggle
    protected void onPreExecute(Boolean post, Integer schedule_id, String toggleState) {
        Post = post;

        Params.add(Pair.create(r.getString(R.string.schedule_id), Integer.toString(schedule_id)));
        Params.add(Pair.create(r.getString(R.string.toggle), toggleState));
    }

    // OneTemp
    protected void onPreExecute (Boolean post, String direction, String lowSpeed, String lowTemp, String highSpeed, String highTemp) {
        Post = post;

        Params.add(Pair.create(r.getString(R.string.one_temp_direction), direction));
        Params.add(Pair.create(r.getString(R.string.one_temp_low_speed), lowSpeed));
        Params.add(Pair.create(r.getString(R.string.one_temp_low_temp), lowTemp));
        Params.add(Pair.create(r.getString(R.string.one_temp_high_speed), highSpeed));
        Params.add(Pair.create(r.getString(R.string.one_temp_high_temp), highTemp));
    }

    // TwoTemp
    protected void onPreExecute (Boolean post, String lowSpeed, String lowTemp, String highSpeed, String highTemp) {
        Post = post;

        Params.add(Pair.create(r.getString(R.string.two_temp_low_speed), lowSpeed));
        Params.add(Pair.create(r.getString(R.string.two_temp_low_temp), lowTemp));
        Params.add(Pair.create(r.getString(R.string.two_temp_high_speed), highSpeed));
        Params.add(Pair.create(r.getString(R.string.two_temp_high_temp), highTemp));
    }

    protected void onPostExecute (List<String> result) {
        delegate.processFinish(result);
    }

    private final String USER_AGENT = "Mozilla/5.0";

    // HTTP Get request
    private void sendGet(String url, List<String> results) throws Exception {

        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();
        if(!checkConnection(url)) {
            results.add(r.getString(R.string.responseCode) + Integer.toString(-1));
            return;
        }
        con.setConnectTimeout(5000);
        // optional default is GET
        con.setRequestMethod("GET");

        //add request header
        con.setRequestProperty("User-Agent", USER_AGENT);
        con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
        con.setRequestProperty("Content-Type", "application/json" );
        con.setRequestProperty("Accept", "application/json");

        int responseCode = con.getResponseCode();
        results.add("GET: " + url);
        results.add("Response code: " + Integer.toString(responseCode));

        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuffer response = new StringBuffer();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        // print result
        results.add(response.toString());

    }

    public static Integer tryParseInt(String text) {
        if (text == null)
            return null;

        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static boolean checkResponseCode(List<String> results) {
        int responseCode = -1;
        for (String string : results)
            if(string.contains("Response code:"))
                responseCode = tryParseInt(string.substring(15));

        return responseCode == HttpURLConnection.HTTP_OK;
    }

    private static final Pattern PATTERN = Pattern.compile(
            "^(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])$");

    public static boolean validate(final String ip) {
        return PATTERN.matcher(ip).matches();
    }

    public static boolean checkConnection(String strURL) {
        try {
            URL url = new URL(strURL);
            HttpURLConnection urlConn = (HttpURLConnection) url.openConnection();
            urlConn.setConnectTimeout(2000); //set timeout to 2 seconds
            urlConn.connect();

            assertEquals(HttpURLConnection.HTTP_OK, urlConn.getResponseCode());
            return true;
        } catch (SocketTimeoutException e) {
            return false;
        } catch (IOException e) {
            return false;
        }
    }

    public boolean checkConnection() {
        sharedPref = context.getSharedPreferences(r.getString(R.string.PREF_NAME), Context.MODE_PRIVATE);


        String urlBase = "http://".concat(sharedPref.getString(r.getString(R.string.PK_IP), "N/A")).concat(":")
                .concat(sharedPref.getString(r.getString(R.string.PK_Port), "N/A")).concat("/");

        if(urlBase.contains("N/A"))
            Toast.makeText(context, "Please enter an IP address and port number via the Settings menu.", Toast.LENGTH_LONG).show();

        String strURL = urlBase.concat(r.getString(R.string.getManual));

        try {
            URL url = new URL(strURL);
            HttpURLConnection urlConn = (HttpURLConnection) url.openConnection();
            urlConn.setConnectTimeout(2000); //set timeout to 2 seconds
            urlConn.connect();

            assertEquals(HttpURLConnection.HTTP_OK, urlConn.getResponseCode());
            return true;
        } catch (SocketTimeoutException e) {
            return false;
        } catch (IOException e) {
            return false;
        }
    }

    public static String formatTime(String time, boolean _12h) {
        if (!_12h)
            return time;

        Integer hrs = tryParseInt(time.substring(0, 2));
        if (hrs == null)
            return time;
        else {
            boolean pm = hrs >= 12;
            if(pm)
                hrs -= 12;

            if (hrs == 0)
                hrs = 12;

            char[] timeChars = time.toCharArray();
            timeChars[0] = (char)((hrs > 9 ? 1 : 0) + 48);
            timeChars[1] = (char)((hrs > 9 ? hrs - 10 : hrs) + 48);
            time = new String(timeChars);
            return time.concat(pm ? "p" : "a");
        }
    }

    private void sendPost(String url, List<Pair<String,String>> params, List<String> results) throws Exception {

        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();
        if(!checkConnection()) {
            results.add(r.getString(R.string.responseCode) + Integer.toString(-1));
            return;
        }
        con.setConnectTimeout(5000);

        // add request header
        con.setRequestMethod("POST");
        con.setRequestProperty("User-Agent", USER_AGENT);
        con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
        con.setRequestProperty("Content-Type", "application/json" );
        con.setRequestProperty("Accept", "application/json");

        JSONObject urlParameters = buildJSON(params);

        // send request
        con.setDoOutput(true);
        OutputStreamWriter wr = new OutputStreamWriter(con.getOutputStream(), "UTF-8");
        wr.write(urlParameters.toString());
        wr.flush();
        wr.close();

        int responseCode = con.getResponseCode();
        results.add("POST: " + url);
        results.add("Response code: " + Integer.toString(responseCode));

        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuffer response = new StringBuffer();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        // print result
        results.add(response.toString());

    }

    private JSONObject buildJSON(List<Pair<String,String>> params) {
        JSONObject data = new JSONObject();

        try {
            for (Pair<String,String> p : params) {
                data.put(p.first, p.second);
            }
        }
        catch (Exception ex) {
            Log.e(TAG, ex.toString());
        }

        return data;
    }
}

