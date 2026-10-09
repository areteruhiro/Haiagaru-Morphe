package app.morphe.extension.chmate;

/** Monotonic, bottom-only timing. Kept independent of Android for regression tests. */
public final class LegacyAutoReloadTiming {
    private final long minimum;
    private long interval;
    private long deadline = -1;
    private int emptyUpdates;
    private boolean loading;

    public LegacyAutoReloadTiming(long minimumMillis) {
        minimum = Math.max(10_000L, Math.min(1_800_000L, minimumMillis));
        interval = minimum;
    }

    public void reading() { deadline = -1; }
    public void bottom(long now) {
        if (!loading && deadline < 0) deadline = now + interval;
    }
    public boolean due(long now) { return !loading && deadline >= 0 && now >= deadline; }
    public void loading() { loading = true; deadline = -1; }
    public boolean isLoading() { return loading; }
    public void completed(boolean hasNew) {
        loading = false;
        deadline = -1;
        if (hasNew) {
            emptyUpdates = 0;
            interval = minimum;
        } else if (++emptyUpdates > 1) {
            interval = Math.min(1_800_000L, interval + interval * 3 / 10);
        }
    }
    public long intervalMillis() { return interval; }

    /** Kotlin Duration stores either nanoseconds or milliseconds with a unit tag. */
    public static long durationMillis(long encoded) {
        long value = encoded >> 1;
        return (encoded & 1L) == 0 ? value / 1_000_000L : value;
    }
}
