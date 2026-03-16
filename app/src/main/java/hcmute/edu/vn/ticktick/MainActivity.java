package hcmute.edu.vn.ticktick;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.core.splashscreen.SplashScreen;
import androidx.core.util.Pair;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.Calendar;

import hcmute.edu.vn.ticktick.adapter.TaskAdapter;
import hcmute.edu.vn.ticktick.database.AppDatabase;
import hcmute.edu.vn.ticktick.database.Task;
import hcmute.edu.vn.ticktick.main.TaskListController;
import hcmute.edu.vn.ticktick.navigation.NavPanel;
import hcmute.edu.vn.ticktick.navigation.NavRailController;
import hcmute.edu.vn.ticktick.navigation.PanelContentFactory;
import hcmute.edu.vn.ticktick.navigation.PanelContentFactory.NavPanelCallback;
import hcmute.edu.vn.ticktick.navigation.PanelContentFactory.ViewDestination;
import hcmute.edu.vn.ticktick.ui.DateUtils;
import hcmute.edu.vn.ticktick.ui.TaskDetailBottomSheet;
import hcmute.edu.vn.ticktick.ui.TaskViewModel;
import hcmute.edu.vn.ticktick.widget.TaskWidgetProvider;

/**
 * Thin coordinator: binds views, wires collaborators together, handles back press.
 * All navigation logic lives in {@link PanelContentFactory} / {@link NavPanel} /
 * {@link NavRailController}; all data logic lives in {@link TaskListController}.
 */
public class MainActivity extends BaseActivity implements NavPanelCallback, NavPanel.Host {

    // --- Views ---
    // Cac bien nay map truc tiep voi id trong res/layout/activity_main.xml.
    private RecyclerView recyclerTasks;
    private TextView tvEmpty;
    private TextView tvGreeting;
    private TextView tvTaskSurfaceTitle;
    private LinearLayout navRail;
    private LinearLayout expandedPanel;
    private View overlayDim;
    private TextView tvPanelTitle;
    private LinearLayout panelContent;

    // --- Rail buttons ---
    // Nhom nut dieu huong ben trai (id: rail_btn_* trong activity_main.xml).
    private ImageButton railBtnTasks, railBtnCalendar, railBtnFilter;
    private ImageButton railBtnTools, railBtnCompleted, railBtnSettings;

    // --- Collaborators ---
    // Cac thanh phan tach rieng trach nhiem: nav, du lieu, adapter, state.
    private NavPanel navPanel;
    private NavRailController railController;
    private PanelContentFactory panelFactory;
    private TaskListController taskListController;
    private TaskAdapter taskAdapter;
    private TaskViewModel viewModel;

    // --- State ---
    private ViewDestination currentDestination = ViewDestination.NEXT_7_DAYS;
    private int currentCategoryId = -1;
    private String currentSectionTitle;
    private long currentRangeStart = 0L;
    private long currentRangeEnd = 0L;

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Khoi tao giao dien va gan hanh vi cho tung khoi chuc nang.
        bindViews();
        setupCollaborators();
        setupRecyclerView();
        setupRailButtons();
        setupFab();
        setupHeroActions();
        setupBackPress();
        updateGreeting();

