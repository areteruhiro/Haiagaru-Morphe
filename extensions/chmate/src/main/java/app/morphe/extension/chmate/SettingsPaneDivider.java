package app.morphe.extension.chmate;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.drawable.Drawable;
import android.view.View;

/** The legacy settings pane paints its divider even when the second pane is off screen. */
public final class SettingsPaneDivider extends Drawable {
    private final View pane;
    private final Drawable original;

    private SettingsPaneDivider(View pane, Drawable original) {
        this.pane = pane;
        this.original = original;
    }

    public static Drawable wrap(View pane, Drawable original) {
        return original == null ? null : new SettingsPaneDivider(pane, original);
    }

    @Override public void draw(Canvas canvas) {
        // A separator belongs between visible panes, never on an outer edge.
        // Use geometry, so this also works in legacy apps without RTL support.
        if (getBounds().left <= 0 || getBounds().right >= pane.getWidth()) return;
        original.setBounds(getBounds());
        original.draw(canvas);
    }

    @Override public int getIntrinsicWidth() { return original.getIntrinsicWidth(); }
    @Override public int getIntrinsicHeight() { return original.getIntrinsicHeight(); }
    @Override public void setAlpha(int alpha) { original.setAlpha(alpha); }
    @Override public void setColorFilter(ColorFilter filter) { original.setColorFilter(filter); }
    @Override public int getOpacity() { return android.graphics.PixelFormat.TRANSLUCENT; }
}
