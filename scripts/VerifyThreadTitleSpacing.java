import java.nio.file.*;
import javax.tools.ToolProvider;

/** Run with java scripts/VerifyThreadTitleSpacing.java from the repository root. */
public class VerifyThreadTitleSpacing {
    public static void main(String[] args) throws Exception {
        Path temp = Files.createTempDirectory("haiagaru-title-spacing-");
        try {
            Path widget = temp.resolve("android/widget/TextView.java");
            Path extension = temp.resolve("app/morphe/extension/chmate");
            Files.createDirectories(widget.getParent());
            Files.createDirectories(extension);
            Files.writeString(widget, """
                package android.widget;
                public class TextView {
                    public float multiplier=1, extra=2; public boolean pad=false; public int writes;
                    public float getLineSpacingMultiplier(){return multiplier;}
                    public float getLineSpacingExtra(){return extra;}
                    public boolean getIncludeFontPadding(){return pad;}
                    public void setLineSpacing(float e,float m){extra=e;multiplier=m;writes++;}
                    public void setIncludeFontPadding(boolean p){pad=p;writes++;}
                }
                """);
            Path config = extension.resolve("Haiagaru.java");
            Files.writeString(config, "package app.morphe.extension.chmate; public class Haiagaru { public static int percent=100; public static int threadTitleSpacingPercent(){return percent;} }");
            Path actual = extension.resolve("ThreadTitleSpacing.java");
            Files.copy(Path.of("extensions/chmate/src/main/java/app/morphe/extension/chmate/ThreadTitleSpacing.java"), actual);
            Path test = temp.resolve("TestSpacing.java");
            Files.writeString(test, """
                import android.widget.TextView;
                import app.morphe.extension.chmate.*;
                public class TestSpacing {
                    public static void main(String[] args) {
                        TextView title = new TextView();
                        ThreadTitleSpacing.apply(title);
                        if(title.writes!=0) throw new AssertionError("100% must preserve defaults");
                        Haiagaru.percent=110; ThreadTitleSpacing.apply(title);
                        if(Math.abs(title.multiplier-1.1f)>0.00001 || Math.abs(title.extra-2.2f)>0.00001 || !title.pad) throw new AssertionError("110% metrics");
                        int writes=title.writes; ThreadTitleSpacing.apply(title);
                        if(title.writes!=writes) throw new AssertionError("recycled row must not compound spacing");
                        Haiagaru.percent=150; ThreadTitleSpacing.apply(title);
                        if(title.multiplier!=1.5f || title.extra!=3f) throw new AssertionError("150% metrics");
                        Haiagaru.percent=100; ThreadTitleSpacing.apply(title);
                        if(title.multiplier!=1 || title.extra!=2 || title.pad) throw new AssertionError("restore baseline");
                    }
                }
                """);
            if (ToolProvider.getSystemJavaCompiler().run(null, null, null, "-d", temp.toString(),
                    widget.toString(), config.toString(), actual.toString(), test.toString()) != 0)
                throw new AssertionError("Compilation failed");
            try (var loader = new java.net.URLClassLoader(new java.net.URL[]{temp.toUri().toURL()})) {
                loader.loadClass("TestSpacing").getMethod("main", String[].class).invoke(null, (Object)new String[0]);
            }
            System.out.println("Thread title spacing: all checks passed");
        } finally {
            try(var paths=Files.walk(temp)) {
                for(Path path:paths.sorted(java.util.Comparator.reverseOrder()).toList()) Files.delete(path);
            }
        }
    }
}
