package app.morphe.extension.chmate;

import java.util.*;
import java.util.regex.*;

/** Pure, conservative planning: same board and flags, plain complete Wacchoi names only. */
public final class NgNameConsolidation {
    private static final Pattern NAME = Pattern.compile("^(?:[^()（）]*[ \\u3000])?[（(][^\\s()（）]+[ \\u3000]+[A-Za-z0-9]{4}-([^\\s()（）<>]{4})[）)]$");
    private NgNameConsolidation() {}
    public static String suffix(String name, short flags) {
        if ((flags & 0x70) != 0 || name == null) return null;
        Matcher match = NAME.matcher(name.trim());
        return match.matches() ? match.group(1) : null;
    }
    public static String key(String suffix, String board, short flags) {
        return suffix + "\u0000" + (board == null ? "" : board) + "\u0000" + flags;
    }
    public static boolean matches(String pattern, String board, String query, String scope) {
        return (scope == null || Objects.equals(scope, board))
                && (query == null || query.isEmpty() || pattern.toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT)));
    }
}
