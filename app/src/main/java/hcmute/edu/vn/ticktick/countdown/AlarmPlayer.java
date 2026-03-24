package hcmute.edu.vn.ticktick.countdown;

import android.content.Context;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.Nullable;

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
        stop();
        try {
            Uri selectedUri = resolveSelectedUri();
            mediaPlayer = createPlayer(selectedUri);

            if (mediaPlayer == null) {
                Uri fallbackUri = getFallbackUri();
                mediaPlayer = createPlayer(fallbackUri);
            }

            if (mediaPlayer != null) {
                mediaPlayer.setLooping(false);
                mediaPlayer.start();
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to play alarm sound", e);
        }
    }

    @Nullable
    private Uri resolveSelectedUri() {
        String selectedUri = new CountdownSoundPreferences(context).getSelectedSoundUri();
        if (selectedUri == null || selectedUri.trim().isEmpty()) {
            return null;
        }
        return Uri.parse(selectedUri);
    }

    @Nullable
    private MediaPlayer createPlayer(@Nullable Uri uri) {
        if (uri == null) {
            return null;
        }
        return MediaPlayer.create(context, uri);
    }

    @Nullable
    private Uri getFallbackUri() {
        Uri fallbackUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        if (fallbackUri == null) {
            fallbackUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        }
        return fallbackUri;
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
