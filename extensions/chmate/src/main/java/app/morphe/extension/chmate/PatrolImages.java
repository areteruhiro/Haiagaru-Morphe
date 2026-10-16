package app.morphe.extension.chmate;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.*;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;

/** Runs inside the native patrol Worker, never a UI callback or an independent crawler. */
public final class PatrolImages {
    private static final String PREFS = "io.github.areteruhiro.chmate.haiagaru.ui-config";
    private static volatile Context context;
    private static volatile String status = "未実行";
    private static final Object LOCK = new Object();
    private PatrolImages() { }
    public static void initialize(Context source) { context = source.getApplicationContext(); }
    private static SharedPreferences prefs() { return context.getSharedPreferences(PREFS, 0); }
    /** Replaced with validated native bindings by the bytecode patch. */
    public static String bindings() { return ""; }

    public static void onThread(Object result, Object board) {
        Context app = context;
        if (app == null || result == null || board == null || !prefs().getBoolean("patrolImagesEnabled", false)
                || Looper.myLooper() == Looper.getMainLooper()) return;
        synchronized (LOCK) {
            try {
                String[] b = bindings().split(";", -1);
                if (b.length != 10) return;
                Object list = ResponseArchiveEntry.field(result, b[0]);
                if (!(list instanceof List) || ((List<?>) list).isEmpty()) return;
                List<?> responses = (List<?>) list;
                String title = (String) ResponseArchiveEntry.field(list, "title");
                String url = ResponseArchiveEntry.snapshot(responses.get(0), board, title).url;
                url = url.replaceFirst("/[0-9]+$", "/");
                SharedPreferences settings = prefs();
                if (!PatrolImageRules.eligible(settings.getBoolean("patrolImageFilterEnabled", true),
                        title, url, settings.getString("patrolImageKeywords", ""),
                        settings.getString("patrolImageUrls", ""))) return;
                int destination = settings.getInt("patrolImageDestination", 0);
                if (destination == 1 && Build.VERSION.SDK_INT < 29 && Build.VERSION.SDK_INT >= 23
                        && app.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    status = "保存権限がありません。自動画像保存の設定を確認してください"; return;
                }
                String key = "patrolImageFolder." + PatrolImageRules.hash(url);
                String folder = settings.getString(key, null);
                if (folder == null) {
                    folder = PatrolImageRules.folder(title, url);
                    settings.edit().putString(key, folder).apply();
                }
                NativeCache cache = new NativeCache(app, b);
                LinkedHashSet<String> images = new LinkedHashSet<>();
                long deadline = SystemClock.elapsedRealtime() + 120_000;
                // Use the same native attachment getter as displayed responses (including Haiagaru projections).
                collect(responses, b[1], images);
                long metadataEnd = SystemClock.elapsedRealtime() + 15_000;
                while (enabled() && ImgurAlbumAttachments.pendingFor(responses)
                        && SystemClock.elapsedRealtime() < metadataEnd) {
                    try { Thread.sleep(200); } catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt(); return;
                    }
                }
                collect(responses, b[1], images);
                int saved = 0, skipped = 0, failed = 0;
                for (String image : images) {
                    if (!enabled() || Thread.currentThread().isInterrupted() || SystemClock.elapsedRealtime() >= deadline) break;
                    String name = PatrolImageRules.hash(ImageVpnController.imageIdentity(image));
                    if (destination == 1 && publicExists(folder, name)) { skipped++; continue; }
                    try (InputStream input = cache.await(image, deadline)) {
                        if (input == null) { failed++; continue; }
                        if (destination == 1) savePublic(folder, name, input);
                        saved++;
                    } catch (IOException | ReflectiveOperationException error) {
                        failed++;
                        // Do not log URL/token-bearing content.
                        Log.w("HaiagaruPatrolImages", "Image was not saved: " + error.getClass().getSimpleName());
                    }
                }
                status = "前回: 取得 " + saved + "／保存済み " + skipped + "／未取得 " + failed;
            } catch (ReflectiveOperationException | RuntimeException error) {
                status = "取得処理を完了できませんでした: " + error.getClass().getSimpleName();
                Log.w("HaiagaruPatrolImages", status);
            }
        }
    }
    private static boolean enabled() { return prefs().getBoolean("patrolImagesEnabled", false); }
    private static void collect(List<?> responses, String method, Set<String> images) throws ReflectiveOperationException {
        for (Object response : responses) {
            if (!enabled() || Thread.currentThread().isInterrupted()) return;
            Method getter = response.getClass().getDeclaredMethod(method); getter.setAccessible(true);
            String[] attachments = (String[]) getter.invoke(response);
            if (attachments != null) for (String image : attachments)
                if (PatrolImageRules.imageUrl(image)) images.add(image);
        }
    }

    /** All HTTP, cache indexing, NG checks and VPN routing remain native. No direct HTTP fallback. */
    private static final class NativeCache {
        final Object manager;
        final Method request, info, stream;
        final String state, code, type;
        NativeCache(Context app, String[] b) throws ReflectiveOperationException {
            Class<?> owner = Class.forName(b[2], true, app.getClassLoader());
            manager = owner.getDeclaredMethod(b[3], Context.class).invoke(null, app);
            info = owner.getDeclaredMethod(b[4], String.class);
            stream = owner.getDeclaredMethod(b[5], String.class);
            Method found = null;
            for (Method m : owner.getDeclaredMethods()) {
                Class<?>[] p = m.getParameterTypes();
                if (m.getReturnType() == boolean.class && p.length == 8 && p[0] == String.class
                        && p[1] == int.class && p[2] == int.class && p[3] == boolean.class
                        && p[4] == boolean.class && p[5] == boolean.class && p[6] == boolean.class) found = m;
            }
            if (found == null) throw new NoSuchMethodException("Native media request");
            request = found;
            request.setAccessible(true); info.setAccessible(true); stream.setAccessible(true);
            state = b[6]; code = b[7]; type = b[8];
        }
        InputStream await(String url, long deadline) throws ReflectiveOperationException, IOException {
            Object data = info.invoke(manager, url);
            if (ready(data)) return (InputStream) stream.invoke(manager, url);
            Class<?> dataClass = request.getParameterTypes()[7];
            Object requestInfo;
            try {
                Constructor<?> constructor = dataClass.getDeclaredConstructor(String.class); constructor.setAccessible(true);
                requestInfo = constructor.newInstance(url);
            } catch (NoSuchMethodException legacy191) {
                Constructor<?> constructor = dataClass.getDeclaredConstructor(); constructor.setAccessible(true);
                requestInfo = constructor.newInstance();
            }
            // Full image, no forced retry, no deletion, no bypass of NG / native policy.
            request.invoke(manager, url, 0, 0, true, false, false, false, requestInfo);
            long end = Math.min(deadline, SystemClock.elapsedRealtime() + 30_000);
            while (enabled() && !Thread.currentThread().isInterrupted() && SystemClock.elapsedRealtime() < end) {
                data = info.invoke(manager, url);
                if (ready(data)) return (InputStream) stream.invoke(manager, url);
                if (data != null && ((Number) ResponseArchiveEntry.field(data, state)).intValue() == 1
                        && ((Number) ResponseArchiveEntry.field(data, code)).intValue() >= 400) return null;
                try { Thread.sleep(200); } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt(); return null;
                }
            }
            return null;
        }
        boolean ready(Object data) throws ReflectiveOperationException {
            if (data == null) return false;
            int s = ((Number) ResponseArchiveEntry.field(data, state)).intValue();
            int c = ((Number) ResponseArchiveEntry.field(data, code)).intValue();
            int t = ((Number) ResponseArchiveEntry.field(data, type)).intValue();
            return s == 1 && c / 100 == 2 && (t == 2 || t == 3 || t == 4 || t == 7 || t == 8);
        }
    }

    private static Uri collection() { return MediaStore.Downloads.EXTERNAL_CONTENT_URI; }
    private static String path(String folder) { return Environment.DIRECTORY_DOWNLOADS + "/Haiagaru/" + folder + "/"; }
    private static boolean publicExists(String folder, String name) {
        if (Build.VERSION.SDK_INT < 29) {
            File root = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Haiagaru/" + folder);
            File[] files = root.listFiles((dir, file) -> file.startsWith(name + ".") && !file.endsWith(".part"));
            return files != null && files.length > 0 && files[0].length() > 0;
        }
        try (Cursor rows = context.getContentResolver().query(collection(), new String[]{"_id"},
                "relative_path=? AND _display_name LIKE ? AND is_pending=0 AND _size>0",
                new String[]{path(folder), name + ".%"}, null)) {
            if (rows == null) return false;
            while (rows.moveToNext()) {
                Uri uri = android.content.ContentUris.withAppendedId(collection(), rows.getLong(0));
                try (InputStream input = context.getContentResolver().openInputStream(uri)) {
                    if (input != null && input.read() != -1) return true;
                } catch (IOException missing) { }
            }
        } catch (RuntimeException denied) { }
        return false;
    }
    private static void savePublic(String folder, String name, InputStream source) throws IOException {
        BufferedInputStream input = new BufferedInputStream(source);
        input.mark(131072);
        BitmapFactory.Options bounds = new BitmapFactory.Options(); bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(input, null, bounds); input.reset();
        if (bounds.outWidth < 1 || bounds.outHeight < 1 || bounds.outMimeType == null)
            throw new IOException("Not an image");
        String mime = bounds.outMimeType;
        String ext = mime.contains("png") ? ".png" : mime.contains("gif") ? ".gif"
                : mime.contains("webp") ? ".webp" : mime.contains("bmp") ? ".bmp" : ".jpg";
        if (Build.VERSION.SDK_INT >= 29) {
            ContentValues values = new ContentValues();
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, name + ext);
            values.put(MediaStore.MediaColumns.MIME_TYPE, mime);
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, path(folder));
            values.put(MediaStore.MediaColumns.IS_PENDING, 1);
            Uri uri = context.getContentResolver().insert(collection(), values);
            if (uri == null) throw new IOException("Cannot create image");
            boolean complete = false;
            try {
                try (OutputStream output = context.getContentResolver().openOutputStream(uri)) {
                    if (output == null) throw new IOException("Cannot write image");
                    copy(input, output);
                }
                values.clear(); values.put(MediaStore.MediaColumns.IS_PENDING, 0);
                context.getContentResolver().update(uri, values, null, null); complete = true;
            } finally { if (!complete) context.getContentResolver().delete(uri, null, null); }
        } else {
            File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Haiagaru/" + folder);
            if (!dir.isDirectory() && !dir.mkdirs()) throw new IOException("Cannot create folder");
            File temporary = new File(dir, name + ".part"), target = new File(dir, name + ext);
            try {
                try (FileOutputStream output = new FileOutputStream(temporary)) { copy(input, output); output.getFD().sync(); }
                if (!temporary.renameTo(target)) throw new IOException("Cannot publish image");
            } finally { if (temporary.exists()) temporary.delete(); }
        }
    }
    private static void copy(InputStream input, OutputStream output) throws IOException {
        byte[] buffer = new byte[32768]; int count;
        while ((count = input.read(buffer)) != -1) {
            if (!enabled() || Thread.currentThread().isInterrupted()) throw new IOException("Cancelled");
            output.write(buffer, 0, count);
        }
    }

    static void addSettingsButton(LinearLayout layout, Activity activity) {
        Button button = new Button(activity); button.setText("巡回時の画像自動保存（スレタイ・URL指定）");
        button.setOnClickListener(v -> show(activity)); layout.addView(button);
    }
    private static void show(Activity activity) {
        SharedPreferences p = activity.getSharedPreferences(PREFS, 0);
        LinearLayout body = new LinearLayout(activity); body.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (16 * activity.getResources().getDisplayMetrics().density); body.setPadding(pad, pad, pad, pad);
        Switch enabled = new Switch(activity); enabled.setText("巡回時にスレの画像を自動保存");
        enabled.setChecked(p.getBoolean("patrolImagesEnabled", false)); body.addView(enabled);
        Switch filter = new Switch(activity); filter.setText("スレタイ・URLで巡回対象を絞り込む（オフなら巡回対象全体）");
        filter.setChecked(p.getBoolean("patrolImageFilterEnabled", true)); body.addView(filter);
        EditText words = editor(body, activity, "スレタイのキーワード（1行に1つ・どれかに一致）", p.getString("patrolImageKeywords", ""));
        EditText urls = editor(body, activity, "スレURL（1行に1つ・部分一致）", p.getString("patrolImageUrls", ""));
        words.setEnabled(filter.isChecked()); urls.setEnabled(filter.isChecked());
        filter.setOnCheckedChangeListener((button, checked) -> { words.setEnabled(checked); urls.setEnabled(checked); });
        Spinner destination = new Spinner(activity);
        destination.setAdapter(new ArrayAdapter<>(activity, android.R.layout.simple_spinner_dropdown_item,
                new String[]{"ChMateの画像キャッシュ", "Download/Haiagaru/スレごとのフォルダ"}));
        destination.setSelection(p.getInt("patrolImageDestination", 0) == 1 ? 1 : 0); body.addView(destination);
        TextView note = new TextView(activity);
        note.setText("ChMate標準の自動巡回と「本文(dat)もダウンロード」を有効にしてください。"
                + "巡回で本文を取得したスレが対象です。未表示でも画像を取得します。独自の巡回は行いません。"
                + "登録内容は部分一致（大文字小文字は区別しません）、空行は無視します。動画は対象外です。"
                + "絞り込みオンで条件が空欄なら取得しません。オフならChMate標準の巡回対象全体から取得します。"
                + "キャッシュはChMateの容量設定に従って消去されます。公開フォルダの保存済み画像は再取得しません。"
                + "通信量・保存容量が増えます。失敗・時間切れは次に本文を取得する巡回で再試行します。\n" + status);
        body.addView(note);
        ScrollView scroll = new ScrollView(activity); scroll.addView(body);
        new AlertDialog.Builder(activity).setTitle("巡回時の画像自動保存").setView(scroll)
                .setNegativeButton("キャンセル", null).setPositiveButton("保存", (d, which) -> {
                    p.edit().putBoolean("patrolImagesEnabled", enabled.isChecked())
                            .putBoolean("patrolImageFilterEnabled", filter.isChecked())
                            .putString("patrolImageKeywords", words.getText().toString())
                            .putString("patrolImageUrls", urls.getText().toString())
                            .putInt("patrolImageDestination", destination.getSelectedItemPosition()).apply();
                    if (enabled.isChecked() && destination.getSelectedItemPosition() == 1
                            && Build.VERSION.SDK_INT >= 23 && Build.VERSION.SDK_INT < 29
                            && activity.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
                        activity.requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0x4849);
                }).show();
    }
    private static EditText editor(LinearLayout body, Activity activity, String label, String value) {
        TextView title = new TextView(activity); title.setText(label); body.addView(title);
        EditText input = new EditText(activity); input.setMinLines(2); input.setGravity(android.view.Gravity.TOP);
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        input.setText(value); body.addView(input); return input;
    }
}
