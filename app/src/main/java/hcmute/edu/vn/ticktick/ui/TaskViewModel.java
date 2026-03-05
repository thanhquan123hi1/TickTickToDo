package hcmute.edu.vn.ticktick.ui;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import java.util.List;

import hcmute.edu.vn.ticktick.database.AppDatabase;
import hcmute.edu.vn.ticktick.database.Category;
import hcmute.edu.vn.ticktick.database.Task;
import hcmute.edu.vn.ticktick.database.TaskDao;
import hcmute.edu.vn.ticktick.database.CategoryDao;

public class TaskViewModel extends AndroidViewModel {

    private final TaskDao taskDao;
    private final CategoryDao categoryDao;

    public TaskViewModel(@NonNull Application application) {
        super(application);
        AppDatabase db = AppDatabase.getDatabase(application);
        taskDao = db.taskDao();
        categoryDao = db.categoryDao();
    }

    // === Task queries ===

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

    public LiveData<List<Task>> getTasksNext7Days() {
        return taskDao.getTasksNext7Days(
                DateUtils.getStartOfToday(),
                DateUtils.getEndOf7Days()
        );
    }

    public LiveData<List<Task>> getInboxTasks() {
        return taskDao.getInboxTasks();
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

    // === Category queries ===

    public LiveData<List<Category>> getAllCategories() {
        return categoryDao.getAllCategories();
    }

    // === Task operations ===

    public void updateTask(Task task) {
        AppDatabase.databaseWriteExecutor.execute(() -> taskDao.update(task));
    }

    public void insertTask(Task task) {
        AppDatabase.databaseWriteExecutor.execute(() -> taskDao.insert(task));
    }

    public void deleteTask(Task task) {
        AppDatabase.databaseWriteExecutor.execute(() -> taskDao.delete(task));
    }
}
