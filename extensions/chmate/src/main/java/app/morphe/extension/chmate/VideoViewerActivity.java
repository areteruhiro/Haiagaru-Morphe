package app.morphe.extension.chmate;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.*;

/** User-visible streaming player. No WebView, login cookies or automatic retries. */
public final class VideoViewerActivity extends Activity {
    private VideoView video;
    private MediaController controls;
    private LinearLayout toolbar;
    private TextView status;
    private String url;
    private int position;
    private boolean prepared, foreground, playWhenReady = true;
    private boolean rotated;

    /** Called only at native external-launch boundaries, never at the external menu below. */
    public static void prepareExternalIntent(Intent intent) {
        if (intent == null || !Intent.ACTION_VIEW.equals(intent.getAction())) return;
        Context context = Haiagaru.applicationContextForExtension();
        if (context == null || !Haiagaru.videoViewerEnabled()) return;
        Uri uri = intent.getData();
        if (uri == null || !XMediaAttachments.isVideo(uri.toString())) return;
        // Do not replace unrelated explicit destinations or pin to another installed app.
        if (intent.getComponent() != null || intent.getPackage() != null) return;
        intent.setClass(context, VideoViewerActivity.class);
    }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        Uri uri = getIntent().getData();
        if (uri == null || !XMediaAttachments.isVideo(uri.toString())) { finish(); return; }
        url = uri.toString();
        if (state != null) {
            position = state.getInt("position", 0);
            playWhenReady = state.getBoolean("playing", true);
            rotated = state.getBoolean("rotated", false);
        }
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(Color.BLACK);
        root.setOnApplyWindowInsetsListener((view, insets) -> {
            view.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(),
                    insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
            return insets;
        });
        toolbar = new LinearLayout(this); toolbar.setGravity(Gravity.CENTER_VERTICAL);
        Button back = overlayButton("戻る"); back.setOnClickListener(v -> finish());
        toolbar.addView(back, new LinearLayout.LayoutParams(-2, -2));
        toolbar.addView(new View(this), new LinearLayout.LayoutParams(0, 1, 1));
        Button more = overlayButton("︙"); more.setContentDescription("動画メニュー");
        more.setOnClickListener(this::showMenu); toolbar.addView(more);
        FrameLayout frame = new FrameLayout(this);
        video = new VideoView(this);
        frame.addView(video, new FrameLayout.LayoutParams(-1, -1, Gravity.CENTER));
        // Overlay controls so hiding them does not resize or shift the video.
        frame.addView(toolbar, new FrameLayout.LayoutParams(-1, -2, Gravity.TOP));
        root.addView(frame, new LinearLayout.LayoutParams(-1, 0, 1));
        status = new TextView(this); status.setTextColor(Color.WHITE); status.setPadding(16, 8, 16, 12);
        root.addView(status);
        setContentView(root); root.requestApplyInsets();
        controls = new MediaController(this) {
            @Override public void show(int timeout) {
                // VideoView and MediaController also request timed shows after touch/seek/play.
                // Ignore every timeout so none of these paths schedule automatic dismissal.
                super.show(0);
                toolbar.setVisibility(View.VISIBLE);
            }
            @Override public void hide() {
                super.hide();
                toolbar.setVisibility(View.INVISIBLE);
            }
        };
        controls.setAnchorView(frame); video.setMediaController(controls);
        video.setOnPreparedListener(player -> {
            prepared = true;
            if (position > 0) video.seekTo(position);
            status.setText("画面タップで操作パネルの表示・非表示を切り替え");
            if (foreground && playWhenReady) video.start();
            controls.show(0);
        });
        video.setOnCompletionListener(player -> {
            playWhenReady = false; position = 0;
            status.setText("再生が終了しました。再生ボタンで最初から再生できます"); controls.show(0);
        });
        video.setOnErrorListener((player, what, extra) -> {
            prepared = false; playWhenReady = false;
            status.setText("動画を再生できません（エラー " + what + "/" + extra
                    + "）。︙から再読み込み、または外部アプリで開いてください");
            return true;
        });
        video.setOnInfoListener((player, what, extra) -> {
            if (what == android.media.MediaPlayer.MEDIA_INFO_BUFFERING_START) status.setText("読み込み中…");
            else if (what == android.media.MediaPlayer.MEDIA_INFO_BUFFERING_END)
                status.setText("画面タップで操作パネルの表示・非表示を切り替え");
            return false;
        });
        load();
    }

    private Button overlayButton(String label) {
        Button button = new Button(this);
        button.setText(label);
        button.setBackgroundColor(Color.TRANSPARENT);
        button.setTextColor(Color.WHITE);
        // A dark text shadow keeps the label readable over bright video without a button background.
        float density = getResources().getDisplayMetrics().density;
        button.setShadowLayer(2 * density, 0, density, Color.BLACK);
        return button;
    }

    private void load() {
        prepared = false;
        status.setText("動画を読み込み中…（通信が発生します）");
        video.setVideoURI(Uri.parse(url));
    }
    private void showMenu(View anchor) {
        controls.show(0);
        PopupMenu menu = new PopupMenu(this, anchor);
        menu.setOnDismissListener(dismissed -> {
            if (foreground) controls.show(0);
        });
        menu.getMenu().add(0, 1, 0, "外部アプリで開く");
        menu.getMenu().add(0, 2, 1, "URLをコピー");
        menu.getMenu().add(0, 3, 2, "再読み込み");
        menu.getMenu().add(0, 4, 3, "画面を回転");
        menu.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case 1:
                    pausePlayback();
                    try {
                        // Extension calls are excluded from the patch's launch interception.
                        Intent external = new Intent(Intent.ACTION_VIEW).setDataAndType(Uri.parse(url), "video/mp4");
                        startActivity(Intent.createChooser(external, "動画を外部アプリで開く"));
                    } catch (RuntimeException error) {
                        Toast.makeText(this, "動画を開ける外部アプリがありません", Toast.LENGTH_LONG).show();
                    }
                    return true;
                case 2:
                    ClipboardManager clipboard = (ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
                    if (clipboard != null) clipboard.setPrimaryClip(ClipData.newPlainText("動画URL", url));
                    Toast.makeText(this, "URLをコピーしました", Toast.LENGTH_SHORT).show(); return true;
                case 3:
                    if (prepared) position = video.getCurrentPosition();
                    video.stopPlayback(); playWhenReady = true; load(); return true;
                case 4:
                    rotated = !rotated;
                    setRequestedOrientation(rotated ? ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                            : ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED); return true;
                default: return false;
            }
        });
        menu.show();
    }
    private void pausePlayback() {
        if (prepared) {
            position = video.getCurrentPosition();
            playWhenReady = video.isPlaying();
            video.pause();
        }
    }
    @Override protected void onResume() {
        super.onResume(); foreground = true;
        if (prepared && playWhenReady) video.start();
    }
    @Override protected void onPause() {
        foreground = false; pausePlayback();
        // Returning from another app should resume only on the user's play action.
        playWhenReady = false;
        if (controls != null) controls.hide();
        super.onPause();
    }
    @Override protected void onSaveInstanceState(Bundle state) {
        state.putInt("position", prepared ? video.getCurrentPosition() : position);
        state.putBoolean("playing", prepared ? video.isPlaying() : playWhenReady);
        state.putBoolean("rotated", rotated); super.onSaveInstanceState(state);
    }
    @Override protected void onDestroy() {
        if (controls != null) controls.hide();
        if (video != null) video.stopPlayback();
        super.onDestroy();
    }
}
