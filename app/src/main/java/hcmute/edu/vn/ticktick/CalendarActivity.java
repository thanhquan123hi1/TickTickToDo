package hcmute.edu.vn.ticktick;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import hcmute.edu.vn.ticktick.adapter.TaskAdapter;
import hcmute.edu.vn.ticktick.calendar.CalendarMonthAdapter;
import hcmute.edu.vn.ticktick.database.Task;
import hcmute.edu.vn.ticktick.ui.DateUtils;
import hcmute.edu.vn.ticktick.ui.TaskDetailBottomSheet;
import hcmute.edu.vn.ticktick.ui.TaskViewModel;

public class CalendarActivity extends BaseActivity {

    private RecyclerView recyclerMonthDays;
    private RecyclerView recyclerTasks;
    private TextView tvSelectedDate;
    private TextView tvMonthLabel;
    private TextView tvEmpty;
    private FloatingActionButton fabAddTask;
    private ImageButton btnPrevMonth;
    private ImageButton btnNextMonth;
    private MaterialToolbar toolbar;

    private TaskViewModel viewModel;
    private TaskAdapter taskAdapter;
    private CalendarMonthAdapter monthAdapter;

    private long selectedDateMillis;
    private Calendar currentMonth;
    private final Set<Long> daysWithTasks = new HashSet<>();

    private LiveData<List<Task>> currentDateSource;
    private LiveData<List<Long>> monthDaysSource;

