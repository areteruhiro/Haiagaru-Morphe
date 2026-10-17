package app.morphe.extension.chmate;

import java.net.URI;
import java.util.Locale;

/** No network requests and no mutation until the user explicitly inserts a command. */
public final class ThreadCommandRules {
    private ThreadCommandRules() {}
    public static String profile(String url) {
        try {
            String h = new URI(url).getHost();
            if (h == null) return "custom";
            h = h.toLowerCase(Locale.ROOT);
            if (h.equals("5ch.net") || h.endsWith(".5ch.net") || h.equals("5ch.io")
                    || h.endsWith(".5ch.io") || h.equals("bbspink.com") || h.endsWith(".bbspink.com")) return "5ch";
            if (h.equals("bbs.eddibb.cc") || h.equals("d1ch.cc") || h.equals("v1ch.cc")) return "edge";
        } catch (Exception ignored) {}
        return "custom";
    }
    public static String[] labels(String profile) {
        if (profile.equals("5ch")) return new String[]{"ワッチョイを表示", "ワッチョイ＋IPを表示（IP公開に注意）", "自由入力"};
        if (profile.equals("edge")) return new String[]{"ワッチョイを表示", "ワッチョイ＋レベルを表示", "レベルを表示", "自由入力"};
        return new String[]{"自由入力（掲示板の仕様を確認してください）"};
    }
    public static String[] values(String profile) {
        if (profile.equals("5ch")) return new String[]{triple("!extend:checked:vvvvv:1000:512"), triple("!extend:checked:vvvvvv:1000:512"), ""};
        if (profile.equals("edge")) return new String[]{triple("!metadent:vv:"), triple("!metadent:vvv:"), "!metadent:v:", ""};
        return new String[]{""};
    }
    private static String triple(String line) { return line + "\n" + line + "\n" + line; }
    public static String normalize(String value) { return value.replace("\r\n", "\n").replace('\r', '\n').trim(); }
    public static String editValue(String initial, String saved) {
        // Upgrade the old, unedited one-line default without overwriting custom commands.
        if (saved == null || (initial.contains("\n") && normalize(saved).equals(initial.split("\n")[0]))) return initial;
        return saved;
    }
    public static boolean contains(String body, String command) {
        for (String line : body.split("\n", -1)) if (line.trim().equals(command.trim())) return true;
        return false;
    }
    public static boolean conflicts(String body, String command) {
        String[] added = normalize(command).split("\n", -1);
        String[] existing = normalize(body).split("\n", -1);
        for (int i = 0; i < added.length; i++) {
            String candidate = added[i].trim();
            String family = candidate.startsWith("!extend:") ? "!extend:" : candidate.startsWith("!metadent:") ? "!metadent:" : "";
            if (family.isEmpty()) continue;
            for (String line : existing) if (line.trim().startsWith(family) && !line.trim().equals(candidate)) return true;
            for (int j = 0; j < i; j++) if (added[j].trim().startsWith(family) && !added[j].trim().equals(candidate)) return true;
        }
        return false;
    }
    public static String insert(String body, String command) {
        return normalize(command) + "\n" + body;
    }
}
