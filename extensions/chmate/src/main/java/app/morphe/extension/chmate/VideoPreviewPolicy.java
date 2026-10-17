package app.morphe.extension.chmate;

/** Pure timing policy shared by every supported video source. */
final class VideoPreviewPolicy {
    static boolean split(boolean enabled, boolean wifiOnly, boolean wifi, long durationMs, int minimumSeconds) {
        return enabled && (!wifiOnly || wifi) && durationMs > 0
                && durationMs >= Math.max(0, minimumSeconds) * 1000L;
    }
    static long positionMs(long durationMs, int index, int count) {
        if (durationMs <= 0 || count < 1 || count > 64 || index < 0 || index >= count) throw new IllegalArgumentException();
        return durationMs / (2 * count) * (index * 2 + 1);
    }
    static int columns(int count) {
        if (count < 1 || count > 64) throw new IllegalArgumentException();
        return (int)Math.ceil(Math.sqrt(count));
    }
    static String time(long milliseconds) {
        long seconds = Math.max(0, milliseconds / 1000);
        return String.format(java.util.Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60);
    }
}
