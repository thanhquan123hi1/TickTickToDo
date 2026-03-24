package hcmute.edu.vn.ticktick.music;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import hcmute.edu.vn.ticktick.R;

public class MusicTrackAdapter extends RecyclerView.Adapter<MusicTrackAdapter.TrackViewHolder> {

    public interface OnTrackClickListener {
        void onTrackClicked(ScannedAudioItem item);
    }

    private final OnTrackClickListener listener;
    private final List<ScannedAudioItem> items = new ArrayList<>();
    private String selectedUri;

    public MusicTrackAdapter(OnTrackClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<ScannedAudioItem> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    public void setSelectedUri(String selectedUri) {
        this.selectedUri = selectedUri;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TrackViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_music_track, parent, false);
        return new TrackViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TrackViewHolder holder, int position) {
        ScannedAudioItem item = items.get(position);
        holder.bind(item, selectedUri, listener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class TrackViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvTitle;
        private final TextView tvSubtitle;
        private final TextView tvSelected;

        TrackViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_track_title);
            tvSubtitle = itemView.findViewById(R.id.tv_track_subtitle);
            tvSelected = itemView.findViewById(R.id.tv_track_selected);
        }

        void bind(ScannedAudioItem item, String selectedUri, OnTrackClickListener listener) {
            String title = item.getTitle();
            if (title == null || title.trim().isEmpty()) {
                title = itemView.getContext().getString(R.string.countdown_sound_unknown_title);
            }

            String artist = item.getArtist();
            if (artist == null || artist.trim().isEmpty() || "<unknown>".equalsIgnoreCase(artist)) {
                artist = itemView.getContext().getString(R.string.countdown_sound_unknown_artist);
            }

            tvTitle.setText(title);
            tvSubtitle.setText(buildSubtitle(artist, item.getDurationMs()));

            boolean isSelected = false;
            Uri uri = item.getUri();
            if (uri != null && selectedUri != null) {
                isSelected = selectedUri.equals(uri.toString());
            }
            tvSelected.setVisibility(isSelected ? View.VISIBLE : View.GONE);

            itemView.setOnClickListener(v -> listener.onTrackClicked(item));
        }

        private String buildSubtitle(String artist, long durationMs) {
            long totalSeconds = Math.max(0L, durationMs / 1000L);
            long minutes = totalSeconds / 60L;
            long seconds = totalSeconds % 60L;
            return artist + "  -  " + String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
        }
    }
}


