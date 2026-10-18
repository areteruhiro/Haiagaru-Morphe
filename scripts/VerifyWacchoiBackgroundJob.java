package app.morphe.extension.chmate;
import android.content.*;
import java.io.*;
import java.lang.reflect.*;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.*;

/** Runs the real worker on Android using cached HTML, without sending HTTP requests. */
public class VerifyWacchoiBackgroundJob {
 public static void main(String[] args)throws Exception {
  File root=new File(args[0]);if(!root.isDirectory()&&!root.mkdirs())throw new IOException("fixture directory");
  Context context=new ContextWrapper(null){public Context getApplicationContext(){return this;}public File getCacheDir(){return root;}public File getFilesDir(){return root;}
    public SharedPreferences getSharedPreferences(String name,int mode){return (SharedPreferences)Proxy.newProxyInstance(SharedPreferences.class.getClassLoader(),new Class[]{SharedPreferences.class},(p,m,a)->m.getName().equals("getBoolean")||m.getName().equals("getLong")?a[1]:null);}
  };
  if(args.length>1){if(WacchoiHistory.read(context).isEmpty())throw new AssertionError("history after process restart");System.out.println("PASS: persisted history readable in new Android process");System.exit(0);}
  CountDownLatch finished=new CountDownLatch(1);
  WacchoiBackgroundJob[] holder=new WacchoiBackgroundJob[1];
  WacchoiBackgroundJob job=new WacchoiBackgroundJob(context,()->{if(holder[0]!=null&&!holder[0].running)finished.countDown();});holder[0]=job;
  Method cache=WacchoiBackgroundJob.class.getDeclaredMethod("cacheFile",String.class);cache.setAccessible(true);
  String base="http://hissi.org/read.php/android/20261016/",profile=base+"test.html";
  seed((File)cache.invoke(job,base),"<table><tr><td><a href='test.html'>ID</a> (ｽｯﾌﾟ Sdff-Q8ld)</td></tr></table>");
  seed((File)cache.invoke(job,profile),"<dl><dt><a href='https://egg.5ch.io/test/read.cgi/android/1792102288/10'>Fixture title</a><dd>Fixture body</dl>");
  job.start(new Intent().putExtra("boards","android").putExtra("from","20261016").putExtra("to","20261016")
    .putExtra("four","Q8ld").putExtra("upper",false).putExtra("limit",1));
  if(!finished.await(10,TimeUnit.SECONDS))throw new AssertionError("worker must finish without Activity");
  List<WacchoiBackgroundJob.Record> rows=job.records();
  if(rows.size()!=2 || rows.get(0).kind!=0 || rows.get(1).kind!=1 || !rows.get(1).b.equals("Fixture title") || !rows.get(1).c.equals("Fixture body"))throw new AssertionError("retained results: "+rows.size()+" "+job.status);
  job.setListener(()->{});
  if(job.records().size()!=2)throw new AssertionError("detach must retain results");
  job.release();
  List<org.json.JSONObject> saved=Collections.emptyList();
  for(int i=0;i<50;i++){saved=WacchoiHistory.read(context);if(!saved.isEmpty()&&saved.get(0).optJSONArray("results")!=null&&saved.get(0).optJSONArray("results").length()==2)break;Thread.sleep(100);}
  if(saved.isEmpty()||saved.get(0).getJSONArray("results").length()!=2||!saved.get(0).getString("four").equals("Q8ld"))throw new AssertionError("saved query/results");
  System.out.println("PASS: Activity-free worker, cached Hissi match, title/body retained, listener detach, completion, saved query/results");
  System.exit(0);
 }
 static void seed(File file,String html)throws Exception{try(OutputStream out=new FileOutputStream(file)){out.write(html.getBytes(Charset.forName("windows-31j")));}}
}
