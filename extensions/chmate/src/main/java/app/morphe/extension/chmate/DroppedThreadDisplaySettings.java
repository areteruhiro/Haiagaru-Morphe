package app.morphe.extension.chmate;

import android.content.*;
import android.widget.*;
import java.lang.reflect.*;
import java.util.*;

/** Native display-dialog control sharing the same preference as Haiagaru settings. */
final class DroppedThreadDisplaySettings {
    static final String LABEL="DAT落ちスレを非表示（未読・履歴・スレ）";
    private static int label242;
    private static Object state242;
    static void save(boolean on){Context c=Haiagaru.applicationContextForExtension();if(c!=null)c.getSharedPreferences(WacchoiSearchOptions.PREFS,0).edit().putBoolean(HiddenDroppedThreads.KEY,on).apply();}
    static void legacy(LinearLayout panel,CheckBox template){
        for(int i=0;i<panel.getChildCount();i++)if("haiagaru.hideDropped".equals(panel.getChildAt(i).getTag()))return;
        CheckBox view=new CheckBox(template.getContext());view.setTag("haiagaru.hideDropped");view.setText(LABEL);view.setTextColor(template.getCurrentTextColor());view.setTextSize(template.getTextSize()/template.getResources().getDisplayMetrics().scaledDensity);view.setButtonTintList(template.getButtonTintList());view.setPadding(template.getPaddingLeft(),template.getPaddingTop(),template.getPaddingRight(),template.getPaddingBottom());view.setChecked(HiddenDroppedThreads.enabled());
        view.setOnCheckedChangeListener((v,on)->{save(on);Haiagaru.refreshBoardListAfterReadSortChange();});panel.addView(view,new LinearLayout.LayoutParams(-1,-2));
    }
    static void append242(List<Object> items,ClassLoader loader)throws Exception{
        Context c=Haiagaru.applicationContextForExtension();label242=c.getResources().getIdentifier("haiagaru_hide_dropped_threads","string",c.getPackageName());if(label242==0)throw new IllegalStateException("Missing DAT-drop label");
        Class<?> row=loader.loadClass("o.zzaxj"),action=loader.loadClass("o.zzcbx");Object unit=loader.loadClass("o.zzbwq").getField("INSTANCE").get(null);
        Field label=row.getDeclaredField("b");label.setAccessible(true);for(Object item:items)if(row.isInstance(item)&&label.getInt(item)==label242)return;
        Object callback=Proxy.newProxyInstance(loader,new Class[]{action},(p,m,args)->{if(!m.getName().equals("invoke")){if(m.getName().equals("hashCode"))return System.identityHashCode(p);if(m.getName().equals("equals"))return p==args[0];return "HideDroppedThreads";}
            save(!HiddenDroppedThreads.enabled());if(state242!=null)state242.getClass().getMethod("c",Object.class).invoke(state242,HiddenDroppedThreads.enabled());Haiagaru.refreshBoardListAfterReadSortChange();return unit;});
        Constructor<?> ctor=row.getDeclaredConstructor(boolean.class,int.class,action);ctor.setAccessible(true);items.add(ctor.newInstance(HiddenDroppedThreads.enabled(),label242,callback));
    }
    static boolean handles242(int label){return label!=0&&label==label242;}
    static boolean checked242(int label,boolean original,ClassLoader loader)throws Exception{
        if(!handles242(label))return original;
        if(state242==null)state242=loader.loadClass("o.b7bb").getMethod("b",Object.class).invoke(null,HiddenDroppedThreads.enabled());
        boolean saved=HiddenDroppedThreads.enabled();
        if((Boolean)state242.getClass().getMethod("c").invoke(state242)!=saved)state242.getClass().getMethod("c",Object.class).invoke(state242,saved);
        return (Boolean)state242.getClass().getMethod("c").invoke(state242);
    }
    private DroppedThreadDisplaySettings(){}
}
