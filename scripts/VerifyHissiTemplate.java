import java.nio.file.*;
import javax.tools.ToolProvider;

/** Runs template checks using production code without loading Android classes. */
public class VerifyHissiTemplate {
    public static void main(String[] args) throws Exception {
        String source = Files.readString(Path.of(
                "extensions/chmate/src/main/java/app/morphe/extension/chmate/HissiMenuCompatibility.java"));
        int start = source.indexOf("    public static String rewriteTemplate(");
        int end = source.indexOf("    /**", start);
        Path temp = Files.createTempDirectory("haiagaru-hissi-template-");
        try {
            Path pkg = temp.resolve("app/morphe/extension/chmate");
            Files.createDirectories(pkg);
            Path production = pkg.resolve("HissiMenuCompatibility.java");
            Files.writeString(production, "package app.morphe.extension.chmate; import java.util.regex.*; public class HissiMenuCompatibility {"
                    + source.substring(source.indexOf("    private static final Pattern HOST_FILTER_PLACEHOLDER"), source.indexOf("    private HissiMenuCompatibility"))
                    + source.substring(start, end) + "}");
            Path stub = pkg.resolve("Haiagaru.java");
            Files.writeString(stub, "package app.morphe.extension.chmate; public class Haiagaru {"
                    + "public static boolean dedicatedCheckerViewerAvailable(){return true;}"
                    + "public static int hissiCheckerMode(){return 0;}}");
            int result = ToolProvider.getSystemJavaCompiler().run(null, null, null,
                    "-d", temp.toString(), production.toString(), stub.toString(),
                    "scripts/VerifyHissiMenu.java",
                    "extensions/chmate/src/main/java/app/morphe/extension/chmate/KyodemoRouting.java");
            if (result != 0) throw new AssertionError("Compilation failed");
            try (var loader = new java.net.URLClassLoader(new java.net.URL[]{temp.toUri().toURL()})) {
                loader.loadClass("VerifyHissiMenu").getMethod("main", String[].class)
                        .invoke(null, (Object) new String[0]);
            }
        } finally {
            try (var paths = Files.walk(temp)) {
                for (Path path : paths.sorted(java.util.Comparator.reverseOrder()).toList())
                    Files.delete(path);
            }
        }
    }
}
