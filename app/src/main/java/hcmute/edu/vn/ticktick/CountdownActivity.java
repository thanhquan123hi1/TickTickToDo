package hcmute.edu.vn.ticktick;

import android.os.Bundle;
import android.view.View;
import android.widget.NumberPicker;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;

import java.util.Locale;

import hcmute.edu.vn.ticktick.countdown.AlarmPlayer;
import hcmute.edu.vn.ticktick.countdown.CountdownTimer;

/**
 * CountdownActivity is responsible solely for UI: binding views, reacting to user
 * input, and updating the display. All timer logic is delegated to {@link CountdownTimer}
 * and alarm playback to {@link AlarmPlayer}.
 */
public class CountdownActivity extends BaseActivity implements CountdownTimer.Listener {

    // --- Views ---
    private MaterialToolbar toolbar;
    private NumberPicker pickerHours, pickerMinutes, pickerSeconds;
    private TextView tvTimerDisplay, tvTimerStatus, tvPresetLabel;
    private ProgressBar progressCountdown;
    private MaterialButton btnStart, btnPause, btnResume, btnReset;
    private View layoutInput, layoutPresets;

    // --- Collaborators ---
    private CountdownTimer countdownTimer;
    private AlarmPlayer alarmPlayer;

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_countdown);

        bindViews();
        setupToolbar();
        setupNumberPickers();
        setupPresetChips();
        setupButtonListeners();

        countdownTimer = new CountdownTimer(this);
        alarmPlayer = new AlarmPlayer(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        countdownTimer.destroy();
        alarmPlayer.stop();
    }

    // -------------------------------------------------------------------------
    // CountdownTimer.Listener callbacks
    // -------------------------------------------------------------------------

    @Override
    public void onTick(long millisUntilFinished) {
        updateTimerDisplay(millisUntilFinished);
        updateProgressBar(millisUntilFinished);
    }

    @Override
    public void onFinish() {
        updateTimerDisplay(0);
        updateProgressBar(0);
        handleTimerFinished();
    }

    // -------------------------------------------------------------------------
    // Button actions
    // -------------------------------------------------------------------------

    private void onStartClicked() {
        int hours   = pickerHours.getValue();
        int minutes = pickerMinutes.getValue();
        int seconds = pickerSeconds.getValue();

        if (hours == 0 && minutes == 0 && seconds == 0) {
            showStatus(R.string.countdown_set_time_warning);
            return;
        }

        countdownTimer.start(hours, minutes, seconds);
        showTimerMode();
    }

    private void onPauseClicked() {
        countdownTimer.pause();
        btnPause.setVisibility(View.GONE);
        btnResume.setVisibility(View.VISIBLE);
        showStatus(R.string.countdown_paused);
    }

    private void onResumeClicked() {
        countdownTimer.resume();
        btnResume.setVisibility(View.GONE);
        btnPause.setVisibility(View.VISIBLE);
        showStatus(R.string.countdown_running);
    }

    private void onResetClicked() {
        countdownTimer.reset();
        alarmPlayer.stop();
        showInputMode();
    }

    // -------------------------------------------------------------------------
    // UI helpers
    // -------------------------------------------------------------------------

    private void handleTimerFinished() {
        btnPause.setVisibility(View.GONE);
        btnResume.setVisibility(View.GONE);
        tvTimerDisplay.setTextColor(getResources().getColor(R.color.completed, null));
        showStatus(R.string.countdown_finished);

        alarmPlayer.play();
        showFinishedDialog();
    }

    private void showTimerMode() {
        layoutInput.setVisibility(View.GONE);
        layoutPresets.setVisibility(View.GONE);
        tvPresetLabel.setVisibility(View.GONE);
        btnStart.setVisibility(View.GONE);

        tvTimerDisplay.setTextColor(getResources().getColor(R.color.nav_item_icon_tint, null));
        tvTimerDisplay.setVisibility(View.VISIBLE);
        progressCountdown.setVisibility(View.VISIBLE);
        btnPause.setVisibility(View.VISIBLE);
        btnReset.setVisibility(View.VISIBLE);
        showStatus(R.string.countdown_running);
    }

    private void showInputMode() {
        layoutInput.setVisibility(View.VISIBLE);
        layoutPresets.setVisibility(View.VISIBLE);
        tvPresetLabel.setVisibility(View.VISIBLE);
        btnStart.setVisibility(View.VISIBLE);

        tvTimerDisplay.setVisibility(View.GONE);
        progressCountdown.setVisibility(View.GONE);
        btnPause.setVisibility(View.GONE);
        btnResume.setVisibility(View.GONE);
        btnReset.setVisibility(View.GONE);
        tvTimerStatus.setVisibility(View.GONE);

        pickerHours.setValue(0);
        pickerMinutes.setValue(0);
        pickerSeconds.setValue(0);
    }

    private void showStatus(int stringResId) {
        tvTimerStatus.setText(stringResId);
        tvTimerStatus.setVisibility(View.VISIBLE);
    }

    private void updateTimerDisplay(long millis) {
        int hours   = (int) (millis / 3_600_000);
        int minutes = (int) ((millis % 3_600_000) / 60_000);
        int seconds = (int) ((millis % 60_000) / 1_000);
        tvTimerDisplay.setText(
                String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds));
    }

    private void updateProgressBar(long remaining) {
        long total = countdownTimer.getTotalTimeMillis();
        if (total > 0) {
            int progress = (int) ((remaining * 100) / total);
            progressCountdown.setProgress(progress);
        }
    }

    private void showFinishedDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.countdown_finished_title)
                .setMessage(R.string.countdown_finished_message)
                .setPositiveButton(R.string.btn_ok, (dialog, which) -> onResetClicked())
                .setCancelable(false)
                .show();
    }

    // -------------------------------------------------------------------------
    // Setup helpers
    // -------------------------------------------------------------------------

    private void bindViews() {
        toolbar          = findViewById(R.id.toolbar_countdown);
        pickerHours      = findViewById(R.id.picker_hours);
        pickerMinutes    = findViewById(R.id.picker_minutes);
        pickerSeconds    = findViewById(R.id.picker_seconds);
        tvTimerDisplay   = findViewById(R.id.tv_timer_display);
        tvTimerStatus    = findViewById(R.id.tv_timer_status);
        progressCountdown = findViewById(R.id.progress_countdown);
        btnStart         = findViewById(R.id.btn_start);
        btnPause         = findViewById(R.id.btn_pause);
        btnResume        = findViewById(R.id.btn_resume);
        btnReset         = findViewById(R.id.btn_reset);
        layoutInput      = findViewById(R.id.layout_input);
        layoutPresets    = findViewById(R.id.layout_presets);
        tvPresetLabel    = findViewById(R.id.tv_preset_label);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupNumberPickers() {
        pickerHours.setMinValue(0);   pickerHours.setMaxValue(23);   pickerHours.setValue(0);
        pickerMinutes.setMinValue(0); pickerMinutes.setMaxValue(59); pickerMinutes.setValue(0);
        pickerSeconds.setMinValue(0); pickerSeconds.setMaxValue(59); pickerSeconds.setValue(0);
    }

    private void setupPresetChips() {
        Chip chip1min  = findViewById(R.id.chip_1min);
        Chip chip5min  = findViewById(R.id.chip_5min);
        Chip chip10min = findViewById(R.id.chip_10min);
        Chip chip25min = findViewById(R.id.chip_25min);

        chip1min.setOnClickListener(v  -> applyPreset(0, 1,  0));
        chip5min.setOnClickListener(v  -> applyPreset(0, 5,  0));
        chip10min.setOnClickListener(v -> applyPreset(0, 10, 0));
        chip25min.setOnClickListener(v -> applyPreset(0, 25, 0));
    }

    private void applyPreset(int hours, int minutes, int seconds) {
        pickerHours.setValue(hours);
        pickerMinutes.setValue(minutes);
        pickerSeconds.setValue(seconds);
    }

    private void setupButtonListeners() {
        btnStart.setOnClickListener(v  -> onStartClicked());
        btnPause.setOnClickListener(v  -> onPauseClicked());
        btnResume.setOnClickListener(v -> onResumeClicked());
        btnReset.setOnClickListener(v  -> onResetClicked());
    }
}
