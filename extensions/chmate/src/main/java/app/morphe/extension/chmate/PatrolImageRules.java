package app.morphe.extension.chmate;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;

/** No Android dependencies: matching and names are shared by both storage destinations. */
public final class PatrolImageRules {
    private PatrolImageRules() { }
    public static boolean eligible(boolean filterEnabled, String title, String url, String keywords, String urls) {
        return !filterEnabled || matches(title, url, keywords, urls);
    }
    public static boolean matches(String title, String url, String keywords, String urls) {
        if (contains(title, keywords)) return true;
        if (urls != null) for (String line : urls.split("\\r?\\n")) {
            String rule = threadUrl(line.trim());
            if (!rule.isEmpty() && threadUrl(url).toLowerCase(Locale.ROOT).contains(rule.toLowerCase(Locale.ROOT))) return true;
        }
        return false;
    }
    public static String threadUrl(String url) {
        if (url == null) return "";
        return url.replaceFirst("(?i)^https?://", "")
                .replaceFirst("[?#].*$", "")
                .replaceFirst("(/test/read\\.cgi/[^/]+/[0-9]+)(?:/.*)?$", "$1/")
                .replaceFirst("(/boards/[^/]+/[0-9]+)(?:/.*)?$", "$1/");
    }
    private static boolean contains(String value, String rules) {
        if (value == null || rules == null) return false;
        value = value.toLowerCase(Locale.ROOT);
        for (String line : rules.split("\\r?\\n")) {
            String rule = line.trim().toLowerCase(Locale.ROOT);
            if (!rule.isEmpty() && value.contains(rule)) return true;
        }
        return false;
    }
    public static boolean imageUrl(String value) {
        try {
            URI uri = URI.create(value);
            String scheme = uri.getScheme(), path = uri.getPath();
            if (!("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))
                    || uri.getHost() == null || uri.getUserInfo() != null || path == null) return false;
            path = path.toLowerCase(Locale.ROOT);
            return path.matches(".*\\.(?:jpe?g|png|gif|webp|bmp)$")
                    || ("pbs.twimg.com".equalsIgnoreCase(uri.getHost()) && path.startsWith("/media/"));
        } catch (RuntimeException invalid) { return false; }
    }
    public static String hash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder out = new StringBuilder();
            for (byte b : digest) out.append(String.format(Locale.ROOT, "%02x", b & 255));
            return out.toString();
        } catch (Exception impossible) { throw new IllegalStateException(impossible); }
    }
    public static String folder(String title, String url) {
        String safe = (title == null ? "thread" : title).replaceAll("[\\p{Cntrl}\\\\/:*?\"<>|]", "_")
                .replaceAll("^[. ]+|[. ]+$", "");
        if (safe.isEmpty()) safe = "thread";
        if (safe.length() > 60) safe = safe.substring(0, 60);
        return safe + "_" + hash(url).substring(0, 12);
    }
}
