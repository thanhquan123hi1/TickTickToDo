package hcmute.edu.vn.ticktick;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;

import java.util.ArrayList;
import java.util.List;

import hcmute.edu.vn.ticktick.adapter.TaskAdapter;
import hcmute.edu.vn.ticktick.database.Task;
import hcmute.edu.vn.ticktick.ui.TaskDetailBottomSheet;
import hcmute.edu.vn.ticktick.ui.TaskViewModel;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private MaterialToolbar toolbar;
    private RecyclerView recyclerTasks;
    private FloatingActionButton fabAddTask;
    private TextView tvEmpty;
    private View addTaskBar;

    private TaskAdapter taskAdapter;
    private TaskViewModel viewModel;

    // Current view mode
    private enum ViewMode {
        NEXT_7_DAYS, TODAY, INBOX, CATEGORY, THIS_WEEK, UNSCHEDULED, COMPLETED
    }
    private ViewMode currentMode = ViewMode.NEXT_7_DAYS;
    private int currentCategoryId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize ViewModel
        viewModel = new ViewModelProvider(this).get(TaskViewModel.class);

        // Bind views
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        toolbar = findViewById(R.id.toolbar);
        recyclerTasks = findViewById(R.id.recycler_tasks);
        fabAddTask = findViewById(R.id.fab_add_task);
        tvEmpty = findViewById(R.id.tv_empty);
        addTaskBar = findViewById(R.id.add_task_bar);

        // Setup toolbar
        setSupportActionBar(toolbar);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.drawer_open, R.string.drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // Setup navigation
        navigationView.setNavigationItemSelectedListener(this);
        navigationView.setCheckedItem(R.id.nav_next_7_days);

        // Setup RecyclerView
        taskAdapter = new TaskAdapter();
        recyclerTasks.setLayoutManager(new LinearLayoutManager(this));
        recyclerTasks.setAdapter(taskAdapter);

        // Task click -> open BottomSheet to edit
        taskAdapter.setOnTaskClickListener(task -> showTaskDetail(task));

        // Task checkbox -> toggle completion
        taskAdapter.setOnTaskCheckListener((task, isChecked) -> {
            task.setCompleted(isChecked);
            viewModel.updateTask(task);
        });

        // FAB -> add new task
        fabAddTask.setOnClickListener(v -> showTaskDetail(null));
        addTaskBar.setOnClickListener(v -> showTaskDetail(null));

        // Load default view (7 ngày tới)
        loadNext7DaysView();

        // Handle back press
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_today) {
            currentMode = ViewMode.TODAY;
            toolbar.setTitle(R.string.nav_today);
            loadTodayView();
        } else if (id == R.id.nav_next_7_days) {
            currentMode = ViewMode.NEXT_7_DAYS;
            toolbar.setTitle(R.string.nav_next_7_days);
            loadNext7DaysView();
        } else if (id == R.id.nav_inbox) {
            currentMode = ViewMode.INBOX;
            toolbar.setTitle(R.string.nav_inbox);
            loadInboxView();
        } else if (id == R.id.nav_work) {
            currentMode = ViewMode.CATEGORY;
            currentCategoryId = 1;
            toolbar.setTitle(R.string.cat_work);
            loadCategoryView(1);
        } else if (id == R.id.nav_study) {
            currentMode = ViewMode.CATEGORY;
            currentCategoryId = 2;
            toolbar.setTitle(R.string.cat_study);
            loadCategoryView(2);
        } else if (id == R.id.nav_travel) {
            currentMode = ViewMode.CATEGORY;
            currentCategoryId = 3;
            toolbar.setTitle(R.string.cat_travel);
            loadCategoryView(3);
        } else if (id == R.id.nav_this_week) {
            currentMode = ViewMode.THIS_WEEK;
            toolbar.setTitle(R.string.filter_this_week);
            loadThisWeekView();
        } else if (id == R.id.nav_unscheduled) {
            currentMode = ViewMode.UNSCHEDULED;
            toolbar.setTitle(R.string.filter_unscheduled);
            loadUnscheduledView();
        } else if (id == R.id.nav_completed) {
            currentMode = ViewMode.COMPLETED;
            toolbar.setTitle(R.string.filter_completed);
            loadCompletedView();
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    // ============ View Loading Methods ============

    private void loadNext7DaysView() {
        // Grouped view: Hôm nay / Ngày mai / Sắp tới
        removeAllObservers();

        final List<Task> todayList = new ArrayList<>();
        final List<Task> tomorrowList = new ArrayList<>();
        final List<Task> upcomingList = new ArrayList<>();

        viewModel.getTasksForToday().observe(this, tasks -> {
            todayList.clear();
            if (tasks != null) todayList.addAll(tasks);
            taskAdapter.setGroupedData(todayList, tomorrowList, upcomingList);
            updateEmptyState();
        });

        viewModel.getTasksForTomorrow().observe(this, tasks -> {
            tomorrowList.clear();
            if (tasks != null) tomorrowList.addAll(tasks);
            taskAdapter.setGroupedData(todayList, tomorrowList, upcomingList);
            updateEmptyState();
        });

        viewModel.getTasksUpcoming().observe(this, tasks -> {
            upcomingList.clear();
            if (tasks != null) upcomingList.addAll(tasks);
            taskAdapter.setGroupedData(todayList, tomorrowList, upcomingList);
            updateEmptyState();
        });
    }

    private void loadTodayView() {
        removeAllObservers();
        viewModel.getTasksForToday().observe(this, tasks -> {
            taskAdapter.setFlatData(getString(R.string.section_today), tasks);
            updateEmptyState();
        });
    }

    private void loadInboxView() {
        removeAllObservers();
        viewModel.getInboxTasks().observe(this, tasks -> {
            taskAdapter.setFlatData(getString(R.string.nav_inbox), tasks);
            updateEmptyState();
        });
    }

    private void loadCategoryView(int categoryId) {
        removeAllObservers();
        viewModel.getTasksByCategory(categoryId).observe(this, tasks -> {
            taskAdapter.setFlatData(toolbar.getTitle().toString(), tasks);
            updateEmptyState();
        });
    }

    private void loadThisWeekView() {
        removeAllObservers();
        viewModel.getTasksThisWeek().observe(this, tasks -> {
            taskAdapter.setFlatData(getString(R.string.filter_this_week), tasks);
            updateEmptyState();
        });
    }

    private void loadUnscheduledView() {
        removeAllObservers();
        viewModel.getUnscheduledTasks().observe(this, tasks -> {
            taskAdapter.setFlatData(getString(R.string.filter_unscheduled), tasks);
            updateEmptyState();
        });
    }

    private void loadCompletedView() {
        removeAllObservers();
        viewModel.getCompletedTasks().observe(this, tasks -> {
            taskAdapter.setFlatData(getString(R.string.filter_completed), tasks);
            updateEmptyState();
        });
    }

    // ============ Helper Methods ============

    private void removeAllObservers() {
        // Remove previous observers to avoid stale data
        viewModel.getTasksForToday().removeObservers(this);
        viewModel.getTasksForTomorrow().removeObservers(this);
        viewModel.getTasksUpcoming().removeObservers(this);
        viewModel.getTasksNext7Days().removeObservers(this);
        viewModel.getInboxTasks().removeObservers(this);
        viewModel.getUnscheduledTasks().removeObservers(this);
        viewModel.getCompletedTasks().removeObservers(this);
        viewModel.getTasksThisWeek().removeObservers(this);
    }

    private void updateEmptyState() {
        if (taskAdapter.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            recyclerTasks.setVisibility(View.GONE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            recyclerTasks.setVisibility(View.VISIBLE);
        }
    }

    private void showTaskDetail(Task task) {
        TaskDetailBottomSheet bottomSheet = TaskDetailBottomSheet.newInstance(task);
        bottomSheet.setOnTaskSavedListener(() -> {
            // Refresh current view after save
            refreshCurrentView();
        });
        bottomSheet.show(getSupportFragmentManager(), "TASK_DETAIL");
    }

    private void refreshCurrentView() {
        switch (currentMode) {
            case NEXT_7_DAYS:
                loadNext7DaysView();
                break;
            case TODAY:
                loadTodayView();
                break;
            case INBOX:
                loadInboxView();
                break;
            case CATEGORY:
                loadCategoryView(currentCategoryId);
                break;
            case THIS_WEEK:
                loadThisWeekView();
                break;
            case UNSCHEDULED:
                loadUnscheduledView();
                break;
            case COMPLETED:
                loadCompletedView();
                break;
        }
    }
}