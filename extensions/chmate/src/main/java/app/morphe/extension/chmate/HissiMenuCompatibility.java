package app.morphe.extension.chmate;

/** Repairs the stock Hissi host filter before ChMate expands menu templates. */
public final class HissiMenuCompatibility {
    private HissiMenuCompatibility() {}

    public static String rewriteTemplate(String template) {
        if (template == null || !(template.contains("://hissi.org/read.php/"))) {
            return template;
        }
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
        String rewritten = template.replace("http://hissi.org/read.php/",
                "haiagaru-hissi://hissi.org/read.php/")
                .replace("https://hissi.org/read.php/",
                        "haiagaru-hissis://hissi.org/read.php/");
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
}
