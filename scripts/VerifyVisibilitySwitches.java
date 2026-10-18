package app.morphe.extension.chmate;

import android.content.*;
import java.lang.reflect.*;
import java.util.*;

/** Synthetic preferences only; never reads or changes the user's app settings. */
public final class VerifyVisibilitySwitches {
    private static final Map<String,Map<String,Object>> STORE=new HashMap<>();
    private static int checks;
    private static void check(boolean ok,String name){if(!ok)throw new AssertionError(name);checks++;System.out.println("PASS "+name);}
    private static Map<String,Object> values(String name){return STORE.computeIfAbsent(name,k->new HashMap<>());}
    private static SharedPreferences prefs(String name){return (SharedPreferences)Proxy.newProxyInstance(SharedPreferences.class.getClassLoader(),new Class[]{SharedPreferences.class},(p,m,a)->{
        Map<String,Object> map=values(name);
        if(m.getName().equals("contains"))return map.containsKey(a[0]);
        if(m.getName().startsWith("get"))return map.getOrDefault(a[0],a[1]);
        throw new UnsupportedOperationException(m.getName());
    });}
    public static final class Item {public int id=0x7e000005;public boolean shown;Item(boolean v){shown=v;}}
    public static void main(String[] args)throws Exception{
        android.os.Looper.prepareMainLooper();
        Context context=new ContextWrapper(null){public SharedPreferences getSharedPreferences(String n,int m){return prefs(n);}};
        Field app=Haiagaru.class.getDeclaredField("applicationContext");app.setAccessible(true);app.set(null,context);
        for(int id:new int[]{0x7e000077,0x7e000078}){
            STORE.clear();String key=WacchoiSearchOptions.menuKey(id);
            check(!ResponseMenuSettings.visible(id),"unconfigured action defaults OFF "+id);
            values("resMenuDialog").put("toolbarSettings","{\"buttons\":[{\"id\":"+id+",\"visible\":false}]}");
            values(WacchoiSearchOptions.PREFS).put(key,true);
            check(ResponseMenuSettings.visible(id),"explicit ON beats saved hidden row "+id);
            values(WacchoiSearchOptions.PREFS).put(key,false);
            check(!ResponseMenuSettings.visible(id),"explicit OFF hides "+id);
            values("haiagaru_response_actions").put("visibility."+id,1);
            check(ResponseMenuSettings.visible(id),"individual show override "+id);
            values("haiagaru_response_actions").put("visibility."+id,2);
            check(!ResponseMenuSettings.visible(id),"individual hide override "+id);
            values("haiagaru_response_actions").clear();values(WacchoiSearchOptions.PREFS).remove(key);
            values("resMenuDialog").put("toolbarSettings","{\"buttons\":[{\"id\":"+id+",\"visible\":true}]}");
            check(ResponseMenuSettings.visible(id),"native ON honored when no override "+id);
            values(WacchoiSearchOptions.PREFS).put("wacchoiSearchEnabled",false);
            check(!ResponseMenuSettings.visible(id),"parent OFF wins "+id);
        }
        STORE.clear();
        check(!ToolbarIcons.visible(context,0x7e000005)&&!ToolbarIcons.visible(context,0x7e000006),"search toolbar defaults OFF");
        for(int i=0;i<4;i++)check(ToolbarIcons.visible(context,0x7e000001+i),"existing toolbar default retained "+i);
        values(ToolbarIcons.PREFS).put("visible.4",true);
        check(ToolbarIcons.visible(context,0x7e000005),"toolbar explicit ON");
        values(WacchoiSearchOptions.PREFS).put("wacchoiSearchEnabled",false);
        check(!ToolbarIcons.visible(context,0x7e000005),"toolbar honors disabled parent");
        for(String key:new String[]{"compactQuickFilters","threadCommandsEnabled","imageViewerRotationButton","wacchoiSearchEnabled"})
            check(SettingsEffects.needsRestart(key),"restart classification "+key);
        for(String key:new String[]{"forceHttps","wacchoiSearchHistory","videoSwipeVolume","wacchoiLowerMenu"})
            check(!SettingsEffects.needsRestart(key),"live/reopen classification "+key);
        SettingsEffects.changed(context,"forceHttps",true);check(HttpsTransport.shouldUpgrade("http"),"index toggle updates live HTTPS transport ON");
        SettingsEffects.changed(context,"forceHttps",false);check(!HttpsTransport.shouldUpgrade("http"),"index toggle updates live HTTPS transport OFF");
        System.out.println("PASS: "+checks+" visibility and setting effect checks");System.exit(0);
    }
}
