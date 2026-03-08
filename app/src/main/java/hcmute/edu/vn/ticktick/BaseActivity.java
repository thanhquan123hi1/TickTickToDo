package hcmute.edu.vn.ticktick;

import android.content.Context;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Base activity that applies the correct locale before every screen is created.
 * All activities in the app should extend this class instead of AppCompatActivity
 * so that language changes take effect globally without any extra per-activity code.
 */
public abstract class BaseActivity extends AppCompatActivity {

    /**
     * Wraps the base context with the locale chosen by the user (or system default).
     * Android calls this before {@code onCreate}, so all string resources loaded
     * during the activity's lifetime use the correct language.
     */
    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LanguageManager.wrapContext(newBase));
    }
}

