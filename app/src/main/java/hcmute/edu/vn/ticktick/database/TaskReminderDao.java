package hcmute.edu.vn.ticktick.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface TaskReminderDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertAll(List<TaskReminder> reminders);

    @Query("DELETE FROM task_reminders WHERE taskId = :taskId")
    void deleteByTaskId(int taskId);

    @Query("SELECT minutesBefore FROM task_reminders WHERE taskId = :taskId ORDER BY minutesBefore ASC")
    List<Integer> getReminderMinutesByTaskId(int taskId);

    @Query("SELECT * FROM task_reminders")
    List<TaskReminder> getAllSync();
}

