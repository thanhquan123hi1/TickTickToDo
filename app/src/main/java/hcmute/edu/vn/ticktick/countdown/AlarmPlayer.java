package hcmute.edu.vn.ticktick.countdown;

import android.content.Context;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.util.Log;

/**
 * Responsible for playing and stopping the alarm sound when the countdown finishes.
 */
public class AlarmPlayer {

    private static final String TAG = "AlarmPlayer";

    private MediaPlayer mediaPlayer;
    private final Context context;

    public AlarmPlayer(Context context) {
        this.context = context.getApplicationContext();
    }

    public void play() {
        try {
            Uri alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            if (alarmUri == null) {
                alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            }
            mediaPlayer = MediaPlayer.create(context, alarmUri);
            if (mediaPlayer != null) {
                mediaPlayer.setLooping(false);
                mediaPlayer.start();
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to play alarm sound", e);
        }
    }

    public void stop() {
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
}
