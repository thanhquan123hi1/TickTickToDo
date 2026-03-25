package hcmute.edu.vn.ticktick.reminder;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import hcmute.edu.vn.ticktick.MainActivity;
import hcmute.edu.vn.ticktick.R;
import hcmute.edu.vn.ticktick.database.AppDatabase;
import hcmute.edu.vn.ticktick.database.Task;
import hcmute.edu.vn.ticktick.widget.TasksWidgetProvider;

public class TaskReminderReceiver extends BroadcastReceiver {

    public static final String EXTRA_TASK_ID = "extra_task_id";
    public static final String EXTRA_MINUTES_BEFORE = "extra_minutes_before";

    private static final String CHANNEL_ID = "deadline_reminders";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        int taskId = intent.getIntExtra(EXTRA_TASK_ID, -1);
        int minutesBefore = intent.getIntExtra(EXTRA_MINUTES_BEFORE, 0);
        if (taskId <= 0 || minutesBefore <= 0) {
            return;
        }

        AppDatabase.databaseWriteExecutor.execute(() -> {
            Task task = AppDatabase.getDatabase(context).taskDao().getTaskByIdSync(taskId);
            if (task == null || task.isCompleted()) {
                return;
            }

            String taskTitle = task.getTitle();
            if (taskTitle == null || taskTitle.trim().isEmpty()) {
                taskTitle = context.getString(R.string.app_name);
            }

            createChannel(context);
            showNotification(context, taskId, taskTitle, minutesBefore);
        });
    }

    private void showNotification(Context context,
                                  int taskId,
                                  String taskTitle,
                                  int minutesBefore) {
        Intent openIntent = new Intent(context, MainActivity.class);
        openIntent.setAction(TasksWidgetProvider.ACTION_OPEN_TASK_DETAIL);
        openIntent.putExtra(TasksWidgetProvider.EXTRA_TASK_ID, taskId);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent contentIntent = PendingIntent.getActivity(
                context,
                taskId,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        String whenText = hcmute.edu.vn.ticktick.ui.DateUtils.formatReminderOffset(context, minutesBefore);
        String contentText = context.getString(R.string.reminder_notification_body, whenText);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_today)
                .setContentTitle(taskTitle)
                .setContentText(contentText)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(contentIntent);

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        try {
            NotificationManagerCompat.from(context).notify(taskId * 1000 + minutesBefore, builder.build());
        } catch (SecurityException ignored) {
            // Permission might be revoked between check and notify.
        }
    }

    private void createChannel(Context context) {
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager == null) {
            return;
        }

        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.reminder_channel_name),
                NotificationManager.IMPORTANCE_HIGH
        );
        channel.setDescription(context.getString(R.string.reminder_channel_description));
        manager.createNotificationChannel(channel);
    }
}
