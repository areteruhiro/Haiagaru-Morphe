import java.nio.file.*;
import javax.tools.ToolProvider;
import java.util.*;

public class VerifyNgBoardScope {
    public static void main(String[] args) throws Exception {
        Path temp = Files.createTempDirectory("haiagaru-ng-scope-");
        try {
            Path file = temp.resolve("NgBoardScope.java");
            String source = Files.readString(Path.of("extensions/chmate/src/main/java/app/morphe/extension/chmate/NgBoardScope.java"));
            Files.writeString(file, source.replace("package app.morphe.extension.chmate;", ""));
            if (ToolProvider.getSystemJavaCompiler().run(null, null, null, "-d", temp.toString(), file.toString()) != 0)
                throw new AssertionError("compile");
            try (var loader = new java.net.URLClassLoader(new java.net.URL[]{temp.toUri().toURL()})) {
                Class<?> cls = loader.loadClass("NgBoardScope");
                var match = cls.getMethod("matches", String.class, String.class);
                var encode = cls.getDeclaredMethod("encode", boolean.class, Collection.class); encode.setAccessible(true);
                String include = (String) encode.invoke(null, false, List.of("poverty", "android"));
                String exclude = (String) encode.invoke(null, true, List.of("poverty", "android"));
                for (String board : List.of("android", "poverty", "edge", "android2", "newboard")) {
                    boolean selected = List.of("android", "poverty").contains(board);
                    if (!(boolean) match.invoke(null, (Object)null, board)) throw new AssertionError("global");
                    if ((boolean) match.invoke(null, include, board) != selected) throw new AssertionError("include " + board);
                    if ((boolean) match.invoke(null, exclude, board) == selected) throw new AssertionError("exclude " + board);
                    if ((boolean) match.invoke(null, "android", board) != board.equals("android")) throw new AssertionError("legacy");
                }
                if (!(boolean) match.invoke(null, exclude, "newboard")) throw new AssertionError("future boards");
                if ((boolean) match.invoke(null, exclude, null)) throw new AssertionError("unknown board must not match");
                if ((boolean) match.invoke(null, "@haiagaru:boards:out:", "android")) throw new AssertionError("empty exclusion");
                if (!exclude.equals(encode.invoke(null, true, List.of("android", "poverty", "android")))) throw new AssertionError("stable key");
                for (List<String> invalid : List.of(List.<String>of(), List.of("https://example/board"), List.of("a,b"), List.of("a\nb"))) {
                    try { encode.invoke(null, true, invalid); throw new AssertionError("invalid accepted"); }
                    catch (java.lang.reflect.InvocationTargetException expected) {
                        if (!(expected.getCause() instanceof IllegalArgumentException)) throw expected;
                    }
                }
            }
            System.out.println("PASS: global, legacy, include, exclude, future boards and invalid scope checks");
        } finally {
            try (var paths = Files.walk(temp)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.delete(path);
            }
        }
    }
}
