package app.morphe.extension.chmate;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** URL-encoded variables; optional substring indices are zero-based, end-exclusive. */
final class ArchiveRouteVariables {
    static final String VIP_TEMPLATE = "https://{$host}/{$bbs}/kako/{$key:0:4}/{$key:0:5}/{$key}.dat";
    private static final Pattern TOKEN = Pattern.compile("\\{\\$([a-z]+)(?::(\\d+):(\\d+))?\\}");
    private ArchiveRouteVariables() {}

    static boolean containsVariable(String template, String name) {
        Matcher matcher = TOKEN.matcher(template);
        while (matcher.find()) if (name.equals(matcher.group(1))) return true;
        return false;
    }

    static boolean isVipThread(String url) {
        try {
            URI uri = new URI(url);
            return ("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                    && "ex14.vip2ch.com".equalsIgnoreCase(uri.getHost())
                    && uri.getRawUserInfo() == null && uri.getPort() == -1
                    && uri.getPath().matches("/test/read\\.(?:cgi|php)/news4ssnip/\\d{9,10}(?:/.*)?");
        } catch (Exception ignored) { return false; }
    }

    static String resolve(String template, String server, String host, String board, String key, String random)
            throws IOException {
        Map<String, String> values = new HashMap<>();
        values.put("server", server); values.put("host", host); values.put("bbs", board);
        values.put("key", key); values.put("rand", random);
        Matcher matcher = TOKEN.matcher(template);
        StringBuffer output = new StringBuffer();
        while (matcher.find()) {
            String value = values.get(matcher.group(1));
            if (value == null) throw new IOException("Unknown DAT route variable");
            if (matcher.group(2) != null) {
                try {
                    int start = Integer.parseInt(matcher.group(2)), end = Integer.parseInt(matcher.group(3));
                    if (start < 0 || end <= start || end > value.length()) throw new IllegalArgumentException();
                    value = value.substring(start, end);
                } catch (IllegalArgumentException error) { throw new IOException("Invalid DAT route slice", error); }
            }
            matcher.appendReplacement(output, Matcher.quoteReplacement(URLEncoder.encode(value, "UTF-8")));
        }
        matcher.appendTail(output);
        if (output.indexOf("{$") >= 0) throw new IOException("Invalid DAT route variable");
        try {
            URI uri = new URI(output.toString());
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null || uri.getRawUserInfo() != null)
                throw new IOException("DAT routes require an HTTPS URL without user information");
        } catch (java.net.URISyntaxException error) { throw new IOException("Invalid DAT route URL", error); }
        return output.toString();
    }
}
