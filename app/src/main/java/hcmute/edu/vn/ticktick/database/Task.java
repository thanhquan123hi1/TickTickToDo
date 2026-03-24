package hcmute.edu.vn.ticktick.database;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "tasks",
        foreignKeys = @ForeignKey(
                entity = Category.class,
                parentColumns = "id",
                childColumns = "categoryId",
                onDelete = ForeignKey.SET_NULL
        ),
        indices = {
                @Index("categoryId"),
                @Index(value = "linkedSchoolEventUid", unique = true)
        })
public class Task {

    @PrimaryKey(autoGenerate = true)
    private int id;

    private String title;       // tieu_de
    private String description; // mo_ta
    private long dueDate;       // ngay_den_han (timestamp)
    private String dueTime;     // gio (HH:mm)
    private int priority;       // do_uu_tien (0=none, 1=low, 2=medium, 3=high)
    private Integer categoryId; // id_danh_muc
    private boolean completed;  // da_xong
    private long createdAt;
    private String linkedSchoolEventUid;

    public Task() {
        this.createdAt = System.currentTimeMillis();
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public long getDueDate() { return dueDate; }
    public void setDueDate(long dueDate) { this.dueDate = dueDate; }

    public String getDueTime() { return dueTime; }
    public void setDueTime(String dueTime) { this.dueTime = dueTime; }

    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }

    public Integer getCategoryId() { return categoryId; }
    public void setCategoryId(Integer categoryId) { this.categoryId = categoryId; }

    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public String getLinkedSchoolEventUid() { return linkedSchoolEventUid; }
    public void setLinkedSchoolEventUid(String linkedSchoolEventUid) { this.linkedSchoolEventUid = linkedSchoolEventUid; }
}
