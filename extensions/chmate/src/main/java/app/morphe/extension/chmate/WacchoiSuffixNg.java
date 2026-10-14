package app.morphe.extension.chmate;

import android.app.AlertDialog;
import android.content.Context;
import android.widget.Toast;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Copies a suffix fragment and opens the native editor; never auto-adds an NG row. */
public final class WacchoiSuffixNg {
    private WacchoiSuffixNg() {}

    // Native NGWord flags: regular expression, chain replies, transparent NG.
    private static final int REGEX = 0x20;
    private static final int CHAIN = 0x08;
    private static final int HIDDEN = 0x01;

    static short registrationFlags(short previous, boolean chain, boolean hidden) {
        return (short) ((previous & ~(0x70 | CHAIN | HIDDEN)) | REGEX
                | (chain ? CHAIN : 0) | (hidden ? HIDDEN : 0));
    }

    public static String suffix(String query) {
        if (query == null) return null;
        Matcher match = Pattern.compile("(?<![A-Za-z0-9])[A-Za-z0-9]{4}-([^\\s()（）<>]{4})(?=$|[\\s()（）<>])").matcher(query);
        return match.find() ? match.group(1) : null;
    }

    public static String expression(String suffix) {
        return "(?<![A-Za-z0-9])[A-Za-z0-9]{4}-(?-i:" + Pattern.quote(suffix)
                + ")(?=$|[\\s()（）<>])";
    }

    public static String clipboardValue(String query) {
        String value = suffix(query);
        return value == null ? null : "-" + value + "|";
    }

    public static void confirm(Context context, String query) {
        confirm(context, query, null);
    }

