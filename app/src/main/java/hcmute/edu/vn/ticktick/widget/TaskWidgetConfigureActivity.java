package hcmute.edu.vn.ticktick.widget;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Spinner;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import hcmute.edu.vn.ticktick.R;

public class TaskWidgetConfigureActivity extends AppCompatActivity {

    private int appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

    private RadioButton radioTasks;
    private Spinner spinnerTaskLimit;
    private CheckBox cbIncludeCompleted;
    private CheckBox cbShowDeadline;
    private Spinner spinnerTheme;

    private RadioButton radioTimer;
    private Spinner spinnerTimerPreset;
    private EditText etCustomMinutes;

    private View sectionTasks;
    private View sectionTimer;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_widget_configure);
        setResult(Activity.RESULT_CANCELED);

        Intent intent = getIntent();
        if (intent != null) {
            appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        }
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
            return;
        }

        bindViews();
        setupOptions();
        loadCurrentConfig();

        Button btnSave = findViewById(R.id.btn_widget_save);
        btnSave.setOnClickListener(v -> saveConfigAndFinish());
    }

    private void bindViews() {
        radioTasks = findViewById(R.id.radio_widget_mode_tasks);
        spinnerTaskLimit = findViewById(R.id.spinner_widget_task_limit);
        cbIncludeCompleted = findViewById(R.id.checkbox_widget_include_completed);
        cbShowDeadline = findViewById(R.id.checkbox_widget_show_deadline);
        spinnerTheme = findViewById(R.id.spinner_widget_theme);

        radioTimer = findViewById(R.id.radio_widget_mode_timer);
        spinnerTimerPreset = findViewById(R.id.spinner_timer_preset);
        etCustomMinutes = findViewById(R.id.et_timer_custom_minutes);

        sectionTasks = findViewById(R.id.section_widget_tasks);
        sectionTimer = findViewById(R.id.section_widget_timer);

        View.OnClickListener modeChanged = v -> updateModeVisibility();
        radioTasks.setOnClickListener(modeChanged);
        radioTimer.setOnClickListener(modeChanged);
    }

    private void setupOptions() {
        spinnerTaskLimit.setAdapter(new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"3", "5", "10"}
        ));

        spinnerTheme.setAdapter(new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{
                        getString(R.string.widget_theme_system),
                        getString(R.string.widget_theme_light),
                        getString(R.string.widget_theme_dark)
                }
        ));

        spinnerTimerPreset.setAdapter(new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"25", "50", getString(R.string.widget_timer_custom)}
        ));

        spinnerTimerPreset.setOnItemSelectedListener(new SimpleItemSelectedListener(position ->
                etCustomMinutes.setVisibility(position == 2 ? View.VISIBLE : View.GONE)));
    }

    private void loadCurrentConfig() {
        WidgetConfig config = WidgetPrefs.load(this, appWidgetId);

        if (WidgetConfig.MODE_TIMER.equals(config.mode)) {
            radioTimer.setChecked(true);
        } else {
            radioTasks.setChecked(true);
        }

        int taskLimitIndex = 0;
        if (config.taskLimit == 5) {
            taskLimitIndex = 1;
        } else if (config.taskLimit == 10) {
            taskLimitIndex = 2;
        }
        spinnerTaskLimit.setSelection(taskLimitIndex);

        cbIncludeCompleted.setChecked(config.includeCompleted);
        cbShowDeadline.setChecked(config.showDeadline);
        spinnerTheme.setSelection(Math.max(0, Math.min(2, config.theme)));

        if (config.timerDurationMinutes == 25) {
            spinnerTimerPreset.setSelection(0);
        } else if (config.timerDurationMinutes == 50) {
            spinnerTimerPreset.setSelection(1);
        } else {
            spinnerTimerPreset.setSelection(2);
            etCustomMinutes.setText(String.valueOf(config.timerDurationMinutes));
            etCustomMinutes.setVisibility(View.VISIBLE);
        }

        updateModeVisibility();
    }

    private void updateModeVisibility() {
        boolean isTaskMode = radioTasks.isChecked();
        sectionTasks.setVisibility(isTaskMode ? View.VISIBLE : View.GONE);
        sectionTimer.setVisibility(isTaskMode ? View.GONE : View.VISIBLE);
    }

    private void saveConfigAndFinish() {
        WidgetConfig config = WidgetPrefs.load(this, appWidgetId);
        config.mode = radioTimer.isChecked() ? WidgetConfig.MODE_TIMER : WidgetConfig.MODE_TASKS;

        config.taskLimit = Integer.parseInt((String) spinnerTaskLimit.getSelectedItem());
        config.includeCompleted = cbIncludeCompleted.isChecked();
        config.showDeadline = cbShowDeadline.isChecked();
        config.theme = spinnerTheme.getSelectedItemPosition();

        int timerMinutes;
        int timerPreset = spinnerTimerPreset.getSelectedItemPosition();
        if (timerPreset == 0) {
            timerMinutes = 25;
        } else if (timerPreset == 1) {
            timerMinutes = 50;
        } else {
            String input = etCustomMinutes.getText() != null ? etCustomMinutes.getText().toString().trim() : "";
            try {
                timerMinutes = Integer.parseInt(input);
            } catch (NumberFormatException e) {
                timerMinutes = 25;
            }
            timerMinutes = Math.max(1, Math.min(180, timerMinutes));
        }

        boolean timerDurationChanged = timerMinutes != config.timerDurationMinutes;
        config.timerDurationMinutes = timerMinutes;
        if (!config.timerRunning && (timerDurationChanged || config.timerRemainingMs <= 0)) {
            config.timerRemainingMs = config.getTimerDurationMs();
            config.timerEndElapsedRealtime = 0L;
        }

        WidgetPrefs.save(this, config);
        TaskWidgetProvider.refreshWidget(this, appWidgetId);

        Intent resultValue = new Intent();
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        setResult(Activity.RESULT_OK, resultValue);
        finish();
    }
}

