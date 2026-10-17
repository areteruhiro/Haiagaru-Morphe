package app.morphe.extension.chmate;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;
import org.json.JSONObject;
import java.text.DateFormat;
import java.util.*;
import java.io.IOException;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import android.app.ProgressDialog;
import android.os.Handler;
import android.os.Looper;
import java.util.concurrent.atomic.AtomicBoolean;
import java.lang.ref.WeakReference;

/** Capability URLs live in private preferences, never in Imgur's deletion-key column. */
public final class MediaUploadManagement {
    static final String PREFS = "haiagaru.upload-management-secret";
    private static final AtomicBoolean BUSY = new AtomicBoolean();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private MediaUploadManagement() {}
    static void remember(Context context, JSONObject result) throws Exception {
        JSONObject record = MediaUploadManagementPolicy.record(result, System.currentTimeMillis());
        if (record == null) return;
        // imgpile can deduplicate bytes while issuing distinct owner slugs.
        // Do not collapse those different deletion targets by shared CDN URL.
        String key = key(record.getString("provider") + "\n" + record.getString("management"));
        if (!context.getSharedPreferences(PREFS, 0).edit().putString(key, record.toString()).commit())
            throw new IOException("送信済みですが削除・管理情報を保存できません。再送しないでください");
    }
    private static String key(String link) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(link.getBytes(StandardCharsets.UTF_8));
        StringBuilder key = new StringBuilder();
        for (byte b : digest) key.append(String.format(Locale.ROOT, "%02x", b & 255));
        return key.toString();
    }
    static List<JSONObject> entries(Context context) {
        List<JSONObject> rows = new ArrayList<>();
        for (Object raw : context.getSharedPreferences(PREFS, 0).getAll().values()) {
            if (!(raw instanceof String)) continue;
            try {
                JSONObject row = new JSONObject((String) raw);
                if (!row.optBoolean("deleted") && MediaUploadManagementPolicy.valid(row.optString("provider"), row.optString("link"), row.optString("management"))) rows.add(row);
            } catch (Exception ignored) { }
        }
        rows.sort((a, b) -> Long.compare(b.optLong("time"), a.optLong("time")));
        return rows;
    }
    public static void show(Activity activity) {
        show(activity, null);
    }
    public static void show(Activity activity, Object owner) {
        List<JSONObject> rows = entries(activity);
        if (rows.isEmpty()) {
            new AlertDialog.Builder(activity).setTitle("imgbox／imgpileのサーバー削除・管理")
                    .setMessage("管理情報を保存したアップロードがありません。\nこの機能の追加後に送信に成功した画像・動画が対象です。過去分の削除キーは復元できません。")
                    .setNeutralButton("imgpile Web送信・履歴", (d, w) -> MediaUploadWebActivity.open(activity))
                    .setPositiveButton("OK", null).show();
            return;
        }
        String[] labels = new String[rows.size()];
        DateFormat date = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
        for (int i = 0; i < rows.size(); i++) {
            JSONObject row = rows.get(i);
            labels[i] = row.optString("provider") + " / " + date.format(new Date(row.optLong("time"))) + "\n" + row.optString("link");
        }
        boolean[] selected = new boolean[rows.size()];
        AlertDialog dialog = new AlertDialog.Builder(activity).setTitle("サーバーから一括削除（imgbox／imgpile）")
                .setMultiChoiceItems(labels, selected, (d, i, checked) -> selected[i] = checked)
                .setNeutralButton("全選択／解除", null).setPositiveButton("削除の確認", null)
                .setNegativeButton("閉じる", null).create();
        dialog.show();
        dialog.getListView().setOnItemLongClickListener((list, view, index, id) -> { confirm(activity, rows.get(index)); return true; });
        dialog.getButton(-3).setOnClickListener(v -> {
            boolean all = true; for (boolean checked : selected) all &= checked;
            for (int i = 0; i < selected.length; i++) { selected[i] = !all; dialog.getListView().setItemChecked(i, !all); }
        });
        dialog.getButton(-1).setOnClickListener(v -> {
            List<JSONObject> chosen = new ArrayList<>();
            for (int i = 0; i < selected.length; i++) if (selected[i]) chosen.add(rows.get(i));
            if (chosen.isEmpty()) { Toast.makeText(activity, "対象を選択してください", Toast.LENGTH_SHORT).show(); return; }
            new AlertDialog.Builder(activity).setTitle(chosen.size() + "件をサーバーから削除しますか？")
                    .setMessage("画像・動画へのリンクが利用できなくなります。元には戻せない操作として扱ってください。成功した項目のみ削除済みにし、失敗・未処理分は残します。\nimgpileの削除には送信者のWebセッションが必要です。長押しで管理ページも開けます。")
                    .setNegativeButton("キャンセル", null).setPositiveButton("削除", (d, w) -> {
                        dialog.dismiss(); start(activity, owner, chosen);
                    }).show();
        });
    }
    private static void start(Activity activity, Object owner, List<JSONObject> chosen) {
        if (!BUSY.compareAndSet(false, true)) { Toast.makeText(activity, "削除を実行中です", Toast.LENGTH_SHORT).show(); return; }
        AtomicBoolean cancel = new AtomicBoolean();
        WeakReference<Object> nativeOwner = new WeakReference<>(owner);
        Context context = activity.getApplicationContext();
        ProgressDialog progress = new ProgressDialog(activity);
        progress.setTitle("サーバーから削除中"); progress.setMessage("0 / " + chosen.size());
        progress.setCanceledOnTouchOutside(false); progress.setOnCancelListener(d -> cancel.set(true));
        progress.setButton(-2, "中止", (d, w) -> cancel.set(true)); progress.show();
        new Thread(() -> {
            int ok = 0, failed = 0; String problem = "";
            try {
                MediaUploadDeletionClient client = new MediaUploadDeletionClient(new MediaUploadWebClient(new MediaUploadSessions()));
                for (JSONObject row : chosen) {
                    if (cancel.get() || activity.isFinishing() || activity.isDestroyed()) break;
                    try {
                        if (client.delete(row)) {
                            row.put("deleted", true);
                            String id = key(row.getString("provider") + "\n" + row.getString("management"));
                            boolean saved = context.getSharedPreferences(PREFS, 0).edit().putString(id, row.toString()).commit();
                            ok++;
                            if (!saved) problem = "サーバー削除は成功しましたが削除済み状態を保存できませんでした。サイト上で確認してください";
                            MAIN.post(() -> NativeUploadHistory.markExternalDeleted(nativeOwner.get(), row.optString("link")));
                        } else { failed++; problem = "削除成功を確認できない応答でした。サイト上で確認してください"; }
                    } catch (Exception error) {
                        failed++;
                        // Never expose a transport exception containing an owner URL or cookie.
                        problem = "削除権限・サイトの応答を確認できません。Web画面のログイン状態と管理ページを確認してください";
                    }
                    final int done = ok + failed;
                    MAIN.post(() -> { if (!activity.isDestroyed() && progress.isShowing()) progress.setMessage(done + " / " + chosen.size()); });
                }
            } finally {
                final int success = ok, errors = failed; final String detail = problem;
                BUSY.set(false);
                MAIN.post(() -> {
                    if (activity.isDestroyed()) return;
                    if (progress.isShowing()) progress.dismiss();
                    if (!activity.isFinishing()) new AlertDialog.Builder(activity).setTitle("一括削除の結果")
                            .setMessage("削除済み: " + success + "件\n失敗: " + errors + "件\n未処理: "
                                    + (chosen.size() - success - errors) + "件\n" + detail).setPositiveButton("OK", null).show();
                });
            }
        }, "Haiagaru-provider-delete").start();
    }
    private static void confirm(Activity activity, JSONObject row) {
        String provider = row.optString("provider"), link = row.optString("link"), management = row.optString("management");
        if (!MediaUploadManagementPolicy.valid(provider, link, management)) return;
        new AlertDialog.Builder(activity).setTitle(provider + "の削除・管理ページを開きますか？")
                .setMessage(link + "\n\n開くだけでは削除しません。ブラウザで対象を確認して削除してください。imgpileはアップロードしたアカウントへのログインが必要です。\n"
                        + "サイトで削除してもChMateの履歴を自動で削除済みにしません。imgboxの管理URLは削除権限を含むため共有しないでください。")
                .setNegativeButton("キャンセル", null).setPositiveButton("管理ページを開く", (d, w) -> {
                    try {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(management))
                                .addCategory(Intent.CATEGORY_BROWSABLE).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        activity.getApplicationContext().startActivity(intent);
                    } catch (RuntimeException ignored) {
                        // ActivityNotFound/SecurityException messages can contain the secret URL.
                        Toast.makeText(activity, "管理ページを開けませんでした。ブラウザの設定を確認してください", Toast.LENGTH_LONG).show();
                    }
                }).show();
    }
}
