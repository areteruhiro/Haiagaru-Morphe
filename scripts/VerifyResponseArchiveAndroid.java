import android.content.Context;
import android.content.ContextWrapper;
import android.os.Looper;
import android.util.AtomicFile;
import java.io.File;
import java.lang.reflect.*;
import java.util.List;
import java.util.UUID;

/** Runs actual patched APK storage code under Android app_process, without launching ChMate.
 * CLASSPATH must contain this test DEX and one patched test APK.
 * All test data lives in a unique /data/local/tmp fixture, never in any app's files.
 */
public final class VerifyResponseArchiveAndroid {
    static int checks;
    static Class<?> archive;
    static void check(boolean ok, String label) { if (!ok) throw new AssertionError(label); checks++; }
    static Object call(String name, Class<?>[] types, Object... args) throws Exception {
        Method method = archive.getDeclaredMethod(name,types); method.setAccessible(true);
        try { return method.invoke(null,args); }
        catch(InvocationTargetException e) { throw (Exception)e.getCause(); }
    }
    static Object field(Object value, String name) throws Exception {
        Field f=value.getClass().getDeclaredField(name); f.setAccessible(true); return f.get(value);
    }
    static void reject(String name, Class<?>[] types, Object... args) throws Exception {
        boolean failed=false; try {call(name,types,args);}catch(Exception expected){failed=true;}
        check(failed,"invalid input rejected: "+name);
    }
    static void cleanup(File root, File file) throws Exception {
        String allowed=root.getCanonicalPath();
        check(file.getCanonicalPath().equals(allowed) || file.getCanonicalPath().startsWith(allowed+"/"),"fixture cleanup confined");
        if(file.isDirectory())for(File child:file.listFiles())cleanup(root,child);
        if(!file.delete())throw new Exception("Cannot remove fixture "+file);
    }
    public static void main(String[] args) throws Exception {
        if(Looper.getMainLooper()==null)Looper.prepareMainLooper();
        final File root=new File("/data/local/tmp","haiagaru-archive-fixture-"+UUID.randomUUID());
        if(!root.mkdir())throw new Exception("Cannot create isolated fixture");
        Context context=new ContextWrapper(null){
            @Override public File getFilesDir(){return root;}
            @Override public File getCacheDir(){return new File(root,"cache");}
            @Override public File getExternalFilesDir(String type){return new File(root,"external");}
            @Override public File getFileStreamPath(String name){return new File(root,name);}
            @Override public String getPackageName(){return "haiagaru.archive.fixture";}
        };
        try {
            archive=Class.forName("app.morphe.extension.chmate.ResponseArchive");
            Class<?> entryClass=Class.forName("app.morphe.extension.chmate.ResponseArchiveEntry");
            Class<?>[] collectionArgs={Context.class,String.class};
            Object one=call("createCollection",collectionArgs,context,"  保存用スレA  ");
            Object two=call("createCollection",collectionArgs,context,"保存用スレB");
            String id=(String)field(one,"id"), other=(String)field(two,"id");
            check(!id.equals(other),"independent collection IDs");
            check("保存用スレA".equals(field(call("readCollection",collectionArgs,context,id),"title")),"title roundtrip and trimming");
            String url="https://example.invalid/test/read.cgi/test/1234567890/42";
            Object entry=entryClass.getConstructor(String.class,String.class,int.class,String.class,String.class,String.class)
                .newInstance(url,"合成元スレ",42,"名前","日時 ID:a+/","<b>保存確認</b><br>line2 &amp; + /");
            File legacy=(File)call("file",collectionArgs,context,url);
            File threads=new File(new File(root,"haiagaru-response-archive"),"threads");
            File first=new File(new File(threads,id),legacy.getName()), second=new File(new File(threads,other),legacy.getName());
            Class<?>[] saveArgs={Context.class,String.class,entryClass};
            call("saveToCollection",saveArgs,context,id,entry);
            check(first.exists() && !second.exists() && !legacy.exists(),"save only chosen collection");
            call("saveToCollection",saveArgs,context,other,entry);
            Object restored=call("read",new Class<?>[]{File.class},second);
            check(field(entry,"body").equals(field(restored,"body")),"raw body retained");
            check(field(entry,"date").equals(field(restored,"date")),"date and ID retained");
            call("saveToCollection",saveArgs,context,id,entry);
            int count=0;for(File f:first.getParentFile().listFiles())if(f.getName().matches("[0-9a-f]{64}\\.json"))count++;
            check(count==1,"duplicate response replaces existing snapshot");
            File backup=new File(first.getPath()+".bak");
            check(first.renameTo(backup),"interrupted atomic write simulated");
            call("read",new Class<?>[]{File.class},first);
            check(first.exists()&&!backup.exists(),"atomic snapshot recovery");
            new AtomicFile(first).delete();
            check(!first.exists()&&second.exists(),"copy deletion isolated");
            call("save",new Class<?>[]{Context.class,entryClass},context,entry);
            check(legacy.exists(),"legacy archive kept separate");
            check(((List<?>)call("collections",new Class<?>[]{Context.class},context)).size()==2,"both collections listed");
            Constructor<?> constructor=one.getClass().getDeclaredConstructors()[0];constructor.setAccessible(true);
            Object updated=constructor.newInstance(id,"変更後の保存用スレ",field(one,"created"));
            call("writeCollection",new Class<?>[]{File.class,one.getClass()},new File(threads,id),updated);
            check("変更後の保存用スレ".equals(field(call("readCollection",collectionArgs,context,id),"title")),"collection renamed");
            reject("createCollection",collectionArgs,context,"   ");
            reject("createCollection",collectionArgs,context,new String(new char[201]).replace('\0','x'));
            for(String bad:new String[]{null,"","../outside","00000000-0000-0000-0000-000000000000"})
                reject("saveToCollection",saveArgs,context,bad,entry);
            check(legacy.exists()&&second.exists(),"invalid input preserves existing copies");
            // Local and MEGA backups use this same ZIP capture/restore implementation.
            Class<?> backups=Class.forName("app.morphe.extension.chmate.HaiagaruBackupArchive");
            Method capture=backups.getDeclaredMethod("capture",Context.class,int.class);capture.setAccessible(true);
            File zip=(File)capture.invoke(null,context,64);
            String prefix="responses/threads/";
            try(java.util.zip.ZipFile contents=new java.util.zip.ZipFile(zip)) {
                check(contents.getEntry(prefix+id+"/collection.json")!=null,"backup includes first saved-thread metadata");
                check(contents.getEntry(prefix+other+"/"+second.getName())!=null,"backup includes second saved-thread response");
                check(contents.getEntry("responses/"+legacy.getName())!=null,"backup includes legacy responses too");
            }
            new AtomicFile(second).delete();
            new AtomicFile(new File(new File(threads,id),"collection.json")).delete();
            new AtomicFile(legacy).delete();
            Method readBackup=backups.getDeclaredMethod("read",Context.class,File.class);readBackup.setAccessible(true);
            Object transfer=readBackup.invoke(null,context,zip);
            Method restore=backups.getDeclaredMethod("restore",Context.class,transfer.getClass(),int.class,boolean.class);restore.setAccessible(true);
            restore.invoke(null,context,transfer,64,false);
            check(second.exists()&&legacy.exists(),"backup restores saved-thread and legacy responses");
            check("変更後の保存用スレ".equals(field(call("readCollection",collectionArgs,context,id),"title")),"backup restores saved-thread names");
            check(field(entry,"body").equals(field(call("read",new Class<?>[]{File.class},second),"body")),"restored response remains readable");
            ((java.io.Closeable)transfer).close();
            if(args.length>0&&args[0].equals("226")) {
                Class<?> collector=Class.forName("o.QWA$5$5$5",false,archive.getClassLoader());
                check(collector.getDeclaredMethods().length>=2,"ART loads patched 226 collector methods");
                Class<?> paneClass=Class.forName("o.QWA",false,archive.getClassLoader());
                Class<?> unsafeClass=Class.forName("sun.misc.Unsafe");
                Field singleton=unsafeClass.getDeclaredField("theUnsafe");singleton.setAccessible(true);
                Object unsafe=singleton.get(null);
                Method allocate=unsafeClass.getMethod("allocateInstance",Class.class);
                Constructor<?> makeCollector=collector.getDeclaredConstructor(paneClass);makeCollector.setAccessible(true);
                Method emit=collector.getDeclaredMethod("emit",Object.class,Class.forName("o.bea12",false,archive.getClassLoader()));emit.setAccessible(true);
                Field guard=paneClass.getDeclaredField("i");guard.setAccessible(true);
                for(boolean previous:new boolean[]{false,true}) {
                    Object pane=allocate.invoke(unsafe,paneClass);
                    guard.setBoolean(pane,previous);
                    boolean reachedOriginal=false;
                    try {emit.invoke(makeCollector.newInstance(pane),null,null);}
                    catch(InvocationTargetException expected){reachedOriginal=expected.getCause() instanceof NullPointerException;}
                    check(reachedOriginal && guard.getBoolean(pane)==previous,"ART native wrapper restores guard on exception, previous="+previous);
                }
            }
            System.out.println("PASS "+checks+" Android response collection checks "+(args.length>0?args[0]:""));
        } finally {cleanup(root,root);}
    }
}
