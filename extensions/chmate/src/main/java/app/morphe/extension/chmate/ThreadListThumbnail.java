package app.morphe.extension.chmate;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ImageSpan;
import android.widget.BaseAdapter;
import android.widget.TextView;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/** Native title-only thumbnail decoration. Works for both ListView and RecyclerView. */
public final class ThreadListThumbnail {
    private static final ExecutorService IO=Executors.newFixedThreadPool(2,r->{Thread t=new Thread(r,"Haiagaru-thread-thumbnail");t.setDaemon(true);return t;});
    private static final Map<String,Bitmap> IMAGES=Collections.synchronizedMap(new LinkedHashMap<String,Bitmap>(64,.75f,true){
        @Override protected boolean removeEldestEntry(Map.Entry<String,Bitmap> entry){return size()>64;}
    });
    private static final Map<String,Long> FAILED=Collections.synchronizedMap(new LinkedHashMap<String,Long>(128,.75f,true){
        @Override protected boolean removeEldestEntry(Map.Entry<String,Long> entry){return size()>1200;}
    });
    private static final Set<String> PENDING=ConcurrentHashMap.newKeySet();
    private static final Map<Object,Boolean> ADAPTERS=Collections.synchronizedMap(new WeakHashMap<>());
    private static final ThreadLocal<Selection> CURRENT=new ThreadLocal<>();
    private static final AtomicBoolean REFRESH=new AtomicBoolean();

    private static final class Selection {
        final Context context;
        final SiteBoardGateway gateway;
        final String site;
        final ThreadListThumbnailRules.Board five;
        TextView title;
        String id;
        Selection(Context context,String site,ThreadListThumbnailRules.Board five){
            this.context=context;this.site=site;this.five=five;
            this.gateway=site==null?null:SiteBoardGateway.get(context);
        }
        String cacheKey(){return site==null?"5ch:"+five.host+":"+five.name+":"+id:"site:"+site+":"+id;}
    }

    /** Called at native adapter entry, before any title or subject is bound. */
    public static void begin(Object adapter){
        CURRENT.remove();
        if(adapter==null)return;
        Context context=Haiagaru.applicationContextForExtension();
        if(context==null)return;
        String site=SiteThreadMetadata.currentBoard();
        if(site!=null){
            if(!ThreadListThumbnailSettings.selected(context,"site:"+site))return;
            CURRENT.set(new Selection(context,site,null));ADAPTERS.put(adapter,true);return;
        }
        String member=adapter.getClass().getName().equals("o.m8a")?"r":adapter.getClass().getName().equals("o.getWriteBytesTotal")?"g":adapter.getClass().getName().equals("o.zzavb")?"c":null;
        if(member==null)return;
        try{
            Field field=adapter.getClass().getDeclaredField(member);field.setAccessible(true);
            Object board=field.get(adapter);
            ThreadListThumbnailRules.Board five=ThreadListThumbnailRules.board(board==null?null:board.toString());
            if(five==null)return;
            ThreadListThumbnailSettings.rememberFiveCh(context,five);
            if(!ThreadListThumbnailSettings.selected(context,five.settingsKey()))return;
            CURRENT.set(new Selection(context,null,five));ADAPTERS.put(adapter,true);
        }catch(ReflectiveOperationException | RuntimeException ignored){/* Unknown native adapters remain untouched. */}
    }

    /** The subject can be read after setText on some ChMate builds. */
    public static void title(TextView title){Selection state=CURRENT.get();if(state!=null)state.title=title;}

    public static void subject(Object row){
        Selection state=CURRENT.get();if(state==null||row==null)return;
        state.id=null;
        String type=row.getClass().getName();
        String name=type.equals("o.MaxAdViewImplExternalSyntheticLambda1")?"e":type.equals("o.CertificateChainCleaner")?"b":type.equals("o.setSkipText")?"d":null;
        if(name==null)return;
        try{
            Field field=row.getClass().getDeclaredField(name);field.setAccessible(true);
            long number=field.getLong(row);
            if(state.site!=null)state.id=SiteThreadIds.source(Long.toString(number));
            else if(number>=1_000_000_000L&&number<SiteThreadIds.BASE)state.id=Long.toString(number);
        }catch(Exception unknown){/* Never guess a thread from a recycled row. */}
    }

    /** Called before each native row return, after both subject and title are known. */
    public static void end(){
        Selection state=CURRENT.get();CURRENT.remove();
        if(state==null||state.id==null||state.title==null||!ThreadListThumbnailSettings.enabled(state.context))return;
        String key=state.cacheKey();Bitmap bitmap=IMAGES.get(key);
        if(bitmap!=null){decorate(state.title,bitmap);return;}
        long now=SystemClock.elapsedRealtime();Long retry=FAILED.get(key);
        if(retry!=null&&retry>now||PENDING.size()>=40||!PENDING.add(key))return;
        IO.execute(()->{
            try{
                String source=state.site==null?fiveChOpeningImage(state.five,state.id):state.gateway.openingImage(state.site,state.id);
                if(source==null){FAILED.put(key,SystemClock.elapsedRealtime()+30*60_000L);return;}
                byte[] bytes=state.site==null?fetch(source,3*1024*1024,true):state.gateway.openingImageBytes(source);
                Bitmap decoded=decode(bytes);
                if(decoded==null)throw new IOException("サムネイルをデコードできません");
                IMAGES.put(key,decoded);FAILED.remove(key);refresh();
            }catch(Exception unavailable){FAILED.put(key,SystemClock.elapsedRealtime()+15*60_000L);}
            finally{PENDING.remove(key);}
        });
    }

