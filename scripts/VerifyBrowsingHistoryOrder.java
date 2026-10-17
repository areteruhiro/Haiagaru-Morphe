package app.morphe.extension.chmate;

import java.time.ZonedDateTime;
import java.util.*;

public final class VerifyBrowsingHistoryOrder {
    static int checks;
    record Row(String title, String board, long visited) { }
    static final BrowsingHistoryOrder.Access<Row> ACCESS = new BrowsingHistoryOrder.Access<>() {
        public String board(Row row) { return row.board; }
        public long visited(Row row) { return row.visited; }
    };
    static void check(boolean value, String label) { if (!value) throw new AssertionError(label); checks++; }
    static long date(String iso) { return ZonedDateTime.parse(iso).toInstant().toEpochMilli(); }
    public static void main(String[] args) throws Exception {
        List<Row> rows = Arrays.asList(new Row("a-old", "host/a", 10), new Row("b-new", "host/b", 40),
                new Row("a-new", "host/a", 50), new Row("b-old", "host/b", 20),
                new Row("other-host", "other/a", 30), new Row("heading", null, 99), new Row("not-visited", "host/c", 0));
        List<Row> ordered = BrowsingHistoryOrder.arrange(rows, ACCESS, 0);
        check(ordered.stream().map(Row::title).toList().equals(List.of("a-new", "a-old", "b-new", "b-old", "other-host")), "Latest board, newest thread first; hosts isolated; headings removed");
        check(rows.size() == 7 && rows.get(0).title.equals("a-old"), "Original saved model untouched");
        check(BrowsingHistoryOrder.arrange(rows, ACCESS, 30).stream().map(Row::title).toList().equals(List.of("a-new", "b-new", "other-host")), "Inclusive cutoff and old threads hidden only");
        check(BrowsingHistoryOrder.arrange(rows, ACCESS, 51).isEmpty(), "Empty results");
        check(BrowsingHistoryOrder.arrange(Collections.<Row>emptyList(), ACCESS, 0).isEmpty(), "Empty input");
        List<Row> ties = Arrays.asList(new Row("b1", "b", Long.MAX_VALUE),new Row("a1", "a", Long.MAX_VALUE),new Row("b2", "b", Long.MAX_VALUE));
        check(BrowsingHistoryOrder.arrange(ties, ACCESS, 0).stream().map(Row::title).toList().equals(List.of("b1", "b2", "a1")), "Stable ties, no overflow");
        TimeZone tokyo = TimeZone.getTimeZone("Asia/Tokyo");
        long now = date("2026-10-17T15:30:00+09:00[Asia/Tokyo]");
        check(BrowsingHistoryOrder.cutoff(now, 2, tokyo) == date("2026-10-15T00:00:00+09:00[Asia/Tokyo]"), "Default two calendar days ago");
        check(BrowsingHistoryOrder.cutoff(now, 1, tokyo) == date("2026-10-16T00:00:00+09:00[Asia/Tokyo]"), "Custom days");
        check(BrowsingHistoryOrder.cutoff(now, 0, tokyo) == 0, "Zero unlimited");
        check(BrowsingHistoryOrder.cutoff(date("2026-01-01T00:00:00+09:00[Asia/Tokyo]"),2,tokyo) == date("2025-12-30T00:00:00+09:00[Asia/Tokyo]"), "Year boundary");
        TimeZone ny=TimeZone.getTimeZone("America/New_York");
        check(BrowsingHistoryOrder.cutoff(date("2026-03-09T12:00:00-04:00[America/New_York]"),2,ny) == date("2026-03-07T00:00:00-05:00[America/New_York]"), "Spring DST uses local calendar");
        check(BrowsingHistoryOrder.cutoff(date("2026-11-02T12:00:00-05:00[America/New_York]"),2,ny) == date("2026-10-31T00:00:00-04:00[America/New_York]"), "Fall DST uses local calendar");
        System.out.println("PASS " + checks + " browsing-history order/date tests");
    }
}
