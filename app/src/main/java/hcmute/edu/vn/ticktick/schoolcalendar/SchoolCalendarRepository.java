package hcmute.edu.vn.ticktick.schoolcalendar;

import android.content.Context;

import androidx.lifecycle.LiveData;

import java.util.List;

import hcmute.edu.vn.ticktick.database.AppDatabase;
import hcmute.edu.vn.ticktick.database.SchoolCalendarDao;
import hcmute.edu.vn.ticktick.database.SchoolCalendarEventEntity;

public class SchoolCalendarRepository {

    private static final long STALE_WINDOW_MILLIS = 30L * 60L * 1000L;

    private final SchoolCalendarDao dao;
    private final SchoolCalendarPreferences preferences;

    public SchoolCalendarRepository(Context context) {
        Context appContext = context.getApplicationContext();
        AppDatabase db = AppDatabase.getDatabase(appContext);
        dao = db.schoolCalendarDao();
        preferences = new SchoolCalendarPreferences(appContext);
    }

    public LiveData<List<SchoolCalendarEventEntity>> observeEvents() {
        return dao.observeAllEvents();
    }

    public void ensurePeriodicSync(Context context) {
        SchoolCalendarSyncScheduler.ensurePeriodicSync(context.getApplicationContext());
    }

    public void requestOneTimeSync(Context context) {
        SchoolCalendarSyncScheduler.enqueueOneTimeSync(context.getApplicationContext());
    }

    public void requestOneTimeSyncIfStale(Context context) {
        long now = System.currentTimeMillis();
        long lastSync = preferences.getLastSyncTime();
        if (lastSync <= 0 || now - lastSync > STALE_WINDOW_MILLIS) {
            requestOneTimeSync(context);
        }
    }

    public long getLastSyncTime() {
        return preferences.getLastSyncTime();
    }

    public String getLastSyncError() {
        return preferences.getLastSyncError();
    }

    public String getSourceUrl() {
        return preferences.getSourceUrl();
    }

    public boolean saveSourceUrlAndValidate(String url) {
        if (!SchoolCalendarPreferences.isValidHttpUrl(url)) {
            return false;
        }
        preferences.saveSourceUrl(url);
        return true;
    }

    public void restoreDefaultUrl() {
        preferences.restoreDefaultSourceUrl();
    }
}

