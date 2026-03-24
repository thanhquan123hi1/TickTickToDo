package hcmute.edu.vn.ticktick.music;

import android.net.Uri;

/**
 * Immutable model for one audio file discovered from MediaStore.
 */
public class ScannedAudioItem {

    private final Uri uri;
    private final String title;
    private final String artist;
    private final long durationMs;

    public ScannedAudioItem(Uri uri, String title, String artist, long durationMs) {
        this.uri = uri;
        this.title = title;
        this.artist = artist;
        this.durationMs = durationMs;
    }

    public Uri getUri() {
        return uri;
    }

    public String getTitle() {
        return title;
    }

    public String getArtist() {
        return artist;
    }

    public long getDurationMs() {
        return durationMs;
    }
}

