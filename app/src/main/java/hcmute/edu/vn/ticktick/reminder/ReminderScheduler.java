package hcmute.edu.vn.ticktick.reminder;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import hcmute.edu.vn.ticktick.database.AppDatabase;
import hcmute.edu.vn.ticktick.database.Task;
import hcmute.edu.vn.ticktick.database.TaskReminder;
import hcmute.edu.vn.ticktick.ui.DateUtils;

public final class ReminderScheduler {

    private ReminderScheduler() {
    }

    public static void replaceTaskReminders(Context context, Task task, List<Integer> oldMinutes, List<Integer> newMinutes) {
        cancelTaskReminders(context, task.getId(), oldMinutes);
        scheduleTaskReminders(context, task, newMinutes);
    }

    public static void scheduleTaskReminders(Context context, Task task, List<Integer> reminderMinutes) {
        if (task == null || task.getId() <= 0 || task.isCompleted()) {
            return;
        }

        long dueDateTimeMillis = DateUtils.getDueDateTimeMillis(task.getDueDate(), task.getDueTime());
        if (dueDateTimeMillis <= 0) {
            return;
        }

        for (int minutesBefore : sanitizeMinutes(reminderMinutes)) {
            long triggerAtMillis = dueDateTimeMillis - minutesBefore * 60_000L;
            if (triggerAtMillis <= System.currentTimeMillis()) {
                continue;
            }
            scheduleSingle(context, task, minutesBefore, triggerAtMillis);
        }
    }

    public static void cancelTaskReminders(Context context, int taskId, List<Integer> reminderMinutes) {
        if (taskId <= 0) {
            return;
        }
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            return;
        }

        for (int minutesBefore : sanitizeMinutes(reminderMinutes)) {
            PendingIntent pendingIntent = buildPendingIntent(context, taskId, minutesBefore, PendingIntent.FLAG_NO_CREATE);
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent);
                pendingIntent.cancel();
            }
        }
    }

    public static void rescheduleAllFromDatabase(Context context) {
        AppDatabase db = AppDatabase.getDatabase(context);
        List<TaskReminder> reminders = db.taskReminderDao().getAllSync();
        for (TaskReminder reminder : reminders) {
            Task task = db.taskDao().getTaskByIdSync(reminder.getTaskId());
            if (task == null) {
                continue;
            }
            List<Integer> singleReminder = Collections.singletonList(reminder.getMinutesBefore());
            scheduleTaskReminders(context, task, singleReminder);
        }
    }

    private static void scheduleSingle(Context context,
                                       Task task,
                                       int minutesBefore,
                                       long triggerAtMillis) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            return;
        }

        PendingIntent pendingIntent = buildPendingIntent(
                context,
                task.getId(),
                minutesBefore,
                PendingIntent.FLAG_UPDATE_CURRENT
        );

        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
    }

    private static PendingIntent buildPendingIntent(Context context,
                                                    int taskId,
                                                    int minutesBefore,
                                                    int pendingIntentFlag) {
        Intent intent = new Intent(context, TaskReminderReceiver.class);
        intent.putExtra(TaskReminderReceiver.EXTRA_TASK_ID, taskId);
        intent.putExtra(TaskReminderReceiver.EXTRA_MINUTES_BEFORE, minutesBefore);

        int requestCode = taskId * 10_000 + Math.min(minutesBefore, 9_999);
        return PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                pendingIntentFlag | PendingIntent.FLAG_IMMUTABLE
        );
    }

    private static List<Integer> sanitizeMinutes(List<Integer> reminderMinutes) {
        if (reminderMinutes == null || reminderMinutes.isEmpty()) {
            return Collections.emptyList();
        }

        List<Integer> sanitized = new ArrayList<>();
        for (Integer value : reminderMinutes) {
            if (value != null && value > 0 && !sanitized.contains(value)) {
                sanitized.add(value);
            }
        }
        Collections.sort(sanitized);
        return sanitized;
    }
}
