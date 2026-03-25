package hcmute.edu.vn.ticktick.widget

import android.content.Intent
import android.widget.RemoteViewsService

class TasksRemoteViewsService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return TasksRemoteViewsFactory(applicationContext, intent)
    }
}

