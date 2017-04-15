package com.dt11.student16.smartfan;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ContextThemeWrapper;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.PopupWindow;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import static java.util.Calendar.HOUR_OF_DAY;
import static java.util.Calendar.MINUTE;

public class ScheduleEditorActivity extends AppCompatActivity implements AsyncResponse {

    private static final String TAG = "ScheduleEditorActivity";

    private String createURL;
    private String deleteURL;
    private String updateURL;
    private String toggleURL;
    private String getURL;
    private String getOpURL;
    private String postOpURL;

    private Context context;
    private Activity activity;
    private RelativeLayout layout;
    private PopupWindow popupWindow;

    private int maxId = 0;
    private int scheduleId = 0;
    private boolean edit;

    private String[] startTimeArray;
    private String[] endTimeArray;
    private String[] daysArray;
    private int[] idArray;
    private boolean[] enabledArray;

    private SharedPreferences sharedPref;
    private boolean format12h = false;

    private List<Integer> overlapSchedules = new ArrayList<>();

    HttpRequests http;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule_editor);

        getSupportActionBar().setTitle(getString(R.string.scheduleEditorTitle));

        sharedPref = this.getSharedPreferences(getString(R.string.PREF_NAME), Context.MODE_PRIVATE);
        format12h = sharedPref.getBoolean(getString(R.string.PK_12h), false);

        context = getApplicationContext();
        activity = ScheduleEditorActivity.this;
        layout = (RelativeLayout) findViewById(R.id.activity_schedule_editor);

        Bundle bundle = getIntent().getExtras();

        startTimeArray = bundle.getStringArray(getString(R.string.startTimeArray));
        endTimeArray = bundle.getStringArray(getString(R.string.endTimeArray));
        daysArray = bundle.getStringArray(getString(R.string.daysArray));
        idArray = bundle.getIntArray(getString(R.string.idArray));
        enabledArray = bundle.getBooleanArray(getString(R.string.enabledArray));

        if (bundle.containsKey(getString(R.string.maxId))) {
            maxId = bundle.getInt(getString(R.string.maxId));
            edit = false;
        }
        else {
            scheduleId = bundle.getInt(getString(R.string.schedule_id));
            edit = true;

            ((TextView) findViewById(R.id.textStartTime))
                    .setText(HttpRequests.formatTime(bundle.getString(getString(R.string.begin_time)), format12h));

            ((TextView) findViewById(R.id.textEndTime))
                    .setText(HttpRequests.formatTime(bundle.getString(getString(R.string.end_time)), format12h));

            ((SeekBar) findViewById(R.id.seekSpeedSchedule))
                    .setProgress(bundle.getInt(getString(R.string.fan_speed)));

            if(bundle.getString(getString(R.string.direction))
                    .equals(getString(R.string.forward)))
                ((RadioButton) findViewById(R.id.rdoCW)).setChecked(true);
            else
                ((RadioButton) findViewById(R.id.rdoCCW)).setChecked(true);


            boolean[] days = bundle.getBooleanArray(getString(R.string.day));
            ((CheckBox) findViewById(R.id.chkSun)).setChecked(days[0]);
            ((CheckBox) findViewById(R.id.chkMon)).setChecked(days[1]);
            ((CheckBox) findViewById(R.id.chkTue)).setChecked(days[2]);
            ((CheckBox) findViewById(R.id.chkWed)).setChecked(days[3]);
            ((CheckBox) findViewById(R.id.chkThu)).setChecked(days[4]);
            ((CheckBox) findViewById(R.id.chkFri)).setChecked(days[5]);
            ((CheckBox) findViewById(R.id.chkSat)).setChecked(days[6]);
        }


        final Button btnReturn = (Button) findViewById(R.id.btnReturn);
        btnReturn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                launchSchedule();
            }
        });

        final Button btnSave = (Button) findViewById(R.id.btnSave);
        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean bool = dataValidation();
                if (bool) {

                    char[] days = {
                        ((CheckBox) findViewById(R.id.chkSun)).isChecked() ? 'Y' : 'N',
                        ((CheckBox) findViewById(R.id.chkMon)).isChecked() ? 'Y' : 'N',
                        ((CheckBox) findViewById(R.id.chkTue)).isChecked() ? 'Y' : 'N',
                        ((CheckBox) findViewById(R.id.chkWed)).isChecked() ? 'Y' : 'N',
                        ((CheckBox) findViewById(R.id.chkThu)).isChecked() ? 'Y' : 'N',
                        ((CheckBox) findViewById(R.id.chkFri)).isChecked() ? 'Y' : 'N',
                        ((CheckBox) findViewById(R.id.chkSat)).isChecked() ? 'Y' : 'N'};

                    String startTime = ((TextView) findViewById(R.id.textStartTime)).getText().toString();
                    String endTime = ((TextView) findViewById(R.id.textEndTime)).getText().toString();
                    if(format12h) {
                        startTime = from12hTo24h(startTime);
                        endTime = from12hTo24h(endTime);
                    }

                    Schedule schedule = new Schedule(
                            edit ? scheduleId : maxId + 1,
                            (((SeekBar) findViewById(R.id.seekSpeedSchedule)).getProgress()),
                            startTime,
                            endTime,
                            ((RadioButton) findViewById(R.id.rdoCW)).isChecked() ?
                                getString(R.string.clockwise) :
                                getString(R.string.counterclockwise),
                            new String(days),
                            true);

                    if(!overlapSchedules.isEmpty())
                        overlapDialog(schedule);
                    else
                        updateRequest(schedule);

                }
            }
        });

        final TextView textStartTime = (TextView) findViewById(R.id.textStartTime);
        textStartTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                popupTimePicker(getString(R.string.startText));
            }
        });

        final TextView textEndTime = (TextView) findViewById(R.id.textEndTime);
        textEndTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                popupTimePicker(getString(R.string.endText));
            }
        });

        String urlBase = "http://".concat(sharedPref.getString(getString(R.string.PK_IP), "N/A")).concat(":")
                .concat(sharedPref.getString(getString(R.string.PK_Port), "N/A")).concat("/");

        if(urlBase.contains("N/A"))
            Toast.makeText(this, "Please enter an IP address and port number via the Settings menu.", Toast.LENGTH_LONG).show();

        createURL = urlBase.concat(getString(R.string.createSchedule));
        deleteURL = urlBase.concat(getString(R.string.deleteSchedule));
        updateURL = urlBase.concat(getString(R.string.updateSchedule));
        toggleURL = urlBase.concat(getString(R.string.toggleSchedule));
        getOpURL = urlBase.concat(getString(R.string.getOp));
        postOpURL = urlBase.concat(getString(R.string.postOp));

