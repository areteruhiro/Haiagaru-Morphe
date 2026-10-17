import java.nio.file.*;
import java.net.*;
import java.util.*;
import javax.tools.ToolProvider;

/** Executes the original 226 collector with a synchronous fake ViewPager callback.
 * This is a reentrancy regression test, not a physical gesture/UI reproduction.
 * Argument: original 226 dex2jar JAR. Binary wrapper wiring is checked separately.
 */
public final class VerifyTabletTabSelection226 {
    static int checks;
    static void check(boolean ok, String label) { if (!ok) throw new AssertionError(label); checks++; }
    public static void main(String[] args) throws Exception {
        Path root = Files.createTempDirectory("haiagaru-tablet226-");
        try {
            Map<String,String> sources = new LinkedHashMap<>();
            sources.put("o/bea12", "package o; public interface bea12 {}");
            sources.put("o/zzao", "package o; public interface zzao { Object emit(Object a, bea12 b); }");
            sources.put("o/Ff", "package o; public class Ff { public int a; public java.util.List b; }");
            sources.put("o/Ff11", "package o; public class Ff11 { public static Ff11 c=new Ff11(); }");
            sources.put("o/fsYhp", "package o; public class fsYhp { public static void c(Object a,String b){} }");
            sources.put("o/isServerAuthCodeRequested", "package o; public class isServerAuthCodeRequested { public static write e=new write(); public static class read { public void b(String s,Object[] args){} } public static class write extends read {} }");
            sources.put("o/Kjv", "package o; public class Kjv {}");
            sources.put("androidx/viewpager/widget/PagerAdapter", "package androidx.viewpager.widget; public class PagerAdapter { public Runnable callback; public void notifyDataSetChanged(){ callback.run(); } }");
            sources.put("androidx/viewpager/widget/ViewPager", "package androidx.viewpager.widget; public class ViewPager { public int current; public Runnable callback; public void setCurrentItem(int n,boolean smooth){ current=n; callback.run(); } }");
            sources.put("android/widget/BaseAdapter", "package android.widget; public class BaseAdapter { public void notifyDataSetChanged(){} }");
            sources.put("o/Yy", "package o; public class Yy extends androidx.viewpager.widget.PagerAdapter { public java.util.List a; }");
            sources.put("o/bea", "package o; public class bea extends android.widget.BaseAdapter {}");
            sources.put("o/initAPM", "package o; public class initAPM { public void setSelection(Object x){} }");
            sources.put("o/closeMultiWebViewFileLock", "package o; public class closeMultiWebViewFileLock extends initAPM {}");
            sources.put("o/TTVideoLandingPageLink2Activity9", "package o; public class TTVideoLandingPageLink2Activity9 extends androidx.viewpager.widget.ViewPager {}");
            sources.put("o/QWA", """
                package o; public class QWA {
                    public int d,modelSelection; public boolean i,fail;
                    public Yy f=new Yy(); public bea c=new bea();
                    public TTVideoLandingPageLink2Activity9 b=new TTVideoLandingPageLink2Activity9();
                    public closeMultiWebViewFileLock e=new closeMultiWebViewFileLock();
                    public QWA(){ Runnable callback=()->{ if(fail)throw new IllegalStateException("fixture"); if(!i)modelSelection=b.current; }; f.callback=callback; b.callback=callback; }
                    public static Yy d(QWA p){return p.f;} public static bea a(QWA p){return p.c;}
                    public static void d(QWA p,boolean value){p.i=value;} public static boolean h(QWA p){return p.i;}
                }
                """);
            List<String> compilerArgs = new ArrayList<>(List.of("-d",root.toString()));
            for(var source:sources.entrySet()) {
                Path path=root.resolve(source.getKey()+".java"); Files.createDirectories(path.getParent());
                Files.writeString(path,source.getValue()); compilerArgs.add(path.toString());
            }
            check(ToolProvider.getSystemJavaCompiler().run(null,null,null,compilerArgs.toArray(String[]::new))==0,"fixture compilation");
            try(var loader=new URLClassLoader(new URL[]{root.toUri().toURL(),Path.of(args[0]).toUri().toURL()},ClassLoader.getPlatformClassLoader())) {
                Class<?> q=loader.loadClass("o.QWA"), state=loader.loadClass("o.Ff");
                Class<?> collector=loader.loadClass("o.QWA$5$5$5");
                var ctor=collector.getDeclaredConstructor(q); ctor.setAccessible(true);
                var emit=collector.getDeclaredMethod("emit",Object.class,loader.loadClass("o.bea12")); emit.setAccessible(true);
                for(int target:new int[]{-1,0,1,2}) for(boolean guard:new boolean[]{false,true}) {
                    Object pane=q.getConstructor().newInstance(), ff=state.getConstructor().newInstance();
                    int stale=target==0?1:0;
                    q.getField("modelSelection").setInt(pane,target);
                    Object pager=q.getField("b").get(pane); pager.getClass().getField("current").setInt(pager,stale);
                    state.getField("a").setInt(ff,target);
                    List<Object> pages=new ArrayList<>(); for(int i=0;i<3;i++)pages.add(loader.loadClass("o.Kjv").getConstructor().newInstance());
                    state.getField("b").set(ff,pages);
                    boolean old=q.getField("i").getBoolean(pane);
                    if(guard)q.getField("i").setBoolean(pane,true);
                    try { emit.invoke(ctor.newInstance(pane),ff,null); }
                    finally { if(guard)q.getField("i").setBoolean(pane,old); }
                    check(q.getField("modelSelection").getInt(pane)==(guard?target:stale),"original callback versus guarded selection target="+target);
                    check(!q.getField("i").getBoolean(pane),"guard restored target="+target);
                }
                for(boolean previous:new boolean[]{false,true}) {
                    Object pane=q.getConstructor().newInstance(), ff=state.getConstructor().newInstance();
                    state.getField("a").setInt(ff,-1); state.getField("b").set(ff,List.of());
                    q.getField("i").setBoolean(pane,true); q.getField("fail").setBoolean(pane,true);
                    boolean failed=false;
                    try { emit.invoke(ctor.newInstance(pane),ff,null); }
                    catch(java.lang.reflect.InvocationTargetException expected){failed=true;}
                    finally { q.getField("i").setBoolean(pane,previous); }
                    check(failed && q.getField("i").getBoolean(pane)==previous,"exception restores prior guard="+previous);
                }
            }
            System.out.println("PASS "+checks+" native 226 collector reentrancy checks (simulated pager)");
        } finally {
            try(var paths=Files.walk(root)){for(Path p:paths.sorted(Comparator.reverseOrder()).toList())Files.delete(p);}
        }
    }
}