    public static void confirm(Context context, String query, String currentBoard) {
        String copied = clipboardValue(query);
        if (copied == null) return;
        android.app.Activity activity = Haiagaru.toolbarActivity(context);
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            Toast.makeText(context, "表示中の画面からもう一度お試しください", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager)
                    activity.getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard == null) throw new IllegalStateException("Clipboard unavailable");
            clipboard.setPrimaryClip(android.content.ClipData.newPlainText("ﾜｯﾁｮｲ下4桁", copied));
            try {
                NativeNgEditor.openName(activity);
                Toast.makeText(activity, copied + " をコピーしました。既存のNGへ追記できます。"
                        + "正規表現で保存する場合、末尾の | は次の条件を続けるか削除してください。",
                        Toast.LENGTH_LONG).show();
            } catch (ReflectiveOperationException | RuntimeException error) {
                android.util.Log.w("Haiagaru", "Cannot open native NGName editor", error);
                Toast.makeText(activity, copied + " をコピーしました。NGネーム設定を手動で開いてください。",
                        Toast.LENGTH_LONG).show();
            }
        } catch (RuntimeException error) {
            android.util.Log.w("Haiagaru", "Cannot copy Wacchoi suffix", error);
            Toast.makeText(activity, "下4桁をコピーできませんでした", Toast.LENGTH_LONG).show();
        }
    }

    private static void register(Context context, String regex, boolean chain, boolean hidden, String board) {
        try {
            ClassLoader loader = context.getClassLoader();
            Class<?> storeType = Class.forName("jp.syoboi.a2chMate.ng.NGWord", false, loader);
            Class<?> itemType = Class.forName("jp.syoboi.a2chMate.ng.NGWord$Item", false, loader);
            Object store = null;
            Object nameType = null;
            Class<?> appType = context.getApplicationContext().getClass();
            for (Method method : appType.getDeclaredMethods()) {
                Class<?>[] parameters = method.getParameterTypes();
                if (!Modifier.isStatic(method.getModifiers()) || method.getReturnType() != storeType
                        || parameters.length != 1 || !parameters[0].isEnum()) continue;
                Object name = null;
                for (Object value : parameters[0].getEnumConstants()) {
                    if ("NAME".equals(((Enum<?>) value).name())) name = value;
                }
                if (name == null) continue;
                method.setAccessible(true);
                store = method.invoke(null, name);
                nameType = name;
                break;
            }
            if (store == null) throw new IllegalStateException("Native NGName provider unavailable");
            String version = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
            String snapshotName = "0.8.10.191 dev".equals(version) ? "a"
                    : "0.8.10.242 dev".equals(version) ? "e" : "d";
            java.util.List<?> entries = (java.util.List<?>) storeType.getMethod(snapshotName).invoke(store);
            if (entries == null) throw new IllegalStateException("Native NGName list unavailable");
            Object previous = null;
            short previousFlags = 0;
            long created = System.currentTimeMillis();
            for (Object existing : entries) {
                // toString() returns the native pattern on all supported versions.
                if (!regex.equals(existing.toString())) continue;
                String existingBoard = null;
                short flags = 0;
                long timestamp = created;
                for (Field field : itemType.getDeclaredFields()) {
                    if (Modifier.isStatic(field.getModifiers())) continue;
                    field.setAccessible(true);
                    if (field.getType() == short.class) flags = field.getShort(existing);
                    if (field.getType() == long.class) timestamp = field.getLong(existing);
                    // Ignore the private cached matching key; the two public
                    // strings are the pattern and optional board restriction.
                    if (Modifier.isPublic(field.getModifiers()) && field.getType() == String.class) {
                        Object value = field.get(existing);
                        if (value != null && !regex.equals(value)) existingBoard = value.toString();
                    }
                }
                if (!java.util.Objects.equals(board, existingBoard)) continue;
                previous = existing;
                previousFlags = flags;
                created = timestamp;
                break;
            }
            if (previous == null && entries.size() >= Haiagaru.getNgRegistrationLimit()) {
                Toast.makeText(context, "NGNameの登録上限に達しています", Toast.LENGTH_LONG).show();
                return;
            }
            Object item = itemType.getConstructor(String.class, short.class, long.class, String.class)
                    .newInstance(regex, registrationFlags(previousFlags, chain, hidden), created, board);
            Method update = null;
            for (Method method : storeType.getDeclaredMethods()) {
                Class<?>[] parameters = method.getParameterTypes();
                if (!Modifier.isStatic(method.getModifiers()) && method.getReturnType() == int.class
                        && parameters.length == 2 && parameters[0] == itemType && parameters[1] == itemType) {
                    update = method;
                    break;
                }
            }
            if (update == null) throw new IllegalStateException("Native NGName registration unavailable");
            update.setAccessible(true);
            update.invoke(store, previous, item);
            notifyLegacyNameChange(loader, nameType, itemType, previous, item);
            Toast.makeText(context, previous == null ? "NGNameに登録しました" : "NGNameの設定を更新しました",
                    Toast.LENGTH_SHORT).show();
        } catch (ReflectiveOperationException | android.content.pm.PackageManager.NameNotFoundException | RuntimeException error) {
            android.util.Log.w("Haiagaru", "Cannot register Wacchoi suffix NG", error);
            Toast.makeText(context, "NGNameに登録できませんでした", Toast.LENGTH_LONG).show();
        }
    }

    /** 191 persists the item but does not notify the displayed response list itself. */
    static void notifyLegacyNameChange(ClassLoader loader, Object nameType,
            Class<?> itemType, Object previous, Object item) throws ReflectiveOperationException {
        if (!"o.mgExternalSyntheticLambda3".equals(nameType.getClass().getName())) return;
        Class<?> changeType = Class.forName("o.MaxNativeAdLoaderImplcExternalSyntheticLambda0", false, loader);
        Object change = changeType.getConstructor(nameType.getClass(), itemType, itemType)
                .newInstance(nameType, previous, item);
        Object event = Class.forName("o.setRequestListener", false, loader).getField("f").get(null);
        event.getClass().getMethod("b", Object.class).invoke(event, change);
    }
}
