package hcmute.edu.vn.ticktick.database;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "task_reminders",
        foreignKeys = @ForeignKey(
                entity = Task.class,
                parentColumns = "id",
                childColumns = "taskId",
                onDelete = ForeignKey.CASCADE
        ),
        indices = {
                @Index("taskId"),
                @Index(value = {"taskId", "minutesBefore"}, unique = true)
        }
)
public class TaskReminder {

    @PrimaryKey(autoGenerate = true)
    private int id;

    private int taskId;
    private int minutesBefore;

    public TaskReminder(int taskId, int minutesBefore) {
        this.taskId = taskId;
        this.minutesBefore = minutesBefore;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getTaskId() {
        return taskId;
    }

    public void setTaskId(int taskId) {
        this.taskId = taskId;
    }

    public int getMinutesBefore() {
        return minutesBefore;
    }

    public void setMinutesBefore(int minutesBefore) {
        this.minutesBefore = minutesBefore;
    }
}

