package hcmute.edu.vn.ticktick.widget

/**
 * Small immutable model used by the widget renderer.
 */
data class TodayWidgetTaskItem(
    val taskId: Int,
    val title: String,
    val dueTime: String?
)
