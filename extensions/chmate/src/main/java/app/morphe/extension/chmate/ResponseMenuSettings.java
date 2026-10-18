package app.morphe.extension.chmate;

import android.content.Context;
import android.view.Menu;
import java.lang.reflect.Field;
import java.util.List;
import java.util.HashSet;
import org.json.JSONObject;
import org.json.JSONArray;

/** Stable IDs shared by the native customization catalog and response actions. */
public final class ResponseMenuSettings {
    private ResponseMenuSettings() {}
    static final int[] IDS = {0x7e484b01, 0x7e484a01, 0x7e484a03, 75, 76, 0x7e000077, 0x7e000078};
    static final String[] NAMES={"投稿者を強調","レスを保存","過去レスを見る","ﾜｯﾁｮｲで検索","ﾜｯﾁｮｲ下4桁をNG登録","ﾜｯﾁｮｲ下4桁で検索","ﾜｯﾁｮｲ上4桁で検索"};
    static boolean defaultVisible(int id){for(int action:IDS)if(action==id)return id==76;return true;}
    private static int icon(int id) {
        if(id==75)return wacchoiSearchIcon();
        if(id==0x7e484a01)return ResponseArchive.archiveIcon(true);
        if(id==0x7e484a03)return ResponseArchive.archiveIcon(false);
        return android.R.drawable.ic_menu_more;
    }
    static int wacchoiSearchIcon(){
        Context context=Haiagaru.applicationContextForExtension();
        int icon=context==null?0:context.getResources().getIdentifier("haiagaru_wacchoi_search","drawable",context.getPackageName());
        return icon!=0?icon:android.R.drawable.ic_menu_search;
    }
    public static Object catalog(Object model) {
        try {
            Context context=Haiagaru.applicationContextForExtension();
            String version=context.getPackageManager().getPackageInfo(context.getPackageName(),0).versionName;
            boolean legacy=version.equals("0.8.10.191 dev");
            List<?> source=null;
            if(model instanceof List)source=(List<?>)model;
            else for(Field f:model.getClass().getDeclaredFields())if(List.class.isAssignableFrom(f.getType())) {f.setAccessible(true);source=(List<?>)f.get(model);break;}
            if(source==null||source.isEmpty())return model;
            @SuppressWarnings("unchecked") List<Object> list=(List<Object>)source;
            Class<?> spec=list.get(0).getClass();
            Field id=spec.getDeclaredField(legacy?"c":"a");id.setAccessible(true);
            Field title=spec.getDeclaredField("b");title.setAccessible(true);
            Field summary=spec.getDeclaredField(legacy?"e":"d");summary.setAccessible(true);
            HashSet<Integer> seen=new HashSet<>();
            java.util.Iterator<Object> iterator=list.iterator();
            while(iterator.hasNext()) {
                Object item=iterator.next();int key=id.getInt(item);
                if(!seen.add(key)){iterator.remove();continue;}
                if(key>=70&&key<=74){int text=context.getResources().getIdentifier("haiagaru_response_share_"+(key-70),"string",context.getPackageName());title.setInt(item,text);summary.setInt(item,text);}
            }
            for(int i=0;i<IDS.length;i++)if(seen.add(IDS[i])) {
                int text=context.getResources().getIdentifier("haiagaru_response_action_"+i,"string",context.getPackageName());
                list.add(spec.getConstructor(int.class,int.class,int.class,int.class,boolean.class).newInstance(IDS[i],icon(IDS[i]),text,text,defaultVisible(IDS[i])));
            }
        }catch(Exception error){android.util.Log.w("Haiagaru","Response toolbar catalog",error);}
        return model;
    }
    public static void catalog226(Object builder) {
        try {
            Context context = Haiagaru.applicationContextForExtension();
            Field field = builder.getClass().getDeclaredField("a"); field.setAccessible(true);
            @SuppressWarnings("unchecked") List<Object> list = (List<Object>) field.get(builder);
            Class<?> spec = Class.forName("jp.syoboi.a2chMate.feature.toolbar.ToolbarButtonSpec", false, builder.getClass().getClassLoader());
            Field id = spec.getDeclaredField("c"); id.setAccessible(true);
            Field title = spec.getDeclaredField("b"); title.setAccessible(true);
            Field summary = spec.getDeclaredField("e"); summary.setAccessible(true);
            HashSet<Integer> seen = new HashSet<>();
            java.util.Iterator<Object> rows = list.iterator();
            while(rows.hasNext()) {
                Object row=rows.next(); int key=id.getInt(row);
                if(!seen.add(key)) { rows.remove(); continue; }
                // These are five separate stock share destinations, not duplicate actions.
                if(key>=70 && key<=74) {
                    int text=context.getResources().getIdentifier("haiagaru_response_share_"+(key-70),"string",context.getPackageName());
                    title.setInt(row,text); summary.setInt(row,text);
                }
            }
            for(int i=0;i<IDS.length;i++) if(seen.add(IDS[i])) {
                int text=context.getResources().getIdentifier("haiagaru_response_action_"+i,"string",context.getPackageName());
                list.add(spec.getConstructor(int.class,int.class,int.class,int.class,boolean.class)
                        .newInstance(IDS[i],icon(IDS[i]),text,text,defaultVisible(IDS[i])));
            }
        } catch(Exception error) { android.util.Log.w("Haiagaru","Response toolbar catalog",error); }
    }
    static boolean visible(int id) {
        if((id==0x7e484a01||id==0x7e484a03)&&!FeatureSwitches.enabled("responseArchiveEnabled",true))return false;
        if((id==75||id==0x7e000077||id==0x7e000078)&&!FeatureSwitches.enabled("wacchoiSearchEnabled",true))return false;
        Context context=Haiagaru.applicationContextForExtension();
        if(context!=null){int override=context.getSharedPreferences("haiagaru_response_actions",0).getInt("visibility."+id,0);if(override!=0)return override==1;}
        // An explicit display switch is a visibility choice, not just another
        // veto layered over the (initially hidden) native catalog.
        if(context!=null&&(id==0x7e000077||id==0x7e000078)){
            android.content.SharedPreferences prefs=context.getSharedPreferences(WacchoiSearchOptions.PREFS,0);
            String key=WacchoiSearchOptions.menuKey(id);
            if(prefs.contains(key))return prefs.getBoolean(key,false);
        }
        JSONObject settings = savedSettings();
        if (settings == null) return defaultVisible(id);
        try {
            JSONArray rows = settings.optJSONArray("buttons");
            if (rows != null) for (int i = 0; i < rows.length(); i++) {
                JSONObject row = rows.optJSONObject(i);
                if (row != null && rowId(row) == id)
                    return row.optBoolean("visible", defaultVisible(id));
            }
        } catch (RuntimeException error) {
            android.util.Log.w("Haiagaru", "Response toolbar visibility", error);
        }
        return defaultVisible(id);
    }
    static void addSettings(android.widget.LinearLayout layout,android.app.Activity activity){
        android.widget.Button button=new android.widget.Button(activity);button.setText("レス長押し・ツールバー項目を個別設定");layout.addView(button);
        button.setOnClickListener(v->{android.widget.LinearLayout panel=new android.widget.LinearLayout(activity);panel.setOrientation(1);
            android.widget.TextView help=new android.widget.TextView(activity);help.setText("項目ごとに「標準のツールバー設定に従う／常に表示／常に非表示」を指定します。未設定の追加6項目は初期オフ。既存の配置順は変更しません。画面を開き直すと反映します。");panel.addView(help);
            android.content.SharedPreferences prefs=activity.getSharedPreferences("haiagaru_response_actions",0);
            for(int i=0;i<IDS.length;i++){final int id=IDS[i];final String name=NAMES[i];android.widget.Button row=new android.widget.Button(activity);Runnable update=()->{int mode=prefs.getInt("visibility."+id,0);row.setText(name+"："+(mode==1?"常に表示":mode==2?"常に非表示":"標準設定"));};update.run();panel.addView(row);
                row.setOnClickListener(w->new android.app.AlertDialog.Builder(activity).setTitle(name).setSingleChoiceItems(new String[]{"標準のツールバー設定に従う","常に表示","常に非表示"},prefs.getInt("visibility."+id,0),(d,n)->{prefs.edit().putInt("visibility."+id,n).apply();update.run();d.dismiss();}).setNegativeButton("キャンセル",null).show());
            }
            android.widget.ScrollView scroll=new android.widget.ScrollView(activity);scroll.addView(panel);new android.app.AlertDialog.Builder(activity).setTitle("レスメニューの項目別設定").setView(scroll).setPositiveButton("閉じる",null).show();
        });
    }

