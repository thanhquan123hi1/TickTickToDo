package hcmute.edu.vn.ticktick.widget;

import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.util.Log;
import android.widget.RemoteViewsService;

public class TaskWidgetService extends RemoteViewsService {

    private static final String TAG = "TaskWidgetService";

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        int appWidgetId = intent != null
                ? intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
                : AppWidgetManager.INVALID_APPWIDGET_ID;
        Log.d(TAG, "onGetViewFactory() widgetId=" + appWidgetId + " data=" + (intent != null ? intent.getData() : null));
        return new TaskWidgetRemoteViewsFactory(getApplicationContext(), intent);
    }
}