//        createURL = getString(R.string.url).concat(getString(R.string.createSchedule));
//        deleteURL = getString(R.string.url).concat(getString(R.string.deleteSchedule));
//        updateURL = getString(R.string.url).concat(getString(R.string.updateSchedule));
//        toggleURL = getString(R.string.url).concat(getString(R.string.toggleSchedule));
//        getOpURL = getString(R.string.url).concat(getString(R.string.getOp));
//        postOpURL = getString(R.string.url).concat(getString(R.string.postOp));

    }

    private String from12hTo24h(String time) {
        Integer hrs = HttpRequests.tryParseInt(time.substring(0, 2));
        boolean pm = time.contains("p");

        if (!pm)
            return time.substring(0, time.length());

        hrs = hrs != 12 ? hrs + 12 : 0;
        char[] timeChars = time.toCharArray();
        char[] hrsChars = hrs.toString().toCharArray();
        timeChars[0] = (hrs != 0 ? hrsChars[0] : '0');
        timeChars[1] = (hrs != 0 ? hrsChars[1] : '0');
        time = new String(timeChars);
        return time.substring(0, time.length());
    }

    private void createRequest(Schedule schedule) {

        http = new HttpRequests(this);
        http.delegate = this;
        http.onPreExecute(true, schedule);
        http.execute(createURL);
    }

    private void updateRequest(Schedule schedule) {

        http = new HttpRequests(this);
        http.delegate = this;
        http.onPreExecute(true, schedule);
        http.execute(updateURL);
    }

    private void toggleRequest(int schedule_id, String toggleState) {

        http = new HttpRequests(this);
        http.delegate = this;
        http.onPreExecute(true, schedule_id, toggleState);
        http.execute(toggleURL);
    }

    private void deleteRequest(int schedule_id) {

        http = new HttpRequests(this);
        http.delegate = this;
        http.onPreExecute(true, schedule_id);
        http.execute(deleteURL);
    }

    private void launchSchedule() {
        Intent intent = new Intent(this, ScheduleActivity.class);
        startActivity(intent);
        overridePendingTransition(0,0);
    }

    private void popupTimePicker(final String startOrEnd) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(LAYOUT_INFLATER_SERVICE);

        View customView = inflater.inflate(R.layout.popup_timepicker,null);

        popupWindow = new PopupWindow(customView, RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT);

        popupWindow.setElevation(5.0f);
        popupWindow.setOutsideTouchable(false);
        popupWindow.setFocusable(true);

        Calendar cal;
        if(startOrEnd == getString(R.string.startText))
            cal = parseDateString(((TextView) findViewById(R.id.textStartTime)).getText().toString(),
                getString(R.string.timeMilitaryNoSeconds));
        else
            cal = parseDateString(((TextView) findViewById(R.id.textEndTime)).getText().toString(),
                getString(R.string.timeMilitaryNoSeconds));

        final TimePicker timePicker = (TimePicker) customView.findViewById(R.id.timePicker);
        if(Build.VERSION.SDK_INT>=23){
            timePicker.setHour(cal.get(HOUR_OF_DAY));
            timePicker.setMinute(cal.get(MINUTE));
        }
        else {
            timePicker.setCurrentHour(cal.get(HOUR_OF_DAY));
            timePicker.setCurrentMinute(cal.get(MINUTE));
        }

        timePicker.setIs24HourView(!format12h);

        Button cancelButton = (Button) customView.findViewById(R.id.btnCancel);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                popupWindow.dismiss();
            }
        });

        Button okayButton = (Button) customView.findViewById(R.id.btnOkay);
        okayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int hour;
                int minute;
                String hours, minutes, time;
                TextView output;

                if(Build.VERSION.SDK_INT>=23){
                    hour = timePicker.getHour();
                    minute = timePicker.getMinute();
                }
                else {
                    hour = timePicker.getCurrentHour();
                    minute = timePicker.getCurrentMinute();
                }

                hours = hour < 10 ? "0".concat((Integer.toString(hour))) : Integer.toString(hour);
                minutes = minute < 10 ? "0".concat((Integer.toString(minute))) : Integer.toString(minute);
                time = hours.concat(":").concat(minutes);

                if(startOrEnd == getString(R.string.startText)) {
                    ((TextView) layout.findViewById(R.id.textStartTime)).setText(HttpRequests.formatTime(time, format12h));
                }
                else if(startOrEnd == getString(R.string.endText)) {
                    ((TextView) layout.findViewById(R.id.textEndTime)).setText(HttpRequests.formatTime(time, format12h));
                }

                popupWindow.dismiss();
            }
        });
        popupWindow.showAtLocation(layout, Gravity.CENTER, 0, 0);
    }

    private boolean dataValidation() {
        TextView textStart = (TextView) findViewById(R.id.textStartTime);
        TextView textEnd = (TextView) findViewById(R.id.textEndTime);
        RadioGroup rdoGrp = (RadioGroup) findViewById(R.id.rdoGrpDirection);
        CheckBox chkSun = (CheckBox) findViewById(R.id.chkSun);
        CheckBox chkMon = (CheckBox) findViewById(R.id.chkMon);
        CheckBox chkTue = (CheckBox) findViewById(R.id.chkTue);
        CheckBox chkWed = (CheckBox) findViewById(R.id.chkWed);
        CheckBox chkThu = (CheckBox) findViewById(R.id.chkThu);
        CheckBox chkFri = (CheckBox) findViewById(R.id.chkFri);
        CheckBox chkSat = (CheckBox) findViewById(R.id.chkSat);
        Button btnSave = (Button) findViewById(R.id.btnSave);

        boolean bool = true;
        String errorString = "";

        // Check start time is before end time
        String dateFormat = getString(R.string.timeMilitaryNoSeconds);
        String st = textStart.getText().toString();
        String et = textEnd.getText().toString();
        if(format12h) {
            st = from12hTo24h(st);
            et = from12hTo24h(et);
        }
        Calendar startTime = parseDateString(st, dateFormat);
        Calendar endTime = parseDateString(et, dateFormat);
        if(startTime.compareTo(endTime) >= 0) {
            if(!errorString.isEmpty())
                errorString = errorString.concat("\n");
            errorString = errorString.concat(getString(R.string.errorStartAfterEnd));
            bool = false;
        }

        if (rdoGrp.getCheckedRadioButtonId() == -1) {
            if(!errorString.isEmpty())
                errorString = errorString.concat("\n");
            errorString = errorString.concat(getString(R.string.errorNoDirection));
            bool = false;
        }

        if (!(chkSun.isChecked() || chkMon.isChecked() || chkTue.isChecked() || chkWed.isChecked()
                || chkThu.isChecked() || chkFri.isChecked() || chkSat.isChecked() )) {
            if(!errorString.isEmpty())
                errorString = errorString.concat("\n");
            errorString = errorString.concat(getString(R.string.errorNoDaysSelected));
            bool = false;
        }

        if(!bool) {
            alertDialog(getString(R.string.error), errorString,
                    getDrawable(R.drawable.ic_error_black_24px), true);
            return false;
        }

        for(int i=0; i < startTimeArray.length; ++i) {

            // skip iteration if target is disabled
            // or if in edit mode and target schedule currently being edited
            if(!enabledArray[i] || (edit && idArray[i] == scheduleId))
                continue;

            // if there's no overlap of days, skip the rest of this iteration
            char[] daysChar = daysArray[i].toCharArray();
            boolean dayOverlap = (chkSun.isChecked() && (daysChar[0] == 'Y')) ||
                                (chkMon.isChecked() && (daysChar[1] == 'Y')) ||
                                (chkTue.isChecked() && (daysChar[2] == 'Y')) ||
                                (chkWed.isChecked() && (daysChar[3] == 'Y')) ||
                                (chkThu.isChecked() && (daysChar[4] == 'Y')) ||
                                (chkFri.isChecked() && (daysChar[5] == 'Y')) ||
                                (chkSat.isChecked() && (daysChar[6] == 'Y'));
            if(!dayOverlap)
                continue;

            Calendar tempStart = parseDateString(startTimeArray[i], dateFormat);
            Calendar tempEnd = parseDateString(endTimeArray[i], dateFormat);

            if((startTime.after(tempStart) && startTime.before(tempEnd)) ||
                (endTime.after(tempStart) && endTime.before(tempEnd)))
                    overlapSchedules.add(idArray[i]);
        }
        return true;
    }

    private Calendar parseDateString(String dateString, String formatString) {
        SimpleDateFormat format = new SimpleDateFormat(formatString);
        Calendar cal = Calendar.getInstance();
        try {
            cal.setTime(format.parse(dateString));
        }
        catch (ParseException parEx) {
            Log.e(TAG, parEx.toString());
        }
        return cal;
    }

    public void processFinish(List<String> result) {
        parseJSON(result.get(result.size()-1));
    }

    private void parseJSON(String jsonString) {

        if(jsonString.equals(getString(R.string.success))) {
            // show user Success dialog, then return to ScheduleActivity
                alertDialog(getString(R.string.success), getString(R.string.successChangesSaved),
                        getDrawable(R.drawable.ic_check_circle_black_24px), false);

                new Timer().schedule(
                        new TimerTask() {
                            @Override
                            public void run() {
                                launchSchedule();
                            }
                        }, 1500
                );
        }
        else if(jsonString.equals(getString(R.string.disableSuccess)) || jsonString.equals(getString(R.string.enableSuccess)))
            return;
        else
            alertDialog(getString(R.string.error), jsonString,
                    getDrawable(R.drawable.ic_error_black_24px), true);

    }

    private void alertDialog(String title, String message, Drawable icon, boolean button) {
        if (button)
            new AlertDialog.Builder(new ContextThemeWrapper(activity ,R.style.DialogTheme))
                    .setTitle(title)
                    .setMessage(message)
                    .setPositiveButton(getString(R.string.btnOkay), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    })
                    .setIcon(icon)
                    .show();
        else
            new AlertDialog.Builder(new ContextThemeWrapper(activity ,R.style.DialogTheme))
                    .setTitle(title)
                    .setMessage(message)
                    .setIcon(icon)
                    .show();
    }

    private void overlapDialog(final Schedule schedule) {
        new AlertDialog.Builder(new ContextThemeWrapper(activity, R.style.DialogTheme))
                    .setTitle(getString(R.string.alertOverlap))
                    .setMessage(overlapSchedules.size() != 1 ? getString(R.string.alertTwoOverlap) :
                                            getString(R.string.alertOneOverlap))
                    .setPositiveButton(getString(R.string.btnOkay), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            for(int i=0; i < overlapSchedules.size(); ++i) {
                                toggleRequest(overlapSchedules.get(i), getString(R.string.no));
                            }
                            updateRequest(schedule);
                        }
                    })
                    .setNegativeButton(getString(R.string.btnCancel), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    })
                    .setIcon(getDrawable(R.drawable.ic_warning_black_24px))
                    .show();
    }
}
