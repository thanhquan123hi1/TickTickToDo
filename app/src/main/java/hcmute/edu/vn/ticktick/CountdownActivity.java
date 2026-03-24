package hcmute.edu.vn.ticktick;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageButton;
import android.widget.NumberPicker;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import hcmute.edu.vn.ticktick.countdown.AlarmPlayer;
import hcmute.edu.vn.ticktick.countdown.CountdownSoundPreferences;
import hcmute.edu.vn.ticktick.countdown.CountdownForegroundService;
import hcmute.edu.vn.ticktick.countdown.CountdownNotificationHelper;
import hcmute.edu.vn.ticktick.countdown.CountdownRingView;
import hcmute.edu.vn.ticktick.countdown.CountdownState;
import hcmute.edu.vn.ticktick.music.MusicPlaylistActivity;

public class CountdownActivity extends BaseActivity {

    private ImageButton btnBack;
    private TextView tvHeaderClock;
    private TextView tvTimerDisplay;
    private TextView tvTimerStatus;
    private TextView tvPrimaryLabel;
    private TextView tvSecondaryLabel;
    private CountdownRingView countdownRingView;
    private MaterialButton btnPrimaryAction;
    private MaterialButton btnSecondaryAction;
    private View layoutInlinePicker;
    private NumberPicker pickerMinutes;
    private NumberPicker pickerSeconds;
    private TextView tvSoundValue;

    private AlarmPlayer alarmPlayer;
    private CountdownSoundPreferences countdownSoundPreferences;
    private CountdownState currentState = CountdownState.idle();
    private long selectedDurationMillis = 5 * 60 * 1000L;
    private boolean isApplyingPickerValues = false;

