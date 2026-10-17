package app.morphe.extension.chmate;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

/** System-owned downloads survive closing the viewer. No video is buffered in app memory. */
public final class VideoDownloads {
    static final int REQUEST_STORAGE = 0x4856;
    private static final ExecutorService WORKER = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private VideoDownloads() { }

    static void confirm(Activity activity, String url, Runnable approved) {
        if (!XMediaAttachments.isVideo(url)) {
            message(activity, "この動画URLは保存できません"); return;
        }
        new AlertDialog.Builder(activity).setTitle("動画を保存")
                .setMessage("Download/Haiagaru にMP4を保存します。再生とは別に動画全体を取得するため、"
                        + "通信量と空き容量にご注意ください。画面を閉じても保存は続きます。"
                        + "進捗・完了・失敗はAndroidのダウンロード通知で確認できます。")
                .setNegativeButton("キャンセル", null)
                .setPositiveButton("保存", (dialog, which) -> approved.run()).show();
    }

    static boolean needsStoragePermission(Activity activity) {
        return Build.VERSION.SDK_INT >= 23 && Build.VERSION.SDK_INT <= 28
                && activity.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED;
    }

    /** Called only after the user confirms saving (and grants legacy storage access, if needed). */
    public static void enqueue(Context context, String url) {
        Context app = context.getApplicationContext();
        if (!XMediaAttachments.isVideo(url)) { message(app, "この動画URLは保存できません"); return; }
        WORKER.execute(() -> {
            try {
                DownloadManager manager = (DownloadManager) app.getSystemService(Context.DOWNLOAD_SERVICE);
                if (manager == null) throw new IllegalStateException("ダウンロード機能が利用できません");
                String filename = VideoDownloadNames.filename(url);
                DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
                request.setTitle(filename);
                request.setDescription("Haiagaru 動画保存");
                request.setMimeType("video/mp4");
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "Haiagaru/" + filename);
                // Preserve the exact playback URL, including signed query parameters. Do not pass login cookies.
                long id = manager.enqueue(request);
                if (id <= 0) throw new IllegalStateException("ダウンロードを登録できませんでした");
                message(app, "動画の保存を開始しました。進捗はダウンロード通知で確認できます");
            } catch (RuntimeException error) {
                // Do not log signed URLs or account information.
                message(app, "動画の保存を開始できませんでした。空き容量・保存権限・ダウンロード機能を確認してください");
            }
        });
    }
    static void message(Context context, String text) {
        MAIN.post(() -> Toast.makeText(context, text, Toast.LENGTH_LONG).show());
    }
}
