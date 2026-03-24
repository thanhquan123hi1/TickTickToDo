package hcmute.edu.vn.ticktick.calendar;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import hcmute.edu.vn.ticktick.R;

public class CalendarMonthAdapter extends RecyclerView.Adapter<CalendarMonthAdapter.DayViewHolder> {

    public interface OnDayClickListener {
        void onDayClick(DayCell cell);
    }

    private final List<DayCell> items = new ArrayList<>();
    private final OnDayClickListener listener;

    public CalendarMonthAdapter(OnDayClickListener listener) {
        this.listener = listener;
    }

    public void submit(List<DayCell> cells) {
        items.clear();
        if (cells != null) {
            items.addAll(cells);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public DayViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_calendar_day, parent, false);
        return new DayViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DayViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class DayViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvDay;
        private final View dot;

        DayViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDay = itemView.findViewById(R.id.tv_calendar_day_number);
            dot = itemView.findViewById(R.id.view_calendar_day_dot);
        }

        void bind(DayCell cell) {
            tvDay.setText(String.valueOf(cell.dayOfMonth));
            tvDay.setActivated(cell.selected);

            if (!cell.inCurrentMonth) {
                tvDay.setTextColor(itemView.getContext().getResources().getColor(R.color.text_hint, null));
                tvDay.setBackgroundResource(android.R.color.transparent);
            } else if (cell.selected) {
                tvDay.setTextColor(itemView.getContext().getResources().getColor(R.color.white, null));
                tvDay.setBackgroundResource(R.drawable.bg_calendar_day_selected);
            } else if (cell.today) {
                tvDay.setTextColor(itemView.getContext().getResources().getColor(R.color.primary_dark, null));
                tvDay.setBackgroundResource(R.drawable.bg_calendar_day_today);
            } else {
                tvDay.setTextColor(itemView.getContext().getResources().getColor(R.color.text_primary, null));
                tvDay.setBackgroundResource(android.R.color.transparent);
            }

            dot.setVisibility(cell.hasTask && cell.inCurrentMonth ? View.VISIBLE : View.INVISIBLE);
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDayClick(cell);
                }
            });
        }
    }

    public static class DayCell {
        public final long dayStartMillis;
        public final int dayOfMonth;
        public final boolean inCurrentMonth;
        public final boolean selected;
        public final boolean today;
        public final boolean hasTask;

        public DayCell(long dayStartMillis,
                       int dayOfMonth,
                       boolean inCurrentMonth,
                       boolean selected,
                       boolean today,
                       boolean hasTask) {
            this.dayStartMillis = dayStartMillis;
            this.dayOfMonth = dayOfMonth;
            this.inCurrentMonth = inCurrentMonth;
            this.selected = selected;
            this.today = today;
            this.hasTask = hasTask;
        }
    }
}

