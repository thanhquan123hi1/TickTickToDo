package hcmute.edu.vn.ticktick.database;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "school_calendar_task_links",
        foreignKeys = @ForeignKey(
                entity = Task.class,
                parentColumns = "id",
                childColumns = "taskId",
                onDelete = ForeignKey.CASCADE
        ),
        indices = {
                @Index(value = {"schoolEventUid"}, unique = true),
                @Index(value = {"taskId"}, unique = true)
        }
)
public class SchoolCalendarTaskLink {

    @PrimaryKey(autoGenerate = true)
    private long id;

    @NonNull
    private String schoolEventUid;

    private int taskId;
    private String syncMode;
    private long createdAt;
    private long updatedAt;

    public SchoolCalendarTaskLink(@NonNull String schoolEventUid, int taskId, String syncMode, long createdAt, long updatedAt) {
        this.schoolEventUid = schoolEventUid;
        this.taskId = taskId;
        this.syncMode = syncMode;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @NonNull
    public String getSchoolEventUid() {
        return schoolEventUid;
    }

    public void setSchoolEventUid(@NonNull String schoolEventUid) {
        this.schoolEventUid = schoolEventUid;
    }

    public int getTaskId() {
        return taskId;
    }

    public void setTaskId(int taskId) {
        this.taskId = taskId;
    }

    public String getSyncMode() {
        return syncMode;
    }

    public void setSyncMode(String syncMode) {
        this.syncMode = syncMode;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }
}

