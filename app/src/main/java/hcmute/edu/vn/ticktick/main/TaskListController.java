package hcmute.edu.vn.ticktick.main;

import androidx.lifecycle.LifecycleOwner;

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
    private final TaskAdapter   adapter;
    private final LifecycleOwner owner;
    private final EmptyStateListener emptyStateListener;

    public TaskListController(TaskViewModel viewModel,
                               TaskAdapter adapter,
                               LifecycleOwner owner,
                               EmptyStateListener emptyStateListener) {
        this.viewModel          = viewModel;
        this.adapter            = adapter;
        this.owner              = owner;
        this.emptyStateListener = emptyStateListener;
    }

    // -------------------------------------------------------------------------
    // Load methods (one per view mode)
    // -------------------------------------------------------------------------

    public void loadNext7Days() {
        removeAllObservers();

        final List<Task> today    = new ArrayList<>();
        final List<Task> tomorrow = new ArrayList<>();
        final List<Task> upcoming = new ArrayList<>();

        viewModel.getTasksForToday().observe(owner, tasks -> {
            today.clear();
            if (tasks != null) today.addAll(tasks);
            adapter.setGroupedData(today, tomorrow, upcoming);
            notifyEmpty();
        });
        viewModel.getTasksForTomorrow().observe(owner, tasks -> {
            tomorrow.clear();
            if (tasks != null) tomorrow.addAll(tasks);
            adapter.setGroupedData(today, tomorrow, upcoming);
            notifyEmpty();
        });
        viewModel.getTasksUpcoming().observe(owner, tasks -> {
            upcoming.clear();
            if (tasks != null) upcoming.addAll(tasks);
            adapter.setGroupedData(today, tomorrow, upcoming);
            notifyEmpty();
        });
    }

    public void loadToday(String sectionTitle) {
        removeAllObservers();
        viewModel.getTasksForToday().observe(owner, tasks -> {
            adapter.setFlatData(sectionTitle, tasks);
            notifyEmpty();
        });
    }

    public void loadInbox(String sectionTitle) {
        removeAllObservers();
        viewModel.getInboxTasks().observe(owner, tasks -> {
            adapter.setFlatData(sectionTitle, tasks);
            notifyEmpty();
        });
    }

    public void loadCategory(int categoryId, String sectionTitle) {
        removeAllObservers();
        viewModel.getTasksByCategory(categoryId).observe(owner, tasks -> {
            adapter.setFlatData(sectionTitle, tasks);
            notifyEmpty();
        });
    }

    public void loadThisWeek(String sectionTitle) {
        removeAllObservers();
        viewModel.getTasksThisWeek().observe(owner, tasks -> {
            adapter.setFlatData(sectionTitle, tasks);
            notifyEmpty();
        });
    }

    public void loadUnscheduled(String sectionTitle) {
        removeAllObservers();
        viewModel.getUnscheduledTasks().observe(owner, tasks -> {
            adapter.setFlatData(sectionTitle, tasks);
            notifyEmpty();
        });
    }

    public void loadCompleted(String sectionTitle) {
        removeAllObservers();
        viewModel.getCompletedTasks().observe(owner, tasks -> {
            adapter.setFlatData(sectionTitle, tasks);
            notifyEmpty();
        });
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private void removeAllObservers() {
        viewModel.getTasksForToday().removeObservers(owner);
        viewModel.getTasksForTomorrow().removeObservers(owner);
        viewModel.getTasksUpcoming().removeObservers(owner);
        viewModel.getTasksNext7Days().removeObservers(owner);
        viewModel.getInboxTasks().removeObservers(owner);
        viewModel.getUnscheduledTasks().removeObservers(owner);
        viewModel.getCompletedTasks().removeObservers(owner);
        viewModel.getTasksThisWeek().removeObservers(owner);
    }

    private void notifyEmpty() {
        emptyStateListener.onEmptyStateChanged(adapter.isEmpty());
    }
}
