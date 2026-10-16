package app.morphe.extension.chmate;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public final class VerifyThemeImportFiles {
    private static int checks;
    private static void check(boolean value, String name) {
        if (!value) throw new AssertionError(name);
        checks++;
    }
    public static void main(String[] args) throws Exception {
        for (String base : new String[]{"day", "night", "ingrs", "gray", "sky", "ml", "day3"}) {
            check(ThemeImportFiles.theme("自作テーマ." + base + ".txt"), base + " theme supported");
        }
        for (String name : new String[]{"../a.day.txt", "/a.day.txt", "..", ".night.txt", "a\\x.day.txt", "bad\n.day.txt",
                "a:evil.day.txt", "a.day.txt ", "a.unknown.txt", "a.night.txt.exe", "a.NIGHT.txt", ""}) {
            check(!ThemeImportFiles.theme(name), "Reject unsafe or unrecognized name: " + name);
        }
        check(ThemeImportFiles.image("picture.PNG"), "PNG asset");
        check(ThemeImportFiles.image("picture.jpg"), "JPEG asset");
        check(ThemeImportFiles.image("picture.webp"), "WebP asset");
        check(!ThemeImportFiles.image("picture.html"), "No unrelated files");
        File root = Files.createTempDirectory("theme-policy-test-").toFile();
        try {
            check(ThemeImportFiles.child(root, "test.ml.txt").getParentFile().equals(root), "Root file");
            check(ThemeImportFiles.child(root, "test/test.ml.txt").getParentFile().getName().equals("test"), "One subfolder");
            check(ThemeImportFiles.child(root, "test/images/icon.png").getParentFile().getName().equals("images"), "Nested assets");
            for (String path : new String[]{"../test.ml.txt", "a/b/test.ml.txt", "/test.ml.txt", "a//test.ml.txt", "a/../test.ml.txt"}) {
                boolean rejected = false;
                try { ThemeImportFiles.child(root, path); } catch (IOException expected) { rejected = true; }
                check(rejected, "Reject invalid path " + path);
            }
        } finally { Files.delete(root.toPath()); }
        System.out.println("PASS " + checks + " theme import policy checks");
    }
}
