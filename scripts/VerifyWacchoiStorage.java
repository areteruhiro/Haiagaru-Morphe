package app.morphe.extension.chmate;

import android.content.*;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.zip.*;
import org.json.*;

/** Synthetic app_process fixture: no live app data, account or network access. */
public class VerifyWacchoiStorage {
    static final Map<String,Long> values=new HashMap<>();
    static int checks;
    static void check(boolean ok,String label){if(!ok)throw new AssertionError(label);checks++;System.out.println("PASS: "+label);}
    static void seed(File f,String text)throws Exception{f.getParentFile().mkdirs();try(OutputStream out=new FileOutputStream(f)){out.write(text.getBytes("UTF-8"));}}
    static void flush()throws Exception{Field f=WacchoiHistory.class.getDeclaredField("IO");f.setAccessible(true);((ExecutorService)f.get(null)).submit(()->{}).get(10,TimeUnit.SECONDS);}
    public static void main(String[] args)throws Exception{
        File root=new File(args[0],"storage-"+UUID.randomUUID());root.mkdirs();
        Context c=new ContextWrapper(null){
            public Context getApplicationContext(){return this;}
            public String getPackageName(){return "app.haiagaru.fixture";}
            public File getFilesDir(){return new File(root,"files");}
            public File getCacheDir(){File dir=new File(root,"cache");dir.mkdirs();return dir;}
            public File getExternalFilesDir(String type){return new File(root,"external");}
            public File getFileStreamPath(String name){return new File(getFilesDir(),name);}
            public SharedPreferences getSharedPreferences(String name,int mode){return (SharedPreferences)Proxy.newProxyInstance(SharedPreferences.class.getClassLoader(),new Class[]{SharedPreferences.class},(p,m,a)->{
                if(m.getName().equals("getLong"))return values.getOrDefault((String)a[0],(Long)a[1]);
                if(m.getName().equals("getBoolean"))return a[1];
                throw new UnsupportedOperationException(m.getName());
            });}
        };
        check(WacchoiStorageLimits.bound(c,0)==20&&WacchoiStorageLimits.scaled(c,5,1048576)==16L*1048576,"default count and cache capacity");
        values.put("wacchoiLimit.historyCount",0L);values.put("wacchoiLimit.cacheHours",0L);
        check(WacchoiStorageLimits.bound(c,0)==Long.MAX_VALUE,"zero means unlimited");
        values.put("wacchoiLimit.cacheMiB",Long.MAX_VALUE);
        check(WacchoiStorageLimits.scaled(c,5,1048576)==Long.MAX_VALUE,"large values saturate without overflow");
        File page=WacchoiPageCache.file(c,"a".repeat(64)+".html");seed(page,"cached page");page.setLastModified(System.currentTimeMillis()-48L*3600000);
        check(WacchoiStorageLimits.fresh(c,page),"unlimited cache reuse");values.put("wacchoiLimit.cacheHours",24L);
        check(!WacchoiStorageLimits.fresh(c,page),"configurable 24-hour expiry");
        values.put("wacchoiLimit.historyRecords",1L);
        WacchoiHistory.Session session=WacchoiHistory.begin(c,new Intent().putExtra("four","Q8ld"));
        session.add(1,"url","title","body","");session.add(1,"url2","title2","body2","");session.save();flush();
        check(WacchoiHistory.read(c).get(0).getJSONArray("results").length()==1,"configured snapshot record limit");
        values.put("wacchoiLimit.historyCount",1L);
        WacchoiHistory.Session next=WacchoiHistory.begin(c,new Intent().putExtra("four","ABCD"));next.status("complete");flush();
        check(WacchoiHistory.read(c).size()==1,"configured history count trim");
        values.put("wacchoiLimit.historyCount",0L);values.put("wacchoiLimit.historyRecords",0L);values.put("wacchoiLimit.historyCharacters",0L);values.put("wacchoiLimit.historyFileMiB",0L);
        WacchoiHistory.Session big=WacchoiHistory.begin(c,new Intent().putExtra("four","BIG1"));
        big.add(1,"url","title","x".repeat(2200000),"");big.save();flush();
        check(WacchoiHistory.read(c).stream().anyMatch(r->r.optString("four").equals("BIG1")&&r.optJSONArray("results").length()==1),"unlimited snapshot stores and reads beyond former 2MiB ceiling");
        int flags=HaiagaruSyncSnapshot.WACCHOI_HISTORY|HaiagaruSyncSnapshot.WACCHOI_CACHE;
        File zip=HaiagaruBackupArchive.capture(c,flags);
        try(ZipFile z=new ZipFile(zip)){check(z.getEntry("wacchoi-pages/"+page.getName())!=null&&z.stream().anyMatch(e->e.getName().startsWith("wacchoi-history/")),"shared MEGA/local ZIP contains histories and cached pages");}
        HaiagaruBackupArchive.Backup backup=HaiagaruBackupArchive.read(c,zip);
        seed(page,"changed");HaiagaruBackupArchive.restore(c,backup,flags,false);
        try(InputStream in=new FileInputStream(page)){check(new String(in.readAllBytes(),"UTF-8").equals("cached page"),"ZIP restore recovers cached page bytes");}
        check(System.currentTimeMillis()-page.lastModified()>24L*3600000,"restore preserves cache age");
        File only=HaiagaruBackupArchive.capture(c,HaiagaruSyncSnapshot.WACCHOI_HISTORY);
        try(ZipFile z=new ZipFile(only)){check(z.stream().noneMatch(e->e.getName().startsWith("wacchoi-pages/")),"cache category can be excluded independently");}
        System.out.println("PASS: "+checks+" storage/backup checks; fixture="+root);System.exit(0);
    }
}
