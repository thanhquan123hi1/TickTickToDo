package hcmute.edu.vn.ticktick.countdown;

import android.os.CountDownTimer;

/**
 * Manages countdown timer logic.
 * Responsible for starting, pausing, resuming, and cancelling the timer.
 */
public class CountdownTimer {

    private static final long TICK_INTERVAL_MS = 50L;

    public interface Listener {
        void onTick(long millisUntilFinished);
        void onFinish();
    }

    private CountDownTimer countDownTimer;
    private long totalTimeMillis = 0;
    private long timeRemainingMillis = 0;

    private final Listener listener;

    public CountdownTimer(Listener listener) {
        this.listener = listener;
    }

    public void start(long hours, long minutes, long seconds) {
        totalTimeMillis = (hours * 3600L + minutes * 60L + seconds) * 1000L;
        timeRemainingMillis = totalTimeMillis;
        scheduleCountdown();
    }

    public void pause() {
        cancelInternal();
    }

    public void resume() {
        scheduleCountdown();
    }

    public void reset() {
        cancelInternal();
        totalTimeMillis = 0;
        timeRemainingMillis = 0;
    }

    public long getTotalTimeMillis() {
        return totalTimeMillis;
    }

    public void destroy() {
        cancelInternal();
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private void scheduleCountdown() {
        countDownTimer = new CountDownTimer(timeRemainingMillis, TICK_INTERVAL_MS) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeRemainingMillis = millisUntilFinished;
                listener.onTick(millisUntilFinished);
            }

            @Override
            public void onFinish() {
                timeRemainingMillis = 0;
                listener.onFinish();
            }
        }.start();
    }

    private void cancelInternal() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }
    }
}
