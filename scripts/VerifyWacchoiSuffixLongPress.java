import java.nio.file.*;
import javax.tools.ToolProvider;

/** JVM regression for the Compose bridge: native callbacks unchanged, our long press consumed. */
public class VerifyWacchoiSuffixLongPress {
    public static void main(String[] args) throws Exception {
        String source = Files.readString(Path.of("extensions/chmate/src/main/java/app/morphe/extension/chmate/WacchoiLongPressMenu.java"));
        String helper = source.substring(source.indexOf("    public static Object suffixCombinedClickable"),
                source.indexOf("    private WacchoiLongPressMenu"))
                .replace("android.util.Log.w", "Log.w").replace("Modifier.isStatic", "java.lang.reflect.Modifier.isStatic");
        Path temp = Files.createTempDirectory("suffix-long-press-");
        try {
            Path file = temp.resolve("Bridge.java");
            Files.writeString(file, "import java.lang.reflect.*; import java.util.*; public class Bridge {"
                + "static Map<Object,Runnable> SUFFIX_LONG_PRESS=new IdentityHashMap<>(); static String combinedClickableOwner=\"Bridge$Clickable\";"
                + helper + """
                interface Fn { Object invoke(); }
                static class Item { final Fn callback; Item(Fn f){callback=f;} }
                static class Wrapped implements Fn { final Item item; Wrapped(Fn f){item=new Item(f);} public Object invoke(){return item.callback.invoke();} }
                static class Log { static void w(String a,String b,Exception e){throw new AssertionError(e);} }
                static class Modifier {}
                static class Role {}
                public static class Clickable {
                    static Fn longClick, click; static boolean enabled, haptic;
                    private static Modifier combine(Modifier m,boolean e,String label,Role r,String longLabel,Fn l,Fn d,boolean h,Object interaction,Fn c){
                        enabled=e; haptic=h; longClick=l; click=c;
                        if(d!=null || interaction!=null || !longLabel.contains("NG編集")) throw new AssertionError("arguments");
                        return m;
                    }
                }
                public static class Clickable241 {
                    static Fn longClick, click;
                    private static Modifier combine(Modifier m,boolean enabled,Fn l,boolean haptic,Object source,Fn c,int mask){
                        if(!enabled || !haptic || source!=null || mask!=0) throw new AssertionError("241 arguments");
                        longClick=l;click=c;return m;
                    }
                }
                public static void verify() {
                    int[] counts={0,0};
                    Fn click=(Fn)Proxy.newProxyInstance(Fn.class.getClassLoader(),new Class<?>[]{Fn.class},(p,m,a)->{counts[0]++;return null;});
                    Modifier modifier=new Modifier();
                    if(suffixCombinedClickable(modifier,false,null,null,click,7)!=null) throw new AssertionError("unrelated callback intercepted");
                    SUFFIX_LONG_PRESS.put(click,()->counts[1]++);
                    if(suffixCombinedClickable(modifier,false,null,null,click,7)!=modifier || !Clickable.enabled || !Clickable.haptic) throw new AssertionError("default masks");
                    Clickable.longClick.invoke();
                    if(counts[0]!=0 || counts[1]!=1) throw new AssertionError("long press also clicked");
                    Clickable.click.invoke();
                    if(counts[0]!=1 || counts[1]!=1) throw new AssertionError("click changed");
                    suffixCombinedClickable(modifier,false,null,null,click,0);
                    if(Clickable.enabled) throw new AssertionError("explicitly disabled changed");
                    if(suffixCombinedClickable5(modifier,false,null,click,3)!=modifier || !Clickable.enabled) throw new AssertionError("226 five-argument bridge");
                    combinedClickableOwner="Bridge$Clickable241";
                    if(suffixCombinedClickable(modifier,false,null,null,click,7)!=modifier) throw new AssertionError("241 bridge");
                    Clickable241.longClick.invoke();
                    if(counts[0]!=1 || counts[1]!=2) throw new AssertionError("241 long press");
                    combinedClickableOwner="Bridge$Clickable";
                    Fn wrapped=new Wrapped(click);
                    if(suffixCombinedClickable(modifier,true,null,null,wrapped,0)!=modifier) throw new AssertionError("native dismiss wrapper not resolved");
                    Clickable.longClick.invoke();
                    if(counts[0]!=1 || counts[1]!=3) throw new AssertionError("wrapped long also clicked");
                    Clickable.click.invoke();
                    if(counts[0]!=2 || counts[1]!=3) throw new AssertionError("wrapped dismiss/click replaced");
                }
            }
            """);
            if (ToolProvider.getSystemJavaCompiler().run(null,null,null,"-d",temp.toString(),file.toString())!=0) throw new AssertionError("compile");
            try(var loader=new java.net.URLClassLoader(new java.net.URL[]{temp.toUri().toURL()})){
                loader.loadClass("Bridge").getMethod("verify").invoke(null);
            }
            System.out.println("PASS suffix Compose click/long press and native fallback");
        } finally { try(var paths=Files.walk(temp)){for(Path p:paths.sorted(java.util.Comparator.reverseOrder()).toList())Files.delete(p);} }
    }
}
