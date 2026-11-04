package app.morphe.extension.chmate;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.io.IOException;

/** A restored history row is not evidence that its DAT was restored too. */
final class RestoredThreadCache {
    static void reconcile(Context context, JSONArray rows) throws Exception {
        if (rows == null || rows.length() == 0) return;
        File external = context.getExternalFilesDir(null);
        if (external == null || !external.isDirectory() || !external.canRead())
            throw new IOException("復元後のスレ本文の保存領域を確認できません");
        File dat = new File(HaiagaruSyncSnapshot.externalRoot(context), "dat");
        if (dat.exists() && (!dat.isDirectory() || !dat.canRead()))
            throw new IOException("復元後のスレ本文フォルダを確認できません");
        File file = context.getDatabasePath("roidon.sqlite");
        if (!file.isFile()) throw new IOException("ChMateの履歴DBが見つかりません");
        try (SQLiteDatabase db = SQLiteDatabase.openDatabase(file.getPath(), null, SQLiteDatabase.OPEN_READWRITE)) {
            reconcile(db, dat, rows);
        }
    }

    /** Run after incoming DAT files are committed, not before ZIP extraction. */
    static void reconcile(SQLiteDatabase db, File dat, JSONArray rows) throws Exception {
        ContentValues values = new ContentValues();
        values.put("res_count", 0);
        db.beginTransaction();
        try {
            for (int i = 0; i < rows.length(); i++) {
                JSONObject row = rows.getJSONObject(i);
                String name = row.optString("name", "");
                long created = row.optLong("created", -1);
                if (name.isEmpty() || name.indexOf('/') >= 0 || name.indexOf('\\') >= 0 || created < 0) continue;
                File log = new File(dat, name + "_" + created + ".dat");
                if (log.exists()) {
                    if (!log.isFile() || !log.canRead())
                        throw new IOException("復元後のスレ本文を確認できません");
                    if (log.length() > 0) continue;
                }
                // Native patrol skips downloads when server_res_count <= res_count.
                // Correct only the derived local-cache count. Keep read_count,
                // server_res_count, read_state, flags, and all marks/history.
                db.update("bookmarks", values, "name=? AND created=? AND res_count>0",
                        new String[]{name, Long.toString(created)});
            }
            db.setTransactionSuccessful();
        } finally { db.endTransaction(); }
    }
    private RestoredThreadCache() {}
}
