package hcmute.edu.vn.ticktick.countdown;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationCompat;

import hcmute.edu.vn.ticktick.CountdownActivity;
import hcmute.edu.vn.ticktick.R;

public final class CountdownNotificationHelper {

    public static final String CHANNEL_ID = "countdown_timer_channel";
    public static final int NOTIFICATION_ID = 44021;

    private CountdownNotificationHelper() {
    }

    public static void ensureChannel(Context context) {
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager == null) {
            return;
        }

        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.countdown_notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
        );
        channel.setDescription(context.getString(R.string.countdown_notification_channel_description));
        channel.setShowBadge(false);
        manager.createNotificationChannel(channel);
    }

    public static Notification build(Context context, CountdownState state) {
        Intent openIntent = new Intent(context, CountdownActivity.class)
                .setAction(CountdownForegroundService.ACTION_OPEN_FROM_NOTIFICATION)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent openPendingIntent = PendingIntent.getActivity(
                context,
                1,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        String pauseOrResumeAction = state.isRunning()
                ? CountdownForegroundService.ACTION_PAUSE
                : CountdownForegroundService.ACTION_RESUME;
        int pauseOrResumeLabel = state.isRunning()
                ? R.string.countdown_pause
                : R.string.countdown_resume;
        int pauseOrResumeIcon = state.isRunning()
                ? android.R.drawable.ic_media_pause
                : android.R.drawable.ic_media_play;

        PendingIntent pauseOrResumePendingIntent = createServicePendingIntent(context, pauseOrResumeAction, 2);
        PendingIntent stopPendingIntent = createServicePendingIntent(context, CountdownForegroundService.ACTION_STOP, 3);

        String contentText = context.getString(
                state.isRunning() ? R.string.countdown_notification_running : R.string.countdown_notification_paused,
                formatDuration(state.getRemainingMillis())
        );

        return new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_timer)
                .setContentTitle(context.getString(R.string.countdown_notification_title))
                .setContentText(contentText)
                .setOnlyAlertOnce(true)
                .setOngoing(state.isRunning() || state.isPaused())
                .setSilent(true)
                .setContentIntent(openPendingIntent)
                .addAction(pauseOrResumeIcon, context.getString(pauseOrResumeLabel), pauseOrResumePendingIntent)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, context.getString(R.string.countdown_stop), stopPendingIntent)
                .build();
    }

    private static PendingIntent createServicePendingIntent(Context context, String action, int requestCode) {
        Intent intent = new Intent(context, CountdownForegroundService.class).setAction(action);
        return PendingIntent.getService(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    public static String formatDuration(long millis) {
        long totalSeconds = Math.max(0L, millis / 1000L);
        long hours = totalSeconds / 3600L;
        long minutes = (totalSeconds % 3600L) / 60L;
        long seconds = totalSeconds % 60L;

        if (hours > 0L) {
            return String.format(java.util.Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds);
        }
        return String.format(java.util.Locale.getDefault(), "%02d:%02d", minutes, seconds);
    }
}

