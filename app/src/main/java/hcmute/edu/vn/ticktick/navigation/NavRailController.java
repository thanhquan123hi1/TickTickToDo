package hcmute.edu.vn.ticktick.navigation;

import android.widget.ImageButton;

import androidx.appcompat.content.res.AppCompatResources;

import hcmute.edu.vn.ticktick.R;

/**
 * Manages the active/inactive visual state of navigation rail buttons.
 * <p>
 * Single Responsibility: knows only how to highlight / reset rail buttons.
 */
public class NavRailController {

    private final ImageButton[] allButtons;
    private ImageButton activeButton;

    public NavRailController(ImageButton... buttons) {
        this.allButtons = buttons;
    }

    /** Marks {@code btn} as active and resets all others. Pass {@code null} to clear all. */
    public void setActive(ImageButton btn) {
        for (ImageButton b : allButtons) {
            b.setBackground(AppCompatResources.getDrawable(
                    b.getContext(), R.drawable.bg_rail_btn_ripple));
        }
        if (btn != null) {
            btn.setBackground(AppCompatResources.getDrawable(
                    btn.getContext(), R.drawable.bg_rail_btn_selected));
        }
        activeButton = btn;
    }

    public ImageButton getActiveButton() {
        return activeButton;
    }
}
