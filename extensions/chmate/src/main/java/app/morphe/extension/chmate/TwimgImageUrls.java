package app.morphe.extension.chmate;

import java.net.URI;
import java.net.URLDecoder;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Makes public X image URLs readable by native extension-based image loaders. */
public final class TwimgImageUrls {
    private static final Pattern URL = Pattern.compile(
            "(?<![A-Za-z0-9_./:@-])(?:(?:https?|ttps?)://|//)?"
                    + "pbs\\.twimg\\.com/media/[^\\s<>\"'()（）]+", Pattern.CASE_INSENSITIVE);
    private TwimgImageUrls() {}

    public static String canonical(String input) {
        if (input == null) return null;
        try {
            String url = input.replace("&amp;", "&");
            if (url.startsWith("//")) url = "https:" + url;
            else if (url.regionMatches(true, 0, "pbs.twimg.com/", 0, 14)) url = "https://" + url;
            else if (url.regionMatches(true, 0, "ttps://", 0, 7) || url.regionMatches(true, 0, "ttp://", 0, 6)) url = "h" + url;
            URI uri = URI.create(url);
            if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                    || !"pbs.twimg.com".equalsIgnoreCase(uri.getHost())
                    || uri.getUserInfo() != null || uri.getPort() != -1) return null;
            String path = uri.getRawPath();
            if (path == null || !path.matches("/media/[A-Za-z0-9_-]+(?:\\.(?i:png|jpe?g|gif|webp))?")) return null;
            if (path.indexOf('.') < 0) {
                String format = null;
                if (uri.getRawQuery() != null) {
                    for (String parameter : uri.getRawQuery().split("&")) {
                        int separator = parameter.indexOf('=');
                        if (separator < 0 || !"format".equals(URLDecoder.decode(parameter.substring(0, separator), "UTF-8"))) continue;
                        String value = URLDecoder.decode(parameter.substring(separator + 1), "UTF-8").toLowerCase(Locale.ROOT);
                        if (format != null && !format.equals(value)) return null;
                        format = value;
                    }
                }
                if (format == null || !format.matches("png|jpe?g|gif|webp")) return null;
                path += "." + format;
            }
            // Retain name=900x900/orig etc., rather than silently changing image size.
            return "https://pbs.twimg.com" + path
                    + (uri.getRawQuery() == null ? "" : "?" + uri.getRawQuery());
        } catch (IllegalArgumentException | java.io.UnsupportedEncodingException error) {
            return null;
        }
    }

    public static String[] extract(String body) {
        LinkedHashSet<String> urls = new LinkedHashSet<>();
        if (body != null) {
            Matcher matcher = URL.matcher(body);
            while (matcher.find() && urls.size() < 16) {
                String url = canonical(matcher.group());
                if (url != null) urls.add(url);
            }
        }
        return urls.toArray(new String[0]);
    }
}
