package hcmute.edu.vn.ticktick.countdown;

import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ServiceInfo;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;

import androidx.annotation.Nullable;
import androidx.core.app.ServiceCompat;

public class CountdownForegroundService extends Service {

    public static final String ACTION_START = "hcmute.edu.vn.ticktick.action.COUNTDOWN_START";
    public static final String ACTION_PAUSE = "hcmute.edu.vn.ticktick.action.COUNTDOWN_PAUSE";
    public static final String ACTION_RESUME = "hcmute.edu.vn.ticktick.action.COUNTDOWN_RESUME";
    public static final String ACTION_STOP = "hcmute.edu.vn.ticktick.action.COUNTDOWN_STOP";
    public static final String ACTION_OPEN_FROM_NOTIFICATION = "hcmute.edu.vn.ticktick.action.COUNTDOWN_OPEN";
    public static final String ACTION_STATE_CHANGED = "hcmute.edu.vn.ticktick.action.COUNTDOWN_STATE_CHANGED";

    public static final String EXTRA_DURATION_MS = "extra_duration_ms";
    public static final String EXTRA_STATUS = "extra_status";
    public static final String EXTRA_TOTAL_MS = "extra_total_ms";
    public static final String EXTRA_REMAINING_MS = "extra_remaining_ms";

    private static final String PREFS = "countdown_foreground_state";
    private static final String KEY_STATUS = "status";
    private static final String KEY_TOTAL = "total";
    private static final String KEY_REMAINING = "remaining";
    private static final String KEY_END_ELAPSED = "end_elapsed";

    private static final long TICK_INTERVAL_MS = 250L;

    private final Handler tickerHandler = new Handler(Looper.getMainLooper());
    private final Runnable tickerRunnable = this::onTick;

    private CountdownState currentState = CountdownState.idle();
    private long endElapsedRealtime = 0L;
    private boolean isInForeground = false;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        CountdownNotificationHelper.ensureChannel(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent != null ? intent.getAction() : null;
        if (action == null) {
            action = ACTION_STOP;
        }

        switch (action) {
            case ACTION_START:
                handleStart(intent);
                break;
            case ACTION_PAUSE:
                handlePause();
                break;
            case ACTION_RESUME:
                handleResume();
                break;
            case ACTION_STOP:
                handleStop();
                break;
            default:
                break;
        }

        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        stopTicker();
        super.onDestroy();
    }

    public static CountdownState readPersistedState(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, MODE_PRIVATE);
        int status = prefs.getInt(KEY_STATUS, CountdownState.STATUS_IDLE);
        long total = prefs.getLong(KEY_TOTAL, 0L);
        long remaining = prefs.getLong(KEY_REMAINING, 0L);
        long endElapsed = prefs.getLong(KEY_END_ELAPSED, 0L);

        if (total <= 0L) {
            return CountdownState.idle();
        }

        if (status == CountdownState.STATUS_RUNNING) {
            long now = SystemClock.elapsedRealtime();
            long computedRemaining = Math.max(0L, endElapsed - now);
            if (computedRemaining <= 0L) {
                return new CountdownState(CountdownState.STATUS_COMPLETED, total, 0L);
            }
            return new CountdownState(CountdownState.STATUS_RUNNING, total, computedRemaining);
        }

        if (status == CountdownState.STATUS_PAUSED) {
            return new CountdownState(CountdownState.STATUS_PAUSED, total, remaining);
        }

        if (status == CountdownState.STATUS_COMPLETED) {
            return new CountdownState(CountdownState.STATUS_COMPLETED, total, 0L);
        }

