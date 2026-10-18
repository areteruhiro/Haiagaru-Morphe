package app.morphe.extension.chmate;

import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Shared parsing rules for public slip indexes; independent of Android UI. */
final class WacchoiSearch {
    static final Pattern SLIP = Pattern.compile(
            "(?<![A-Za-z0-9+/])([A-Za-z0-9+/]{4})-([A-Za-z0-9+/]{4})(?![A-Za-z0-9+/])");
    private static final Pattern PAGE = Pattern.compile(
            "(?i)href\\s*=\\s*['\"]?(?:\\./)?\\?p=(\\d+)");

    static boolean validToken(String token) {
        return token != null && token.matches("[A-Za-z0-9+/]{4}");
    }

    static void selectAllBoards(List<String> available, Set<String> selected) {
        selected.clear();
        selected.addAll(available);
    }

    static boolean allBoardsSelected(List<String> available, Set<String> selected) {
        return !available.isEmpty() && selected.containsAll(available);
    }

    static int nextPage(String html, int current) {
        int next = Integer.MAX_VALUE;
        Matcher matcher = PAGE.matcher(html);
        while (matcher.find()) {
            try {
                int candidate = Integer.parseInt(matcher.group(1));
                if (candidate > current && candidate < next) next = candidate;
            } catch (NumberFormatException ignored) { }
        }
        return next == Integer.MAX_VALUE ? -1 : next;
    }

    static String profileUrl(String base, String href) {
        try {
            URI resolved = URI.create(base).resolve(href.replace("&amp;", "&"));
            String host = resolved.getHost();
            if (!("http".equals(resolved.getScheme()) || "https".equals(resolved.getScheme()))
                    || !("hissi.org".equalsIgnoreCase(host) || "www.hissi.org".equalsIgnoreCase(host))
                    || resolved.getUserInfo() != null || resolved.getPath() == null
                    || !resolved.getPath().matches("/read\\.php/[A-Za-z0-9_-]+/[0-9]{8}/[A-Za-z0-9_-]+\\.html")) {
                return null;
            }
            return resolved.toString();
        } catch (IllegalArgumentException invalid) { return null; }
    }

    private WacchoiSearch() { }
}
