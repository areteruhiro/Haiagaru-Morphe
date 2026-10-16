package app.morphe.extension.chmate;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Build;
import android.os.Bundle;
import android.webkit.*;
import android.widget.*;

/** A visible, app-local login session. No JS bridge, password collection or cookie export. */
public final class ImgurWebLoginActivity extends Activity {
    private WebView web;
    private FrameLayout browserFrame;
    private TextView location;
    private final java.util.ArrayList<WebView> windows = new java.util.ArrayList<>();
    private TextView status;
    private Button verify;
    private Switch enabled;
    private boolean updating;
    private int verificationGeneration;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        if (!ImgurWebUpload.supported(this)) { finish(); return; }
        ImgurWebUpload.init(this);
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) (12 * getResources().getDisplayMetrics().density);
        root.setPadding(padding, padding, padding, padding);
        root.setOnApplyWindowInsetsListener((view, insets) -> {
            view.setPadding(padding, padding + insets.getSystemWindowInsetTop(), padding,
                    padding + insets.getSystemWindowInsetBottom()); return insets;
        });
        enabled = new Switch(this); enabled.setText("Imgur Webのログイン済みアカウントで送信（開発中）");
        enabled.setChecked(ImgurWebUpload.enabled()); root.addView(enabled);
        status = new TextView(this);
        status.setText("この画面でImgurにログインしてください。Chrome等とはセッションを共有しません。"
                + "\n有効時は認証失敗でも匿名送信しません。Web仕様変更・追加認証により使えない場合があります。"
                + "\nWeb版の利用規約・制限が適用され、無料利用を保証する機能ではありません。"
                + "\nGoogleログインもWebView内で開きますが、Google側で拒否される場合があります。");
        root.addView(status);
        LinearLayout buttons = new LinearLayout(this); root.addView(buttons);
        button(buttons, "ログイン", () -> {
            while (windows.size() > 1) closePopup(web);
            web.loadUrl("https://imgur.com/signin");
        });
        verify = button(buttons, "確認・有効化", this::verify);
        button(buttons, "解除", () -> new AlertDialog.Builder(this).setMessage("Imgurのログイン情報をこのアプリから削除しますか？")
                .setNegativeButton("キャンセル", null).setPositiveButton("解除", (d, w) -> clearSession()).show());
        button(buttons, "戻る", this::finish);
        enabled.setOnCheckedChangeListener((view, checked) -> {
            if (updating) return;
            if (!checked) {
                verificationGeneration++;
                verify.setEnabled(true);
                ImgurWebUpload.prefs(this).edit().putBoolean("enabled", false).apply();
            }
            else { setChecked(false); verify(); }
        });
        location = new TextView(this); root.addView(location);
        browserFrame = new FrameLayout(this);
        root.addView(browserFrame, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(root);
        web = newLoginWindow();
        web.loadUrl("https://imgur.com/signin");
    }
    private WebView newLoginWindow() {
        WebView view = new WebView(this);
        WebSettings settings = view.getSettings(); settings.setJavaScriptEnabled(true);
        // Keep the real WebView UA. Do not impersonate Chrome to bypass Google's browser policy.
        settings.setSupportMultipleWindows(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(false); settings.setAllowContentAccess(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setSaveFormData(false); settings.setSavePassword(false);
        if (Build.VERSION.SDK_INT >= 26) settings.setSafeBrowsingEnabled(true);
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(view, true);
        ImgurWebUpload.prefs(this).edit().putString("userAgent", settings.getUserAgentString()).apply();
        view.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, String url) { return block(url); }
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return request.isForMainFrame() && block(request.getUrl().toString());
            }
            private boolean block(String url) {
                if (ImgurWebPolicy.loginUrl(url)) return false;
                status.setText("許可されていないサイトへの移動を停止しました。ImgurまたはGoogleのログイン画面をご利用ください。");
                return true;
            }
            @Override public void onPageStarted(WebView view, String url, android.graphics.Bitmap icon) {
                if (!"about:blank".equals(url) && !ImgurWebPolicy.loginUrl(url)) {
                    view.stopLoading(); block(url); return;
                }
                if (view == web) showLocation(url);
            }
            @Override public void onPageFinished(WebView view, String url) {
                CookieManager.getInstance().flush();
                if (view == web) showLocation(url);
            }
        });
        view.setWebChromeClient(new WebChromeClient() {
            @Override public boolean onCreateWindow(WebView parent, boolean dialog, boolean userGesture,
                    android.os.Message result) {
                if (!userGesture || windows.size() >= 3) return false;
                WebView child = newLoginWindow();
                ((WebView.WebViewTransport) result.obj).setWebView(child);
                result.sendToTarget(); return true;
            }
            @Override public void onCloseWindow(WebView window) { closePopup(window); }
        });
        if (web != null) web.setVisibility(android.view.View.GONE);
        windows.add(view); web = view;
        browserFrame.addView(view, new FrameLayout.LayoutParams(-1, -1));
        return view;
    }
    private void showLocation(String url) {
        android.net.Uri uri = android.net.Uri.parse(url);
        location.setText(ImgurWebPolicy.loginUrl(url) ? "https://" + uri.getHost() : "ログイン画面を読み込み中…");
    }
    private void closePopup(WebView window) {
        if (windows.size() <= 1 || windows.indexOf(window) <= 0) return;
        browserFrame.removeView(window); windows.remove(window);
        window.stopLoading(); window.destroy();
        web = windows.get(windows.size() - 1); web.setVisibility(android.view.View.VISIBLE);
        showLocation(web.getUrl() == null ? "about:blank" : web.getUrl());
    }
    private Button button(LinearLayout parent, String title, Runnable action) {
        Button button = new Button(this); button.setText(title);
        button.setOnClickListener(v -> action.run());
        parent.addView(button, new LinearLayout.LayoutParams(0, -2, 1)); return button;
    }
    private void setChecked(boolean value) { updating = true; enabled.setChecked(value); updating = false; }
    private void verify() {
        final int generation = ++verificationGeneration;
        verify.setEnabled(false); status.setText("ログイン状態を確認しています…");
        new Thread(() -> {
            try {
                String account = ImgurWebUpload.checkAccount();
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed() || generation != verificationGeneration) return;
                    ImgurWebUpload.prefs(this).edit().putBoolean("enabled", true).apply();
                    setChecked(true); verify.setEnabled(true);
                    status.setText("送信先: " + account + "\n既存のImgurアップロードからこのアカウントへ送信します。");
                });
            } catch (Exception error) {
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed() || generation != verificationGeneration) return;
                    // Do not disable an already-enabled mode: it must not fall back anonymously.
                    verify.setEnabled(true);
                    status.setText("ログインを確認できません。ログイン後に再度確認してください。"
                            + "\nWeb版の仕様変更・追加認証が原因の場合もあります。");
                });
            }
        }, "Haiagaru-Imgur-session").start();
    }
    private void clearSession() {
        verificationGeneration++;
        verify.setEnabled(true);
        ImgurWebUpload.prefs(this).edit().putBoolean("enabled", false).apply(); setChecked(false);
        while (windows.size() > 1) closePopup(web);
        web.stopLoading(); web.loadUrl("about:blank");
        CookieManager manager = CookieManager.getInstance();
        for (String host : new String[]{"imgur.com", "www.imgur.com", "api.imgur.com"}) {
            String cookies = manager.getCookie("https://" + host + "/");
            if (cookies == null) continue;
            for (String cookie : cookies.split(";")) {
                String name = cookie.split("=", 2)[0].trim();
                if (!name.matches("[A-Za-z0-9_!#$%&'*+.^`|~-]+")) continue;
                for (String domain : new String[]{"", "; Domain=.imgur.com", "; Domain=" + host})
                    manager.setCookie("https://" + host + "/", name + "=; Path=/; Max-Age=0; Secure" + domain);
            }
        }
        manager.flush();
        WebStorage.getInstance().deleteOrigin("https://imgur.com");
        web.clearHistory(); web.clearCache(false);
        status.setText("このアプリ内のImgurセッションを解除しました。Web送信は無効です。");
    }
    @Override public void onBackPressed() {
        if (web.canGoBack()) web.goBack();
        else if (windows.size() > 1) closePopup(web);
        else super.onBackPressed();
    }
    @Override protected void onDestroy() {
        verificationGeneration++;
        for (WebView window : windows) { window.stopLoading(); window.destroy(); }
        windows.clear();
        super.onDestroy();
    }
}
