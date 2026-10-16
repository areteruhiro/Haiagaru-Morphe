package app.morphe.extension.chmate;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.text.Html;
import android.util.AtomicFile;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONObject;
import java.io.*;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

/** Separate response snapshots; native whole-thread archives and DATs are never modified. */
public final class ResponseArchive {
    private static final int ITEM = 0x7e484a01, SWITCHER = 0x7e484a02;
    private static final String EXTRA = "haiagaru.response.archive";
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final ExecutorService IO = Executors.newSingleThreadExecutor();
    private ResponseArchive() { }

    public static void appendLegacy(Object fragment, Object menu, Object response) {
        try { append(fragment, menu, response, ResponseArchiveEntry.field(fragment, "W")); }
        catch (Exception e) { report(e); }
    }

    public static void appendDialog(Object dialog, Object menu) {
        try {
            Object parent = dialog.getClass().getMethod("getParentFragment").invoke(dialog);
            Object selection = ResponseArchiveEntry.field(parent, "i");
            append(parent, menu, ResponseArchiveEntry.field(selection, "b"), ResponseArchiveEntry.field(parent, "E"));
        } catch (Exception e) { report(e); }
    }

    private static ResponseArchiveEntry snapshot(Object response, Object board) throws Exception {
        Activity activity = Haiagaru.toolbarActivity(null);
        return ResponseArchiveEntry.snapshot(response, board, activity == null ? null : String.valueOf(activity.getTitle()));
    }

    private static void append(Object owner, Object menuObject, Object response, Object board) throws Exception {
        if (!(menuObject instanceof Menu) || response == null || board == null) return;
        Menu menu = (Menu) menuObject;
        if (menu.findItem(ITEM) != null) return;
        ResponseArchiveEntry entry = snapshot(response, board);
        menu.add(Menu.NONE, ITEM, menu.size(), "このレスをアーカイブに保存")
                .setIntent(new Intent().putExtra(EXTRA, json(entry).toString()))
                .setOnMenuItemClickListener(ResponseArchive::handle);
    }

    public static boolean handle(Object object) {
        if (!(object instanceof MenuItem) || ((MenuItem) object).getItemId() != ITEM) return false;
        Activity activity = Haiagaru.toolbarActivity(null);
        try {
            MenuItem item = (MenuItem) object;
            ResponseArchiveEntry entry = decode(new JSONObject(item.getIntent().getStringExtra(EXTRA)));
            if (activity != null) confirm(activity, entry);
        } catch (Exception e) { failure(activity, e); }
        return true;
    }

    /** 242's response-body sheet passes its exact target as the third argument. */
    public static void appendSheet(Object model, Object builder, Object response) {
        try {
            Object flow = ResponseArchiveEntry.field(model, "af");
            Method get = flow.getClass().getMethod("d"); get.setAccessible(true);
            ResponseArchiveEntry entry = snapshot(response, get.invoke(flow));
            for (Method method : builder.getClass().getDeclaredMethods()) {
                Class<?>[] p = method.getParameterTypes();
                if (Modifier.isStatic(method.getModifiers()) || p.length != 6 || p[0] != String.class
                        || p[1] != String.class || p[2] != boolean.class || p[3] != boolean.class
                        || p[4] != Integer.class || !p[5].isInterface()) continue;
                Object callback = Proxy.newProxyInstance(p[5].getClassLoader(), new Class<?>[]{p[5]}, (proxy, m, args) -> {
                    if (m.getDeclaringClass() == Object.class) {
                        if ("hashCode".equals(m.getName())) return System.identityHashCode(proxy);
                        if ("equals".equals(m.getName())) return proxy == args[0];
                        return "HaiagaruResponseArchive";
                    }
                    if ("invoke".equals(m.getName()) && m.getParameterTypes().length == 0) {
                        Activity a = Haiagaru.toolbarActivity(null);
                        if (a != null) confirm(a, entry);
                    }
                    return null;
                });
                method.setAccessible(true);
                method.invoke(builder, "このレスをアーカイブに保存", null, true, false,
                        Integer.valueOf(android.R.drawable.ic_menu_save), callback);
                return;
            }
        } catch (Exception e) { report(e); }
    }

