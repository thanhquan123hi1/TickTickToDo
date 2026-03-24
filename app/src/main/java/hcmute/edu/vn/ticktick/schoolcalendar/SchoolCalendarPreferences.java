package hcmute.edu.vn.ticktick.schoolcalendar;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Patterns;

import hcmute.edu.vn.ticktick.R;

public class SchoolCalendarPreferences {

    private static final String PREFS_NAME = "school_calendar_prefs";
    private static final String KEY_ICS_URL = "ics_url";
    private static final String KEY_LAST_SYNC_TIME = "last_sync_time";
    private static final String KEY_LAST_SYNC_ERROR = "last_sync_error";

    private final SharedPreferences preferences;
    private final Context appContext;

    public SchoolCalendarPreferences(Context context) {
        appContext = context.getApplicationContext();
        preferences = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public String getSourceUrl() {
        String saved = preferences.getString(KEY_ICS_URL, "");
        if (saved != null && !saved.trim().isEmpty()) {
            return saved.trim();
        }
        return appContext.getString(R.string.school_calendar_default_ics_url).trim();
    }

    public void saveSourceUrl(String url) {
        preferences.edit().putString(KEY_ICS_URL, url == null ? "" : url.trim()).apply();
    }

    public void restoreDefaultSourceUrl() {
        preferences.edit().remove(KEY_ICS_URL).apply();
    }

    public long getLastSyncTime() {
        return preferences.getLong(KEY_LAST_SYNC_TIME, 0L);
    }

    public void setLastSyncTime(long value) {
        preferences.edit().putLong(KEY_LAST_SYNC_TIME, value).apply();
    }

    public String getLastSyncError() {
        String value = preferences.getString(KEY_LAST_SYNC_ERROR, "");
        return value == null ? "" : value;
    }

    public void setLastSyncError(String value) {
        preferences.edit().putString(KEY_LAST_SYNC_ERROR, value == null ? "" : value).apply();
    }

    public static boolean isValidHttpUrl(String value) {
        if (value == null) {
            return false;
        }
        String trimmed = value.trim();
        return (trimmed.startsWith("http://") || trimmed.startsWith("https://"))
                && Patterns.WEB_URL.matcher(trimmed).matches();
    }
}

