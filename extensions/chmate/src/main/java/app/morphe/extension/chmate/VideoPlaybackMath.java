package app.morphe.extension.chmate;

/** Overflow-safe relative seeking, shared with gesture tests. */
public final class VideoPlaybackMath {
    private VideoPlaybackMath() { }
    public static int seekTarget(int position, int duration, int seconds) {
        if (duration <= 0) return Math.max(0, position);
        return (int) Math.max(0L, Math.min(duration - 1L, position + seconds * 1000L));
    }
}
