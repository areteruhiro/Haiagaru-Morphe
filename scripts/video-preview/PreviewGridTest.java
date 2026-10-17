package app.morphe.extension.chmate;
import android.graphics.*;
import android.media.MediaMetadataRetriever;
import java.lang.reflect.Method;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;

/** Actual Android Canvas renderer; deterministic synthetic frames, no network/preferences changes. */
public final class PreviewGridTest {
    static int checks;
    static void check(boolean ok) { if (!ok) throw new AssertionError("grid " + checks); checks++; }
    public static final class Frames extends MediaMetadataRetriever {
        final ArrayList<Long> times = new ArrayList<>();
        final int[] colors = {Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW};
        boolean fail;
        @Override public Bitmap getScaledFrameAtTime(long time, int option, int w, int h) {
            times.add(time);
            if (fail) return null;
            Bitmap frame = Bitmap.createBitmap(160, 90, Bitmap.Config.RGB_565);
            frame.eraseColor(colors[(times.size()-1) % 4]); return frame;
        }
    }
    public static String run(android.content.Context context) throws Exception {
        Method method = VideoFrameThumbnail.class.getDeclaredMethod("grid", MediaMetadataRetriever.class, long.class, int.class);
        method.setAccessible(true);
        Frames frames = new Frames();
        Bitmap grid = null;
        try {
            grid = (Bitmap)method.invoke(null, frames, 80000L, 4);
            check(grid.getWidth() == 640 && grid.getHeight() == 640);
            for (int i = 0; i < 4; i++) {
                check(frames.times.get(i) == (10000L + i * 20000) * 1000);
                check(grid.getPixel(i % 2 * 320 + 160, i / 2 * 320 + 160) == frames.colors[i]);
            }
            File output = new File(context.getCacheDir(), "video-preview-grid-test.png");
            try (FileOutputStream out = new FileOutputStream(output)) { grid.compress(Bitmap.CompressFormat.PNG,100,out); }
            Frames missing = new Frames(); missing.fail = true;
            try { check(method.invoke(null, missing, 80000L, 4) == null); } finally { missing.release(); }
            for (int count : new int[]{1, 3, 5, 7, 9, 64}) {
                Frames many = new Frames(); Bitmap mosaic = null;
                try {
                    mosaic = (Bitmap)method.invoke(null, many, 128000L, count);
                    check(mosaic != null && many.times.size() == count);
                    for (int i = 0; i < count; i++) check(many.times.get(i) == VideoPreviewPolicy.positionMs(128000L, i, count) * 1000);
                    check(mosaic.getWidth() <= 640 && mosaic.getHeight() <= 640);
                } finally { if (mosaic != null) mosaic.recycle(); many.release(); }
            }
            return "PASS " + checks + " Android grid checks; " + output;
        } finally { if (grid != null) grid.recycle(); frames.release(); }
    }
}
