package service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
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
    AlarmManager mAlarmManager;
    private List<Task> mTasks;

    private void scheduleAllAlarms() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                reloadTasks();
                for (Task t : mTasks) {
                    scheduleForTask(t);
                }
            }
        }).start();
    }

    private void cancelAllAlarms() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                reloadTasks();
                for (Task t : mTasks) {
                    unScheduleForTask(t);
                }
            }
        }).start();
    }

    public void scheduleForTask(Task t) {
        new AsyncTask<Task, Void, Void>() {
            @Override
            protected Void doInBackground(Task... tasks) {
                Task t = tasks[0];
                if (t.getNextScheduledTime() < System.currentTimeMillis())
                    return null;

                Intent displayTaskIntent = new Intent(ReminderScheduleService.this, ReminderPublishReceiver.class);
                displayTaskIntent.putExtra(TASK_ID, t.getId());

                PendingIntent displayIntent = PendingIntent.getBroadcast(ReminderScheduleService.this, t.getId().intValue(), displayTaskIntent, 0);
                mAlarmManager.set(AlarmManager.RTC_WAKEUP, t.getNextScheduledTime() - REMINDER_DURATION, displayIntent);
                Log.d("ReminderScheduleService", "Scheduling task:" + t.getDescription() + " on " + new Date(t.getNextScheduledTime() - REMINDER_DURATION).toString());
                return null;
            }
        }.execute(t);
    }

    public void unScheduleForTask(Task t) {
        new AsyncTask<Task, Void, Void>() {
            @Override
            protected Void doInBackground(Task... tasks) {
                Task t = tasks[0];

                Intent displayTaskIntent = new Intent(ReminderScheduleService.this, ReminderPublishReceiver.class);
                displayTaskIntent.putExtra(TASK_ID, t.getId());

                if (t.getId() == null) {
                    Log.d("Debugging", "t.getId() is null!");
                }
                PendingIntent displayIntent = PendingIntent.getBroadcast(ReminderScheduleService.this, t.getId().intValue(), displayTaskIntent, 0);
                mAlarmManager.cancel(displayIntent);
                Log.d("ReminderScheduleService", "Unscheduling task:" + t.getDescription() + " on " + new Date(t.getNextScheduledTime() - REMINDER_DURATION).toString());
                return null;
            }
        }.execute(t);
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
