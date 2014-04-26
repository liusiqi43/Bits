package service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.siqi.bits.Task;
import com.siqi.bits.app.MainActivity;
import com.siqi.bits.app.R;

import java.io.IOException;

import model.TaskManager;

/**
 * Created by me on 4/30/14.
 */
public class ReminderPublishReceiver extends BroadcastReceiver {
    private int NOTIFICATION_ID = R.string.task_reminder_publisher;
    private NotificationManager mNM;

    /**
     * Show a notification while this service is running.
     */
    private void publishNotificationForTask(Context ctx, Task t) {
        try {
            // The PendingIntent to launch our activity if the user selects this notification
            PendingIntent contentIntent = PendingIntent.getActivity(ctx, 0,
                    new Intent(ctx, MainActivity.class), 0);

            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(ctx)
                    .setContentTitle("Bits reminder")
                    .setContentText("You may want to do \"" + t.getDescription() + "\"?")
                    .setSmallIcon(R.drawable.ic_launcher)
                    .setAutoCancel(true)
                    .setLargeIcon(t.getCategory().getIconBitmap(ctx))
                    .setContentIntent(contentIntent);

            //Done intent
            Intent doneReceive = new Intent();
            doneReceive.setAction(ReminderScheduleService.DONE_ACTION);
            doneReceive.putExtra(ReminderScheduleService.TASK_ID, t.getId());
            PendingIntent pendingIntentDone = PendingIntent.getBroadcast(ctx, 0, doneReceive, PendingIntent.FLAG_UPDATE_CURRENT);
            notificationBuilder.addAction(R.drawable.ic_action_accept, ctx.getString(R.string.done), pendingIntentDone);

            //Skip intent
            Intent skipReceive = new Intent();
            skipReceive.setAction(ReminderScheduleService.SKIP_ACTION);
            skipReceive.putExtra(ReminderScheduleService.TASK_ID, t.getId());
            PendingIntent pendingIntentSkip = PendingIntent.getBroadcast(ctx, 0, skipReceive, PendingIntent.FLAG_UPDATE_CURRENT);
            notificationBuilder.addAction(R.drawable.ic_action_forward, ctx.getString(R.string.skip), pendingIntentSkip);


            Notification notification = notificationBuilder.build();

            // Send the notification.
            mNM.notify(t.getId().intValue(), notification);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        mNM = (NotificationManager)context.getSystemService(context.NOTIFICATION_SERVICE);
        long id = intent.getLongExtra(ReminderScheduleService.TASK_ID, -1);

        Log.d("ReminderScheduleService", "Publish Intent received");
        if (id != -1) {
            Log.d("ReminderScheduleService", "id="+id);
            publishNotificationForTask(context, TaskManager.getInstance(context).getTask(id));
        }
    }
}
