package app.morphe.extension.chmate;

import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

/** Safe, unique filenames. Signed query parameters are never copied into the filename. */
public final class VideoDownloadNames {
    private VideoDownloadNames() { }
    public static String filename(String url) {
        String stem = "video";
        try {
            String path = new URI(url).getPath();
            if (path != null) {
                String last = path.substring(path.lastIndexOf('/') + 1);
                if (last.toLowerCase(Locale.ROOT).endsWith(".mp4")) last = last.substring(0, last.length() - 4);
                last = last.replaceAll("[^A-Za-z0-9_-]", "_");
                if (!last.isEmpty()) stem = last.substring(0, Math.min(64, last.length()));
            }
        } catch (Exception ignored) { }
        String time = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.ROOT).format(new Date());
        return stem + "-" + time + "-" + UUID.randomUUID().toString().substring(0, 8) + ".mp4";
    }
}