        return CountdownState.idle();
    }

    private void handleStart(Intent intent) {
        long durationMs = intent != null ? intent.getLongExtra(EXTRA_DURATION_MS, 0L) : 0L;
        if (durationMs <= 0L) {
            return;
        }

        endElapsedRealtime = SystemClock.elapsedRealtime() + durationMs;
        currentState = new CountdownState(CountdownState.STATUS_RUNNING, durationMs, durationMs);
        persistState(currentState, endElapsedRealtime);

        promoteToForeground();
        startTicker();
        broadcastState();
    }

    private void handlePause() {
        if (!currentState.isRunning()) {
            return;
        }

        long remaining = Math.max(0L, endElapsedRealtime - SystemClock.elapsedRealtime());
        currentState = new CountdownState(CountdownState.STATUS_PAUSED, currentState.getTotalDurationMillis(), remaining);
        endElapsedRealtime = 0L;
        stopTicker();

        persistState(currentState, endElapsedRealtime);
        updateNotification();
        broadcastState();
    }

    private void handleResume() {
        if (!currentState.isPaused()) {
            CountdownState persisted = readPersistedState(this);
            if (!persisted.isPaused()) {
                return;
            }
            currentState = persisted;
        }

        endElapsedRealtime = SystemClock.elapsedRealtime() + currentState.getRemainingMillis();
        currentState = new CountdownState(
                CountdownState.STATUS_RUNNING,
                currentState.getTotalDurationMillis(),
                currentState.getRemainingMillis()
        );

        persistState(currentState, endElapsedRealtime);
        promoteToForeground();
        startTicker();
        broadcastState();
    }

    private void handleStop() {
        stopTicker();
        currentState = CountdownState.idle();
        endElapsedRealtime = 0L;

        persistState(currentState, 0L);
        broadcastState();
        stopForeground(STOP_FOREGROUND_REMOVE);
        isInForeground = false;

        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.cancel(CountdownNotificationHelper.NOTIFICATION_ID);
        }
        stopSelf();
    }

    private void onTick() {
        if (!currentState.isRunning()) {
            return;
        }

        long remaining = Math.max(0L, endElapsedRealtime - SystemClock.elapsedRealtime());
        currentState = new CountdownState(
                CountdownState.STATUS_RUNNING,
                currentState.getTotalDurationMillis(),
                remaining
        );

        if (remaining <= 0L) {
            onTimerCompleted();
            return;
        }

        persistState(currentState, endElapsedRealtime);
        updateNotification();
        broadcastState();
        tickerHandler.postDelayed(tickerRunnable, TICK_INTERVAL_MS);
    }

    private void onTimerCompleted() {
        stopTicker();
        currentState = new CountdownState(CountdownState.STATUS_COMPLETED, currentState.getTotalDurationMillis(), 0L);
        endElapsedRealtime = 0L;

        persistState(currentState, 0L);
        broadcastState();
        stopForeground(STOP_FOREGROUND_REMOVE);
        isInForeground = false;

        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.cancel(CountdownNotificationHelper.NOTIFICATION_ID);
        }
        stopSelf();
    }

    private void promoteToForeground() {
        if (!isInForeground) {
            ServiceCompat.startForeground(
                    this,
                    CountdownNotificationHelper.NOTIFICATION_ID,
                    CountdownNotificationHelper.build(this, currentState),
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            );
            isInForeground = true;
        } else {
            updateNotification();
        }
    }

    private void updateNotification() {
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.notify(
                    CountdownNotificationHelper.NOTIFICATION_ID,
                    CountdownNotificationHelper.build(this, currentState)
            );
        }
    }

    private void startTicker() {
        stopTicker();
        tickerHandler.post(tickerRunnable);
    }

    private void stopTicker() {
        tickerHandler.removeCallbacks(tickerRunnable);
    }

    private void broadcastState() {
        Intent stateIntent = new Intent(ACTION_STATE_CHANGED);
        stateIntent.setPackage(getPackageName());
        stateIntent.putExtra(EXTRA_STATUS, currentState.getStatus());
        stateIntent.putExtra(EXTRA_TOTAL_MS, currentState.getTotalDurationMillis());
        stateIntent.putExtra(EXTRA_REMAINING_MS, currentState.getRemainingMillis());
        sendBroadcast(stateIntent);
    }

    private void persistState(CountdownState state, long endElapsed) {
        getSharedPreferences(PREFS, MODE_PRIVATE)
                .edit()
                .putInt(KEY_STATUS, state.getStatus())
                .putLong(KEY_TOTAL, state.getTotalDurationMillis())
                .putLong(KEY_REMAINING, state.getRemainingMillis())
                .putLong(KEY_END_ELAPSED, endElapsed)
                .apply();
    }
}
