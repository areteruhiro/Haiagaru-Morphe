package app.morphe.extension.chmate;

import android.app.Activity;
import android.content.*;
import android.view.*;
import android.widget.*;
import java.util.*;
import org.json.*;

/** Display-only HOME ordering. Never rewrites bookmarks, stars or read positions. */
public final class HomeThreadOrder {
    private static final String PREFS="haiagaru.home-thread-order";
    private static final int MENU=0x48690133;
    private static int activeDisplayMode;
    private static final Map<Integer,List<?>> latest=new HashMap<>();
    private static final Map<Object,Integer> modes=new WeakHashMap<>();
    private static final ThreadLocal<Integer> sortingMode=new ThreadLocal<>();
    public static void selected(Object owner,int mode){
        synchronized(modes){modes.put(owner,Math.abs(mode));}
        sortingMode.set(Math.abs(mode));
    }
    static int logicalMode(int mode){
        Integer selected=sortingMode.get();
        return Math.abs(mode)==102&&selected!=null&&(selected==108||selected==109)?selected:Math.abs(mode);
    }
    static int mode(Object fragment){
        synchronized(modes){Integer value=modes.get(fragment);if(value!=null)return value;}
        try{Object model=fragment.getClass().getMethod("m").invoke(fragment);synchronized(modes){Integer value=modes.get(model);if(value!=null)return value;}}catch(Exception ignored){}
        return 0;
    }
    private static SharedPreferences prefs(){return Haiagaru.applicationContextForExtension().getSharedPreferences(PREFS,0);}
    private static boolean target(int mode){int m=Math.abs(mode);return m==100||m==101||m==102||m==103||m==108||m==109;}
    static String key(Object row){
        if(row==null)return null;String type=row.getClass().getName(),board,id;
        if(type.equals("o.onAdDisplayed$setIcon")){board="g";id="h";}
        else if(type.equals("o.Yy2$open")){board="b";id="g";}
        else if(type.equals("o.getAvailabilityStatus$r8lambdawJ5MHcSJed_CjC7r4OWD0UxyJsQ")){board="e";id="b";}
        else return null;
        try{Object b=NativeFields.find(row.getClass(),board).get(row);long thread=NativeFields.find(row.getClass(),id).getLong(row);return b==null||thread<=0?null:b.toString()+"\n"+thread;}catch(Exception unavailable){return null;}
    }
    static List<?> apply(List<?> source,int mode){
        if(source==null||!target(mode)||Haiagaru.applicationContextForExtension()==null)return source;
        activeDisplayMode=mode;
        synchronized(latest){latest.put(mode,new ArrayList<>(source));}
        Map<String,Integer> ranks=new HashMap<>();try{JSONArray order=new JSONArray(prefs().getString("order:"+Math.abs(mode),"[]"));for(int i=0;i<order.length();i++)ranks.put(order.getString(i),i);}catch(JSONException ignored){}
        Set<String> pinned=prefs().getStringSet("pins:"+Math.abs(mode),Collections.emptySet());if(ranks.isEmpty()&&pinned.isEmpty())return source;
        // Manual order crosses board/date groups; omit headings which no longer
        // describe the following thread. Native data and default sorting stay intact.
        ArrayList<Object> result=new ArrayList<>();
        Map<Object,String> keys=new IdentityHashMap<>();
        for(Object row:source){String key=key(row);if(key!=null){result.add(row);keys.put(row,key);}}
        result.sort(Comparator.comparingInt((Object row)->pinned.contains(keys.get(row))?0:1)
                .thenComparingInt(row->ranks.getOrDefault(keys.get(row),Integer.MAX_VALUE)));
        return result;
    }
    public static void menu(Object fragment,Menu menu){
        if(menu==null)return;MenuItem item=menu.findItem(MENU);if(item==null){item=menu.add(Menu.NONE,MENU,Menu.NONE,"並び替える");item.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);item.setOnMenuItemClickListener(v->{show(Haiagaru.toolbarActivity(fragment),mode(fragment));return true;});}item.setVisible(target(mode(fragment)));
    }
    static void show(Activity activity,final int mode){
        if(activity==null||activity.isFinishing())return;List<?> source;synchronized(latest){source=latest.get(mode);}
        if(source==null){Toast.makeText(activity,"HOMEのスレタブを表示してから開いてください",Toast.LENGTH_LONG).show();return;}
        List<?> sorted=apply(source,mode);ArrayList<Object> rows=new ArrayList<>();HashSet<String> seen=new HashSet<>();for(Object row:sorted){String key=key(row);if(key!=null&&seen.add(key))rows.add(row);}
        if(rows.isEmpty()){Toast.makeText(activity,"並び替えるスレがありません",Toast.LENGTH_LONG).show();return;}
        ArrayList<String> order=new ArrayList<>();for(Object row:rows)order.add(key(row));Map<String,String> titles=new HashMap<>();for(Object row:rows){String key=key(row);try{String type=row.getClass().getName();titles.put(key,String.valueOf(NativeFields.find(row.getClass(),type.equals("o.onAdDisplayed$setIcon")?"e":type.equals("o.Yy2$open")?"k":"m").get(row)));}catch(Exception ignored){titles.put(key,key);}}Set<String> pinned=new HashSet<>(prefs().getStringSet("pins:"+Math.abs(mode),Collections.emptySet()));
        LinearLayout box=new LinearLayout(activity);box.setOrientation(1);TextView help=new TextView(activity);help.setText("スレを長押しして上下移動・ピン留めできます。ピン留めしたスレは先頭へ表示します。変更は保存で確定します。");box.addView(help);
        ListView list=new ListView(activity);box.addView(list,new LinearLayout.LayoutParams(-1,(int)(420*activity.getResources().getDisplayMetrics().density)));
        Runnable refresh=()->{ArrayList<String> labels=new ArrayList<>();for(String key:order){String title=titles.get(key);labels.add((pinned.contains(key)?"📌 ":"")+title);}list.setAdapter(new ArrayAdapter<>(activity,android.R.layout.simple_list_item_1,labels));};
        HaiagaruDialog dialog=new HaiagaruDialog.Builder(activity).setTitle("HOMEのスレを並び替える").setView(box).setNegativeButton("キャンセル",null).setNeutralButton("標準の順番",(d,w)->{prefs().edit().remove("order:"+Math.abs(mode)).remove("pins:"+Math.abs(mode)).apply();BrowsingHistory.refreshVisible(activity);}).setPositiveButton("保存",(d,w)->{prefs().edit().putString("order:"+Math.abs(mode),new JSONArray(order).toString()).putStringSet("pins:"+Math.abs(mode),pinned).apply();BrowsingHistory.refreshVisible(activity);}).create();
        list.setOnItemLongClickListener((parent,view,position,id)->{String key=order.get(position);new HaiagaruDialog.Builder(activity).setTitle("スレの位置を変更").setItems(new String[]{"一つ上へ","一つ下へ",pinned.contains(key)?"ピン留めを解除":"ピン留め"},(d,action)->{int at=order.indexOf(key);if(action==0&&at>0)Collections.swap(order,at,at-1);if(action==1&&at<order.size()-1)Collections.swap(order,at,at+1);if(action==2){if(!pinned.remove(key))pinned.add(key);}order.sort(Comparator.comparingInt(k->pinned.contains(k)?0:1));refresh.run();dialog.refresh();}).setNegativeButton("閉じる",null).show();return true;});refresh.run();dialog.show();
    }
    static View titleControl(Activity activity,String server,String board,Object id){
        final int mode=activeDisplayMode;List<?> source;synchronized(latest){source=latest.get(mode);}
        if(source==null||!target(mode)||server==null||board==null)return null;
        String suffix="/"+board+"\n"+id,wanted=null;
        ArrayList<String> order=new ArrayList<>();
        for(Object row:apply(source,mode)){String key=key(row);if(key==null)continue;order.add(key);
            String normalized=key.replace("%2F","/").replace("%2f","/");
            if(normalized.endsWith(suffix)&&normalized.substring(0,normalized.length()-suffix.length()).replaceFirst("^https?://","").equals(server))wanted=key;}
        if(wanted==null)return null;
        final String selected=wanted;Button button=new Button(activity);button.setText("HOMEでの位置を変更");button.setAllCaps(false);
        button.setOnClickListener(v->{Set<String> pins=new HashSet<>(prefs().getStringSet("pins:"+mode,Collections.emptySet()));
            new HaiagaruDialog.Builder(activity).setTitle("HOMEでの位置を変更").setItems(new String[]{"一つ上へ","一つ下へ",pins.contains(selected)?"ピン留めを解除":"ピン留め"},(dialog,action)->{
                int at=order.indexOf(selected);if(action==0&&at>0)Collections.swap(order,at,at-1);if(action==1&&at+1<order.size())Collections.swap(order,at,at+1);
                if(action==2){if(!pins.remove(selected))pins.add(selected);}
                prefs().edit().putString("order:"+mode,new JSONArray(order).toString()).putStringSet("pins:"+mode,pins).apply();BrowsingHistory.refreshVisible(activity);
                Toast.makeText(activity,"HOMEの表示順を変更しました",Toast.LENGTH_SHORT).show();
            }).setNegativeButton("閉じる",null).show();});return button;
    }
    private HomeThreadOrder(){}
}
