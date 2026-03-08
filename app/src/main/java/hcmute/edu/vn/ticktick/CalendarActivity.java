package hcmute.edu.vn.ticktick;

import android.os.Bundle;
import android.view.View;
import android.widget.CalendarView;
import android.widget.TextView;

import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import hcmute.edu.vn.ticktick.adapter.TaskAdapter;
import hcmute.edu.vn.ticktick.database.Task;
import hcmute.edu.vn.ticktick.ui.TaskDetailBottomSheet;
import hcmute.edu.vn.ticktick.ui.TaskViewModel;

public class CalendarActivity extends BaseActivity {

    private CalendarView calendarView;
    private RecyclerView recyclerTasks;
    private TextView tvSelectedDate, tvEmpty;
    private FloatingActionButton fabAddTask;
    private MaterialToolbar toolbar;

    private TaskViewModel viewModel;
    private TaskAdapter taskAdapter;

    private long selectedDateMillis;

    private static final SimpleDateFormat DISPLAY_FORMAT =
            new SimpleDateFormat("EEEE, dd/MM/yyyy", Locale.of("vi", "VN"));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar);

        viewModel = new ViewModelProvider(this).get(TaskViewModel.class);

        // Bind views
        toolbar = findViewById(R.id.toolbar_calendar);
        calendarView = findViewById(R.id.calendar_view);
        recyclerTasks = findViewById(R.id.recycler_calendar_tasks);
        tvSelectedDate = findViewById(R.id.tv_selected_date);
        tvEmpty = findViewById(R.id.tv_calendar_empty);
        fabAddTask = findViewById(R.id.fab_add_task_calendar);

        // Setup toolbar with back button
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        // Setup RecyclerView
        taskAdapter = new TaskAdapter();
        recyclerTasks.setLayoutManager(new LinearLayoutManager(this));
        recyclerTasks.setAdapter(taskAdapter);

        taskAdapter.setOnTaskClickListener(task -> {
            TaskDetailBottomSheet bottomSheet = TaskDetailBottomSheet.newInstance(task);
            bottomSheet.setOnTaskSavedListener(() -> loadTasksForDate(selectedDateMillis));
            bottomSheet.show(getSupportFragmentManager(), "TASK_DETAIL");
        });

        taskAdapter.setOnTaskCheckListener((task, isChecked) -> {
            task.setCompleted(isChecked);
            viewModel.updateTask(task);
        });

        // FAB -> add new task with pre-selected date
        fabAddTask.setOnClickListener(v -> {
            Task newTask = new Task();
            newTask.setDueDate(selectedDateMillis);
            TaskDetailBottomSheet bottomSheet = TaskDetailBottomSheet.newInstance(newTask);
            bottomSheet.setOnTaskSavedListener(() -> loadTasksForDate(selectedDateMillis));
            bottomSheet.show(getSupportFragmentManager(), "TASK_DETAIL");
        });

        // Set initial date to today
        selectedDateMillis = getStartOfDay(System.currentTimeMillis());
        updateSelectedDateLabel(selectedDateMillis);
        loadTasksForDate(selectedDateMillis);

        // Calendar date change listener
        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            Calendar cal = Calendar.getInstance();
            cal.set(year, month, dayOfMonth, 0, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            selectedDateMillis = cal.getTimeInMillis();
            updateSelectedDateLabel(selectedDateMillis);
            loadTasksForDate(selectedDateMillis);
        });
    }

    private void updateSelectedDateLabel(long dateMillis) {
        tvSelectedDate.setText(DISPLAY_FORMAT.format(new Date(dateMillis)));
    }

    private void loadTasksForDate(long dateMillis) {
        long startOfDay = getStartOfDay(dateMillis);
        long endOfDay = startOfDay + 24 * 60 * 60 * 1000;

        // Remove previous observers
        viewModel.getTasksForDate(startOfDay, endOfDay).removeObservers(this);

        viewModel.getTasksForDate(startOfDay, endOfDay).observe(this, tasks -> {
            if (tasks != null && !tasks.isEmpty()) {
                String dateStr = DISPLAY_FORMAT.format(new Date(dateMillis));
                taskAdapter.setFlatData(dateStr, tasks);
                recyclerTasks.setVisibility(View.VISIBLE);
                tvEmpty.setVisibility(View.GONE);
            } else {
                taskAdapter.setFlatData("", null);
                recyclerTasks.setVisibility(View.GONE);
                tvEmpty.setVisibility(View.VISIBLE);
            }
        });
    }

    private long getStartOfDay(long timeMillis) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(timeMillis);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }
}
