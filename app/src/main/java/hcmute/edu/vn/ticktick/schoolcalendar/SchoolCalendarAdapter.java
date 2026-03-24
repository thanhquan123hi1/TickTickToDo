package hcmute.edu.vn.ticktick.schoolcalendar;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import hcmute.edu.vn.ticktick.R;
import hcmute.edu.vn.ticktick.database.SchoolCalendarEventEntity;

public class SchoolCalendarAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_HEADER = 0;
    private static final int VIEW_TYPE_EVENT = 1;

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm", Locale.of("vi", "VN"));
    private static final DateTimeFormatter DATE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM HH:mm", Locale.of("vi", "VN"));

    private final List<Object> items = new ArrayList<>();
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onEventClick(SchoolCalendarEventEntity event);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setGroupedData(List<SchoolCalendarEventEntity> overdue,
                               List<SchoolCalendarEventEntity> today,
                               List<SchoolCalendarEventEntity> nextThreeDays) {
        items.clear();

        if (today != null && !today.isEmpty()) {
            items.add(new SectionHeader("Hôm nay", today.size()));
            items.addAll(today);
        }

        if (nextThreeDays != null && !nextThreeDays.isEmpty()) {
            items.add(new SectionHeader("3 ngày tới", nextThreeDays.size()));
            items.addAll(nextThreeDays);
        }

        if (overdue != null && !overdue.isEmpty()) {
            items.add(new SectionHeader("Quá hạn", overdue.size()));
            items.addAll(overdue);
        }

        notifyDataSetChanged();
    }

    public boolean isEmpty() {
        for (Object item : items) {
            if (item instanceof SchoolCalendarEventEntity) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position) instanceof SectionHeader ? VIEW_TYPE_HEADER : VIEW_TYPE_EVENT;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == VIEW_TYPE_HEADER) {
            return new HeaderViewHolder(inflater.inflate(R.layout.item_section_header, parent, false));
        }
        EventViewHolder holder = new EventViewHolder(inflater.inflate(R.layout.item_school_calendar_event, parent, false));
        holder.itemView.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos != RecyclerView.NO_POSITION && listener != null && items.get(pos) instanceof SchoolCalendarEventEntity) {
                listener.onEventClick((SchoolCalendarEventEntity) items.get(pos));
            }
        });
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof HeaderViewHolder) {
            ((HeaderViewHolder) holder).bind((SectionHeader) items.get(position));
        } else {
            ((EventViewHolder) holder).bind((SchoolCalendarEventEntity) items.get(position));
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvTitle;
        private final TextView tvCount;

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

    static class EventViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvTitle;
        private final TextView tvTime;
        private final TextView tvCategory;

        EventViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_school_event_title);
            tvTime = itemView.findViewById(R.id.tv_school_event_time);
            tvCategory = itemView.findViewById(R.id.tv_school_event_category);
        }

        void bind(SchoolCalendarEventEntity event) {
            tvTitle.setText(event.getTitle());
            tvTime.setText(formatTimeLine(itemView, event));

            String category = event.getCategory();
            if (category != null && !category.trim().isEmpty()) {
                tvCategory.setText(category);
                tvCategory.setVisibility(View.VISIBLE);
            } else {
                tvCategory.setVisibility(View.GONE);
            }
        }

        private String formatTimeLine(View itemView, SchoolCalendarEventEntity event) {
            long reference = event.getReferenceTimeMillis();
            if (reference <= 0) {
                reference = event.getStartTimeMillis();
            }
            if (reference <= 0) {
                return itemView.getContext().getString(R.string.school_calendar_time_unknown);
            }

            LocalDateTime dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(reference), ZoneId.systemDefault());
            LocalDateTime now = LocalDateTime.now();

            if (dateTime.toLocalDate().isEqual(now.toLocalDate())) {
                return TIME_FORMAT.format(dateTime);
            }

            if (reference < System.currentTimeMillis()) {
                long days = Duration.between(dateTime, now).toDays();
                if (days <= 0) {
                    days = 1;
                }
                return itemView.getContext().getString(R.string.school_calendar_overdue_days, days);
            }

            return DATE_TIME_FORMAT.format(dateTime);
        }
    }

    static class SectionHeader {
        final String title;
        final int count;

        SectionHeader(String title, int count) {
            this.title = title;
            this.count = count;
        }
    }
}
