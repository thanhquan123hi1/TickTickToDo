package hcmute.edu.vn.ticktick.countdown;

/**
 * Immutable snapshot for countdown rendering and notification updates.
 */
public final class CountdownState {

    public static final int STATUS_IDLE = 0;
    public static final int STATUS_RUNNING = 1;
    public static final int STATUS_PAUSED = 2;
    public static final int STATUS_COMPLETED = 3;

    private final int status;
    private final long totalDurationMillis;
    private final long remainingMillis;

    public CountdownState(int status, long totalDurationMillis, long remainingMillis) {
        this.status = status;
        this.totalDurationMillis = Math.max(0L, totalDurationMillis);
        this.remainingMillis = Math.max(0L, remainingMillis);
    }

    public static CountdownState idle() {
        return new CountdownState(STATUS_IDLE, 0L, 0L);
    }

    public int getStatus() {
        return status;
    }

    public long getTotalDurationMillis() {
        return totalDurationMillis;
    }

    public long getRemainingMillis() {
        return remainingMillis;
    }

    public boolean isRunning() {
        return status == STATUS_RUNNING;
    }

    public boolean isPaused() {
        return status == STATUS_PAUSED;
    }

    public boolean isCompleted() {
        return status == STATUS_COMPLETED;
    }

    public float getProgressFraction() {
        if (totalDurationMillis <= 0L) {
            return 0f;
        }
        float value = (float) remainingMillis / (float) totalDurationMillis;
        if (value < 0f) return 0f;
        return Math.min(value, 1f);
    }
}

