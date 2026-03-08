package hcmute.edu.vn.ticktick.navigation;

import android.content.Context;
import android.content.Intent;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.List;

import hcmute.edu.vn.ticktick.CalendarActivity;
import hcmute.edu.vn.ticktick.CountdownActivity;
import hcmute.edu.vn.ticktick.LanguageSelectionDialog;
import hcmute.edu.vn.ticktick.R;
import hcmute.edu.vn.ticktick.database.AppDatabase;
import hcmute.edu.vn.ticktick.database.Category;
import hcmute.edu.vn.ticktick.ui.AddCategoryDialog;

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

        // Smart views cố định
        addItem(R.drawable.ic_today,  R.string.nav_today,      () -> navigate(ViewDestination.TODAY,      -1));
        addItem(R.drawable.ic_week,   R.string.nav_next_7_days, () -> navigate(ViewDestination.NEXT_7_DAYS, -1));
        addItem(R.drawable.ic_inbox,  R.string.nav_inbox,      () -> navigate(ViewDestination.INBOX,      -1));

        panelContent.addView(itemBuilder.buildDivider());
        panelContent.addView(itemBuilder.buildSectionTitle(context.getString(R.string.nav_lists)));

        // Load danh sách category từ database
        loadCategoriesFromDatabase();

        // Nút thêm danh mục mới
        panelContent.addView(itemBuilder.buildAddCategoryItem(
                context.getString(R.string.btn_add_category),
                this::showAddCategoryDialog
        ));
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
        addItem(R.drawable.ic_filter,   R.string.nav_settings_language,     this::showLanguageDialog);
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

    /**
     * Load danh sách category từ database và thêm vào panel.
     * Chạy trên background thread, cập nhật UI trên main thread.
     */
    private void loadCategoriesFromDatabase() {
        AppDatabase db = AppDatabase.getDatabase(context);
        AppDatabase.databaseWriteExecutor.execute(() -> {
            List<Category> categories = db.categoryDao().getAllCategoriesSync();

            // Cập nhật UI trên main thread
            if (context instanceof AppCompatActivity) {
                ((AppCompatActivity) context).runOnUiThread(() -> {
                    // Tìm vị trí sau section title "Danh sách" để chèn categories
                    // Xóa các item category cũ (nếu có) và thêm mới
                    addCategoryItemsToPanel(categories);
                });
            }
        });
    }

    /**
     * Thêm các item category vào panel content.
     * @param categories Danh sách category từ database.
     */
    private void addCategoryItemsToPanel(List<Category> categories) {
        // Tìm index sau divider và section title
        int insertIndex = findCategoryInsertIndex();

        for (Category cat : categories) {
            int iconRes = getIconResIdByName(cat.getIconName());
            LinearLayout item = itemBuilder.buildItem(
                    iconRes,
                    cat.getName(),
                    () -> navigate(ViewDestination.CATEGORY, cat.getId())
            );
            panelContent.addView(item, insertIndex);
            insertIndex++;
        }
    }

    /**
     * Tìm vị trí trong panel để chèn category items.
     * Vị trí này nằm sau section title "Danh sách".
     */
    private int findCategoryInsertIndex() {
        // Mặc định: sau smart views (3) + divider (1) + section title (1) = index 5
        // Nhưng để chính xác hơn, đếm số view hiện tại
        return panelContent.getChildCount();
    }

    /**
     * Hiển thị dialog thêm category mới.
     */
    private void showAddCategoryDialog() {
        if (context instanceof AppCompatActivity) {
            AppCompatActivity activity = (AppCompatActivity) context;
            AddCategoryDialog dialog = AddCategoryDialog.newInstance();
            dialog.setOnCategoryAddedListener(category -> {
                // Reload panel sau khi thêm category
                buildTasksPanel();
            });
            dialog.show(activity.getSupportFragmentManager(), "ADD_CATEGORY");
        }
    }

    /**
     * Map tên icon (lưu trong database) sang resource ID drawable.
     * @param iconName Tên icon, ví dụ: "ic_work", "ic_study"
     * @return Resource ID, hoặc ic_list nếu không tìm thấy.
     */
    private int getIconResIdByName(String iconName) {
        if (iconName == null) return R.drawable.ic_list;

        switch (iconName) {
            case "ic_work":     return R.drawable.ic_work;
            case "ic_study":    return R.drawable.ic_study;
            case "ic_travel":   return R.drawable.ic_travel;
            case "ic_list":     return R.drawable.ic_list;
            case "ic_star":     return R.drawable.ic_star;
            case "ic_inbox":    return R.drawable.ic_inbox;
            case "ic_calendar": return R.drawable.ic_calendar;
            case "ic_timer":    return R.drawable.ic_timer;
            case "ic_filter":   return R.drawable.ic_filter;
            case "ic_check":    return R.drawable.ic_check;
            case "ic_today":    return R.drawable.ic_today;
            case "ic_week":     return R.drawable.ic_week;
            default:            return R.drawable.ic_list;
        }
    }

    /**
     * Opens the language selection dialog.
     * Keeps the panel open so the user can still see it behind the dialog.
     */
    private void showLanguageDialog() {
        if (context instanceof AppCompatActivity) {
            AppCompatActivity activity = (AppCompatActivity) context;
            LanguageSelectionDialog dialog = LanguageSelectionDialog.newInstance();
            dialog.show(activity.getSupportFragmentManager(), "LANGUAGE_DIALOG");
        }
    }

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
