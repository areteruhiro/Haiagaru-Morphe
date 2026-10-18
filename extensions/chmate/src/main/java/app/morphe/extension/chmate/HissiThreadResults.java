package app.morphe.extension.chmate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Extracts titles and posted text from the already downloaded public profile. */
final class HissiThreadResults {
    private static final Pattern POST = Pattern.compile("(?is)<dl\\b[^>]*>(.*?)</dl\\s*>");
    private static final Pattern LINK = Pattern.compile(
            "(?is)<a\\b[^>]*?href\\s*=\\s*['\"]?(https?://[a-z0-9.-]+\\.5ch\\.(?:io|net)/test/read\\.cgi/[^\\s'\">]+)['\"]?[^>]*>(.*?)</a\\s*>");
    private static final Pattern BODY = Pattern.compile("(?is)<dd\\b[^>]*>(.*?)(?:</dd\\s*>|$)");
    static final class Result {
        final String url, titleHtml, bodyHtml;
        Result(String url, String titleHtml, String bodyHtml) {
            this.url = url; this.titleHtml = titleHtml; this.bodyHtml = bodyHtml;
        }
    }
    static List<Result> parse(String html) {
        Map<String, Result> results = new LinkedHashMap<>();
        Matcher posts = POST.matcher(html);
        while (posts.find()) {
            String block = posts.group(1);
            Matcher link = LINK.matcher(block), body = BODY.matcher(block);
            if (!link.find() || !body.find()) continue;
            String url = link.group(1).replace("&amp;", "&");
            results.putIfAbsent(url, new Result(url, link.group(2), body.group(1)));
        }
        // Older profiles may only publish a thread list, without individual posts.
        if (results.isEmpty()) {
            Matcher links = LINK.matcher(html);
            while (links.find()) {
                String url = links.group(1).replace("&amp;", "&");
                results.putIfAbsent(url, new Result(url, links.group(2), ""));
            }
        }
        return new ArrayList<>(results.values());
    }
    private HissiThreadResults() { }
}