    /**
     * Native response actions are created before Haiagaru's actions. Supplying the
     * saved catalog index as MenuItem order lets Android interleave those later
     * additions at the position chosen in ChMate's toolbar settings.
     */
    static int order(int id, int fallback) {
        JSONObject settings = savedSettings();
        if (settings == null) return fallback;
        try {
            JSONArray rows = settings.optJSONArray("buttons");
            if (rows != null) for (int i = 0; i < rows.length(); i++) {
                JSONObject row = rows.optJSONObject(i);
                if (row == null || rowId(row) != id) continue;
                // ChMate normally persists order by array position. Some newer
                // settings writers also retain an explicit slot alongside it;
                // prefer that value so all added actions follow drag-reordering.
                if (row.has("position")) return Math.max(0, row.optInt("position", i));
                if (row.has("order")) return Math.max(0, row.optInt("order", i));
                return i;
            }
        } catch (RuntimeException error) {
            android.util.Log.w("Haiagaru", "Response toolbar order", error);
        }
        return fallback;
    }

    private static int rowId(JSONObject row) {
        Object raw = row.opt("id");
        if (raw instanceof Number) return ((Number) raw).intValue();
        if (raw instanceof String) {
            String value = ((String) raw).trim();
            try { return value.startsWith("0x") || value.startsWith("0X")
                    ? (int) Long.parseLong(value.substring(2), 16) : Integer.parseInt(value); }
            catch (NumberFormatException ignored) { }
        }
        return Integer.MIN_VALUE;
    }

    private static JSONObject savedSettings() {
        Context context=Haiagaru.applicationContextForExtension();
        if(context==null)return null;
        try {
            String saved=context.getSharedPreferences("resMenuDialog",0).getString("toolbarSettings","");
            return saved.isEmpty() ? null : new JSONObject(saved);
        } catch(Exception error) {
            android.util.Log.w("Haiagaru","Response toolbar settings",error);
            return null;
        }
    }
    public static void prepare(Object object) {
        if(!(object instanceof Menu))return;
        Menu menu=(Menu)object;
        // Native catalog entries are placeholders; bind fresh per-response callbacks below.
        for(int id:IDS)menu.removeItem(id);
    }
}
