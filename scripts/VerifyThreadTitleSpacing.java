import java.nio.file.*;
import javax.tools.ToolProvider;

/** Run with java scripts/VerifyThreadTitleSpacing.java from the repository root. */
public class VerifyThreadTitleSpacing {
    public static void main(String[] args) throws Exception {
        Path temp = Files.createTempDirectory("haiagaru-title-spacing-");
        try {
            Path widget = temp.resolve("android/widget/TextView.java");
            Path paint = temp.resolve("android/graphics/Paint.java");
            Path spanned = temp.resolve("android/text/Spanned.java");
            Path spannable = temp.resolve("android/text/SpannableString.java");
            Path heightSpan = temp.resolve("android/text/style/LineHeightSpan.java");
            Path extension = temp.resolve("app/morphe/extension/chmate");
            Files.createDirectories(widget.getParent());
            Files.createDirectories(extension);
            Files.createDirectories(paint.getParent());
            Files.createDirectories(heightSpan.getParent());
            Files.writeString(paint, "package android.graphics; public class Paint { public static class FontMetricsInt { public int ascent=-30, descent=10, top=-35, bottom=12; } }");
            Files.writeString(heightSpan, "package android.text.style; public interface LineHeightSpan { void chooseHeight(CharSequence t,int s,int e,int v,int h,android.graphics.Paint.FontMetricsInt f); }");
            Files.writeString(spanned, "package android.text; public interface Spanned extends CharSequence { int SPAN_EXCLUSIVE_EXCLUSIVE=33; <T> T[] getSpans(int s,int e,Class<T> c); int getSpanStart(Object o); int getSpanEnd(Object o); }");
            Files.writeString(spannable, """
                package android.text;
                public class SpannableString implements Spanned {
                    private final String text;
                    private final java.util.Map<Object,int[]> spans=new java.util.IdentityHashMap<>();
                    public SpannableString(CharSequence t){text=t.toString();if(t instanceof Spanned p) for(Object o:p.getSpans(0,t.length(),Object.class))setSpan(o,p.getSpanStart(o),p.getSpanEnd(o),33);}
                    public String toString(){return text;} public int length(){return text.length();}
                    public char charAt(int i){return text.charAt(i);} public CharSequence subSequence(int s,int e){return text.substring(s,e);}
                    public void setSpan(Object o,int s,int e,int f){spans.put(o,new int[]{s,e});}
                    public void removeSpan(Object o){spans.remove(o);}
                    public int getSpanStart(Object o){return spans.containsKey(o)?spans.get(o)[0]:-1;}
                    public int getSpanEnd(Object o){return spans.containsKey(o)?spans.get(o)[1]:-1;}
                    @SuppressWarnings("unchecked") public <T>T[] getSpans(int s,int e,Class<T> c){
                        var list=new java.util.ArrayList<T>();for(Object o:spans.keySet())if(c.isInstance(o))list.add(c.cast(o));
                        return list.toArray((T[])java.lang.reflect.Array.newInstance(c,list.size()));}
                }
                """);
            Files.writeString(widget, """
                package android.widget;
                public class TextView {
                    public int writes; public CharSequence text="first line second line";
                    public CharSequence getText(){return text;}
                    public void setText(CharSequence t){text=t;writes++;}
                }
                """);
            Path config = extension.resolve("Haiagaru.java");
            Files.writeString(config, "package app.morphe.extension.chmate; public class Haiagaru { public static int percent=100; public static int threadTitleSpacingPercent(){return percent;} }");
            Path actual = extension.resolve("ThreadTitleSpacing.java");
            Files.copy(Path.of("extensions/chmate/src/main/java/app/morphe/extension/chmate/ThreadTitleSpacing.java"), actual);
            Path test = temp.resolve("TestSpacing.java");
            Files.writeString(test, """
                import android.widget.TextView;
                import android.text.*;
                import android.text.style.LineHeightSpan;
                import android.graphics.Paint;
                import app.morphe.extension.chmate.*;
                public class TestSpacing {
                    public static void main(String[] args) {
                        TextView title = new TextView();
                        ThreadTitleSpacing.apply(title);
                        if(title.writes!=0) throw new AssertionError("100% must preserve defaults");
                        Object highlight=new Object();
                        SpannableString text=new SpannableString(title.text);text.setSpan(highlight,0,5,33);title.text=text;
                        for(int percent:new int[]{105,110,150,200}){
                            Haiagaru.percent=percent; ThreadTitleSpacing.apply(title);
                            Spanned result=(Spanned)title.text;
                            LineHeightSpan[] spans=result.getSpans(0,result.length(),LineHeightSpan.class);
                            if(spans.length!=1 || result.getSpanStart(highlight)!=0 || result.getSpanEnd(highlight)!=5)
                                throw new AssertionError("preserve native spans with one spacing span");
                            Paint.FontMetricsInt fm=new Paint.FontMetricsInt();
                            spans[0].chooseHeight(title.text,0,10,0,0,fm);
                            if(fm.ascent!=-30 || fm.top!=-35 || fm.bottom!=12 || fm.descent!=10+Math.round(40*(percent-100)/100f))
                                throw new AssertionError("scale inter-line gap only: "+percent);
                            int adjusted=fm.descent;
                            fm.bottom=Math.max(fm.bottom,adjusted); // Android fallback propagation.
                            spans[0].chooseHeight(title.text,10,15,0,40,fm);
                            if(fm.descent!=adjusted)throw new AssertionError("framework reused metrics must not compound");
                            spans[0].chooseHeight(title.text,15,title.text.length(),0,80,fm);
                            if(fm.descent!=10)throw new AssertionError("reused metrics restored on last line");
                            fm=new Paint.FontMetricsInt();
                            spans[0].chooseHeight(title.text,10,title.text.length(),0,40,fm);
                            if(fm.descent!=10 || fm.ascent!=-30)throw new AssertionError("last/single line unchanged");
                            int writes=title.writes;ThreadTitleSpacing.apply(title);
                            if(title.writes!=writes)throw new AssertionError("recycled row must not compound spacing");
                        }
                        Haiagaru.percent=100; ThreadTitleSpacing.apply(title);
                        Spanned restored=(Spanned)title.text;
                        if(restored.getSpans(0,restored.length(),LineHeightSpan.class).length!=0 || restored.getSpanStart(highlight)!=0)
                            throw new AssertionError("100% removes only our span");
                        title.text="";ThreadTitleSpacing.apply(title);ThreadTitleSpacing.apply(null);
                    }
                }
                """);
            if (ToolProvider.getSystemJavaCompiler().run(null, null, null, "-d", temp.toString(),
                    paint.toString(), spanned.toString(), spannable.toString(), heightSpan.toString(),
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
