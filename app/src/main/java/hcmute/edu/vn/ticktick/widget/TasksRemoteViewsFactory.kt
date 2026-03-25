package hcmute.edu.vn.ticktick.widget

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import hcmute.edu.vn.ticktick.LanguageManager
import hcmute.edu.vn.ticktick.R
import hcmute.edu.vn.ticktick.database.AppDatabase
import hcmute.edu.vn.ticktick.database.Task
import hcmute.edu.vn.ticktick.ui.DateUtils
import java.util.Locale
import java.util.concurrent.TimeUnit

class TasksRemoteViewsFactory(
    context: Context,
    intent: Intent?
) : RemoteViewsService.RemoteViewsFactory {

    private val localizedContext: Context = LanguageManager.wrapContext(context)
    private val appDatabase: AppDatabase = AppDatabase.getDatabase(localizedContext)
    private val maxVisibleTasks: Int = intent?.getIntExtra(EXTRA_MAX_VISIBLE_TASKS, 4) ?: 4

    private val tasks = mutableListOf<TasksWidgetTaskItem>()

    override fun onCreate() = Unit

    override fun onDataSetChanged() {
        tasks.clear()
        val now = System.currentTimeMillis()

        try {
            val future = AppDatabase.databaseWriteExecutor.submit<List<TasksWidgetTaskItem>> {
                appDatabase.taskDao()
                    .getWidgetActiveScheduledTasks()
                    .asSequence()
                    .filter { task -> isNotOverdue(task, now) }
                    .take(maxVisibleTasks)
                    .map { task ->
                        TasksWidgetTaskItem(
                            taskId = task.id,
                            title = task.title.orEmpty(),
                            dueDate = task.dueDate,
                            dueTime = task.dueTime?.takeIf { it.isNotBlank() }
                        )
                    }
                    .toList()
            }
            tasks.addAll(future.get(4, TimeUnit.SECONDS))
        } catch (_: Exception) {
            // Keep empty state instead of crashing host launcher.
        }
    }

    override fun onDestroy() {
        tasks.clear()
    }

    override fun getCount(): Int = tasks.size

    override fun getViewAt(position: Int): RemoteViews? {
        if (position !in tasks.indices) return null
        val item = tasks[position]

        val row = RemoteViews(localizedContext.packageName, R.layout.widget_tasks_item)
        row.setTextViewText(R.id.widget_item_title, item.title)

        val timeLabel = normalizeDueTime(item.dueTime)
        val dateLabel = if (item.dueDate > 0L) DateUtils.formatDate(item.dueDate) else ""
        val dueLabel = listOf(dateLabel, timeLabel).filter { it.isNotBlank() }.joinToString(" ")

        row.setViewVisibility(R.id.widget_item_due, if (dueLabel.isBlank()) View.GONE else View.VISIBLE)
        row.setTextViewText(R.id.widget_item_due, dueLabel)
        row.setImageViewResource(R.id.widget_item_check, R.drawable.ic_widget_checkbox_unchecked)

        val openDetailIntent = Intent().apply {
            action = TasksWidgetProvider.ACTION_OPEN_TASK_DETAIL
            putExtra(TasksWidgetProvider.EXTRA_TASK_ID, item.taskId)
            data = Uri.parse("ticktick://widget/task/${item.taskId}")
        }
        row.setOnClickFillInIntent(R.id.widget_item_root, openDetailIntent)

        val markDoneIntent = Intent().apply {
            action = TasksWidgetProvider.ACTION_MARK_TASK_DONE
            putExtra(TasksWidgetProvider.EXTRA_TASK_ID, item.taskId)
            data = Uri.parse("ticktick://widget/task/${item.taskId}/done")
        }
        row.setOnClickFillInIntent(R.id.widget_item_check, markDoneIntent)

        return row
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long = tasks.getOrNull(position)?.taskId?.toLong() ?: position.toLong()

    override fun hasStableIds(): Boolean = true

    private fun normalizeDueTime(raw: String?): String {
        val value = raw?.trim().orEmpty()
        if (value.isBlank()) return ""

        return try {
            val parts = value.split(":")
            if (parts.size < 2) return value
            val hour = parts[0].toInt()
            val minute = parts[1].toInt()

            if (is24HourFormat()) {
                String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
            } else {
                val amPm = if (hour >= 12) localizedContext.getString(R.string.widget_tasks_pm) else localizedContext.getString(R.string.widget_tasks_am)
                val hour12 = when (val h = hour % 12) {
                    0 -> 12
                    else -> h
                }
                String.format(Locale.getDefault(), "%d:%02d %s", hour12, minute, amPm)
            }
        } catch (_: Exception) {
            value
        }
    }

    private fun is24HourFormat(): Boolean {
        val locale = localizedContext.resources.configuration.locales[0] ?: Locale.getDefault()
        val language = locale.language.lowercase(Locale.ROOT)
        return language != "en"
    }

    private fun isNotOverdue(task: Task, now: Long): Boolean {
        val dueAt = DateUtils.getDueDateTimeMillis(task.dueDate, task.dueTime)
        return !task.isCompleted && dueAt >= now
    }

    companion object {
        const val EXTRA_MAX_VISIBLE_TASKS = "extra_max_visible_tasks"
    }
}
