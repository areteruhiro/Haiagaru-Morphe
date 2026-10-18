package app.morphe.extension.chmate;

import android.content.*;
import java.lang.reflect.*;
import java.util.*;

/** Synthetic settings and native-layout fixtures only; no live board/user data. */
public final class VerifyEdgeReporterScopes {
 static final Map<String,Map<String,Object>> STORE=new HashMap<>();static int checks;
 static Map<String,Object> values(String n){return STORE.computeIfAbsent(n,k->new HashMap<>());}
 static SharedPreferences prefs(String n){return (SharedPreferences)Proxy.newProxyInstance(SharedPreferences.class.getClassLoader(),new Class[]{SharedPreferences.class},(p,m,a)->{
  Map<String,Object> v=values(n);String name=m.getName();
  if(name.equals("contains"))return v.containsKey(a[0]);if(name.startsWith("get"))return v.getOrDefault(a[0],a[1]);
  if(name.equals("edit"))return Proxy.newProxyInstance(SharedPreferences.Editor.class.getClassLoader(),new Class[]{SharedPreferences.Editor.class},(e,method,args)->{
   if(method.getName().startsWith("put")){v.put((String)args[0],args[1]);return e;}if(method.getName().equals("apply"))return null;if(method.getName().equals("commit"))return true;throw new UnsupportedOperationException();
  });throw new UnsupportedOperationException(name);
 });}
 static void equal(Object expected,Object actual,String label){checks++;if(!Objects.equals(expected,actual))throw new AssertionError(label+": "+expected+" != "+actual);}
 static String display(int version,String board,String title){EdgeReporterDisplay.begin(o.NativeRows.adapter(version,board));EdgeReporterDisplay.subject(o.NativeRows.subject(version,1791989395L));String result=EdgeReporterDisplay.title(title);EdgeReporterDisplay.end();return result;}
 public static void main(String[] args)throws Exception{
  android.os.Looper.prepareMainLooper();Context context=new ContextWrapper(null){public SharedPreferences getSharedPreferences(String n,int mode){return prefs(n);}};
  Field app=Haiagaru.class.getDeclaredField("applicationContext");app.setAccessible(true);app.set(null,context);
  for(boolean old:new boolean[]{false,true}){STORE.clear();values(WacchoiSearchOptions.PREFS).put(EdgeReporterOptions.MENU,old);EdgeReporterOptions.initialize(context);equal(old,EdgeReporterOptions.board(),"old board setting migrated");values(WacchoiSearchOptions.PREFS).put(EdgeReporterOptions.MENU,!old);EdgeReporterOptions.initialize(context);equal(old,EdgeReporterOptions.board(),"migration not repeated");}
  STORE.clear();EdgeReporterHistory.initialize(context);
  String board="bbs.eddibb.cc%2Fliveedge",title="Thread",full="Thread [a.+/★]";
  for(boolean menu:new boolean[]{false,true})for(boolean list:new boolean[]{false,true}){
   values(WacchoiSearchOptions.PREFS).put(EdgeReporterOptions.MENU,menu);values(WacchoiSearchOptions.PREFS).put(EdgeReporterOptions.BOARD,list);
   equal(menu,EdgeReporterOptions.menu(),"menu preference");equal(list,EdgeReporterOptions.board(),"board preference");
   equal(menu||list,Haiagaru.rewriteSubjectUrl("https://bbs.eddibb.cc/liveedge/subject.txt").endsWith("subject-metadent.txt"),"feed needed by either scope");
   equal(menu?full:title,EdgeReporterHistory.title(board,1791989395L,full),"history writer scope");
   equal(menu?full:title,EdgeReporterHistory.historyTitle(1791989395L,board,title),"Threads history scope");
   equal(menu?full:title,EdgeReporterHistory.resolveTitle(board,title),"NG title scope");
   for(int version:new int[]{191,226,242}){
    equal(list?full:title,display(version,board,title),"board cached title "+version);
    equal(list?full:title,display(version,board,full),"board acquired title "+version);
    equal(full,display(version,"other",full),"unrelated board untouched "+version);
   }
  }
  equal("Thread",EdgeReporterDisplay.title("Thread"),"no display state leak");
  System.out.println("PASS "+checks+" reporter scope/migration checks (191/226/242)");System.exit(0);
 }
}
