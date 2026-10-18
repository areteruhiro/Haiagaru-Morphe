package app.morphe.extension.chmate;

import android.util.AtomicFile;
import android.util.Log;
import java.io.*;
import java.util.UUID;

/** Runs on the native DAT-reading worker before the parser caches the response list. */
public final class RestoredDatRows {
    private static final java.util.Map<String, String> CHECKED = new java.util.LinkedHashMap<String, String>() {
        @Override protected boolean removeEldestEntry(java.util.Map.Entry<String, String> entry) { return size() > 256; }
    };
    public static synchronized void prepare(File dat) {
        File staged = null;
        try {
            if (dat == null || !dat.isFile() || !dat.getName().endsWith(".dat")
                    || dat.getParentFile() == null || !"dat".equals(dat.getParentFile().getName())) return;
            String stamp = dat.length() + ":" + dat.lastModified();
            if (stamp.equals(CHECKED.get(dat.getAbsolutePath()))) return;
            // Normal logs take one block scan per file revision and create no files.
            boolean zeros = false;
            try (InputStream in = new BufferedInputStream(new FileInputStream(dat))) {
                byte[] block = new byte[65536]; int read;
                scan: while ((read = in.read(block)) != -1) for (int i = 0; i < read; i++)
                    if (block[i] == 0) { zeros = true; break scan; }
            }
            if (!zeros) { CHECKED.put(dat.getAbsolutePath(), stamp); return; }
            long length = dat.length(), modified = dat.lastModified();
            staged = File.createTempFile("haiagaru-dat-row-", ".tmp", dat.getParentFile());
            int count;
            try (InputStream in = new BufferedInputStream(new FileInputStream(dat));
                 FileOutputStream file = new FileOutputStream(staged);
                 OutputStream out = new BufferedOutputStream(file)) {
                count = DeletedDatRows.copy(in, out);
                out.flush(); file.getFD().sync();
            }
            if (count == 0) { CHECKED.put(dat.getAbsolutePath(), stamp); return; }
            if (length != staged.length() || length != dat.length() || modified != dat.lastModified()) return;
            File directory = new File(dat.getParentFile().getParentFile(), "haiagaru-dat-recovery");
            if (!directory.isDirectory() && !directory.mkdirs()) throw new IOException("Cannot preserve original DAT");
            File original = new File(directory, dat.getName() + "." + UUID.randomUUID() + ".original");
            try (InputStream in = new BufferedInputStream(new FileInputStream(dat));
                 FileOutputStream out = new FileOutputStream(original)) {
                transfer(in, out); out.getFD().sync();
            }
            if (original.length() != length || length != dat.length() || modified != dat.lastModified()) return;
            AtomicFile atomic = new AtomicFile(dat);
            FileOutputStream output = null;
            try {
                output = atomic.startWrite();
                try (InputStream in = new BufferedInputStream(new FileInputStream(staged))) { transfer(in, output); }
                atomic.finishWrite(output); output = null;
            } finally { if (output != null) atomic.failWrite(output); }
            dat.setLastModified(modified);
            CHECKED.put(dat.getAbsolutePath(), dat.length() + ":" + dat.lastModified());
            Log.i("Haiagaru", "Preserved " + count + " erased DAT response slot(s); original retained");
        } catch (Exception error) { Log.w("Haiagaru", "DAT row preservation skipped", error); }
        finally { if (staged != null) staged.delete(); }
    }
    private static void transfer(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[65536]; int read;
        while ((read = in.read(buffer)) != -1) out.write(buffer, 0, read);
    }
}
