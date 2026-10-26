package app.morphe.extension.chmate;

import android.app.Activity;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.ScrollView;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.Arrays;

/**
 * ChMate's recognizer and ChMate's saved gesture assignments for native tool tabs.
 * There are no hard-coded swipe directions. Unsupported actions are left alone.
 */
final class NativeToolTabChMateGestures {
    private final Activity host;
    private final Object fragment;
    private final NativeToolTabSwipeLayout surface;
    private Object recognizer;
    private Method touch, distance;
    private boolean tracking, intercepted, cancelling;
    private String pending;

    NativeToolTabChMateGestures(Activity host, Object fragment, NativeToolTabSwipeLayout surface) {
        this.host=host; this.fragment=fragment; this.surface=surface;
        try {
            Class<?> type=Class.forName(HaiagaruSettingsGestures.recognizerClassName(),
                    false,host.getClassLoader());
            Constructor<?> constructor=null;
            for(Constructor<?> candidate:type.getConstructors()) {
                Class<?>[] args=candidate.getParameterTypes();
                if(args.length==2&&args[0]==android.content.Context.class&&args[1].isInterface())
                    constructor=candidate;
            }
            if(constructor==null)throw new NoSuchMethodException("ChMate gesture constructor");
            Class<?> listener=constructor.getParameterTypes()[1];
            Object proxy=Proxy.newProxyInstance(type.getClassLoader(),new Class<?>[]{listener},
                    (self,method,args)->{
                        if(method.getDeclaringClass()==Object.class){
                            if("hashCode".equals(method.getName()))return System.identityHashCode(self);
                            if("equals".equals(method.getName()))return self==args[0];
                            return "NativeToolTabChMateGestures";
                        }
                        if(!cancelling&&tracking&&args!=null&&args.length==3
                                &&args[0] instanceof Integer) {
                            String action=assignedAction((Integer)args[0]);
                            if(action!=null){pending=action;intercepted=true;}
                        }
                        return null;
                    });
            recognizer=constructor.newInstance(host,proxy);
            for(Method candidate:type.getDeclaredMethods()){
                if(!Modifier.isPublic(candidate.getModifiers()))continue;
                if(candidate.getReturnType()==boolean.class
                        &&Arrays.equals(candidate.getParameterTypes(),new Class<?>[]{MotionEvent.class}))
                    touch=candidate;
                if(candidate.getReturnType()==void.class
                        &&Arrays.equals(candidate.getParameterTypes(),new Class<?>[]{int.class}))
                    distance=candidate;
            }
            if(touch==null||distance==null)throw new NoSuchMethodException("ChMate gesture methods");
        }catch(ReflectiveOperationException|RuntimeException error){
            recognizer=null;
            android.util.Log.w("Haiagaru","Native tool gesture recognition unavailable",error);
        }
    }

    boolean available(){return recognizer!=null;}
    boolean intercepted(){return intercepted;}
    boolean dispatch(MotionEvent event){
        if(recognizer==null)return false;
        int action=event.getActionMasked();
        if(action==MotionEvent.ACTION_DOWN){
            tracking=false;intercepted=false;pending=null;
            SharedPreferences prefs=PreferenceManager.getDefaultSharedPreferences(host);
            if(!prefs.getBoolean("gesture",true)||!surface.isShown())return false;
            View body=surface.getChildCount()==0?null:surface.getChildAt(surface.getChildCount()-1);
            if(body==null||!body.isShown())return false;
            int[] pos=new int[2];body.getLocationOnScreen(pos);
            if(event.getRawX()<pos[0]||event.getRawY()<pos[1]
                    ||event.getRawX()>=pos[0]+body.getWidth()
                    ||event.getRawY()>=pos[1]+body.getHeight())return false;
            try{distance.invoke(recognizer,prefs.getInt("gestureLength",100));}
            catch(ReflectiveOperationException error){android.util.Log.w("Haiagaru","Gesture distance unavailable",error);return false;}
            tracking=true;
        }
        if(!tracking)return false;
        if(action==MotionEvent.ACTION_POINTER_DOWN||event.getPointerCount()!=1){
            cancel();return false;
        }
        try{touch.invoke(recognizer,event);}
        catch(ReflectiveOperationException|RuntimeException error){
            android.util.Log.w("Haiagaru","Native tool gesture failed",error);cancel();return false;
        }
        boolean consume=intercepted;
        if(action==MotionEvent.ACTION_UP){
            String completed=pending;
            tracking=false;pending=null;intercepted=false;
            if(completed!=null)surface.post(()->perform(completed));
        }else if(action==MotionEvent.ACTION_CANCEL){cancel();}
        return consume;
    }

