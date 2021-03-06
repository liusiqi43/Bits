package service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.siqi.bits.Task;
import com.siqi.bits.app.MainActivity;
import com.siqi.bits.app.R;

import java.io.IOException;

import managers.TaskManager;

/**
 * Created by me on 4/30/14.
 */
public class ReminderPublishReceiver extends BroadcastReceiver {
  private NotificationManager mNM;

  /**
   * Show a notification while this service is running.
   */
  private void publishNotificationForTask(Context ctx, Task t) {
    if (!PreferenceManager.getDefaultSharedPreferences(ctx).getBoolean("IS_NOTIFICATIONS_ON", true))
      return;

    try {
      // The PendingIntent to launch our activity if the user selects this notification
      PendingIntent contentIntent = PendingIntent.getActivity(ctx, 0,
          new Intent(ctx, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP), 0);

      NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(ctx)
          .setContentTitle(ctx.getString(R.string.bits_reminder_title))
          .setContentText(ctx.getString(R.string.reminder_you_may_want_to) + t.getDescription() +
              ctx.getString(R.string.reminder_questionmark))
          .setSmallIcon(R.drawable.ic_launcher)
          .setAutoCancel(true)
          .setLargeIcon(t.getCategory().getIconBitmap(ctx))
          .setContentIntent(contentIntent);

      //Done intent
      Intent doneReceive = new Intent();
      doneReceive.setAction(ReminderScheduleService.DONE_ACTION);
      doneReceive.putExtra(ReminderScheduleService.TASK_ID, t.getId());
      PendingIntent pendingIntentDone = PendingIntent.getBroadcast(ctx, t.getId().intValue(),
          doneReceive, PendingIntent.FLAG_UPDATE_CURRENT);
      notificationBuilder.addAction(R.drawable.ic_action_accept, ctx.getString(R.string.done),
          pendingIntentDone);

      //Skip intent
      Intent skipReceive = new Intent();
      skipReceive.setAction(ReminderScheduleService.SKIP_ACTION);
      skipReceive.putExtra(ReminderScheduleService.TASK_ID, t.getId());
      PendingIntent pendingIntentSkip = PendingIntent.getBroadcast(ctx, t.getId().intValue(),
          skipReceive, PendingIntent.FLAG_UPDATE_CURRENT);
      notificationBuilder.addAction(R.drawable.ic_action_forward, ctx.getString(R.string.skip),
          pendingIntentSkip);


      Notification notification = notificationBuilder.build();

      // Send the notification.
      Log.d("ReminderScheduleService", "publishNotificationForTask: for " + ctx.getPackageName()
          + " id=" + t.getId().intValue());
      mNM.notify(ctx.getPackageName(), t.getId().intValue(), notification);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void onReceive(Context context, Intent intent) {
    mNM = (NotificationManager) context.getSystemService(context.NOTIFICATION_SERVICE);
    long id = intent.getLongExtra(ReminderScheduleService.TASK_ID, -1);
    boolean doSomethingReminder = intent.getBooleanExtra(ReminderScheduleService
        .DO_SOMETHING_REMINDER, false);

    Log.d("ReminderScheduleService", "Publish Intent received");
    if (id != -1) {
      Log.d("ReminderScheduleService", "id=" + id);
      publishNotificationForTask(context, TaskManager.getInstance(context).getTask(id));
    }

    if (doSomethingReminder) {
      publishDoSomethingReminder(context);
    }
  }

  private void publishDoSomethingReminder(Context ctx) {
    if (!PreferenceManager.getDefaultSharedPreferences(ctx).getBoolean("IS_NOTIFICATIONS_ON", true))
      return;

    // The PendingIntent to launch our activity if the user selects this notification
    PendingIntent contentIntent = PendingIntent.getActivity(ctx, 0,
        new Intent(ctx, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP), 0);

    NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(ctx)
        .setContentTitle(ctx.getString(R.string.bits_reminder_title))
        .setContentText(ctx.getString(R.string.do_something_for_yourself_today))
        .setAutoCancel(true)
        .setLargeIcon(BitmapFactory.decodeResource(ctx.getResources(), R.drawable.ic_launcher))
        .setContentIntent(contentIntent);

    Notification notification = notificationBuilder.build();

    mNM.notify(ctx.getPackageName(), ReminderScheduleService.DO_SOMETHING_REMINDER_REQ_CODE,
        notification);
  }
}
