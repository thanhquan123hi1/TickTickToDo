package hcmute.edu.vn.ticktick.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface SchoolCalendarDao {

    @Query("SELECT * FROM school_calendar_events ORDER BY referenceTimeMillis ASC")
    LiveData<List<SchoolCalendarEventEntity>> observeAllEvents();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<SchoolCalendarEventEntity> events);

    @Update
    void update(SchoolCalendarEventEntity event);

    @Delete
    void delete(SchoolCalendarEventEntity event);

    @Query("DELETE FROM school_calendar_events")
    void deleteAll();

    @Query("SELECT * FROM school_calendar_events WHERE uid = :uid LIMIT 1")
    SchoolCalendarEventEntity getEventByUidSync(String uid);
}
