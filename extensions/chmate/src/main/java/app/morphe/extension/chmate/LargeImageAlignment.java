package app.morphe.extension.chmate;

import android.graphics.Matrix;

/** Keep the viewer centre independent of the resolution of its draft bitmap. */
public final class LargeImageAlignment {
    private LargeImageAlignment() {}

    public static float offset(int contentSize, float bitmapScale, float displayScale) {
        if (contentSize <= 0 || Float.isNaN(bitmapScale) || Float.isInfinite(bitmapScale)
                || Float.isNaN(displayScale) || Float.isInfinite(displayScale)
                || bitmapScale <= 0 || displayScale <= 0) return 0;
        return contentSize * (displayScale - bitmapScale) * 0.5f;
    }

    public static void correct(Matrix matrix, int rotatedWidth, int rotatedHeight,
            float bitmapScale, float displayScale) {
        float x = offset(rotatedWidth, bitmapScale, displayScale);
        float y = offset(rotatedHeight, bitmapScale, displayScale);
        if (x != 0 || y != 0) matrix.postTranslate(x, y);
    }
}
