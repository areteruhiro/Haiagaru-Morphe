package app.morphe.extension.chmate;

import java.util.Locale;

/** Literal, case-insensitive AND search. No regex or network requests. */
final class WacchoiHistorySearch {
    static boolean matches(String text,String query){
        if(query==null||query.trim().isEmpty())return true;
        String haystack=(text==null?"":text).toLowerCase(Locale.ROOT);
        for(String word:query.trim().toLowerCase(Locale.ROOT).split("[\\s\\u3000]+"))
            if(!haystack.contains(word))return false;
        return true;
    }
    private WacchoiHistorySearch(){}
}
