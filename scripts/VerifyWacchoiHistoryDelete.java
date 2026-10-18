package app.morphe.extension.chmate;
import android.content.*;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;
public final class VerifyWacchoiHistoryDelete {
 static int count;static void check(boolean ok,String label){if(!ok)throw new AssertionError(label);count++;System.out.println("PASS "+label);}
 static void flush()throws Exception{Field f=WacchoiHistory.class.getDeclaredField("IO");f.setAccessible(true);((ExecutorService)f.get(null)).submit(()->{}).get(20,TimeUnit.SECONDS);}
 public static void main(String[] args)throws Exception{
  android.os.Looper.prepareMainLooper();
  File root=new File("/data/local/tmp/haiagaru-history-delete-"+UUID.randomUUID());root.mkdirs();
  Context c=new ContextWrapper(null){public Context getApplicationContext(){return this;}public File getFilesDir(){return root;}public SharedPreferences getSharedPreferences(String n,int m){return (SharedPreferences)Proxy.newProxyInstance(SharedPreferences.class.getClassLoader(),new Class[]{SharedPreferences.class},(p,method,a)->{if(method.getName().startsWith("get"))return a[1];throw new UnsupportedOperationException();});}};
  WacchoiHistory.Session lower=WacchoiHistory.begin(c,new Intent().putExtra("four","ABCD"));
  WacchoiHistory.Session upper=WacchoiHistory.begin(c,new Intent().putExtra("four","1234").putExtra("upper",true));flush();
  check(WacchoiHistory.read(c).size()==2,"upper and lower histories saved");
  check(WacchoiHistory.deleteNow(c,Collections.singleton(lower.data.getString("id")),false)==0,"selected lower deleted");
  check(WacchoiHistory.read(c).size()==1&&WacchoiHistory.read(c).get(0).optBoolean("upper"),"unselected upper retained");
  lower.status("late write");flush();check(WacchoiHistory.read(c).size()==1,"deleted active session cannot reappear");
  upper.status("still active");flush();check(WacchoiHistory.read(c).get(0).optString("status").equals("still active"),"unselected active session still saves");
  File pages=new File(root,"hissi-public-pages");pages.mkdirs();File page=new File(pages,"a".repeat(64)+".html");try(FileOutputStream out=new FileOutputStream(page)){out.write(1);}
  check(WacchoiHistory.deleteNow(c,Collections.emptySet(),true)==0,"all deletion succeeds");
  upper.status("after clear");flush();check(WacchoiHistory.read(c).isEmpty(),"all deletion invalidates pending session");check(page.isFile(),"page cache preserved");
  WacchoiHistory.Session next=WacchoiHistory.begin(c,new Intent().putExtra("four","NEW1"));flush();check(WacchoiHistory.read(c).size()==1,"new searches can save after clear");
  WacchoiHistory.deleteNow(c,Collections.emptySet(),true);page.delete();pages.delete();WacchoiHistory.directory(c).delete();root.delete();
  System.out.println("PASS "+count+" history deletion checks");System.exit(0);
 }
}
