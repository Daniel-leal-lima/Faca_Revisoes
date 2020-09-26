package com.ex.faarevisoes;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class EventRecyclerAdapter extends RecyclerView.Adapter<EventRecyclerAdapter.MyViewHolder> {
    Context context;
    ArrayList<Events> arrayList;
    DBOpenHelper dbOpenHelper;

    public EventRecyclerAdapter(Context context, ArrayList<Events> arrayList) {
        this.context = context;
        this.arrayList = arrayList;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.show_events_rowlayout, parent, false);

        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final MyViewHolder holder, final int position) {

        final Events events = arrayList.get(position);
        holder.Event.setText(events.getEVENT());
        holder.Time.setText(events.getTIME());
        holder.Date.setText(events.getDATE());
        holder.delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {// Deleta
                deleteCalendarEvent(events.getEVENT(), events.getDATE(), events.getTIME());
                arrayList.remove(position);
                notifyDataSetChanged();
            }
        });

        if (isAlarmed(events.getDATE(),events.getEVENT(),events.getTIME())){
            holder.setAlarm.setImageResource(R.drawable.ic_notification);
        }
        else {
            holder.setAlarm.setImageResource(R.drawable.ic_notification_off);

        }
        //notifyDataSetChanged();
        Calendar datecalendar = Calendar.getInstance();
        datecalendar.setTime(convertStringToDate(events.getDATE()));
        final int alarmYear = datecalendar.get(Calendar.YEAR);
        final int alarmMonth = datecalendar.get(Calendar.MONTH);
        final int alarmDay = datecalendar.get(Calendar.DAY_OF_MONTH);
        Calendar timecalendar = Calendar.getInstance();
        timecalendar.setTime(convertStringToTime(events.getTIME()));
        final int alarmHour = timecalendar.get(Calendar.HOUR_OF_DAY);
        final int alarmMinute = timecalendar.get(Calendar.MINUTE);

        holder.setAlarm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isAlarmed(events.getDATE(),events.getEVENT(),events.getTIME())){
                    holder.setAlarm.setImageResource(R.drawable.ic_notification_off);
                    cancelAlarm(getRequestCode(events.getDATE(),events.getEVENT(),events.getTIME()));
                    updateEvent(events.getDATE(),events.getEVENT(),events.getTIME(),"off");
                }
                else {
                    holder.setAlarm.setImageResource(R.drawable.ic_notification);
                    Calendar alarmCalendar = Calendar.getInstance();
                    alarmCalendar.set(alarmYear,alarmMonth,alarmDay,alarmHour,alarmMinute);
                    setAlarm(alarmCalendar,events.getDATE(),events.getTIME(),getRequestCode(events.getDATE(),
                            events.getEVENT(),events.getTIME()));
                    updateEvent(events.getDATE(),events.getEVENT(),events.getTIME(),"on");
                }
                notifyDataSetChanged();
            }
        });

    }

    @Override
    public int getItemCount() {
        return arrayList.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {

        TextView Event, Date, Time;
        Button delete;
        ImageButton setAlarm;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            Event = itemView.findViewById(R.id.eventname);
            Date = itemView.findViewById(R.id.eventdate);
            Time = itemView.findViewById(R.id.eventtime);
            delete = itemView.findViewById(R.id.delete);
            setAlarm = itemView.findViewById(R.id.alarmBtn);
        }
    }

    private void deleteCalendarEvent(String event, String date, String time) {
        dbOpenHelper = new DBOpenHelper(context);
        SQLiteDatabase database = dbOpenHelper.getWritableDatabase();
        dbOpenHelper.deleteEvent(event, date, time, database);
        dbOpenHelper.close();

    }

    private boolean isAlarmed(String date, String event, String time) {
        boolean alarmed = false;
        dbOpenHelper = new DBOpenHelper(context);
        SQLiteDatabase database = dbOpenHelper.getReadableDatabase();
        Cursor cursor = dbOpenHelper.ReadIDEvents(date,event,time,database);
        while (cursor.moveToNext()) {
            String notify = cursor.getString(cursor.getColumnIndex(DBStructure.Notify));
            if (notify.equals("on")){
                alarmed = true;
            }
            else {
                alarmed = false;
            }
        }
        cursor.close();
        dbOpenHelper.close();
        return alarmed;

    }
    private Date convertStringToDate(String dateInString){
        java.text.SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
        Date date = null;
        try {
            date = format.parse(dateInString);

        } catch (java.text.ParseException e) {
            e.printStackTrace();
        }
        return date;
    }
    private void setAlarm(Calendar calendar, String event, String time, int RequestCode){
        Intent intent = new Intent(context.getApplicationContext(),AlarmReciever.class);
        intent.putExtra("event",event);
        intent.putExtra("time",time);
        intent.putExtra("id",RequestCode);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context,RequestCode,intent, PendingIntent.FLAG_ONE_SHOT);
        AlarmManager alarmManager = (AlarmManager) context.getApplicationContext().getSystemService(Context.ALARM_SERVICE);
        alarmManager.set(AlarmManager.RTC_WAKEUP,calendar.getTimeInMillis(),pendingIntent);
    }
    private Date convertStringToTime(String dateInString){
        java.text.SimpleDateFormat format = new SimpleDateFormat("kk:mm", Locale.ENGLISH);
        Date date = null;
        try {
            date = format.parse(dateInString);

        } catch (java.text.ParseException e) {
            e.printStackTrace();
        }
        return date;
    }

    private void cancelAlarm(int RequestCode){
        Intent intent = new Intent(context.getApplicationContext(),AlarmReciever.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context,RequestCode,intent, PendingIntent.FLAG_ONE_SHOT);
        AlarmManager alarmManager = (AlarmManager) context.getApplicationContext().getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(pendingIntent);
    }

    private int getRequestCode(String date, String event, String time){
        int code = 0;
        dbOpenHelper = new DBOpenHelper(context);
        SQLiteDatabase database = dbOpenHelper.getReadableDatabase();
        Cursor cursor = dbOpenHelper.ReadIDEvents(date,event,time,database);
        while (cursor.moveToNext()){
            code = cursor.getInt(cursor.getColumnIndex(DBStructure.ID));
        }
        cursor.close();
        dbOpenHelper.close();

        return code;

    }

    private void updateEvent(String date, String event, String time,String notify){
        dbOpenHelper = new DBOpenHelper(context);
        SQLiteDatabase database = dbOpenHelper.getWritableDatabase();
        dbOpenHelper.updateEvent(date,event,time,notify,database);
        dbOpenHelper.close();
    }
}