        // Default view
        railController.setActive(railBtnTasks);
        navigateTo(ViewDestination.NEXT_7_DAYS, -1, null);
        handleWidgetIntent(getIntent());
    }

    @Override
    protected void onNewIntent(@NonNull Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleWidgetIntent(intent);
    }

    // -------------------------------------------------------------------------
    // NavPanel.Host implementation
    // -------------------------------------------------------------------------

    @Override public LinearLayout getNavRail() { return navRail; }
    @Override public LinearLayout getExpandedPanel() { return expandedPanel; }
    @Override public View getOverlayDim() { return overlayDim; }
    @Override public int resolveColor(int res) { return getResources().getColor(res, null); }

    // -------------------------------------------------------------------------
    // NavPanelCallback implementation
    // -------------------------------------------------------------------------

    @Override
    public void navigateTo(ViewDestination destination, int categoryId, String titleOverride) {
        // Doi man hinh logic (bo loc/nhom task) va cap nhat title toolbar.
        currentDestination = destination;
        currentCategoryId = destination == ViewDestination.CATEGORY ? categoryId : -1;

        switch (destination) {
            case ALL_TASKS:
                updateSectionTitle(getString(R.string.nav_all_tasks));
                taskListController.loadAllTasks(getString(R.string.nav_all_tasks));
                break;
            case NEXT_7_DAYS:
                updateSectionTitle(getString(R.string.nav_next_7_days));
                taskListController.loadNext7Days();
                break;
            case TODAY:
                updateSectionTitle(getString(R.string.nav_today));
                taskListController.loadToday(getString(R.string.section_today));
                break;
            case INBOX:
                updateSectionTitle(getString(R.string.nav_inbox));
                taskListController.loadInbox(getString(R.string.nav_inbox));
                break;
            case CATEGORY:
                updateSectionTitle(titleOverride != null ? titleOverride : getString(R.string.task_category));
                taskListController.loadCategory(categoryId, currentSectionTitle);
                break;
            case THIS_WEEK:
                updateSectionTitle(getString(R.string.filter_this_week));
                taskListController.loadThisWeek(getString(R.string.filter_this_week));
                break;
            case UNSCHEDULED:
                updateSectionTitle(getString(R.string.filter_unscheduled));
                taskListController.loadUnscheduled(getString(R.string.filter_unscheduled));
                break;
            case COMPLETED:
                updateSectionTitle(getString(R.string.filter_completed));
                taskListController.loadCompleted(getString(R.string.filter_completed));
                break;
            case DATE_RANGE:
                String rangeTitle = titleOverride != null ? titleOverride : DateUtils.formatDateRange(currentRangeStart, currentRangeEnd);
                updateSectionTitle(rangeTitle.isEmpty() ? getString(R.string.filter_date_range) : rangeTitle);
                taskListController.loadDateRange(currentSectionTitle, currentRangeStart, DateUtils.getStartOfNextDay(currentRangeEnd));
                break;
        }
    }

    @Override
    public void closePanel() {
        navPanel.close();
    }

    @Override
    public void openDateRangeFilter() {
        MaterialDatePicker.Builder<Pair<Long, Long>> builder = MaterialDatePicker.Builder.dateRangePicker()
                .setTitleText(getString(R.string.filter_date_range));

        if (currentRangeStart > 0 && currentRangeEnd > 0) {
            builder.setSelection(Pair.create(currentRangeStart, currentRangeEnd));
        }

        MaterialDatePicker<Pair<Long, Long>> picker = builder.build();
        picker.addOnPositiveButtonClickListener(selection -> {
            if (selection == null || selection.first == null || selection.second == null) {
                return;
            }
            currentRangeStart = DateUtils.getStartOfDay(selection.first);
            currentRangeEnd = DateUtils.getStartOfDay(selection.second);
            navigateTo(ViewDestination.DATE_RANGE, -1, DateUtils.formatDateRange(currentRangeStart, currentRangeEnd));
        });
        picker.show(getSupportFragmentManager(), "DATE_RANGE_PICKER");
    }

    // -------------------------------------------------------------------------
    // Private setup helpers
    // -------------------------------------------------------------------------

    private void bindViews() {
        // Map bien Java -> view id trong activity_main.xml.
        recyclerTasks = findViewById(R.id.recycler_tasks);
        tvEmpty = findViewById(R.id.tv_empty);
        tvGreeting = findViewById(R.id.tv_greeting);
        tvTaskSurfaceTitle = findViewById(R.id.tv_task_surface_title);
        navRail = findViewById(R.id.nav_rail);
        expandedPanel = findViewById(R.id.expanded_panel);
        overlayDim = findViewById(R.id.overlay_dim);
        tvPanelTitle = findViewById(R.id.tv_panel_title);
        panelContent = findViewById(R.id.panel_content);

        railBtnTasks = findViewById(R.id.rail_btn_tasks);
        railBtnCalendar = findViewById(R.id.rail_btn_calendar);
        railBtnFilter = findViewById(R.id.rail_btn_filter);
        railBtnTools = findViewById(R.id.rail_btn_tools);
        railBtnCompleted = findViewById(R.id.rail_btn_completed);
        railBtnSettings = findViewById(R.id.rail_btn_settings);
    }

    private void setupCollaborators() {
        // Khoi tao cac doi tuong xu ly du lieu va dieu huong, Activity chi dieu phoi.
        viewModel = new ViewModelProvider(this).get(TaskViewModel.class);
        taskAdapter = new TaskAdapter();
        taskAdapter.setOnTaskClickListener(this::showTaskDetail);
        taskAdapter.setOnTaskCheckListener((task, isChecked) -> {
            task.setCompleted(isChecked);
            viewModel.updateTask(task);
        });

        navPanel = new NavPanel(this);
        railController = new NavRailController(
                railBtnTasks, railBtnCalendar, railBtnFilter,
                railBtnTools, railBtnCompleted, railBtnSettings);
        panelFactory = new PanelContentFactory(this, panelContent, tvPanelTitle, this);
        taskListController = new TaskListController(
                viewModel,
                taskAdapter,
                this,
                this::updateEmptyState,
                getString(R.string.section_today),
                getString(R.string.section_tomorrow),
                getString(R.string.section_upcoming)
        );

        overlayDim.setOnClickListener(v -> closePanel());
    }

    private void setupRecyclerView() {
        // Cau hinh danh sach task theo chieu doc.
        recyclerTasks.setLayoutManager(new LinearLayoutManager(this));
        recyclerTasks.setAdapter(taskAdapter);
    }

    private void setupFab() {
        // FAB va add task bar deu mo form tao task moi.
        FloatingActionButton fab = findViewById(R.id.fab_add_task);
        View addTaskBar = findViewById(R.id.add_task_bar);
        fab.setOnClickListener(v -> showTaskDetail(null));
        addTaskBar.setOnClickListener(v -> showTaskDetail(null));
    }

    private void setupHeroActions() {
        // Mo panel ho so tu avatar sidebar.
        View.OnClickListener openProfile = v -> togglePanel(null, panelFactory::buildProfilePanel);
        findViewById(R.id.btn_avatar).setOnClickListener(openProfile);
    }

    private void setupRailButtons() {
        // Moi nut rail mo 1 panel noi dung tuong ung.
        railBtnTasks.setOnClickListener(v -> togglePanel(railBtnTasks, panelFactory::buildTasksPanel));
        railBtnCalendar.setOnClickListener(v -> togglePanel(railBtnCalendar, panelFactory::buildCalendarPanel));
        railBtnFilter.setOnClickListener(v -> togglePanel(railBtnFilter, panelFactory::buildFilterPanel));
        railBtnTools.setOnClickListener(v -> togglePanel(railBtnTools, panelFactory::buildToolsPanel));
        railBtnSettings.setOnClickListener(v -> togglePanel(railBtnSettings, panelFactory::buildSettingsPanel));

        // Nut completed di thang vao bo loc "Da hoan thanh".
        railBtnCompleted.setOnClickListener(v -> {
            closePanel();
            railController.setActive(railBtnCompleted);
            navigateTo(ViewDestination.COMPLETED, -1, null);
        });
    }

    private void setupBackPress() {
        // Neu panel dang mo thi dong panel truoc, chua thoat man hinh ngay.
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (navPanel.isOpen()) {
                    closePanel();
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
    }

    // -------------------------------------------------------------------------
    // UI helpers
    // -------------------------------------------------------------------------

    private void togglePanel(ImageButton btn, Runnable panelBuilder) {
        // Click lai cung nut dang active -> dong panel; nguoc lai build va mo panel.
        if (navPanel.isOpen() && railController.getActiveButton() == btn) {
            closePanel();
        } else {
            railController.setActive(btn);
            panelBuilder.run();
            navPanel.open();
        }
    }

    private void showTaskDetail(Task task) {
        TaskDetailBottomSheet sheet = task != null
                ? TaskDetailBottomSheet.newInstance(task)
                : TaskDetailBottomSheet.newTask(resolvePresetDueDate(), resolvePresetCategoryId());
        sheet.show(getSupportFragmentManager(), "TASK_DETAIL");
    }

    private Long resolvePresetDueDate() {
        switch (currentDestination) {
            case TODAY:
                return DateUtils.getStartOfToday();
            case DATE_RANGE:
                return currentRangeStart > 0 ? currentRangeStart : null;
            default:
                return null;
        }
    }

    private Integer resolvePresetCategoryId() {
        return currentDestination == ViewDestination.CATEGORY ? currentCategoryId : null;
    }

    private void updateEmptyState(boolean isEmpty) {
        // Khi khong co task: hien thong bao rong, an danh sach.
        tvEmpty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        recyclerTasks.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    private void updateGreeting() {
        // Cap nhat loi chao theo thoi gian trong ngay.
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        int greetingRes = hour < 12
                ? R.string.greeting_morning
                : hour < 18 ? R.string.greeting_afternoon : R.string.greeting_evening;
        tvGreeting.setText(greetingRes);
    }

    private void updateSectionTitle(String title) {
        // Cap nhat title cho toolbar va danh sach task.
        currentSectionTitle = title;
        tvTaskSurfaceTitle.setText(title);
    }

    private void handleWidgetIntent(Intent intent) {
        if (intent == null || intent.getAction() == null) {
            return;
        }

        String action = intent.getAction();
        if (TaskWidgetProvider.ACTION_ADD_TASK.equals(action)) {
            showTaskDetail(null);
            consumeWidgetIntent(intent);
            return;
        }

        if (TaskWidgetProvider.ACTION_OPEN_TASK_DETAIL.equals(action)) {
            int taskId = intent.getIntExtra(TaskWidgetProvider.EXTRA_TASK_ID, -1);
            if (taskId <= 0) {
                consumeWidgetIntent(intent);
                return;
            }

            AppDatabase.databaseWriteExecutor.execute(() -> {
                Task task = AppDatabase.getDatabase(getApplicationContext()).taskDao().getTaskByIdSync(taskId);
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }
                    if (task == null) {
                        Toast.makeText(this, R.string.msg_task_not_found, Toast.LENGTH_SHORT).show();
                    } else {
                        showTaskDetail(task);
                    }
                });
            });
            consumeWidgetIntent(intent);
        }
    }

    private void consumeWidgetIntent(Intent intent) {
        intent.setAction(null);
        intent.removeExtra(TaskWidgetProvider.EXTRA_TASK_ID);
    }
}