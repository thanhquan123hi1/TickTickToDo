package hcmute.edu.vn.ticktick.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import hcmute.edu.vn.ticktick.R;

/**
 * Adapter hiển thị grid icon để người dùng chọn khi tạo category mới.
 * Mỗi icon là một ImageView có thể chọn/bỏ chọn.
 */
public class IconPickerAdapter extends RecyclerView.Adapter<IconPickerAdapter.IconViewHolder> {

    /**
     * Danh sách tên icon (tên resource drawable, không có tiền tố "ic_").
     * Ví dụ: "work", "study", "travel", ...
     */
    private final List<String> iconNames;

    /**
     * Danh sách resource ID tương ứng với iconNames.
     * Ví dụ: R.drawable.ic_work, R.drawable.ic_study, ...
     */
    private final List<Integer> iconResIds;

    /**
     * Vị trí icon đang được chọn trong danh sách.
     * -1 nghĩa là chưa chọn icon nào.
     */
    private int selectedPosition = -1;

    /**
     * Callback khi người dùng chọn một icon.
     */
    public interface OnIconSelectedListener {
        void onIconSelected(String iconName, int iconResId);
    }

    private OnIconSelectedListener listener;

    /**
     * Constructor.
     * @param iconNames Danh sách tên icon (dùng để lưu vào database)
     * @param iconResIds Danh sách resource ID icon (dùng để hiển thị)
     */
    public IconPickerAdapter(List<String> iconNames, List<Integer> iconResIds) {
        this.iconNames = iconNames;
        this.iconResIds = iconResIds;
    }

    /**
     * Gán callback khi chọn icon.
     */
    public void setOnIconSelectedListener(OnIconSelectedListener listener) {
        this.listener = listener;
    }

    /**
     * Lấy tên icon đang được chọn.
     * @return Tên icon hoặc null nếu chưa chọn.
     */
    public String getSelectedIconName() {
        if (selectedPosition >= 0 && selectedPosition < iconNames.size()) {
            return iconNames.get(selectedPosition);
        }
        return null;
    }

    @NonNull
    @Override
    public IconViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate layout item_icon_picker.xml
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_icon_picker, parent, false);
        return new IconViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull IconViewHolder holder, int position) {
        int iconRes = iconResIds.get(position);
        holder.ivIcon.setImageResource(iconRes);

        // Đánh dấu trạng thái selected
        holder.ivIcon.setSelected(position == selectedPosition);

        // Đổi màu icon theo trạng thái
        int tintColor = holder.itemView.getContext().getResources().getColor(
                position == selectedPosition ? R.color.accent : R.color.text_secondary, null);
        holder.ivIcon.setColorFilter(tintColor);

        // Xử lý click chọn icon
        holder.itemView.setOnClickListener(v -> {
            int oldPosition = selectedPosition;
            selectedPosition = holder.getBindingAdapterPosition();

            // Cập nhật UI cho item cũ và mới
            if (oldPosition >= 0) notifyItemChanged(oldPosition);
            notifyItemChanged(selectedPosition);

            // Gọi callback
            if (listener != null) {
                listener.onIconSelected(iconNames.get(selectedPosition), iconRes);
            }
        });
    }

    @Override
    public int getItemCount() {
        return iconResIds.size();
    }

    /**
     * ViewHolder cho mỗi icon trong grid.
     */
    static class IconViewHolder extends RecyclerView.ViewHolder {
        ImageView ivIcon;

        IconViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.iv_icon);
        }
    }
}

