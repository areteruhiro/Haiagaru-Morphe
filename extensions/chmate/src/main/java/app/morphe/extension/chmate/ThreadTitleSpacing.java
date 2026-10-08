package app.morphe.extension.chmate;

import android.widget.TextView;
import java.util.WeakHashMap;

/** Only called for the thread-row title, never response text or row badges. */
public final class ThreadTitleSpacing {
    private static final WeakHashMap<TextView, Baseline> BASELINES = new WeakHashMap<>();

    private ThreadTitleSpacing() {}

    public static void apply(TextView title) {
        if (title == null) return;
        int percent = Haiagaru.threadTitleSpacingPercent();
        Baseline base = BASELINES.get(title);
        if (base == null) {
            if (percent == 100) return; // Default is a complete no-op.
            base = new Baseline(title);
            BASELINES.put(title, base);
        }
        float multiplier = base.multiplier * percent / 100f;
        float extra = base.extra * percent / 100f;
        boolean includePad = percent > 100 || base.includePad;
        if (title.getLineSpacingMultiplier() != multiplier || title.getLineSpacingExtra() != extra)
            title.setLineSpacing(extra, multiplier);
        if (title.getIncludeFontPadding() != includePad) title.setIncludeFontPadding(includePad);
        // TextView recomputes its measured height; do not impose a fixed row height.
    }

    private static final class Baseline {
        final float multiplier;
        final float extra;
        final boolean includePad;
        Baseline(TextView title) {
            multiplier = title.getLineSpacingMultiplier();
            extra = title.getLineSpacingExtra();
            includePad = title.getIncludeFontPadding();
        }
    }
}
