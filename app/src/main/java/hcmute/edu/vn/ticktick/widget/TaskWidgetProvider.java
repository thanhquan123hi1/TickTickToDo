package hcmute.edu.vn.ticktick.widget;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.SystemClock;
import android.util.Log;
import android.widget.RemoteViews;

import hcmute.edu.vn.ticktick.MainActivity;
import hcmute.edu.vn.ticktick.R;

public class TaskWidgetProvider extends AppWidgetProvider {

    private static final String TAG = "TaskWidgetProvider";

    public static final String ACTION_OPEN_TASK_DETAIL = "hcmute.edu.vn.ticktick.action.OPEN_TASK_DETAIL";
    public static final String ACTION_ADD_TASK = "hcmute.edu.vn.ticktick.action.ADD_TASK";
    public static final String ACTION_OPEN_APP = "hcmute.edu.vn.ticktick.action.OPEN_APP";
    public static final String ACTION_TIMER_START = "hcmute.edu.vn.ticktick.action.TIMER_START";
    public static final String ACTION_TIMER_PAUSE = "hcmute.edu.vn.ticktick.action.TIMER_PAUSE";
    public static final String ACTION_TIMER_RESET = "hcmute.edu.vn.ticktick.action.TIMER_RESET";
    public static final String ACTION_TIMER_FINISH = "hcmute.edu.vn.ticktick.action.TIMER_FINISH";
    public static final String EXTRA_TASK_ID = "extra_task_id";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Log.d(TAG, "onUpdate() ids=" + appWidgetIds.length);
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        super.onDeleted(context, appWidgetIds);
        for (int appWidgetId : appWidgetIds) {
            Log.d(TAG, "onDeleted() cleanup widgetId=" + appWidgetId);
            cancelTimerFinishAlarm(context, appWidgetId);
            WidgetPrefs.remove(context, appWidgetId);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (intent == null || intent.getAction() == null) {
            return;
        }

        String action = intent.getAction();
        int appWidgetId = resolveWidgetId(intent);
        Log.d(TAG, "onReceive() action=" + action + " widgetId=" + appWidgetId);

        if (AppWidgetManager.ACTION_APPWIDGET_UPDATE.equals(action)) {
            if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                refreshWidget(context, appWidgetId);
            } else {
                refreshAllWidgets(context);
            }
            return;
        }

