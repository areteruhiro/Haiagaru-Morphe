import java.nio.file.*;
import java.net.*;
import javax.tools.ToolProvider;
import org.json.*;

/** Exercises the actual public-embed parser; optional live test does not download media. */
public class VerifyInstagram {
    public static void main(String[] args) throws Exception {
        Path temp = Files.createTempDirectory("haiagaru-instagram-test-");
        try {
            Path pkg = Files.createDirectories(temp.resolve("app/morphe/extension/chmate"));
            Files.copy(Path.of("extensions/chmate/src/main/java/app/morphe/extension/chmate/InstagramMediaAttachments.java"), pkg.resolve("InstagramMediaAttachments.java"));
            Files.writeString(pkg.resolve("XMediaAttachments.java"), "package app.morphe.extension.chmate; public class XMediaAttachments { public static void setGeneratedPoster(String v,String p) {} }");
            String jar = Path.of("build/verification/tools/json.jar").toAbsolutePath().toString();
            if (ToolProvider.getSystemJavaCompiler().run(null,null,null,"-cp",jar,"-d",temp.toString(),pkg.resolve("InstagramMediaAttachments.java").toString(),pkg.resolve("XMediaAttachments.java").toString()) != 0) throw new AssertionError("compile");
            try (var loader = new URLClassLoader(new URL[]{temp.toUri().toURL(),Path.of(jar).toUri().toURL()})) {
                Class<?> parser = loader.loadClass("app.morphe.extension.chmate.InstagramMediaAttachments");
                var parse = parser.getMethod("fromHtml",String.class,String.class);
                JSONObject image = new JSONObject().put("display_url","https://s.cdninstagram.com/a.jpg");
                JSONObject video = new JSONObject().put("display_url","https://s.fbcdn.net/b.jpg").put("is_video",true).put("video_url","https://s.fbcdn.net/b.mp4");
                JSONObject post = new JSONObject().put("shortcode","Test").put("edge_sidecar_to_children",new JSONObject().put("edges",new JSONArray().put(new JSONObject().put("node",image)).put(new JSONObject().put("node",video))));
                String root = new JSONObject().put("gql_data",new JSONObject().put("shortcode_media",post)).toString();
                String html = new JSONObject().put("contextJSON",root).toString() + "<img src='https://s.fbcdn.net/profile.jpg'>";
                String[] result = (String[]) parse.invoke(null,html,"Test");
                if (result.length != 3) throw new AssertionError("mixed sidecar " + java.util.Arrays.toString(result));
                // A mismatched embed must not contribute its media.
                if (((String[]) parse.invoke(null,html,"Other")).length != 0) throw new AssertionError("shortcode mismatch");
                var urls = parser.getMethod("postIds",String.class);
                if (((String[])urls.invoke(null,"https://www.instagram.com/p/Test/ https://instagram.com/reel/Test/")).length != 1) throw new AssertionError("dedup");
                for (String prefix : new String[]{"", "ttps://", "ttp://", "//", "http://", "https://"}) {
                    String url = prefix + "www.instagram.com/p/DeLi00MEntO/";
                    if (!java.util.Arrays.equals((String[])urls.invoke(null,url),new String[]{"DeLi00MEntO"}))
                        throw new AssertionError("Incomplete Instagram URL: " + url);
                }
                for (String bad : new String[]{"https://evil.instagram.com/p/Test/", "evilinstagram.com/p/Test/",
                        "https://instagram.com.evil/p/Test/", "ftp://instagram.com/p/Test/", "user@instagram.com/p/Test/"}) {
                    if (((String[])urls.invoke(null,bad)).length != 0) throw new AssertionError("Invalid Instagram URL: " + bad);
                }
                if (((String[])urls.invoke(null,"www.instagram.com/p/Test/ ttps://www.instagram.com/p/Test/")).length != 1)
                    throw new AssertionError("Incomplete Instagram dedup");
                if ((boolean)parser.getMethod("validMedia",String.class,boolean.class).invoke(null,"https://s.fbcdn.net.evil/a.jpg",false)) throw new AssertionError("unsafe host");
                if (args.length > 0) {
                    var request = java.net.http.HttpRequest.newBuilder(URI.create("https://www.instagram.com/p/DeLi00MEntO/embed/")).header("User-Agent","Mozilla/5.0").timeout(java.time.Duration.ofSeconds(30)).GET().build();
                    var response = java.net.http.HttpClient.newBuilder().followRedirects(java.net.http.HttpClient.Redirect.NORMAL).build().send(request,java.net.http.HttpResponse.BodyHandlers.ofString());
                    String[] live = (String[])parse.invoke(null,response.body(),"DeLi00MEntO");
                    if (response.statusCode() != 200 || live.length != 10) throw new AssertionError("live post: HTTP " + response.statusCode() + ", media " + live.length);
                    System.out.println("Example Instagram post: 10 images");
                }
                System.out.println("Instagram parser: all checks passed");
            }
        } finally {
            try (var paths = Files.walk(temp)) {
                for (Path p : paths.sorted(java.util.Comparator.reverseOrder()).toList()) Files.delete(p);
            }
        }
    }
}
