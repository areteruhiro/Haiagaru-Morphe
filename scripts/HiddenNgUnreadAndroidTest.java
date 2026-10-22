package app.morphe.extension.chmate;
import android.content.Context;
import java.lang.reflect.*;
import java.util.*;
public class HiddenNgUnreadAndroidTest {
 static int checks;
 static void check(boolean ok,String label){if(!ok)throw new AssertionError(label);checks++;}
 static Object call(String name,Class<?>[] args,Object... values)throws Exception{Method m=HiddenNgUnread.class.getDeclaredMethod(name,args);m.setAccessible(true);return m.invoke(null,values);}
 public static String run(Context c)throws Exception{
  checks=0;
  Set<Integer> hidden=new HashSet<>(Arrays.asList(1,10,11,12,99));
  check((Integer)call("subtract",new Class[]{int.class,int.class,Set.class},12,9,hidden)==9,"three unread hidden");
  check((Integer)call("subtract",new Class[]{int.class,int.class,Set.class},12,10,hidden)==10,"only unread hidden excluded");
  check((Integer)call("subtract",new Class[]{int.class,int.class,Set.class},12,15,hidden)==12,"never increases total");
  check((Integer)call("subtract",new Class[]{int.class,int.class,Set.class},12,9,Collections.emptySet())==12,"unknown remains unread");
  Method unread=HiddenNgUnread.class.getDeclaredMethod("unreadHidden",String.class,int.class,int.class,Set.class);unread.setAccessible(true);
  Method invalidate=HiddenNgUnread.class.getDeclaredMethod("invalidate",String.class);invalidate.setAccessible(true);
  String cacheKey="synthetic-board/thread";Set<Integer> cachedRows=new HashSet<>(Arrays.asList(1,10,11,12,99));
  check((Integer)unread.invoke(null,cacheKey,9,12,cachedRows)==3,"cached count includes only hidden unread rows up to total");
  check((Integer)unread.invoke(null,cacheKey,10,12,cachedRows)==2,"cached count honors updated read position");
  cachedRows.add(13);invalidate.invoke(null,cacheKey);
  check((Integer)unread.invoke(null,cacheKey,9,13,cachedRows)==4,"membership change invalidates sorted unread cache");
  Class<?>[] text={String.class};
  check(call("key",text,"https://egg.5ch.net/test/read.cgi/android/1792102288/21").equals(call("key",text,"https://egg.5ch.io/test/read.cgi/android/1792102288/")),"domain alias and response URL");
  check(!call("key",text,"https://egg.5ch.io/test/read.cgi/android/1792102288/").equals(call("key",text,"https://egg.5ch.io/test/read.cgi/software/1792102288/")),"board isolation");
  check(call("key",text,"file:///archive/123.dat")==null,"invalid origins ignored");
  return "PASS "+checks+" hidden-NG unread value checks (no read-position writes)";
 }
}
