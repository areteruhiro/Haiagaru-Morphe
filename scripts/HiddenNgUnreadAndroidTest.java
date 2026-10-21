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
  Class<?>[] text={String.class};
  check(call("key",text,"https://egg.5ch.net/test/read.cgi/android/1792102288/21").equals(call("key",text,"https://egg.5ch.io/test/read.cgi/android/1792102288/")),"domain alias and response URL");
  check(!call("key",text,"https://egg.5ch.io/test/read.cgi/android/1792102288/").equals(call("key",text,"https://egg.5ch.io/test/read.cgi/software/1792102288/")),"board isolation");
  check(call("key",text,"file:///archive/123.dat")==null,"invalid origins ignored");
  return "PASS "+checks+" hidden-NG unread value checks (no read-position writes)";
 }
}
