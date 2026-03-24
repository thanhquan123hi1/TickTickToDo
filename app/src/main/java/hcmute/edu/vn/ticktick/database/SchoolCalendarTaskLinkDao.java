package hcmute.edu.vn.ticktick.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

@Dao
public interface SchoolCalendarTaskLinkDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(SchoolCalendarTaskLink link);

    @Update
    void update(SchoolCalendarTaskLink link);

    @Query("SELECT * FROM school_calendar_task_links WHERE schoolEventUid = :schoolEventUid LIMIT 1")
    SchoolCalendarTaskLink getBySchoolEventUidSync(String schoolEventUid);

    @Query("SELECT * FROM school_calendar_task_links WHERE taskId = :taskId LIMIT 1")
    SchoolCalendarTaskLink getByTaskIdSync(int taskId);

    @Query("DELETE FROM school_calendar_task_links WHERE schoolEventUid = :schoolEventUid")
    void deleteBySchoolEventUid(String schoolEventUid);
}