    private final Handler clockHandler = new Handler(Looper.getMainLooper());
    private final Runnable clockRunnable = new Runnable() {
        @Override
        public void run() {
            tvHeaderClock.setText(new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date()));
            clockHandler.postDelayed(this, 30_000L);
        }
    };

    private final BroadcastReceiver stateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            CountdownState updatedState = new CountdownState(
                intent.getIntExtra(CountdownForegroundService.EXTRA_STATUS, CountdownState.STATUS_IDLE),
                intent.getLongExtra(CountdownForegroundService.EXTRA_TOTAL_MS, 0L),
                intent.getLongExtra(CountdownForegroundService.EXTRA_REMAINING_MS, 0L)
            );
            syncSelectedDurationWithState(updatedState);
            renderState(updatedState);
        }
    };

    private final ActivityResultLauncher<String> notificationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    startCountdownFromPicker();
                } else {
                    showStatus(R.string.countdown_permission_required);
                }
            });

    private final ActivityResultLauncher<Intent> soundPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> refreshSoundLabel());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_countdown);

        bindViews();
        setupPickers();
        setupActions();

        alarmPlayer = new AlarmPlayer(this);
        countdownSoundPreferences = new CountdownSoundPreferences(this);
        refreshSoundLabel();
    }

    @Override
    protected void onStart() {
        super.onStart();
        registerReceiver(stateReceiver, new IntentFilter(CountdownForegroundService.ACTION_STATE_CHANGED), Context.RECEIVER_NOT_EXPORTED);
        clockHandler.removeCallbacks(clockRunnable);
        clockHandler.post(clockRunnable);

        CountdownState persistedState = CountdownForegroundService.readPersistedState(this);
        syncSelectedDurationWithState(persistedState);
        renderState(persistedState);
        refreshSoundLabel();
    }

    @Override
    protected void onStop() {
        super.onStop();
        clockHandler.removeCallbacks(clockRunnable);
        try {
            unregisterReceiver(stateReceiver);
        } catch (IllegalArgumentException ignored) {
            // Receiver may already be unregistered by system recreation edge cases.
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        alarmPlayer.stop();
    }

    private void bindViews() {
        btnBack = findViewById(R.id.btn_back);
        tvHeaderClock = findViewById(R.id.tv_header_clock);
        tvTimerDisplay = findViewById(R.id.tv_timer_display);
        tvTimerStatus = findViewById(R.id.tv_timer_status);
        tvPrimaryLabel = findViewById(R.id.tv_primary_label);
        tvSecondaryLabel = findViewById(R.id.tv_secondary_label);
        countdownRingView = findViewById(R.id.view_countdown_ring);
        btnPrimaryAction = findViewById(R.id.btn_primary_action);
        btnSecondaryAction = findViewById(R.id.btn_secondary_action);
        layoutInlinePicker = findViewById(R.id.layout_inline_picker);
        pickerMinutes = findViewById(R.id.picker_minutes);
        pickerSeconds = findViewById(R.id.picker_seconds);
        tvSoundValue = findViewById(R.id.tv_sound_value);
    }

    private void setupPickers() {
        pickerMinutes.setMinValue(0);
        pickerMinutes.setMaxValue(180);
        pickerMinutes.setWrapSelectorWheel(false);
        pickerSeconds.setMinValue(0);
        pickerSeconds.setMaxValue(59);
        pickerSeconds.setWrapSelectorWheel(false);

        NumberPicker.OnValueChangeListener listener = (picker, oldVal, newVal) -> {
            if (isApplyingPickerValues) {
                return;
            }
            updateSelectedDurationFromPickers();
            if (!currentState.isRunning() && !currentState.isPaused()) {
                tvTimerDisplay.setText(CountdownNotificationHelper.formatDuration(selectedDurationMillis));
                if (currentState.isCompleted()) {
                    showStatus(R.string.countdown_status_idle);
                }
            }
        };

        pickerMinutes.setOnValueChangedListener(listener);
        pickerSeconds.setOnValueChangedListener(listener);
        applyDurationToPickers(selectedDurationMillis);
    }

    private void setupActions() {
        btnBack.setOnClickListener(v -> finish());

        btnPrimaryAction.setOnClickListener(v -> {
            if (currentState.isRunning()) {
                sendServiceCommand(CountdownForegroundService.ACTION_PAUSE, 0L);
                return;
            }

            if (currentState.isPaused()) {
                sendServiceCommand(CountdownForegroundService.ACTION_RESUME, 0L);
                return;
            }

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
                return;
            }

            startCountdownFromPicker();
        });

        btnSecondaryAction.setOnClickListener(v -> {
            alarmPlayer.stop();
            sendServiceCommand(CountdownForegroundService.ACTION_STOP, 0L);
        });

        findViewById(R.id.btn_choose_sound).setOnClickListener(v ->
                soundPickerLauncher.launch(new Intent(this, MusicPlaylistActivity.class)));
    }

    private void startCountdownFromPicker() {
        updateSelectedDurationFromPickers();
        long durationMs = selectedDurationMillis;
        if (durationMs <= 0L) {
            showStatus(R.string.countdown_set_time_warning);
            return;
        }

        alarmPlayer.stop();
        sendServiceCommand(CountdownForegroundService.ACTION_START, durationMs);
    }

    private void sendServiceCommand(String action, long durationMs) {
        Intent serviceIntent = new Intent(this, CountdownForegroundService.class).setAction(action);
        if (CountdownForegroundService.ACTION_START.equals(action)) {
            serviceIntent.putExtra(CountdownForegroundService.EXTRA_DURATION_MS, durationMs);
            ContextCompat.startForegroundService(this, serviceIntent);
            return;
        }
        startService(serviceIntent);
    }

    private void renderState(CountdownState newState) {
        CountdownState previousState = currentState;
        currentState = newState;

        boolean editable = newState.getStatus() == CountdownState.STATUS_IDLE || newState.getStatus() == CountdownState.STATUS_COMPLETED;
        layoutInlinePicker.setVisibility(editable ? View.VISIBLE : View.GONE);
        setPickerEnabled(editable);

        if (newState.isRunning()) {
            tvTimerDisplay.setText(CountdownNotificationHelper.formatDuration(newState.getRemainingMillis()));
            countdownRingView.setProgressFraction(newState.getProgressFraction());
            showStatus(R.string.countdown_running);
            applyActionState(R.drawable.ic_countdown_pause, R.string.countdown_pause, true, R.string.countdown_stop);
            return;
        }

        if (newState.isPaused()) {
            tvTimerDisplay.setText(CountdownNotificationHelper.formatDuration(newState.getRemainingMillis()));
            countdownRingView.setProgressFraction(newState.getProgressFraction());
            showStatus(R.string.countdown_paused);
            applyActionState(R.drawable.ic_countdown_play, R.string.countdown_resume, true, R.string.countdown_stop);
            return;
        }

        if (newState.isCompleted()) {
            tvTimerDisplay.setText(CountdownNotificationHelper.formatDuration(newState.getRemainingMillis()));
            countdownRingView.setProgressFraction(0f);
            showStatus(R.string.countdown_finished);
            applyActionState(R.drawable.ic_countdown_play, R.string.countdown_start, true, R.string.countdown_reset);
            maybeHandleCompletion(previousState, newState);
            return;
        }

        tvTimerDisplay.setText(CountdownNotificationHelper.formatDuration(selectedDurationMillis));
        countdownRingView.setProgressFraction(selectedDurationMillis > 0L ? 1f : 0f);
        showStatus(R.string.countdown_status_idle);
        applyActionState(R.drawable.ic_countdown_play, R.string.countdown_start, false, R.string.countdown_stop);
    }

    private void maybeHandleCompletion(CountdownState previousState, CountdownState newState) {
        if (previousState.getStatus() == CountdownState.STATUS_COMPLETED || !newState.isCompleted()) {
            return;
        }

        alarmPlayer.play();
        new AlertDialog.Builder(this)
                .setTitle(R.string.countdown_finished_title)
                .setMessage(R.string.countdown_finished_message)
                .setPositiveButton(R.string.btn_ok, (dialog, which) -> {
                    alarmPlayer.stop();
                    sendServiceCommand(CountdownForegroundService.ACTION_STOP, 0L);
                })
                .setCancelable(false)
                .show();
    }

    private void applyActionState(int primaryIconRes, int primaryTextRes, boolean showSecondary, int secondaryTextRes) {
        btnPrimaryAction.setIconResource(primaryIconRes);
        tvPrimaryLabel.setText(primaryTextRes);

        btnSecondaryAction.setVisibility(showSecondary ? View.VISIBLE : View.INVISIBLE);
        tvSecondaryLabel.setVisibility(showSecondary ? View.VISIBLE : View.INVISIBLE);
        tvSecondaryLabel.setText(secondaryTextRes);
    }

    private void showStatus(int stringRes) {
        tvTimerStatus.setText(stringRes);
    }

    private void syncSelectedDurationWithState(CountdownState state) {
        if (state.getTotalDurationMillis() <= 0L) {
            return;
        }
        selectedDurationMillis = state.getTotalDurationMillis();
        applyDurationToPickers(selectedDurationMillis);
    }

    private void updateSelectedDurationFromPickers() {
        int minutes = pickerMinutes.getValue();
        int seconds = pickerSeconds.getValue();
        selectedDurationMillis = (minutes * 60L + seconds) * 1000L;
    }

    private void applyDurationToPickers(long durationMillis) {
        long totalSeconds = Math.max(0L, durationMillis / 1000L);
        int minutes = (int) Math.min(180L, totalSeconds / 60L);
        int seconds = (int) (totalSeconds % 60L);

        isApplyingPickerValues = true;
        pickerMinutes.setValue(minutes);
        pickerSeconds.setValue(seconds);
        isApplyingPickerValues = false;
    }

    private void setPickerEnabled(boolean enabled) {
        pickerMinutes.setEnabled(enabled);
        pickerSeconds.setEnabled(enabled);
        layoutInlinePicker.setAlpha(enabled ? 1f : 0.5f);
    }

    private void refreshSoundLabel() {
        tvSoundValue.setText(countdownSoundPreferences.getSelectedSoundTitle(this));
    }
}