    private String assignedAction(int code){
        if(code==0||!surface.isShown())return null;
        SharedPreferences prefs=PreferenceManager.getDefaultSharedPreferences(host);
        if(!prefs.getBoolean("gesture",true))return null;
        // Priority follows ChMate's built-in navigation before the optional extension.
        for(String key:new String[]{"gestureCloseTab","gestureNewTab","gestureBack",
                "gestureForward","gestureTop","gestureBottom","gestureRefresh",
                NgEditGesture.KEY}){
            if(prefs.getInt(key,0)==code)return key;
        }
        return null;
    }

    private void perform(String action){
        if(!surface.isAttachedToWindow()||!surface.isShown()
                ||host.isFinishing()||host.isDestroyed())return;
        switch(action){
            case "gestureCloseTab":
                NativeToolTabs.closeCurrentTab(host,fragment);break;
            case "gestureNewTab":
                NativeToolTabs.duplicateToolTab(host,fragment);break;
            case "gestureBack":
                if(!NativeToolTabs.moveThreadTool(host,fragment,-1))history(false);break;
            case "gestureForward":
                if(!NativeToolTabs.moveThreadTool(host,fragment,1))history(true);break;
            case "gestureTop": case "gestureBottom":
                scrollToEnd("gestureBottom".equals(action));break;
            case "gestureRefresh":{
                WebView web=findWebView(surface);
                if(web!=null)web.reload();break;
            }
            default:
                if(NgEditGesture.KEY.equals(action))NgEditGesture.onGesture(surface,
                        PreferenceManager.getDefaultSharedPreferences(host).getInt(action,0));
        }
    }
    private void history(boolean forward){
        WebView web=findWebView(surface);
        if(web!=null){
            if(forward&&web.canGoForward())web.goForward();
            else if(!forward&&web.canGoBack())web.goBack();
        }
    }
    private void scrollToEnd(boolean bottom){
        View view=findScrollable(surface,bottom);
        if(view instanceof WebView){
            if(bottom)((WebView)view).pageDown(true);
            else ((WebView)view).pageUp(true);
        }else if(view instanceof ScrollView){
            ((ScrollView)view).fullScroll(bottom?View.FOCUS_DOWN:View.FOCUS_UP);
        }
    }
    private static View findScrollable(View root,boolean down){
        if(root instanceof WebView||root instanceof ScrollView)return root;
        if(root instanceof ViewGroup){
            ViewGroup group=(ViewGroup)root;
            for(int i=group.getChildCount()-1;i>=0;i--){
                View child=group.getChildAt(i);
                if(!child.isShown())continue;
                View found=findScrollable(child,down);
                if(found!=null)return found;
            }
        }
        return null;
    }
    private static WebView findWebView(View root){
        if(root instanceof WebView)return (WebView)root;
        if(root instanceof ViewGroup){
            ViewGroup group=(ViewGroup)root;
            for(int i=group.getChildCount()-1;i>=0;i--){
                View child=group.getChildAt(i);
                if(child.isShown()){
                    WebView found=findWebView(child);
                    if(found!=null)return found;
                }
            }
        }
        return null;
    }
    private void cancel(){
        tracking=false;intercepted=false;pending=null;
        if(recognizer!=null&&touch!=null&&!cancelling){
            MotionEvent event=MotionEvent.obtain(0,0,MotionEvent.ACTION_CANCEL,0,0,0);
            cancelling=true;
            try{touch.invoke(recognizer,event);}
            catch(ReflectiveOperationException|RuntimeException ignored){}
            finally{cancelling=false;event.recycle();}
        }
    }
}
