package app.morphe.extension.chmate;

import android.content.Context;
import android.app.Activity;
import android.widget.CheckBox;
import android.widget.Toast;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.WeakHashMap;

/** Removes only thread-scoped NG rows after their source thread is deleted. */
final class ThreadScopedNg {
    private static final String[] CATEGORIES = {"ID", "NAME", "WORD"};
    private static final ThreadLocal<Boolean> BYPASS = new ThreadLocal<>();
    private static final Map<Activity,Boolean> PROMPTS = new WeakHashMap<>();

    private ThreadScopedNg() {}

    static void beginBypass(){BYPASS.set(Boolean.TRUE);}
    static void endBypass(){BYPASS.remove();}

    /** Called by ChMate's native bookmark deletion method after it returns. */
    public static void nativeThreadDeleted(Object nativeBoard, long threadId) {
        Context context = Haiagaru.applicationContext();
        if (context == null || nativeBoard == null || threadId <= 0) return;
        onThreadDeleted(context, nativeBoard.toString(), threadId);
    }

    static int onThreadDeleted(Context context, String board, long threadId) {
        int removed = 0;
        for (String category : CATEGORIES) {
            try {
                NativeNgStore store = new NativeNgStore(context, category);
                List<NativeNgStore.Entry> before = store.entries();
                ArrayList<Object> after = new ArrayList<>(before.size());
                int categoryRemoved = 0;
                for (NativeNgStore.Entry entry : before) {
                    if (NgBoardScope.isThread(entry.board, board, threadId)) categoryRemoved++;
                    else after.add(entry.nativeItem);
                }
                if (categoryRemoved > 0) {
                    store.replace(before, after);
                    removed += categoryRemoved;
                }
            } catch (Exception error) {
                // The thread has already been deleted. Keep cleanup failures
                // visible in logcat without interrupting the native deletion.
                Log.w("Haiagaru", "Unable to remove NG rules for deleted thread " + board + "/" + threadId, error);
            }
        }
        return removed;
    }

    /** Defers native ID/name/word insertion until the thread-only checkbox is chosen. */
    public static boolean interceptRegistration(Object nativeStore,Object before,Object after) {
        if(Boolean.TRUE.equals(BYPASS.get())||nativeStore==null||before!=null||after==null)return false;
        Context context=Haiagaru.applicationContext();if(context==null)return false;
        boolean supported=false;
        for(String category:CATEGORIES)try{
            if(new NativeNgStore(context,category).store==nativeStore){supported=true;break;}
        }catch(Exception ignored){}
        if(!supported)return false;
        FilterPosition.ThreadTarget target=FilterPosition.currentVisibleThread();if(target==null)return false;
        synchronized(PROMPTS){if(PROMPTS.containsKey(target.activity))return true;PROMPTS.put(target.activity,Boolean.TRUE);}
        Runnable display=()->showRegistrationPrompt(target.activity,nativeStore,before,after,target);
        if(android.os.Looper.myLooper()==android.os.Looper.getMainLooper())display.run();
        else new android.os.Handler(android.os.Looper.getMainLooper()).post(display);
        return true;
    }

    private static void showRegistrationPrompt(Activity activity,Object nativeStore,Object before,Object after,FilterPosition.ThreadTarget target){
        if(activity.isFinishing()||activity.isDestroyed()){synchronized(PROMPTS){PROMPTS.remove(activity);}return;}
        CheckBox onlyThread=new CheckBox(activity);onlyThread.setText("このスレのみを対象にする");
        HaiagaruDialog dialog=new HaiagaruDialog.Builder(activity).setTitle("NGに登録")
                .setMessage("対象を選んでNG登録します。")
                .setView(onlyThread).setNegativeButton("登録しない",(d,w)->finishPrompt(activity))
                .setPositiveButton("登録",null).create();
        dialog.setOnCancelListener(d->finishPrompt(activity));
        dialog.setOnShowListener(d->dialog.getButton(HaiagaruDialog.BUTTON_POSITIVE).setOnClickListener(v->{
            try{
                Object item=after;
                if(onlyThread.isChecked()){
                    NativeNgStore.Entry entry=new NativeNgStore.Entry(after);
                    item=after.getClass().getConstructor(String.class,short.class,long.class,String.class)
                            .newInstance(entry.pattern,entry.flags,entry.timestamp,NgBoardScope.thread(target.board,target.threadId));
                }
                Method update=null;
                for(Method method:nativeStore.getClass().getDeclaredMethods()){
                    Class<?>[] p=method.getParameterTypes();
                    if(method.getReturnType()==int.class&&Arrays.equals(p,new Class<?>[]{after.getClass(),after.getClass()})){update=method;break;}
                }
                if(update==null)throw new NoSuchMethodException("Native NG updater changed");
                update.setAccessible(true);BYPASS.set(Boolean.TRUE);
                try{update.invoke(nativeStore,before,item);}finally{BYPASS.remove();}
                if(onlyThread.isChecked())Toast.makeText(activity,"このスレのみのNGとして登録しました",Toast.LENGTH_SHORT).show();
                dialog.dismiss();finishPrompt(activity);
            }catch(Exception error){Log.w("Haiagaru","NG registration failed",error);Toast.makeText(activity,"NGを登録できませんでした",Toast.LENGTH_LONG).show();finishPrompt(activity);}
        }));
        dialog.show();
    }
    private static void finishPrompt(Activity activity){synchronized(PROMPTS){PROMPTS.remove(activity);}}
}
