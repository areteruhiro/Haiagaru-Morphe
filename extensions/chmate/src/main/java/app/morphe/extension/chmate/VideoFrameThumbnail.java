package app.morphe.extension.chmate;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

/** Bounded downloads, one worker, local decoder: never fetch or decode on the UI thread. */
final class VideoFrameThumbnail {
    private static final java.util.concurrent.ExecutorService WORKER = Executors.newSingleThreadExecutor();
    private static final Set<String> ATTEMPTED = Collections.newSetFromMap(new ConcurrentHashMap<>());
    static void request(String url) {
        if (ATTEMPTED.size() >= 128 || !ATTEMPTED.add(url)) return;
        android.util.Log.i("HaiagaruMedia", "Request video frame: " + url);
        WORKER.execute(() -> generate(url));
    }
    private static void generate(String url) {
        Context context = Haiagaru.applicationContextForExtension();
        if (context == null) return;
        File video = null;
        HttpURLConnection connection = null;
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        Bitmap frame = null;
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(url.getBytes("UTF-8"));
            StringBuilder key = new StringBuilder();
            for (byte b : digest) key.append(String.format(java.util.Locale.ROOT, "%02x", b & 255));
            File directory = new File(context.getCacheDir(), "haiagaru-video-frames");
            if (!directory.exists() && !directory.mkdirs()) return;
            File image = new File(directory, key + ".jpg");
            if (!image.exists()) {
                if (Haiagaru.mediaAutoFetchMode() == 3) return;
                video = File.createTempFile("frame-", ".mp4", directory);
                long maxBytes = Haiagaru.videoThumbnailMaxMb() * 1024L * 1024L;
                long timeoutMs = Haiagaru.videoThumbnailTimeoutSeconds() * 1000L;
                long started = android.os.SystemClock.elapsedRealtime();
                connection = (HttpURLConnection) new URL(url).openConnection();
                connection.setInstanceFollowRedirects(false);
                connection.setConnectTimeout((int) Math.min(8000L, timeoutMs));
                connection.setReadTimeout((int) Math.min(8000L, timeoutMs));
                int status = connection.getResponseCode();
                if (status != 200 || connection.getContentLengthLong() > maxBytes) {
                    android.util.Log.w("HaiagaruMedia", "Video frame download rejected: HTTP " + status
                            + ", bytes=" + connection.getContentLengthLong() + ", limit=" + maxBytes);
                    return;
                }
                try (InputStream input = connection.getInputStream(); OutputStream output = new FileOutputStream(video)) {
                    byte[] buffer = new byte[8192];
                    long total = 0;
                    int count;
                    while (true) {
                        long remaining = timeoutMs - (android.os.SystemClock.elapsedRealtime() - started);
                        if (remaining <= 0) return;
                        connection.setReadTimeout((int) Math.min(8000L, remaining));
                        count = input.read(buffer);
                        if (count == -1) break;
                        total += count;
                        if (total > maxBytes || android.os.SystemClock.elapsedRealtime() - started > timeoutMs) return;
                        output.write(buffer, 0, count);
                    }
                }
                retriever.setDataSource(video.getAbsolutePath());
                frame = retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
                if (frame == null) return;
                if (frame.getWidth() > 640 || frame.getHeight() > 640) {
                    float scale = 640f / Math.max(frame.getWidth(), frame.getHeight());
                    Bitmap scaled = Bitmap.createScaledBitmap(frame, Math.max(1, Math.round(frame.getWidth() * scale)),
                            Math.max(1, Math.round(frame.getHeight() * scale)), true);
                    if (scaled != frame) frame.recycle();
                    frame = scaled;
                }
                try (OutputStream output = new FileOutputStream(image)) {
                    if (!frame.compress(Bitmap.CompressFormat.JPEG, 85, output)) { image.delete(); return; }
                }
            }
            String poster = VideoThumbnailServer.register(key.toString(), image);
            XMediaAttachments.setGeneratedPoster(url, poster);
            android.util.Log.i("HaiagaruMedia", "Video frame ready: " + poster);
            ImgurAlbumAttachments.refreshVideoModels();
        } catch (Exception error) {
            android.util.Log.w("HaiagaruMedia", "Video frame generation failed", error);
        } finally {
            if (frame != null) frame.recycle();
            try { retriever.release(); } catch (Exception ignored) {}
            if (connection != null) connection.disconnect();
            if (video != null) video.delete();
        }
    }
}
