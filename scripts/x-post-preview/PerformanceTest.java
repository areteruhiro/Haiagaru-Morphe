package app.morphe.extension.chmate;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.SpannableStringBuilder;
import android.util.LruCache;
import android.widget.TextView;
import java.lang.ref.WeakReference;
import java.lang.reflect.*;
import java.util.*;

/** Counts actual TextView rewrites and parser misses, with synthetic cached posts only. */
public final class PerformanceTest {
    public static String result = "pending";
    public static int checks;
    public static long elapsedNanos;
    static CountingView view;
    static final class CountingView extends TextView {
        int writes;
        CountingView(Context context) { super(context); }
        @Override public boolean isShown() { return true; }
        @Override public boolean isAttachedToWindow() { return true; }
        @Override public void setText(CharSequence text, BufferType type) { super.setText(text,type); writes++; }
    }
    static Field field(Class<?> type, String name) throws Exception {
        Field f = type.getDeclaredField(name); f.setAccessible(true); return f;
    }
    static void check(boolean ok,String message) {
        if (!ok) throw new AssertionError(message);
        checks++;
    }
    public static void start(Context context) {
        try {
            Haiagaru.enabled=true; Haiagaru.full=true; Haiagaru.collapsible=false;
            Class<?> entryType = Class.forName("app.morphe.extension.chmate.XPostPreview$Entry");
            Constructor<?> constructor=entryType.getDeclaredConstructor();constructor.setAccessible(true);
            Map cache=(Map)field(XPostPreview.class,"CACHE").get(null);
            String id="6789001",otherId="6789002";
            Object entry=constructor.newInstance(), other=constructor.newInstance();
            Field data=field(entryType,"data"),expires=field(entryType,"expires");
            data.set(entry,new XPostPreview.Data(id,"test","fixture","original",""));
            expires.setLong(entry,android.os.SystemClock.elapsedRealtime()+900000);
            cache.put(id,entry);cache.put(otherId,other);
            view=new CountingView(context);
            view.setText(new SpannableStringBuilder("本文 https://x.com/fixture/status/"+id),TextView.BufferType.SPANNABLE);
            XPostPreview.apply(view);
            check(view.getText().toString().contains("original"),"warm preview rendered");
            int writes=view.writes;
            LruCache ids=(LruCache)field(XPostPreview.class,"IDS").get(null);
            int misses=ids.missCount();
            long start=System.nanoTime();
            for(int i=0;i<1000;i++){XPostPreview.onBound(view);XPostPreview.apply(view);}
            elapsedNanos=System.nanoTime()-start;
            check(view.writes==writes,"1000 unchanged bind/layout pairs produce zero rewrites");
            check(ids.missCount()==misses,"unchanged preview does not repeat URL parsing");
            // Complete a different post through the production completion path.
            XPostMetadata.failures=0;
            XPostMetadata.response="{\"id_str\":\""+otherId+"\",\"text\":\"other\",\"user\":{\"name\":\"test\",\"screen_name\":\"fixture\"}}";
            Method resolve=XPostPreview.class.getDeclaredMethod("resolve",String.class,entryType);resolve.setAccessible(true);
            int revision=field(XPostPreview.class,"revision").getInt(null);
            resolve.invoke(null,otherId,other);
            XPostPreview.apply(view);
            check(field(XPostPreview.class,"revision").getInt(null)==revision,"unrelated completion does not invalidate all rows");
            check(view.writes==writes,"unrelated completion produces zero rewrites");
            int calls=XPostMetadata.calls;
            resolve.invoke(null,"6789003",constructor.newInstance());
            check(XPostMetadata.calls==calls,"evicted/cleared queued request never starts a download");
            data.set(entry,new XPostPreview.Data(id,"test","fixture","updated",""));
            XPostPreview.apply(view);
            check(view.writes==writes+1&&view.getText().toString().contains("updated"),"only changed row is rewritten once");
            XPostPreview.apply(view);
            check(view.writes==writes+1,"updated row is stable on next layout");
            String plain="X URLのない本文";
            view.setText(new SpannableStringBuilder(plain),TextView.BufferType.SPANNABLE);
            XPostPreview.apply(view);misses=ids.missCount();writes=view.writes;
            for(int i=0;i<1000;i++){XPostPreview.onBound(view);XPostPreview.apply(view);}
            check(view.writes==writes,"ordinary responses produce zero preview rewrites");
            check(ids.missCount()==misses,"negative URL scan results are reused");
            // Duplicate completion notifications should be merged into one UI rewrite.
            view.setText(new SpannableStringBuilder("https://x.com/fixture/status/"+id),TextView.BufferType.SPANNABLE);
            XPostPreview.apply(view);
            data.set(entry,new XPostPreview.Data(id,"test","fixture","batched",""));
            int beforeBatch=view.writes;
            Method refresh=XPostPreview.class.getDeclaredMethod("refresh",List.class);refresh.setAccessible(true);
            List<WeakReference<TextView>> refs=Arrays.asList(new WeakReference<>(view),new WeakReference<>(view));
            refresh.invoke(null,refs);refresh.invoke(null,refs);
            Handler main = new Handler(Looper.getMainLooper());
            // Start the assertion delay after queued notifications have run.
            // A busy UI must not make a 150ms test deadline precede the 16ms batch.
            main.post(()->main.postDelayed(()->{
                try {
                    check(view.writes==beforeBatch+1&&view.getText().toString().contains("batched"),"four completion notifications coalesce into one rewrite");
                    check(ids.size()<=ids.maxSize(),"URL cache stays within its memory bound");
                    result="PASS "+checks+" preview performance checks; 1000 unchanged bind/layout pairs="+(elapsedNanos/1000000.0)+"ms, zero rewrites";
                }catch(Throwable e){result="FAIL "+e;}
                finally{view=null;}
            },150));
        }catch(Throwable e){result="FAIL "+e;}
    }
}
