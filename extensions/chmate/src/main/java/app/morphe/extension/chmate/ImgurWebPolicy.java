package app.morphe.extension.chmate;

import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Fail closed when Imgur's undocumented web-client contract changes. No credentials here. */
public final class ImgurWebPolicy {
    private ImgurWebPolicy() {}

    public static boolean loginUrl(String url) {
        try {
            URI uri = new URI(url);
            return "https".equals(uri.getScheme()) && uri.getRawUserInfo() == null
                    && (uri.getPort() == -1 || uri.getPort() == 443)
                    && ("imgur.com".equals(uri.getHost()) || "www.imgur.com".equals(uri.getHost())
                        || "accounts.google.com".equals(uri.getHost()));
        } catch (Exception ignored) { return false; }
    }

    public static String scriptUrl(String html) {
        Matcher matcher = Pattern.compile("https://s\\.imgur\\.com/desktop-assets/js/main\\.[a-zA-Z0-9]+\\.js").matcher(html);
        return matcher.find() ? matcher.group() : null;
    }

    public static String webClientId(String script) {
        // Observed on 2026-10-16: exported hH is the public desktop client ID,
        // not an OAuth token/secret. Never substitute a third-party app's ID.
        Matcher export = Pattern.compile("hH:\\(\\)=>([a-zA-Z_$][a-zA-Z0-9_$]*)").matcher(script);
        if (!export.find()) return null;
        String area = script.substring(export.end(), Math.min(script.length(), export.end() + 5000));
        Matcher value = Pattern.compile("(?:[,;]|var )" + Pattern.quote(export.group(1))
                + "=\"([a-fA-F0-9]{15})\"").matcher(area);
        return value.find() ? value.group(1) : null;
    }

    public static boolean ownedBy(long expectedId, String expectedName, long actualId, String actualName) {
        if (expectedId <= 0 || expectedName == null || expectedName.isEmpty()) return false;
        if (actualId > 0) return actualId == expectedId;
        return actualName != null && expectedName.equalsIgnoreCase(actualName);
    }

    public static boolean mediaUrl(String url) {
        try {
            URI uri = new URI(url);
            return "https".equals(uri.getScheme()) && "i.imgur.com".equals(uri.getHost())
                    && uri.getRawUserInfo() == null && uri.getPort() == -1
                    && uri.getPath().matches("/[a-zA-Z0-9]+\\.(?:jpe?g|png|gif|webp|mp4|webm)");
        } catch (Exception ignored) { return false; }
    }
}
