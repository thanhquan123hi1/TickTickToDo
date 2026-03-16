package hcmute.edu.vn.ticktick.widget;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import hcmute.edu.vn.ticktick.R;
import hcmute.edu.vn.ticktick.database.AppDatabase;
import hcmute.edu.vn.ticktick.database.Task;

public class TaskWidgetRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory {

    private static final String TAG = "TaskWidgetFactory";

    private final Context context;
    private final AppDatabase appDatabase;
    private final int appWidgetId;

    private final List<Task> tasks = new ArrayList<>();
    private WidgetConfig config;

    public TaskWidgetRemoteViewsFactory(Context context, Intent intent) {
        this.context = context;
        this.appDatabase = AppDatabase.getDatabase(context);
        this.appWidgetId = intent != null
                ? intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
                : AppWidgetManager.INVALID_APPWIDGET_ID;
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate() widgetId=" + appWidgetId);
    }

    @Override
    public void onDataSetChanged() {
        config = WidgetPrefs.load(context, appWidgetId);
        Log.d(TAG, "onDataSetChanged() widgetId=" + appWidgetId + " mode=" + config.mode + " limit=" + config.taskLimit + " includeCompleted=" + config.includeCompleted);
        loadTasks();
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy() widgetId=" + appWidgetId);
        tasks.clear();
    }

    @Override
    public int getCount() {
        int count = tasks.size();
        Log.v(TAG, "getCount() widgetId=" + appWidgetId + " count=" + count);
        return count;
    }

    @Override
    public RemoteViews getViewAt(int position) {
        if (position < 0 || position >= tasks.size()) {
            Log.w(TAG, "getViewAt() invalid position=" + position + " size=" + tasks.size());
            return null;
        }

        Task task = tasks.get(position);
        RemoteViews row = new RemoteViews(context.getPackageName(), R.layout.widget_task_item);

        String title = task.getTitle();
        if (task.isCompleted()) {
            title = context.getString(R.string.widget_task_completed_prefix, title);
        }
        row.setTextViewText(R.id.widget_item_title, title);

        boolean showDeadline = config == null || config.showDeadline;
        String dueText = showDeadline ? buildDueText(task) : "";
        row.setTextViewText(R.id.widget_item_due, dueText);
        row.setViewVisibility(R.id.widget_item_due, TextUtils.isEmpty(dueText) ? View.GONE : View.VISIBLE);

        Intent fillInIntent = new Intent();
        fillInIntent.setAction(TaskWidgetProvider.ACTION_OPEN_TASK_DETAIL);
        fillInIntent.putExtra(TaskWidgetProvider.EXTRA_TASK_ID, task.getId());
        row.setOnClickFillInIntent(R.id.widget_item_root, fillInIntent);

        return row;
    }

    @Override
    public RemoteViews getLoadingView() {
        return null;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public long getItemId(int position) {
        if (position < 0 || position >= tasks.size()) {
            return position;
        }
        return tasks.get(position).getId();
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    private void loadTasks() {
        tasks.clear();

        int limit = config != null ? config.taskLimit : WidgetConfig.TASK_LIMIT_DEFAULT;
        boolean includeCompleted = config != null && config.includeCompleted;

        try {
            Future<List<Task>> future = AppDatabase.databaseWriteExecutor.submit(() -> includeCompleted
                    ? appDatabase.taskDao().getWidgetTasksAll(limit)
                    : appDatabase.taskDao().getWidgetTasks(limit));

            List<Task> fetchedTasks = future.get(5, TimeUnit.SECONDS);
            if (fetchedTasks != null) {
                tasks.addAll(fetchedTasks);
            }
            Log.d(TAG, "loadTasks() widgetId=" + appWidgetId + " fetched=" + tasks.size());
        } catch (Exception e) {
            Log.e(TAG, "loadTasks() failed widgetId=" + appWidgetId, e);
        }
    }

    private String buildDueText(Task task) {
        if (task.getDueDate() <= 0) {
            return context.getString(R.string.widget_due_unscheduled);
        }

        long dueDate = task.getDueDate();
        String dueTime = task.getDueTime();
        String datePart;

        if (isToday(dueDate) && !TextUtils.isEmpty(dueTime)) {
            datePart = dueTime;
        } else {
            datePart = new SimpleDateFormat("dd/MM", Locale.getDefault()).format(dueDate);
            if (!TextUtils.isEmpty(dueTime)) {
                datePart = datePart + " " + dueTime;
            }
        }

        return context.getString(R.string.widget_due_format, datePart);
    }

    private boolean isToday(long timestamp) {
        Calendar target = Calendar.getInstance();
        target.setTimeInMillis(timestamp);
        Calendar now = Calendar.getInstance();
        return target.get(Calendar.YEAR) == now.get(Calendar.YEAR)
                && target.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR);
    }
}
