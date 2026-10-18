package app.morphe.extension.chmate;

import android.app.*;
import android.content.*;
import android.text.Html;
import android.view.*;
import android.widget.*;
import java.lang.ref.WeakReference;
import java.lang.reflect.*;
import java.util.*;

/** Read-only popular-response popup. Never sends native navigation/read-count actions. */
public final class PopularReplies {
    private static final String PREFS = "io.github.areteruhiro.chmate.haiagaru.ui-config";
    private static final ArrayList<WeakReference<Object>> FRAGMENTS = new ArrayList<>();
    private static final ThreadLocal<Boolean> RESETTING = new ThreadLocal<>();
    private static final java.util.concurrent.ExecutorService WORKER=java.util.concurrent.Executors.newSingleThreadExecutor();
    public static void register(Object fragment) {
        FRAGMENTS.removeIf(ref -> ref.get()==null || ref.get()==fragment);
        FRAGMENTS.add(new WeakReference<>(fragment));
    }
    private static SharedPreferences prefs(Context c) { return c.getSharedPreferences(PREFS,0); }
    private static boolean enabled() {
        Context c=Haiagaru.applicationContextForExtension();
        return c!=null && prefs(c).getBoolean("popularReplyPopup",false);
    }
    public static boolean interceptModel(Object model,Object kind) {
        if (!enabled() || !(kind instanceof Enum) || !"POPULAR".equals(((Enum<?>)kind).name())) return false;
        return openModel(model);
    }
    static boolean enabledFor(Context c){return prefs(c).getBoolean("popularReplyPopup",false);}
    static boolean openModel(Object model) {
        if(!enabled())return false;
        for (WeakReference<Object> ref : new ArrayList<>(FRAGMENTS)) {
            Object fragment=ref.get(); if(fragment==null)continue;
            try {
                if(QuickFilterToolbar.findResponseViewModel(fragment)==model) {
                    View root=(View)fragment.getClass().getMethod("getView").invoke(fragment);
                    if(root!=null && root.isShown()) { show(fragment); return true; }
                }
            } catch(Exception error) { android.util.Log.w("Haiagaru","Popular popup owner",error); }
        }
        // Fail closed rather than switching the original thread's filter unexpectedly.
        Toast.makeText(Haiagaru.applicationContextForExtension(),"人気レスを開く画面を確認できませんでした",Toast.LENGTH_SHORT).show();
        return true;
    }
    public static boolean interceptCallback(Object callback,Object kind) {
        try {
            for(Class<?> type=callback.getClass();type!=null;type=type.getSuperclass()) {
                for(Field f:type.getDeclaredFields()) if(f.getName().equals("receiver")) {
                    f.setAccessible(true); return interceptModel(f.get(callback),kind);
                }
            }
        } catch(Exception error) { android.util.Log.w("Haiagaru","Popular callback",error); }
        return false;
    }
    public static boolean interceptLegacy(Object fragment) {
        if(!enabled())return false;
        if(Boolean.TRUE.equals(RESETTING.get()))return true;
        try {
            Object adapter=ResponseArchiveEntry.field(fragment,"c");
            Object model=ResponseArchiveEntry.field(adapter,"Y");
            Object observable=ResponseArchiveEntry.field(model,"c");
            Field value=observable.getClass().getDeclaredField("mValue");value.setAccessible(true);
            if(!value.getBoolean(observable))return false;
            value.setBoolean(observable,false);
            RESETTING.set(true);
            try { observable.getClass().getMethod("c").invoke(observable); }
            finally { RESETTING.remove(); }
            show(fragment); return true;
        } catch(Exception error) {
            android.util.Log.w("Haiagaru","Popular legacy popup",error); return false;
        }
    }
    private static Object flow(Object model,String name,String getter) throws Exception {
        Object flow=ResponseArchiveEntry.field(model,name);
        Method method=flow.getClass().getMethod(getter);method.setAccessible(true);return method.invoke(flow);
    }
    static List<?> fullList(Object value) throws Exception {
        if(!(value instanceof List))throw new IllegalStateException("読み込み済みレスがありません");
        Object base;
        try { base=ResponseArchiveEntry.field(value,"mBase"); } catch(NoSuchFieldException ignored){base=null;}
        return base instanceof List ? (List<?>)base : (List<?>)value;
    }
    static void show(Object fragment) {
        try {
            Activity a=(Activity)fragment.getClass().getMethod("getActivity").invoke(fragment);
            if(a==null||a.isFinishing())return;
            String version=a.getPackageManager().getPackageInfo(a.getPackageName(),0).versionName;
            Object list,board;
            if(version.equals("0.8.10.191 dev")) {
                list=ResponseArchiveEntry.field(ResponseArchiveEntry.field(fragment,"c"),"K");
                board=ResponseArchiveEntry.field(fragment,"W");
            } else {
                Object model=QuickFilterToolbar.findResponseViewModel(fragment);
                if(version.equals("0.8.10.226 dev")) {
                    list=flow(model,"y","c");board=flow(model,"d","c");
                } else {
                    Object loaded=flow(model,"l","d");
                    list=ResponseArchiveEntry.field(loaded,"c");board=flow(model,"r","d");
                }
            }
            ArrayList<ResponseArchiveEntry> entries=new ArrayList<>();
            for(Object response:fullList(list))if(response!=null)
                entries.add(ResponseArchiveEntry.snapshot(response,board,String.valueOf(a.getTitle())));
            render(a,fragment,entries);
        } catch(Exception error) {
            android.util.Log.w("Haiagaru","Popular popup",error);
            Context c=Haiagaru.applicationContextForExtension();
            if(c!=null)Toast.makeText(c,"人気レスの取得に失敗しました",Toast.LENGTH_LONG).show();
        }
    }
    private static void render(Activity a,Object fragment,List<ResponseArchiveEntry> entries) {
        boolean dark=darkBackground(a,fragment);
        Context themed=new android.view.ContextThemeWrapper(a,dark?
                android.R.style.Theme_Material_Dialog_Alert:android.R.style.Theme_Material_Light_Dialog_Alert);
        AlertDialog loading=new AlertDialog.Builder(themed).setMessage("人気レスを集計しています…")
                .setNegativeButton("キャンセル",null).create();
        loading.show();
        WORKER.execute(()->{
            try {
                LinkedHashMap<Integer,String> texts=new LinkedHashMap<>();
                for(ResponseArchiveEntry e:entries)texts.put(e.number,plain(e.body));
                Map<Integer,Integer> counts=PopularReplyRules.count(texts);
                a.runOnUiThread(()->{
                    if(!loading.isShowing()||a.isFinishing()||a.isDestroyed())return;
                    loading.dismiss();display(a,fragment,entries,texts,counts);
                });
            }catch(Exception error){android.util.Log.w("Haiagaru","Popular reply count",error);
                a.runOnUiThread(()->{if(!a.isFinishing()&&!a.isDestroyed()){loading.dismiss();
                    Toast.makeText(a,"人気レスを集計できませんでした",Toast.LENGTH_SHORT).show();}});}
        });
    }
    private static void display(Activity a,Object fragment,List<ResponseArchiveEntry> entries,
            Map<Integer,String> texts,Map<Integer,Integer> counts) {
        int threshold=Math.max(1,Math.min(9999,prefs(a).getInt("popularReplyThreshold",3)));
        ArrayList<ResponseArchiveEntry> matches=new ArrayList<>();
        for(ResponseArchiveEntry e:entries)if(counts.getOrDefault(e.number,0)>=threshold)matches.add(e);
        boolean dark=darkBackground(a,fragment);
        Context themed=new android.view.ContextThemeWrapper(a,dark?
                android.R.style.Theme_Material_Dialog_Alert:android.R.style.Theme_Material_Light_Dialog_Alert);
        int foreground=dark?android.graphics.Color.WHITE:android.graphics.Color.BLACK;
        android.widget.BaseAdapter adapter=new android.widget.BaseAdapter() {
            public int getCount(){return matches.size();}
            public Object getItem(int i){return matches.get(i);}
            public long getItemId(int i){return matches.get(i).number;}
            public View getView(int i,View old,ViewGroup parent){
                TextView text=old instanceof TextView?(TextView)old:new TextView(themed);
                ResponseArchiveEntry e=matches.get(i);
                int pad=(int)(12*a.getResources().getDisplayMetrics().density);
                text.setPadding(pad,pad,pad,pad);text.setTextSize(15);text.setTextColor(foreground);
                text.setText(e.number+"　直接返信 "+counts.get(e.number)+"件\n"+plain(e.name)+"　"+plain(e.date)+"\n"+texts.get(e.number));
                return text;
            }
        };
        ListView list=new ListView(themed);list.setAdapter(adapter);
        list.setBackgroundColor(dark?0xff171717:android.graphics.Color.WHITE);
        AlertDialog dialog=new AlertDialog.Builder(themed).setTitle("人気レス（直接返信"+threshold+"件以上・"+matches.size()+"件）")
                .setView(list).setPositiveButton("閉じる",null)
                .setNeutralButton("閾値を変更",(d,w)->threshold(a,()->render(a,fragment,entries))).create();
        list.setOnItemClickListener((parent,view,position,id)->{
            try {
                if(FilterPosition.jumpToResponse(fragment,matches.get(position).number))dialog.dismiss();
                else Toast.makeText(a,"現在の表示条件では該当レスに移動できません",Toast.LENGTH_SHORT).show();
            }catch(Exception error){android.util.Log.w("Haiagaru","Popular reply navigation",error);
                Toast.makeText(a,"該当レスに移動できませんでした",Toast.LENGTH_SHORT).show();}
        });
        dialog.show();
    }
    private static boolean darkBackground(Activity a,Object fragment) {
        try {
            View root=(View)fragment.getClass().getMethod("getView").invoke(fragment);
            for(View view=root;view!=null;view=view.getParent() instanceof View?(View)view.getParent():null) {
                if(!(view.getBackground() instanceof android.graphics.drawable.ColorDrawable))continue;
                int color=((android.graphics.drawable.ColorDrawable)view.getBackground()).getColor();
                if(android.graphics.Color.alpha(color)<200)continue;
                return (299*android.graphics.Color.red(color)+587*android.graphics.Color.green(color)+114*android.graphics.Color.blue(color))<128000;
            }
        }catch(Exception ignored){}
        return (a.getResources().getConfiguration().uiMode&android.content.res.Configuration.UI_MODE_NIGHT_MASK)
                ==android.content.res.Configuration.UI_MODE_NIGHT_YES;
    }
    private static String plain(String html){return Html.fromHtml(html).toString().trim();}
    private static void threshold(Activity a,Runnable after) {
        EditText input=new EditText(a);input.setInputType(2);
        input.setText(String.valueOf(prefs(a).getInt("popularReplyThreshold",3)));
        AlertDialog dialog=new AlertDialog.Builder(a).setTitle("直接返信数の閾値（1〜9999）")
                .setView(input).setNegativeButton("キャンセル",null).setPositiveButton("保存",null).create();
        dialog.setOnShowListener(d->dialog.getButton(-1).setOnClickListener(v->{
            try {int n=Integer.parseInt(input.getText().toString());if(n<1||n>9999)throw new NumberFormatException();
                prefs(a).edit().putInt("popularReplyThreshold",n).apply();dialog.dismiss();if(after!=null)after.run();
            }catch(NumberFormatException error){input.setError("1〜9999を入力してください");}
        }));dialog.show();
    }
    static void addSettings(android.widget.LinearLayout layout,Activity a) {
        Switch toggle=new Switch(a);toggle.setText("人気レスを直接返信数で判定し、ポップアップで表示");
        toggle.setChecked(prefs(a).getBoolean("popularReplyPopup",false));
        toggle.setOnCheckedChangeListener((v,on)->prefs(a).edit().putBoolean("popularReplyPopup",on).apply());layout.addView(toggle);
        Button threshold=new Button(a);threshold.setText("人気レスの直接返信数の閾値（初期3件）");
        threshold.setOnClickListener(v->threshold(a,null));layout.addView(threshold);
    }
    private PopularReplies() { }
}
