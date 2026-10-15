import app.morphe.extension.chmate.NativeRestoreFiles;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;

public final class VerifyNativeRestoreFiles {
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
    private static void failure(File source, File target, NativeRestoreFiles.Validator validator) throws Exception {
        byte[] original = Files.readAllBytes(target.toPath());
        try { NativeRestoreFiles.install(source, target, validator); throw new AssertionError("Expected failure"); }
        catch (IOException expected) { }
        check(Arrays.equals(original, Files.readAllBytes(target.toPath())), "failed install preserves old DB");
        check(source.isFile(), "failed install preserves source");
    }
    public static void main(String[] args) throws Exception {
        File directory = Files.createTempDirectory("haiagaru-restore-test-").toFile();
        File source = new File(directory, "source.sqlite"), target = new File(directory, "target.sqlite");
        byte[] data = new byte[1024 * 1024 + 31];
        new java.util.Random(42).nextBytes(data);
        Files.write(source.toPath(), data);
        byte[] old = new byte[]{1, 2, 3};
        Files.write(target.toPath(), old);
        failure(source, target, file -> { throw new IOException("injected SQLite rejection"); });
        Thread.currentThread().interrupt();
        failure(source, target, file -> { });
        check(Thread.currentThread().isInterrupted(), "cancellation flag not consumed");
        Thread.interrupted();
        File wal = new File(source + "-wal");
        Files.write(wal.toPath(), new byte[]{1});
        failure(source, target, file -> { });
        Files.delete(wal.toPath());
        NativeRestoreFiles.install(source, target, file -> check(file.length() == data.length, "stage validated"));
        check(Arrays.equals(data, Files.readAllBytes(target.toPath())), "complete installed content");
        check(Arrays.equals(data, Files.readAllBytes(source.toPath())), "source unchanged");
        File[] previous = directory.listFiles((dir, name) -> name.startsWith("target.sqlite.before-restore-"));
        check(previous != null && previous.length == 1, "old DB retained exactly once");
        check(Arrays.equals(old, Files.readAllBytes(previous[0].toPath())), "old DB recoverable");
        File empty = new File(directory, "empty.sqlite");
        Files.write(empty.toPath(), new byte[0]);
        failure(empty, target, file -> { });
        failure(target, target, file -> { });
        check(directory.listFiles((dir, name) -> name.startsWith("haiagaru-restore-")).length == 0,
                "no leaked staging files");
        File sparse = new File(directory, "large-sparse.dat");
        try (var output = new java.io.RandomAccessFile(sparse, "rw")) { output.setLength(3L * 1024 * 1024 * 1024 + 123); }
        check(NativeRestoreFiles.size(sparse) == 3L * 1024 * 1024 * 1024 + 123, "size accounting exceeds 2 GiB");
        Files.delete(sparse.toPath());
        System.out.println("PASS native restore: validated streaming copy, cancellation, old DB and source preservation; " + directory);
    }
}
