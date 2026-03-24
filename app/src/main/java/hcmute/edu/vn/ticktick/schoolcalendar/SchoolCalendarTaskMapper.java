package hcmute.edu.vn.ticktick.schoolcalendar;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Locale;

import hcmute.edu.vn.ticktick.database.SchoolCalendarEventEntity;
import hcmute.edu.vn.ticktick.database.Task;
import hcmute.edu.vn.ticktick.ui.DateUtils;

public class SchoolCalendarTaskMapper {

    public Task toExportedTask(SchoolCalendarEventEntity event, Integer categoryId) {
        Task task = new Task();
        task.setTitle(event.getTitle());
        task.setDescription(event.getDescription());

        long referenceMillis = event.getReferenceTimeMillis() > 0
                ? event.getReferenceTimeMillis()
                : event.getStartTimeMillis();
        if (referenceMillis > 0) {
            task.setDueDate(DateUtils.getStartOfDay(referenceMillis));
            LocalDateTime dt = LocalDateTime.ofInstant(Instant.ofEpochMilli(referenceMillis), ZoneId.systemDefault());
            task.setDueTime(String.format(Locale.getDefault(), "%02d:%02d", dt.getHour(), dt.getMinute()));
        }

        task.setCategoryId(categoryId);
        task.setLinkedSchoolEventUid(event.getUid());
        return task;
    }

    public void applyEventToLinkedTask(SchoolCalendarEventEntity event, Task task) {
        task.setTitle(event.getTitle());
        task.setDescription(event.getDescription());

        long referenceMillis = event.getReferenceTimeMillis() > 0
                ? event.getReferenceTimeMillis()
                : event.getStartTimeMillis();
        if (referenceMillis > 0) {
            task.setDueDate(DateUtils.getStartOfDay(referenceMillis));
            LocalDateTime dt = LocalDateTime.ofInstant(Instant.ofEpochMilli(referenceMillis), ZoneId.systemDefault());
            task.setDueTime(String.format(Locale.getDefault(), "%02d:%02d", dt.getHour(), dt.getMinute()));
        } else {
            task.setDueDate(0L);
            task.setDueTime(null);
        }
    }
}

