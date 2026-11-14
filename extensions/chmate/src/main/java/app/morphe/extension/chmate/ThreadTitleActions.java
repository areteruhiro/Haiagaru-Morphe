package app.morphe.extension.chmate;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import java.lang.reflect.Field;

/** Extends the native title dialog before its view is attached; native actions stay intact. */
public final class ThreadTitleActions {
    static final String SHOW_REFRESH="titleMenuShowRefresh",SHOW_WATCH="titleMenuShowWatch",SHOW_AUTH="titleMenuShowAuth";
    private static java.lang.ref.WeakReference<Object> pending=new java.lang.ref.WeakReference<>(null);
    public static View wrap(Object fragment,View original){
        if(original==null)return null;
        try{
            Activity a=Haiagaru.toolbarActivity(fragment);if(a==null)return original;
            String version=a.getPackageManager().getPackageInfo(a.getPackageName(),0).versionName;
            if(!"0.8.10.191 dev".equals(version)){
                // Material's ModalBottomSheet creates a separate Dialog window.
                // Decorating this Fragment's ComposeView would put controls behind it.
                pending=new java.lang.ref.WeakReference<>(fragment);return original;
            }
            return content(fragment,a,original);
        }catch(Exception error){android.util.Log.w("Haiagaru","Thread title actions unavailable",error);return original;}
    }
    private static final class MenuThread {
        final Object info;
        final ThreadWatchRules.Target target;
        MenuThread(Object info,ThreadWatchRules.Target target){this.info=info;this.target=target;}
    }
    private static MenuThread thread(Object fragment,Activity a)throws Exception{
        String version=a.getPackageManager().getPackageInfo(a.getPackageName(),0).versionName;
        boolean old=version.contains("191"), middle=version.contains("226");
        Bundle args=(Bundle)fragment.getClass().getMethod("getArguments").invoke(fragment);
        if(args==null)return null;
        Object info=args.get("threadUrl");
        if(info==null){Object packed=args.get("args");if(packed!=null)for(Field f:packed.getClass().getDeclaredFields())if(f.getType().getName().equals("jp.syoboi.a2chMate.client.BBSUrlInfo")){f.setAccessible(true);info=f.get(packed);break;}}
        if(info==null)return null;
        String server=(String)NativeFields.find(info.getClass(),old?"i":middle?"g":"f").get(info);
        String board=(String)NativeFields.find(info.getClass(),old?"b":middle?"f":"g").get(info);
        Object id=NativeFields.find(info.getClass(),middle?"c":"e").get(info);
        ReporterArchiveLookup.request(a,server,board,id,args.getString("threadTitle"));
        return new MenuThread(info,ThreadWatchRules.fromParts(server,board,id));
    }
    public static java.util.List<?> appendNativeGroups(java.util.List<?> original){
        Object fragment=pending.get();if(fragment==null||original==null)return original;
        try{
            if(!Boolean.TRUE.equals(fragment.getClass().getMethod("isAdded").invoke(fragment)))return original;
            Activity a=Haiagaru.toolbarActivity(fragment);if(a==null)return original;
            MenuThread menu=thread(fragment,a);if(menu==null)return original;
            java.util.ArrayList<String> labels=new java.util.ArrayList<>();
            java.util.ArrayList<Runnable> actions=new java.util.ArrayList<>();
            if(FeatureSwitches.enabled(SHOW_REFRESH,true)){
                String label=RefreshTargets.menuLabel(a,menu.info);
                if(label!=null){labels.add(label);actions.add(()->RefreshTargets.toggleFromMenu(a,menu.info));}
            }
            ThreadWatchRules.Target target=menu.target;
            if(target!=null&&FeatureSwitches.enabled(SHOW_WATCH,true)){
                labels.add("このスレの新着通知");actions.add(()->ThreadWatch.show(a,target));
            }
            if(target!=null&&FeatureSwitches.enabled(SHOW_AUTH,true)&&ReadingAuth.eligible(target.url)){
                labels.add("エッヂの閲覧認証");actions.add(()->ReadingAuth.open(a,target.url));
            }
            if(labels.isEmpty())return original;
            boolean middle=fragment.getClass().getName().equals("o.getDislikeManager");
            ClassLoader loader=fragment.getClass().getClassLoader();
            Class<?> row=loader.loadClass(middle?"o.getTtAdContainer$IconCompatParcelizer":"o.zzbbs$RemoteActionCompatParcelizer");
            Class<?> fn=loader.loadClass(middle?"o.binderDied":"o.zzcbx");
            Object unit=NativeFields.find(loader.loadClass(middle?"o.Ff11":"o.zzbwq"),middle?"c":"INSTANCE").get(null);
            Object icon=null;
            if(!middle){
                Field field=NativeFields.find(row,"d");
                for(Object group:original)if(group instanceof java.util.List){
                    for(Object item:(java.util.List<?>)group)if(row.isInstance(item)){icon=field.get(item);if(icon!=null)break;}
                    if(icon!=null)break;
                }
                if(icon==null)return original;
            }
            java.lang.reflect.Constructor<?> ctor=middle?row.getDeclaredConstructor(String.class,fn):
                row.getDeclaredConstructor(String.class,NativeFields.find(row,"d").getType(),fn,boolean.class,boolean.class);
            ctor.setAccessible(true);
            java.util.ArrayList<Object> added=new java.util.ArrayList<>();
            for(int i=0;i<labels.size();i++){
                Runnable action=actions.get(i);
                Object callback=java.lang.reflect.Proxy.newProxyInstance(loader,new Class<?>[]{fn},(self,method,args)->{
                    switch(method.getName()){
                        case "invoke": if(!a.isFinishing()&&!a.isDestroyed())action.run();return unit;
                        case "hashCode": return System.identityHashCode(self);
                        case "equals": return self==args[0];
                        default: return "Haiagaru thread title action";
                    }
                });
                added.add(middle?ctor.newInstance(labels.get(i),callback):ctor.newInstance(labels.get(i),icon,callback,false,false));
            }
            java.util.ArrayList<Object> groups=new java.util.ArrayList<>(original);
            groups.add(added);
            return groups;
        }catch(Exception error){android.util.Log.w("Haiagaru","Native thread title actions unavailable",error);return original;}
    }
    /** Compose versions use packed text units. Zero leaves the themed size in effect. */
    public static long compactLabelSize(String label){
        android.content.Context context=Haiagaru.applicationContextForExtension();
        if(context==null||label==null||label.length()<8)return 0;
        android.content.res.Resources res=context.getResources();
        android.util.DisplayMetrics dm=res.getDisplayMetrics();
        int screen=res.getConfiguration().screenWidthDp;
        if(screen<=0)return 0;
        android.text.TextPaint paint=new android.text.TextPaint();
        paint.setTextSize(android.util.TypedValue.applyDimension(android.util.TypedValue.COMPLEX_UNIT_SP,14,dm));
        float available=(screen-112)*dm.density; // Sheet padding, menu icon and text padding.
        if(available<=0)return 0;
        float measured=paint.measureText(label);
        if(measured<=available)return 0;
        float size=Math.max(11f,(float)Math.floor(28f*available/measured)/2f);
        return (1L<<32)|(Float.floatToRawIntBits(size)&0xffffffffL);
    }
    private static void fitLegacyLabels(View view){
        if(android.os.Build.VERSION.SDK_INT>=26&&view instanceof android.widget.Button){
            android.widget.TextView text=(android.widget.TextView)view;
            float scale=text.getResources().getDisplayMetrics().scaledDensity;
            int size=Math.round(text.getTextSize()/scale);
            if(text.getMaxLines()==1){
                text.setSingleLine(false);
                text.setMaxLines(2);
                text.setEllipsize(android.text.TextUtils.TruncateAt.END);
                if(size>11)text.setAutoSizeTextTypeUniformWithConfiguration(11,size,1,android.util.TypedValue.COMPLEX_UNIT_SP);
            }
        }
        if(view instanceof android.view.ViewGroup){
            android.view.ViewGroup parent=(android.view.ViewGroup)view;
            for(int i=0;i<parent.getChildCount();i++)fitLegacyLabels(parent.getChildAt(i));
        }
    }
    private static View content(Object fragment,Activity a,View original)throws Exception{
            MenuThread menu=thread(fragment,a);if(menu==null)return original;
            Object info=menu.info;
            ThreadWatchRules.Target t=menu.target;
            // 191's native dialog is a ScrollView with its own menu container.
            // Add the existing actions to that container so they scroll with native entries.
            LinearLayout nativeMenu=null;
            if(original instanceof android.widget.ScrollView){
                android.view.ViewGroup scroll=(android.view.ViewGroup)original;
                if(scroll.getChildCount()==1&&scroll.getChildAt(0) instanceof LinearLayout)
                    nativeMenu=(LinearLayout)scroll.getChildAt(0);
            }
            LinearLayout box=new LinearLayout(a);box.setOrientation(LinearLayout.VERTICAL);
            if(nativeMenu==null)box.addView(original,new LinearLayout.LayoutParams(-1,-2));
            LinearLayout controls=new LinearLayout(a);controls.setOrientation(LinearLayout.VERTICAL);
            LinearLayout actions=new LinearLayout(a);
            android.widget.CheckBox refresh=FeatureSwitches.enabled(SHOW_REFRESH,true)?RefreshTargets.control(a,info):null;
            if(refresh!=null)controls.addView(refresh,new LinearLayout.LayoutParams(-1,-2));
            if(t!=null){
            if(FeatureSwitches.enabled(SHOW_WATCH,true)){
            Button watch=new Button(a);watch.setAllCaps(false);watch.setText("このスレの新着通知");actions.addView(watch,new LinearLayout.LayoutParams(0,-2,1));
            watch.setOnClickListener(v->ThreadWatch.show(a,t));
            }
            if(FeatureSwitches.enabled(SHOW_AUTH,true)&&ReadingAuth.eligible(t.url)){
                Button auth=new Button(a);auth.setText("エッヂの閲覧認証");auth.setAllCaps(false);actions.addView(auth,new LinearLayout.LayoutParams(0,-2,1));
                auth.setOnClickListener(v->ReadingAuth.open(a,t.url));
            }
            }
            if(actions.getChildCount()>0)controls.addView(actions,new LinearLayout.LayoutParams(-1,-2));
            fitLegacyLabels(original);
            fitLegacyLabels(controls);
            if(nativeMenu!=null){
                if(controls.getChildCount()>0){controls.setBackgroundColor(android.graphics.Color.TRANSPARENT);nativeMenu.addView(controls,new LinearLayout.LayoutParams(-1,-2));}
                return original;
            }
            if(controls.getChildCount()==0){box.removeView(original);return original;}
            box.addView(controls,new LinearLayout.LayoutParams(-1,-2));
            return box;
    }
    private ThreadTitleActions(){}
}
