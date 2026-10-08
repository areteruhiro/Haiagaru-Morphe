package app.morphe.extension.chmate;

import java.net.URI;
import java.util.LinkedHashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** Public X embed metadata only; no login credentials or third-party mirrors. */
public final class XMediaAttachments {
    private static final java.util.Map<String, String> POSTERS = java.util.Collections.synchronizedMap(
            new java.util.LinkedHashMap<String, String>() {
                @Override protected boolean removeEldestEntry(java.util.Map.Entry<String, String> entry) {
                    return size() > 256;
                }
            });
    private static final Pattern POST = Pattern.compile(
            "https?://(?:(?:www|mobile)\\.)?(?:x\\.com|twitter\\.com)/(?:[A-Za-z0-9_]+/status|i/web/status)/(\\d+)(?!\\d)");
    private static final Pattern DIRECT = Pattern.compile(
            "https://video\\.twimg\\.com/[^\\s<>\"']+", Pattern.CASE_INSENSITIVE);

    private XMediaAttachments() {}

    public static Object nativeVideo(String url, String className) {
        if (url == null || !validMedia(url, true)) return null;
        int mode = Haiagaru.mediaAutoFetchMode();
        if (mode == 0 || mode == 3) return null;
        if (!POSTERS.containsKey(url)) VideoFrameThumbnail.request(url);
        try {
            java.lang.reflect.Constructor<?> constructor = Class.forName(className)
                    .getDeclaredConstructor(String.class, String.class);
            constructor.setAccessible(true);
            String poster = POSTERS.get(url);
            if (poster == null) poster = VideoThumbnailServer.placeholder();
            return constructor.newInstance(url, poster);
        } catch (Exception error) {
            android.util.Log.w("HaiagaruMedia", "Native video projection unavailable", error);
            return null;
        }
    }

    static void setGeneratedPoster(String url, String poster) { POSTERS.put(url, poster); }
    static boolean isPoster(String url) { return POSTERS.containsValue(url); }
    static String generatedPoster(String url) { return POSTERS.get(url); }
    public static boolean isVideo(String url) { return validMedia(url, true); }

    public static String[] postIds(String body) {
        LinkedHashSet<String> ids = new LinkedHashSet<>();
        Matcher matcher = POST.matcher(body);
        while (matcher.find() && ids.size() < 8) ids.add(matcher.group(1));
        return ids.toArray(new String[0]);
    }

    public static String[] directVideos(String body) {
        LinkedHashSet<String> urls = new LinkedHashSet<>();
        Matcher matcher = DIRECT.matcher(body);
        while (matcher.find() && urls.size() < 8) {
            String url = matcher.group().replace("&amp;", "&");
            if (validMedia(url, true)) urls.add(url);
        }
        return urls.toArray(new String[0]);
    }

    private static boolean validMedia(String url, boolean video) {
        try {
            URI uri = URI.create(url);
            return "https".equalsIgnoreCase(uri.getScheme()) && uri.getUserInfo() == null
                    && uri.getPort() == -1
                    && (video ? "video.twimg.com".equalsIgnoreCase(uri.getHost())
                        || (uri.getHost() != null && (uri.getHost().endsWith(".cdninstagram.com") || uri.getHost().endsWith(".fbcdn.net")))
                        : "pbs.twimg.com".equalsIgnoreCase(uri.getHost()))
                    && uri.getPath() != null
                    && uri.getPath().matches(video ? "(?i).+\\.mp4" : "(?i).+\\.(jpg|jpeg|png|webp|gif)");
        } catch (IllegalArgumentException error) { return false; }
    }

    public static String[] fromJson(String json, String expectedId) throws JSONException {
        JSONObject post = new JSONObject(json);
        if (!expectedId.equals(post.optString("id_str"))) return new String[0];
        JSONArray media = post.optJSONArray("mediaDetails");
        if (media == null || media.length() == 0) media = post.optJSONArray("photos");
        LinkedHashSet<String> urls = new LinkedHashSet<>();
        if (media == null) return new String[0];
        for (int i = 0; i < media.length() && i < 16; i++) {
            JSONObject item = media.optJSONObject(i);
            if (item == null) continue;
            String thumbnail = item.optString("media_url_https");
            if (thumbnail.isEmpty()) thumbnail = item.optString("url");
            JSONObject info = item.optJSONObject("video_info");
            JSONArray variants = info == null ? null : info.optJSONArray("variants");
            String best = null;
            long bitrate = -1;
            if (variants != null) for (int j = 0; j < variants.length(); j++) {
                JSONObject variant = variants.optJSONObject(j);
                if (variant == null || !"video/mp4".equals(variant.optString("content_type"))) continue;
                String url = variant.optString("url");
                long rate = variant.optLong("bitrate", 0);
                if (validMedia(url, true) && rate > bitrate) { best = url; bitrate = rate; }
            }
            if (best != null) {
                if (validMedia(thumbnail, false)) {
                    POSTERS.put(best, thumbnail);
                    urls.add(thumbnail);
                }
                urls.add(best);
            } else if (validMedia(thumbnail, false)) urls.add(thumbnail);
        }
        return urls.toArray(new String[0]);
    }
}
