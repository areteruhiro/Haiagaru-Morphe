package app.morphe.extension.chmate;

import java.time.Instant;
import java.util.Locale;
import java.util.TimeZone;

public final class VerifyBackupTime {
    private static int checks;
    private static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
        checks++;
    }
    public static void main(String[] args) {
        TimeZone originalZone = TimeZone.getDefault();
        Locale originalLocale = Locale.getDefault();
        try {
            Locale.setDefault(Locale.JAPAN);
            long instant = Instant.parse("2026-10-16T17:32:39Z").toEpochMilli();
            TimeZone.setDefault(TimeZone.getTimeZone("Asia/Tokyo"));
            check(BackupTime.filename(instant).equals("20261017-023239-000"), "Filename uses device timezone across date boundary");
            check(BackupTime.display(instant).equals("2026-10-17 02:32:39"), "Display uses device timezone without offset");
            TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
            check(BackupTime.filename(instant).equals("20261016-173239-000"), "Filename reflects changed device timezone");
            check(BackupTime.display(instant).equals("2026-10-16 17:32:39"), "Display reflects changed device timezone");
            TimeZone.setDefault(TimeZone.getTimeZone("America/Los_Angeles"));
            check(BackupTime.display(instant).equals("2026-10-16 10:32:39"), "Negative offset and daylight saving respected");
            long winter = Instant.parse("2026-12-16T17:32:39Z").toEpochMilli();
            check(BackupTime.display(winter).equals("2026-12-16 09:32:39"), "Winter standard time respected");
            check(TimeZone.getDefault().getID().equals("America/Los_Angeles"), "Formatting never changes the device timezone");
            System.out.println("PASS " + checks + " backup time checks");
        } finally {
            TimeZone.setDefault(originalZone); Locale.setDefault(originalLocale);
        }
    }
}
