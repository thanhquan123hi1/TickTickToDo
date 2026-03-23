package hcmute.edu.vn.ticktick.navigation;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;

import hcmute.edu.vn.ticktick.CalendarActivity;
import hcmute.edu.vn.ticktick.CountdownActivity;
import hcmute.edu.vn.ticktick.LanguageSelectionDialog;
import hcmute.edu.vn.ticktick.R;
import hcmute.edu.vn.ticktick.contacts.ui.ContactsActivity;
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
        void navigateTo(ViewDestination destination, int categoryId, String titleOverride);
        void closePanel();
        void openDateRangeFilter();
    }

    /** Represents every navigation destination that the panel can trigger. */
    public enum ViewDestination {
        ALL_TASKS,
        TODAY, NEXT_7_DAYS,
        CATEGORY,
        THIS_WEEK, UNSCHEDULED, COMPLETED, DATE_RANGE
    }

    private final Context context;
    private final LinearLayout panelContent;
    private final TextView tvPanelTitle;
    private final PanelItemBuilder itemBuilder;
    private final NavPanelCallback callback;
    private static final String TAG_CATEGORY_ITEM = "panel_category_item";

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

        addItem(R.drawable.ic_list,   R.string.nav_all_tasks,   () -> navigate(ViewDestination.ALL_TASKS,    -1, context.getString(R.string.nav_all_tasks)));
        addItem(R.drawable.ic_today,  R.string.nav_today,       () -> navigate(ViewDestination.TODAY,        -1, context.getString(R.string.nav_today)));
        addItem(R.drawable.ic_week,   R.string.nav_next_7_days, () -> navigate(ViewDestination.NEXT_7_DAYS,  -1, context.getString(R.string.nav_next_7_days)));

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
        addItem(R.drawable.ic_today, R.string.nav_today,
                () -> navigate(ViewDestination.TODAY, -1, context.getString(R.string.nav_today)));
        addItem(R.drawable.ic_week, R.string.filter_this_week,
                () -> navigate(ViewDestination.THIS_WEEK, -1, context.getString(R.string.filter_this_week)));
    }

    public void buildFilterPanel() {
        tvPanelTitle.setText(R.string.nav_filters);
        clearContent();

        addItem(R.drawable.ic_calendar, R.string.filter_date_range, this::openDateRangePicker);
        addItem(R.drawable.ic_week, R.string.filter_this_week,
                () -> navigate(ViewDestination.THIS_WEEK, -1, context.getString(R.string.filter_this_week)));
        addItem(R.drawable.ic_filter, R.string.filter_unscheduled,
                () -> navigate(ViewDestination.UNSCHEDULED, -1, context.getString(R.string.filter_unscheduled)));
        addItem(R.drawable.ic_completed, R.string.filter_completed,
                () -> navigate(ViewDestination.COMPLETED, -1, context.getString(R.string.filter_completed)));
        addItem(R.drawable.ic_list, R.string.filter_clear,
                () -> navigate(ViewDestination.ALL_TASKS, -1, context.getString(R.string.nav_all_tasks)));
    }

    public void buildToolsPanel() {
        tvPanelTitle.setText(R.string.nav_tools);
        clearContent();

        addItem(R.drawable.ic_timer, R.string.nav_countdown, () -> {
            callback.closePanel();
            context.startActivity(new Intent(context, CountdownActivity.class));
        });
        addItem(R.drawable.ic_calendar, R.string.nav_calendar, () -> {
            callback.closePanel();
            context.startActivity(new Intent(context, CalendarActivity.class));
        });
        addItem(R.drawable.ic_list, R.string.nav_contacts, () -> {
            callback.closePanel();
            context.startActivity(new Intent(context, ContactsActivity.class));
        });
    }

    public void buildSettingsPanel() {
        tvPanelTitle.setText(R.string.nav_settings);
        clearContent();

        addItem(R.drawable.ic_settings, R.string.nav_settings_notification, () -> toast(R.string.nav_settings_notification));
        addItem(R.drawable.ic_filter,   R.string.nav_settings_language,     this::showLanguageDialog);
        addItem(R.drawable.ic_list,     R.string.nav_settings_backup,       () -> toast(R.string.nav_settings_backup));
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
        clearInjectedCategoryItems();
        int insertIndex = findCategoryInsertIndex();

        for (Category cat : categories) {
            int iconRes = getIconResIdByName(cat.getIconName());
            LinearLayout item = itemBuilder.buildCategoryItem(
                    iconRes,
                    cat.getName(),
                    () -> navigate(ViewDestination.CATEGORY, cat.getId(), cat.getName()),
                    anchor -> showCategoryOptions(anchor, cat)
            );
            item.setTag(TAG_CATEGORY_ITEM);
            panelContent.addView(item, insertIndex);
            insertIndex++;
        }
    }

    private void clearInjectedCategoryItems() {
        for (int i = panelContent.getChildCount() - 1; i >= 0; i--) {
            View child = panelContent.getChildAt(i);
            if (TAG_CATEGORY_ITEM.equals(child.getTag())) {
                panelContent.removeViewAt(i);
            }
        }
    }

    /**
     * Tìm vị trí trong panel để chèn category items.
     * Vị trí này nằm sau section title "Danh sách".
     */
    private int findCategoryInsertIndex() {
        // Mặc định: sau smart views (3) + divider (1) + section title (1) = index 5
        // Nhưng để chính xác hơn, đếm số view hiện tại
        return Math.max(0, panelContent.getChildCount() - 1);
    }

    /**
     * Hiển thị dialog thêm category mới.
     */
    private void showAddCategoryDialog() {
        if (context instanceof AppCompatActivity) {
            AppCompatActivity activity = (AppCompatActivity) context;
            AddCategoryDialog dialog = AddCategoryDialog.newInstance();
            dialog.setOnCategoryChangedListener(new AddCategoryDialog.OnCategoryChangedListener() {
                @Override
                public void onCategorySaved(Category category, boolean isEdit) {
                    buildTasksPanel();
                }

                @Override
                public void onCategoryDeleted(Category category) {
                    buildTasksPanel();
                }
            });
            dialog.show(activity.getSupportFragmentManager(), "ADD_CATEGORY");
        }
    }

    /**
     * Hiển thị dialog sửa category.
     */
    private void showEditCategoryDialog(Category category) {
        if (context instanceof AppCompatActivity) {
            AppCompatActivity activity = (AppCompatActivity) context;
            AddCategoryDialog dialog = AddCategoryDialog.newInstance(category);
            dialog.setOnCategoryChangedListener(new AddCategoryDialog.OnCategoryChangedListener() {
                @Override
                public void onCategorySaved(Category updatedCategory, boolean isEdit) {
                    buildTasksPanel();
                }

                @Override
                public void onCategoryDeleted(Category deletedCategory) {
                    buildTasksPanel();
                }
            });
            dialog.show(activity.getSupportFragmentManager(), "EDIT_CATEGORY");
        }
    }

    /**
     * Hiển thị menu tùy chọn cho category (sửa/xóa).
     */
    private void showCategoryOptions(View anchor, Category category) {
        PopupMenu popupMenu = new PopupMenu(context, anchor);
        popupMenu.getMenu().add(0, 1, 0, R.string.btn_edit);
        popupMenu.getMenu().add(0, 2, 1, R.string.btn_delete);
        popupMenu.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == 1) {
                showEditCategoryDialog(category);
                return true;
            }
            if (item.getItemId() == 2) {
                showDeleteCategoryDialog(category);
                return true;
            }
            return false;
        });
        popupMenu.show();
    }

    private void showDeleteCategoryDialog(Category category) {
        new MaterialAlertDialogBuilder(context)
                .setTitle(R.string.dialog_delete_category_title)
                .setMessage(R.string.dialog_delete_category_message)
                .setPositiveButton(R.string.btn_delete, (dialog, which) -> deleteCategory(category))
                .setNegativeButton(R.string.btn_cancel, null)
                .show();
    }

    private void deleteCategory(Category category) {
        AppDatabase db = AppDatabase.getDatabase(context);
        AppDatabase.databaseWriteExecutor.execute(() -> {
            db.categoryDao().delete(category);
            if (context instanceof AppCompatActivity) {
                ((AppCompatActivity) context).runOnUiThread(() -> {
                    Toast.makeText(context, R.string.msg_category_deleted, Toast.LENGTH_SHORT).show();
                    buildTasksPanel();
                });
            }
        });
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

    private void openDateRangePicker() {
        callback.closePanel();
        callback.openDateRangeFilter();
    }

    private void navigate(ViewDestination dest, int categoryId, String titleOverride) {
        callback.navigateTo(dest, categoryId, titleOverride);
        callback.closePanel();
    }

    private void addItem(int iconRes, int labelRes, Runnable onClick) {
        panelContent.addView(itemBuilder.buildItem(iconRes, context.getString(labelRes), onClick));
    }

    private void clearContent() {
        panelContent.removeAllViews();
    }

    private void toast(int msgRes) {
        Toast.makeText(context, msgRes, Toast.LENGTH_SHORT).show();
        callback.closePanel();
    }
}
