import app.morphe.extension.chmate.ResponseArchiveEntry;
import java.nio.file.*;
import javax.tools.ToolProvider;

/** Uses the actual snapshot class with synthetic native models. No app data or network. */
public final class VerifyResponseArchiveEntry {
    private static int checks;
    private static void check(boolean ok, String name) { if (!ok) throw new AssertionError(name); checks++; }
    public static void main(String[] args) throws Exception {
        Path temp = Files.createTempDirectory("haiagaru-response-archive-");
        String[] types = {"processAdDisplayErrorPostbackForUserError", "BouncyCastleSocketAdapterCompanion", "KeJ11"};
        String[][] fields = {{"o", "n", "q", "c"}, {"l", "m", "t", "d"}, {"o", "n", "p", "h"}};
        try {
            for (int i = 0; i < types.length; i++) {
                String[] f = fields[i];
                Path src = temp.resolve(types[i] + ".java");
                Files.writeString(src, "package o; public class " + types[i] + " { public int " + f[0]
                        + "=42; public String " + f[1] + "=\"name\", " + f[2]
                        + "=\"date ID:abc+/\", " + f[3] + "=\"hello<br>world &amp; test\"; }");
                check(ToolProvider.getSystemJavaCompiler().run(null, null, null, "-d", temp.toString(), src.toString()) == 0, "native fixture compile");
            }
            Path board = temp.resolve("Board.java");
            Files.writeString(board, "public class Board { public String d(int n){return url(n);} public String e(int n){return url(n);} public String c(int n){return url(n);} private String url(int n){return \"https://example.org/test/read.cgi/b/1234567890/\"+n;} }");
            check(ToolProvider.getSystemJavaCompiler().run(null, null, null, "-d", temp.toString(), board.toString()) == 0, "board compile");
            try (var loader = new java.net.URLClassLoader(new java.net.URL[]{temp.toUri().toURL()})) {
                Object b = loader.loadClass("Board").getConstructor().newInstance();
                for (String type : types) {
                    Object response = loader.loadClass("o." + type).getConstructor().newInstance();
                    ResponseArchiveEntry e = ResponseArchiveEntry.snapshot(response, b, "thread title");
                    check(e.number == 42 && e.url.endsWith("/42"), "selected response number/url");
                    check(e.name.equals("name") && e.date.equals("date ID:abc+/"), "name/date unchanged");
                    check(e.body.equals("hello<br>world &amp; test"), "raw body unchanged");
                    check(e.title.equals("thread title"), "title retained");
                }
            }
            for (String bad : new String[]{"file:///sdcard/a", "https://user:pass@example.org/a", "javascript:alert(1)", "not url"}) {
                try { new ResponseArchiveEntry(bad, "t", 1, "", "", "body"); throw new AssertionError("unsafe URL"); }
                catch (IllegalArgumentException expected) { checks++; }
            }
            try { new ResponseArchiveEntry("https://example.org/a", "t", 0, "", "", "body"); throw new AssertionError("zero number"); }
            catch (IllegalArgumentException expected) { checks++; }
            try { ResponseArchiveEntry.snapshot(new Object(), new Object(), "t"); throw new AssertionError("unknown native model"); }
            catch (IllegalArgumentException expected) { checks++; }
            System.out.println("PASS " + checks + " response snapshot checks (191/226/242)");
        } finally {
            try (var paths = Files.walk(temp)) {
                for (Path p : paths.sorted(java.util.Comparator.reverseOrder()).toList()) Files.delete(p);
            }
        }
    }
}
