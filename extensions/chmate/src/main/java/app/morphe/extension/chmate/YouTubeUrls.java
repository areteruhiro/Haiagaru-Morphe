package app.morphe.extension.chmate;

import java.net.URI;
import java.net.URLDecoder;
import java.util.Locale;

/** Only public YouTube video URLs, not lookalike hosts, arbitrary embeds or playlists. */
public final class YouTubeUrls {
    private YouTubeUrls() { }
    public static String videoId(String url) {
        if (url == null) return null;
        try {
            URI uri = new URI(url);
            if (!("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme()))
                    || uri.getUserInfo() != null || uri.getPort() != -1 || uri.getHost() == null) return null;
            String host = uri.getHost().toLowerCase(Locale.ROOT), path = uri.getPath(), id = null;
            if ("youtu.be".equals(host)) id = path.matches("/[A-Za-z0-9_-]{11}/?") ? path.split("/")[1] : null;
            else if ("youtube.com".equals(host) || "www.youtube.com".equals(host) || "m.youtube.com".equals(host)
                    || "music.youtube.com".equals(host)) {
                if ("/watch".equals(path) && uri.getRawQuery() != null) {
                    for (String pair : uri.getRawQuery().split("&")) {
                        String[] value = pair.split("=", 2);
                        if (value.length == 2 && "v".equals(value[0])) {
                            if (id != null) return null;
                            id = URLDecoder.decode(value[1], "UTF-8");
                        }
                    }
                } else if (path.matches("/(shorts|live|embed)/[A-Za-z0-9_-]{11}/?")) {
                    id = path.split("/")[2];
                }
            }
            return id != null && id.matches("[A-Za-z0-9_-]{11}") ? id : null;
        } catch (Exception malformed) { return null; }
    }
}
