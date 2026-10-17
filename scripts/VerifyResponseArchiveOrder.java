package app.morphe.extension.chmate;

public final class VerifyResponseArchiveOrder {
    static int checks;
    static void check(boolean ok, String label) { if (!ok) throw new AssertionError(label); checks++; }
    public static void main(String[] args) {
        long unknown = ResponseArchiveOrder.UNKNOWN;
        long early = ResponseArchiveOrder.postedAt("2026/10/17(土) 21:31:19.27 ID:a");
        long late = ResponseArchiveOrder.postedAt("2026/10/17(Sat) 21:31:19.28 ID:b");
        check(early != unknown && late - early == 10, "centiseconds and weekday ignored");
        check(ResponseArchiveOrder.postedAt("2026/10/17 21:31:19.270") == early, "milliseconds");
        check(ResponseArchiveOrder.postedAt("2026/10/17（六） 21:31:19.2") == early - 70, "fullwidth weekday and tenths");
        check(ResponseArchiveOrder.postedAt("2026/10/17 21:31:19") == early - 270, "no fraction");
        check(ResponseArchiveOrder.postedAt("2024/2/29(木) 1:02:03.00") != unknown, "valid leap day");
        for (String bad : new String[]{null, "", "Over 1000", "2026/2/29(日) 21:31:19", "2026/10/17 25:31:19"})
            check(ResponseArchiveOrder.postedAt(bad) == unknown, "unknown/invalid date");
        for (int mode = 0; mode < 4; mode++) {
            int order = ResponseArchiveOrder.compare(100, late, "a", 200, early, "b", mode);
            check(Integer.signum(order) == (mode == 0 || mode == 3 ? 1 : -1), "mode " + mode);
            check(ResponseArchiveOrder.compare(100, unknown, "a", 200, early, "b", mode >= 2 ? mode : mode + 2) > 0,
                    "unknown posting date last " + mode);
            check(ResponseArchiveOrder.compare(100, early, "a", 100, early, "b", mode) < 0, "stable tie " + mode);
        }
        System.out.println("PASS " + checks + " archive date/order checks");
    }
}
