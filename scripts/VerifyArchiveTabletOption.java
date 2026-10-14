import java.nio.file.*;
import javax.tools.ToolProvider;

/** Tests the actual archive navigation method with isolated intent/pref stubs. */
public final class VerifyArchiveTabletOption {
    public static void main(String[] args) throws Exception {
        Path base = Path.of("extensions/chmate/src/main/java/app/morphe/extension/chmate");
        String viewer = Files.readString(base.resolve("HissiMenuActivity.java"));
        String route = viewer.substring(viewer.indexOf("    private boolean openEddiThreadInChMate("), viewer.indexOf("    private static String toHttpsUrl("));
        String settings = Files.readString(base.resolve("Haiagaru.java"));
        String getter = settings.substring(settings.indexOf("    static boolean archiveThreadStandalone("), settings.indexOf("    private static void addEddiArchiveSearchControl("));
        String harness = """
            import java.util.*;
            public class ArchiveTabletHarness {
                static class Context {}
                static class Prefs { Map<String, Boolean> values = new HashMap<>(); boolean getBoolean(String key, boolean fallback) { return values.getOrDefault(key, fallback); } }
                static class Haiagaru {
                    static Prefs prefs = new Prefs(); static Prefs preferences(Context context) { return prefs; }
                    GETTER
                }
                static class Uri { static String parse(String url) { return url; } }
                static class Intent {
                    static final String ACTION_VIEW = "view";
                    String action, url; Class<?> target; Map<String, Boolean> extras = new HashMap<>();
                    Intent(String action, String url) { this.action = action; this.url = url; }
                    void setClass(Object context, Class<?> type) { target = type; }
                    void putExtra(String key, boolean value) { extras.put(key, value); }
                }
                static class OpenUrlActivity {}
                static class HissiLinkRouting { static boolean isThreadUrl(String url) { return url != null && url.contains("/test/read.cgi/"); } }
                static class Log { static void e(String tag, String message, Throwable error) {} }
                static class Viewer extends Context {
                    static final String LOG_TAG = "test";
                    boolean eddiArchiveMode = true; Intent launched;
                    void startActivity(Intent intent) { launched = intent; }
                    ROUTE
                }
                public static void verify() {
                    String url = "https://bbs.eddibb.cc/test/read.cgi/liveedge/1702028272/";
                    Viewer viewer = new Viewer();
                    if (!Haiagaru.archiveThreadStandalone(viewer)) throw new AssertionError("Default changed");
                    for (boolean enabled : new boolean[]{true, false, true}) {
                        Haiagaru.prefs.values.put("archiveThreadStandalone", enabled);
                        viewer.launched = null;
                        if (!viewer.openEddiThreadInChMate(url)) throw new AssertionError("Not handled");
                        Intent launched = viewer.launched;
                        if (launched.target != OpenUrlActivity.class || !url.equals(launched.url)) throw new AssertionError("Wrong destination");
                        if (enabled != Boolean.TRUE.equals(launched.extras.get("haiagaru.archive.return"))) throw new AssertionError("Wrong tablet forwarding marker");
                        if (!enabled && launched.extras.containsKey("haiagaru.archive.return")) throw new AssertionError("Off must leave native routing unchanged");
                    }
                    viewer.eddiArchiveMode = false; viewer.launched = null;
                    if (viewer.openEddiThreadInChMate(url) || viewer.launched != null) throw new AssertionError("Non-archive affected");
                    viewer.eddiArchiveMode = true;
                    if (viewer.openEddiThreadInChMate("https://eddiarchive3rd.boy.jp/") || viewer.launched != null) throw new AssertionError("Search URL handled as thread");
                }
            }
            """;
        Path temp = Files.createTempDirectory("haiagaru-archive-tablet-");
        try {
            Path file = temp.resolve("ArchiveTabletHarness.java");
            Files.writeString(file, harness.replace("GETTER", getter).replace("ROUTE", route));
            if (ToolProvider.getSystemJavaCompiler().run(null, null, null, "-d", temp.toString(), file.toString()) != 0) throw new AssertionError("Compilation failed");
            try (var loader = new java.net.URLClassLoader(new java.net.URL[]{temp.toUri().toURL()})) {
                loader.loadClass("ArchiveTabletHarness").getMethod("verify").invoke(null);
            }
            System.out.println("PASS archive tablet option: default, on/off, destination and non-archive isolation");
        } finally {
            try (var paths = Files.walk(temp)) {
                for (Path path : paths.sorted(java.util.Comparator.reverseOrder()).toList()) Files.delete(path);
            }
        }
    }
}
