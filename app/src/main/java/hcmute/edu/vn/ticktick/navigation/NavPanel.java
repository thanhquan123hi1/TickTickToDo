package hcmute.edu.vn.ticktick.navigation;

import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.ColorRes;

import hcmute.edu.vn.ticktick.R;

/**
 * Manages the expanded side-panel: open, close, and background-tint of the nav rail.
 * <p>
 * Single Responsibility: knows only how to animate / show / hide the panel.
 */
public class NavPanel {

    public interface Host {
        LinearLayout getNavRail();
        LinearLayout getExpandedPanel();
        View getOverlayDim();
        int resolveColor(@ColorRes int resId);
    }

    private static final int ANIM_DURATION_OPEN  = 250;
    private static final int ANIM_DURATION_CLOSE = 200;

    private final Host host;

    public NavPanel(Host host) {
        this.host = host;
    }

    public boolean isOpen() {
        return host.getExpandedPanel().getVisibility() == View.VISIBLE;
    }

    public void open() {
        LinearLayout panel = host.getExpandedPanel();
        View overlay = host.getOverlayDim();

        overlay.setVisibility(View.VISIBLE);
        panel.setVisibility(View.VISIBLE);

        host.getNavRail().setBackgroundColor(
                host.resolveColor(R.color.rail_bg_active));

        panel.setTranslationX(-panel.getWidth());
        panel.animate().translationX(0).setDuration(ANIM_DURATION_OPEN).start();

        overlay.setAlpha(0f);
        overlay.animate().alpha(1f).setDuration(ANIM_DURATION_OPEN).start();
    }

    public void close() {
        LinearLayout panel = host.getExpandedPanel();
        View overlay = host.getOverlayDim();

        host.getNavRail().setBackgroundColor(
                host.resolveColor(R.color.surface));

        panel.animate()
                .translationX(-panel.getWidth())
                .setDuration(ANIM_DURATION_CLOSE)
                .withEndAction(() -> panel.setVisibility(View.GONE))
                .start();

        overlay.animate()
                .alpha(0f)
                .setDuration(ANIM_DURATION_CLOSE)
                .withEndAction(() -> overlay.setVisibility(View.GONE))
                .start();
    }
}
