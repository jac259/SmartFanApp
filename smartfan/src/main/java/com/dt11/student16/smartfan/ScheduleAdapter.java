package com.dt11.student16.smartfan;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.util.Pair;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.GridLayout;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import static android.view.View.TEXT_ALIGNMENT_GRAVITY;
import static android.view.ViewGroup.FOCUS_BLOCK_DESCENDANTS;

public class ScheduleAdapter extends ArrayAdapter<Schedule> implements AsyncResponse {

    private static final String TAG = "ScheduleAdapter";

    SharedPreferences sharedPref;

    Context context;
    int layoutResourceId;
    ArrayList<Schedule> data = null;
    ScheduleHolder holder;

    HttpRequests http;

    private String updateURL;
    private String toggleURL;
    private Resources r;

    private boolean format12h;

    public ScheduleAdapter(Context context, int layoutResourceId, ArrayList<Schedule> data, boolean _format12h) {
        super(context, layoutResourceId, data);
        this.layoutResourceId = layoutResourceId;
        this.context = context;
        this.data = data;
        r = context.getResources();
        format12h = _format12h;
    }

    public void removeItem(int position) {
        this.remove(this.getItem(position));
        this.notifyDataSetChanged();
    }

    @Override
    public void processFinish(List<String> result) {

    }

