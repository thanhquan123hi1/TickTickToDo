package hcmute.edu.vn.ticktick.schoolcalendar;

import android.content.Context;

import androidx.annotation.Nullable;

import hcmute.edu.vn.ticktick.database.AppDatabase;
import hcmute.edu.vn.ticktick.database.SchoolCalendarDao;
import hcmute.edu.vn.ticktick.database.SchoolCalendarEventEntity;
import hcmute.edu.vn.ticktick.database.SchoolCalendarTaskLink;
import hcmute.edu.vn.ticktick.database.SchoolCalendarTaskLinkDao;
import hcmute.edu.vn.ticktick.database.Task;
import hcmute.edu.vn.ticktick.database.TaskDao;
import hcmute.edu.vn.ticktick.widget.TodayTasksWidgetProvider;

public class SchoolTaskLinkRepository {

    public static final String SYNC_MODE_MANUAL_EXPORT = "MANUAL_EXPORT";
    public static final String SYNC_MODE_MIRROR_ON_EDIT = "MIRROR_ON_EDIT";

    private final Context appContext;
    private final AppDatabase db;
    private final TaskDao taskDao;
    private final SchoolCalendarDao schoolDao;
    private final SchoolCalendarTaskLinkDao linkDao;
    private final SchoolCalendarTaskMapper mapper;

    public SchoolTaskLinkRepository(Context context) {
        appContext = context.getApplicationContext();
        db = AppDatabase.getDatabase(appContext);
        taskDao = db.taskDao();
        schoolDao = db.schoolCalendarDao();
        linkDao = db.schoolCalendarTaskLinkDao();
        mapper = new SchoolCalendarTaskMapper();
    }

    @Nullable
    public Task getLinkedTaskSync(String schoolEventUid) {
        SchoolCalendarTaskLink link = linkDao.getBySchoolEventUidSync(schoolEventUid);
        if (link != null) {
            return taskDao.getTaskByIdSync(link.getTaskId());
        }
        return taskDao.getTaskBySchoolEventUidSync(schoolEventUid);
    }

    public ExportResult exportToTask(SchoolCalendarEventEntity event, @Nullable Integer defaultCategoryId) {
        final ExportResult[] holder = new ExportResult[1];

        db.runInTransaction(() -> {
            Task existing = getLinkedTaskSync(event.getUid());
            if (existing != null) {
                holder[0] = new ExportResult(existing, false);
                return;
            }

            Task task = mapper.toExportedTask(event, defaultCategoryId);
            int taskId = (int) taskDao.insert(task);
            task.setId(taskId);

            long now = System.currentTimeMillis();
            linkDao.insert(new SchoolCalendarTaskLink(
                    event.getUid(),
                    taskId,
                    SYNC_MODE_MANUAL_EXPORT,
                    now,
                    now
            ));

            holder[0] = new ExportResult(task, true);
        });

        TodayTasksWidgetProvider.refreshAllWidgets(appContext);
        return holder[0];
    }

    public void updateSchoolEvent(SchoolCalendarEventEntity updatedEvent) {
        db.runInTransaction(() -> {
            schoolDao.update(updatedEvent);

            Task linkedTask = getLinkedTaskSync(updatedEvent.getUid());
            if (linkedTask != null) {
                mapper.applyEventToLinkedTask(updatedEvent, linkedTask);
                taskDao.update(linkedTask);

                SchoolCalendarTaskLink link = linkDao.getBySchoolEventUidSync(updatedEvent.getUid());
                if (link != null) {
                    link.setSyncMode(SYNC_MODE_MIRROR_ON_EDIT);
                    link.setUpdatedAt(System.currentTimeMillis());
                    linkDao.update(link);
                }
            }
        });

        TodayTasksWidgetProvider.refreshAllWidgets(appContext);
    }

    public void deleteSchoolEvent(SchoolCalendarEventEntity event) {
        db.runInTransaction(() -> {
            schoolDao.delete(event);

            Task linkedTask = getLinkedTaskSync(event.getUid());
            if (linkedTask != null) {
                linkedTask.setLinkedSchoolEventUid(null);
                taskDao.update(linkedTask);
            }

            linkDao.deleteBySchoolEventUid(event.getUid());
        });

        TodayTasksWidgetProvider.refreshAllWidgets(appContext);
    }

    public static class ExportResult {
        private final Task task;
        private final boolean created;

        public ExportResult(Task task, boolean created) {
            this.task = task;
            this.created = created;
        }

        public Task getTask() {
            return task;
        }

        public boolean isCreated() {
            return created;
        }
    }
}
