import java.nio.file.*;
import javax.tools.ToolProvider;

public class VerifyXMedia {
    public static void main(String[] args) throws Exception {
        String source = Files.readString(Path.of("extensions/chmate/src/main/java/app/morphe/extension/chmate/XMediaAttachments.java"));
        String patterns = source.substring(source.indexOf("    private static final Pattern POST"),
                source.indexOf("    private XMediaAttachments"));
        String methods = patterns + source.substring(source.indexOf("    public static String[] postIds"),
                source.indexOf("    public static String[] fromJson"));
        Path temp = Files.createTempDirectory("haiagaru-x-media-");
        try {
            Path file = temp.resolve("XMediaAttachments.java");
            Files.writeString(file, "import java.net.*; import java.util.*; import java.util.regex.*; public class XMediaAttachments {" + methods + "}");
            if (ToolProvider.getSystemJavaCompiler().run(null, null, null, "-d", temp.toString(), file.toString()) != 0)
                throw new AssertionError("Compilation failed");
            try (var loader = new java.net.URLClassLoader(new java.net.URL[]{temp.toUri().toURL()})) {
                Class<?> parser = loader.loadClass("XMediaAttachments");
                var posts = parser.getMethod("postIds", String.class);
                String[] ids = (String[]) posts.invoke(null,
                        "https://x.com/TAHUBASU222/status/2107828483158392869/video/ https://x.com/TAHUBASU222/status/2107828483158392869");
                if (!java.util.Arrays.equals(ids, new String[]{"2107828483158392869"})) throw new AssertionError("Post normalization");
                String[] photos = (String[]) posts.invoke(null,
                        "https://x.com/unkochan1234567/status/863141727439278080/photo/ https://x.com/unkochan1234567/status/863141727439278080");
                if (!java.util.Arrays.equals(photos, new String[]{"863141727439278080"})) throw new AssertionError("Photo normalization");
                String[] numberedPhoto = (String[]) posts.invoke(null,
                        "https://x.com/unkochan1234567/status/2093970387596140971/photo/1 https://x.com/unkochan1234567/status/2093970387596140971/");
                if (!java.util.Arrays.equals(numberedPhoto, new String[]{"2093970387596140971"})) throw new AssertionError("Numbered photo normalization");
                for (String prefix : new String[]{"", "ttps://", "ttp://", "//", "https://", "http://"}) {
                    for (String path : new String[]{"unkochan1234567/status/2093970387596140971/photo/1", "i/status/2093970387596140971", "i/web/status/2093970387596140971"}) {
                        String url = prefix + "x.com/" + path;
                        if (!java.util.Arrays.equals((String[])posts.invoke(null,url), new String[]{"2093970387596140971"}))
                            throw new AssertionError("Incomplete post URL: " + url);
                    }
                }
                if (((String[])posts.invoke(null,"x.com/u/status/123 ttps://x.com/i/status/123")).length != 1)
                    throw new AssertionError("Incomplete URL dedup");
                for (String bad : new String[]{"https://evil.x.com/u/status/123", "evilx.com/u/status/123",
                        "https://x.com.evil/u/status/123", "ftp://x.com/u/status/123", "user@x.com/u/status/123",
                        "x.com/u/status/123abc"}) {
                    if (((String[])posts.invoke(null,bad)).length != 0) throw new AssertionError("Invalid post URL: " + bad);
                }
                if (((String[])posts.invoke(null,(Object)null)).length != 0) throw new AssertionError("null body");
                if (((String[]) posts.invoke(null, "https://evil.x.com/u/status/123")).length != 0) throw new AssertionError("Host validation");
                var videos = parser.getMethod("directVideos", String.class);
                String direct = "https://video.twimg.com/ext_tw_video/2104908394507411457/pu/vid/avc1/720x960/x6cfbDt22DoPXS7l.mp4";
                if (!java.util.Arrays.equals((String[]) videos.invoke(null, direct), new String[]{direct})) throw new AssertionError("Direct video");
                for (String prefix : new String[]{"", "ttps://", "ttp://", "//", "http://"}) {
                    if (!java.util.Arrays.equals((String[])videos.invoke(null,prefix + direct.substring(8)),new String[]{direct}))
                        throw new AssertionError("Incomplete direct video: " + prefix);
                }
                if (((String[])videos.invoke(null,"https://evil.video.twimg.com/a.mp4")).length != 0)
                    throw new AssertionError("Direct media host validation");
                if (((String[]) videos.invoke(null, "https://video.twimg.com/../../evil.txt")).length != 0) throw new AssertionError("Extension validation");
                System.out.println("X URL extraction: all checks passed");
            }
        } finally {
            try (var paths = Files.walk(temp)) {
                for (Path path : paths.sorted(java.util.Comparator.reverseOrder()).toList()) Files.delete(path);
            }
        }
    }
}
