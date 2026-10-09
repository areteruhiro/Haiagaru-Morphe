import java.nio.file.*;
import javax.tools.ToolProvider;

/** Checks 226's five-argument and 241/242's six-argument native builders. */
public class VerifyWacchoiNameSheet {
    public static final class Builder226 {
        String title;
        Runnable callback;
        public void add(String title, String subtitle, boolean enabled, boolean checked, Runnable callback) {
            if (!enabled || checked) throw new AssertionError("226 item flags");
            this.title = title;
            this.callback = callback;
        }
    }
    public static final class Builder242 {
        String title;
        Integer icon;
        Runnable callback;
        public void add(String title, String subtitle, boolean enabled, boolean checked,
                Integer icon, Runnable callback) {
            if (!enabled || checked) throw new AssertionError("242 item flags");
            this.title = title;
            this.icon = icon;
            this.callback = callback;
        }
    }
    public static void main(String[] args) throws Exception {
        String source = Files.readString(Path.of("extensions/chmate/src/main/java/app/morphe/extension/chmate/WacchoiLongPressMenu.java"));
        String helper = source.substring(source.indexOf("    private static void appendSheetItem"),
                source.indexOf("    /** Marks the response-menu builder"));
        Path temp = Files.createTempDirectory("haiagaru-name-sheet-");
        try {
            Path file = temp.resolve("Sheet.java");
            Files.writeString(file, "import java.lang.reflect.Method; public class Sheet {" + helper + "}");
            if (ToolProvider.getSystemJavaCompiler().run(null, null, null,
                    "-d", temp.toString(), file.toString()) != 0) throw new AssertionError("compile");
            try (var loader = new java.net.URLClassLoader(new java.net.URL[]{temp.toUri().toURL()})) {
                var append = loader.loadClass("Sheet").getDeclaredMethod("appendSheetItem", java.lang.reflect.Method.class,
                        Object.class, boolean.class, String.class, int.class, Object.class);
                append.setAccessible(true);
                int[] clicks = {0};
                Runnable click = () -> clicks[0]++;
                Builder226 old = new Builder226();
                append.invoke(null, Builder226.class.getMethod("add", String.class, String.class,
                        boolean.class, boolean.class, Runnable.class), old, false, "NG suffix", 123, click);
                old.callback.run();
                Builder242 current = new Builder242();
                append.invoke(null, Builder242.class.getMethod("add", String.class, String.class,
                        boolean.class, boolean.class, Integer.class, Runnable.class), current, true, "NG suffix", 123, click);
                current.callback.run();
                if (!"NG suffix".equals(old.title) || !"NG suffix".equals(current.title)
                        || current.icon != 123 || clicks[0] != 2) throw new AssertionError("menu callback");
                System.out.println("PASS: 226/241/242 name-sheet item signatures and callbacks");
            }
        } finally {
            try (var paths = Files.walk(temp)) {
                for (Path path : paths.sorted(java.util.Comparator.reverseOrder()).toList()) Files.delete(path);
            }
        }
    }
}
