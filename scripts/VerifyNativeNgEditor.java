import java.nio.file.*;
import javax.tools.ToolProvider;

/** Offline reflection contracts: no Android UI, clipboard or NG writes. */
public final class VerifyNativeNgEditor {
    public static void main(String[] args) throws Exception {
        String source = Files.readString(Path.of("extensions/chmate/src/main/java/app/morphe/extension/chmate/NativeNgEditor.java"));
        String helper = source.substring(source.indexOf("    static int nameOrdinal"), source.indexOf("    static void openName"));
        Path temp = Files.createTempDirectory("haiagaru-ng-editor-");
        try {
            Path file = temp.resolve("Editor.java");
            Files.writeString(file, "import java.lang.reflect.*; import java.util.*; public class Editor {" + helper + """
                enum Legacy { WORD, NAME, ID }
                enum Modern { ID, THREAD, NAME, WORD }
                static class Old {
                    static void open(android.app.Activity a, Legacy kind, jp.syoboi.a2chMate.client.BBSUrlInfo url) {}
                }
                static class New {
                    static class Companion {
                        void launch(android.app.Activity a, Modern kind, jp.syoboi.a2chMate.client.BBSUrlInfo url) {}
                    }
                }
                static class Missing { static void irrelevant(Legacy kind) {} }
                static class Conflict {
                    static void a(android.app.Activity a, Legacy k, jp.syoboi.a2chMate.client.BBSUrlInfo u) {}
                    static void b(android.app.Activity a, Modern k, jp.syoboi.a2chMate.client.BBSUrlInfo u) {}
                }
                public static void verify() throws Exception {
                    if (nameOrdinal(Old.class) != 1 || nameOrdinal(New.class) != 2)
                        throw new AssertionError("NAME ordinal must not be hardcoded");
                    for (Class<?> type : new Class<?>[]{Missing.class, Conflict.class}) {
                        try { nameOrdinal(type); throw new AssertionError("Wrong tab fallback"); }
                        catch (ReflectiveOperationException expected) {}
                    }
                }
            }
            """);
            if (ToolProvider.getSystemJavaCompiler().run(null, null, null, "-d", temp.toString(), file.toString(),
                    "scripts/ng-editor/android/app/Activity.java", "scripts/ng-editor/jp/syoboi/a2chMate/client/BBSUrlInfo.java") != 0)
                throw new AssertionError("Compile");
            try (var loader = new java.net.URLClassLoader(new java.net.URL[]{temp.toUri().toURL()})) {
                loader.loadClass("Editor").getMethod("verify").invoke(null);
            }
            String action = Files.readString(Path.of("extensions/chmate/src/main/java/app/morphe/extension/chmate/WacchoiSuffixNg.java"));
            String confirm = action.substring(action.indexOf("    public static void confirm"), action.indexOf("    private static void register"));
            if (confirm.contains("register(activity") || !confirm.contains("setPrimaryClip") || !confirm.contains("NativeNgEditor.openName"))
                throw new AssertionError("Copy/editor action must not register a row");
            System.out.println("PASS native NGName editor ordinal, safe missing/ambiguous contracts, clipboard action and no automatic registration");
        } finally {
            try (var paths = Files.walk(temp)) {
                for (Path p : paths.sorted(java.util.Comparator.reverseOrder()).toList()) Files.delete(p);
            }
        }
    }
}
