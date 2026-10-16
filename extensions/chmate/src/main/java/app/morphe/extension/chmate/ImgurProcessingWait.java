package app.morphe.extension.chmate;

import java.io.IOException;
import java.io.InterruptedIOException;

/** Bounded processing polling, deliberately separate from the non-repeatable upload. */
public final class ImgurProcessingWait {
    public static final int DEFAULT_SECONDS = 120;
    public static final int MAX_SECONDS = 1800;
    private ImgurProcessingWait() { }
    public interface Source<T> {
        boolean ready(T value);
        T fetch(int remainingMillis) throws Exception;
    }
    public interface Clock {
        long millis();
        void sleep(long millis) throws InterruptedException;
    }
    public static int seconds(int value) { return Math.max(0, Math.min(MAX_SECONDS, value)); }
    public static <T> T await(T initial, int seconds, Source<T> source, Clock clock) throws Exception {
        long deadline = clock.millis() + seconds(seconds) * 1000L;
        T value = initial;
        while (!source.ready(value)) {
            long remaining = deadline - clock.millis();
            if (remaining <= 0) throw new IOException("Imgurの処理待ち時間を超えました。再送せず「投稿一覧」を確認してください");
            try { clock.sleep(Math.min(2000, remaining)); }
            catch (InterruptedException error) {
                Thread.currentThread().interrupt();
                throw new InterruptedIOException("Imgurの処理待ちを中断しました。再送せず投稿一覧を確認してください");
            }
            remaining = deadline - clock.millis();
            if (remaining <= 0) continue;
            value = source.fetch((int) Math.min(Integer.MAX_VALUE, remaining));
            if (clock.millis() > deadline) throw new IOException("Imgurの処理待ち時間を超えました。再送せず投稿一覧を確認してください");
        }
        return value;
    }
}
