package hcmute.edu.vn.ticktick;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.core.splashscreen.SplashScreen;

import androidx.activity.OnBackPressedCallback;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import hcmute.edu.vn.ticktick.adapter.TaskAdapter;
import hcmute.edu.vn.ticktick.database.Task;
import hcmute.edu.vn.ticktick.main.TaskListController;
import hcmute.edu.vn.ticktick.navigation.NavPanel;
import hcmute.edu.vn.ticktick.navigation.NavRailController;
import hcmute.edu.vn.ticktick.navigation.PanelContentFactory;
import hcmute.edu.vn.ticktick.navigation.PanelContentFactory.NavPanelCallback;
import hcmute.edu.vn.ticktick.navigation.PanelContentFactory.ViewDestination;
import hcmute.edu.vn.ticktick.ui.TaskDetailBottomSheet;
import hcmute.edu.vn.ticktick.ui.TaskViewModel;

/**
 * Thin coordinator: binds views, wires collaborators together, handles back press.
 * All navigation logic lives in {@link PanelContentFactory} / {@link NavPanel} /
 * {@link NavRailController}; all data logic lives in {@link TaskListController}.
 */
public class MainActivity extends BaseActivity
        implements NavPanelCallback, NavPanel.Host {

    // --- Views ---
    // Cac bien nay map truc tiep voi id trong res/layout/activity_main.xml.
    private MaterialToolbar    toolbar;
    private RecyclerView       recyclerTasks;
    private TextView           tvEmpty;
    private LinearLayout       navRail;
    private LinearLayout       expandedPanel;
    private View               overlayDim;
    private TextView           tvPanelTitle;
    private LinearLayout       panelContent;

    // --- Rail buttons ---
    // Nhom nut dieu huong ben trai (id: rail_btn_* trong activity_main.xml).
    private ImageButton railBtnTasks, railBtnCalendar, railBtnFilter;
    private ImageButton railBtnTools, railBtnCompleted, railBtnSettings;

    // --- Collaborators ---
    // Cac thanh phan tach rieng trach nhiem: nav, du lieu, adapter, state.
    private NavPanel            navPanel;
    private NavRailController   railController;
    private PanelContentFactory panelFactory;
    private TaskListController  taskListController;
    private TaskAdapter         taskAdapter;
    private TaskViewModel       viewModel;

    // --- State ---
    private ViewDestination currentDestination = ViewDestination.NEXT_7_DAYS;
    private int             currentCategoryId  = -1;

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
        setupToolbar();
        setupRecyclerView();
        setupRailButtons();
        setupFab();
        setupBackPress();

        // Default view
        railController.setActive(railBtnTasks);
        navigateTo(ViewDestination.NEXT_7_DAYS, -1);
    }

    // -------------------------------------------------------------------------
    // NavPanel.Host implementation
    // -------------------------------------------------------------------------

    @Override public LinearLayout getNavRail()       { return navRail; }
    @Override public LinearLayout getExpandedPanel() { return expandedPanel; }
    @Override public View         getOverlayDim()    { return overlayDim; }
    @Override public int          resolveColor(int res)  { return getResources().getColor(res, null); }

    // -------------------------------------------------------------------------
    // NavPanelCallback implementation
    // -------------------------------------------------------------------------

    @Override
    public void navigateTo(ViewDestination destination, int categoryId) {
        // Doi man hinh logic (bo loc/nhom task) va cap nhat title toolbar.
        currentDestination = destination;
        switch (destination) {
            case NEXT_7_DAYS: toolbar.setTitle(R.string.nav_next_7_days);
                taskListController.loadNext7Days(); break;
            case TODAY:       toolbar.setTitle(R.string.nav_today);
                taskListController.loadToday(getString(R.string.section_today)); break;
            case INBOX:       toolbar.setTitle(R.string.nav_inbox);
                taskListController.loadInbox(getString(R.string.nav_inbox)); break;
            case CATEGORY:
                currentCategoryId = categoryId;
                taskListController.loadCategory(categoryId, toolbar.getTitle().toString()); break;
            case THIS_WEEK:   toolbar.setTitle(R.string.filter_this_week);
                taskListController.loadThisWeek(getString(R.string.filter_this_week)); break;
            case UNSCHEDULED: toolbar.setTitle(R.string.filter_unscheduled);
                taskListController.loadUnscheduled(getString(R.string.filter_unscheduled)); break;
            case COMPLETED:   toolbar.setTitle(R.string.filter_completed);
                taskListController.loadCompleted(getString(R.string.filter_completed)); break;
        }
    }

    @Override
    public void closePanel() {
        navPanel.close();
    }

    // -------------------------------------------------------------------------
    // Private setup helpers
    // -------------------------------------------------------------------------

    private void bindViews() {
        // Map bien Java -> view id trong activity_main.xml.
        toolbar        = findViewById(R.id.toolbar);
        recyclerTasks  = findViewById(R.id.recycler_tasks);
        tvEmpty        = findViewById(R.id.tv_empty);
        navRail        = findViewById(R.id.nav_rail);
        expandedPanel  = findViewById(R.id.expanded_panel);
        overlayDim     = findViewById(R.id.overlay_dim);
        tvPanelTitle   = findViewById(R.id.tv_panel_title);
        panelContent   = findViewById(R.id.panel_content);

        railBtnTasks     = findViewById(R.id.rail_btn_tasks);
        railBtnCalendar  = findViewById(R.id.rail_btn_calendar);
        railBtnFilter    = findViewById(R.id.rail_btn_filter);
        railBtnTools     = findViewById(R.id.rail_btn_tools);
        railBtnCompleted = findViewById(R.id.rail_btn_completed);
        railBtnSettings  = findViewById(R.id.rail_btn_settings);
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

        navPanel       = new NavPanel(this);
        railController = new NavRailController(
                railBtnTasks, railBtnCalendar, railBtnFilter,
                railBtnTools, railBtnCompleted, railBtnSettings);
        panelFactory   = new PanelContentFactory(this, panelContent, tvPanelTitle, this);
        taskListController = new TaskListController(viewModel, taskAdapter, this,
                isEmpty -> updateEmptyState(isEmpty));

        overlayDim.setOnClickListener(v -> closePanel());
    }

    private void setupToolbar() {
        // Gan toolbar trong layout lam ActionBar cua Activity.
        setSupportActionBar(toolbar);
    }

    private void setupRecyclerView() {
        // Cau hinh danh sach task theo chieu doc.
        recyclerTasks.setLayoutManager(new LinearLayoutManager(this));
        recyclerTasks.setAdapter(taskAdapter);
    }

    private void setupFab() {
        // FAB va add task bar deu mo form tao task moi.
        FloatingActionButton fab   = findViewById(R.id.fab_add_task);
        View addTaskBar            = findViewById(R.id.add_task_bar);
        fab.setOnClickListener(v       -> showTaskDetail(null));
        addTaskBar.setOnClickListener(v -> showTaskDetail(null));
    }

    private void setupRailButtons() {
        // Moi nut rail mo 1 panel noi dung tuong ung.
        railBtnTasks.setOnClickListener(v     -> togglePanel(railBtnTasks,     panelFactory::buildTasksPanel));
        railBtnCalendar.setOnClickListener(v  -> togglePanel(railBtnCalendar,  panelFactory::buildCalendarPanel));
        railBtnFilter.setOnClickListener(v    -> togglePanel(railBtnFilter,    panelFactory::buildFilterPanel));
        railBtnTools.setOnClickListener(v     -> togglePanel(railBtnTools,     panelFactory::buildToolsPanel));
        railBtnSettings.setOnClickListener(v  -> togglePanel(railBtnSettings,  panelFactory::buildSettingsPanel));

        // Nut completed di thang vao bo loc "Da hoan thanh".
        railBtnCompleted.setOnClickListener(v -> {
            closePanel();
            railController.setActive(railBtnCompleted);
            navigateTo(ViewDestination.COMPLETED, -1);
        });

        // Avatar mo panel ho so, khong active nut rail nao.
        findViewById(R.id.btn_avatar).setOnClickListener(v ->
                togglePanel(null, panelFactory::buildProfilePanel));
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
        // Mo bottom sheet tao/sua task; luu xong thi reload theo destination hien tai.
        TaskDetailBottomSheet sheet = TaskDetailBottomSheet.newInstance(task);
        sheet.setOnTaskSavedListener(() -> navigateTo(currentDestination, currentCategoryId));
        sheet.show(getSupportFragmentManager(), "TASK_DETAIL");
    }

    private void updateEmptyState(boolean isEmpty) {
        // Khi khong co task: hien thong bao rong, an danh sach.
        tvEmpty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        recyclerTasks.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }
}