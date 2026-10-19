package app.morphe.extension.chmate;

import android.content.Context;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.system.Os;
import android.system.OsConstants;
import java.io.*;
import java.security.MessageDigest;

/** Success means the closed document can be read back with exactly the saved bytes. */
final class BackupDocuments {
    static void save(Context context, File snapshot, Uri uri, Runnable verifying) throws Exception {
        if (snapshot.length() == 0) throw new IOException("空のバックアップは保存できません");
        MessageDigest expected = MessageDigest.getInstance("SHA-256");
        ParcelFileDescriptor descriptor = context.getContentResolver().openFileDescriptor(uri, "wt");
        if (descriptor == null) throw new IOException("保存先を開けませんでした");
        try (OutputStream output = new ParcelFileDescriptor.AutoCloseOutputStream(descriptor);
             InputStream input = new FileInputStream(snapshot)) {
            byte[] buffer = new byte[65536]; int n; long copied = 0;
            while ((n = input.read(buffer)) != -1) {
                expected.update(buffer, 0, n); output.write(buffer, 0, n); copied += n;
            }
            output.flush();
            if (copied != snapshot.length()) throw new IOException("バックアップを最後まで保存できませんでした");
            if (OsConstants.S_ISREG(Os.fstat(descriptor.getFileDescriptor()).st_mode))
                Os.fsync(descriptor.getFileDescriptor());
            descriptor.checkError();
        }
        verifying.run();
        MessageDigest actual = MessageDigest.getInstance("SHA-256");
        try (InputStream input = context.getContentResolver().openInputStream(uri)) {
            if (input == null) throw new IOException("保存結果を確認できませんでした");
            byte[] buffer = new byte[65536]; int n;
            while ((n = input.read(buffer)) != -1) actual.update(buffer, 0, n);
        }
        if (!MessageDigest.isEqual(expected.digest(), actual.digest()))
            throw new IOException("保存結果が元のバックアップと一致しません。保存先を確認して再実行してください");
    }
}
