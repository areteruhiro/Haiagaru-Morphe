package app.morphe.extension.chmate;

import android.content.Context;
import android.content.SharedPreferences;
import java.io.*;
import java.lang.reflect.Method;
import java.net.*;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.*;

/** Bounded asynchronous supplement for cached rows whose native DROP flag is still zero. */
final class DroppedThreadStatus {
    private static final long RETRY=5*60_000L,CONFIRMED=7*24*60*60_000L;
    private static final String PREFS="haiagaru-confirmed-dropped-threads";
    private static final Map<String,Result> results=new LinkedHashMap<>();
    private static final Map<String,Board> boards=new LinkedHashMap<>();
    private static final Set<String> pending=new HashSet<>();
    private static final ThreadPoolExecutor IO=new ThreadPoolExecutor(2,2,30,TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(128),r->{java.lang.Thread t=new java.lang.Thread(r,"Haiagaru-DAT-status");t.setDaemon(true);return t;});
    private static final class Result {final boolean dropped;final long until;Result(boolean d,long u){dropped=d;until=u;}}
    private static final class Board {final Set<String> ids;final long until;Board(Set<String> i,long u){ids=i;until=u;}}
    static String rowUrl(Object row,Context context)throws Exception{
        String name=row.getClass().getName();Object model;
        if(name.equals("o.onAdDisplayed$setIcon"))model=row.getClass().getMethod("b").invoke(row);
        else if(name.equals("o.Yy2$open"))model=row.getClass().getMethod("c",Context.class).invoke(row,context);
        else if(name.equals("o.getAvailabilityStatus$r8lambdawJ5MHcSJed_CjC7r4OWD0UxyJsQ"))model=row.getClass().getMethod("e",Context.class).invoke(row,context);
        else return null;
        if(model==null)return null;
        Method url=model.getClass().getMethod(ArchiveOrigins.nativeUrlMethod());
        return (String)url.invoke(model);
    }
    static boolean dropped(Object row){
        Context context=Haiagaru.applicationContextForExtension();if(context==null)return false;
        try{
            DroppedThreadRules.Thread thread=DroppedThreadRules.thread(rowUrl(row,context));if(thread==null)return false;
            long now=System.currentTimeMillis();
            synchronized(results){
                Result saved=results.get(thread.key);if(saved!=null&&saved.until>now)return saved.dropped;
            }
            long saved=context.getSharedPreferences(PREFS,0).getLong(thread.key,0);
            if(saved>now&&saved<=now+CONFIRMED){remember(thread.key,true,saved);return true;}
            synchronized(pending){
                if(pending.size()>=130||!pending.add(thread.key))return false;
            }
            try{IO.execute(()->check(context,thread));}
            catch(RejectedExecutionException ignored){synchronized(pending){pending.remove(thread.key);}}
        }catch(Exception ignored){/* Unknown/native non-thread row: keep visible. */}
        return false;
    }
    private static void remember(String key,boolean dropped,long until){
        synchronized(results){if(results.size()>=4096)results.remove(results.keySet().iterator().next());results.put(key,new Result(dropped,until));}
    }
    private static void check(Context context,DroppedThreadRules.Thread thread){
        boolean dropped=false;
        try{
            if(!HiddenDroppedThreads.enabled())return;
            String boardKey=thread.origin+"/"+thread.board;Board board;
            synchronized(boards){board=boards.get(boardKey);}
            if(board==null||board.until<=System.currentTimeMillis()){
                Set<String> ids=DroppedThreadRules.subject(fetch(thread.origin+"/"+thread.board+"/subject.txt"));
                if(!ids.isEmpty()){
                    board=new Board(ids,System.currentTimeMillis()+RETRY);
                    synchronized(boards){if(boards.size()>=128)boards.remove(boards.keySet().iterator().next());boards.put(boardKey,board);}
                }
            }
            if(board==null||!board.ids.contains(thread.id))dropped=DroppedThreadRules.archived(fetch(thread.url));
            long until=System.currentTimeMillis()+(dropped?CONFIRMED:RETRY);
            remember(thread.key,dropped,until);
            if(dropped){
                SharedPreferences p=context.getSharedPreferences(PREFS,0);
                SharedPreferences.Editor edit=p.edit();Map<String,?> all=p.getAll();long now=System.currentTimeMillis();
                for(Map.Entry<String,?> e:all.entrySet())if(!(e.getValue() instanceof Long)||(Long)e.getValue()<=now)edit.remove(e.getKey());
                // Disposable detection cache only; never delete DAT/history or write native thread flags.
                if(all.size()>=4096)edit.clear();edit.putLong(thread.key,until).apply();
                Haiagaru.refreshBoardListAfterReadSortChange();
            }
        }catch(Exception ignored){remember(thread.key,false,System.currentTimeMillis()+RETRY);}
        finally{synchronized(pending){pending.remove(thread.key);}}
    }
    private static String fetch(String url)throws IOException{
        HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();
        c.setInstanceFollowRedirects(false);c.setConnectTimeout(6000);c.setReadTimeout(6000);
        c.setRequestProperty("User-Agent","Monazilla/1.00 (Haiagaru DAT status)");
        try{
            if(c.getResponseCode()!=200)throw new IOException("Unconfirmed HTTP status");
            try(InputStream in=c.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){
                byte[] buffer=new byte[8192];int n;
                while((n=in.read(buffer))!=-1){if(out.size()+n>2*1024*1024)throw new IOException("Response too large");out.write(buffer,0,n);}
                String type=c.getContentType();Charset encoding=Charset.forName(type!=null&&type.toLowerCase(Locale.ROOT).contains("utf-8")?"UTF-8":"MS932");
                return new String(out.toByteArray(),encoding);
            }
        }finally{c.disconnect();}
    }
    private DroppedThreadStatus(){}
}
