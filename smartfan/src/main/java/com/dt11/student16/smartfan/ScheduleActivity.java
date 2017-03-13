package com.dt11.student16.smartfan;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.view.ContextThemeWrapper;
import android.util.Log;
import android.util.Pair;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;

public class ScheduleActivity extends AppCompatActivity implements AsyncResponse {

    private static final String TAG = "ScheduleActivity";

    private String createURL;
    private String deleteURL;
    private String getURL;
    private String toggleURL;
    private String getOpURL;
    private String postOpURL;

    HttpRequests http;

    private ListView mainListView;
    private ScheduleAdapter adapter;

    private Context context;
    private Activity activity;
    private RelativeLayout layout;
    private PopupWindow popupWindow;

    private int maxId = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule);

        getSupportActionBar().setTitle(getString(R.string.scheduleTitle));

        context = getApplicationContext();
        activity = ScheduleActivity.this;
        layout = (RelativeLayout) findViewById(R.id.activity_schedule);

        Button btnReturn = (Button) findViewById(R.id.btnReturn);
        btnReturn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                launchMain();
            }
        });

        Button btnChangeMode = (Button) findViewById(R.id.btnChangeMode);
        btnChangeMode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setMode();
            }
        });

        FloatingActionButton btnAddSchedule = (FloatingActionButton) findViewById(R.id.btnAddSchedule);
        btnAddSchedule.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                launchEditor();
            }
        });

        createURL = getString(R.string.url).concat(getString(R.string.createSchedule));
        deleteURL = getString(R.string.url).concat(getString(R.string.deleteSchedule));
        getURL = getString(R.string.url).concat(getString(R.string.getSchedule));
        toggleURL = getString(R.string.url).concat(getString(R.string.toggleSchedule));
        getOpURL = getString(R.string.url).concat(getString(R.string.getOp));
        postOpURL = getString(R.string.url).concat(getString(R.string.postOp));

        getRequest(getURL);

    }

    private void launchMain() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        overridePendingTransition(0,0);
    }

    private void launchEditor() {
        Intent intent = new Intent(this, ScheduleEditorActivity.class);

        Bundle bundle = new Bundle();
        bundle.putInt(getString(R.string.maxId), maxId);

        String[] startTimeArray = new String[adapter.data.size()];
        String[] endTimeArray = new String[adapter.data.size()];
        String[] daysArray = new String[adapter.data.size()];
        int[] idArray = new int[adapter.data.size()];
        boolean[] enabledArray = new boolean[adapter.data.size()];

        for(int i=0; i < adapter.data.size(); ++i) {
            startTimeArray[i] = adapter.data.get(i).startTime;
            endTimeArray[i] = adapter.data.get(i).endTime;
            daysArray[i] = adapter.data.get(i).days;
            idArray[i] = adapter.data.get(i).id;
            enabledArray[i] = adapter.data.get(i).enabled;
        }

        bundle.putStringArray(getString(R.string.startTimeArray), startTimeArray);
        bundle.putStringArray(getString(R.string.endTimeArray), endTimeArray);
        bundle.putStringArray(getString(R.string.daysArray), daysArray);
        bundle.putIntArray(getString(R.string.idArray), idArray);
        bundle.putBooleanArray(getString(R.string.enabledArray), enabledArray);

        intent.putExtras(bundle);

        startActivity(intent);
    }

    private void launchEditor(Schedule schedule) {
        Intent intent =  new Intent(this, ScheduleEditorActivity.class);

        String[] startTimeArray = new String[adapter.data.size()];
        String[] endTimeArray = new String[adapter.data.size()];
        String[] daysArray = new String[adapter.data.size()];
        int[] idArray = new int[adapter.data.size()];
        boolean[] enabledArray = new boolean[adapter.data.size()];

        for(int i=0; i < adapter.data.size(); ++i) {
            startTimeArray[i] = adapter.data.get(i).startTime;
            endTimeArray[i] = adapter.data.get(i).endTime;
            daysArray[i] = adapter.data.get(i).days;
            idArray[i] = adapter.data.get(i).id;
            enabledArray[i] = adapter.data.get(i).enabled;
        }

        int scheduleId = schedule.id;
        String startTime = schedule.startTime;
        String endTime = schedule.endTime;
        String direction = schedule.direction;
        int fanSpeed = schedule.speed;
        char[] d = schedule.days.toCharArray();
        boolean[] days = {  d[0] == 'Y', d[1] == 'Y', d[2] == 'Y', d[3] == 'Y',
                            d[4] == 'Y', d[5] == 'Y', d[6] == 'Y' };

        Bundle bundle = new Bundle();
        bundle.putBoolean(getString(R.string.edit), true);
        bundle.putInt(getString(R.string.schedule_id), scheduleId);
        bundle.putString(getString(R.string.begin_time), startTime);
        bundle.putString(getString(R.string.end_time), endTime);
        bundle.putString(getString(R.string.direction), direction);
        bundle.putInt(getString(R.string.fan_speed), fanSpeed);
        bundle.putBooleanArray(getString(R.string.day), days);

        bundle.putStringArray(getString(R.string.startTimeArray), startTimeArray);
        bundle.putStringArray(getString(R.string.endTimeArray), endTimeArray);
        bundle.putStringArray(getString(R.string.daysArray), daysArray);
        bundle.putIntArray(getString(R.string.idArray), idArray);
        bundle.putBooleanArray(getString(R.string.enabledArray), enabledArray);

        intent.putExtras(bundle);

        startActivity(intent);
    }

    public void processFinish(List<String> result) {
        parseJSON(result.get(result.size()-1));
    }

    private void parseJSON(String jsonString) {
        JSONObject json;
        JSONArray jsonArray;
        ArrayList<Schedule> schedules = new ArrayList<>();
        String[] dayList = {"Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};

        try {
            if(jsonString.equals(getString(R.string.disableSuccess)) ||
                    jsonString.equals(getString(R.string.enableSuccess)))
                return;

            json = new JSONObject(jsonString);
            jsonArray = json.getJSONArray("data");

            for(int i=0; i<jsonArray.length(); ++i) {
                Schedule schedule;
                JSONObject jsonSchedule = jsonArray.getJSONObject(i);
                String id = jsonSchedule.getString(getString(R.string.schedule_id));
                int j = i + 1;
                boolean bool = false;
                char[] days = {'N','N','N','N','N','N','N'};

                days[Arrays.asList(dayList).indexOf(jsonSchedule.getString(getString(R.string.day)))] = 'Y';

                while(!bool) {
                    if (j >= jsonArray.length())
                        break;
                    JSONObject tempJson = jsonArray.getJSONObject(j);
                    String tempId = tempJson.getString(getString(R.string.schedule_id));
                    if (tempId.equals(id)) {
                        days[Arrays.asList(dayList).indexOf(tempJson.getString(getString(R.string.day)))] = 'Y';
                        j += 1;
                        if (j >= jsonArray.length()) {
                            i = j - 1;
                            bool = true;
                        }
                    }
                    else {
                        i = j - 1;
                        bool = true;
                    }
                }

                schedule = new Schedule(Integer.parseInt(jsonSchedule.getString(getString(R.string.schedule_id))),
                                        Integer.parseInt(jsonSchedule.getString(getString(R.string.fan_speed))),
                                        jsonSchedule.getString(getString(R.string.begin_time)),
                                        jsonSchedule.getString(getString(R.string.end_time)),
                                        jsonSchedule.getString(getString(R.string.direction)).equals(
                                                getString(R.string.clockwise)) ?
                                                getString(R.string.forward) :
                                                getString(R.string.reverse),
                                        new String(days),
                                        jsonSchedule.getString(getString(R.string.enabled)).equals(
                                                getString(R.string.yes)));

                schedules.add(schedule);

                maxId = Math.max(maxId, schedule.id);
            }
        }
        catch (Exception ex) {
            Log.e(TAG, ex.toString());
            return;
        }

        adapter = new ScheduleAdapter(this, R.layout.listview_schedule_item, schedules);

        mainListView = (ListView) findViewById(R.id.listSchedules);
        mainListView.setAdapter(adapter);
        mainListView.setItemsCanFocus(false);
        mainListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int pos, long id) {

                //ScheduleAdapter.ScheduleHolder schedule = (ScheduleAdapter.ScheduleHolder) adapterView.getItemAtPosition(pos);
                Schedule schedule = (Schedule) adapterView.getItemAtPosition(pos);
                launchEditor(schedule);
            }
        });
        mainListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int pos, long id) {
                Schedule schedule = (Schedule) adapterView.getItemAtPosition(pos);
                popupEditDelete(schedule, pos);

                return true;
            }
        });
    }

    private void popupEditDelete(final Schedule schedule, final int pos) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(LAYOUT_INFLATER_SERVICE);

        View customView = inflater.inflate(R.layout.popup_editdelete,null);

        popupWindow = new PopupWindow(customView, RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT);

        popupWindow.setElevation(5.0f);
        popupWindow.setOutsideTouchable(false);
        popupWindow.setFocusable(true);

        Button btnEdit = (Button) customView.findViewById(R.id.btnEdit);
        btnEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                popupWindow.dismiss();
                launchEditor(schedule);
            }
        });

        Button btnDelete = (Button) customView.findViewById(R.id.btnDelete);
        btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                popupWindow.dismiss();
                deleteDialog(getString(R.string.btnDelete), getString(R.string.alertDelete),
                            getDrawable(R.drawable.ic_delete_black_24px), schedule.id, pos);
            }
        });

        popupWindow.showAtLocation(layout, Gravity.CENTER, 0, 0);
    }

    private void getRequest(String url) {

        http = new HttpRequests(this);
        http.delegate = this;
        http.onPreExecute(false);
        http.execute(url);
    }

    private void deleteRequest(int schedule_id) {

        http = new HttpRequests(this);
        http.delegate = this;
        http.onPreExecute(true, schedule_id);
        http.execute(deleteURL);
    }

    private void toggleRequest(int schedule_id, String toggleState) {

        http = new HttpRequests(this);
        http.delegate = this;
        http.onPreExecute(true, schedule_id, toggleState);
        http.execute(toggleURL);
    }

    private void deleteDialog(String title, String message, Drawable icon, final int schedule_id,
                              final int position) {
        new AlertDialog.Builder(new ContextThemeWrapper(activity ,R.style.DialogTheme))
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(getString(R.string.btnYes), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        deleteRequest(schedule_id);
                        dialog.dismiss();
//                        finish();
//                        startActivity(getIntent());
//                        overridePendingTransition(0,0);
//                        adapter.removeItem(position);
                        adapter.remove(adapter.getItem(position));
                        adapter.notifyDataSetChanged();
                        mainListView.setAdapter(adapter);
                    }
                })
                .setNegativeButton(getString(R.string.btnNo), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .setIcon(icon)
                .show();
    }

    private void setMode() {

        String mode = getString(R.string.scheduleMode);

        http = new HttpRequests(this);
        http.delegate = this;
        http.onPreExecute(true, mode);
        http.execute(postOpURL);
    }

}
