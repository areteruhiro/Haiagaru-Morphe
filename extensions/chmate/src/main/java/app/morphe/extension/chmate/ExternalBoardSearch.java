package app.morphe.extension.chmate;

import java.util.Locale;

/** Literal, case-insensitive matching of a displayed board name or destination URL. */
final class ExternalBoardSearch {
    static boolean matches(String query,String title,String url) {
        String needle=query==null?"":query.trim().toLowerCase(Locale.ROOT);
        return needle.isEmpty() || (title!=null && title.toLowerCase(Locale.ROOT).contains(needle))
                || (url!=null && url.toLowerCase(Locale.ROOT).contains(needle));
    }
}