    private static final SimpleDateFormat DISPLAY_FORMAT =
            new SimpleDateFormat("EEEE, dd/MM/yyyy", Locale.of("vi", "VN"));
    private static final SimpleDateFormat MONTH_LABEL_FORMAT =
            new SimpleDateFormat("MMMM yyyy", Locale.of("vi", "VN"));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar);

        viewModel = new ViewModelProvider(this).get(TaskViewModel.class);

        toolbar = findViewById(R.id.toolbar_calendar);
        recyclerMonthDays = findViewById(R.id.recycler_calendar_days);
        recyclerTasks = findViewById(R.id.recycler_calendar_tasks);
        tvSelectedDate = findViewById(R.id.tv_selected_date);
        tvMonthLabel = findViewById(R.id.tv_month_label);
        tvEmpty = findViewById(R.id.tv_calendar_empty);
        fabAddTask = findViewById(R.id.fab_add_task_calendar);
        btnPrevMonth = findViewById(R.id.btn_prev_month);
        btnNextMonth = findViewById(R.id.btn_next_month);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        setupTaskList();
        setupMonthGrid();

        selectedDateMillis = DateUtils.getStartOfDay(System.currentTimeMillis());
        currentMonth = Calendar.getInstance();
        currentMonth.setTimeInMillis(selectedDateMillis);
        currentMonth.set(Calendar.DAY_OF_MONTH, 1);

        updateSelectedDateLabel(selectedDateMillis);
        loadTasksForDate(selectedDateMillis);
        bindMonthNavigation();
        observeMonthDots();
        renderMonth();

        fabAddTask.setOnClickListener(v -> {
            TaskDetailBottomSheet bottomSheet = TaskDetailBottomSheet.newTask(selectedDateMillis, null);
            bottomSheet.show(getSupportFragmentManager(), "TASK_DETAIL");
        });
    }

    private void setupTaskList() {
        taskAdapter = new TaskAdapter();
        recyclerTasks.setLayoutManager(new LinearLayoutManager(this));
        recyclerTasks.setAdapter(taskAdapter);

        taskAdapter.setOnTaskClickListener(task -> {
            TaskDetailBottomSheet bottomSheet = TaskDetailBottomSheet.newInstance(task);
            bottomSheet.show(getSupportFragmentManager(), "TASK_DETAIL");
        });

        taskAdapter.setOnTaskCheckListener((task, isChecked) -> {
            task.setCompleted(isChecked);
            viewModel.updateTask(task);
        });
    }

    private void setupMonthGrid() {
        monthAdapter = new CalendarMonthAdapter(this::onCalendarDaySelected);
        recyclerMonthDays.setLayoutManager(new GridLayoutManager(this, 7));
        recyclerMonthDays.setAdapter(monthAdapter);
    }

    private void bindMonthNavigation() {
        btnPrevMonth.setOnClickListener(v -> {
            currentMonth.add(Calendar.MONTH, -1);
            observeMonthDots();
            renderMonth();
        });
        btnNextMonth.setOnClickListener(v -> {
            currentMonth.add(Calendar.MONTH, 1);
            observeMonthDots();
            renderMonth();
        });
    }

    private void observeMonthDots() {
        if (monthDaysSource != null) {
            monthDaysSource.removeObservers(this);
        }

        long monthStart = getMonthStartMillis(currentMonth);
        long monthEndExclusive = getNextMonthStartMillis(currentMonth);
        monthDaysSource = viewModel.getTaskDueDatesInRange(monthStart, monthEndExclusive);
        monthDaysSource.observe(this, dueDates -> {
            daysWithTasks.clear();
            if (dueDates != null) {
                for (Long dueDate : dueDates) {
                    if (dueDate != null && dueDate > 0) {
                        daysWithTasks.add(DateUtils.getStartOfDay(dueDate));
                    }
                }
            }
            renderMonth();
        });
    }

    private void onCalendarDaySelected(CalendarMonthAdapter.DayCell dayCell) {
        if (dayCell == null) {
            return;
        }

        selectedDateMillis = dayCell.dayStartMillis;

        Calendar selectedCal = Calendar.getInstance();
        selectedCal.setTimeInMillis(selectedDateMillis);
        boolean monthChanged = selectedCal.get(Calendar.YEAR) != currentMonth.get(Calendar.YEAR)
                || selectedCal.get(Calendar.MONTH) != currentMonth.get(Calendar.MONTH);

        if (monthChanged) {
            currentMonth.set(Calendar.YEAR, selectedCal.get(Calendar.YEAR));
            currentMonth.set(Calendar.MONTH, selectedCal.get(Calendar.MONTH));
            currentMonth.set(Calendar.DAY_OF_MONTH, 1);
            observeMonthDots();
        }

        updateSelectedDateLabel(selectedDateMillis);
        loadTasksForDate(selectedDateMillis);
        renderMonth();
    }

    private void renderMonth() {
        if (currentMonth == null) {
            return;
        }

        tvMonthLabel.setText(capitalizeMonth(MONTH_LABEL_FORMAT.format(new Date(currentMonth.getTimeInMillis()))));
        monthAdapter.submit(buildDayCells());
    }

    private List<CalendarMonthAdapter.DayCell> buildDayCells() {
        List<CalendarMonthAdapter.DayCell> cells = new ArrayList<>(42);

        Calendar firstOfMonth = Calendar.getInstance();
        firstOfMonth.setTimeInMillis(currentMonth.getTimeInMillis());
        firstOfMonth.set(Calendar.DAY_OF_MONTH, 1);

        int dayOfWeek = firstOfMonth.get(Calendar.DAY_OF_WEEK);
        int leading = toMondayFirstOffset(dayOfWeek);

        Calendar gridStart = Calendar.getInstance();
        gridStart.setTimeInMillis(firstOfMonth.getTimeInMillis());
        gridStart.add(Calendar.DAY_OF_MONTH, -leading);

        Calendar today = Calendar.getInstance();
        long selectedDayStart = DateUtils.getStartOfDay(selectedDateMillis);

        for (int i = 0; i < 42; i++) {
            long dayStart = DateUtils.getStartOfDay(gridStart.getTimeInMillis());
            boolean inCurrentMonth = gridStart.get(Calendar.MONTH) == currentMonth.get(Calendar.MONTH)
                    && gridStart.get(Calendar.YEAR) == currentMonth.get(Calendar.YEAR);
            boolean selected = dayStart == selectedDayStart;
            boolean isToday = DateUtils.isSameDay(gridStart, today);
            boolean hasTask = daysWithTasks.contains(dayStart);

            cells.add(new CalendarMonthAdapter.DayCell(
                    dayStart,
                    gridStart.get(Calendar.DAY_OF_MONTH),
                    inCurrentMonth,
                    selected,
                    isToday,
                    hasTask
            ));
            gridStart.add(Calendar.DAY_OF_MONTH, 1);
        }

        return cells;
    }

    private int toMondayFirstOffset(int javaDayOfWeek) {
        int mondayBased = javaDayOfWeek - Calendar.MONDAY;
        if (mondayBased < 0) {
            mondayBased += 7;
        }
        return mondayBased;
    }

    private long getMonthStartMillis(Calendar month) {
        Calendar start = Calendar.getInstance();
        start.setTimeInMillis(month.getTimeInMillis());
        start.set(Calendar.DAY_OF_MONTH, 1);
        return DateUtils.getStartOfDay(start.getTimeInMillis());
    }

    private long getNextMonthStartMillis(Calendar month) {
        Calendar end = Calendar.getInstance();
        end.setTimeInMillis(getMonthStartMillis(month));
        end.add(Calendar.MONTH, 1);
        return end.getTimeInMillis();
    }

    private String capitalizeMonth(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        return value.substring(0, 1).toUpperCase(Locale.getDefault()) + value.substring(1);
    }

    private void updateSelectedDateLabel(long dateMillis) {
        tvSelectedDate.setText(DISPLAY_FORMAT.format(new Date(dateMillis)));
    }

    private void loadTasksForDate(long dateMillis) {
        long startOfDay = DateUtils.getStartOfDay(dateMillis);
        long endOfDay = DateUtils.getStartOfNextDay(dateMillis);

        if (currentDateSource != null) {
            currentDateSource.removeObservers(this);
        }

        currentDateSource = viewModel.getTasksForDate(startOfDay, endOfDay);
        currentDateSource.observe(this, tasks -> {
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
}
