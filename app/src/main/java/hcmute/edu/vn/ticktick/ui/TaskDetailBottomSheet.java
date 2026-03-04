package hcmute.edu.vn.ticktick.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import hcmute.edu.vn.ticktick.R;
import hcmute.edu.vn.ticktick.database.AppDatabase;
import hcmute.edu.vn.ticktick.database.Category;
import hcmute.edu.vn.ticktick.database.Task;

public class TaskDetailBottomSheet extends BottomSheetDialogFragment {

    private TextInputEditText etTitle, etDescription;
    private MaterialButton btnSelectDate, btnSelectTime, btnSave, btnDelete, btnCancel;
    private Spinner spinnerCategory, spinnerPriority;

    private Task editingTask;
    private long selectedDueDate = 0;
    private String selectedDueTime = null;
    private List<Category> categories = new ArrayList<>();
    private OnTaskSavedListener savedListener;

    public interface OnTaskSavedListener {
        void onTaskSaved();
    }

    public void setOnTaskSavedListener(OnTaskSavedListener listener) {
        this.savedListener = listener;
    }

    public static TaskDetailBottomSheet newInstance(@Nullable Task task) {
        TaskDetailBottomSheet fragment = new TaskDetailBottomSheet();
        fragment.editingTask = task;
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_task_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Bind views
        etTitle = view.findViewById(R.id.et_task_title);
        etDescription = view.findViewById(R.id.et_task_description);
        btnSelectDate = view.findViewById(R.id.btn_select_date);
        btnSelectTime = view.findViewById(R.id.btn_select_time);
        btnSave = view.findViewById(R.id.btn_save_task);
        btnDelete = view.findViewById(R.id.btn_delete_task);
        btnCancel = view.findViewById(R.id.btn_cancel);
        spinnerCategory = view.findViewById(R.id.spinner_category);
        spinnerPriority = view.findViewById(R.id.spinner_priority);

        setupPrioritySpinner();
        loadCategories();
        setupDatePicker();
        setupTimePicker();
        setupButtons();

        // If editing existing task, populate fields
        if (editingTask != null) {
            populateFields();
        }
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
    }

    private void loadCategories() {
        AppDatabase db = AppDatabase.getDatabase(requireContext());
        AppDatabase.databaseWriteExecutor.execute(() -> {
            categories = db.categoryDao().getAllCategoriesSync();
            requireActivity().runOnUiThread(() -> {
                List<String> categoryNames = new ArrayList<>();
                categoryNames.add(getString(R.string.nav_inbox)); // "Hộp thư đến" as default
                for (Category cat : categories) {
                    categoryNames.add(cat.getName());
                }
                ArrayAdapter<String> adapter = new ArrayAdapter<>(
                        requireContext(), android.R.layout.simple_spinner_item, categoryNames);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerCategory.setAdapter(adapter);

                // Set selection if editing
                if (editingTask != null && editingTask.getCategoryId() != null) {
                    for (int i = 0; i < categories.size(); i++) {
                        if (categories.get(i).getId() == editingTask.getCategoryId()) {
                            spinnerCategory.setSelection(i + 1); // +1 for "Hộp thư đến"
                            break;
                        }
                    }
                }
            });
        });
    }

    private void setupDatePicker() {
        btnSelectDate.setOnClickListener(v -> {
            MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                    .setTitleText(getString(R.string.select_date))
                    .setSelection(selectedDueDate > 0 ? selectedDueDate : MaterialDatePicker.todayInUtcMilliseconds())
                    .build();

            datePicker.addOnPositiveButtonClickListener(selection -> {
                selectedDueDate = selection;
                btnSelectDate.setText(DateUtils.formatDate(selectedDueDate));
            });

            datePicker.show(getParentFragmentManager(), "DATE_PICKER");
        });
    }

    private void setupTimePicker() {
        btnSelectTime.setOnClickListener(v -> {
            Calendar now = Calendar.getInstance();
            MaterialTimePicker timePicker = new MaterialTimePicker.Builder()
                    .setTimeFormat(TimeFormat.CLOCK_24H)
                    .setHour(now.get(Calendar.HOUR_OF_DAY))
                    .setMinute(now.get(Calendar.MINUTE))
                    .setTitleText(getString(R.string.select_time))
                    .build();

            timePicker.addOnPositiveButtonClickListener(v1 -> {
                selectedDueTime = String.format(Locale.getDefault(), "%02d:%02d",
                        timePicker.getHour(), timePicker.getMinute());
                btnSelectTime.setText(selectedDueTime);
            });

            timePicker.show(getParentFragmentManager(), "TIME_PICKER");
        });
    }

    private void setupButtons() {
        btnSave.setOnClickListener(v -> saveTask());
        btnCancel.setOnClickListener(v -> dismiss());

        if (editingTask != null) {
            btnDelete.setVisibility(View.VISIBLE);
            btnDelete.setOnClickListener(v -> deleteTask());
        }
    }

    private void populateFields() {
        etTitle.setText(editingTask.getTitle());
        etDescription.setText(editingTask.getDescription());

        if (editingTask.getDueDate() > 0) {
            selectedDueDate = editingTask.getDueDate();
            btnSelectDate.setText(DateUtils.formatDate(selectedDueDate));
        }

        if (editingTask.getDueTime() != null && !editingTask.getDueTime().isEmpty()) {
            selectedDueTime = editingTask.getDueTime();
            btnSelectTime.setText(selectedDueTime);
        }

        spinnerPriority.setSelection(editingTask.getPriority());
    }

    private void saveTask() {
        String title = etTitle.getText() != null ? etTitle.getText().toString().trim() : "";
        if (title.isEmpty()) {
            etTitle.setError("Vui lòng nhập tiêu đề");
            return;
        }

        String description = etDescription.getText() != null ? etDescription.getText().toString().trim() : "";

        AppDatabase db = AppDatabase.getDatabase(requireContext());

        AppDatabase.databaseWriteExecutor.execute(() -> {
            Task task;
            if (editingTask != null) {
                task = editingTask;
            } else {
                task = new Task();
            }

            task.setTitle(title);
            task.setDescription(description);
            task.setDueDate(selectedDueDate);
            task.setDueTime(selectedDueTime);
            task.setPriority(spinnerPriority.getSelectedItemPosition());

            // Category
            int categoryPosition = spinnerCategory.getSelectedItemPosition();
            if (categoryPosition > 0 && categoryPosition <= categories.size()) {
                task.setCategoryId(categories.get(categoryPosition - 1).getId());
            } else {
                task.setCategoryId(null);
            }

            if (editingTask != null) {
                db.taskDao().update(task);
            } else {
                db.taskDao().insert(task);
            }

            requireActivity().runOnUiThread(() -> {
                String msg = editingTask != null ?
                        getString(R.string.msg_task_updated) :
                        getString(R.string.msg_task_added);
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();

                if (savedListener != null) {
                    savedListener.onTaskSaved();
                }
                dismiss();
            });
        });
    }

    private void deleteTask() {
        if (editingTask == null) return;

        AppDatabase db = AppDatabase.getDatabase(requireContext());
        AppDatabase.databaseWriteExecutor.execute(() -> {
            db.taskDao().delete(editingTask);
            requireActivity().runOnUiThread(() -> {
                Toast.makeText(requireContext(), getString(R.string.msg_task_deleted), Toast.LENGTH_SHORT).show();
                if (savedListener != null) {
                    savedListener.onTaskSaved();
                }
                dismiss();
            });
        });
    }
}
