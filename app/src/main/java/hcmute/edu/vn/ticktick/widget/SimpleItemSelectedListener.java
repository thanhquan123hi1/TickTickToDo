package hcmute.edu.vn.ticktick.widget;

import android.view.View;
import android.widget.AdapterView;

public class SimpleItemSelectedListener implements AdapterView.OnItemSelectedListener {

    public interface OnSelected {
        void onSelected(int position);
    }

    private final OnSelected callback;

    public SimpleItemSelectedListener(OnSelected callback) {
        this.callback = callback;
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        callback.onSelected(position);
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        // no-op
    }
}

