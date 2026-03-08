package hcmute.edu.vn.ticktick.navigation;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.content.res.AppCompatResources;

import hcmute.edu.vn.ticktick.R;

/**
 * Stateless factory that creates individual panel items, dividers, and section titles.
 * <p>
 * Single Responsibility: knows only how to inflate/style panel rows.
 * Open/Closed: new item styles can be added without touching existing methods.
 */
public class PanelItemBuilder {

    private final Context context;

    public PanelItemBuilder(Context context) {
        this.context = context;
    }

    /** Creates a tappable row with an icon and a label. */
    public LinearLayout buildItem(int iconRes, String title, Runnable onClick) {
        LinearLayout item = buildBaseItem(dp(2));
        item.addView(buildIcon(iconRes));
        item.addView(buildLabel(title));
        item.setOnClickListener(v -> onClick.run());
        return item;
    }

    /** Creates a tappable row for category with an icon, label and a more options menu. */
    public LinearLayout buildCategoryItem(int iconRes,
                                          String title,
                                          Runnable onClick,
                                          View.OnClickListener onMoreClick) {
        LinearLayout item = buildBaseItem(dp(2));
        item.addView(buildIcon(iconRes));
        item.addView(buildLabel(title));

        ImageView more = new ImageView(context);
        LinearLayout.LayoutParams moreLp = new LinearLayout.LayoutParams(dp(24), dp(24));
        more.setLayoutParams(moreLp);
        more.setImageResource(R.drawable.ic_more_vert);
        more.setColorFilter(context.getResources().getColor(R.color.text_secondary, null));
        more.setPadding(dp(2), dp(2), dp(2), dp(2));
        more.setOnClickListener(onMoreClick);
        item.addView(more);

        item.setOnClickListener(v -> onClick.run());
        return item;
    }

    /** Creates a thin horizontal divider. */
    public View buildDivider() {
        View div = new View(context);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(1));
        lp.setMargins(dp(16), dp(8), dp(16), dp(8));
        div.setLayoutParams(lp);
        div.setBackgroundColor(context.getResources().getColor(R.color.divider, null));
        return div;
    }

    /** Creates an ALL-CAPS secondary-color section title. */
    public TextView buildSectionTitle(String title) {
        TextView tv = new TextView(context);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(dp(16), dp(4), dp(16), dp(4));
        tv.setLayoutParams(lp);
        tv.setText(title);
        tv.setTextSize(12);
        tv.setTextColor(context.getResources().getColor(R.color.text_secondary, null));
        tv.setAllCaps(true);
        return tv;
    }

    /**
     * Tạo nút "+ Thêm danh mục" với style đặc biệt.
     * Icon dấu cộng màu accent, text màu accent.
     * @param title Text hiển thị (ví dụ: "+ Thêm danh mục")
     * @param onClick Hành động khi click
     * @return LinearLayout item
     */
    public LinearLayout buildAddCategoryItem(String title, Runnable onClick) {
        LinearLayout item = buildBaseItem(dp(8));

        ImageView icon = new ImageView(context);
        int size = dp(22);
        LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(size, size);
        icon.setLayoutParams(iconLp);
        icon.setImageResource(R.drawable.ic_add);
        icon.setColorFilter(context.getResources().getColor(R.color.accent, null));
        item.addView(icon);

        TextView tv = new TextView(context);
        LinearLayout.LayoutParams tvLp = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        tvLp.setMarginStart(dp(14));
        tv.setLayoutParams(tvLp);
        tv.setText(title);
        tv.setTextSize(15);
        tv.setTextColor(context.getResources().getColor(R.color.accent, null));
        item.addView(tv);

        item.setOnClickListener(v -> onClick.run());
        return item;
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private LinearLayout buildBaseItem(int topMargin) {
        LinearLayout item = new LinearLayout(context);
        item.setOrientation(LinearLayout.HORIZONTAL);
        item.setGravity(Gravity.CENTER_VERTICAL);

        int hPad = dp(16);
        int vPad = dp(12);
        item.setPadding(hPad, vPad, hPad, vPad);
        item.setBackground(AppCompatResources.getDrawable(context, R.drawable.bg_panel_item_ripple));
        item.setClickable(true);
        item.setFocusable(true);

        LinearLayout.LayoutParams itemLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        itemLp.setMargins(0, topMargin, 0, dp(2));
        item.setLayoutParams(itemLp);
        return item;
    }

    private ImageView buildIcon(int iconRes) {
        ImageView icon = new ImageView(context);
        int size = dp(22);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
        icon.setLayoutParams(lp);
        icon.setImageResource(iconRes);
        icon.setColorFilter(context.getResources().getColor(R.color.nav_item_icon_tint, null));
        return icon;
    }

    private TextView buildLabel(String title) {
        TextView tv = new TextView(context);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        lp.setMarginStart(dp(14));
        tv.setLayoutParams(lp);
        tv.setText(title);
        tv.setTextSize(15);
        tv.setTextColor(context.getResources().getColor(R.color.text_primary, null));
        return tv;
    }

    private int dp(int dp) {
        return (int) (dp * context.getResources().getDisplayMetrics().density);
    }
}
