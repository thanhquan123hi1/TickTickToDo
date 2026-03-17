package hcmute.edu.vn.ticktick.reminder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import hcmute.edu.vn.ticktick.database.AppDatabase;

public class ReminderBootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) {
            return;
        }

        String action = intent.getAction();
        if (Intent.ACTION_BOOT_COMPLETED.equals(action)
                || Intent.ACTION_LOCKED_BOOT_COMPLETED.equals(action)
                || Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) {
            AppDatabase.databaseWriteExecutor.execute(
                    () -> ReminderScheduler.rescheduleAllFromDatabase(context.getApplicationContext())
            );
        }
    }
}

