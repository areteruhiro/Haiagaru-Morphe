import java.nio.file.*;
import java.net.URLClassLoader;
import java.util.*;
import javax.tools.ToolProvider;

public class VerifyThumbnailClickAttachments {
    public static class Thumbnail {
        private final String url;
        public Thumbnail(String url) { this.url = url; }
        public String getImageUrl() { return url; }
    }
    public static class Row {
        private final List<Thumbnail> e;
        public Row(Thumbnail... values) { e = List.of(values); }
    }
    public static void main(String[] args) throws Exception {
        Path output = Files.createTempDirectory("haiagaru-thumbnail-click-");
        int compiled = ToolProvider.getSystemJavaCompiler().run(null, null, null, "-d", output.toString(),
                "extensions/chmate/src/main/java/app/morphe/extension/chmate/ThumbnailClickAttachments.java");
        if (compiled != 0) throw new AssertionError("Compilation failed");
        try (var loader = new URLClassLoader(new java.net.URL[]{output.toUri().toURL()})) {
            var method = loader.loadClass("app.morphe.extension.chmate.ThumbnailClickAttachments")
                    .getMethod("links", Object.class, Object.class, CharSequence[].class);
            var first = new Thumbnail("https://i.imgur.com/6GuDq9K.jpeg");
            var second = new Thumbnail("https://i.imgur.com/RMMv1zJ.jpeg");
            var row = new Row(first, second);
            CharSequence[] nativeLinks = {first.getImageUrl(), second.getImageUrl()};
            if (method.invoke(null, row, second, nativeLinks) != nativeLinks) throw new AssertionError("Normal projection changed");
            CharSequence[] stale = {first.getImageUrl()};
            var corrected = (CharSequence[]) method.invoke(null, row, second, stale);
            if (!Arrays.equals(corrected, nativeLinks)) throw new AssertionError("Talk images not aligned");
            var reversed = (CharSequence[]) method.invoke(null, row, first,
                    new CharSequence[]{second.getImageUrl(), first.getImageUrl()});
            if (!Arrays.equals(reversed, nativeLinks)) throw new AssertionError("Order not aligned");
            if (method.invoke(null, row, new Thumbnail(first.getImageUrl()), stale) != stale)
                throw new AssertionError("Unbound click changed");
            var video = new Thumbnail("https://video.twimg.com/a.mp4");
            if (method.invoke(null, new Row(video), video, stale) != stale)
                throw new AssertionError("Video reinterpreted as image");
            CharSequence typed = new StringBuilder(video.getImageUrl());
            var mixed = (CharSequence[]) method.invoke(null, new Row(second, video), second,
                    new CharSequence[]{typed, second.getImageUrl()});
            if (!mixed[0].equals(second.getImageUrl()) || mixed[1] != typed)
                throw new AssertionError("Native media object discarded");
            if (method.invoke(null, row, second, (Object) null) != null)
                throw new AssertionError("Null projection changed");
            if (method.invoke(null, new Object(), second, stale) != stale)
                throw new AssertionError("Unknown row changed");
            System.out.println("PASS: Talk image alignment, ordering, normal clicks and video fallback");
        }
    }
}
