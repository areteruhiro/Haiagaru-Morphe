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
    private static int icon(int id) {
        if(id==0x7e484a01)return ResponseArchive.archiveIcon(true);
        if(id==0x7e484a03)return ResponseArchive.archiveIcon(false);
        return android.R.drawable.ic_menu_more;
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
                list.add(spec.getConstructor(int.class,int.class,int.class,int.class,boolean.class).newInstance(IDS[i],icon(IDS[i]),text,text,true));
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
                        .newInstance(IDS[i],icon(IDS[i]),text,text,true));
            }
        } catch(Exception error) { android.util.Log.w("Haiagaru","Response toolbar catalog",error); }
    }
    static boolean visible(int id) {
        if((id==0x7e000077||id==0x7e000078)&&!WacchoiSearchOptions.menu(id))return false;
        JSONObject settings = savedSettings();
        if (settings == null) return true;
        try {
            JSONArray rows = settings.optJSONArray("buttons");
            if (rows != null) for (int i = 0; i < rows.length(); i++) {
                JSONObject row = rows.optJSONObject(i);
                if (row != null && rowId(row) == id)
                    return row.optBoolean("visible", true);
            }
        } catch (RuntimeException error) {
            android.util.Log.w("Haiagaru", "Response toolbar visibility", error);
        }
        return true;
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
