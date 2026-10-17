package app.morphe.extension.chmate;

import android.webkit.CookieManager;

/** Cookies are requested for an exact URL, never copied to another provider or storage host. */
final class MediaUploadSessions implements MediaUploadWebClient.Cookies {
    public String get(String url) { return CookieManager.getInstance().getCookie(url); }
    public void set(String url, String cookie) { CookieManager.getInstance().setCookie(url, cookie); }
}
