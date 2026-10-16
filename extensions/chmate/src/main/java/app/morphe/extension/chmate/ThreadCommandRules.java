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
        if (profile.equals("5ch")) return new String[]{"!extend:checked:vvvvv:1000:512", "!extend:checked:vvvvvv:1000:512", ""};
        if (profile.equals("edge")) return new String[]{"!metadent:vv:", "!metadent:vvv:", "!metadent:v:", ""};
        return new String[]{""};
    }
    public static boolean contains(String body, String command) {
        for (String line : body.split("\n", -1)) if (line.trim().equals(command.trim())) return true;
        return false;
    }
    public static boolean conflicts(String body, String command) {
        String family = command.startsWith("!extend:") ? "!extend:" : command.startsWith("!metadent:") ? "!metadent:" : "";
        if (family.isEmpty()) return false;
        for (String line : body.split("\n", -1)) if (line.trim().startsWith(family) && !line.trim().equals(command.trim())) return true;
        return false;
    }
    public static String insert(String body, String command) {
        return contains(body, command) ? body : command + "\n" + body;
    }
}
