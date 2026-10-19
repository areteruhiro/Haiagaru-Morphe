package app.morphe.extension.chmate;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import java.util.concurrent.atomic.AtomicInteger;

/** Notifications contain operation status only, never backup contents or credentials. */
final class BackupProgress {
    private static final AtomicInteger NEXT = new AtomicInteger(64000);
    private final Context context;
    private final int id = NEXT.incrementAndGet();
    private final String title;
    BackupProgress(Context context, String title) {
        this.context = context.getApplicationContext();
        this.title = title;
        update("処理を開始しています…", false);
    }
    void update(String text, boolean done) {
        try {
            NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (Build.VERSION.SDK_INT >= 26) manager.createNotificationChannel(new NotificationChannel(
                    "haiagaru-backup", "バックアップ・復元", NotificationManager.IMPORTANCE_LOW));
            Notification.Builder builder = Build.VERSION.SDK_INT >= 26
                    ? new Notification.Builder(context, "haiagaru-backup") : new Notification.Builder(context);
            builder.setSmallIcon(android.R.drawable.stat_sys_upload).setContentTitle(title)
                    .setContentText(text).setStyle(new Notification.BigTextStyle().bigText(text))
                    .setOngoing(!done).setAutoCancel(done).setOnlyAlertOnce(true);
            if (!done) builder.setProgress(0, 0, true);
            manager.notify(id, builder.build());
        } catch (RuntimeException error) {
            // An OS notification denial must not abort a valid backup.
            android.util.Log.w("HaiagaruBackup", "Cannot display operation notification", error);
        }
    }
    void dismiss() {
        ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE)).cancel(id);
    }
}
