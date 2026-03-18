package hcmute.edu.vn.ticktick.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.view.View
import android.widget.RemoteViews
import hcmute.edu.vn.ticktick.LanguageManager
import hcmute.edu.vn.ticktick.MainActivity
import hcmute.edu.vn.ticktick.R
import hcmute.edu.vn.ticktick.database.AppDatabase
import java.util.Calendar
import java.util.concurrent.TimeUnit

class TodayTasksWidgetProvider : AppWidgetProvider() {

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        schedulePeriodicRefresh(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        cancelPeriodicRefresh(context)
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (widgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, widgetId)
        }
        schedulePeriodicRefresh(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_MARK_TASK_DONE -> {
                val taskId = intent.getIntExtra(EXTRA_TASK_ID, -1)
                if (taskId > 0) {
                    AppDatabase.databaseWriteExecutor.execute {
                        AppDatabase.getDatabase(context).taskDao().markTaskCompleted(taskId)
                        refreshAllWidgets(context)
                    }
                }
            }

            ACTION_OPEN_TASK_DETAIL -> {
                val taskId = intent.getIntExtra(EXTRA_TASK_ID, -1)
                if (taskId > 0) {
                    val openIntent = Intent(context, MainActivity::class.java).apply {
                        action = ACTION_OPEN_TASK_DETAIL
                        putExtra(EXTRA_TASK_ID, taskId)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP
                    }
                    context.startActivity(openIntent)
                }
            }

            ACTION_REFRESH_WIDGETS,
            ACTION_OPEN_TODAY,
            Intent.ACTION_DATE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_LOCALE_CHANGED -> {
                refreshAllWidgets(context)
            }
        }
    }

    companion object {
        const val ACTION_OPEN_TASK_DETAIL = "hcmute.edu.vn.ticktick.action.OPEN_TASK_DETAIL"
        const val ACTION_ADD_TASK = "hcmute.edu.vn.ticktick.action.ADD_TASK"
        const val ACTION_OPEN_TODAY = "hcmute.edu.vn.ticktick.action.OPEN_TODAY"
        const val ACTION_MARK_TASK_DONE = "hcmute.edu.vn.ticktick.action.MARK_TASK_DONE"
        const val ACTION_REFRESH_WIDGETS = "hcmute.edu.vn.ticktick.action.REFRESH_WIDGETS"
        const val EXTRA_TASK_ID = "extra_task_id"

        @JvmStatic
        fun refreshAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, TodayTasksWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(component)
            if (appWidgetIds.size == 0) return

            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widget_task_list)
            for (widgetId in appWidgetIds) {
                updateWidget(context, appWidgetManager, widgetId)
            }
        }

        private fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val localizedContext = LanguageManager.wrapContext(context)
            val views = RemoteViews(localizedContext.packageName, R.layout.widget_today)
            val maxVisibleTasks = resolveVisibleCount(appWidgetManager, appWidgetId)
            val summary = loadTaskSummary(localizedContext, maxVisibleTasks)

            views.setTextViewText(R.id.widget_title, localizedContext.getString(R.string.section_today))
            views.setTextViewText(R.id.widget_footer, localizedContext.getString(R.string.app_name))

            if (summary.extraCount > 0) {
                views.setViewVisibility(R.id.widget_badge, View.VISIBLE)
                views.setTextViewText(
                    R.id.widget_badge,
                    localizedContext.getString(R.string.widget_today_extra_badge, summary.extraCount)
                )
            } else {
                views.setViewVisibility(R.id.widget_badge, View.GONE)
            }

            val addIntent = Intent(localizedContext, MainActivity::class.java).apply {
                action = ACTION_ADD_TASK
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            views.setOnClickPendingIntent(
                R.id.widget_btn_add,
                PendingIntent.getActivity(
                    localizedContext,
                    appWidgetId + 11000,
                    addIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
            views.setInt(R.id.widget_btn_add, "setColorFilter", 0xFFFFFFFF.toInt())

            val openAppIntent = Intent(localizedContext, MainActivity::class.java).apply {
                action = ACTION_OPEN_TODAY
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            val openTodayPendingIntent = PendingIntent.getActivity(
                localizedContext,
                appWidgetId + 12000,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_header_container, openTodayPendingIntent)
            views.setOnClickPendingIntent(R.id.widget_badge, openTodayPendingIntent)
            views.setOnClickPendingIntent(R.id.widget_empty, openTodayPendingIntent)

            val listServiceIntent = Intent(localizedContext, TodayTasksRemoteViewsService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                putExtra(TodayTasksRemoteViewsFactory.EXTRA_MAX_VISIBLE_TASKS, maxVisibleTasks)
                data = Uri.parse("ticktick://widget/today/$appWidgetId/$maxVisibleTasks")
            }
            views.setRemoteAdapter(R.id.widget_task_list, listServiceIntent)
            views.setEmptyView(R.id.widget_task_list, R.id.widget_empty)
            views.setTextViewText(R.id.widget_empty, localizedContext.getString(R.string.widget_today_empty))

            val taskTemplateIntent = Intent(localizedContext, TodayTasksWidgetProvider::class.java)
            val taskTemplatePendingIntent = PendingIntent.getBroadcast(
                localizedContext,
                appWidgetId + 13000,
                taskTemplateIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
            views.setPendingIntentTemplate(R.id.widget_task_list, taskTemplatePendingIntent)

            applyTheme(localizedContext, views)
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        private fun resolveVisibleCount(appWidgetManager: AppWidgetManager, appWidgetId: Int): Int {
            val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
            val minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 180)
            return when {
                minHeight >= 220 -> 5
                minHeight >= 170 -> 4
                else -> 3
            }
        }

        private fun loadTaskSummary(context: Context, visibleLimit: Int): TaskSummary {
            val (startOfToday, endOfToday) = todayBounds()
            return try {
                val future = AppDatabase.databaseWriteExecutor.submit<TaskSummary> {
                    val count = AppDatabase.getDatabase(context)
                        .taskDao()
                        .countTodayWidgetTasks(startOfToday, endOfToday)
                    TaskSummary(totalCount = count, visibleCount = visibleLimit)
                }
                future.get(3, TimeUnit.SECONDS)
            } catch (_: Exception) {
                TaskSummary(totalCount = 0, visibleCount = visibleLimit)
            }
        }

        private fun applyTheme(context: Context, views: RemoteViews) {
            val isNight = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
            if (isNight) {
                views.setInt(R.id.widget_root, "setBackgroundResource", R.drawable.bg_widget_surface_dark)
                views.setTextColor(R.id.widget_title, 0xFFFFFFFF.toInt())
                views.setTextColor(R.id.widget_empty, 0xFFBDBDBD.toInt())
                views.setTextColor(R.id.widget_footer, 0xFF9E9E9E.toInt())
                views.setTextColor(R.id.widget_badge, 0xFFE0E0E0.toInt())
            } else {
                views.setInt(R.id.widget_root, "setBackgroundResource", R.drawable.bg_widget_surface)
                views.setTextColor(R.id.widget_title, 0xFF101010.toInt())
                views.setTextColor(R.id.widget_empty, 0xFF6D5A49.toInt())
                views.setTextColor(R.id.widget_footer, 0xFF7A7A7A.toInt())
                views.setTextColor(R.id.widget_badge, 0xFF7D7D7D.toInt())
            }
        }

        private fun schedulePeriodicRefresh(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val pending = PendingIntent.getBroadcast(
                context,
                7001,
                Intent(context, TodayTasksWidgetProvider::class.java).setAction(ACTION_REFRESH_WIDGETS),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.setInexactRepeating(
                AlarmManager.RTC,
                System.currentTimeMillis() + AlarmManager.INTERVAL_HALF_HOUR,
                AlarmManager.INTERVAL_HALF_HOUR,
                pending
            )
        }

        private fun cancelPeriodicRefresh(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val pending = PendingIntent.getBroadcast(
                context,
                7001,
                Intent(context, TodayTasksWidgetProvider::class.java).setAction(ACTION_REFRESH_WIDGETS),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pending)
        }

        private fun todayBounds(): Pair<Long, Long> {
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val start = calendar.timeInMillis
            calendar.add(Calendar.DAY_OF_YEAR, 1)
            return start to calendar.timeInMillis
        }
    }

    private data class TaskSummary(
        val totalCount: Int,
        val visibleCount: Int
    ) {
        val extraCount: Int get() = (totalCount - visibleCount).coerceAtLeast(0)
    }
}
