package app.morphe.extension.chmate;

import android.content.Context;
import android.content.SharedPreferences;
import android.app.Activity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

/** Exact normal-home hooks; the recent-thread popup does not enter these hooks. */
public final class BrowsingHistory {
    private static volatile Context context;
    private static final String PREFS = "io.github.areteruhiro.chmate.haiagaru.ui-config";
    private static final List<Display> displays=new ArrayList<>();
    private static final List<Legacy> legacyFragments=new ArrayList<>();
    private static final class Display {
        final WeakReference<List<Object>> output;final List<?> source;final int mode;
        Display(List<Object> o,List<?> s,int m){output=new WeakReference<>(o);source=s;mode=m;}
    }
    private static final class Legacy {
        final WeakReference<Object> fragment;final int mode;
        Legacy(Object f,int m){fragment=new WeakReference<>(f);mode=m;}
    }
    private BrowsingHistory() { }
    static void initialize(Context value) { context = value.getApplicationContext(); }
    static boolean enabled() { return context != null && prefs().getBoolean("groupBrowsingHistoryByBoard", true); }
    static int days() { return context == null ? 2 : Math.max(0, Math.min(36500, prefs().getInt("browsingHistoryDays", 2))); }
    private static SharedPreferences prefs() { return context.getSharedPreferences(PREFS, 0); }
    private static Field field(Object owner, String name) throws Exception {
        Field value = owner.getClass().getDeclaredField(name); value.setAccessible(true); return value;
    }
    public static List<?> transform(List<?> input, int mode) {
        List<?> result=transformValues(input,mode);
        if(input==null||!HiddenDroppedThreads.target(mode))return result;
        // A display-owned copy lets ON/OFF refresh without editing the native model.
        ArrayList<Object> copy=new ArrayList<>(result);
        synchronized(displays){
            displays.removeIf(d->d.output.get()==null);
            if(displays.size()>=32)displays.remove(0);
            displays.add(new Display(copy,input,mode));
        }
        return copy;
    }
    private static List<?> transformValues(List<?> input,int mode){
        input=HiddenDroppedThreads.filter(input,mode);
        // Native HOME IDs: 101 = unread, 103 = browsing history, 104 = posted.
        // Never override unread sorting (including its native board-sort option).
        if (input == null || Math.abs(mode) != 103 || !enabled()) return input;
        try {
            return BrowsingHistoryOrder.arrange(input, new BrowsingHistoryOrder.Access<Object>() {
                @Override public String board(Object row) throws Exception {
                    if (row == null) return null;
                    String name = row.getClass().getName();
                    String board;
                    if (name.equals("o.onAdDisplayed$setIcon")) board = "g";
                    else if (name.equals("o.Yy2$open")) board = "b";
                    else if (name.equals("o.getAvailabilityStatus$r8lambdawJ5MHcSJed_CjC7r4OWD0UxyJsQ")) board = "e";
                    else return null;
                    Object id = field(row, board).get(row);
                    if (id == null) throw new IllegalStateException("Missing history BoardID");
                    return id.toString();
                }
                @Override public long visited(Object row) throws Exception {
                    String name = row.getClass().getName();
                    return field(row, name.equals("o.onAdDisplayed$setIcon") ? "c"
                            : name.equals("o.Yy2$open") ? "e" : "d").getLong(row);
                }
            }, BrowsingHistoryOrder.cutoff(System.currentTimeMillis(), days(), TimeZone.getDefault()));
        } catch (Exception error) {
            Log.w("Haiagaru", "Browsing history grouping left native list unchanged", error);
            return input;
        }
    }
    /** Run only after the exact legacy home-sort method has completed. */
    public static void applyLegacy(Object fragment, int mode) {
        if (fragment == null || !HiddenDroppedThreads.target(mode)) return;
        synchronized(legacyFragments){
            legacyFragments.removeIf(e->e.fragment.get()==null||e.fragment.get()==fragment);
            if(legacyFragments.size()>=16)legacyFragments.remove(0);
            legacyFragments.add(new Legacy(fragment,mode));
        }
        try {
            boolean legacy191 = fragment.getClass().getName().equals("o.r8lambdaTb_p0z6z2AqSZIga1YhmAVmiTPk");
            Object adapter = field(fragment, legacy191 ? "e" : "i").get(fragment);
            if (!(adapter instanceof BaseAdapter)) return;
            // g/d is the displayed copy; i/g is the native filter source, not the all-history DB model.
            String[] fields = legacy191 ? new String[]{"i", "g"} : new String[]{"g", "d"};
            Object[] replacements = new Object[fields.length];
            for (int i = 0; i < fields.length; i++) {
                Object original = field(adapter, fields[i]).get(adapter);
                if (!(original instanceof List)) return;
                List<?> source=HiddenDroppedThreads.source(original,(List<?>)original);
                List<?> ordered = transformValues(source, mode);
                if(ordered==original){replacements[i]=original;continue;}
                replacements[i] = original.getClass().getConstructor(List.class).newInstance(ordered);
                HiddenDroppedThreads.remember(replacements[i],source);
            }
            for (int i = 0; i < fields.length; i++) field(adapter, fields[i]).set(adapter, replacements[i]);
            ((BaseAdapter) adapter).notifyDataSetChanged();
        } catch (Exception error) {
            Log.w("Haiagaru", "Normal history adapter left unchanged", error);
        }
    }
    /** Reapply HOME copies when settings or asynchronously confirmed status change. */
    static void refreshVisible(Activity activity){
        synchronized(displays){
            for(Display display:displays){
                List<Object> output=display.output.get();if(output==null)continue;
                List<?> next=transformValues(display.source,display.mode);
                if(!output.equals(next)){output.clear();output.addAll(next);}
            }
            displays.removeIf(d->d.output.get()==null);
        }
        List<Legacy> legacy;synchronized(legacyFragments){legacy=new ArrayList<>(legacyFragments);}
        for(Legacy entry:legacy){
            Object f=entry.fragment.get();if(f==null)continue;
            try{
                if(Boolean.TRUE.equals(f.getClass().getMethod("isAdded").invoke(f))
                        &&f.getClass().getMethod("getActivity").invoke(f)==activity)applyLegacy(f,entry.mode);
            }catch(Exception error){Log.w("Haiagaru","HOME display refresh unavailable",error);}
        }
        if(activity!=null&&activity.getWindow()!=null)notifyLists(activity.getWindow().getDecorView());
    }
    private static void notifyLists(View view){
        try{
            Object adapter=null;
            if(view instanceof AdapterView)adapter=((AdapterView<?>)view).getAdapter();
            else for(Class<?> c=view.getClass();c!=null;c=c.getSuperclass())if(c.getName().equals("androidx.recyclerview.widget.RecyclerView")){
                adapter=view.getClass().getMethod("getAdapter").invoke(view);break;
            }
            if(adapter instanceof android.widget.WrapperListAdapter)adapter=((android.widget.WrapperListAdapter)adapter).getWrappedAdapter();
            if(adapter instanceof BaseAdapter)((BaseAdapter)adapter).notifyDataSetChanged();
            else if(adapter!=null)adapter.getClass().getMethod("notifyDataSetChanged").invoke(adapter);
        }catch(Exception error){Log.w("Haiagaru","HOME adapter notification unavailable",error);}
        if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=0;i<group.getChildCount();i++)notifyLists(group.getChildAt(i));}
    }
}
