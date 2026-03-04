package hcmute.edu.vn.ticktick.adapter;

import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import hcmute.edu.vn.ticktick.R;
import hcmute.edu.vn.ticktick.database.Task;
import hcmute.edu.vn.ticktick.ui.DateUtils;

public class TaskAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_HEADER = 0;
    private static final int VIEW_TYPE_TASK = 1;

    private final List<Object> items = new ArrayList<>();
    private OnTaskClickListener listener;
    private OnTaskCheckListener checkListener;

    public interface OnTaskClickListener {
        void onTaskClick(Task task);
    }

    public interface OnTaskCheckListener {
        void onTaskCheckChanged(Task task, boolean isChecked);
    }

    public void setOnTaskClickListener(OnTaskClickListener listener) {
        this.listener = listener;
    }

    public void setOnTaskCheckListener(OnTaskCheckListener checkListener) {
        this.checkListener = checkListener;
    }

    /**
     * Set data with section grouping: Hôm nay, Ngày mai, Sắp tới
     */
    public void setGroupedData(List<Task> todayTasks, List<Task> tomorrowTasks, List<Task> upcomingTasks) {
        items.clear();

        if (todayTasks != null && !todayTasks.isEmpty()) {
            items.add(new SectionHeader("Hôm nay", todayTasks.size()));
            items.addAll(todayTasks);
        }

        if (tomorrowTasks != null && !tomorrowTasks.isEmpty()) {
            items.add(new SectionHeader("Ngày mai", tomorrowTasks.size()));
            items.addAll(tomorrowTasks);
        }

        if (upcomingTasks != null && !upcomingTasks.isEmpty()) {
            items.add(new SectionHeader("Sắp tới", upcomingTasks.size()));
            items.addAll(upcomingTasks);
        }

        notifyDataSetChanged();
    }

    /**
     * Set flat data (no sections) for filtered views
     */
    public void setFlatData(String sectionTitle, List<Task> tasks) {
        items.clear();
        if (tasks != null && !tasks.isEmpty()) {
            items.add(new SectionHeader(sectionTitle, tasks.size()));
            items.addAll(tasks);
        }
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position) instanceof SectionHeader ? VIEW_TYPE_HEADER : VIEW_TYPE_TASK;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == VIEW_TYPE_HEADER) {
            View view = inflater.inflate(R.layout.item_section_header, parent, false);
            return new HeaderViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.item_task, parent, false);
            return new TaskViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof HeaderViewHolder) {
            SectionHeader header = (SectionHeader) items.get(position);
            ((HeaderViewHolder) holder).bind(header);
        } else if (holder instanceof TaskViewHolder) {
            Task task = (Task) items.get(position);
            ((TaskViewHolder) holder).bind(task);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public boolean isEmpty() {
        // Check if there are any Task items (not just headers)
        for (Object item : items) {
            if (item instanceof Task) return false;
        }
        return true;
    }

    // ViewHolder for section headers
    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvCount;

        HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_section_title);
            tvCount = itemView.findViewById(R.id.tv_section_count);
        }

        void bind(SectionHeader header) {
            tvTitle.setText(header.title);
            tvCount.setText(String.valueOf(header.count));
        }
    }

    // ViewHolder for tasks
    class TaskViewHolder extends RecyclerView.ViewHolder {
        CheckBox cbTask;
        TextView tvTitle, tvDescription, tvTime;

        TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            cbTask = itemView.findViewById(R.id.cb_task);
            tvTitle = itemView.findViewById(R.id.tv_task_title);
            tvDescription = itemView.findViewById(R.id.tv_task_description);
            tvTime = itemView.findViewById(R.id.tv_task_time);
        }

        void bind(Task task) {
            tvTitle.setText(task.getTitle());

            // Strikethrough if completed
            if (task.isCompleted()) {
                tvTitle.setPaintFlags(tvTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                tvTitle.setAlpha(0.5f);
            } else {
                tvTitle.setPaintFlags(tvTitle.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
                tvTitle.setAlpha(1.0f);
            }

            // Description
            if (task.getDescription() != null && !task.getDescription().isEmpty()) {
                tvDescription.setText(task.getDescription());
                tvDescription.setVisibility(View.VISIBLE);
            } else {
                tvDescription.setVisibility(View.GONE);
            }

            // Date/Time display
            String displayText = DateUtils.getDisplayDateOrTime(task.getDueDate(), task.getDueTime());
            if (!displayText.isEmpty()) {
                tvTime.setText(displayText);
                tvTime.setVisibility(View.VISIBLE);

                // Color overdue tasks red
                if (task.getDueDate() > 0 && task.getDueDate() < System.currentTimeMillis() && !task.isCompleted()) {
                    tvTime.setTextColor(itemView.getContext().getResources().getColor(R.color.overdue, null));
                } else {
                    tvTime.setTextColor(itemView.getContext().getResources().getColor(R.color.primary, null));
                }
            } else {
                tvTime.setVisibility(View.GONE);
            }

            // Checkbox
            cbTask.setOnCheckedChangeListener(null);
            cbTask.setChecked(task.isCompleted());
            cbTask.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (checkListener != null) {
                    checkListener.onTaskCheckChanged(task, isChecked);
                }
            });

            // Click listener
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onTaskClick(task);
                }
            });
        }
    }

    // Section header model
    public static class SectionHeader {
        public final String title;
        public final int count;

        public SectionHeader(String title, int count) {
            this.title = title;
            this.count = count;
        }
    }
}
