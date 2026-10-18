package app.morphe.extension.chmate;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.Map;
import java.util.WeakHashMap;

/** Native thread-send login gate and successful-post logout callback. */
public final class BeAutoLogout {
    public static final String KEY="beLogoutAfterThread";
    private static final AtomicBoolean running=new AtomicBoolean();
    private static final Object sessionLock=new Object();
    private static final Map<Object,Object> targets=new WeakHashMap<>();
    private BeAutoLogout() {}
    // Pinned to the native BE login/logout worker by the bytecode patch.
    public static String nativeWorker() { return ""; }
    public static String nativeAction() { return ""; }
    public static String nativeBoardUrl() { return ""; }
    private static boolean fiveCh(String url) {
        try {
            String host=new java.net.URI(url).getHost();
            if(host==null)return false;
            host=host.toLowerCase(java.util.Locale.ROOT);
            return host.equals("5ch.net") || host.endsWith(".5ch.net")
                    || host.equals("5ch.io") || host.endsWith(".5ch.io");
        }catch(Exception ignored){return false;}
    }
    private static boolean enabled(Context context) {
        return context!=null && context.getSharedPreferences("io.github.areteruhiro.chmate.haiagaru.ui-config",0).getBoolean(KEY,false);
    }
    static boolean eligible(boolean enabled,String threadUrl,String boardUrl) {
        return enabled && threadUrl==null && fiveCh(boardUrl);
    }
    /** Called only by the native new-thread HTTP method, on its posting worker. */
    public static void beforeSend(Object board) throws Exception {
        if(!enabled(Haiagaru.applicationContextForExtension()) || board==null)return;
        Method url=board.getClass().getMethod(nativeBoardUrl());
        if(!fiveCh((String)url.invoke(board)))return;
        if(Looper.myLooper()==Looper.getMainLooper())throw new IllegalStateException("BE自動ログインはバックグラウンドで実行する必要があります");
        synchronized(sessionLock) {
            if(running.get())throw new IllegalStateException("BEのログアウト中です。完了してからスレ立てを再試行してください");
            try { action(board,true); }
            catch(Exception error) {
                // Never continue sending a thread if authentication could not be confirmed.
                throw new IllegalStateException("BE自動ログインに失敗しました。保存済みのBE情報と通信状態を確認してください（スレは送信していません）",error);
            }
        }
    }
    public static void editor(Object fragment,Object target,boolean thread,String url) {
        Object activity=ExternalBoardAuth.activity(fragment);
        if(activity==null)return;
        synchronized(targets) {
            if(thread && fiveCh(url))targets.put(activity,target);
            else targets.remove(activity);
        }
    }
    public static void completed(Object activity,int resultCode) {
        if(resultCode!=android.app.Activity.RESULT_OK)return;
        Context context=Haiagaru.applicationContextForExtension();
        if(!enabled(context))return;
        try {
            Object target; synchronized(targets){target=targets.remove(activity);}
            if(target==null || !running.compareAndSet(false,true))return;
            final Object board=target;
            new Thread(()->{
                try {
                    synchronized(sessionLock){action(board,false);}
                    message(context,"スレ立て完了後、BEからログアウトしました");
                }catch(Exception error){
                    android.util.Log.w("Haiagaru","Automatic BE logout failed",error);
                    message(context,"BEの自動ログアウトに失敗しました。手動でログアウトしてください");
                }finally{running.set(false);}
            },"Haiagaru-BE-logout").start();
        }catch(Exception error){android.util.Log.w("Haiagaru","BE logout target unavailable",error);}
    }
    private static void action(Object board,boolean login)throws Exception {
        ClassLoader loader=board.getClass().getClassLoader();
        Class<?> worker=Class.forName(nativeWorker(),false,loader);
        Object result;
        try {
            if(nativeAction().equals("invokeSuspend")) {
                Object task=constructor(worker,3).newInstance(login,board,null);
                Method action=worker.getDeclaredMethod("invokeSuspend",Object.class);action.setAccessible(true);
                Class<?> unit=Class.forName("o.zzbwq",false,loader);
                result=action.invoke(task,unit.getField("INSTANCE").get(null));
            }else {
                Object task=constructor(worker,1).newInstance(new Object[]{null});
                Method action=worker.getDeclaredMethod(nativeAction(),Object[].class);action.setAccessible(true);
                result=action.invoke(task,new Object[]{new Object[]{login,board}});
            }
        }catch(InvocationTargetException error) {
            Throwable cause=error.getCause();
            if(cause instanceof Exception)throw (Exception)cause;
            throw new IllegalStateException("BE authentication failed",cause);
        }
        if(result instanceof Exception)throw (Exception)result;
    }
    private static void message(Context context,String text) {
        new Handler(Looper.getMainLooper()).post(()->Toast.makeText(context,text,Toast.LENGTH_LONG).show());
    }
    private static Constructor<?> constructor(Class<?> worker,int count)throws Exception {
        Constructor<?> found=null;
        for(Constructor<?> value:worker.getDeclaredConstructors())if(value.getParameterTypes().length==count) {
            if(found!=null)throw new IllegalStateException("Ambiguous native BE worker");found=value;
        }
        if(found==null)throw new NoSuchMethodException("Native BE worker");
        found.setAccessible(true);return found;
    }
}
