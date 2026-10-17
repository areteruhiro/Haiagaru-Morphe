package app.morphe.extension.chmate;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.BaseAdapter;
import java.lang.reflect.Field;
import java.util.List;
import java.util.TimeZone;

/** Exact normal-home hooks; the recent-thread popup does not enter these hooks. */
public final class BrowsingHistory {
    private static volatile Context context;
    private static final String PREFS = "io.github.areteruhiro.chmate.haiagaru.ui-config";
    private BrowsingHistory() { }
    static void initialize(Context value) { context = value.getApplicationContext(); }
    static boolean enabled() { return context != null && prefs().getBoolean("groupBrowsingHistoryByBoard", true); }
    static int days() { return context == null ? 2 : Math.max(0, Math.min(36500, prefs().getInt("browsingHistoryDays", 2))); }
    private static SharedPreferences prefs() { return context.getSharedPreferences(PREFS, 0); }
    private static Field field(Object owner, String name) throws Exception {
        Field value = owner.getClass().getDeclaredField(name); value.setAccessible(true); return value;
    }
    public static List<?> transform(List<?> input, int mode) {
        // Native HOME IDs: 101 = unread, 103 = browsing history, 104 = posted.
        // Never override unread sorting (including its native board-sort option).
        if (input == null || Math.abs(mode) != 103 || !enabled()) return input;
        try {
            return BrowsingHistoryOrder.arrange(input, new BrowsingHistoryOrder.Access<Object>() {
                @Override public String board(Object row) throws Exception {
                    if (row == null) return null;
                    String name = row.getClass().getName();
                    String board;
                    if (name.equals("o.onAdDisplayed$setIcon")) board = "g";
                    else if (name.equals("o.Yy2$open")) board = "b";
                    else if (name.equals("o.getAvailabilityStatus$r8lambdawJ5MHcSJed_CjC7r4OWD0UxyJsQ")) board = "e";
                    else return null;
                    Object id = field(row, board).get(row);
                    if (id == null) throw new IllegalStateException("Missing history BoardID");
                    return id.toString();
                }
                @Override public long visited(Object row) throws Exception {
                    String name = row.getClass().getName();
                    return field(row, name.equals("o.onAdDisplayed$setIcon") ? "c"
                            : name.equals("o.Yy2$open") ? "e" : "d").getLong(row);
                }
            }, BrowsingHistoryOrder.cutoff(System.currentTimeMillis(), days(), TimeZone.getDefault()));
        } catch (Exception error) {
            Log.w("Haiagaru", "Browsing history grouping left native list unchanged", error);
            return input;
        }
    }
    /** Run only after the exact legacy home-sort method has completed. */
    public static void applyLegacy(Object fragment, int mode) {
        if (fragment == null || Math.abs(mode) != 103 || !enabled()) return;
        try {
            boolean legacy191 = fragment.getClass().getName().equals("o.r8lambdaTb_p0z6z2AqSZIga1YhmAVmiTPk");
            Object adapter = field(fragment, legacy191 ? "e" : "i").get(fragment);
            if (!(adapter instanceof BaseAdapter)) return;
            // g/d is the displayed copy; i/g is the native filter source, not the all-history DB model.
            String[] fields = legacy191 ? new String[]{"i", "g"} : new String[]{"g", "d"};
            Object[] replacements = new Object[fields.length];
            for (int i = 0; i < fields.length; i++) {
                Object original = field(adapter, fields[i]).get(adapter);
                if (!(original instanceof List)) return;
                List<?> ordered = transform((List<?>) original, mode);
                if (ordered == original) return;
                replacements[i] = original.getClass().getConstructor(List.class).newInstance(ordered);
            }
            for (int i = 0; i < fields.length; i++) field(adapter, fields[i]).set(adapter, replacements[i]);
            ((BaseAdapter) adapter).notifyDataSetChanged();
        } catch (Exception error) {
            Log.w("Haiagaru", "Normal history adapter left unchanged", error);
        }
    }
}
