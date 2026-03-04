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

    @Query("SELECT * FROM tasks ORDER BY dueDate ASC, createdAt ASC")
    LiveData<List<Task>> getAllTasks();

    // Hôm nay: tasks with dueDate between start and end of today
    @Query("SELECT * FROM tasks WHERE dueDate >= :startOfDay AND dueDate < :endOfDay AND completed = 0 ORDER BY dueDate ASC")
    LiveData<List<Task>> getTasksForToday(long startOfDay, long endOfDay);

    // Ngày mai
    @Query("SELECT * FROM tasks WHERE dueDate >= :startOfTomorrow AND dueDate < :endOfTomorrow AND completed = 0 ORDER BY dueDate ASC")
    LiveData<List<Task>> getTasksForTomorrow(long startOfTomorrow, long endOfTomorrow);

    // Sắp tới (next 7 days, excluding today and tomorrow)
    @Query("SELECT * FROM tasks WHERE dueDate >= :startOfDayAfterTomorrow AND dueDate < :endOfWeek AND completed = 0 ORDER BY dueDate ASC")
    LiveData<List<Task>> getTasksUpcoming(long startOfDayAfterTomorrow, long endOfWeek);

    // 7 ngày tới (all tasks in next 7 days)
    @Query("SELECT * FROM tasks WHERE dueDate >= :startOfToday AND dueDate < :endOfWeek AND completed = 0 ORDER BY dueDate ASC")
    LiveData<List<Task>> getTasksNext7Days(long startOfToday, long endOfWeek);

    // Hộp thư đến (no category)
    @Query("SELECT * FROM tasks WHERE categoryId IS NULL AND completed = 0 ORDER BY dueDate ASC")
    LiveData<List<Task>> getInboxTasks();

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

    // Count by category
    @Query("SELECT COUNT(*) FROM tasks WHERE categoryId = :categoryId AND completed = 0")
    LiveData<Integer> getTaskCountByCategory(int categoryId);

    // Get task by ID
    @Query("SELECT * FROM tasks WHERE id = :id")
    Task getTaskById(int id);
}
