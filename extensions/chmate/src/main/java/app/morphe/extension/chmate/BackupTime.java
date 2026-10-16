package app.morphe.extension.chmate;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/** New formatter per operation so timezone changes are reflected without an app restart. */
final class BackupTime {
    private BackupTime() { }

    static String filename(long epochMillis) {
        return format("yyyyMMdd-HHmmss-SSS", Locale.ROOT, epochMillis);
    }

    static String display(long epochMillis) {
        return format("yyyy-MM-dd HH:mm:ss", Locale.getDefault(), epochMillis);
    }

    private static String format(String pattern, Locale locale, long epochMillis) {
        SimpleDateFormat formatter = new SimpleDateFormat(pattern, locale);
        formatter.setTimeZone(TimeZone.getDefault());
        return formatter.format(new Date(epochMillis));
    }
}
