package app.morphe.extension.chmate;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

/** Repairs the stock Hissi host filter before ChMate expands menu templates. */
public final class HissiMenuCompatibility {
    private HissiMenuCompatibility() {}

    public static String rewriteTemplate(String template) {
        // The long-press action is not identical to the settings-menu action
        // on every ChMate generation.  Some builds omit the slash after the
        // host, use a protocol-relative URL, or already contain an expanded
        // URL.  Match the stable Hissi path instead of one exact spelling.
        if (template == null || !template.contains("hissi.org/read.php")) {
            return template;
        }
        // The patch option can deliberately omit the in-app Activity. In that
        // build keep ChMate's original template instead of emitting an
        // unhandled haiagaru-hissi:// URI.
        if (!Haiagaru.dedicatedCheckerViewerAvailable()) return template;
        String stockFilter = "{$host[match:[25]ch.net$]}";
        String oldFilter = "{$host[match:(^|\\.)(2ch\\.net|5ch\\.(net|io))$]}";
        String supportedFilter = "{$host[match:(?:^|\\.)(?:2ch\\.net|5ch\\.(?:net|io)|"
                + "bbspink\\.com|open2ch\\.net|machi\\.to|vip2ch\\.com|5chan\\.jp)$|"
                + "^(?:jbbs\\.shitaraba\\.net|bbs\\.eddibb\\.cc|bbs\\.punipuni\\.eu|"
                + "bbs\\.kamemushi\\.com|bbs\\.jpnkn\\.com|bbs\\.3chan\\.cc|"
                + "refugee-chan\\.mobi|yaruozatsudan\\.com|yaruoshelter\\.com|"
                + "yarumakai\\.com|v1ch\\.cc|pinkdarker\\.com)$]}";
        if (!template.contains(stockFilter) && !template.contains(oldFilter)
                && !template.contains("haiagaru_mode=")) {
            return template;
        }
        // Keep the old hosts working, including when domain conversion is disabled.
        // Do not change the destination, ID/date expansion, or the menu preference.
        boolean protocolRelative = template.contains("//hissi.org/read.php/")
                && !template.contains("://hissi.org/read.php/");
        String rewritten = template.replace("http://hissi.org/read.php/",
                "haiagaru-hissi://hissi.org/read.php/")
                .replace("https://hissi.org/read.php/",
                        "haiagaru-hissis://hissi.org/read.php/");
        if (protocolRelative) {
            rewritten = rewritten.replace("//hissi.org/read.php/",
                    "haiagaru-hissi://hissi.org/read.php/");
        }
        String sourceFilter = rewritten.contains(stockFilter) ? stockFilter : oldFilter;
        if (!rewritten.contains(sourceFilter)) {
            // A previously rewritten template only needs its selected mode refreshed.
            return rewritten.replaceAll("&haiagaru_mode=[0-9]+", "&haiagaru_mode="
                    + Haiagaru.hissiCheckerMode());
        }
        return rewritten.replace(sourceFilter,
                "?haiagaru_host={$host}&haiagaru_key={$key}&haiagaru_mode="
                        + Haiagaru.hissiCheckerMode() + supportedFilter);
    }

    /**
     * Rewrites a fully expanded URL immediately before ChMate hands it to
     * ACTION_VIEW.  The ID long-press action in some releases bypasses the
     * menu-template expansion hook, so this second boundary is required for
     * that route as well.
     */
    public static String rewriteExternalUrl(String url) {
        if (url == null || !Haiagaru.dedicatedCheckerViewerAvailable()) return url;
        if (url.startsWith("http://hissi.org/read.php/")) {
            return "haiagaru-hissi://hissi.org/read.php/" + url.substring(
                    "http://hissi.org/read.php/".length());
        }
        if (url.startsWith("https://hissi.org/read.php/")) {
            return "haiagaru-hissis://hissi.org/read.php/" + url.substring(
                    "https://hissi.org/read.php/".length());
        }
        if (url.startsWith("//hissi.org/read.php/")) {
            return "haiagaru-hissi://hissi.org/read.php/" + url.substring(
                    "//hissi.org/read.php/".length());
        }
        return url;
    }

    /** Keep the checker inside this installed ChMate, even if browsers or old test builds also match. */
    public static void prepareExternalIntent(Intent intent) {
        if (intent == null || !Intent.ACTION_VIEW.equals(intent.getAction())
                || !Haiagaru.dedicatedCheckerViewerAvailable()) return;
        Uri uri = intent.getData();
        if (uri == null || !"hissi.org".equalsIgnoreCase(uri.getHost())
                || uri.getPath() == null || !uri.getPath().startsWith("/read.php/")) return;
        String scheme = uri.getScheme();
        if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)
                && !"haiagaru-hissi".equalsIgnoreCase(scheme)
                && !"haiagaru-hissis".equalsIgnoreCase(scheme)) return;
        Context context = Haiagaru.applicationContextForExtension();
        if (context != null) intent.setClassName(context, HissiMenuActivity.class.getName());
    }
}