    private static void confirm(Activity a, ResponseArchiveEntry entry) {
        if (a.isFinishing()) return;
        new AlertDialog.Builder(a).setTitle("レス " + entry.number + " を保存")
                .setMessage("選んだレスの名前・日時・本文・URLを端末内に保存します。画像・動画ファイルは含みません。\n同じURLのレスは上書きします。")
                .setNegativeButton("キャンセル", null).setPositiveButton("保存", (d, w) -> IO.execute(() -> {
                    try { save(a.getApplicationContext(), entry); MAIN.post(() -> toast(a, "レスをアーカイブに保存しました")); }
                    catch (Exception e) { MAIN.post(() -> failure(a, e)); }
                })).show();
    }

    private static File directory(Context context) throws IOException {
        File dir = new File(context.getFilesDir(), "haiagaru-response-archive");
        if (!dir.isDirectory() && !dir.mkdirs()) throw new IOException("Cannot create response archive");
        return dir;
    }

    static File file(Context c, String url) throws Exception {
        byte[] hash = MessageDigest.getInstance("SHA-256").digest(url.getBytes(StandardCharsets.UTF_8));
        StringBuilder key = new StringBuilder();
        for (byte b : hash) key.append(String.format(Locale.ROOT, "%02x", b & 255));
        return new File(directory(c), key + ".json");
    }

    static JSONObject json(ResponseArchiveEntry e) throws Exception {
        return new JSONObject().put("version", 1).put("url", e.url).put("title", e.title)
                .put("number", e.number).put("name", e.name).put("date", e.date).put("body", e.body);
    }

    static ResponseArchiveEntry decode(JSONObject j) throws Exception {
        if (j.getInt("version") != 1) throw new IOException("Unknown response archive version");
        return new ResponseArchiveEntry(j.getString("url"), j.getString("title"), j.getInt("number"),
                j.getString("name"), j.getString("date"), j.getString("body"));
    }

    static void save(Context context, ResponseArchiveEntry entry) throws Exception {
        AtomicFile atomic = new AtomicFile(file(context, entry.url));
        FileOutputStream out = null;
        try {
            out = atomic.startWrite();
            out.write(json(entry).toString().getBytes(StandardCharsets.UTF_8));
            atomic.finishWrite(out);
        } catch (Exception e) { if (out != null) atomic.failWrite(out); throw e; }
    }

    static ResponseArchiveEntry read(File file) throws Exception {
        try (InputStream in = new AtomicFile(file).openRead(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192]; int count;
            while ((count = in.read(buffer)) != -1) {
                if (out.size() + count > 8000000) throw new IOException("Response archive too large");
                out.write(buffer, 0, count);
            }
            return decode(new JSONObject(new String(out.toByteArray(), StandardCharsets.UTF_8)));
        }
    }

    public static void showList(Activity a) { showList(a, 0); }
    private static void showList(Activity a, int offset) {
        if (a == null || a.isFinishing()) return;
        IO.execute(() -> {
            try {
                File[] stored = directory(a).listFiles((dir, name) -> name.matches("[0-9a-f]{64}\\.json(?:\\.bak)?"));
                if (stored == null) throw new IOException("Cannot list response archive");
                // AtomicFile may leave only .bak after an interrupted write. openRead restores it.
                LinkedHashSet<File> unique = new LinkedHashSet<>();
                for (File f : stored) unique.add(f.getName().endsWith(".bak")
                        ? new File(f.getParentFile(), f.getName().substring(0, f.getName().length() - 4)) : f);
                File[] files = unique.toArray(new File[0]);
                Arrays.sort(files, (x, y) -> Long.compare(y.lastModified(), x.lastModified()));
                ArrayList<ResponseArchiveEntry> rows = new ArrayList<>();
                int end = Math.min(files.length, offset + 100), skipped = 0;
                for (int i = offset; i < end; i++) {
                    try {
                        ResponseArchiveEntry e = read(files[i]);
                        // Keep only lightweight labels while the list is open.
                        rows.add(new ResponseArchiveEntry(e.url, e.title, e.number, e.name, e.date, ""));
                    } catch (Exception e) { skipped++; report(e); }
                }
                final int bad = skipped;
                ArrayList<String> labels = new ArrayList<>();
                for (ResponseArchiveEntry e : rows) labels.add(e.title + "\nレス " + e.number + "  " + plain(e.name));
                boolean more = end < files.length;
                if (more) labels.add("次の100件を表示");
                if (offset > 0) labels.add("先頭に戻る");
                MAIN.post(() -> {
                    if (a.isFinishing() || a.isDestroyed()) return;
                    new AlertDialog.Builder(a).setTitle("レスのアーカイブ（" + files.length + "件）")
                            .setItems(labels.toArray(new String[0]), (d, which) -> {
                                if (which < rows.size()) loadDetail(a, rows.get(which));
                                else showList(a, more && which == rows.size() ? end : 0);
                            }).setMessage(labels.isEmpty() ? "保存したレスはありません。レス本文を長押しして保存できます。" : null)
                            .setNegativeButton("閉じる", null).show();
                    if (bad > 0) toast(a, "読み込めない保存レスを" + bad + "件スキップしました");
                });
            } catch (Exception e) { MAIN.post(() -> failure(a, e)); }
        });
    }

