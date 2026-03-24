package hcmute.edu.vn.ticktick.schoolcalendar;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import java.util.List;

import hcmute.edu.vn.ticktick.database.AppDatabase;
import hcmute.edu.vn.ticktick.database.Category;
import hcmute.edu.vn.ticktick.database.CategoryDao;
import hcmute.edu.vn.ticktick.database.SchoolCalendarEventEntity;
import hcmute.edu.vn.ticktick.database.Task;

public class SchoolCalendarViewModel extends AndroidViewModel {

    private final SchoolCalendarRepository repository;
    private final SchoolTaskLinkRepository linkRepository;
    private final SchoolCalendarExportUseCase exportUseCase;
    private final CategoryDao categoryDao;

    private final MutableLiveData<SyncMeta> syncMeta = new MutableLiveData<>();
    private final LiveData<List<SchoolCalendarEventEntity>> events;
    private final LiveData<List<WorkInfo>> oneTimeWork;

    public SchoolCalendarViewModel(@NonNull Application application) {
        super(application);
        repository = new SchoolCalendarRepository(application);
        linkRepository = new SchoolTaskLinkRepository(application);
        exportUseCase = new SchoolCalendarExportUseCase(linkRepository);
        categoryDao = AppDatabase.getDatabase(application).categoryDao();

        events = repository.observeEvents();
        oneTimeWork = WorkManager.getInstance(application)
                .getWorkInfosForUniqueWorkLiveData(SchoolCalendarSyncScheduler.UNIQUE_ONE_TIME_WORK);
        refreshSyncMeta();
    }

    public LiveData<List<SchoolCalendarEventEntity>> getEvents() {
        return events;
    }

    public LiveData<List<WorkInfo>> getOneTimeWorkInfo() {
        return oneTimeWork;
    }

    public LiveData<SyncMeta> getSyncMeta() {
        return syncMeta;
    }

    public void ensurePeriodicSync() {
        repository.ensurePeriodicSync(getApplication());
    }

    public void requestManualSync() {
        repository.requestOneTimeSync(getApplication());
    }

    public void syncIfStale() {
        repository.requestOneTimeSyncIfStale(getApplication());
    }

    public String getCurrentSourceUrl() {
        return repository.getSourceUrl();
    }

    public boolean saveSourceUrl(String url) {
        boolean valid = repository.saveSourceUrlAndValidate(url);
        if (valid) {
            repository.requestOneTimeSync(getApplication());
        }
        return valid;
    }

    public void restoreDefaultUrl() {
        repository.restoreDefaultUrl();
        repository.requestOneTimeSync(getApplication());
    }

    public void refreshSyncMeta() {
        syncMeta.setValue(new SyncMeta(
                repository.getLastSyncTime(),
                repository.getLastSyncError()
        ));
    }

    public void getLinkedTask(String schoolEventUid, TaskCallback callback) {
        AppDatabase.databaseWriteExecutor.execute(() -> callback.onResult(linkRepository.getLinkedTaskSync(schoolEventUid)));
    }

    public void exportEventToTask(SchoolCalendarEventEntity event, ExportCallback callback) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            List<Category> categories = categoryDao.getAllCategoriesSync();
            SchoolTaskLinkRepository.ExportResult result = exportUseCase.exportEvent(event, categories);
            callback.onResult(result);
        });
    }

    public void updateSchoolEvent(SchoolCalendarEventEntity event, Runnable callback) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            linkRepository.updateSchoolEvent(event);
            if (callback != null) {
                callback.run();
            }
        });
    }

    public void deleteSchoolEvent(SchoolCalendarEventEntity event, Runnable callback) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            linkRepository.deleteSchoolEvent(event);
            if (callback != null) {
                callback.run();
            }
        });
    }

    public interface TaskCallback {
        void onResult(Task task);
    }

    public interface ExportCallback {
        void onResult(SchoolTaskLinkRepository.ExportResult result);
    }

    public static class SyncMeta {
        public final long lastSyncTime;
        public final String lastError;

        public SyncMeta(long lastSyncTime, String lastError) {
            this.lastSyncTime = lastSyncTime;
            this.lastError = lastError == null ? "" : lastError;
        }
    }
}
