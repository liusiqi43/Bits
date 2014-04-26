package service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created by me on 4/30/14.
 */
public class ReminderServiceStartReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Intent startServiceIntent = new Intent(context, ReminderScheduleService.class);
        context.startService(startServiceIntent);
    }
}
