package hcmute.edu.vn.ticktick.database;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "school_calendar_events", indices = {@Index("referenceTimeMillis")})
public class SchoolCalendarEventEntity {

    @PrimaryKey
    @NonNull
    private String uid;

    private String title;
    private String description;
    private String category;
    private long startTimeMillis;
    private long endTimeMillis;
    private long referenceTimeMillis;
    private long lastModifiedMillis;
    private long syncedAtMillis;
    private String sourceUrl;

    @NonNull
    public String getUid() {
        return uid;
    }

    public void setUid(@NonNull String uid) {
        this.uid = uid;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public long getStartTimeMillis() {
        return startTimeMillis;
    }

    public void setStartTimeMillis(long startTimeMillis) {
        this.startTimeMillis = startTimeMillis;
    }

    public long getEndTimeMillis() {
        return endTimeMillis;
    }

    public void setEndTimeMillis(long endTimeMillis) {
        this.endTimeMillis = endTimeMillis;
    }

    public long getReferenceTimeMillis() {
        return referenceTimeMillis;
    }

    public void setReferenceTimeMillis(long referenceTimeMillis) {
        this.referenceTimeMillis = referenceTimeMillis;
    }

    public long getLastModifiedMillis() {
        return lastModifiedMillis;
    }

    public void setLastModifiedMillis(long lastModifiedMillis) {
        this.lastModifiedMillis = lastModifiedMillis;
    }

    public long getSyncedAtMillis() {
        return syncedAtMillis;
    }

    public void setSyncedAtMillis(long syncedAtMillis) {
        this.syncedAtMillis = syncedAtMillis;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }
}

