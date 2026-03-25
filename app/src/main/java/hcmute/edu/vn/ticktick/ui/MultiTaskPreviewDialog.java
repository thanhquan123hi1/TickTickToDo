package hcmute.edu.vn.ticktick.ui;

import android.content.Context;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import androidx.appcompat.app.AlertDialog;

import java.util.ArrayList;
import java.util.List;

import hcmute.edu.vn.ticktick.utils.OCRTaskParser;

public class MultiTaskPreviewDialog {

    public interface OnTasksSavedListener {
        void onTasksSaved(List<OCRTaskParser.TaskDraft> editedDrafts);
    }

    public static void show(Context context, List<OCRTaskParser.TaskDraft> drafts, OnTasksSavedListener listener) {
        ScrollView scrollView = new ScrollView(context);
        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) (16 * context.getResources().getDisplayMetrics().density);
        container.setPadding(padding, padding, padding, padding);
        scrollView.addView(container);

        List<DraftViewRow> viewRows = new ArrayList<>();

        for (OCRTaskParser.TaskDraft draft : drafts) {
            LinearLayout row = new LinearLayout(context);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(0, 0, 0, padding);

            CheckBox checkBox = new CheckBox(context);
            checkBox.setChecked(true);
            row.addView(checkBox);

            EditText editText = new EditText(context);
            editText.setText(draft.title);
            editText.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            ));
            row.addView(editText);

            container.addView(row);
            viewRows.add(new DraftViewRow(draft, checkBox, editText));
        }

        new AlertDialog.Builder(context)
                .setTitle("Lưu các tác vụ đã quét")
                .setView(scrollView)
                .setPositiveButton("Lưu tất cả", (dialog, which) -> {
                    List<OCRTaskParser.TaskDraft> selectedDrafts = new ArrayList<>();
                    for (DraftViewRow row : viewRows) {
                        if (row.checkBox.isChecked()) {
                            // Update title to whatever user edited
                            row.draft.title = row.editText.getText().toString().trim();
                            if (!row.draft.title.isEmpty()) {
                                selectedDrafts.add(row.draft);
                            }
                        }
                    }
                    if (listener != null) {
                        listener.onTasksSaved(selectedDrafts);
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private static class DraftViewRow {
        OCRTaskParser.TaskDraft draft;
        CheckBox checkBox;
        EditText editText;

        DraftViewRow(OCRTaskParser.TaskDraft draft, CheckBox checkBox, EditText editText) {
            this.draft = draft;
            this.checkBox = checkBox;
            this.editText = editText;
        }
    }
}

