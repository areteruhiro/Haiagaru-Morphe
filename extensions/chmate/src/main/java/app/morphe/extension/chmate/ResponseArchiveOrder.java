package app.morphe.extension.chmate;

import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Chronological keys, independent of translated weekday labels and response IDs. */
final class ResponseArchiveOrder {
    static final long UNKNOWN = Long.MIN_VALUE;
    private static final Pattern DATE = Pattern.compile(
            "(\\d{4}/\\d{1,2}/\\d{1,2})(?:[（(][^）)]*[）)])?\\s+(\\d{1,2}:\\d{2}:\\d{2})(?:\\.(\\d{1,3}))?");
    static long postedAt(String date) {
        Matcher match = DATE.matcher(date == null ? "" : date);
        if (!match.find()) return UNKNOWN;
        String fraction = match.group(3);
        fraction = fraction == null ? "000" : (fraction + "000").substring(0, 3);
        String value = match.group(1) + " " + match.group(2) + "." + fraction;
        SimpleDateFormat format = new SimpleDateFormat("yyyy/M/d H:mm:ss.SSS", Locale.ROOT);
        format.setTimeZone(TimeZone.getTimeZone("Asia/Tokyo"));
        format.setLenient(false);
        ParsePosition position = new ParsePosition(0);
        java.util.Date parsed = format.parse(value, position);
        return parsed == null || position.getIndex() != value.length() ? UNKNOWN : parsed.getTime();
    }
    // Modes: saved descending/ascending, posted descending/ascending. Unknown dates stay last.
    static int compare(long savedA, long postedA, String idA, long savedB, long postedB, String idB, int mode) {
        long a = mode >= 2 ? postedA : savedA, b = mode >= 2 ? postedB : savedB;
        if (a == UNKNOWN && b != UNKNOWN) return 1;
        if (b == UNKNOWN && a != UNKNOWN) return -1;
        int result = (mode & 1) == 0 ? Long.compare(b, a) : Long.compare(a, b);
        return result != 0 ? result : idA.compareTo(idB);
    }
}
