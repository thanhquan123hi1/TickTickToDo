package hcmute.edu.vn.ticktick.schoolcalendar;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import hcmute.edu.vn.ticktick.R;
import hcmute.edu.vn.ticktick.database.AppDatabase;
import hcmute.edu.vn.ticktick.database.Category;
import hcmute.edu.vn.ticktick.database.SchoolCalendarEventEntity;
import hcmute.edu.vn.ticktick.database.Task;
import hcmute.edu.vn.ticktick.ui.DateUtils;
import hcmute.edu.vn.ticktick.ui.TaskDetailBottomSheet;

public class SchoolEventDetailBottomSheet extends BottomSheetDialogFragment {

    private TextInputEditText etTitle;
    private TextInputEditText etDescription;
    private MaterialButton btnSelectDate;
    private MaterialButton btnSelectTime;
    private MaterialButton btnSave;
    private MaterialButton btnDelete;
    private MaterialButton btnExport;
    private Spinner spinnerCategory;
    private Spinner spinnerPriority;
    private TextView tvSheetHeading;
    private TextView tvSheetSubtitle;
    private TextView tvTaskStatus;

    private SchoolCalendarEventEntity eventEntity;
    private Task linkedTask;
    private SchoolCalendarViewModel schoolCalendarViewModel;
    private List<Category> categories = new ArrayList<>();
    private long selectedReferenceTime;

    public static SchoolEventDetailBottomSheet newInstance(SchoolCalendarEventEntity event) {
        SchoolEventDetailBottomSheet fragment = new SchoolEventDetailBottomSheet();
        fragment.eventEntity = event;
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_school_event_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        schoolCalendarViewModel = new ViewModelProvider(requireActivity()).get(SchoolCalendarViewModel.class);

        tvSheetHeading = view.findViewById(R.id.tv_sheet_heading);
        tvSheetSubtitle = view.findViewById(R.id.tv_sheet_subtitle);
        tvTaskStatus = view.findViewById(R.id.tv_task_status);
        etTitle = view.findViewById(R.id.et_task_title);
        etDescription = view.findViewById(R.id.et_task_description);
        btnSelectDate = view.findViewById(R.id.btn_select_date);
        btnSelectTime = view.findViewById(R.id.btn_select_time);
        btnSave = view.findViewById(R.id.btn_save_task);
        btnDelete = view.findViewById(R.id.btn_delete_task);
        btnExport = view.findViewById(R.id.btn_export_task);
        spinnerCategory = view.findViewById(R.id.spinner_category);
        spinnerPriority = view.findViewById(R.id.spinner_priority);

        setupPrioritySpinner();
        setupDatePicker();
        setupTimePicker();
        setupActions();

        populateEventData();
        loadCategories();
        refreshLinkedTaskState();
    }