    @SuppressWarnings("deprecation")
    private static String plain(String html) { return Html.fromHtml(html).toString(); }

    private static void loadDetail(Activity a, ResponseArchiveEntry row) {
        IO.execute(() -> {
            try {
                ResponseArchiveEntry entry = read(file(a, row.url));
                MAIN.post(() -> { if (!a.isFinishing() && !a.isDestroyed()) detail(a, entry); });
            } catch (Exception e) { MAIN.post(() -> failure(a, e)); }
        });
    }

    private static void detail(Activity a, ResponseArchiveEntry e) {
        ScrollView scroll = new ScrollView(a);
        TextView text = new TextView(a);
        int pad = (int) (16 * a.getResources().getDisplayMetrics().density);
        text.setPadding(pad, pad, pad, pad); text.setTextIsSelectable(true);
        // No WebView, image getter or automatic URL fetch: offline viewing only.
        text.setText(e.number + " " + plain(e.name) + "\n" + plain(e.date) + "\n\n" + plain(e.body) + "\n\n" + e.url);
        scroll.addView(text);
        new AlertDialog.Builder(a).setTitle(e.title).setView(scroll)
                .setNegativeButton("一覧へ", (d, w) -> showList(a))
                .setPositiveButton("URLをコピー", (d, w) -> {
                    ClipboardManager c = (ClipboardManager) a.getSystemService(Context.CLIPBOARD_SERVICE);
                    if (c != null) c.setPrimaryClip(ClipData.newPlainText("レスURL", e.url));
                    showList(a);
                }).setNeutralButton("削除", (d, w) -> new AlertDialog.Builder(a)
                        .setMessage("この保存レスだけを削除しますか？ 元のスレやスレ単位のアーカイブは削除しません。")
                        .setNegativeButton("キャンセル", (x, y) -> detail(a, e))
                        .setPositiveButton("削除", (x, y) -> IO.execute(() -> {
                            try { new AtomicFile(file(a, e.url)).delete(); MAIN.post(() -> showList(a)); }
                            catch (Exception error) { MAIN.post(() -> failure(a, error)); }
                        })).show()).show();
    }

    /** A separate entry above the native archive list, without replacing its thread content. */
    public static void onActivityResumed(Activity a) {
        if (!a.getClass().getName().equals("jp.syoboi.a2chMate.activity.ArchiveListActivity")) return;
        a.getWindow().getDecorView().post(() -> {
            ViewGroup content = a.findViewById(android.R.id.content);
            if (content == null || content.findViewById(SWITCHER) != null || content.getChildCount() != 1) return;
            View nativeList = content.getChildAt(0);
            ViewGroup.LayoutParams original = nativeList.getLayoutParams();
            LinearLayout container = new LinearLayout(a); container.setOrientation(LinearLayout.VERTICAL);
            Button button = new Button(a); button.setId(SWITCHER); button.setText("レスのアーカイブを開く");
            button.setOnClickListener(v -> showList(a));
            content.removeView(nativeList);
            container.addView(button, new LinearLayout.LayoutParams(-1, -2));
            container.addView(nativeList, new LinearLayout.LayoutParams(-1, 0, 1));
            content.addView(container, original);
        });
    }

    private static void toast(Activity a, String text) { if (a != null && !a.isDestroyed()) Toast.makeText(a, text, Toast.LENGTH_LONG).show(); }
    private static void failure(Activity a, Exception e) { report(e); toast(a, "レスのアーカイブを処理できませんでした"); }
    private static void report(Exception e) { android.util.Log.w("Haiagaru", "Response archive operation failed", e); }
}
