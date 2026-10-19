package app.morphe.extension.chmate;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import java.io.File;
import java.lang.reflect.Field;

/** Haiagaru toolbar actions; stock icons and stored toolbar order stay untouched. */
public final class ToolbarIcons {
    static final String PREFS = "haiagaru_toolbar_icons";
    static final String[] NAMES = {"エッヂ過去ログ", "フィルタ", "すべて既読", "長押しコピー", "ﾜｯﾁｮｲ下4桁で検索", "ﾜｯﾁｮｲ上4桁で検索"};
    static final String[] RESOURCES = {"haiagaru_edge_archive", "haiagaru_quick_filter", "haiagaru_mark_all_read", "haiagaru_direct_copy", "haiagaru_wacchoi_lower", "haiagaru_wacchoi_upper"};
    private static final Bitmap[] CACHE = new Bitmap[NAMES.length];
    private ToolbarIcons() {}
    static int index(int action) { int i = action - 0x7e000001; return i >= 0 && i < NAMES.length ? i : -1; }
    public static boolean visible(Context context, int action) {
        int i = index(action);
        if((i==4||i==5)&&!FeatureSwitches.enabled("wacchoiSearchEnabled",true))return false;
        return i < 0 || context.getSharedPreferences(PREFS, 0).getBoolean("visible." + i, i<4);
    }
    static File file(Context context, int i) { return new File(context.getFilesDir(), "haiagaru-toolbar-icons/" + i + ".png"); }
    static synchronized void invalidate(int i) { CACHE[i] = null; }
    private static final class PreviewItem {
        final int action;
        PreviewItem(int i) { action = 0x7e000001 + i; }
    }
    static Drawable preview(Context context, int i, int tint) {
        int id = context.getResources().getIdentifier(RESOURCES[i], "drawable", context.getPackageName());
        Drawable base = context.getResources().getDrawable(id, context.getTheme()).mutate();
        base.setTint(tint);
        return replace(context, new PreviewItem(i), tint, base);
    }
    /** Called only by the toolbar renderer, never by constructors, editors or serialization. */
    public static boolean visibleForToolbar(Object item, boolean selected) {
        if (!selected) return false;
        Context context = Haiagaru.applicationContextForExtension();
        if (context == null || item == null) return selected;
        try {
            for (Field field : item.getClass().getDeclaredFields()) {
                if (field.getType() != int.class || java.lang.reflect.Modifier.isStatic(field.getModifiers())) continue;
                field.setAccessible(true); int action = field.getInt(item);
                if (index(action) >= 0) return visible(context, action);
            }
        } catch (ReflectiveOperationException | RuntimeException ignored) { }
        return selected;
    }
    public static Drawable replaceNative(Object item, Drawable original) {
        // Native methods reuse Context's parameter register for the returned Drawable.
        // Never read their overwritten parameter registers at a return-site hook.
        Context context = Haiagaru.applicationContextForExtension();
        return context == null ? original : replace(context, item, 0, original);
    }
    public static Drawable replace(Context context, Object item, int color, Drawable original) {
        int i = -1;
        try {
            for (Field f : item.getClass().getDeclaredFields()) {
                if (f.getType() != int.class || java.lang.reflect.Modifier.isStatic(f.getModifiers())) continue;
                f.setAccessible(true); int candidate = index(f.getInt(item));
                if (candidate >= 0) { if (i >= 0) return original; i = candidate; }
            }
            if (i < 0) return original;
            Bitmap bitmap;
            synchronized (ToolbarIcons.class) {
                bitmap = CACHE[i];
                File image = file(context, i);
                if (bitmap == null && image.isFile()) {
                    // Files are normalized to <=256px on import. Do not decode arbitrary originals here.
                    BitmapFactory.Options bounds = new BitmapFactory.Options(); bounds.inJustDecodeBounds = true;
                    BitmapFactory.decodeFile(image.getPath(), bounds);
                    if (bounds.outWidth > 0 && bounds.outWidth <= 256 && bounds.outHeight > 0 && bounds.outHeight <= 256)
                        CACHE[i] = bitmap = BitmapFactory.decodeFile(image.getPath());
                }
            }
            if (bitmap != null) {
                final int size = Math.round(24 * context.getResources().getDisplayMetrics().density);
                BitmapDrawable custom = new BitmapDrawable(context.getResources(), bitmap) {
                    @Override public int getIntrinsicWidth() { return size; }
                    @Override public int getIntrinsicHeight() { return size; }
                };
                custom.setFilterBitmap(true); custom.setAntiAlias(true);
                return custom; // Preserve user image colors rather than applying the theme tint.
            }
            // Native color=0 means theme foreground, not transparent. Preserve the native
            // renderer's tint, user theme overrides and enabled/disabled state by default.
            return original;
        } catch (ReflectiveOperationException | RuntimeException error) { return original; }
    }
    static void addSettingsButton(android.widget.LinearLayout layout, android.app.Activity activity) {
        android.widget.Button button = new android.widget.Button(activity);
        button.setText("ツールバー表示・追加アイコン");
        button.setOnClickListener(v -> activity.startActivity(new android.content.Intent(activity, ToolbarIconsActivity.class)));
        layout.addView(button);
    }
}
