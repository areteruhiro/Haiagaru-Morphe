import java.nio.file.*;
import javax.tools.ToolProvider;

/** Compiles and exercises the actual metadata parser without Android runtime dependencies. */
public class VerifyImgurAlbum {
    public static void main(String[] args) throws Exception {
        String source = Files.readString(Path.of("extensions/chmate/src/main/java/app/morphe/extension/chmate/ImgurAlbumAttachments.java"));
        String patterns = source.substring(source.indexOf("    private static final Pattern ALBUM"),
                source.indexOf("    private static final ExecutorService"));
        String parser = source.substring(source.indexOf("    public static String imageFromHtml"),
                source.indexOf("    public static String[] imagesFromJson"));
        Path temp = Files.createTempDirectory("haiagaru-imgur-test-");
        try {
            Path file = temp.resolve("ImgurParser.java");
            Files.writeString(file, "import java.util.regex.*; public class ImgurParser {" + patterns + parser + "}");
            if (ToolProvider.getSystemJavaCompiler().run(null, null, null, "-d", temp.toString(), file.toString()) != 0)
                throw new AssertionError("Parser compilation failed");
            try (var loader = new java.net.URLClassLoader(new java.net.URL[]{temp.toUri().toURL()})) {
                var method = loader.loadClass("ImgurParser").getMethod("imageFromHtml", String.class);
                var albumField = loader.loadClass("ImgurParser").getDeclaredField("ALBUM");
                albumField.setAccessible(true);
                var album = (java.util.regex.Pattern) albumField.get(null);
                for (String prefix : new String[]{"", "ttps://", "ttp://", "//", "https://", "http://"}) {
                    var match = album.matcher(prefix + "imgur.com/a/K2De8");
                    if (!match.find() || !"K2De8".equals(match.group(1))) throw new AssertionError("Incomplete album URL: " + prefix);
                }
                for (String bad : new String[]{"https://evil.imgur.com/a/K2De8", "ftp://imgur.com/a/K2De8",
                        "https://imgur.com.evil/a/K2De8", "user@imgur.com/a/K2De8"}) {
                    if (album.matcher(bad).find()) throw new AssertionError("Invalid album URL: " + bad);
                }
                String[][] cases = {
                    {"<meta property=\"og:image\" content=\"https://i.imgur.com/LWkGnaI.png?fb\">", "https://i.imgur.com/LWkGnaI.png"},
                    {"<META content='https://i.imgur.com/ABC.jpg#x' property='og:image'>", "https://i.imgur.com/ABC.jpg"},
                    {"<meta name='twitter:image' content='https://i.imgur.com/small.jpg'>", null},
                    {"<meta property='og:image' content='https://i.imgur.com.evil/ABC.png'>", null},
                    {"<meta property='og:image' content='http://i.imgur.com/ABC.png'>", null},
                    {"<html>no image</html>", null}
                };
                for (String[] test : cases) {
                    Object result = method.invoke(null, test[0]);
                    if (!java.util.Objects.equals(test[1], result)) throw new AssertionError("Unexpected: " + result);
                }
                if (args.length > 0) {
                    Object result = method.invoke(null, Files.readString(Path.of(args[0])));
                    if (!"https://i.imgur.com/LWkGnaI.png".equals(result)) throw new AssertionError("Real album: " + result);
                }
                System.out.println("Imgur parser: all checks passed");
            }
        } finally {
            try (var paths = Files.walk(temp)) {
                for (Path path : paths.sorted(java.util.Comparator.reverseOrder()).toList()) Files.delete(path);
            }
        }
    }
}
