package hcmute.edu.vn.ticktick.widget;

import android.content.Context;
import android.content.SharedPreferences;

public final class WidgetPrefs {

    private static final String PREFS_NAME = "ticktick_widget_prefs";

    private WidgetPrefs() {
    }

    public static WidgetConfig load(Context context, int appWidgetId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        WidgetConfig config = new WidgetConfig();
        config.appWidgetId = appWidgetId;
        config.mode = prefs.getString(key(appWidgetId, "mode"), WidgetConfig.MODE_TASKS);
        config.taskLimit = prefs.getInt(key(appWidgetId, "task_limit"), WidgetConfig.TASK_LIMIT_DEFAULT);
        config.includeCompleted = prefs.getBoolean(key(appWidgetId, "include_completed"), false);
        config.showDeadline = prefs.getBoolean(key(appWidgetId, "show_deadline"), true);
        config.theme = prefs.getInt(key(appWidgetId, "theme"), WidgetConfig.THEME_SYSTEM);

        config.timerDurationMinutes = prefs.getInt(
                key(appWidgetId, "timer_duration_minutes"),
                WidgetConfig.TIMER_DURATION_MINUTES_DEFAULT
        );
        config.timerRunning = prefs.getBoolean(key(appWidgetId, "timer_running"), false);
        config.timerEndElapsedRealtime = prefs.getLong(key(appWidgetId, "timer_end_elapsed"), 0L);
        config.timerRemainingMs = prefs.getLong(
                key(appWidgetId, "timer_remaining_ms"),
                config.getTimerDurationMs()
        );
        return config;
    }

    public static void save(Context context, WidgetConfig config) {
        SharedPreferences.Editor editor = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
        int id = config.appWidgetId;
        editor.putString(key(id, "mode"), config.mode);
        editor.putInt(key(id, "task_limit"), config.taskLimit);
        editor.putBoolean(key(id, "include_completed"), config.includeCompleted);
        editor.putBoolean(key(id, "show_deadline"), config.showDeadline);
        editor.putInt(key(id, "theme"), config.theme);

        editor.putInt(key(id, "timer_duration_minutes"), config.timerDurationMinutes);
        editor.putBoolean(key(id, "timer_running"), config.timerRunning);
        editor.putLong(key(id, "timer_end_elapsed"), config.timerEndElapsedRealtime);
        editor.putLong(key(id, "timer_remaining_ms"), config.timerRemainingMs);
        editor.apply();
    }

    public static void remove(Context context, int appWidgetId) {
        SharedPreferences.Editor editor = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
        editor.remove(key(appWidgetId, "mode"));
        editor.remove(key(appWidgetId, "task_limit"));
        editor.remove(key(appWidgetId, "include_completed"));
        editor.remove(key(appWidgetId, "show_deadline"));
        editor.remove(key(appWidgetId, "theme"));
        editor.remove(key(appWidgetId, "timer_duration_minutes"));
        editor.remove(key(appWidgetId, "timer_running"));
        editor.remove(key(appWidgetId, "timer_end_elapsed"));
        editor.remove(key(appWidgetId, "timer_remaining_ms"));
        editor.apply();
    }

    private static String key(int appWidgetId, String suffix) {
        return "widget_" + appWidgetId + "_" + suffix;
    }
}

