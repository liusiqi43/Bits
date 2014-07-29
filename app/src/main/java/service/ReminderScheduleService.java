package service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

import com.siqi.bits.Task;

import java.util.Date;
import java.util.List;

import managers.TaskManager;
import utils.Utils;

/**
 * Created by me on 4/30/14.
 */
public class ReminderScheduleService extends Service {
    public static final String DONE_ACTION = "com.siqi.bits.intent.action.DONE_ACTION";
    public static final String SKIP_ACTION = "com.siqi.bits.intent.action.SKIP_ACTION";
    public static final String TASK_ID = "TASK_ID";
    public static final int MINUTE = 60 * 1000;

    // This is the object that receives interactions from clients.  See
    // RemoteService for a more complete example.
    private final IBinder mBinder = new LocalBinder();
    AlarmManager mAlarmManager;
    private SharedPreferences mPreferences;
    private List<Task> mTasks;

    private void scheduleAllAlarms() {
        reloadTasks();
        for (Task t : mTasks) {
            scheduleForTask(t);
        }
    }

    private void cancelAllAlarms() {
        reloadTasks();
        for (Task t : mTasks) {
            unScheduleForTask(t);
        }
    }

    public void scheduleForTask(Task t) {
        if (t.getNextScheduledTime() - Integer.parseInt(mPreferences.getString("NOTIFY_MINUTES_COUNT_BEFORE_LATE", "30")) * MINUTE <= Utils.currentTimeMillis())
            return;

        Intent displayTaskIntent = new Intent(ReminderScheduleService.this, ReminderPublishReceiver.class);
        displayTaskIntent.putExtra(TASK_ID, t.getId());

        PendingIntent displayIntent = PendingIntent.getBroadcast(ReminderScheduleService.this, t.getId().intValue(), displayTaskIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_ONE_SHOT);
        mAlarmManager.set(AlarmManager.RTC_WAKEUP, t.getNextScheduledTime() - Integer.parseInt(mPreferences.getString("NOTIFY_MINUTES_COUNT_BEFORE_LATE", "30")) * MINUTE, displayIntent);
        Log.d("ReminderScheduleService", "Scheduling task:" + t.getId().intValue() + " on " + new Date(t.getNextScheduledTime() - Integer.parseInt(mPreferences.getString("NOTIFY_MINUTES_COUNT_BEFORE_LATE", "30")) * MINUTE).toString());
    }

    public void unScheduleForTask(Task t) {
        Intent displayTaskIntent = new Intent(ReminderScheduleService.this, ReminderPublishReceiver.class);
        displayTaskIntent.putExtra(TASK_ID, t.getId());

        if (t.getId() == null) {
            Log.d("Debugging", "t.getId() is null!");
        }
        PendingIntent displayIntent = PendingIntent.getBroadcast(ReminderScheduleService.this, t.getId().intValue(), displayTaskIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_ONE_SHOT);
        mAlarmManager.cancel(displayIntent);
        displayIntent.cancel();
        Log.d("ReminderScheduleService", "Unscheduling task:" + t.getId().intValue() + " on " + new Date(t.getNextScheduledTime() - Integer.parseInt(mPreferences.getString("NOTIFY_MINUTES_COUNT_BEFORE_LATE", "30")) * MINUTE).toString());
    }

    /**
     * May not cancel all alarms properly
     */
    private void resetAllAlarms() {
        cancelAllAlarms();
        scheduleAllAlarms();
    }

    private void reloadTasks() {
        mTasks = TaskManager.getInstance(getApplicationContext()).getAllSortedTasks();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mAlarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        mPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        scheduleAllAlarms();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public class LocalBinder extends Binder {
        public ReminderScheduleService getService() {
            return ReminderScheduleService.this;
        }
    }

}
