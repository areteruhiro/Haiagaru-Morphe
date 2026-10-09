package app.morphe.extension.chmate;

import java.net.URI;
import java.util.LinkedHashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

/** Public embed data only: no login, cookies, private-post access or mirror services. */
public final class InstagramMediaAttachments {
    private static final Pattern POST = Pattern.compile(
            "(?<![A-Za-z0-9_./:@-])(?:(?:https?|ttps?)://|//)?"
                    + "(?:www\\.)?instagram\\.com/(?:p|reel|reels|tv)/([A-Za-z0-9_-]+)(?=[/?#\\s<>\"'()（）]|$)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern CONTEXT = Pattern.compile(
            "\"contextJSON\"\\s*:\\s*(\"(?:\\\\.|[^\"\\\\])*+\")");
    private static final Pattern META = Pattern.compile("<meta\\b[^>]*>", Pattern.CASE_INSENSITIVE);
    private static final Pattern PROPERTY = Pattern.compile("property\\s*=\\s*['\"]og:(image|video)['\"]", Pattern.CASE_INSENSITIVE);
    private static final Pattern CONTENT = Pattern.compile("content\\s*=\\s*['\"]([^'\"]+)['\"]", Pattern.CASE_INSENSITIVE);

    private InstagramMediaAttachments() {}

    public static String[] postIds(String body) {
        LinkedHashSet<String> ids = new LinkedHashSet<>();
        if (body == null) return new String[0];
        Matcher matches = POST.matcher(body);
        while (matches.find() && ids.size() < 8) ids.add(matches.group(1));
        return ids.toArray(new String[0]);
    }

    public static boolean validMedia(String url, boolean video) {
        try {
            URI uri = URI.create(url);
            String host = uri.getHost();
            return "https".equalsIgnoreCase(uri.getScheme()) && uri.getUserInfo() == null
                    && uri.getPort() == -1 && host != null
                    && (host.endsWith(".cdninstagram.com") || host.endsWith(".fbcdn.net"))
                    && uri.getPath() != null
                    && uri.getPath().matches(video ? "(?i).+\\.mp4" : "(?i).+\\.(jpg|jpeg|png|webp)");
        } catch (IllegalArgumentException error) { return false; }
    }

    public static String[] fromHtml(String html, String expectedCode) {
        LinkedHashSet<String> urls = new LinkedHashSet<>();
        Matcher contexts = CONTEXT.matcher(html);
        while (contexts.find()) {
            try {
                Object decoded = new JSONTokener(contexts.group(1)).nextValue();
                if (!(decoded instanceof String)) continue;
                JSONObject root = new JSONObject((String) decoded);
                JSONObject data = root.optJSONObject("gql_data");
                JSONObject post = data == null ? null : data.optJSONObject("shortcode_media");
                if (post == null || !expectedCode.equals(post.optString("shortcode"))) continue;
                JSONObject context = root.optJSONObject("context");
                if (context != null && context.optBoolean("copyright_blocked")) return new String[0];
                JSONObject children = post.optJSONObject("edge_sidecar_to_children");
                JSONArray edges = children == null ? null : children.optJSONArray("edges");
                if (edges == null || edges.length() == 0) append(post, urls);
                else for (int i = 0; i < edges.length() && i < 20; i++) {
                    JSONObject edge = edges.optJSONObject(i);
                    JSONObject node = edge == null ? null : edge.optJSONObject("node");
                    if (node != null) append(node, urls);
                }
                return urls.toArray(new String[0]);
            } catch (org.json.JSONException ignored) {
                // A site format change must not crash the response renderer.
            }
        }
        // Only the post's explicit OpenGraph media, never arbitrary img tags.
        Matcher metas = META.matcher(html);
        String image = null, video = null;
        while (metas.find()) {
            Matcher property = PROPERTY.matcher(metas.group());
            Matcher content = CONTENT.matcher(metas.group());
            if (!property.find() || !content.find()) continue;
            String url = content.group(1).replace("&amp;", "&");
            if ("video".equalsIgnoreCase(property.group(1)) && validMedia(url, true)) video = url;
            else if ("image".equalsIgnoreCase(property.group(1)) && validMedia(url, false)) image = url;
        }
        if (image != null) urls.add(image);
        if (video != null) {
            if (image != null) XMediaAttachments.setGeneratedPoster(video, image);
            urls.add(video);
        }
        return urls.toArray(new String[0]);
    }

    private static void append(JSONObject node, LinkedHashSet<String> urls) {
        String image = node.optString("display_url");
        String video = node.optString("video_url");
        if (validMedia(image, false)) urls.add(image);
        if (node.optBoolean("is_video") && validMedia(video, true)) {
            if (validMedia(image, false)) XMediaAttachments.setGeneratedPoster(video, image);
            urls.add(video);
        }
    }
}
