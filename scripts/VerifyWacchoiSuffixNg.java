import java.nio.file.*;
import javax.tools.ToolProvider;
public class VerifyWacchoiSuffixNg {
    public static void main(String[] args) throws Exception {
        String source = Files.readString(Path.of("extensions/chmate/src/main/java/app/morphe/extension/chmate/WacchoiSuffixNg.java"));
        String methods = source.substring(source.indexOf("    public static String suffix"), source.indexOf("    public static void confirm"));
        Path temp = Files.createTempDirectory("haiagaru-ng-suffix-");
        try {
            Path file = temp.resolve("Suffix.java");
            Files.writeString(file,"import java.util.regex.*; public class Suffix {" + methods + "}");
            if (ToolProvider.getSystemJavaCompiler().run(null,null,null,"-d",temp.toString(),file.toString()) != 0) throw new AssertionError("compile");
            try (var loader = new java.net.URLClassLoader(new java.net.URL[]{temp.toUri().toURL()})) {
                Class<?> type = loader.loadClass("Suffix");
                var extract = type.getMethod("suffix",String.class);
                String suffix = (String) extract.invoke(null,"名無しさん (ﾜｯﾁｮｲ 8f01-uDul)");
                if (!"uDul".equals(suffix)) throw new AssertionError("suffix");
                for (String name : new String[]{"(ｽｯﾌﾟ Sd03-uDul)", "(ｽﾌﾟﾌﾟ Sdaa-uDul)",
                        "(ｻｻｸｯﾃﾛﾗ Sp01-uDul)", "(ｵｯﾍﾟｹ Sr01-uDul)", "(ﾜｯﾁｮｲW 8f01-uDul)"}) {
                    if (!suffix.equals(extract.invoke(null, name))) throw new AssertionError("carrier suffix: " + name);
                }
                var regex = java.util.regex.Pattern.compile((String)type.getMethod("expression",String.class).invoke(null,suffix));
                if (!regex.matcher("(ﾜｯﾁｮｲ abcd-uDul)").find() || regex.matcher("(ﾜｯﾁｮｲ abcd-uDux)").find()
                        || regex.matcher("abcd-uDulExtra").find()) throw new AssertionError("suffix matching");
                if (java.util.regex.Pattern.compile(regex.pattern(), java.util.regex.Pattern.CASE_INSENSITIVE)
                        .matcher("abcd-udul").find()) throw new AssertionError("case-sensitive identity");
                if (!regex.matcher("(ｽｯﾌﾟ Sd03-uDul)").find()
                        || !regex.matcher("(ｻｻｸｯﾃﾛﾗ Sp01-uDul)").find()
                        || regex.matcher("Sd03-udul").find()
                        || regex.matcher("XSd03-uDul").find()) throw new AssertionError("carrier matching");
                String symbolic = (String)extract.invoke(null,"abcd-a+/-");
                if (!"a+/-".equals(symbolic)) throw new AssertionError("symbol suffix");
                if (!java.util.regex.Pattern.compile((String)type.getMethod("expression",String.class).invoke(null,symbolic)).matcher("ffff-a+/-").find()) throw new AssertionError("quoted symbols");
                System.out.println("Wacchoi suffix NG: all checks passed");
            }
        } finally {
            try (var paths = Files.walk(temp)) {
                for (Path p : paths.sorted(java.util.Comparator.reverseOrder()).toList()) Files.delete(p);
            }
        }
    }
}
