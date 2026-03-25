package hcmute.edu.vn.ticktick.music;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Collections;
import java.util.List;
import android.media.MediaPlayer;
import android.content.Context;

import hcmute.edu.vn.ticktick.BaseActivity;
import hcmute.edu.vn.ticktick.R;
import hcmute.edu.vn.ticktick.countdown.CountdownSoundPreferences;

public class MusicPlaylistActivity extends BaseActivity {

    private ImageButton btnBack;
    private Button btnUseDefault;
    private Button btnRefresh;
    private Button btnPermissionAction;
    private ProgressBar progressBar;
    private TextView tvStatus;
    private RecyclerView rvTracks;

    private MusicTrackAdapter adapter;
    private MediaStoreMusicScanner scanner;
    private CountdownSoundPreferences soundPreferences;
    private boolean triedPermissionRequest = false;
    private MediaPlayer mediaPlayer;

    private static String getAudioPermissionForSdk() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                ? Manifest.permission.READ_MEDIA_AUDIO
                : Manifest.permission.READ_EXTERNAL_STORAGE;
    }

    private final ActivityResultLauncher<String> audioPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    loadPlaylistFromDevice();
                } else {
                    showPermissionState();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music_playlist);

        scanner = new MediaStoreMusicScanner();
        soundPreferences = new CountdownSoundPreferences(this);

        bindViews();
        applySystemInsets();
        setupList();
        setupActions();
        ensurePermissionAndLoad();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Re-check permission after returning from system settings.
        if (hasAudioPermission() && adapter != null && adapter.getItemCount() == 0) {
            loadPlaylistFromDevice();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopPreview();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopPreview();
    }

    private void bindViews() {
        btnBack = findViewById(R.id.btn_back);
        btnUseDefault = findViewById(R.id.btn_use_default_sound);
        btnRefresh = findViewById(R.id.btn_refresh_playlist);
        btnPermissionAction = findViewById(R.id.btn_permission_action);
        progressBar = findViewById(R.id.progress_loading_tracks);
        tvStatus = findViewById(R.id.tv_playlist_status);
        rvTracks = findViewById(R.id.rv_tracks);
    }

    private void setupList() {
        adapter = new MusicTrackAdapter(this::onTrackSelected);
        adapter.setSelectedUri(soundPreferences.getSelectedSoundUri());

        rvTracks.setLayoutManager(new LinearLayoutManager(this));
        rvTracks.setAdapter(adapter);
    }

    private void setupActions() {
        btnBack.setOnClickListener(v -> finish());

        btnUseDefault.setOnClickListener(v -> {
            stopPreview();
            soundPreferences.clearSelection();
            adapter.setSelectedUri(null);
            Toast.makeText(this, R.string.countdown_sound_selected_default_toast, Toast.LENGTH_SHORT).show();
        });

        btnRefresh.setOnClickListener(v -> ensurePermissionAndLoad());

        btnPermissionAction.setOnClickListener(v -> {
            if (!hasAudioPermission() && isPermissionPermanentlyDenied()) {
                openAppSettings();
                return;
            }
            audioPermissionLauncher.launch(getAudioPermissionForSdk());
        });
    }

    private void ensurePermissionAndLoad() {
        if (hasAudioPermission()) {
            loadPlaylistFromDevice();
            return;
        }

        // Auto-request once so users immediately see the permission prompt.
        if (!triedPermissionRequest) {
            triedPermissionRequest = true;
            audioPermissionLauncher.launch(getAudioPermissionForSdk());
            return;
        }

        showPermissionState();
    }

    private boolean hasAudioPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }
        return ContextCompat.checkSelfPermission(this, getAudioPermissionForSdk())
                == PackageManager.PERMISSION_GRANTED;
    }

    private void showPermissionState() {
        progressBar.setVisibility(View.GONE);
        rvTracks.setVisibility(View.GONE);
        btnPermissionAction.setVisibility(View.VISIBLE);
        tvStatus.setVisibility(View.VISIBLE);

        if (isPermissionPermanentlyDenied()) {
            tvStatus.setText(R.string.countdown_sound_permission_settings_required);
            btnPermissionAction.setText(R.string.countdown_sound_open_settings);
            return;
        }

        tvStatus.setText(R.string.countdown_sound_permission_required);
        btnPermissionAction.setText(R.string.countdown_sound_request_permission);
    }

    private boolean isPermissionPermanentlyDenied() {
        return !hasAudioPermission() && triedPermissionRequest
                && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && !shouldShowRequestPermissionRationale(getAudioPermissionForSdk());
    }

    private void applySystemInsets() {
        View root = findViewById(R.id.layout_music_playlist_root);
        if (root == null) {
            return;
        }
        final int baseLeft = root.getPaddingLeft();
        final int baseTop = root.getPaddingTop();
        final int baseRight = root.getPaddingRight();
        final int baseBottom = root.getPaddingBottom();

        // Keep toolbar and list clear of status/navigation bars on all devices.
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(
                    baseLeft + bars.left,
                    baseTop + bars.top,
                    baseRight + bars.right,
                    baseBottom + bars.bottom
            );
            return insets;
        });
        ViewCompat.requestApplyInsets(root);
    }

    private void openAppSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.fromParts("package", getPackageName(), null));
        startActivity(intent);
    }

    private void loadPlaylistFromDevice() {
        progressBar.setVisibility(View.VISIBLE);
        rvTracks.setVisibility(View.GONE);
        btnPermissionAction.setVisibility(View.GONE);
        tvStatus.setVisibility(View.VISIBLE);
        tvStatus.setText(R.string.countdown_sound_loading);

        // MediaStore query runs in a background thread to keep UI smooth.
        new Thread(() -> {
            List<ScannedAudioItem> tracks;
            try {
                tracks = scanner.scan(getApplicationContext());
            } catch (Exception ex) {
                tracks = Collections.emptyList();
            }

            List<ScannedAudioItem> finalTracks = tracks;
            runOnUiThread(() -> renderTracks(finalTracks));
        }).start();
    }

    private void renderTracks(@NonNull List<ScannedAudioItem> tracks) {
        progressBar.setVisibility(View.GONE);

        adapter.setSelectedUri(soundPreferences.getSelectedSoundUri());
        adapter.submitList(tracks);

        if (tracks.isEmpty()) {
            rvTracks.setVisibility(View.GONE);
            tvStatus.setVisibility(View.VISIBLE);
            tvStatus.setText(R.string.countdown_sound_empty);
            return;
        }

        rvTracks.setVisibility(View.VISIBLE);
        tvStatus.setVisibility(View.GONE);
    }

    private void onTrackSelected(@NonNull ScannedAudioItem item) {
        if (item.getUri() == null) {
            return;
        }

        String trackTitle = item.getTitle();
        if (trackTitle == null || trackTitle.trim().isEmpty()) {
            trackTitle = getString(R.string.countdown_sound_unknown_title);
        }

        soundPreferences.saveSelection(item.getUri().toString(), trackTitle);
        adapter.setSelectedUri(item.getUri().toString());
        Toast.makeText(this, getString(R.string.countdown_sound_selected_track_toast, trackTitle), Toast.LENGTH_SHORT).show();
        playPreview(item.getUri());
    }

    private void playPreview(Uri uri) {
        stopPreview();
        try {
            mediaPlayer = MediaPlayer.create(this, uri);
            if (mediaPlayer != null) {
                mediaPlayer.start();
                mediaPlayer.setOnCompletionListener(mp -> stopPreview());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void stopPreview() {
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
}
