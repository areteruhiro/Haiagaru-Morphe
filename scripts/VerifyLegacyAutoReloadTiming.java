import app.morphe.extension.chmate.LegacyAutoReloadTiming;

public final class VerifyLegacyAutoReloadTiming {
    private static void check(boolean condition, String label) {
        if (!condition) throw new AssertionError(label);
    }
    public static void main(String[] args) {
        LegacyAutoReloadTiming timing = new LegacyAutoReloadTiming(10_000);
        check(!timing.due(99_999), "Never reload while initially reading");
        timing.bottom(100_000);
        check(!timing.due(109_999) && timing.due(110_000), "Wait full minimum at bottom");
        timing.loading();
        check(!timing.due(999_999), "Do not duplicate requests during loading");
        timing.completed(true);
        check(!timing.due(999_999), "Do not immediately reload on completion");
        timing.reading();
        check(!timing.due(999_999), "New posts must be read before waiting");
        timing.bottom(1_000_000);
        check(timing.due(1_010_000), "Next bottom arrival starts a new wait");
        timing.loading();
        timing.completed(false);
        check(timing.intervalMillis() == 10_000, "First empty response keeps minimum");
        timing.loading();
        timing.completed(false);
        check(timing.intervalMillis() == 13_000, "Repeated empty responses back off");
        timing.completed(true);
        check(timing.intervalMillis() == 10_000, "New posts reset backoff");
        for (int i = 0; i < 100; i++) timing.completed(false);
        check(timing.intervalMillis() == 1_800_000, "Backoff is capped");
        check(new LegacyAutoReloadTiming(1).intervalMillis() == 10_000, "Minimum ten seconds");
        check(new LegacyAutoReloadTiming(Long.MAX_VALUE).intervalMillis() == 1_800_000, "Maximum thirty minutes");
        check(new LegacyAutoReloadTiming(25_000).intervalMillis() == 25_000, "Respect supplied board/user minimum");
        check(LegacyAutoReloadTiming.durationMillis(10_000_000_000L << 1) == 10_000, "Decode nanosecond Duration");
        check(LegacyAutoReloadTiming.durationMillis((25_000L << 1) | 1) == 25_000, "Decode millisecond Duration");
        System.out.println("Legacy auto-reload timing: all checks passed");
    }
}
