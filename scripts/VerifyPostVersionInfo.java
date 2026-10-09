import java.nio.file.*;
import javax.tools.ToolProvider;

/** Compiles the production formatter with a fixture build version. */
public class VerifyPostVersionInfo {
    public static void main(String[] args) throws Exception {
        Path temp = Files.createTempDirectory("haiagaru-post-version-");
        try {
            Path pkg = Files.createDirectories(temp.resolve("app/morphe/extension/chmate"));
            Path formatter = pkg.resolve("PostVersionInfo.java");
            Files.copy(Path.of("extensions/chmate/src/main/java/app/morphe/extension/chmate/PostVersionInfo.java"), formatter);
            Path config = pkg.resolve("BuildConfig.java");
            Files.writeString(config, "package app.morphe.extension.chmate; final class BuildConfig { static final String HAIAGARU_VERSION = \"test-9.8.7\"; }");
            if (ToolProvider.getSystemJavaCompiler().run(null, null, null, "-d", temp.toString(),
                    formatter.toString(), config.toString()) != 0) throw new AssertionError("compile");
            try (var loader = new java.net.URLClassLoader(new java.net.URL[]{temp.toUri().toURL()})) {
                var append = loader.loadClass("app.morphe.extension.chmate.PostVersionInfo").getMethod("append", String.class);
                for (String version : new String[]{"0.8.10.191 dev", "0.8.10.226 dev", "0.8.10.241", "0.8.10.242 dev"}) {
                    String original = "2chMate " + version + "/Xiaomi/Mi Note 10/11/DR";
                    String expected = original + "/Haiagaru test-9.8.7";
                    if (!expected.equals(append.invoke(null, original))) throw new AssertionError("version/device info " + version);
                    if (!expected.equals(append.invoke(null, expected))) throw new AssertionError("duplicate version " + version);
                }
                for (String text : new String[]{"", "ordinary message", "https://example.com/", "2chMate"}) {
                    if (!text.equals(append.invoke(null, text))) throw new AssertionError("unrelated text " + text);
                }
                if (append.invoke(null, (Object)null) != null) throw new AssertionError("null text");
                System.out.println("PASS: four ChMate versions, original info preserved, bundle version and idempotence");
            }
        } finally {
            try (var paths = Files.walk(temp)) {
                for (Path path : paths.sorted(java.util.Comparator.reverseOrder()).toList()) Files.delete(path);
            }
        }
    }
}
