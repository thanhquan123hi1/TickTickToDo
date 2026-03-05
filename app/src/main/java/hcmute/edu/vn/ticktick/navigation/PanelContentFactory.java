package hcmute.edu.vn.ticktick.navigation;

import android.content.Context;
import android.content.Intent;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import hcmute.edu.vn.ticktick.CalendarActivity;
import hcmute.edu.vn.ticktick.CountdownActivity;
import hcmute.edu.vn.ticktick.R;

/**
 * Builds the content (rows) for every side-panel section.
 * <p>
 * Single Responsibility: knows only what items belong to each panel.
 * Open/Closed: add a new panel section by adding a new method — nothing else changes.
 * <p>
 * The {@link NavPanelCallback} interface decouples this class from the Activity.
 */
public class PanelContentFactory {

    /** Callbacks fired when a panel item is tapped. */
    public interface NavPanelCallback {
        void navigateTo(ViewDestination destination, int categoryId);
        void closePanel();
    }

    /** Represents every navigation destination that the panel can trigger. */
    public enum ViewDestination {
        TODAY, NEXT_7_DAYS, INBOX,
        CATEGORY,
        THIS_WEEK, UNSCHEDULED, COMPLETED
    }

    private final Context context;
    private final LinearLayout panelContent;
    private final TextView tvPanelTitle;
    private final PanelItemBuilder itemBuilder;
    private final NavPanelCallback callback;

    public PanelContentFactory(Context context,
                                LinearLayout panelContent,
                                TextView tvPanelTitle,
                                NavPanelCallback callback) {
        this.context       = context;
        this.panelContent  = panelContent;
        this.tvPanelTitle  = tvPanelTitle;
        this.callback      = callback;
        this.itemBuilder   = new PanelItemBuilder(context);
    }

    // -------------------------------------------------------------------------
    // Public panel builders
    // -------------------------------------------------------------------------

    public void buildTasksPanel() {
        tvPanelTitle.setText(R.string.nav_rail_tasks);
        clearContent();

        addItem(R.drawable.ic_today,  R.string.nav_today,      () -> navigate(ViewDestination.TODAY,      -1));
        addItem(R.drawable.ic_week,   R.string.nav_next_7_days, () -> navigate(ViewDestination.NEXT_7_DAYS, -1));
        addItem(R.drawable.ic_inbox,  R.string.nav_inbox,      () -> navigate(ViewDestination.INBOX,      -1));

        panelContent.addView(itemBuilder.buildDivider());
        panelContent.addView(itemBuilder.buildSectionTitle(context.getString(R.string.nav_lists)));

        addItem(R.drawable.ic_work,   R.string.cat_work,   () -> navigate(ViewDestination.CATEGORY, 1));
        addItem(R.drawable.ic_study,  R.string.cat_study,  () -> navigate(ViewDestination.CATEGORY, 2));
        addItem(R.drawable.ic_travel, R.string.cat_travel, () -> navigate(ViewDestination.CATEGORY, 3));
    }

    public void buildCalendarPanel() {
        tvPanelTitle.setText(R.string.nav_calendar);
        clearContent();

        addItem(R.drawable.ic_calendar, R.string.nav_calendar, () -> {
            callback.closePanel();
            context.startActivity(new Intent(context, CalendarActivity.class));
        });
        addItem(R.drawable.ic_today, R.string.nav_today,           () -> navigate(ViewDestination.TODAY,     -1));
        addItem(R.drawable.ic_week,  R.string.filter_this_week,    () -> navigate(ViewDestination.THIS_WEEK, -1));
    }

    public void buildFilterPanel() {
        tvPanelTitle.setText(R.string.nav_filters);
        clearContent();

        addItem(R.drawable.ic_week,      R.string.filter_this_week,   () -> navigate(ViewDestination.THIS_WEEK,   -1));
        addItem(R.drawable.ic_filter,    R.string.filter_unscheduled, () -> navigate(ViewDestination.UNSCHEDULED, -1));
        addItem(R.drawable.ic_completed, R.string.filter_completed,   () -> navigate(ViewDestination.COMPLETED,   -1));
        addItem(R.drawable.ic_star,      R.string.priority_high, () -> {
            Toast.makeText(context, R.string.priority_high, Toast.LENGTH_SHORT).show();
            callback.closePanel();
        });
    }

    public void buildToolsPanel() {
        tvPanelTitle.setText(R.string.nav_tools);
        clearContent();

        addItem(R.drawable.ic_timer,    R.string.nav_countdown, () -> {
            callback.closePanel();
            context.startActivity(new Intent(context, CountdownActivity.class));
        });
        addItem(R.drawable.ic_calendar, R.string.nav_calendar, () -> {
            callback.closePanel();
            context.startActivity(new Intent(context, CalendarActivity.class));
        });
    }

    public void buildSettingsPanel() {
        tvPanelTitle.setText(R.string.nav_settings);
        clearContent();

        addItem(R.drawable.ic_settings, R.string.nav_settings_notification, () -> toast(R.string.nav_settings_notification));
        addItem(R.drawable.ic_filter,   R.string.nav_settings_language,     () -> toast(R.string.nav_settings_language));
        addItem(R.drawable.ic_inbox,    R.string.nav_settings_backup,       () -> toast(R.string.nav_settings_backup));
    }

    public void buildProfilePanel() {
        tvPanelTitle.setText(R.string.nav_profile_title);
        clearContent();

        addItem(R.drawable.ic_avatar_placeholder, R.string.nav_profile_info,  () -> toast(R.string.nav_profile_info));
        addItem(R.drawable.ic_star,               R.string.nav_profile_theme, () -> toast(R.string.nav_profile_theme));
        addItem(R.drawable.ic_check,              R.string.nav_profile_about, () -> toast(R.string.nav_profile_about));
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private void navigate(ViewDestination dest, int categoryId) {
        callback.navigateTo(dest, categoryId);
        callback.closePanel();
    }

    private void addItem(int iconRes, int labelRes, Runnable onClick) {
        panelContent.addView(
                itemBuilder.buildItem(iconRes, context.getString(labelRes), onClick));
    }

    private void clearContent() {
        panelContent.removeAllViews();
    }

    private void toast(int msgRes) {
        Toast.makeText(context, msgRes, Toast.LENGTH_SHORT).show();
        callback.closePanel();
    }
}
