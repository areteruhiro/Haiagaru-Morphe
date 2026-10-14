import java.nio.file.*;
import javax.tools.ToolProvider;

/** Exercise the actual extension state logic with a fake display-only title bar. */
public final class VerifyAutoReloadTitleState {
    private static void check(boolean value, String label) { if (!value) throw new AssertionError(label); }
    public static void main(String[] args) throws Exception {
        String source = Files.readString(Path.of("extensions/chmate/src/main/java/app/morphe/extension/chmate/LegacyAutoReload.java"));
        String methods = source.substring(source.indexOf("    public static String displayTitle"),
                source.indexOf("    public static void scrollState"));
        Path dir = Path.of("build/verification/auto-title-state-tests");
        Files.createDirectories(dir);
        String harness = "import java.util.WeakHashMap; import app.morphe.extension.chmate.AutoReloadTitle; "
                + "public class TitleHarness { private static final WeakHashMap<Object, TitleState> TITLES = new WeakHashMap<>();"
                + methods
                + "public static void update(Object bar,long ms){updateTitleInterval(bar,ms);}"
                + "public static class Bar { public String shown; public int calls; public void setTitle(String s){calls++;shown=displayTitle(this,s);} }"
                + "public static class Log { public static void w(String tag,String msg,Exception error){throw new AssertionError(error);} }}";
        Path file = dir.resolve("TitleHarness.java");
        Files.writeString(file, harness);
        if (ToolProvider.getSystemJavaCompiler().run(null, null, null, "-d", dir.toString(), file.toString(),
                "extensions/chmate/src/main/java/app/morphe/extension/chmate/AutoReloadTitle.java") != 0)
            throw new AssertionError("Harness compile");
        try (var loader = new java.net.URLClassLoader(new java.net.URL[]{dir.toUri().toURL()})) {
            Class<?> type = loader.loadClass("TitleHarness"), barType = loader.loadClass("TitleHarness$Bar");
            Object bar = barType.getConstructor().newInstance(), other = barType.getConstructor().newInstance();
            var setter = barType.getMethod("setTitle", String.class);
            var update = type.getMethod("update", Object.class, long.class);
            var shown = barType.getField("shown");
            setter.invoke(bar, "original"); setter.invoke(other, "other thread");
            update.invoke(bar, bar, 10000L);
            check("【自動10s】 original".equals(shown.get(bar)), "Start indicator");
            int calls = barType.getField("calls").getInt(bar);
            update.invoke(bar, bar, 10000L);
            check(calls == barType.getField("calls").getInt(bar), "No redundant redraw");
            update.invoke(bar, bar, 13000L);
            check("【自動13s】 original".equals(shown.get(bar)), "Backoff without duplicated prefix");
            setter.invoke(bar, "native title changed");
            check("【自動13s】 native title changed".equals(shown.get(bar)), "Live native title update");
            update.invoke(bar, bar, 0L);
            check("native title changed".equals(shown.get(bar)), "Stop restores latest native title");
            check("other thread".equals(shown.get(other)), "Other tablet title unaffected");
            update.invoke(bar, other, 10000L);
            setter.invoke(other, new Object[]{null});
            update.invoke(bar, other, 0L);
            check(shown.get(other) == null, "Hidden title stays hidden");
        }
        System.out.println("PASS title start/stop/backoff/native-update/tablet isolation");
    }
}
