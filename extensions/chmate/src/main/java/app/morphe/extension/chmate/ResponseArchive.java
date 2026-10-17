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
import android.text.InputFilter;
import android.util.AtomicFile;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
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
    private static final int ITEM = 0x7e484a01, SWITCHER = 0x7e484a02, VIEW_ARCHIVE = 0x7e484a03;
    private static final ThreadLocal<Object> ACTIVE_DIALOG = new ThreadLocal<>();
    private static final String EXTRA = "haiagaru.response.archive";
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final ExecutorService IO = Executors.newSingleThreadExecutor();
    private ResponseArchive() { }

    public static void appendLegacy(Object fragment, Object menu, Object response) {
        appendViewMenu(menu);
        try { append(fragment, menu, response, ResponseArchiveEntry.field(fragment, "W")); }
        catch (Exception e) { report(e); }
    }

    public static void appendDialog(Object dialog, Object menu) {
        appendViewMenu(menu);
        try {
            Object parent = dialog.getClass().getMethod("getParentFragment").invoke(dialog);
            if (isLegacyParent(parent)) {
                Object selection = ResponseArchiveEntry.field(parent, "v");
                append(parent, menu, ResponseArchiveEntry.field(selection, "c"), ResponseArchiveEntry.field(parent, "W"));
                return;
            }
            Object selection = ResponseArchiveEntry.field(parent, "i");
            append(parent, menu, ResponseArchiveEntry.field(selection, "b"), ResponseArchiveEntry.field(parent, "E"));
        } catch (Exception e) { report(e); }
    }

    private static boolean isLegacyParent(Object parent) {
        for (Class<?> type = parent == null ? null : parent.getClass(); type != null; type = type.getSuperclass())
            if ("o.pa".equals(type.getName())) return true;
        return false;
    }

    /** 191's ordinary response menu is separate from its link/name popups. */
    public static void captureDialog(Object dialog) { ACTIVE_DIALOG.set(dialog); }

    public static void appendCapturedDialog(Object menu) {
        Object dialog = ACTIVE_DIALOG.get();
        ACTIVE_DIALOG.remove();
        if (dialog != null) appendDialog(dialog, menu);
    }

    private static void appendViewMenu(Object object) {
        if (!(object instanceof Menu)) return;
        Menu menu = (Menu) object;
        if (menu.findItem(VIEW_ARCHIVE) == null)
            menu.add(Menu.NONE, VIEW_ARCHIVE, menu.size(), "過去レスを見る")
                    .setOnMenuItemClickListener(ResponseArchive::handle);
    }

    private static ResponseArchiveEntry snapshot(Object response, Object board) throws Exception {
        Activity activity = Haiagaru.toolbarActivity(null);
        return ResponseArchiveEntry.snapshot(response, board, activity == null ? null : String.valueOf(activity.getTitle()));
    }

    private static void append(Object owner, Object menuObject, Object response, Object board) throws Exception {
        appendViewMenu(menuObject);
        if (!(menuObject instanceof Menu) || response == null || board == null) return;
        Menu menu = (Menu) menuObject;
        if (menu.findItem(ITEM) != null) return;
        ResponseArchiveEntry entry = snapshot(response, board);
        menu.add(Menu.NONE, ITEM, menu.size(), "このレスをアーカイブに保存")
                .setIntent(new Intent().putExtra(EXTRA, json(entry).toString()))
                .setOnMenuItemClickListener(ResponseArchive::handle);
    }

    public static boolean handle(Object object) {
        if (!(object instanceof MenuItem)) return false;
        int id = ((MenuItem) object).getItemId();
        if (id != ITEM && id != VIEW_ARCHIVE) return false;
        Activity activity = Haiagaru.toolbarActivity(null);
        if (id == VIEW_ARCHIVE) {
            showList(activity);
            return true;
        }
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
            appendSheetAction(builder, "過去レスを見る", android.R.drawable.ic_menu_recent_history,
                    () -> showList(Haiagaru.toolbarActivity(null)));
        } catch (Exception e) { report(e); }
        try {
            Object flow = ResponseArchiveEntry.field(model, "af");
            Method get = flow.getClass().getMethod("d"); get.setAccessible(true);
            ResponseArchiveEntry entry = snapshot(response, get.invoke(flow));
            appendSheetAction(builder, "このレスをアーカイブに保存", android.R.drawable.ic_menu_save, () -> {
                Activity a = Haiagaru.toolbarActivity(null);
                if (a != null) confirm(a, entry);
            });
        } catch (Exception e) { report(e); }
    }

    private static void appendSheetAction(Object builder, String title, int icon, Runnable action) throws Exception {
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
                        action.run();
                    }
                    return null;
                });
                method.setAccessible(true);
                method.invoke(builder, title, null, true, false, Integer.valueOf(icon), callback);
                return;
            }
            throw new NoSuchMethodException("Response menu builder action");
    }

    private static void confirm(Activity a, ResponseArchiveEntry entry) {
        if (a.isFinishing()) return;
        chooseCollection(a, entry);
    }

    private static void confirmSave(Activity a, ResponseArchiveEntry entry, Collection collection) {
        if (a.isFinishing()) return;
        new AlertDialog.Builder(a).setTitle("「" + collection.title + "」に保存")
                .setMessage("選んだレスの名前・日時・本文・URLを端末内に保存します。画像・動画ファイルは含みません。\n同じURLのレスは上書きします。")
                .setNegativeButton("キャンセル", null).setPositiveButton("保存", (d, w) -> IO.execute(() -> {
                    try {
                        saveToCollection(a, collection.id, entry);
                        MAIN.post(() -> toast(a, "「" + collection.title + "」に保存しました"));
                    }
                    catch (Exception e) { MAIN.post(() -> failure(a, e)); }
                })).show();
    }

    private static File directory(Context context) throws IOException {
        File dir = new File(context.getFilesDir(), "haiagaru-response-archive");
        if (!dir.isDirectory() && !dir.mkdirs()) throw new IOException("Cannot create response archive");
        return dir;
    }

    static File file(Context c, String url) throws Exception {
        return fileIn(directory(c), url);
    }

    private static File fileIn(File dir, String url) throws Exception {
        byte[] hash = MessageDigest.getInstance("SHA-256").digest(url.getBytes(StandardCharsets.UTF_8));
        StringBuilder key = new StringBuilder();
        for (byte b : hash) key.append(String.format(Locale.ROOT, "%02x", b & 255));
        return new File(dir, key + ".json");
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
        writeJson(file(context, entry.url), json(entry));
    }

    private static void writeJson(File file, JSONObject value) throws Exception {
        AtomicFile atomic = new AtomicFile(file);
        FileOutputStream out = null;
        try {
            out = atomic.startWrite();
            out.write(value.toString().getBytes(StandardCharsets.UTF_8));
            atomic.finishWrite(out);
        } catch (Exception e) { if (out != null) atomic.failWrite(out); throw e; }
    }

    static ResponseArchiveEntry read(File file) throws Exception {
        return decode(readJson(file, 8000000));
    }

    private static JSONObject readJson(File file, int limit) throws Exception {
        try (InputStream in = new AtomicFile(file).openRead(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192]; int count;
            while ((count = in.read(buffer)) != -1) {
                if (out.size() + count > limit) throw new IOException("Response archive too large");
                out.write(buffer, 0, count);
            }
            return new JSONObject(new String(out.toByteArray(), StandardCharsets.UTF_8));
        }
    }

    static final class Collection {
        final String id, title;
        final long created;
        Collection(String id, String title, long created) { this.id = id; this.title = title; this.created = created; }
    }

    private static File collectionRoot(Context c) throws IOException {
        File root = new File(directory(c), "threads");
        if (!root.isDirectory() && !root.mkdirs()) throw new IOException("Cannot create archive threads");
        return root;
    }

    private static File collectionDirectory(Context c, String id) throws IOException {
        if (id == null || !id.matches("[0-9a-f]{8}(?:-[0-9a-f]{4}){3}-[0-9a-f]{12}"))
            throw new IOException("Invalid archive thread ID");
        File root = collectionRoot(c).getCanonicalFile(), dir = new File(root, id).getCanonicalFile();
        if (!root.equals(dir.getParentFile())) throw new IOException("Archive thread outside root");
        return dir;
    }

    static Collection createCollection(Context c, String title) throws Exception {
        title = collectionTitle(title);
        Collection collection = new Collection(UUID.randomUUID().toString(), title, System.currentTimeMillis());
        File dir = collectionDirectory(c, collection.id);
        if (!dir.mkdir()) throw new IOException("Cannot create archive thread");
        writeCollection(dir, collection);
        return collection;
    }

    private static String collectionTitle(String title) {
        if (title == null || title.trim().isEmpty() || title.trim().length() > 200)
            throw new IllegalArgumentException("保存用スレの名前を1～200文字で入力してください");
        return title.trim();
    }

    private static void writeCollection(File dir, Collection collection) throws Exception {
        writeJson(new File(dir, "collection.json"), new JSONObject().put("version", 1)
                .put("title", collection.title).put("created", collection.created));
    }

    static Collection readCollection(Context c, String id) throws Exception {
        JSONObject value = readJson(new File(collectionDirectory(c, id), "collection.json"), 16384);
        if (value.getInt("version") != 1) throw new IOException("Unknown archive thread version");
        return new Collection(id, collectionTitle(value.getString("title")), value.getLong("created"));
    }

    static List<Collection> collections(Context c) throws Exception {
        ArrayList<Collection> result = new ArrayList<>();
        File[] dirs = collectionRoot(c).listFiles();
        if (dirs == null) throw new IOException("Cannot list archive threads");
        for (File dir : dirs) if (dir.isDirectory()) {
            try { result.add(readCollection(c, dir.getName())); }
            catch (Exception e) { report(e); }
        }
        Collections.sort(result, (x, y) -> Long.compare(y.created, x.created));
        return result;
    }

    static void saveToCollection(Context c, String id, ResponseArchiveEntry entry) throws Exception {
        readCollection(c, id); // Do not silently recreate a missing/restored-invalid destination.
        writeJson(fileIn(collectionDirectory(c, id), entry.url), json(entry));
    }

    private static File[] archiveFiles(File dir) throws IOException {
        File[] files = dir.listFiles((ignored, name) -> name.matches("[0-9a-f]{64}\\.json(?:\\.bak)?"));
        if (files == null) throw new IOException("Cannot list response archive");
        LinkedHashSet<File> unique = new LinkedHashSet<>();
        for (File f : files) unique.add(f.getName().endsWith(".bak")
                ? new File(f.getParentFile(), f.getName().substring(0, f.getName().length() - 4)) : f);
        File[] result = unique.toArray(new File[0]);
        Arrays.sort(result, (x, y) -> Long.compare(y.lastModified(), x.lastModified()));
        return result;
    }

    public static void showList(Activity a) { chooseCollection(a, null); }

    private static void chooseCollection(Activity a, ResponseArchiveEntry entry) { chooseCollection(a, entry, 0); }
    private static void chooseCollection(Activity a, ResponseArchiveEntry entry, int offset) {
        if (a == null || a.isFinishing()) return;
        IO.execute(() -> {
            try {
                List<Collection> stored = collections(a);
                int end = Math.min(stored.size(), offset + 100);
                List<Collection> page = new ArrayList<>(stored.subList(offset, end));
                ArrayList<String> labels = new ArrayList<>();
                for (Collection collection : page) labels.add(collection.title);
                int create = labels.size(); labels.add("＋ 新しい保存用スレを作成");
                int legacy = labels.size(); if (entry == null) labels.add("従来の保存レスを見る");
                int next = labels.size(); if (end < stored.size()) labels.add("次の100件");
                MAIN.post(() -> {
                    if (a.isFinishing() || a.isDestroyed()) return;
                    new AlertDialog.Builder(a).setTitle(entry == null ? "保存用スレ" : "保存先のスレを選択")
                            .setItems(labels.toArray(new String[0]), (d, which) -> {
                                if (which < page.size()) {
                                    if (entry == null) showCollection(a, page.get(which), 0);
                                    else confirmSave(a, entry, page.get(which));
                                } else if (which == create) editCollection(a, null, entry);
                                else if (entry == null && which == legacy) showLegacyList(a, 0);
                                else if (which == next) chooseCollection(a, entry, end);
                            }).setNegativeButton("閉じる", null).show();
                });
            } catch (Exception e) { MAIN.post(() -> failure(a, e)); }
        });
    }

    private static void editCollection(Activity a, Collection existing, ResponseArchiveEntry entry) {
        EditText input = new EditText(a); input.setSingleLine(true);
        input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(200)});
        input.setHint("保存用スレの名前");
        String initial = existing != null ? existing.title : entry == null ? "" : entry.title;
        input.setText(initial.substring(0, Math.min(initial.length(), 200)));
        AlertDialog dialog = new AlertDialog.Builder(a).setTitle(existing == null ? "保存用スレを作成" : "名前を変更")
                .setView(input).setNegativeButton("キャンセル", null).setPositiveButton("保存", null).create();
        dialog.setOnShowListener(ignored -> dialog.getButton(-1).setOnClickListener(v -> {
            final String title;
            try { title = collectionTitle(input.getText().toString()); }
            catch (IllegalArgumentException e) { input.setError(e.getMessage()); return; }
            dialog.getButton(-1).setEnabled(false);
            IO.execute(() -> {
                try {
                    Collection saved;
                    if (existing == null) saved = createCollection(a, title);
                    else {
                        Collection current = readCollection(a, existing.id);
                        saved = new Collection(current.id, title, current.created);
                        writeCollection(collectionDirectory(a, saved.id), saved);
                    }
                    MAIN.post(() -> {
                        dialog.dismiss();
                        if (a.isFinishing() || a.isDestroyed()) return;
                        if (entry == null) showCollection(a, saved, 0); else confirmSave(a, entry, saved);
                    });
                } catch (Exception e) { MAIN.post(() -> { dialog.getButton(-1).setEnabled(true); failure(a, e); }); }
            });
        }));
        dialog.show();
    }

    private static void showCollection(Activity a, Collection collection, int offset) {
        if (a == null || a.isFinishing()) return;
        IO.execute(() -> {
            try {
                Collection current = readCollection(a, collection.id);
                File[] files = archiveFiles(collectionDirectory(a, current.id));
                int end = Math.min(files.length, offset + 10);
                ArrayList<ResponseArchiveEntry> rows = new ArrayList<>();
                for (int i = offset; i < end; i++) {
                    try { rows.add(read(files[i])); } catch (Exception e) { report(e); }
                }
                ArrayList<String> previews = new ArrayList<>();
                for (ResponseArchiveEntry row : rows) {
                    String body = plain(row.body);
                    previews.add(row.number + " " + plain(row.name) + "\n" + plain(row.date) + "\n"
                            + row.title + "\n\n" + body.substring(0, Math.min(body.length(), 20000))
                            + (body.length() > 20000 ? "\n［タップで全文］" : "") + "\n\n" + row.url);
                }
                MAIN.post(() -> {
                    if (a.isFinishing() || a.isDestroyed()) return;
                    ScrollView scroll = new ScrollView(a);
                    LinearLayout posts = new LinearLayout(a); posts.setOrientation(LinearLayout.VERTICAL);
                    scroll.addView(posts);
                    int pad = (int) (12 * a.getResources().getDisplayMetrics().density);
                    for (ResponseArchiveEntry row : rows) {
                        TextView text = new TextView(a); text.setPadding(pad, pad, pad, pad);
                        text.setText(previews.get(rows.indexOf(row)));
                        posts.addView(text);
                        View line = new View(a); line.setBackgroundColor(0x55777777);
                        posts.addView(line, new LinearLayout.LayoutParams(-1, Math.max(1, pad / 12)));
                        text.setOnClickListener(v -> detail(a, row, current, offset));
                    }
                    if (rows.isEmpty()) {
                        TextView empty = new TextView(a); empty.setPadding(pad, pad, pad, pad);
                        empty.setText("保存したレスはありません。レス長押しからこのスレを選んで保存してください。");
                        posts.addView(empty);
                    }
                    AlertDialog viewer = new AlertDialog.Builder(a).setTitle(current.title + "（" + files.length + "レス）")
                            .setView(scroll).setNegativeButton("スレ一覧", (d, w) -> showList(a))
                            .setNeutralButton("名前を変更", (d, w) -> editCollection(a, current, null))
                            .setPositiveButton(end < files.length ? "次の10件" : offset > 0 ? "先頭へ" : "閉じる",
                                    (d, w) -> { if (end < files.length || offset > 0) showCollection(a, current, end < files.length ? end : 0); })
                            .create();
                    viewer.setOnShowListener(ignored -> {
                        // A post opens its detail viewer without leaving stacked collection dialogs.
                        for (int i = 0; i < posts.getChildCount(); i++) {
                            View child = posts.getChildAt(i);
                            if (child instanceof TextView && child.hasOnClickListeners()) {
                                final int index = i / 2;
                                child.setOnClickListener(v -> { viewer.dismiss(); detail(a, rows.get(index), current, offset); });
                            }
                        }
                    });
                    viewer.show();
                });
            } catch (Exception e) { MAIN.post(() -> failure(a, e)); }
        });
    }

    private static void showLegacyList(Activity a, int offset) {
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
                                else showLegacyList(a, more && which == rows.size() ? end : 0);
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
                MAIN.post(() -> { if (!a.isFinishing() && !a.isDestroyed()) detail(a, entry, null, 0); });
            } catch (Exception e) { MAIN.post(() -> failure(a, e)); }
        });
    }

    private static void detail(Activity a, ResponseArchiveEntry e, Collection collection, int offset) {
        ScrollView scroll = new ScrollView(a);
        TextView text = new TextView(a);
        int pad = (int) (16 * a.getResources().getDisplayMetrics().density);
        text.setPadding(pad, pad, pad, pad); text.setTextIsSelectable(true);
        // No WebView, image getter or automatic URL fetch: offline viewing only.
        text.setText(e.number + " " + plain(e.name) + "\n" + plain(e.date) + "\n\n" + plain(e.body) + "\n\n" + e.url);
        scroll.addView(text);
        new AlertDialog.Builder(a).setTitle(e.title).setView(scroll)
                .setNegativeButton("一覧へ", (d, w) -> returnToList(a, collection, offset))
                .setPositiveButton("URLをコピー", (d, w) -> {
                    ClipboardManager c = (ClipboardManager) a.getSystemService(Context.CLIPBOARD_SERVICE);
                    if (c != null) c.setPrimaryClip(ClipData.newPlainText("レスURL", e.url));
                    returnToList(a, collection, offset);
                }).setNeutralButton("削除", (d, w) -> new AlertDialog.Builder(a)
                        .setMessage("この保存レスだけを削除しますか？ 元のスレやスレ単位のアーカイブは削除しません。")
                        .setNegativeButton("キャンセル", (x, y) -> detail(a, e, collection, offset))
                        .setPositiveButton("削除", (x, y) -> IO.execute(() -> {
                            try {
                                File saved = collection == null ? file(a, e.url) : fileIn(collectionDirectory(a, collection.id), e.url);
                                new AtomicFile(saved).delete();
                                MAIN.post(() -> returnToList(a, collection, 0));
                            }
                            catch (Exception error) { MAIN.post(() -> failure(a, error)); }
                        })).show()).show();
    }

    private static void returnToList(Activity a, Collection collection, int offset) {
        if (collection == null) showLegacyList(a, offset); else showCollection(a, collection, offset);
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
