import java.nio.file.*;
import javax.tools.ToolProvider;

/** Runs the actual append implementation with isolated native-model and Android stubs. */
public final class VerifyImgurAttachmentDedup {
    public static void main(String[] args) throws Exception {
        String source = Files.readString(Path.of("extensions/chmate/src/main/java/app/morphe/extension/chmate/ImgurAlbumAttachments.java"));
        String patterns = source.substring(source.indexOf("    private static final Pattern ALBUM"), source.indexOf("    private static final ExecutorService"));
        String append = source.substring(source.indexOf("    private static void append("), source.indexOf("    private static final class Entry"));
        String harness = """
            import java.util.*; import java.util.regex.*; import java.lang.reflect.*;
            import app.morphe.extension.chmate.TwimgImageUrls;
            public class AlbumDedupHarness {
                static class Entry { String[] images; Entry(String... images) { this.images = images; } }
                static Map<String, Entry> CACHE = new HashMap<>();
                static Map<Object, Boolean> VIDEO_MODELS = new HashMap<>();
                static class Haiagaru { static boolean enabled = true; static int mediaAutoFetchMode() { return 1; } static boolean mediaImageFetchEnabled() { return enabled; } }
                static class InstagramMediaAttachments { static String[] postIds(String url) { return new String[0]; } }
                static class XMediaAttachments { static boolean isVideo(String url) { return false; } static boolean isPoster(String url) { return false; } static String generatedPoster(String url) { return null; } }
                static class VideoFrameThumbnail { static void request(String url) {} }
                static class Log { static void w(String tag, String message, Throwable error) { throw new AssertionError(error); } }
                static class Model { String[] urls; CharSequence[] projections = {"cached"}; Model(String... urls) { this.urls = urls; } }
                static void check(Model model, String... expected) { if (!Arrays.equals(model.urls, expected)) throw new AssertionError(Arrays.toString(model.urls)); }
                public static void verify() {
                    String image = "https://i.imgur.com/LWkGnaI.png", other = "https://i.imgur.com/Other.jpg";
                    CACHE.put("K2De8", new Entry(image, other));
                    for (String host : new String[]{"i.imgur.com", "imgur.com", "www.imgur.com"}) {
                        Model model = new Model("https://" + host + "/a/K2De8", image);
                        append(model, "urls", new String[]{image, other, image});
                        check(model, image, other);
                        if (model.projections != null) throw new AssertionError("Projection not invalidated");
                        append(model, "urls", new String[]{image, other});
                        check(model, image, other);
                    }
                    Model pending = new Model("https://i.imgur.com/a/Pending", image);
                    append(pending, "urls", new String[0]); check(pending, "https://i.imgur.com/a/Pending", image);
                    CACHE.put("Pending", new Entry());
                    append(pending, "urls", new String[0]); check(pending, "https://i.imgur.com/a/Pending", image);
                    Haiagaru.enabled = false;
                    Model disabled = new Model("https://i.imgur.com/a/K2De8");
                    append(disabled, "urls", new String[]{image}); check(disabled, "https://i.imgur.com/a/K2De8");
                    Haiagaru.enabled = true;
                    Model mixed = new Model("https://i.imgur.com/a/K2De8?x=1#images", "https://i.imgur.com/a/Unknown", other);
                    append(mixed, "urls", new String[]{image, other});
                    check(mixed, "https://i.imgur.com/a/Unknown", other, image);
                    String xImage = "https://pbs.twimg.com/media/HUL_2IXaEAAne0T?format=png&name=900x900";
                    String normalized = TwimgImageUrls.canonical(xImage);
                    Model x = new Model(xImage, normalized);
                    append(x, "urls", new String[]{normalized}); check(x, normalized);
                    if (x.projections != null) throw new AssertionError("X projection not invalidated");
                    Haiagaru.enabled = false;
                    Model xDisabled = new Model(xImage);
                    append(xDisabled, "urls", new String[]{normalized}); check(xDisabled, xImage);
                }
            REPLACE
            }
            """;
        Path temp = Files.createTempDirectory("haiagaru-album-dedup-");
        try {
            Path file = temp.resolve("AlbumDedupHarness.java");
            Files.writeString(file, harness.replace("REPLACE", patterns + append));
            if (ToolProvider.getSystemJavaCompiler().run(null, null, null, "-d", temp.toString(), file.toString(),
                    "extensions/chmate/src/main/java/app/morphe/extension/chmate/TwimgImageUrls.java") != 0) throw new AssertionError("Compilation failed");
            try (var loader = new java.net.URLClassLoader(new java.net.URL[]{temp.toUri().toURL()})) {
                loader.loadClass("AlbumDedupHarness").getMethod("verify").invoke(null);
            }
            System.out.println("PASS album replacement, deduplication, retries, disabled mode and projection invalidation");
        } finally {
            try (var paths = Files.walk(temp)) {
                for (Path path : paths.sorted(java.util.Comparator.reverseOrder()).toList()) Files.delete(path);
            }
        }
    }
}
