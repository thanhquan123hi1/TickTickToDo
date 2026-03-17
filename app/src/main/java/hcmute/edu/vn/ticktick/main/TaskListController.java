package hcmute.edu.vn.ticktick.main;

import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;

import java.util.ArrayList;
import java.util.List;

import hcmute.edu.vn.ticktick.adapter.TaskAdapter;
import hcmute.edu.vn.ticktick.database.Task;
import hcmute.edu.vn.ticktick.ui.TaskViewModel;

/**
 * Manages all task-list loading logic: subscribing to LiveData, calling the adapter,
 * and notifying when the empty state changes.
 * <p>
 * Single Responsibility: knows only how to observe the ViewModel and push data to the adapter.
 * This class is easily unit-tested without an Activity.
 */
public class TaskListController {

    public interface EmptyStateListener {
        void onEmptyStateChanged(boolean isEmpty);
    }

    private final TaskViewModel viewModel;
    private final TaskAdapter adapter;
    private final LifecycleOwner owner;
    private final EmptyStateListener emptyStateListener;
    private final String todayTitle;
    private final String tomorrowTitle;
    private final String upcomingTitle;

    private final List<LiveData<?>> activeSources = new ArrayList<>();

    public TaskListController(TaskViewModel viewModel,
                               TaskAdapter adapter,
                               LifecycleOwner owner,
                               EmptyStateListener emptyStateListener,
                               String todayTitle,
                               String tomorrowTitle,
                               String upcomingTitle) {
        this.viewModel = viewModel;
        this.adapter = adapter;
        this.owner = owner;
        this.emptyStateListener = emptyStateListener;
        this.todayTitle = todayTitle;
        this.tomorrowTitle = tomorrowTitle;
        this.upcomingTitle = upcomingTitle;
    }

    // -------------------------------------------------------------------------
    // Load methods (one per view mode)
    // -------------------------------------------------------------------------

    public void loadAllTasks(String sectionTitle) {
        clearActiveSources();
        observeSource(viewModel.getAllActiveTasks(), tasks -> {
            adapter.setFlatData(sectionTitle, tasks);
            notifyEmpty();
        });
    }

    public void loadNext7Days() {
        clearActiveSources();

        final List<Task> today    = new ArrayList<>();
        final List<Task> tomorrow = new ArrayList<>();
        final List<Task> upcoming = new ArrayList<>();

        observeSource(viewModel.getTasksForToday(), tasks -> {
            today.clear();
            if (tasks != null) today.addAll(tasks);
            adapter.setGroupedData(todayTitle, today, tomorrowTitle, tomorrow, upcomingTitle, upcoming);
            notifyEmpty();
        });
        observeSource(viewModel.getTasksForTomorrow(), tasks -> {
            tomorrow.clear();
            if (tasks != null) tomorrow.addAll(tasks);
            adapter.setGroupedData(todayTitle, today, tomorrowTitle, tomorrow, upcomingTitle, upcoming);
            notifyEmpty();
        });
        observeSource(viewModel.getTasksUpcoming(), tasks -> {
            upcoming.clear();
            if (tasks != null) upcoming.addAll(tasks);
            adapter.setGroupedData(todayTitle, today, tomorrowTitle, tomorrow, upcomingTitle, upcoming);
            notifyEmpty();
        });
    }

    public void loadToday(String sectionTitle) {
        clearActiveSources();
        observeSource(viewModel.getTasksForToday(), tasks -> {
            adapter.setFlatData(sectionTitle, tasks);
            notifyEmpty();
        });
    }

    public void loadCategory(int categoryId, String sectionTitle) {
        clearActiveSources();
        observeSource(viewModel.getTasksByCategory(categoryId), tasks -> {
            adapter.setFlatData(sectionTitle, tasks);
            notifyEmpty();
        });
    }

    public void loadThisWeek(String sectionTitle) {
        clearActiveSources();
        observeSource(viewModel.getTasksThisWeek(), tasks -> {
            adapter.setFlatData(sectionTitle, tasks);
            notifyEmpty();
        });
    }

    public void loadUnscheduled(String sectionTitle) {
        clearActiveSources();
        observeSource(viewModel.getUnscheduledTasks(), tasks -> {
            adapter.setFlatData(sectionTitle, tasks);
            notifyEmpty();
        });
    }

    public void loadCompleted(String sectionTitle) {
        clearActiveSources();
        observeSource(viewModel.getCompletedTasks(), tasks -> {
            adapter.setFlatData(sectionTitle, tasks);
            notifyEmpty();
        });
    }

    public void loadDateRange(String sectionTitle, long startDate, long endDateExclusive) {
        clearActiveSources();
        observeSource(viewModel.getTasksForDateRange(startDate, endDateExclusive), tasks -> {
            adapter.setFlatData(sectionTitle, tasks);
            notifyEmpty();
        });
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private void clearActiveSources() {
        for (LiveData<?> source : activeSources) {
            source.removeObservers(owner);
        }
        activeSources.clear();
    }

    private <T> void observeSource(LiveData<T> source, Observer<T> observer) {
        activeSources.add(source);
        source.observe(owner, observer);
    }

    private void notifyEmpty() {
        emptyStateListener.onEmptyStateChanged(adapter.isEmpty());
    }
}
