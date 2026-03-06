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

import com.google.android.material.textfield.TextInputEditText;

import java.util.Arrays;
import java.util.List;

import hcmute.edu.vn.ticktick.R;
import hcmute.edu.vn.ticktick.adapter.IconPickerAdapter;
import hcmute.edu.vn.ticktick.database.AppDatabase;
import hcmute.edu.vn.ticktick.database.Category;

/**
 * Dialog cho phép người dùng thêm một danh mục (category) mới.
 * Người dùng nhập tên và chọn icon từ grid.
 */
public class AddCategoryDialog extends DialogFragment {

    /**
     * Callback khi category được thêm thành công.
     */
    public interface OnCategoryAddedListener {
        void onCategoryAdded(Category category);
    }

    private OnCategoryAddedListener listener;

    // Views trong dialog
    private TextInputEditText etCategoryName;
    private RecyclerView recyclerIcons;
    private IconPickerAdapter iconAdapter;

    // Tên icon được chọn (lưu vào database)
    private String selectedIconName = null;

    /**
     * Danh sách tên icon có sẵn trong app.
     * Tên này khớp với tên file trong drawable (bỏ tiền tố "ic_").
     */
    private static final List<String> ICON_NAMES = Arrays.asList(
            "ic_work",
            "ic_study",
            "ic_travel",
            "ic_list",
            "ic_star",
            "ic_inbox",
            "ic_calendar",
            "ic_timer",
            "ic_filter",
            "ic_check",
            "ic_today",
            "ic_week"
    );

    /**
     * Danh sách resource ID tương ứng với ICON_NAMES.
     */
    private static final List<Integer> ICON_RES_IDS = Arrays.asList(
            R.drawable.ic_work,
            R.drawable.ic_study,
            R.drawable.ic_travel,
            R.drawable.ic_list,
            R.drawable.ic_star,
            R.drawable.ic_inbox,
            R.drawable.ic_calendar,
            R.drawable.ic_timer,
            R.drawable.ic_filter,
            R.drawable.ic_check,
            R.drawable.ic_today,
            R.drawable.ic_week
    );

    /**
     * Tạo instance mới của dialog.
     */
    public static AddCategoryDialog newInstance() {
        return new AddCategoryDialog();
    }

    /**
     * Gán callback khi thêm category thành công.
     */
    public void setOnCategoryAddedListener(OnCategoryAddedListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        // Inflate layout dialog
        View view = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_add_category, null);

        // Bind views
        etCategoryName = view.findViewById(R.id.et_category_name);
        recyclerIcons = view.findViewById(R.id.recycler_icons);

        // Setup icon picker grid (4 cột)
        setupIconPicker();

        // Tạo AlertDialog với nút Lưu và Hủy
        return new AlertDialog.Builder(requireContext())
                .setTitle("Thêm danh mục mới")
                .setView(view)
                .setPositiveButton("Lưu", (dialog, which) -> saveCategory())
                .setNegativeButton("Hủy", null)
                .create();
    }

    /**
     * Cấu hình RecyclerView hiển thị grid icon.
     */
    private void setupIconPicker() {
        // Grid 4 cột
        recyclerIcons.setLayoutManager(new GridLayoutManager(requireContext(), 4));

        // Tạo adapter với danh sách icon
        iconAdapter = new IconPickerAdapter(ICON_NAMES, ICON_RES_IDS);
        iconAdapter.setOnIconSelectedListener((iconName, iconResId) -> {
            selectedIconName = iconName;
        });

        recyclerIcons.setAdapter(iconAdapter);
    }

    /**
     * Validate và lưu category mới vào database.
     */
    private void saveCategory() {
        // Lấy tên từ input
        String name = etCategoryName.getText() != null
                ? etCategoryName.getText().toString().trim()
                : "";

        // Validate tên không rỗng
        if (name.isEmpty()) {
            Toast.makeText(requireContext(), "Vui lòng nhập tên danh mục", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate đã chọn icon
        if (selectedIconName == null) {
            Toast.makeText(requireContext(), "Vui lòng chọn biểu tượng", Toast.LENGTH_SHORT).show();
            return;
        }

        // Tạo category mới
        Category category = new Category(name, selectedIconName);

        // Lưu vào database trên background thread
        AppDatabase db = AppDatabase.getDatabase(requireContext());
        AppDatabase.databaseWriteExecutor.execute(() -> {
            long id = db.categoryDao().insert(category);
            category.setId((int) id);

            // Gọi callback trên main thread
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), "Đã thêm danh mục: " + name, Toast.LENGTH_SHORT).show();
                    if (listener != null) {
                        listener.onCategoryAdded(category);
                    }
                });
            }
        });
    }
}

