package app.morphe.extension.chmate;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.util.Base64;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceError;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.CookieManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.nio.charset.StandardCharsets;
import java.util.List;

/** Displays the Hissi menu destination inside ChMate's task. */
public final class HissiMenuActivity extends Activity {
    private static final String LOG_TAG = "HaiagaruHissi";
    private static final String HISSI_SCHEME = "haiagaru-hissi";
    private static final String HISSI_SECURE_SCHEME = "haiagaru-hissis";
    private String sourceHost;
    private String sourceBoard;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        Uri incoming = getIntent() == null ? null : getIntent().getData();
        sourceHost = incoming == null ? null : incoming.getQueryParameter("haiagaru_host");
        sourceBoard = incoming == null ? null : boardFromMenuPath(incoming);
        // Read the current setting again here. ChMate may cache an expanded
        // menu template, so the mode embedded in its URL can be stale.
        int checkerMode = Haiagaru.hissiCheckerMode();
        boolean useKyodemo = checkerMode == 2 || (checkerMode == 0
                && sourceHost != null && !is5chHost(sourceHost));
        String alternateTarget = checkerMode == 3
                ? (useKyodemo ? toHttpsUrl(incoming) : toKyodemoUrl(incoming, sourceHost)) : null;
        String target = useKyodemo ? toKyodemoUrl(incoming, sourceHost) : toHttpsUrl(incoming);
        if (target == null) {
            TextView error = new TextView(this);
            error.setText(useKyodemo
                    ? "この板のID検索先を特定できませんでした"
                    : "必死チェッカーのURLを開けませんでした");
            error.setTextColor(Color.WHITE);
            error.setPadding(32, 32, 32, 32);
            setContentView(error);
            return;
        }

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setBackgroundColor(Color.WHITE);
        TextView title = new TextView(this);
        title.setText(useKyodemo
                ? "‹   ID検索（kyodemo） · ChMate内表示"
                : "‹   必死チェッカーもどき  ·  ChMate内表示");
        title.setTextColor(Color.BLACK);
        title.setTextSize(18);
        title.setGravity(Gravity.CENTER_VERTICAL);
        title.setPadding(24, 12, 24, 12);
        title.setOnClickListener(view -> finish());
        layout.addView(title, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 56 * getResources().getDisplayMetrics().densityDpi / 160));
        WebView webView = new WebView(this);
        WebSettings settings = webView.getSettings();
        // Kyodemo's analysis and screenshot actions are implemented by its
        // same-origin fetch handlers. Hissi's static pages do not need JS.
        settings.setJavaScriptEnabled(useKyodemo || checkerMode == 3);
        settings.setDomStorageEnabled(useKyodemo || checkerMode == 3);
        settings.setLoadsImagesAutomatically(true);
        settings.setSupportMultipleWindows(true);
        if (useKyodemo || checkerMode == 3) {
            CookieManager.getInstance().setAcceptCookie(true);
            CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);
        }
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return request.isForMainFrame() && openThreadInChMate(request.getUrl().toString());
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return openThreadInChMate(url);
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (request.isForMainFrame()) {
                    title.setText("‹   読み込みエラー · " + error.getDescription());
                }
            }
        });
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onCreateWindow(WebView source, boolean isDialog,
                    boolean isUserGesture, Message resultMsg) {
                if (!isUserGesture) return false;
                WebView.HitTestResult hit = source.getHitTestResult();
                String selectedUrl = hit == null ? null : hit.getExtra();
                if (selectedUrl != null && !selectedUrl.isEmpty()) {
                    if (!openThreadInChMate(selectedUrl)) source.loadUrl(selectedUrl);
                    return false;
                }
                if (resultMsg == null || !(resultMsg.obj instanceof WebView.WebViewTransport)) {
                    return false;
                }

                // Some WebView builds omit the hit-test URL for target=_blank.
                // Receive that first navigation in a temporary hidden window.
                WebView popup = new WebView(HissiMenuActivity.this);
                popup.setVisibility(View.INVISIBLE);
                layout.addView(popup, new LinearLayout.LayoutParams(1, 1));
                popup.setWebViewClient(new WebViewClient() {
                    private boolean dispatched;

                    private void dispatch(String url) {
                        if (dispatched) return;
                        dispatched = true;
                        if (!openThreadInChMate(url)) source.loadUrl(url);
                        popup.post(() -> {
                            layout.removeView(popup);
                            popup.destroy();
                        });
                    }

                    @Override
                    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                        if (request.isForMainFrame()) dispatch(request.getUrl().toString());
                        return true;
                    }

                    @Override
                    public boolean shouldOverrideUrlLoading(WebView view, String url) {
                        dispatch(url);
                        return true;
                    }

                    @Override
                    public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                        dispatch(url);
                    }
                });
                WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
                transport.setWebView(popup);
                resultMsg.sendToTarget();
                popup.postDelayed(() -> {
                    if (popup.getParent() != null) {
                        layout.removeView(popup);
                        popup.destroy();
                    }
                }, 10000);
                return true;
            }
        });
        if (alternateTarget != null) {
            TextView alternate = new TextView(this);
            alternate.setText(useKyodemo ? "hissi.orgで開く" : "Kyodemoで開く");
            alternate.setTextColor(Color.BLUE);
            alternate.setGravity(Gravity.CENTER);
            alternate.setPadding(20, 12, 20, 12);
            alternate.setOnClickListener(view -> webView.loadUrl(alternateTarget));
            layout.addView(alternate, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 52 * getResources().getDisplayMetrics().densityDpi / 160));
        }
        webView.loadUrl(target);
        layout.addView(webView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        setContentView(layout);
    }

    private boolean openThreadInChMate(String url) {
        String original = KyodemoRouting.sourceThreadUrl(sourceHost, sourceBoard, url);
        if (original != null) url = original;
        if (!HissiLinkRouting.isThreadUrl(url)) return false;
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            intent.setClass(this, OpenUrlActivity.class);
            startActivity(intent);
            return true;
        } catch (RuntimeException error) {
            Log.e(LOG_TAG, "Could not open Hissi thread link in ChMate: " + url, error);
            return false;
        }
    }

    private static String toHttpsUrl(Uri uri) {
        if (uri == null || (!HISSI_SCHEME.equalsIgnoreCase(uri.getScheme())
                && !HISSI_SECURE_SCHEME.equalsIgnoreCase(uri.getScheme()))
                || !"hissi.org".equalsIgnoreCase(uri.getHost())) return null;
        String path = uri.getEncodedPath();
        if (path == null || !path.startsWith("/read.php/")) return null;
        String scheme = HISSI_SECURE_SCHEME.equalsIgnoreCase(uri.getScheme())
                ? "https" : "http";
        StringBuilder result = new StringBuilder(scheme).append("://hissi.org").append(path);
        if (uri.getEncodedQuery() != null && uri.getQueryParameter("haiagaru_host") == null) {
            result.append('?').append(uri.getEncodedQuery());
        }
        return result.toString();
    }

    private static String toKyodemoUrl(Uri uri, String sourceHost) {
        if (uri == null || !"hissi.org".equalsIgnoreCase(uri.getHost())) return null;
        List<String> path = uri.getPathSegments();
        if (path.size() < 4 || !"read.php".equals(path.get(0))) return null;
        String encodedId = path.get(path.size() - 1);
        if (!encodedId.endsWith(".html")) return null;
        encodedId = encodedId.substring(0, encodedId.length() - 5);
        String id;
        try {
            id = new String(Base64.decode(encodedId, Base64.URL_SAFE | Base64.NO_WRAP),
                    StandardCharsets.UTF_8);
        } catch (IllegalArgumentException error) {
            return null;
        }
        return KyodemoRouting.idSearchUrl(sourceHost, boardFromMenuPath(uri), id,
                uri.getQueryParameter("haiagaru_key"), path.get(path.size() - 2));
    }

    private static String boardFromMenuPath(Uri uri) {
        List<String> path = uri.getPathSegments();
        if (path.size() < 4 || !"read.php".equals(path.get(0))) return null;
        StringBuilder board = new StringBuilder();
        for (int index = 1; index < path.size() - 2; index++) {
            if (board.length() > 0) board.append('/');
            board.append(path.get(index));
        }
        return board.toString();
    }

    private static boolean is5chHost(String sourceHost) {
        String host = sourceHost.toLowerCase(java.util.Locale.ROOT);
        return host.equals("2ch.net") || host.endsWith(".2ch.net")
                || host.equals("5ch.net") || host.endsWith(".5ch.net")
                || host.equals("5ch.io") || host.endsWith(".5ch.io");
    }

    private static int parseCheckerMode(String value) {
        try {
            int mode = Integer.parseInt(value);
            return mode < 0 || mode > 3 ? 0 : mode;
        } catch (RuntimeException ignored) {
            return 0;
        }
    }
}
