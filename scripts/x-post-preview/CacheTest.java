package app.morphe.extension.chmate;

import android.content.Context;
import android.content.ContextWrapper;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

/** Runs real disk/metadata code in an isolated class loader. Never downloads posts. */
public final class CacheTest {
    static int checks;
    static void check(boolean ok,String label) { if(!ok)throw new AssertionError(label);checks++; }
    static String json(String id) { return "{\"id_str\":\""+id+"\",\"text\":\"キャッシュ全文 + / &\\n本文\",\"user\":{\"name\":\"検証\",\"screen_name\":\"fixture\"}}"; }
    public static String run(Context app) throws Exception {
        try { return runChecks(app); }
        catch(Throwable error) { return "FAIL " + android.util.Log.getStackTraceString(error); }
    }
    private static String runChecks(Context app) throws Exception {
        checks=0; File root=new File(app.getCacheDir(),"x-cache-fixture-"+UUID.randomUUID());
        File dir=new File(root,"haiagaru-x-posts"); long now=System.currentTimeMillis();
        XPostDiskCache cache=new XPostDiskCache(dir);
        try {
            check(cache.read("123",now)==null,"cold miss");
            cache.write("123",json("123"),now);
            check(new org.json.JSONObject(cache.read("123",now)).getString("text").equals("キャッシュ全文 + / &\n本文"),"exact Unicode/body");
            check(new XPostDiskCache(dir).read("123",now+1000)!=null,"new cache instance reads persisted data");
            check(cache.read("123",now+XPostDiskCache.MAX_AGE-1)!=null,"valid for seven days");
            check(cache.read("123",now+XPostDiskCache.MAX_AGE)==null,"expires at seven days without sliding");
            check(!new File(dir,"123.json").exists(),"expired entry removed");
            cache.write("123",json("999"),now);check(cache.read("123",now)==null,"mismatched post not cached");
            cache.write("123",null,now);check(cache.read("123",now)==null,"failed result not cached");
            cache.write("123",json("123"),now+400000);check(cache.read("123",now)==null,"future clock rejected");
            try(FileOutputStream stream=new FileOutputStream(new File(dir,"123.json"))) {stream.write("corrupt".getBytes(StandardCharsets.UTF_8));}
            check(cache.read("123",now)==null,"corrupt entry ignored");
            try {cache.read("../bad",now);throw new AssertionError("unsafe ID");}catch(IllegalArgumentException expected){checks++;}
            for(int i=1;i<=130;i++)cache.write(Integer.toString(i),json(Integer.toString(i)),now+i);
            check(dir.listFiles(f->f.getName().endsWith(".json")).length==128,"entry cap");
            check(cache.read("1",now)==null&&cache.read("130",now)!=null,"oldest pruned");
            cache.clear();
            char[] chars=new char[480000];Arrays.fill(chars,'a');String large=new String(chars);
            for(int i=1;i<=38;i++)cache.write(Integer.toString(i),"{\"id_str\":\""+i+"\",\"text\":\""+large+"\"}",now+i);
            long bytes=0;for(File f:dir.listFiles())bytes+=f.length();
            check(bytes<=XPostDiskCache.MAX_BYTES,"16MiB total cap");
            check(cache.read("38",now)!=null&&cache.read("1",now)==null,"size prune keeps newer entries");
            cache.clear(); check(dir.listFiles().length==0,"manual clear");
            Haiagaru.context=new ContextWrapper(app){@Override public File getCacheDir(){return root;}};
            cache.write("123",json("123"),now);
            String loaded=XPostMetadata.fetchPreview("123");check(loaded!=null,"fetch uses disk before network");
            java.lang.reflect.Field field=XPostMetadata.class.getDeclaredField("CACHE");field.setAccessible(true);
            ((Map<?,?>)field.get(null)).clear();
            check(XPostMetadata.fetchPreview("123").equals(loaded),"memory reset still reads durable cache");
            long stamp=new File(dir,"123.json").lastModified();
            check(stamp==now,"cache hit never extends disk retention");
            ((Map<?,?>)field.get(null)).clear();
            ExecutorService pool=Executors.newFixedThreadPool(8);
            try {
                ArrayList<Future<String>> tasks=new ArrayList<>();
                for(int i=0;i<8;i++)tasks.add(pool.submit(()->XPostMetadata.fetchPreview("123")));
                for(Future<String> task:tasks)check(loaded.equals(task.get(10,TimeUnit.SECONDS)),"concurrent readers");
            }finally{pool.shutdownNow();}
            check(XPostMetadata.fetch("../bad")==null,"fetch invalid ID no network");
            XPostMetadata.clear();check(cache.read("123",now)==null&&((Map<?,?>)field.get(null)).isEmpty(),"clear disk and memory");
            return "PASS "+checks+" X preview cache checks; no network requests";
        }finally { cache.clear(); dir.delete();root.delete();Haiagaru.context=null; }
    }
}
