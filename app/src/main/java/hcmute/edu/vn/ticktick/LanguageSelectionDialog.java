package hcmute.edu.vn.ticktick;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/**
 * A dialog that lets the user pick the app language.
 *
 * Options:
 *   - Follow System
 *   - Vietnamese
 *   - English
 *   - Chinese (Simplified)
 *
 * After the user confirms, the preference is saved via {@link LanguageManager}
 * and the app restarts from {@link MainActivity} to apply the new locale.
 */
public class LanguageSelectionDialog extends DialogFragment {

    public static LanguageSelectionDialog newInstance() {
        return new LanguageSelectionDialog();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Context ctx = requireContext();

        // Build radio-group view programmatically to keep the layout simple.
        RadioGroup radioGroup = buildRadioGroup(ctx);

        // Pre-select the currently saved language.
        String current = LanguageManager.getSavedLanguage(ctx);
        preselectRadio(radioGroup, current);

        return new MaterialAlertDialogBuilder(ctx)
                .setTitle(R.string.language_title)
                .setView(radioGroup)
                .setPositiveButton(R.string.btn_ok, (dialog, which) -> {
                    int checkedId = radioGroup.getCheckedRadioButtonId();
                    String chosen = tagFromId(checkedId);
                    applyLanguage(chosen);
                })
                .setNegativeButton(R.string.btn_cancel, null)
                .create();
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private RadioGroup buildRadioGroup(Context ctx) {
        RadioGroup group = new RadioGroup(ctx);
        int padding = dpToPx(ctx, 24);
        group.setPadding(padding, padding / 2, padding, padding / 2);

        group.addView(makeRadio(ctx, R.id.radio_lang_system,      R.string.language_system));
        group.addView(makeRadio(ctx, R.id.radio_lang_vi,           R.string.language_vietnamese));
        group.addView(makeRadio(ctx, R.id.radio_lang_en,           R.string.language_english));
        group.addView(makeRadio(ctx, R.id.radio_lang_zh,           R.string.language_chinese));
        return group;
    }

    private RadioButton makeRadio(Context ctx, int id, int labelRes) {
        RadioButton rb = new RadioButton(ctx);
        rb.setId(id);
        rb.setText(labelRes);
        int verticalPadding = dpToPx(ctx, 10);
        rb.setPadding(0, verticalPadding, 0, verticalPadding);
        return rb;
    }

    private void preselectRadio(RadioGroup group, String langCode) {
        int idToCheck;
        switch (langCode) {
            case LanguageManager.LANG_EN:     idToCheck = R.id.radio_lang_en;     break;
            case LanguageManager.LANG_ZH:     idToCheck = R.id.radio_lang_zh;     break;
            case LanguageManager.LANG_SYSTEM: idToCheck = R.id.radio_lang_system; break;
            case LanguageManager.LANG_VI:
            default:                          idToCheck = R.id.radio_lang_vi;     break;
        }
        group.check(idToCheck);
    }

    private String tagFromId(int radioId) {
        if (radioId == R.id.radio_lang_en)     return LanguageManager.LANG_EN;
        if (radioId == R.id.radio_lang_zh)     return LanguageManager.LANG_ZH;
        if (radioId == R.id.radio_lang_system) return LanguageManager.LANG_SYSTEM;
        return LanguageManager.LANG_VI;
    }

    private void applyLanguage(String langCode) {
        Context appCtx = requireContext().getApplicationContext();
        LanguageManager.saveLanguage(appCtx, langCode);

        // Restart the app from MainActivity so every screen re-inflates with the new locale.
        Intent restart = new Intent(requireContext(), MainActivity.class);
        restart.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(restart);
    }

    private int dpToPx(Context ctx, int dp) {
        float density = ctx.getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}


