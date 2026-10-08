package app.morphe.extension.chmate;

import android.app.AlertDialog;
import android.content.Context;
import android.widget.Toast;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Registers a quoted suffix regex through ChMate's own NGName store. */
public final class WacchoiSuffixNg {
    private WacchoiSuffixNg() {}

    public static String suffix(String query) {
        if (query == null) return null;
        Matcher match = Pattern.compile("(?i)(?<![a-z0-9])[a-f0-9]{4}-([^\\s()（）<>]{4})(?=$|[\\s()（）<>])").matcher(query);
        return match.find() ? match.group(1) : null;
    }

    public static String expression(String suffix) {
        return "(?<![A-Za-z0-9])[0-9A-Fa-f]{4}-(?-i:" + Pattern.quote(suffix)
                + ")(?=$|[\\s()（）<>])";
    }

    public static void confirm(Context context, String query) {
        String suffix = suffix(query);
        if (suffix == null) return;
        android.app.Activity activity = Haiagaru.toolbarActivity(context);
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            Toast.makeText(context, "表示中の画面からもう一度お試しください", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
        new AlertDialog.Builder(activity)
                .setTitle("ﾜｯﾁｮｲ下4桁でNGName登録")
                .setMessage("末尾が「" + suffix + "」の名前を全板でNGにします。\n"
                        + "前半が違うワッチョイも対象です。別の利用者と一致する場合があります。\n"
                        + "解除・編集はChMateのNGネーム設定から行えます。")
                .setNegativeButton("キャンセル", null)
                .setPositiveButton("登録", (dialog, which) -> register(activity, expression(suffix)))
                .show();
        } catch (android.view.WindowManager.BadTokenException error) {
            android.util.Log.w("Haiagaru", "NG confirmation activity no longer visible", error);
        }
    }

    private static void register(Context context, String regex) {
        try {
            ClassLoader loader = context.getClassLoader();
            Class<?> storeType = Class.forName("jp.syoboi.a2chMate.ng.NGWord", false, loader);
            Class<?> itemType = Class.forName("jp.syoboi.a2chMate.ng.NGWord$Item", false, loader);
            Object store = null;
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
                break;
            }
            if (store == null) throw new IllegalStateException("Native NGName provider unavailable");
            java.util.List<?> entries = null;
            for (Method method : storeType.getDeclaredMethods()) {
                if (Modifier.isPublic(method.getModifiers()) && !Modifier.isStatic(method.getModifiers())
                        && method.getParameterTypes().length == 0
                        && method.getReturnType() == java.util.ArrayList.class) {
                    entries = (java.util.List<?>) method.invoke(store);
                    break;
                }
            }
            if (entries == null) throw new IllegalStateException("Native NGName list unavailable");
            for (Object existing : entries) {
                for (java.lang.reflect.Field field : itemType.getDeclaredFields()) {
                    if (Modifier.isStatic(field.getModifiers()) || field.getType() != String.class) continue;
                    field.setAccessible(true);
                    if (regex.equals(field.get(existing))) {
                        Toast.makeText(context, "同じ条件が既に登録されています", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }
            }
            if (entries.size() >= Haiagaru.getNgRegistrationLimit()) {
                Toast.makeText(context, "NGNameの登録上限に達しています", Toast.LENGTH_LONG).show();
                return;
            }
            Object item = itemType.getConstructor(String.class, short.class, long.class, String.class)
                    .newInstance(regex, (short) 32, System.currentTimeMillis(), null);
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
            update.invoke(store, null, item);
            Toast.makeText(context, "NGNameに登録しました", Toast.LENGTH_SHORT).show();
        } catch (ReflectiveOperationException | RuntimeException error) {
            android.util.Log.w("Haiagaru", "Cannot register Wacchoi suffix NG", error);
            Toast.makeText(context, "NGNameに登録できませんでした", Toast.LENGTH_LONG).show();
        }
    }
}
