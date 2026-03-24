package hcmute.edu.vn.ticktick.countdown;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.Nullable;

import hcmute.edu.vn.ticktick.R;

/**
 * Persists the countdown alarm sound selected by the user.
 */
public class CountdownSoundPreferences {

    private static final String PREFS_NAME = "countdown_sound_prefs";
    private static final String KEY_SOUND_URI = "sound_uri";
    private static final String KEY_SOUND_TITLE = "sound_title";

    private final SharedPreferences prefs;

    public CountdownSoundPreferences(Context context) {
        Context appContext = context.getApplicationContext();
        prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void saveSelection(String uri, String title) {
        prefs.edit()
                .putString(KEY_SOUND_URI, uri)
                .putString(KEY_SOUND_TITLE, title)
                .apply();
    }

    public void clearSelection() {
        prefs.edit()
                .remove(KEY_SOUND_URI)
                .remove(KEY_SOUND_TITLE)
                .apply();
    }

    @Nullable
    public String getSelectedSoundUri() {
        return prefs.getString(KEY_SOUND_URI, null);
    }

    public String getSelectedSoundTitle(Context context) {
        String selectedTitle = prefs.getString(KEY_SOUND_TITLE, null);
        if (selectedTitle == null || selectedTitle.trim().isEmpty()) {
            return context.getString(R.string.countdown_sound_default);
        }
        return selectedTitle;
    }
}

