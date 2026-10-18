package app.morphe.extension.chmate;

import android.content.Context;
import java.lang.reflect.*;
import java.util.*;

/** Synthetic records only: no database updates or real browsing-history deletion. */
public final class HistoryTest {
    static int checks;
    static void check(boolean value, String label) { if (!value) throw new AssertionError(label); checks++; }
    static Field field(Object o,String n)throws Exception { Field f=o.getClass().getDeclaredField(n); f.setAccessible(true); return f; }
    static Object row(Class<?> type, Object board, String title, long visited)throws Exception {
        for(Constructor<?> c:type.getDeclaredConstructors()) {
            Class<?>[] types=c.getParameterTypes();
            if(types.length<12 || types[0]!=long.class || types[1]!=String.class || types[types.length-1].getName().startsWith("o.zz"))continue;
            Object[] values=new Object[types.length];
            for(int i=0;i<types.length;i++) {
                if(types[i]==long.class)values[i]=Long.valueOf(i==7?visited:1);
                else if(types[i]==int.class)values[i]=Integer.valueOf(0);
                else if(types[i]==byte.class)values[i]=Byte.valueOf((byte)0);
                else if(types[i]==boolean.class)values[i]=Boolean.FALSE;
            }
            values[1]=title;values[2]=board;c.setAccessible(true);return c.newInstance(values);
        }
        throw new AssertionError("Native row constructor missing");
    }
    static Object board(Class<?> type,String value)throws Exception {
        for(Method m:type.getDeclaredMethods()) {
            if(Modifier.isStatic(m.getModifiers()) && m.getReturnType()==type && Arrays.equals(m.getParameterTypes(),new Class<?>[]{String.class})) {
                m.setAccessible(true);return m.invoke(null,value);
            }
        }
        try {
            Constructor<?> one=type.getDeclaredConstructor(String.class);one.setAccessible(true);return one.newInstance(value);
        } catch(NoSuchMethodException ignored) { }
        int slash=value.indexOf('/');
        return type.getConstructor(String.class,String.class,String.class,String.class)
                .newInstance(value.substring(0,slash),value.substring(slash+1),value,value);
    }
    public static String run(Context context)throws Exception {
        checks=0;
        String version=context.getPackageManager().getPackageInfo(context.getPackageName(),0).versionName;
        boolean v191=version.contains("191"),v226=version.contains("226");
        String rowType=v191?"o.onAdDisplayed$setIcon":v226?"o.Yy2$open":"o.getAvailabilityStatus$r8lambdawJ5MHcSJed_CjC7r4OWD0UxyJsQ";
        ClassLoader loader=context.getClassLoader();
        Class<?> boardType=Class.forName(v191?"jp.syoboi.a2chMate.client.BBSUrlInfo$BoardID":"jp.syoboi.a2chMate.client.BoardID",true,loader);
        Class<?> type=Class.forName(rowType,true,loader);
        Object a=board(boardType,"egg.5ch.io/android"),b=board(boardType,"greta.5ch.io/poverty"),other=board(boardType,"other.example/android");
        check(!a.toString().equals(other.toString()),"Native BoardID keeps hosts");
        long now=System.currentTimeMillis();
        Object a1=row(type,a,"a-new",now),a2=row(type,a,"a-old",now-5000),b1=row(type,b,"b-new",now-1000),c1=row(type,other,"other",now-2000);
        Object old=row(type,b,"old",now-8L*86400000),never=row(type,a,"never",0);
        List<Object> source=Arrays.asList(a2,b1,c1,a1,old,never);
        android.content.SharedPreferences prefs=context.getSharedPreferences("io.github.areteruhiro.chmate.haiagaru.ui-config",0);
        Map<String,?> before=prefs.getAll();
        try {
            prefs.edit().putBoolean("groupBrowsingHistoryByBoard",true).putInt("browsingHistoryDays",2).commit();
            BrowsingHistory.initialize(context);
            List<?> result=BrowsingHistory.transform(source,103);
            check(result.equals(Arrays.asList(a1,a2,b1,c1)),"Native row fields: recency grouping, date filtering");
            check(source.size()==6 && source.get(0)==a2,"Native input untouched");
            check(BrowsingHistory.transform(source,100)==source,"All/favorite screen unchanged");
            check(BrowsingHistory.transform(source,101)==source,"Unread screen unchanged");
            check(BrowsingHistory.transform(source,-101)==source,"Reverse unread screen unchanged");
            check(BrowsingHistory.transform(source,102)==source,"Thread screen unchanged");
            check(BrowsingHistory.transform(source,104)==source,"Posting history unchanged");
            check(BrowsingHistory.transform(source,107)==source,"Board screen unchanged");
            check(BrowsingHistory.transform(source,-103).equals(result),"Signed history sort");
            prefs.edit().putInt("browsingHistoryDays",0).commit();
            check(BrowsingHistory.transform(source,103).equals(Arrays.asList(a1,a2,never,b1,old,c1)),"All-period grouping retains zero-timestamp legacy rows");
            prefs.edit().putBoolean("groupBrowsingHistoryByBoard",false).commit();
            check(BrowsingHistory.transform(source,103)==source,"Feature off retains native reference");
            prefs.edit().putBoolean("groupBrowsingHistoryByBoard",true).putInt("browsingHistoryDays",2).commit();
            if(v191 || v226) {
                String listType=v191?"o.onAdDisplayed$setBackgroundResource":"o.Yy2$CheckResult";
                Class<?> listClass=Class.forName(listType,true,loader);
                Object input=listClass.getConstructor(List.class).newInstance(source);
                String adapterType=v191?"o.r8lambdaXZREaGE4vvLAA2ZOtsKBd3knAPA":"o.bea1";
                Object adapter=Class.forName(adapterType,true,loader).getConstructor(Context.class).newInstance(context);
                field(adapter,v191?"i":"g").set(adapter,input);
                field(adapter,v191?"g":"d").set(adapter,input);
                Object fragment=Class.forName(v191?"o.r8lambdaTb_p0z6z2AqSZIga1YhmAVmiTPk":"jp.syoboi.a2chMate.fragment.HomeFragment",true,loader).getConstructor().newInstance();
                field(fragment,v191?"e":"i").set(fragment,adapter);
                BrowsingHistory.applyLegacy(fragment,101);
                check(field(adapter,v191?"g":"d").get(adapter)==input,"Legacy unread list and native sort retained");
                BrowsingHistory.applyLegacy(fragment,103);
                check(((List<?>)field(adapter,v191?"i":"g").get(adapter)).equals(result),"Legacy filter-source native subclass updated");
                check(((List<?>)field(adapter,v191?"g":"d").get(adapter)).equals(result),"Legacy display native subclass updated");
                check(((List<?>)input).size()==6,"Legacy original model retained");
            } else {
                Class<?> data=Class.forName("o.SafeParcelableClass",true,loader);
                Class<?> options=Class.forName("o.ApiApiOptionsHasAccountOptions",true,loader);
                Object opt=options.getConstructors()[0].newInstance(1f,0,false,false,false,false,false,false);
                Constructor<?> ctor=data.getConstructor(List.class,int.class,boolean.class,options,int.class,int.class);
                Object state=ctor.newInstance(source,2,false,opt,103,0);
                check(((List<?>)field(state,"a").get(state)).equals(result),"242 native AdapterData constructor connected");
                check(((Integer)field(state,"b").get(state))==103,"242 mode retained");
                for(int mode:new int[]{101,-101,102,104,105,106,107}) {
                    for(int sort=0;sort<5;sort++) {
                        List<Object> ordered=new ArrayList<>(source);
                        if((sort&1)!=0)Collections.reverse(ordered);
                        Collections.rotate(ordered,sort);
                        Object unread=ctor.newInstance(ordered,2,sort==0,opt,mode,sort);
                        check(field(unread,"a").get(unread)==ordered,"242 non-history native order/count retained: "+mode+"/"+sort);
                    }
                }
                Object favorites=ctor.newInstance(source,2,false,opt,100,0);
                check(field(favorites,"a").get(favorites)==source,"242 other screen untouched at native constructor");
            }
        } finally {
            android.content.SharedPreferences.Editor restore=prefs.edit();
            for(String key:new String[]{"groupBrowsingHistoryByBoard","browsingHistoryDays"}) {
                if(!before.containsKey(key))restore.remove(key);
                else if(before.get(key) instanceof Boolean)restore.putBoolean(key,(Boolean)before.get(key));
                else restore.putInt(key,(Integer)before.get(key));
            }
            restore.commit();
        }
        return "PASS "+version+" "+checks+" native history tests";
    }
}
