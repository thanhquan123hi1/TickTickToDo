package hcmute.edu.vn.ticktick.widget;

public class WidgetConfig {

    public static final String MODE_TASKS = "tasks";
    public static final String MODE_TIMER = "timer";

    public static final int TASK_LIMIT_DEFAULT = 3;
    public static final int TIMER_DURATION_MINUTES_DEFAULT = 25;
    public static final int THEME_SYSTEM = 0;
    public static final int THEME_LIGHT = 1;
    public static final int THEME_DARK = 2;

    public int appWidgetId;
    public String mode = MODE_TASKS;
    public int taskLimit = TASK_LIMIT_DEFAULT;
    public boolean includeCompleted = false;
    public boolean showDeadline = true;
    public int theme = THEME_SYSTEM;

    public int timerDurationMinutes = TIMER_DURATION_MINUTES_DEFAULT;
    public boolean timerRunning = false;
    public long timerEndElapsedRealtime = 0L;
    public long timerRemainingMs = getTimerDurationMs();

    public long getTimerDurationMs() {
        return Math.max(1, timerDurationMinutes) * 60_000L;
    }
}