        if (ACTION_TIMER_START.equals(action)) {
            startTimer(context, appWidgetId);
            return;
        }
        if (ACTION_TIMER_PAUSE.equals(action)) {
            pauseTimer(context, appWidgetId);
            return;
        }
        if (ACTION_TIMER_RESET.equals(action)) {
            resetTimer(context, appWidgetId);
            return;
        }
        if (ACTION_TIMER_FINISH.equals(action)) {
            finishTimer(context, appWidgetId);
        }
    }

    public static void refreshAllWidgets(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        ComponentName componentName = new ComponentName(context, TaskWidgetProvider.class);
        int[] appWidgetIds = manager.getAppWidgetIds(componentName);
        if (appWidgetIds == null || appWidgetIds.length == 0) {
            Log.d(TAG, "refreshAllWidgets() no widgets");
            return;
        }
        Log.d(TAG, "refreshAllWidgets() ids=" + appWidgetIds.length);
        manager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widget_task_list);
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, manager, appWidgetId);
        }
    }

    public static void refreshWidget(Context context, int appWidgetId) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        manager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_task_list);
        updateAppWidget(context, manager, appWidgetId);
    }

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        WidgetConfig config = WidgetPrefs.load(context, appWidgetId);
        if (WidgetConfig.MODE_TIMER.equals(config.mode)) {
            updateTimerWidget(context, appWidgetManager, appWidgetId, config);
            return;
        }
        updateTaskWidget(context, appWidgetManager, appWidgetId, config);
    }

    private static void updateTaskWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId, WidgetConfig config) {
        Log.d(TAG, "updateTaskWidget() id=" + appWidgetId + " limit=" + config.taskLimit + " includeCompleted=" + config.includeCompleted + " showDeadline=" + config.showDeadline);

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_tasks);

        views.setOnClickPendingIntent(R.id.widget_btn_open, buildOpenAppPendingIntent(context, appWidgetId));
        views.setOnClickPendingIntent(R.id.widget_btn_add, buildAddTaskPendingIntent(context, appWidgetId));

        Intent serviceIntent = new Intent(context, TaskWidgetService.class);
        serviceIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        serviceIntent.setData(Uri.parse("ticktick://widget/tasks/" + appWidgetId + "?limit=" + config.taskLimit + "&all=" + config.includeCompleted + "&deadline=" + config.showDeadline));
        views.setRemoteAdapter(R.id.widget_task_list, serviceIntent);
        views.setEmptyView(R.id.widget_task_list, R.id.widget_empty_container);

        Intent templateIntent = new Intent(context, MainActivity.class);
        templateIntent.setAction(ACTION_OPEN_TASK_DETAIL);
        templateIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent templatePendingIntent = PendingIntent.getActivity(
                context,
                appWidgetId + 20_000,
                templateIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE
        );
        views.setPendingIntentTemplate(R.id.widget_task_list, templatePendingIntent);

        applyTaskTheme(views, config);

        appWidgetManager.updateAppWidget(appWidgetId, views);
        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_task_list);
    }

    private static void updateTimerWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId, WidgetConfig config) {
        long now = SystemClock.elapsedRealtime();
        if (config.timerRunning) {
            config.timerRemainingMs = Math.max(0L, config.timerEndElapsedRealtime - now);
            if (config.timerRemainingMs == 0L) {
                config.timerRunning = false;
                config.timerEndElapsedRealtime = 0L;
            }
            WidgetPrefs.save(context, config);
        }

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_timer);
        views.setTextViewText(R.id.widget_timer_title, context.getString(R.string.widget_timer_title));
        views.setTextViewText(
                R.id.widget_timer_duration,
                context.getString(R.string.widget_timer_duration_format, config.timerDurationMinutes)
        );

        views.setOnClickPendingIntent(R.id.widget_timer_btn_open, buildOpenAppPendingIntent(context, appWidgetId));
        views.setOnClickPendingIntent(R.id.widget_timer_btn_start_pause, buildTimerActionPendingIntent(context, appWidgetId, config.timerRunning ? ACTION_TIMER_PAUSE : ACTION_TIMER_START));
        views.setOnClickPendingIntent(R.id.widget_timer_btn_reset, buildTimerActionPendingIntent(context, appWidgetId, ACTION_TIMER_RESET));
        views.setTextViewText(R.id.widget_timer_btn_start_pause, context.getString(config.timerRunning ? R.string.widget_timer_pause : R.string.widget_timer_start));

        long chronoBase = SystemClock.elapsedRealtime() + Math.max(0L, config.timerRemainingMs);
        views.setChronometer(R.id.widget_timer_chronometer, chronoBase, "%s", config.timerRunning);
        views.setChronometerCountDown(R.id.widget_timer_chronometer, true);

        int stateRes = config.timerRunning
                ? R.string.widget_timer_running
                : (config.timerRemainingMs == 0L ? R.string.widget_timer_done : R.string.widget_timer_paused);
        views.setTextViewText(R.id.widget_timer_state, context.getString(stateRes));

        applyTimerTheme(views, config);
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    private static void applyTaskTheme(RemoteViews views, WidgetConfig config) {
        if (config.theme == WidgetConfig.THEME_DARK) {
            views.setInt(R.id.widget_root, "setBackgroundResource", R.drawable.bg_widget_surface_dark);
            views.setTextColor(R.id.widget_title, Color.WHITE);
            views.setTextColor(R.id.widget_empty, 0xFFCCCCCC);
        } else {
            views.setInt(R.id.widget_root, "setBackgroundResource", R.drawable.bg_widget_surface);
            views.setTextColor(R.id.widget_title, 0xFF3B2B1F);
            views.setTextColor(R.id.widget_empty, 0xFF6D5A49);
        }
    }

    private static void applyTimerTheme(RemoteViews views, WidgetConfig config) {
        if (config.theme == WidgetConfig.THEME_DARK) {
            views.setInt(R.id.widget_timer_root, "setBackgroundResource", R.drawable.bg_widget_surface_dark);
        } else {
            views.setInt(R.id.widget_timer_root, "setBackgroundResource", R.drawable.bg_widget_surface);
        }
    }

    private static void startTimer(Context context, int appWidgetId) {
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            return;
        }
        WidgetConfig config = WidgetPrefs.load(context, appWidgetId);
        if (!WidgetConfig.MODE_TIMER.equals(config.mode) || config.timerRunning) {
            return;
        }

        long remaining = config.timerRemainingMs > 0 ? config.timerRemainingMs : config.getTimerDurationMs();
        config.timerRunning = true;
        config.timerRemainingMs = remaining;
        config.timerEndElapsedRealtime = SystemClock.elapsedRealtime() + remaining;
        WidgetPrefs.save(context, config);
        scheduleTimerFinishAlarm(context, appWidgetId, remaining);
        refreshWidget(context, appWidgetId);
    }

    private static void pauseTimer(Context context, int appWidgetId) {
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            return;
        }
        WidgetConfig config = WidgetPrefs.load(context, appWidgetId);
        if (!config.timerRunning) {
            return;
        }

        config.timerRemainingMs = Math.max(0L, config.timerEndElapsedRealtime - SystemClock.elapsedRealtime());
        config.timerRunning = false;
        config.timerEndElapsedRealtime = 0L;
        WidgetPrefs.save(context, config);
        cancelTimerFinishAlarm(context, appWidgetId);
        refreshWidget(context, appWidgetId);
    }

    private static void resetTimer(Context context, int appWidgetId) {
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            return;
        }
        WidgetConfig config = WidgetPrefs.load(context, appWidgetId);
        config.timerRunning = false;
        config.timerEndElapsedRealtime = 0L;
        config.timerRemainingMs = config.getTimerDurationMs();
        WidgetPrefs.save(context, config);
        cancelTimerFinishAlarm(context, appWidgetId);
        refreshWidget(context, appWidgetId);
    }

    private static void finishTimer(Context context, int appWidgetId) {
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            return;
        }
        WidgetConfig config = WidgetPrefs.load(context, appWidgetId);
        config.timerRunning = false;
        config.timerEndElapsedRealtime = 0L;
        config.timerRemainingMs = 0L;
        WidgetPrefs.save(context, config);
        refreshWidget(context, appWidgetId);
    }

    private static void scheduleTimerFinishAlarm(Context context, int appWidgetId, long remainingMs) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            return;
        }
        long triggerAt = System.currentTimeMillis() + remainingMs;
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, buildTimerActionPendingIntent(context, appWidgetId, ACTION_TIMER_FINISH));
    }

    private static void cancelTimerFinishAlarm(Context context, int appWidgetId) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            return;
        }
        alarmManager.cancel(buildTimerActionPendingIntent(context, appWidgetId, ACTION_TIMER_FINISH));
    }

    private static PendingIntent buildOpenAppPendingIntent(Context context, int appWidgetId) {
        Intent openIntent = new Intent(context, MainActivity.class);
        openIntent.setAction(ACTION_OPEN_APP);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return PendingIntent.getActivity(
                context,
                appWidgetId + 10_000,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    private static PendingIntent buildAddTaskPendingIntent(Context context, int appWidgetId) {
        Intent addTaskIntent = new Intent(context, MainActivity.class);
        addTaskIntent.setAction(ACTION_ADD_TASK);
        addTaskIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return PendingIntent.getActivity(
                context,
                appWidgetId + 30_000,
                addTaskIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    private static PendingIntent buildTimerActionPendingIntent(Context context, int appWidgetId, String action) {
        Intent intent = new Intent(context, TaskWidgetProvider.class);
        intent.setAction(action);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        intent.setData(Uri.parse("ticktick://widget/timer/" + appWidgetId + "/" + action));
        return PendingIntent.getBroadcast(
                context,
                appWidgetId + 40_000 + action.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    private static int resolveWidgetId(Intent intent) {
        int appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            return appWidgetId;
        }
        int[] appWidgetIds = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS);
        if (appWidgetIds != null && appWidgetIds.length > 0) {
            return appWidgetIds[0];
        }
        return AppWidgetManager.INVALID_APPWIDGET_ID;
    }
}

