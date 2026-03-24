package hcmute.edu.vn.ticktick.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface TaskDao {

    @Insert
    long insert(Task task);

    @Update
    void update(Task task);

    @Delete
    void delete(Task task);

    @Query("SELECT * FROM tasks WHERE completed = 0 ORDER BY CASE WHEN dueDate = 0 THEN 1 ELSE 0 END, dueDate ASC, createdAt ASC")
    LiveData<List<Task>> getAllActiveTasks();

    // Hôm nay: tasks with dueDate between start and end of today
    @Query("SELECT * FROM tasks WHERE dueDate >= :startOfDay AND dueDate < :endOfDay AND completed = 0 ORDER BY dueDate ASC")
    LiveData<List<Task>> getTasksForToday(long startOfDay, long endOfDay);

    // Ngày mai
    @Query("SELECT * FROM tasks WHERE dueDate >= :startOfTomorrow AND dueDate < :endOfTomorrow AND completed = 0 ORDER BY dueDate ASC")
    LiveData<List<Task>> getTasksForTomorrow(long startOfTomorrow, long endOfTomorrow);

    // Sắp tới (next 7 days, excluding today and tomorrow)
    @Query("SELECT * FROM tasks WHERE dueDate >= :startOfDayAfterTomorrow AND dueDate < :endOfWeek AND completed = 0 ORDER BY dueDate ASC")
    LiveData<List<Task>> getTasksUpcoming(long startOfDayAfterTomorrow, long endOfWeek);

    // By category
    @Query("SELECT * FROM tasks WHERE categoryId = :categoryId AND completed = 0 ORDER BY dueDate ASC")
    LiveData<List<Task>> getTasksByCategory(int categoryId);

    // Chưa lên lịch (no due date)
    @Query("SELECT * FROM tasks WHERE dueDate = 0 AND completed = 0 ORDER BY createdAt ASC")
    LiveData<List<Task>> getUnscheduledTasks();

    // Đã hoàn thành
    @Query("SELECT * FROM tasks WHERE completed = 1 ORDER BY dueDate DESC")
    LiveData<List<Task>> getCompletedTasks();

    // Tuần này
    @Query("SELECT * FROM tasks WHERE dueDate >= :startOfWeek AND dueDate < :endOfWeek AND completed = 0 ORDER BY dueDate ASC")
    LiveData<List<Task>> getTasksThisWeek(long startOfWeek, long endOfWeek);

    // Tasks for a specific date range (Calendar view)
    @Query("SELECT * FROM tasks WHERE dueDate >= :startOfDay AND dueDate < :endOfDay ORDER BY dueDate ASC, createdAt ASC")
    LiveData<List<Task>> getTasksForDate(long startOfDay, long endOfDay);

    @Query("SELECT * FROM tasks WHERE dueDate >= :startDate AND dueDate < :endDateExclusive AND completed = 0 ORDER BY dueDate ASC, createdAt ASC")
    LiveData<List<Task>> getTasksForDateRange(long startDate, long endDateExclusive);

    @Query("SELECT DISTINCT dueDate FROM tasks WHERE dueDate >= :startDate AND dueDate < :endDateExclusive")
    LiveData<List<Long>> getTaskDueDatesInRange(long startDate, long endDateExclusive);

    @Query("SELECT * FROM tasks WHERE id = :taskId LIMIT 1")
    Task getTaskByIdSync(int taskId);

    @Query("SELECT * FROM tasks WHERE linkedSchoolEventUid = :uid LIMIT 1")
    Task getTaskBySchoolEventUidSync(String uid);

    @Query("SELECT * FROM tasks WHERE completed = 0 AND dueDate >= :startOfToday AND dueDate < :endOfToday ORDER BY CASE WHEN dueTime IS NULL OR dueTime = '' THEN 1 ELSE 0 END, dueTime ASC, createdAt ASC LIMIT :limit")
    List<Task> getTodayWidgetTasks(long startOfToday, long endOfToday, int limit);

    @Query("SELECT COUNT(*) FROM tasks WHERE completed = 0 AND dueDate >= :startOfToday AND dueDate < :endOfToday")
    int countTodayWidgetTasks(long startOfToday, long endOfToday);

    @Query("SELECT * FROM tasks WHERE completed = 0 AND dueDate > 0 ORDER BY dueDate ASC, CASE WHEN dueTime IS NULL OR dueTime = '' THEN 1 ELSE 0 END, dueTime ASC, createdAt ASC")
    List<Task> getWidgetActiveScheduledTasks();

    @Query("UPDATE tasks SET completed = 1 WHERE id = :taskId")
    void markTaskCompleted(int taskId);
}