    private static void decorate(TextView title,Bitmap bitmap){
        CharSequence original=title.getText();
        if(original==null||original.length()==0)return;
        // Native adapters may reuse a title TextView on notifyDataSetChanged().
        // Replace the old span so a changed size is applied to cached images too.
        if(original instanceof Spanned){
            Spanned spanned=(Spanned)original;
            ThumbnailSpan[] thumbnails=spanned.getSpans(0,original.length(),ThumbnailSpan.class);
            if(thumbnails.length>0){
                SpannableStringBuilder cleaned=new SpannableStringBuilder(original);
                for(ThumbnailSpan thumbnail:thumbnails)cleaned.removeSpan(thumbnail);
                if(cleaned.length()>=3&&cleaned.charAt(0)=='\uFFFC'&&cleaned.charAt(1)==' '&&cleaned.charAt(2)==' ')
                    cleaned.delete(0,3);
                original=cleaned;
            }
        }
        if(original.length()==0)return;
        int limit=Math.round(ThreadListThumbnailSettings.sizeDp(title.getContext())*title.getResources().getDisplayMetrics().density);
        int width=Math.max(1,Math.round(limit*Math.min(1f,(float)bitmap.getWidth()/Math.max(1,bitmap.getHeight()))));
        int height=Math.max(1,Math.round(limit*Math.min(1f,(float)bitmap.getHeight()/Math.max(1,bitmap.getWidth()))));
        Drawable drawable=new BitmapDrawable(title.getResources(),bitmap);drawable.setBounds(0,0,width,height);
        SpannableStringBuilder text=new SpannableStringBuilder("\uFFFC  ").append(original);
        text.setSpan(new ThumbnailSpan(drawable),0,1,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        title.setText(text);
    }
    private static final class ThumbnailSpan extends ImageSpan {
        ThumbnailSpan(Drawable image){super(image,ImageSpan.ALIGN_BOTTOM);}
    }

    private static Bitmap decode(byte[] bytes){
        BitmapFactory.Options options=new BitmapFactory.Options();options.inJustDecodeBounds=true;
        BitmapFactory.decodeByteArray(bytes,0,bytes.length,options);
        if(options.outWidth<1||options.outHeight<1||options.outWidth>16000||options.outHeight>16000)return null;
        options.inJustDecodeBounds=false;options.inPreferredConfig=Bitmap.Config.RGB_565;
        options.inSampleSize=1;
        while(Math.max(options.outWidth,options.outHeight)/options.inSampleSize>160)options.inSampleSize*=2;
        return BitmapFactory.decodeByteArray(bytes,0,bytes.length,options);
    }

    private static String fiveChOpeningImage(ThreadListThumbnailRules.Board board,String id)throws IOException{
        String address=board.threadUrl(id);
        byte[] html=fetch(address,2*1024*1024,false);
        return ThreadListThumbnailRules.firstFiveChImage(new String(html,java.nio.charset.StandardCharsets.UTF_8),address);
    }

    /** No redirect or non-HTTPS fallback: only explicitly selected public sources. */
    private static byte[] fetch(String address,int limit,boolean image)throws IOException{
        if(!ThreadListThumbnailRules.publicImage(address))throw new IOException("取得先が対応範囲外です");
        if(image&&ImageVpnController.protectedPreview(address)){
            app.morphe.extension.chmate.vpn.nativecore.imagevpn.Image result=ImageVpnController.fetchPreview(address);
            byte[] body=result.getBody();String contentType=result.getContentType();
            if(body==null||body.length>limit||contentType==null||!contentType.toLowerCase(java.util.Locale.ROOT).startsWith("image/"))
                throw new IOException("VPN経由でサムネイルを取得できません");
            return body;
        }
        HttpURLConnection c=(HttpURLConnection)URI.create(address).toURL().openConnection();
        c.setInstanceFollowRedirects(false);c.setConnectTimeout(6000);c.setReadTimeout(10000);
        c.setRequestProperty("User-Agent","Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 Chrome/120 Mobile Safari/537.36");
        try{
            if(c.getResponseCode()!=200)throw new IOException("HTTP "+c.getResponseCode());
            String type=c.getContentType();
            if(image&&(type==null||!type.toLowerCase(java.util.Locale.ROOT).startsWith("image/")))throw new IOException("画像ではありません");
            if(c.getContentLengthLong()>limit)throw new IOException("取得上限を超えました");
            try(InputStream in=c.getInputStream();ByteArrayOutputStream result=new ByteArrayOutputStream()){
                byte[] buffer=new byte[8192];int n;
                while((n=in.read(buffer))!=-1){if(result.size()+n>limit)throw new IOException("取得上限を超えました");result.write(buffer,0,n);}
                return result.toByteArray();
            }
        }finally{c.disconnect();}
    }

    static void refresh(){
        if(!REFRESH.compareAndSet(false,true))return;
        new Handler(Looper.getMainLooper()).postDelayed(()->{
            REFRESH.set(false);List<Object> adapters;
            synchronized(ADAPTERS){adapters=new ArrayList<>(ADAPTERS.keySet());}
            for(Object adapter:adapters)if(adapter!=null)try{
                if(adapter instanceof BaseAdapter)((BaseAdapter)adapter).notifyDataSetChanged();
                else adapter.getClass().getMethod("notifyDataSetChanged").invoke(adapter);
            }catch(Exception ignored){/* A future native rebind still consumes the ready bitmap. */}
        },500);
    }
    private ThreadListThumbnail(){}
}
