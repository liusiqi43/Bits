package service;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.siqi.bits.Task;

import managers.TaskManager;

/**
 * Created by me on 5/1/14.
 */
public class ActionOnTaskReceiver extends BroadcastReceiver {
    private TaskManager tm;
    private NotificationManager mNM;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (tm == null)
            tm = TaskManager.getInstance(context);
        if (mNM == null)
            mNM = (NotificationManager) context.getSystemService(context.NOTIFICATION_SERVICE);

        String action = intent.getAction();

        Log.d("ReminderScheduleService", "ActionOnTaskReceiver: Intent received with action = " + action);

        Long id = intent.getLongExtra(ReminderScheduleService.TASK_ID, -1);
        if (ReminderScheduleService.DONE_ACTION.equals(action)) {
            Log.d("ReminderScheduleService", "ActionOnTaskReceiver: Done id = " + id);
            if (id != -1) {
                Task t = tm.getTask(id);
                tm.setDoneActionForTask(t);
            }
        } else if (ReminderScheduleService.SKIP_ACTION.equals(action)) {
            Log.d("ReminderScheduleService", "ActionOnTaskReceiver: Skip id = " + id);
            if (id != -1) {
                Task t = tm.getTask(id);
                tm.setSkipActionForTask(t);
            }
        }

        Log.d("ReminderScheduleService", "ActionOnTaskReceiver: Cancel notification for tag = " + context.getPackageName() + " id = " + id.intValue());
        mNM.cancel(context.getPackageName(), id.intValue());
    }
}
