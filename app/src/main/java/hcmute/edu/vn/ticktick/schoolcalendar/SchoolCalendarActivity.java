package hcmute.edu.vn.ticktick.schoolcalendar;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.WorkInfo;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import hcmute.edu.vn.ticktick.BaseActivity;
import hcmute.edu.vn.ticktick.R;
import hcmute.edu.vn.ticktick.database.SchoolCalendarEventEntity;
import hcmute.edu.vn.ticktick.database.Task;
import hcmute.edu.vn.ticktick.ui.DateUtils;
import hcmute.edu.vn.ticktick.ui.TaskDetailBottomSheet;

public class SchoolCalendarActivity extends BaseActivity {

    private static final Locale VIETNAM_LOCALE = new Locale("vi", "VN");
    private static final SimpleDateFormat SYNC_TIME_FORMAT =
            new SimpleDateFormat("dd/MM/yyyy HH:mm", VIETNAM_LOCALE);

    private MaterialToolbar toolbar;
    private TextView tvSyncStatus;
    private RecyclerView recyclerEvents;
    private TextView tvStateMessage;
    private ProgressBar progressBar;

    private SchoolCalendarViewModel viewModel;
    private SchoolCalendarAdapter adapter;

    private List<SchoolCalendarEventEntity> latestEvents = new ArrayList<>();
    private SchoolCalendarViewModel.SyncMeta latestSyncMeta;
    private boolean isSyncRunning;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_school_calendar);

        bindViews();
        setupToolbar();
        setupList();

        viewModel = new ViewModelProvider(this).get(SchoolCalendarViewModel.class);
        observeData();

        viewModel.ensurePeriodicSync();
        viewModel.syncIfStale();
    }

    @Override
    protected void onResume() {
        super.onResume();
        viewModel.refreshSyncMeta();
    }

    private void bindViews() {
        toolbar = findViewById(R.id.toolbar_school_calendar);
        tvSyncStatus = findViewById(R.id.tv_school_sync_status);
        recyclerEvents = findViewById(R.id.recycler_school_events);
        tvStateMessage = findViewById(R.id.tv_school_state_message);
        progressBar = findViewById(R.id.progress_school_loading);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupList() {
        adapter = new SchoolCalendarAdapter();
        adapter.setOnItemClickListener(event -> {
            viewModel.getLinkedTask(event.getUid(), linkedTask -> {
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }
                    if (linkedTask != null) {
                        TaskDetailBottomSheet.newInstance(linkedTask)
                                .show(getSupportFragmentManager(), "TASK_DETAIL");
                        return;
                    }
                    SchoolEventDetailBottomSheet.newInstance(event)
                            .show(getSupportFragmentManager(), "SCHOOL_EVENT_DETAIL");
                });
            });
        });
        recyclerEvents.setLayoutManager(new LinearLayoutManager(this));
        recyclerEvents.setAdapter(adapter);
    }

    private void observeData() {
        viewModel.getEvents().observe(this, events -> {
            latestEvents = events == null ? new ArrayList<>() : events;
            render();
        });

        viewModel.getSyncMeta().observe(this, syncMeta -> {
            latestSyncMeta = syncMeta;
            render();
        });

        viewModel.getOneTimeWorkInfo().observe(this, workInfos -> {
            isSyncRunning = false;
            if (workInfos != null) {
                for (WorkInfo info : workInfos) {
                    if (info.getState() == WorkInfo.State.RUNNING || info.getState() == WorkInfo.State.ENQUEUED) {
                        isSyncRunning = true;
                        break;
                    }
                }
            }
            viewModel.refreshSyncMeta();
            render();
        });
    }

    private void render() {
        GroupedEvents grouped = groupEvents(latestEvents);
        adapter.setGroupedData(grouped.overdue, grouped.today, grouped.nextThreeDays);

        boolean hasData = !adapter.isEmpty();
        boolean hasError = latestSyncMeta != null && !TextUtils.isEmpty(latestSyncMeta.lastError);

        progressBar.setVisibility(isSyncRunning && !hasData ? View.VISIBLE : View.GONE);
        recyclerEvents.setVisibility(hasData ? View.VISIBLE : View.GONE);

        if (!hasData) {
            tvStateMessage.setVisibility(View.VISIBLE);
            if (isSyncRunning) {
                tvStateMessage.setText(R.string.school_calendar_syncing);
            } else if (hasError) {
                tvStateMessage.setText(getString(R.string.school_calendar_empty_error));
            } else {
                tvStateMessage.setText(R.string.school_calendar_empty);
            }
        } else {
            if (hasError) {
                tvStateMessage.setVisibility(View.VISIBLE);
                tvStateMessage.setText(R.string.school_calendar_showing_cached_data);
            } else {
                tvStateMessage.setVisibility(View.GONE);
            }
        }

        updateSyncStatus(hasError);
    }

    private void updateSyncStatus(boolean hasError) {
        if (isSyncRunning) {
            tvSyncStatus.setText(R.string.school_calendar_syncing);
            return;
        }

        if (latestSyncMeta != null && latestSyncMeta.lastSyncTime > 0) {
            String formatted = SYNC_TIME_FORMAT.format(new Date(latestSyncMeta.lastSyncTime));
            if (hasError) {
                tvSyncStatus.setText(getString(R.string.school_calendar_sync_status_with_warning, formatted) + "\nLỗi: " + latestSyncMeta.lastError);
            } else {
                tvSyncStatus.setText(getString(R.string.school_calendar_last_synced, formatted));
            }
            return;
        }

        if (hasError) {
            tvSyncStatus.setText(getString(R.string.school_calendar_sync_failed_generic) + "\nChi tiết: " + latestSyncMeta.lastError);
        } else {
            tvSyncStatus.setText(R.string.school_calendar_not_synced_yet);
        }
    }

    private GroupedEvents groupEvents(List<SchoolCalendarEventEntity> events) {
        GroupedEvents grouped = new GroupedEvents();
        long now = System.currentTimeMillis();
        long startToday = DateUtils.getStartOfToday();
        long startTomorrow = DateUtils.getStartOfTomorrow();
        // keep endNext3Days as startToday + 4 days, but we will gather all future events
        long endNext3Days = startToday + 4L * 24L * 60L * 60L * 1000L;

        if (events == null) {
            return grouped;
        }

        for (SchoolCalendarEventEntity event : events) {
            long reference = event.getReferenceTimeMillis();
            if (reference <= 0) {
                reference = event.getStartTimeMillis();
            }
            if (reference <= 0) {
                continue;
            }

            if (reference < startToday) {
                grouped.overdue.add(event);
                continue;
            }

            if (reference >= startToday && reference < startTomorrow) {
                grouped.today.add(event);
                continue;
            }

            if (reference >= startTomorrow) {
                grouped.nextThreeDays.add(event);
            }
        }

        return grouped;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_school_calendar, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_school_refresh) {
            viewModel.requestManualSync();
            return true;
        }
        if (itemId == R.id.action_school_settings) {
            showSettingsDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showSettingsDialog() {
        View content = getLayoutInflater().inflate(R.layout.dialog_school_calendar_settings, null, false);
        EditText etUrl = content.findViewById(R.id.et_school_calendar_url);
        etUrl.setText(viewModel.getCurrentSourceUrl());

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.school_calendar_settings_title)
                .setView(content)
                .setPositiveButton(R.string.btn_save, null)
                .setNegativeButton(R.string.btn_cancel, null)
                .setNeutralButton(R.string.school_calendar_restore_default, null);

        androidx.appcompat.app.AlertDialog dialog = builder.create();
        dialog.setOnShowListener(dialogInterface -> {
            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String input = etUrl.getText() == null ? "" : etUrl.getText().toString().trim();
                if (!viewModel.saveSourceUrl(input)) {
                    etUrl.setError(getString(R.string.school_calendar_error_invalid_url));
                    return;
                }
                Toast.makeText(this, R.string.school_calendar_url_saved, Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            });

            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> {
                viewModel.restoreDefaultUrl();
                Toast.makeText(this, R.string.school_calendar_default_restored, Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            });
        });

        dialog.show();
    }

    private static class GroupedEvents {
        final List<SchoolCalendarEventEntity> overdue = new ArrayList<>();
        final List<SchoolCalendarEventEntity> today = new ArrayList<>();
        final List<SchoolCalendarEventEntity> nextThreeDays = new ArrayList<>();
    }
}
