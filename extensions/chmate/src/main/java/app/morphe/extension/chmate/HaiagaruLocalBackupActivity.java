package app.morphe.extension.chmate;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/** Android document-picker transport for the same validated snapshot used by MEGA. */
public final class HaiagaruLocalBackupActivity extends Activity {
    public static final String EXTRA_MODE = "haiagaru.backup.mode";
    public static final String EXTRA_CATEGORIES = "haiagaru.backup.categories";
    public static final int MODE_SAVE = 1;
    public static final int MODE_OPEN = 2;
    private static final int REQUEST_DOCUMENT = 1;
    private int mode;
    private int categories;
    private boolean pickerStarted;
    private HaiagaruBackupArchive.Backup openedBackup;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        mode = getIntent().getIntExtra(EXTRA_MODE, 0);
        categories = getIntent().getIntExtra(EXTRA_CATEGORIES, HaiagaruSyncSnapshot.DEFAULT)
                & HaiagaruSyncSnapshot.ALL;
        pickerStarted = state != null && state.getBoolean("pickerStarted", false);
        if (mode != MODE_SAVE && mode != MODE_OPEN) {
            finish();
            return;
        }
        if (!pickerStarted) launchPicker();
    }

    @Override protected void onSaveInstanceState(Bundle state) {
        state.putBoolean("pickerStarted", pickerStarted);
        super.onSaveInstanceState(state);
    }

    private void launchPicker() {
        Intent picker = new Intent(mode == MODE_SAVE
                ? Intent.ACTION_CREATE_DOCUMENT : Intent.ACTION_OPEN_DOCUMENT);
        picker.addCategory(Intent.CATEGORY_OPENABLE);
        picker.setType("application/zip");
        if (mode == MODE_SAVE) {
            picker.putExtra(Intent.EXTRA_TITLE, "haiagaru-backup-" +
                    BackupTime.filename(System.currentTimeMillis()) + ".zip");
        } else {
            picker.setType("*/*");
            picker.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"application/zip", "application/x-zip-compressed", "application/json", "text/plain", "application/octet-stream"});
        }
        try {
            pickerStarted = true;
            startActivityForResult(picker, REQUEST_DOCUMENT);
        } catch (Exception error) {
            showError("ファイル選択画面を開けませんでした");
        }
    }

    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request != REQUEST_DOCUMENT) return;
        Uri uri = data == null ? null : data.getData();
        if (result != RESULT_OK || uri == null) {
            finish();
            return;
        }
        if (mode == MODE_SAVE) save(uri);
        else open(uri);
    }

    private void save(Uri uri) {
        if (categories == 0) {
            showError("バックアップする項目が選ばれていません");
            return;
        }
        run("端末のファイルにバックアップしました", () -> {
            File snapshot = HaiagaruBackupArchive.capture(getApplicationContext(), categories);
            try (ParcelFileDescriptor file = getContentResolver().openFileDescriptor(uri, "wt");
                 InputStream input = new FileInputStream(snapshot)) {
                if (file == null) throw new IOException("保存先を開けませんでした");
                try (OutputStream output = new ParcelFileDescriptor.AutoCloseOutputStream(file)) {
                    HaiagaruBackupArchive.copy(input, output);
                    output.flush();
                }
            } finally { snapshot.delete(); }
        });
    }

    private void open(Uri uri) {
        run(null, () -> {
            File temporary = HaiagaruBackupArchive.temporary(this);
            boolean transferred = false;
            try (InputStream input = getContentResolver().openInputStream(uri);
                 OutputStream output = new FileOutputStream(temporary)) {
                if (input == null) throw new IOException("ファイルを開けませんでした");
                HaiagaruBackupArchive.copy(input, output);
                transferred = true;
            } finally { if (!transferred) temporary.delete(); }
            HaiagaruBackupArchive.Backup backup = HaiagaruBackupArchive.read(this, temporary);
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) backup.close();
                else { openedBackup = backup; confirmRestore(backup); }
            });
        });
    }

    private void confirmRestore(HaiagaruBackupArchive.Backup backup) {
        JSONObject snapshot = backup.snapshot;
        int available = snapshot.optInt("categories", 0) & HaiagaruSyncSnapshot.ALL;
        int[] bits = HaiagaruMegaSync.CATEGORY_BITS;
        String[] names = HaiagaruMegaSync.CATEGORY_NAMES.clone();
        boolean[] selected = new boolean[bits.length];
        for (int index = 0; index < bits.length; index++) {
            boolean present = (available & bits[index]) != 0;
            names[index] += present ? "" : "（バックアップに含まれません）";
            selected[index] = present && (categories & bits[index]) != 0;
        }
        String summary;
        try {
            summary = HaiagaruMegaSync.buildChangeSummary(getApplicationContext(), snapshot, available);
        } catch (Exception error) {
            showError("変更内容を確認できませんでした");
            return;
        }
        new AlertDialog.Builder(this).setTitle("端末のバックアップから復元")
                .setMultiChoiceItems(names, selected, (dialog, index, checked) ->
                        selected[index] = checked && (available & bits[index]) != 0)
                .setNegativeButton("キャンセル", (dialog, which) -> finish())
                .setNeutralButton("内容・注意事項", (d, w) -> new AlertDialog.Builder(this)
                        .setMessage("復元前に現在のデータをアプリ内へ退避します。\n" + summary + "\n" + HaiagaruBackupSecrets.WARNING)
                        .setPositiveButton("項目選択に戻る", (info, chosen) -> confirmRestore(backup))
                        .setOnCancelListener(info -> finish()).show())
                .setPositiveButton("復元", (dialog, which) -> {
                    int flags = 0;
                    for (int index = 0; index < bits.length; index++) {
                        if (selected[index]) flags |= bits[index];
                    }
                    if (flags == 0) {
                        showError("復元する項目を選択してください");
                        return;
                    }
                    final int chosen = flags;
                    HaiagaruMegaSync.confirmSensitive(this, chosen, () -> {
                    openedBackup = null; // Worker owns the temporary file from here.
                    run("復元しました。ChMateを再起動してください", () -> {
                        try (HaiagaruBackupArchive.Backup owned = backup) {
                            HaiagaruBackupArchive.restore(getApplicationContext(), backup, chosen, false);
                        }
                    });
                    }, this::finish);
                })
                .setOnCancelListener(dialog -> finish()).show();
    }

    private void run(String success, Operation operation) {
        new Thread(() -> {
            try {
                operation.perform();
                if (success != null) runOnUiThread(() -> {
                    Toast.makeText(this, success, Toast.LENGTH_LONG).show();
                    finish();
                });
            } catch (Exception error) {
                runOnUiThread(() -> showError(error instanceof IOException
                        ? error.getMessage() : "バックアップを処理できませんでした"));
            }
        }, "Haiagaru-local-backup").start();
    }

    private void showError(String message) {
        new AlertDialog.Builder(this).setTitle("バックアップを処理できませんでした")
                .setMessage(message).setPositiveButton("閉じる", (dialog, which) -> finish())
                .setOnCancelListener(dialog -> finish()).show();
    }

    @Override protected void onDestroy() {
        if (openedBackup != null) { openedBackup.close(); openedBackup = null; }
        super.onDestroy();
    }

    private interface Operation { void perform() throws Exception; }
}
