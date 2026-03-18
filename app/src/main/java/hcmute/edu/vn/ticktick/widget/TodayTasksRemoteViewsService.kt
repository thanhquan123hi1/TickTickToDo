package hcmute.edu.vn.ticktick.widget

import android.content.Intent
import android.widget.RemoteViewsService

class TodayTasksRemoteViewsService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return TodayTasksRemoteViewsFactory(applicationContext, intent)
    }
}

