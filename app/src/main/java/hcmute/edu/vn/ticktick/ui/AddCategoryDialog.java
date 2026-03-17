package hcmute.edu.vn.ticktick.ui;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Arrays;
import java.util.List;

import hcmute.edu.vn.ticktick.R;
import hcmute.edu.vn.ticktick.adapter.IconPickerAdapter;
import hcmute.edu.vn.ticktick.database.AppDatabase;
import hcmute.edu.vn.ticktick.database.Category;

public class AddCategoryDialog extends DialogFragment {

    public interface OnCategoryChangedListener {
        void onCategorySaved(Category category, boolean isEdit);
        void onCategoryDeleted(Category category);
    }

    private OnCategoryChangedListener listener;
    private TextInputEditText etCategoryName;
    private RecyclerView recyclerIcons;
    private IconPickerAdapter iconAdapter;
    private String selectedIconName = null;
    private Category editingCategory;

    private static final List<String> ICON_NAMES = Arrays.asList(
            "ic_work",
            "ic_study",
            "ic_travel",
            "ic_list",
            "ic_star",
            "ic_calendar",
            "ic_timer",
            "ic_filter",
            "ic_check",
            "ic_today",
            "ic_week"
    );

    private static final List<Integer> ICON_RES_IDS = Arrays.asList(
            R.drawable.ic_work,
            R.drawable.ic_study,
            R.drawable.ic_travel,
            R.drawable.ic_list,
            R.drawable.ic_star,
            R.drawable.ic_calendar,
            R.drawable.ic_timer,
            R.drawable.ic_filter,
            R.drawable.ic_check,
            R.drawable.ic_today,
            R.drawable.ic_week
    );

    public static AddCategoryDialog newInstance() {
        return new AddCategoryDialog();
    }

    public static AddCategoryDialog newInstance(@NonNull Category category) {
        AddCategoryDialog dialog = new AddCategoryDialog();
        dialog.editingCategory = category;
        return dialog;
    }

    public void setOnCategoryChangedListener(OnCategoryChangedListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = getLayoutInflater().inflate(R.layout.dialog_add_category, null);

        etCategoryName = view.findViewById(R.id.et_category_name);
        recyclerIcons = view.findViewById(R.id.recycler_icons);
        setupIconPicker();
        bindEditingState();

        AlertDialog.Builder builder = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(isEditMode() ? R.string.category_dialog_title_edit : R.string.category_dialog_title_add)
                .setView(view)
                .setPositiveButton(R.string.btn_save, (dialog, which) -> saveCategory())
                .setNegativeButton(R.string.btn_cancel, null);

        if (isEditMode()) {
            builder.setNeutralButton(R.string.btn_delete, (dialog, which) -> confirmDeleteCategory());
        }

        return builder.create();
    }

    private boolean isEditMode() {
        return editingCategory != null;
    }

    private void setupIconPicker() {
        recyclerIcons.setLayoutManager(new GridLayoutManager(requireContext(), 4));
        iconAdapter = new IconPickerAdapter(ICON_NAMES, ICON_RES_IDS);
        iconAdapter.setOnIconSelectedListener((iconName, iconResId) -> selectedIconName = iconName);
        recyclerIcons.setAdapter(iconAdapter);
    }

    private void bindEditingState() {
        if (!isEditMode()) return;
        etCategoryName.setText(editingCategory.getName());
        selectedIconName = editingCategory.getIconName();
        iconAdapter.setSelectedIconByName(selectedIconName);
    }

    private void saveCategory() {
        String name = etCategoryName.getText() != null
                ? etCategoryName.getText().toString().trim()
                : "";

        if (name.isEmpty()) {
            Toast.makeText(requireContext(), R.string.error_category_name_required, Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedIconName == null) {
            Toast.makeText(requireContext(), R.string.error_category_icon_required, Toast.LENGTH_SHORT).show();
            return;
        }

        final boolean isEdit = isEditMode();
        final Category category = isEdit ? editingCategory : new Category();
        category.setName(name);
        category.setIconName(selectedIconName);

        AppDatabase db = AppDatabase.getDatabase(requireContext());
        AppDatabase.databaseWriteExecutor.execute(() -> {
            if (isEdit) {
                db.categoryDao().update(category);
            } else {
                long id = db.categoryDao().insert(category);
                category.setId((int) id);
            }

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    Toast.makeText(
                            requireContext(),
                            isEdit ? R.string.msg_category_updated : R.string.msg_category_added,
                            Toast.LENGTH_SHORT
                    ).show();
                    if (listener != null) {
                        listener.onCategorySaved(category, isEdit);
                    }
                });
            }
        });
    }

    private void confirmDeleteCategory() {
        if (!isEditMode()) return;
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.dialog_delete_category_title)
                .setMessage(R.string.dialog_delete_category_message)
                .setPositiveButton(R.string.btn_delete, (dialog, which) -> deleteCategory())
                .setNegativeButton(R.string.btn_cancel, null)
                .show();
    }

    private void deleteCategory() {
        if (!isEditMode()) return;
        Category category = editingCategory;
        AppDatabase db = AppDatabase.getDatabase(requireContext());
        AppDatabase.databaseWriteExecutor.execute(() -> {
            db.categoryDao().delete(category);
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), R.string.msg_category_deleted, Toast.LENGTH_SHORT).show();
                    if (listener != null) {
                        listener.onCategoryDeleted(category);
                    }
                });
            }
        });
    }
}
