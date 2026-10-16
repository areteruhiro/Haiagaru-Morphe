package app.morphe.extension.chmate;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** Version-pinned native binding adapters: never infer a posting target from draft text. */
public final class ThreadCommands {
    private static final String TAG = "haiagaru.thread.commands";
    private ThreadCommands() {}
    private static Object field(Object object, String name) throws Exception {
        for (Class<?> type = object.getClass(); type != null; type = type.getSuperclass()) {
            try { Field f = type.getDeclaredField(name); f.setAccessible(true); return f.get(object); }
            catch (NoSuchFieldException ignored) { }
        }
        throw new NoSuchFieldException(name);
    }
    private static Object unwrap(Object value) throws Exception {
        for (Class<?> iface : value.getClass().getInterfaces()) for (Method m : iface.getMethods()) {
            if (m.getParameterTypes().length == 0 && (m.getName().equals("getValue") || m.getName().equals("e"))) {
                m.setAccessible(true); return m.invoke(value);
            }
        }
        throw new IllegalStateException("Unknown posting model wrapper");
    }
    public static void attach(Object fragment, View root) {
        root.post(() -> attachReady(fragment, root));
    }
    private static void attachReady(Object fragment, View root) {
        try {
            if (root.findViewWithTag(TAG) != null) return;
            Object binding, model;
            String bodyName, targetName, modeName, urlMethod;
            if (fragment.getClass().getName().equals("o.p9ExternalSyntheticLambda6")) {
                binding = field(fragment, "d"); model = field(fragment, "y");
                bodyName = "f"; targetName = "w"; modeName = "o"; urlMethod = "i";
            } else {
                boolean modern242;
                try {
                    Class.forName("jp.syoboi.a2chMate.feature.resedit.ResEditFragmentViewModel", false, fragment.getClass().getClassLoader());
                    modern242 = true;
                } catch (ClassNotFoundException ignored) { modern242 = false; }
                if (modern242) {
                    binding = field(fragment, "i"); model = unwrap(field(fragment, "y"));
                    bodyName = "b"; targetName = "T"; modeName = "x"; urlMethod = "A";
                } else {
                    binding = field(fragment, "h"); model = unwrap(field(fragment, "u"));
                    bodyName = "a"; targetName = "P"; modeName = "r"; urlMethod = "G";
                }
            }
            Object target = field(model, targetName);
            String url = (String) target.getClass().getMethod(urlMethod).invoke(target);
            ExternalBoardAuth.target(fragment, url);
            Object mode = field(model, modeName);
            if (!(mode instanceof Enum) || !((Enum<?>) mode).name().equals("Thread")) return;
            EditText body = (EditText) field(binding, bodyName);
            String profile = ThreadCommandRules.profile(url);
            // Insert alongside the body container, preserving its native listeners and layout params.
            View anchor = body;
            while (anchor.getParent() instanceof ViewGroup) {
                ViewGroup parent = (ViewGroup) anchor.getParent();
                if (parent instanceof LinearLayout && ((LinearLayout) parent).getOrientation() == LinearLayout.VERTICAL) {
                    Button button = new Button(root.getContext());
                    button.setTag(TAG); button.setText("コマンド（" + (profile.equals("edge") ? "エッヂ" : profile.equals("5ch") ? "5ch" : "カスタム") + "）");
                    button.setOnClickListener(v -> choose(body, profile));
                    parent.addView(button, parent.indexOfChild(anchor), new LinearLayout.LayoutParams(-1, -2));
                    return;
                }
                anchor = parent;
            }
        } catch (Exception e) {
            android.util.Log.w("Haiagaru", "Thread command UI unavailable", e);
        }
    }
    private static void choose(EditText body, String profile) {
        Context c = body.getContext();
        new AlertDialog.Builder(c).setTitle("スレ立てコマンド")
                .setItems(ThreadCommandRules.labels(profile), (d, index) -> edit(body, profile, index)).setNegativeButton("閉じる", null).show();
    }
    private static void edit(EditText body, String profile, int index) {
        Context c = body.getContext();
        SharedPreferences prefs = c.getSharedPreferences("haiagaru_thread_commands", 0);
        String key = profile + "." + index;
        String initial = ThreadCommandRules.values(profile)[index];
        EditText input = new EditText(c); input.setSingleLine(true);
        input.setText(prefs.getString(key, initial));
        AlertDialog dialog = new AlertDialog.Builder(c).setTitle("コマンドを編集して追加")
                .setMessage("本文の先頭に追加します。候補ごとの編集内容を保存します。掲示板の設定によっては無効です。IP表示コマンドはIPが公開されます。")
                .setView(input).setNegativeButton("キャンセル", null)
                .setNeutralButton("初期値", null).setPositiveButton("本文に追加", null).create();
        dialog.setOnShowListener(d -> {
            dialog.getButton(-3).setOnClickListener(v -> input.setText(initial));
            dialog.getButton(-1).setOnClickListener(v -> {
                String command = input.getText().toString().trim();
                if (command.isEmpty() || command.length() > 1024 || command.contains("\n") || command.contains("\r")) {
                    input.setError("1行のコマンドを入力してください（1024文字まで）"); return;
                }
                String draft = body.getText().toString();
                if (ThreadCommandRules.conflicts(draft, command)) {
                    input.setError("同系統のコマンドが本文にあります。本文で編集・削除してください"); return;
                }
                prefs.edit().putString(key, command).apply();
                if (ThreadCommandRules.contains(draft, command)) Toast.makeText(c, "追加済みです", Toast.LENGTH_SHORT).show();
                else { body.getText().insert(0, command + "\n"); body.setSelection(command.length() + 1); }
                dialog.dismiss();
            });
        });
        dialog.show();
    }
}
