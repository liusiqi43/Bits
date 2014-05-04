package service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.siqi.bits.Task;

import java.util.Date;
import java.util.List;

import model.TaskManager;

/**
 * Created by me on 4/30/14.
 */
public class ReminderScheduleService extends Service {
    public static final String DONE_ACTION = "com.siqi.bits.intent.action.DONE_ACTION";
    public static final String SKIP_ACTION = "com.siqi.bits.intent.action.SKIP_ACTION";
    public static final String TASK_ID = "TASK_ID";
    public static final int REMINDER_DURATION = 5 * 60 * 1000;
    // This is the object that receives interactions from clients.  See
    // RemoteService for a more complete example.
    private final IBinder mBinder = new LocalBinder();
    private List<Task> mTasks;
    AlarmManager mAlarmManager;

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
        if (t.getNextScheduledTime() < System.currentTimeMillis())
            return;

        // Display a notification about us starting.  We put an icon in the status bar.
        Intent displayTaskIntent = new Intent(this, ReminderPublishReceiver.class);
        displayTaskIntent.putExtra(TASK_ID, t.getId());

        PendingIntent displayIntent = PendingIntent.getBroadcast(this, t.getId().intValue(), displayTaskIntent, 0);
        mAlarmManager.set(AlarmManager.RTC_WAKEUP, t.getNextScheduledTime() - REMINDER_DURATION, displayIntent);
        Log.d("ReminderScheduleService", "Scheduling task:" + t.getDescription() + " on " + new Date(t.getNextScheduledTime() - REMINDER_DURATION).toString());
    }

    public void unScheduleForTask(Task t) {
        Intent displayTaskIntent = new Intent(this, ReminderPublishReceiver.class);
        displayTaskIntent.putExtra(TASK_ID, t.getId());

        PendingIntent displayIntent = PendingIntent.getBroadcast(this, t.getId().intValue(), displayTaskIntent, 0);
        mAlarmManager.cancel(displayIntent);
        Log.d("ReminderScheduleService", "Unscheduling task:" + t.getDescription() + " on " + new Date(t.getNextScheduledTime() - REMINDER_DURATION).toString());
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
