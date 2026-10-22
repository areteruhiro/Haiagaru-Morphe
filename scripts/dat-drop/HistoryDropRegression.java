package app.morphe.extension.chmate;

import android.content.Context;
import java.lang.reflect.*;
import java.util.*;

/** Synthetic native rows only. No history DB changes or network needed. */
public final class HistoryDropRegression {
    public static String run(Context context)throws Exception {
        String version=context.getPackageManager().getPackageInfo(context.getPackageName(),0).versionName;
        boolean v191=version.contains("191"),v226=version.contains("226");
        Class<?> type=Class.forName(v191?"o.onAdDisplayed$setIcon":v226?"o.Yy2$open":"o.getAvailabilityStatus$r8lambdawJ5MHcSJed_CjC7r4OWD0UxyJsQ",true,context.getClassLoader());
        Object board=DroppedThreadsAndroidTest.board(context);
        Object dropped=DroppedThreadsAndroidTest.row(type,board,1999000123L,1,v191);
        Object stale=DroppedThreadsAndroidTest.row(type,board,1999000123L,0,v191);
        Object broken=DroppedThreadsAndroidTest.row(type,board,1999000124L,0,v191);
        DroppedThreadsAndroidTest.field(broken,v191?"g":v226?"b":"e").set(broken,null);
        Class<?> filter=Class.forName("app.morphe.extension.chmate.HiddenDroppedThreads",true,context.getClassLoader());
        Field cache=filter.getDeclaredField("nativeStatus");cache.setAccessible(true);
        Map map=(Map)cache.get(null);Map saved;
        synchronized(map){saved=new LinkedHashMap(map);}
        try {
            List<?> out=(List<?>)DroppedThreadsAndroidTest.call(context,"HiddenDroppedThreads","filter",new Class<?>[]{List.class,int.class,boolean.class},Arrays.asList(dropped,broken),103,true);
            if(out.contains(dropped)||!out.contains(broken))throw new AssertionError("One bad URL cancels filtering of other history rows");
            out=(List<?>)DroppedThreadsAndroidTest.call(context,"HiddenDroppedThreads","filter",new Class<?>[]{List.class,int.class,boolean.class},Arrays.asList(dropped,stale),103,true);
            if(!out.isEmpty())throw new AssertionError("Stale zero-flag history copy erases positive native DAT status");
            List<Object> source=Arrays.asList(stale);
            out=(List<?>)DroppedThreadsAndroidTest.call(context,"HiddenDroppedThreads","filter",new Class<?>[]{List.class,int.class,boolean.class},source,103,true);
            if(!out.isEmpty())throw new AssertionError("Positive native evidence is lost across history refresh");
            if(DroppedThreadsAndroidTest.call(context,"HiddenDroppedThreads","filter",new Class<?>[]{List.class,int.class,boolean.class},source,103,false)!=source)throw new AssertionError("OFF does not restore source");
            if(DroppedThreadsAndroidTest.field(stale,v191?"o":"l").getInt(stale)!=0)throw new AssertionError("Native flags changed");
            return "PASS "+version+" 5 history regression checks";
        } finally { synchronized(map){map.clear();map.putAll(saved);} }
    }
}
