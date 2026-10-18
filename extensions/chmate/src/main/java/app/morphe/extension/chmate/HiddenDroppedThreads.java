package app.morphe.extension.chmate;

import android.content.Context;
import java.lang.reflect.Field;
import java.util.*;

/** Display copies only; exact native STATE_DROP bit, never inferred from response counts. */
public final class HiddenDroppedThreads {
    static final String KEY="hideDroppedHomeThreads";
    private static final Map<Object,List<?>> originals=Collections.synchronizedMap(new IdentityHashMap<>());
    static boolean enabled(){Context c=Haiagaru.applicationContextForExtension();return c!=null&&c.getSharedPreferences(WacchoiSearchOptions.PREFS,0).getBoolean(KEY,false);}
    static boolean target(int mode){int id=Math.abs(mode);return id==101||id==102||id==103;}
    static boolean dropped(Object row)throws Exception{
        if(row==null)return false;
        String name=row.getClass().getName();String field;
        if(name.equals("o.onAdDisplayed$setIcon"))field="o";
        else if(name.equals("o.Yy2$open")||name.equals("o.getAvailabilityStatus$r8lambdawJ5MHcSJed_CjC7r4OWD0UxyJsQ"))field="l";
        else return false;
        Field state=row.getClass().getDeclaredField(field);state.setAccessible(true);
        return (state.getInt(row)&1)!=0;
    }
    static List<?> filter(List<?> source,int mode){
        return filter(source,mode,enabled());
    }
    static List<?> filter(List<?> source,int mode,boolean active){
        if(source==null||!target(mode)||!active)return source;
        try{ArrayList<Object> out=new ArrayList<>();for(Object row:source)if(!dropped(row))out.add(row);return out.size()==source.size()?source:out;}
        catch(Exception error){android.util.Log.w("Haiagaru","DAT-drop display filter left unchanged",error);return source;}
    }
    // Native legacy list wrappers are recreated during sorting. Remember their source to restore OFF.
    static List<?> source(Object wrapper,List<?> fallback){synchronized(originals){List<?> original=originals.get(wrapper);return original==null?fallback:original;}}
    static void remember(Object wrapper,List<?> source){synchronized(originals){if(originals.size()>=24)originals.clear();originals.put(wrapper,source);}}
    private HiddenDroppedThreads(){}
}
