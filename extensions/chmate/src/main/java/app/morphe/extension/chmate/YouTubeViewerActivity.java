package app.morphe.extension.chmate;

import android.app.Activity;
import android.content.*;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.*;
import android.webkit.*;
import android.widget.*;

/** Official embedded player only. No stream extraction, login automation or ad bypass. */
public final class YouTubeViewerActivity extends Activity {
    private WebView web;
    private LinearLayout root, toolbar;
    private FrameLayout content;
    private View fullscreen;
    private WebChromeClient.CustomViewCallback fullscreenCallback;
    private String url;
    private Runnable unregisterBack;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        unregisterBack = VideoViewerBack.register(this);
        String id = getIntent().getData() == null ? null : YouTubeUrls.videoId(getIntent().getData().toString());
        if (id == null || !Haiagaru.videoViewerEnabled() || !VideoViewerSettings.youtubeEnabled(this)) { finish(); return; }
        url = getIntent().getData().toString();
        root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(Color.BLACK);
        root.setOnApplyWindowInsetsListener((view, insets) -> {
            view.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(),
                    insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom()); return insets;
        });
        toolbar = new LinearLayout(this);
        Button back = button("戻る"); back.setOnClickListener(v -> onBackPressed()); toolbar.addView(back);
        toolbar.addView(new View(this), new LinearLayout.LayoutParams(0, 1, 1));
        Button more = button("︙"); more.setContentDescription("YouTubeメニュー"); more.setOnClickListener(this::menu); toolbar.addView(more);
        root.addView(toolbar);
        content = new FrameLayout(this); root.addView(content, new LinearLayout.LayoutParams(-1, 0, 1));
        web = new WebView(this); content.addView(web, new FrameLayout.LayoutParams(-1, -1));
        WebSettings settings = web.getSettings(); settings.setJavaScriptEnabled(true); settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(false); settings.setAllowContentAccess(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setMediaPlaybackRequiresUserGesture(true);
        web.setBackgroundColor(Color.BLACK);
        web.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, String target) { return navigate(target); }
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return !request.isForMainFrame() ? false : navigate(request.getUrl().toString());
            }
        });
        web.setWebChromeClient(new WebChromeClient() {
            @Override public void onShowCustomView(View view, CustomViewCallback callback) {
                if (fullscreen != null) { callback.onCustomViewHidden(); return; }
                fullscreen = view; fullscreenCallback = callback;
                web.setVisibility(View.GONE); toolbar.setVisibility(View.GONE);
                content.addView(view, new FrameLayout.LayoutParams(-1, -1));
            }
            @Override public void onHideCustomView() { hideFullscreen(); }
        });
        setContentView(root); root.requestApplyInsets();
        // A stable app-identifying HTTPS base supplies the Referer required by official embeds.
        // Do not impersonate another client or extract protected media URLs.
        String html = "<!doctype html><html><head><meta name='viewport' content='width=device-width,initial-scale=1'>"
                + "<style>html,body,iframe{margin:0;border:0;width:100%;height:100%;background:#000}</style></head>"
                + "<body><iframe allow='fullscreen; encrypted-media; picture-in-picture' allowfullscreen "
                + "referrerpolicy='strict-origin-when-cross-origin' src='https://www.youtube.com/embed/" + id
                + "?playsinline=1&amp;enablejsapi=1'></iframe></body></html>";
        web.loadDataWithBaseURL("https://github.com/areteruhiro/Haiagaru-Morphe/", html, "text/html", "UTF-8", null);
    }
    private Button button(String label) {
        Button button = new Button(this); button.setText(label); button.setTextColor(Color.WHITE);
        button.setBackgroundColor(Color.TRANSPARENT); return button;
    }
    private boolean navigate(String target) {
        try {
            Uri uri = Uri.parse(target);
            String host = uri.getHost();
            if ("https".equalsIgnoreCase(uri.getScheme()) && host != null
                    && (host.equals("youtube.com") || host.endsWith(".youtube.com") || host.equals("youtu.be"))) return false;
            if ("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme())) external(target);
        } catch (RuntimeException ignored) { }
        return true;
    }
    private void external(String target) {
        try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(target)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); }
        catch (RuntimeException failure) { Toast.makeText(this, "外部で開けませんでした", Toast.LENGTH_SHORT).show(); }
    }
    private void menu(View anchor) {
        PopupMenu menu = new PopupMenu(this, anchor);
        menu.getMenu().add(0, 1, 0, "外部アプリで開く");
        menu.getMenu().add(0, 2, 1, "URLをコピー");
        menu.getMenu().add(0, 3, 2, "再読み込み");
        menu.getMenu().add(0, 4, 3, "YouTubeサイト表示");
        menu.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case 1: pauseVideo(); external(url); return true;
                case 2:
                    ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                    if (clipboard != null) clipboard.setPrimaryClip(ClipData.newPlainText("YouTube URL", url)); return true;
                case 3: web.reload(); return true;
                case 4: web.loadUrl("https://m.youtube.com/watch?v=" + YouTubeUrls.videoId(url)); return true;
                default: return false;
            }
        }); menu.show();
    }
    private void hideFullscreen() {
        if (fullscreen == null) return;
        content.removeView(fullscreen); fullscreen = null;
        web.setVisibility(View.VISIBLE); toolbar.setVisibility(View.VISIBLE);
        WebChromeClient.CustomViewCallback callback = fullscreenCallback; fullscreenCallback = null;
        if (callback != null) callback.onCustomViewHidden();
    }
    private void pauseVideo() {
        if (web == null) return;
        web.evaluateJavascript("Array.prototype.forEach.call(document.querySelectorAll('video'),function(v){v.pause()});"
                + "Array.prototype.forEach.call(document.querySelectorAll('iframe'),function(f){f.contentWindow.postMessage("
                + "JSON.stringify({event:'command',func:'pauseVideo',args:[]}), 'https://www.youtube.com')});", null);
    }
    @Override public void onBackPressed() {
        finish();
    }
    @Override protected void onPause() { pauseVideo(); if (web != null) web.onPause(); super.onPause(); }
    @Override protected void onResume() { super.onResume(); if (web != null) web.onResume(); }
    @Override protected void onDestroy() {
        if (unregisterBack != null) { unregisterBack.run(); unregisterBack = null; }
        hideFullscreen();
        if (web != null) { web.stopLoading(); content.removeView(web); web.destroy(); web = null; }
        super.onDestroy();
    }
}