    @Override
    public View getView(int position, View convertView, final ViewGroup parent) {
        View row = convertView;
        holder = row == null ? new ScheduleHolder() : (ScheduleHolder) row.getTag();

        sharedPref = context.getSharedPreferences(r.getString(R.string.PREF_NAME), Context.MODE_PRIVATE);

        String urlBase = "http://".concat(sharedPref.getString(r.getString(R.string.PK_IP), "N/A")).concat(":")
                .concat(sharedPref.getString(r.getString(R.string.PK_Port), "N/A")).concat("/");

        if(urlBase.contains("N/A"))
            Toast.makeText(context, "Please enter an IP address and port number via the Settings menu.", Toast.LENGTH_LONG).show();

        updateURL = urlBase.concat(r.getString(R.string.updateSchedule));
        toggleURL = urlBase.concat(r.getString(R.string.toggleSchedule));

        if(row == null) {
            LayoutInflater inflater = ((Activity) context).getLayoutInflater();
            row = inflater.inflate(layoutResourceId, parent, false);

            holder.seekSpeed = (SeekBar) row.findViewById(R.id.seekSpeedSchedule);
            holder.textStart = (TextView) row.findViewById(R.id.textStart);
            holder.textEnd = (TextView) row.findViewById(R.id.textEnd);
            holder.textHyphen = (TextView) row.findViewById(R.id.textHyphen);
            holder.textDirection = (TextView) row.findViewById(R.id.textDirection);
            holder.textSunday = (TextView) row.findViewById(R.id.textSunday);
            holder.textMonday = (TextView) row.findViewById(R.id.textMonday);
            holder.textTuesday = (TextView) row.findViewById(R.id.textTuesday);
            holder.textWednesday = (TextView) row.findViewById(R.id.textWednesday);
            holder.textThursday = (TextView) row.findViewById(R.id.textThursday);
            holder.textFriday = (TextView) row.findViewById(R.id.textFriday);
            holder.textSaturday = (TextView) row.findViewById(R.id.textSaturday);
            holder.textSpacer = (TextView) row.findViewById(R.id.textSpacer);
            holder.switchEnabled = (Switch) row.findViewById(R.id.switchEnabled);

            row.setTag(holder);
        }

        final Schedule schedule = data.get(position);

        // set progress of seek bar and turn off interaction
        holder.seekSpeed.setMax(r.getInteger(R.integer.scale49));
        holder.seekSpeed.setProgress(schedule.speed);
        holder.seekSpeed.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                return true;
            }
        });
        holder.seekSpeed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });
        holder.seekSpeed.setFocusable(false);
        holder.seekSpeed.setFocusableInTouchMode(false);

        // load other properties into holder
        holder.textStart.setText(HttpRequests.formatTime(schedule.startTime, format12h));
        holder.textEnd.setText(HttpRequests.formatTime(schedule.endTime, format12h));
        holder.textHyphen.setGravity(format12h ? Gravity.CENTER : Gravity.START | Gravity.CENTER_HORIZONTAL);
        holder.textDirection.setText(schedule.direction);
        setDays(schedule.days, holder);
        holder.switchEnabled.setChecked(schedule.enabled);
        holder.switchEnabled.setFocusable(false);
        holder.switchEnabled.setFocusableInTouchMode(false);
        holder.switchEnabled.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(compoundButton.isPressed()) {
                    int pos = data.indexOf(schedule);
                    List<Pair<Integer,Integer>> overlapSchedules = checkOverlap(pos);
                    if(!overlapSchedules.isEmpty() && b) {
                        Toast.makeText(context, r.getString(R.string.toastOverlap), Toast.LENGTH_SHORT).show();
                        for (int i = 0; i < overlapSchedules.size(); ++i) {
                            Pair<Integer,Integer> sched = overlapSchedules.get(i);
                            toggleRequest(sched.first, r.getString(R.string.no));
                            data.get(sched.second).enabled = false;
                            ((Switch) parent.getChildAt(sched.second)
                                    .findViewById(R.id.switchEnabled)).setChecked(false);
                        }
                    }
                    toggleRequest(schedule.id, b ? r.getString(R.string.yes) : r.getString(R.string.no));
                    data.get(pos).enabled = b;
                    notifyDataSetChanged();
                }
            }
        });

        GridLayout gridListSchedule = (GridLayout) row.findViewById(R.id.gridListSchedule);
        gridListSchedule.setDescendantFocusability(FOCUS_BLOCK_DESCENDANTS);

        return row;
    }

    private void updateRequest(Schedule schedule) {

        http = new HttpRequests(context);
        http.delegate = this;
        http.onPreExecute(true, schedule);
        http.execute(updateURL);
    }

    private void toggleRequest(int schedule_id, String toggleState) {

        http = new HttpRequests(context);
        http.delegate = this;
        http.onPreExecute(true, schedule_id, toggleState);
        http.execute(toggleURL);
    }

    private void setDays(String dayString, ScheduleHolder holder) {
        Boolean[] days = new Boolean[7];
        int activeColor = ContextCompat.getColor(context, R.color.colorPrimaryLight);
        int inactiveColor = ContextCompat.getColor(context, R.color.colorBackground);
        int hidCount = 0;

        for (int i = 0; i < 7; i++) {
            days[i] = (dayString.charAt(i) == 'Y');
            hidCount = hidCount + (days[i] ? 0 : 1);
        }

        holder.textSunday.setTextColor(days[0] ? activeColor : inactiveColor);
        holder.textMonday.setTextColor(days[1] ? activeColor : inactiveColor);
        holder.textTuesday.setTextColor(days[2] ? activeColor : inactiveColor);
        holder.textWednesday.setTextColor(days[3] ? activeColor : inactiveColor);
        holder.textThursday.setTextColor(days[4] ? activeColor : inactiveColor);
        holder.textFriday.setTextColor(days[5] ? activeColor : inactiveColor);
        holder.textSaturday.setTextColor(days[6] ? activeColor : inactiveColor);
    }

    private List<Pair<Integer,Integer>> checkOverlap(int position) {
        Schedule enableSchedule = data.get(position);
        String dateFormat = r.getString(R.string.timeMilitaryNoSeconds);
        List<Pair<Integer,Integer>> overlapSchedules = new ArrayList<>();

        char[] enableChar = enableSchedule.days.toCharArray();
        Calendar enableStart = parseDateString(enableSchedule.startTime, dateFormat);
        Calendar enableEnd = parseDateString(enableSchedule.endTime, dateFormat);

        for(int i=0; i < data.size(); ++i) {
            Schedule checkSchedule = data.get(i);

            // skip iteration if target is disabled or if enableSchedule reached in list
            if(!checkSchedule.enabled || enableSchedule.id == checkSchedule.id)
                continue;

            // if there's no overlap of days, skip the rest of this iteration
            char[] daysChar = checkSchedule.days.toCharArray();
            boolean dayOverlap = (enableChar[0] == 'Y' && (daysChar[0] == 'Y')) ||
                                 (enableChar[1] == 'Y' && (daysChar[1] == 'Y')) ||
                                 (enableChar[2] == 'Y' && (daysChar[2] == 'Y')) ||
                                 (enableChar[3] == 'Y' && (daysChar[3] == 'Y')) ||
                                 (enableChar[4] == 'Y' && (daysChar[4] == 'Y')) ||
                                 (enableChar[5] == 'Y' && (daysChar[5] == 'Y')) ||
                                 (enableChar[6] == 'Y' && (daysChar[6] == 'Y'));
            if(!dayOverlap)
                continue;

            Calendar tempStart = parseDateString(checkSchedule.startTime, dateFormat);
            Calendar tempEnd = parseDateString(checkSchedule.endTime, dateFormat);

            if((enableStart.after(tempStart) && enableStart.before(tempEnd)) ||
                    (enableEnd.after(tempStart) && enableEnd.before(tempEnd)))
                overlapSchedules.add(new Pair<>(checkSchedule.id, i));
        }

        return overlapSchedules;

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

    public static class ScheduleHolder {
        SeekBar seekSpeed;
        TextView textStart;
        TextView textEnd;
        TextView textHyphen;
        TextView textDirection;
        TextView textSunday;
        TextView textMonday;
        TextView textTuesday;
        TextView textWednesday;
        TextView textThursday;
        TextView textFriday;
        TextView textSaturday;
        TextView textSpacer;
        Switch switchEnabled;
    }
}