    private void setupPrioritySpinner() {
        String[] priorities = {
                getString(R.string.priority_none),
                getString(R.string.priority_low),
                getString(R.string.priority_medium),
                getString(R.string.priority_high)
        };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_spinner_item, priorities);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPriority.setAdapter(adapter);
        spinnerPriority.setEnabled(false);
        spinnerPriority.setSelection(0);
    }

    private void setupDatePicker() {
        btnSelectDate.setOnClickListener(v -> {
            MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                    .setTitleText(getString(R.string.select_date))
                    .setSelection(selectedReferenceTime > 0
                            ? selectedReferenceTime
                            : MaterialDatePicker.todayInUtcMilliseconds())
                    .build();

            datePicker.addOnPositiveButtonClickListener(selection -> {
                if (selection == null) {
                    return;
                }
                LocalDateTime selectedDate = LocalDateTime.ofInstant(
                        Instant.ofEpochMilli(selection),
                        ZoneId.systemDefault());
                LocalDateTime baseDateTime = getSelectedDateTime();
                LocalDateTime updated = baseDateTime
                        .withYear(selectedDate.getYear())
                        .withMonth(selectedDate.getMonthValue())
                        .withDayOfMonth(selectedDate.getDayOfMonth());
                selectedReferenceTime = updated.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
                updateDateTimeButtons();
            });

            datePicker.show(getParentFragmentManager(), "SCHOOL_EVENT_DATE_PICKER");
        });
    }

    private void setupTimePicker() {
        btnSelectTime.setOnClickListener(v -> {
            LocalDateTime current = getSelectedDateTime();
            MaterialTimePicker timePicker = new MaterialTimePicker.Builder()
                    .setTimeFormat(TimeFormat.CLOCK_24H)
                    .setHour(current.getHour())
                    .setMinute(current.getMinute())
                    .setTitleText(getString(R.string.select_time))
                    .build();

            timePicker.addOnPositiveButtonClickListener(v1 -> {
                LocalDateTime updated = current
                        .withHour(timePicker.getHour())
                        .withMinute(timePicker.getMinute())
                        .withSecond(0)
                        .withNano(0);
                selectedReferenceTime = updated.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
                updateDateTimeButtons();
            });

            timePicker.show(getParentFragmentManager(), "SCHOOL_EVENT_TIME_PICKER");
        });
    }

    private void setupActions() {
        btnSave.setOnClickListener(v -> saveChanges());
        btnDelete.setOnClickListener(v -> confirmDelete());
    }

    private void populateEventData() {
        tvSheetHeading.setText(R.string.school_event_detail_title);
        tvSheetSubtitle.setText(R.string.school_event_detail_subtitle);

        etTitle.setText(eventEntity.getTitle());
        etDescription.setText(eventEntity.getDescription());

        selectedReferenceTime = resolveReferenceTime(eventEntity);
        updateDateTimeButtons();

        btnDelete.setVisibility(View.VISIBLE);
    }

    private void updateDateTimeButtons() {
        if (selectedReferenceTime > 0) {
            btnSelectDate.setText(DateUtils.formatDate(selectedReferenceTime));
            LocalDateTime dt = LocalDateTime.ofInstant(Instant.ofEpochMilli(selectedReferenceTime), ZoneId.systemDefault());
            btnSelectTime.setText(String.format(Locale.getDefault(), "%02d:%02d", dt.getHour(), dt.getMinute()));
        } else {
            btnSelectDate.setText(R.string.school_calendar_time_unknown);
            btnSelectTime.setText("--:--");
        }
    }

    private void loadCategories() {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            List<Category> loaded = AppDatabase.getDatabase(requireContext()).categoryDao().getAllCategoriesSync();
            if (!isAdded()) {
                return;
            }
            requireActivity().runOnUiThread(() -> {
                categories = loaded;
                List<String> categoryNames = new ArrayList<>();
                categoryNames.add(getString(R.string.filter_unscheduled));
                for (Category cat : categories) {
                    categoryNames.add(cat.getName());
                }
                ArrayAdapter<String> adapter = new ArrayAdapter<>(
                        requireContext(), android.R.layout.simple_spinner_item, categoryNames);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerCategory.setAdapter(adapter);
                spinnerCategory.setSelection(findPreferredCategoryPosition());
            });
        });
    }

    private int findPreferredCategoryPosition() {
        for (int i = 0; i < categories.size(); i++) {
            String lower = categories.get(i).getName() == null
                    ? ""
                    : categories.get(i).getName().toLowerCase(Locale.ROOT);
            if (lower.contains("study") || lower.contains("học") || lower.contains("school")) {
                return i + 1;
            }
        }
        return 0;
    }

    private void refreshLinkedTaskState() {
        schoolCalendarViewModel.getLinkedTask(eventEntity.getUid(), task -> {
            if (!isAdded()) {
                return;
            }
            requireActivity().runOnUiThread(() -> {
                linkedTask = task;
                updateStatusChip();
                updateExportButton();
            });
        });
    }

    private void updateStatusChip() {
        if (linkedTask != null) {
            tvTaskStatus.setText(R.string.school_event_status_exported);
            tvTaskStatus.setBackgroundResource(R.drawable.bg_status_chip_completed);
        } else {
            tvTaskStatus.setText(R.string.school_event_status_not_exported);
            tvTaskStatus.setBackgroundResource(R.drawable.bg_status_chip_pending);
        }
    }

    private void updateExportButton() {
        if (linkedTask != null) {
            btnExport.setText(R.string.btn_open_task);
            btnExport.setOnClickListener(v -> openLinkedTask());
            return;
        }

        btnExport.setText(R.string.btn_export_task);
        btnExport.setOnClickListener(v -> exportToNormalTask());
    }

    private void exportToNormalTask() {
        schoolCalendarViewModel.exportEventToTask(eventEntity, result -> {
            if (!isAdded()) {
                return;
            }
            requireActivity().runOnUiThread(() -> {
                linkedTask = result.getTask();
                updateStatusChip();
                updateExportButton();
                int messageRes = result.isCreated()
                        ? R.string.msg_exported_successfully
                        : R.string.msg_exported_already_exists;
                Toast.makeText(requireContext(), messageRes, Toast.LENGTH_SHORT).show();
            });
        });
    }

    private void openLinkedTask() {
        if (linkedTask == null) {
            return;
        }
        dismiss();
        TaskDetailBottomSheet sheet = TaskDetailBottomSheet.newInstance(linkedTask);
        sheet.show(getParentFragmentManager(), "TASK_DETAIL");
    }

    private void saveChanges() {
        String title = etTitle.getText() == null ? "" : etTitle.getText().toString().trim();
        if (TextUtils.isEmpty(title)) {
            etTitle.setError(getString(R.string.error_task_title_required));
            return;
        }

        eventEntity.setTitle(title);
        eventEntity.setDescription(etDescription.getText() == null ? "" : etDescription.getText().toString().trim());
        applySelectedTimeToEvent();
        eventEntity.setLastModifiedMillis(System.currentTimeMillis());

        schoolCalendarViewModel.updateSchoolEvent(eventEntity, () -> {
            if (!isAdded()) {
                return;
            }
            requireActivity().runOnUiThread(() -> {
                Toast.makeText(requireContext(), R.string.school_event_saved, Toast.LENGTH_SHORT).show();
                refreshLinkedTaskState();
            });
        });
    }

    private void confirmDelete() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.school_event_delete_title)
                .setMessage(R.string.school_event_delete_message)
                .setPositiveButton(R.string.btn_delete, (dialog, which) -> deleteEvent())
                .setNegativeButton(R.string.btn_cancel, null)
                .show();
    }

    private void deleteEvent() {
        schoolCalendarViewModel.deleteSchoolEvent(eventEntity, () -> {
            if (!isAdded()) {
                return;
            }
            requireActivity().runOnUiThread(() -> {
                Toast.makeText(requireContext(), R.string.school_event_deleted, Toast.LENGTH_SHORT).show();
                dismiss();
            });
        });
    }

    private void applySelectedTimeToEvent() {
        if (selectedReferenceTime <= 0) {
            eventEntity.setReferenceTimeMillis(0L);
            return;
        }

        long oldStart = eventEntity.getStartTimeMillis();
        long oldEnd = eventEntity.getEndTimeMillis();
        long duration = oldEnd > oldStart ? oldEnd - oldStart : 0L;

        eventEntity.setReferenceTimeMillis(selectedReferenceTime);
        eventEntity.setStartTimeMillis(selectedReferenceTime);
        eventEntity.setEndTimeMillis(duration > 0 ? selectedReferenceTime + duration : selectedReferenceTime);
    }

    private long resolveReferenceTime(SchoolCalendarEventEntity event) {
        if (event.getReferenceTimeMillis() > 0) {
            return event.getReferenceTimeMillis();
        }
        if (event.getStartTimeMillis() > 0) {
            return event.getStartTimeMillis();
        }
        Calendar now = Calendar.getInstance();
        now.set(Calendar.SECOND, 0);
        now.set(Calendar.MILLISECOND, 0);
        return now.getTimeInMillis();
    }

    private LocalDateTime getSelectedDateTime() {
        long base = selectedReferenceTime > 0 ? selectedReferenceTime : System.currentTimeMillis();
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(base), ZoneId.systemDefault());
    }
}
