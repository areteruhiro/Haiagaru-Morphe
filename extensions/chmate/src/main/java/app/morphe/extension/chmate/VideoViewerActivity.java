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
import android.view.MotionEvent;
import android.view.KeyEvent;
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
    private VideoViewerGestures gestures;
    private VideoTouchControls touchControls;
    private Runnable unregisterBack;

    public static boolean installed(Context context) {
        if (context == null) return false;
        try (java.io.InputStream marker = context.getAssets().open("haiagaru/video-viewer/installed")) {
            return marker.read() == '1';
        } catch (java.io.IOException ignored) { return false; }
    }

    /** Called only at native external-launch boundaries, never at the external menu below. */
    public static void prepareExternalIntent(Intent intent) {
        if (intent == null || !Intent.ACTION_VIEW.equals(intent.getAction())) return;
        Context context = Haiagaru.applicationContextForExtension();
        if (context == null || !Haiagaru.videoViewerEnabled()) return;
        Uri uri = intent.getData();
        if (uri == null) return;
        // Do not replace unrelated explicit destinations or pin to another installed app.
        if (intent.getComponent() != null || intent.getPackage() != null) return;
        if (YouTubeUrls.videoId(uri.toString()) != null && VideoViewerSettings.youtubeEnabled(context)) {
            intent.setClass(context, YouTubeViewerActivity.class);
        } else if (XMediaAttachments.isVideo(uri.toString())) intent.setClass(context, VideoViewerActivity.class);
    }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        unregisterBack = VideoViewerBack.register(this);
        if (!Haiagaru.videoViewerEnabled()) { finish(); return; }
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
        setVolumeControlStream(android.media.AudioManager.STREAM_MUSIC);
        touchControls = new VideoTouchControls(this);
        // Confirmed taps are handled by the detector, so a double tap cannot toggle controls twice.
        video.setOnTouchListener((view, event) -> true);
        frame.addView(video, new FrameLayout.LayoutParams(-1, -1, Gravity.CENTER));
        // Overlay controls so hiding them does not resize or shift the video.
        frame.addView(toolbar, new FrameLayout.LayoutParams(-1, -2, Gravity.TOP));
        root.addView(frame, new LinearLayout.LayoutParams(-1, 0, 1));
        status = new TextView(this); status.setTextColor(Color.WHITE); status.setPadding(16, 8, 16, 12);
        root.addView(status);
        setContentView(root); root.requestApplyInsets();
        controls = new MediaController(this) {
            private Runnable unregisterControllerBack;
            @Override public void show(int timeout) {
                // VideoView and MediaController also request timed shows after touch/seek/play.
                // Ignore every timeout so none of these paths schedule automatic dismissal.
                super.show(0);
                if (unregisterControllerBack != null) unregisterControllerBack.run();
                unregisterControllerBack = VideoViewerBack.register(this, VideoViewerActivity.this);
                toolbar.setVisibility(View.VISIBLE);
            }
            @Override public boolean dispatchKeyEvent(KeyEvent event) {
                if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
                    if (event.getAction() == KeyEvent.ACTION_UP && !event.isCanceled()) finish();
                    return true;
                }
                return super.dispatchKeyEvent(event);
            }
            @Override public void hide() {
                if (unregisterControllerBack != null) {
                    unregisterControllerBack.run(); unregisterControllerBack = null;
                }
                super.hide();
                toolbar.setVisibility(View.INVISIBLE);
            }
        };
        controls.setAnchorView(frame); video.setMediaController(controls);
        video.setOnPreparedListener(player -> {
            prepared = true;
            if (position > 0) video.seekTo(position);
            setStatus("");
            if (foreground && playWhenReady) video.start();
            controls.show(0);
        });
        video.setOnCompletionListener(player -> {
            playWhenReady = false; position = 0;
            setStatus("再生が終了しました。再生ボタンで最初から再生できます"); controls.show(0);
        });
        video.setOnErrorListener((player, what, extra) -> {
            prepared = false; playWhenReady = false;
            setStatus("動画を再生できません（エラー " + what + "/" + extra
                    + "）。︙から再読み込み、または外部アプリで開いてください");
            return true;
        });
        video.setOnInfoListener((player, what, extra) -> {
            if (what == android.media.MediaPlayer.MEDIA_INFO_BUFFERING_START) setStatus("読み込み中…");
            else if (what == android.media.MediaPlayer.MEDIA_INFO_BUFFERING_END)
                setStatus("");
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
        setStatus("動画を読み込み中…（通信が発生します）");
        video.setVideoURI(Uri.parse(url));
    }

    @Override public boolean dispatchTouchEvent(MotionEvent event) {
        if (touchControls != null && video != null && touchControls.dispatch(event, video)) return true;
        if (gestures != null && gestures.dispatch(event, video)) {
            if (touchControls != null) touchControls.cancel();
            return true;
        }
        return super.dispatchTouchEvent(event);
    }
    void toggleControls() {
        if (controls == null) return;
        if (controls.isShowing()) controls.hide(); else controls.show(0);
    }
    float videoMidpoint() {
        int[] p = new int[2]; video.getLocationOnScreen(p); return p[0] + video.getWidth() / 2f;
    }
    void cancelNativeVideoGesture() { if (gestures != null) gestures.cancel(); }
    private void setStatus(String message) {
        status.setText(message);
        status.setVisibility(message.isEmpty() ? View.GONE : View.VISIBLE);
    }
    void gestureStatus(String message) { setStatus(message); }
    void seekRelative(int seconds) {
        if (!prepared || video.getDuration() <= 0) return;
        video.seekTo(VideoPlaybackMath.seekTarget(video.getCurrentPosition(), video.getDuration(), seconds));
        setStatus(Math.abs(seconds) + (seconds < 0 ? "秒巻き戻し" : "秒早送り"));
    }
    boolean dispatchVideoTouch(MotionEvent event) { return super.dispatchTouchEvent(event); }
    boolean isVideoToolbarTouch(float x, float y) {
        if (toolbar == null || toolbar.getVisibility() != View.VISIBLE) return false;
        int[] p = new int[2]; toolbar.getLocationOnScreen(p);
        return x >= p[0] && x < p[0] + toolbar.getWidth() && y >= p[1] && y < p[1] + toolbar.getHeight();
    }
    void performVideoGesture(String key) {
        switch (key) {
            case "gestureBack": case "gestureCloseTab": finish(); break;
            case "gestureTop": if (prepared) video.seekTo(0); break;
            case "gestureBottom": if (prepared) video.seekTo(Math.max(0, video.getDuration() - 1)); break;
            case "gestureRefresh":
                if (prepared) position = video.getCurrentPosition();
                video.stopPlayback(); playWhenReady = true; load(); break;
            case "gestureFullScreen":
                if (controls.isShowing()) controls.hide(); else controls.show(0); break;
            case "gestureSetting":
                try { startActivity(new Intent(this, Class.forName("jp.syoboi.a2chMate.activity.SettingActivity", false, getClassLoader()))); }
                catch (ReflectiveOperationException | RuntimeException error) {
                    Toast.makeText(this, "設定を開けませんでした", Toast.LENGTH_SHORT).show();
                }
                break;
            case NgEditGesture.KEY: NgEditGesture.handle(video, 0x48690042); break;
            // No next-video/forward history exists in a single-video viewer.
            case "gestureForward": Toast.makeText(this, "次の動画はありません", Toast.LENGTH_SHORT).show(); break;
            default: break;
        }
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
        menu.getMenu().add(0, 5, 4, "操作設定");
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
                case 5: VideoViewerSettings.show(this); return true;
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
        if (video != null) gestures = new VideoViewerGestures(this);
        if (prepared && playWhenReady) video.start();
    }
    @Override protected void onPause() {
        if (touchControls != null) touchControls.cancel();
        foreground = false; pausePlayback();
        // Returning from another app should resume only on the user's play action.
        playWhenReady = false;
        if (gestures != null) gestures.close();
        if (controls != null) controls.hide();
        super.onPause();
    }
    @Override protected void onSaveInstanceState(Bundle state) {
        state.putInt("position", prepared ? video.getCurrentPosition() : position);
        state.putBoolean("playing", prepared ? video.isPlaying() : playWhenReady);
        state.putBoolean("rotated", rotated); super.onSaveInstanceState(state);
    }
    @Override protected void onDestroy() {
        if (unregisterBack != null) { unregisterBack.run(); unregisterBack = null; }
        if (touchControls != null) touchControls.cancel();
        if (gestures != null) gestures.close();
        if (controls != null) controls.hide();
        if (video != null) video.stopPlayback();
        super.onDestroy();
    }
    @Override public void onBackPressed() { finish(); }
}
