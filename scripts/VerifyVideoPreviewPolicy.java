package app.morphe.extension.chmate;

public final class VerifyVideoPreviewPolicy {
    static int count;
    static void check(boolean value) { if (!value) throw new AssertionError("case " + count); count++; }
    public static void main(String[] args) {
        check(!VideoPreviewPolicy.split(false, false, true, 60000, 60));
        check(!VideoPreviewPolicy.split(true, true, false, 60000, 60));
        check(VideoPreviewPolicy.split(true, true, true, 60000, 60));
        check(VideoPreviewPolicy.split(true, false, false, 60000, 60));
        check(!VideoPreviewPolicy.split(true, false, true, 59999, 60));
        check(!VideoPreviewPolicy.split(true, false, true, 0, 0));
        check(VideoPreviewPolicy.split(true, false, true, 1, 0));
        for (int i = 0; i < 4; i++) check(VideoPreviewPolicy.positionMs(80000, i, 4) == 10000 + i * 20000);
        for (int n : new int[]{1, 3, 5, 7, 9, 16, 64}) {
            int columns = VideoPreviewPolicy.columns(n);
            check(columns * columns >= n && (columns-1) * (columns-1) < n);
            long previous = -1;
            for (int i = 0; i < n; i++) {
                long time = VideoPreviewPolicy.positionMs(128000, i, n);
                check(time > previous && time < 128000); previous = time;
            }
        }
        check(VideoPreviewPolicy.time(0).equals("0:00"));
        check(VideoPreviewPolicy.time(61000).equals("1:01"));
        check(VideoPreviewPolicy.time(3600000).equals("60:00"));
        System.out.println("PASS " + count + " video preview policy checks");
    }
}
