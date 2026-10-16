package app.morphe.extension.chmate;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

/** File policy shared by the SAF importer and host-side tests. */
public final class ThemeImportFiles {
    private ThemeImportFiles() {}
    public static boolean safeName(String name) {
        if (name == null || name.isEmpty() || name.length() > 160
                || name.startsWith(".") || !name.equals(name.trim())) return false;
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (c < 32 || c == 127 || "/\\:*?\"<>|".indexOf(c) >= 0) return false;
        }
        return true;
    }
    public static boolean theme(String name) {
        // These base-theme identifiers are shared by 191 / 226 / 242.
        return safeName(name) && name.matches(".+\\.(day|night|ingrs|gray|sky|ml|day3)\\.txt");
    }
    public static boolean image(String name) {
        if (!safeName(name)) return false;
        String lower = name.toLowerCase(Locale.ROOT);
        return lower.endsWith(".png") || lower.endsWith(".jpg")
                || lower.endsWith(".jpeg") || lower.endsWith(".webp");
    }
    public static File child(File root, String relative) throws IOException {
        String[] parts = relative.split("/", -1);
        if (parts.length > 3 || (theme(parts[parts.length - 1]) && parts.length > 2)) {
            throw new IOException("テーマのフォルダ階層が深すぎます");
        }
        for (String part : parts) if (!safeName(part)) throw new IOException("使用できないファイル名です");
        File result = new File(root, relative);
        if (!result.getCanonicalPath().startsWith(root.getCanonicalPath() + File.separator)) {
            throw new IOException("保存先がテーマフォルダの外です");
        }
        return result;
    }
}
