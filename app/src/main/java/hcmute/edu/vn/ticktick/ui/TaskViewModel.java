package hcmute.edu.vn.ticktick.ui;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import java.util.ArrayList;
import java.util.List;

import hcmute.edu.vn.ticktick.database.AppDatabase;
import hcmute.edu.vn.ticktick.database.Task;
import hcmute.edu.vn.ticktick.database.TaskDao;
import hcmute.edu.vn.ticktick.database.TaskReminder;
import hcmute.edu.vn.ticktick.database.TaskReminderDao;
import hcmute.edu.vn.ticktick.reminder.ReminderScheduler;
import hcmute.edu.vn.ticktick.widget.TodayTasksWidgetProvider;

public class TaskViewModel extends AndroidViewModel {

    private static final String TAG = "TaskViewModel";

    private final TaskDao taskDao;
    private final TaskReminderDao taskReminderDao;

    public TaskViewModel(@NonNull Application application) {
        super(application);
        AppDatabase db = AppDatabase.getDatabase(application);
        taskDao = db.taskDao();
        taskReminderDao = db.taskReminderDao();
    }

    // === Task queries ===

    public LiveData<List<Task>> getAllActiveTasks() {
        return taskDao.getAllActiveTasks();
    }

    public LiveData<List<Task>> getTasksForToday() {
        return taskDao.getTasksForToday(
                DateUtils.getStartOfToday(),
                DateUtils.getEndOfToday()
        );
    }

    public LiveData<List<Task>> getTasksForTomorrow() {
        return taskDao.getTasksForTomorrow(
                DateUtils.getStartOfTomorrow(),
                DateUtils.getEndOfTomorrow()
        );
    }

    public LiveData<List<Task>> getTasksUpcoming() {
        return taskDao.getTasksUpcoming(
                DateUtils.getStartOfDayAfterTomorrow(),
                DateUtils.getEndOf7Days()
        );
    }

    public LiveData<List<Task>> getTasksByCategory(int categoryId) {
        return taskDao.getTasksByCategory(categoryId);
    }

    public LiveData<List<Task>> getUnscheduledTasks() {
        return taskDao.getUnscheduledTasks();
    }

    public LiveData<List<Task>> getCompletedTasks() {
        return taskDao.getCompletedTasks();
    }

    public LiveData<List<Task>> getTasksThisWeek() {
        return taskDao.getTasksThisWeek(
                DateUtils.getStartOfWeek(),
                DateUtils.getEndOfWeek()
        );
    }

    public LiveData<List<Task>> getTasksForDate(long startOfDay, long endOfDay) {
        return taskDao.getTasksForDate(startOfDay, endOfDay);
    }

    public LiveData<List<Task>> getTasksForDateRange(long startDate, long endDateExclusive) {
        return taskDao.getTasksForDateRange(startDate, endDateExclusive);
    }

    // === Task operations ===

    public void insertTask(Task task) {
        insertTask(task, new ArrayList<>());
    }

    public void insertTask(Task task, List<Integer> reminderMinutes) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            long id = taskDao.insert(task);
            int taskId = (int) id;
            task.setId(taskId);

            List<Integer> safeMinutes = sanitizeReminderMinutes(reminderMinutes);
            replaceReminderRows(taskId, safeMinutes);
            ReminderScheduler.scheduleTaskReminders(getApplication(), task, safeMinutes);

            Log.d(TAG, "insertTask() id=" + id + " -> refresh widgets");
            TodayTasksWidgetProvider.refreshAllWidgets(getApplication());
        });
    }

    public void updateTask(Task task) {
        updateTask(task, null);
    }

    public void updateTask(Task task, List<Integer> reminderMinutes) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            List<Integer> oldMinutes = taskReminderDao.getReminderMinutesByTaskId(task.getId());

            List<Integer> targetMinutes;
            if (reminderMinutes != null) {
                targetMinutes = sanitizeReminderMinutes(reminderMinutes);
                replaceReminderRows(task.getId(), targetMinutes);
            } else {
                targetMinutes = sanitizeReminderMinutes(oldMinutes);
            }

            taskDao.update(task);
            ReminderScheduler.replaceTaskReminders(getApplication(), task, oldMinutes, targetMinutes);

            Log.d(TAG, "updateTask() taskId=" + task.getId() + " -> refresh widgets");
            TodayTasksWidgetProvider.refreshAllWidgets(getApplication());
        });
    }

    public void deleteTask(Task task) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            List<Integer> oldMinutes = taskReminderDao.getReminderMinutesByTaskId(task.getId());
            ReminderScheduler.cancelTaskReminders(getApplication(), task.getId(), oldMinutes);

            taskReminderDao.deleteByTaskId(task.getId());
            taskDao.delete(task);
            Log.d(TAG, "deleteTask() taskId=" + task.getId() + " -> refresh widgets");
            TodayTasksWidgetProvider.refreshAllWidgets(getApplication());
        });
    }

    public interface ReminderLoadCallback {
        void onLoaded(List<Integer> reminderMinutes);
    }

    public void loadReminderMinutes(int taskId, ReminderLoadCallback callback) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            List<Integer> minutes = taskReminderDao.getReminderMinutesByTaskId(taskId);
            callback.onLoaded(sanitizeReminderMinutes(minutes));
        });
    }

    private void replaceReminderRows(int taskId, List<Integer> reminderMinutes) {
        taskReminderDao.deleteByTaskId(taskId);
        if (reminderMinutes.isEmpty()) {
            return;
        }

        List<TaskReminder> rows = new ArrayList<>();
        for (int minutes : reminderMinutes) {
            rows.add(new TaskReminder(taskId, minutes));
        }
        taskReminderDao.insertAll(rows);
    }

    private List<Integer> sanitizeReminderMinutes(List<Integer> values) {
        List<Integer> safe = new ArrayList<>();
        if (values == null) {
            return safe;
        }

        for (Integer value : values) {
            if (value != null && value > 0 && !safe.contains(value)) {
                safe.add(value);
            }
        }
        safe.sort(Integer::compareTo);
        return safe;
    }
}
