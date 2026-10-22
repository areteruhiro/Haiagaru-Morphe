package app.morphe.extension.chmate;

import android.app.Activity;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import java.lang.ref.WeakReference;
import java.lang.reflect.*;
import java.util.*;
import java.util.regex.*;

/** Per-adapter display filters. Native DAT, read counts, NG flags and navigation stay untouched. */
public final class ExtraQuickFilters {
    private static final String[] LABELS = {"人気レス", "リンク", "画像", "動画", "AA", "自分", "返信"};
    private static final String[] KINDS = {"POPULAR", "LINK", "IMAGE", "MOVIE"};
    private static final ThreadLocal<Boolean> RESETTING=new ThreadLocal<>();
    static boolean resetting(){return Boolean.TRUE.equals(RESETTING.get());}
    private static final Map<Object, State> STATES = Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<View, WeakReference<Bar>> BARS = new WeakHashMap<>();
    private static final Map<Object, WeakReference<Object>> FRAGMENTS = new WeakHashMap<>();
    private static final ArrayList<WeakReference<Object>> OWNERS = new ArrayList<>();
    private static final ArrayList<Threshold> THRESHOLDS = new ArrayList<>();
    /** Called by the native toolbar dispatcher. Its anchor View is reused: never attach listeners to it. */
    public static boolean longClick(Object fragment,View button){
        if(button==null||button.getId()!=QuickFilterToolbar.ID)return false;
        boolean done=reset(fragment);
        Toast.makeText(button.getContext(),done?"フィルターを解除しました":"フィルターを解除できませんでした",Toast.LENGTH_SHORT).show();
        return true;
    }
    static void updateOptions(Activity activity){
        {
            List<Object> reset=new ArrayList<>();
            int allowed=0;for(int i=4;i<7;i++)if(QuickFilterOptions.visible(i))allowed|=1<<(i-4);
            synchronized(STATES){for(Map.Entry<Object,State> entry:STATES.entrySet())if((entry.getValue().extra&~allowed)!=0){entry.getValue().extra&=allowed;reset.add(entry.getKey());}}
            for(Object adapter:reset)try{rebuild(adapter);}catch(Exception error){Log.w("Haiagaru","Disable extra filters",error);}
        }
        for(WeakReference<Bar> ref:new ArrayList<>(BARS.values())){Bar bar=ref.get();if(bar!=null)bar.post(bar::update);}
        for(WeakReference<Object> ref:new ArrayList<>(OWNERS))if(ref.get()!=null)FloatingFilterButton.update(ref.get());
    }
    private static void styleButton(ToggleButton button){
        float density=button.getResources().getDisplayMetrics().density;
        int accent=themeColor(button.getContext(),android.R.attr.colorAccent,button.getCurrentTextColor());
        int text=button.getCurrentTextColor();
        android.graphics.drawable.StateListDrawable states=new android.graphics.drawable.StateListDrawable();
        for(boolean checked:new boolean[]{true,false}){
            android.graphics.drawable.GradientDrawable shape=new android.graphics.drawable.GradientDrawable();shape.setCornerRadius(8*density);
            shape.setColor(checked?(accent&0x00ffffff)|0x18000000:android.graphics.Color.TRANSPARENT);
            shape.setStroke(Math.max(1,(int)density),checked?accent:(text&0x00ffffff)|0x30000000);
            states.addState(checked?new int[]{android.R.attr.state_checked}:new int[0],shape);
        }
        button.setBackground(states);button.setMinWidth((int)(80*density));
    }
    private static int themeColor(Context context,int attr,int fallback){
        android.content.res.TypedArray colors=context.obtainStyledAttributes(new int[]{attr});
        try{return colors.getColor(0,fallback);}finally{colors.recycle();}
    }
    private static int bottomInset(Activity activity){
        android.graphics.Rect visible=new android.graphics.Rect();View decor=activity.getWindow().getDecorView();decor.getWindowVisibleDisplayFrame(visible);
        return Math.max(0,decor.getHeight()-visible.bottom);
    }
    private static boolean showBottomPanel(Activity activity,Object a)throws Exception{
        collect(a,PopularReplies.fullList(field(a,legacy(a)?"K":middle(a)?"J":"o")));
        GridLayout grid=new GridLayout(activity);grid.setColumnCount(4);int pad=(int)(8*grid.getResources().getDisplayMetrics().density);grid.setPadding(pad,pad,pad,pad);
        PopupWindow popup=new PopupWindow(grid,-1,-2,true);popup.setOutsideTouchable(true);
        popup.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(themeColor(activity,android.R.attr.colorBackground,android.graphics.Color.DKGRAY)));popup.setElevation(pad*2);
        ToggleButton[] buttons=new ToggleButton[QuickFilterOptions.extras()?7:4];
        Runnable redraw=()->{try{boolean[] on=selected(a);String[] names=labels(a);for(int i=0;i<buttons.length;i++){buttons[i].setTextOn(names[i]);buttons[i].setTextOff(names[i]);buttons[i].setChecked(on[i]);buttons[i].setText(names[i]);}}catch(Exception e){Log.w("Haiagaru","Bottom filter state",e);}};
        for(int i=0;i<buttons.length;i++){final int index=i;ToggleButton button=new ToggleButton(activity);buttons[i]=button;button.setAllCaps(false);button.setTextSize(13);styleButton(button);
            button.setMinimumHeight(pad*8);GridLayout.LayoutParams params=new GridLayout.LayoutParams(GridLayout.spec(i/4),GridLayout.spec(i%4,1f));params.width=0;params.height=-2;params.setMargins(pad/2,pad/2,pad/2,pad/2);grid.addView(button,params);
            button.setOnClickListener(v->{try{boolean requested=button.isChecked();toggle(a,index);if(index<4&&!(index==0&&PopularReplies.enabledFor(activity))){State state=state(a);state.pendingFilters[index]=requested;state.pendingFilterMask|=1<<index;}redraw.run();}catch(Exception e){Toast.makeText(activity,"フィルターを変更できませんでした",Toast.LENGTH_SHORT).show();redraw.run();}});
            if(i==2)button.setOnLongClickListener(v->{
                // The popup's Views are detached by dismiss(); never use their post queue.
                new android.os.Handler(android.os.Looper.getMainLooper()).post(()->{
                    popup.dismiss();
                    if(!activity.isFinishing()&&!activity.isDestroyed()&&!imageList(a))
                        Toast.makeText(activity,"画像一覧を開けませんでした",Toast.LENGTH_SHORT).show();
                });
                return true;
            });
            button.setVisibility(QuickFilterOptions.visible(i)?View.VISIBLE:View.GONE);
        }
        redraw.run();android.os.Handler handler=new android.os.Handler(android.os.Looper.getMainLooper());
        Runnable sync=new Runnable(){public void run(){if(!popup.isShowing())return;redraw.run();handler.postDelayed(this,150);}};
        popup.setOnDismissListener(()->{handler.removeCallbacks(sync);state(a).pendingFilterMask=0;});
        popup.setInputMethodMode(PopupWindow.INPUT_METHOD_NOT_NEEDED);
        popup.showAtLocation(activity.getWindow().getDecorView(),android.view.Gravity.BOTTOM,0,bottomInset(activity)+pad);handler.postDelayed(sync,150);return true;
    }
    private static final class Threshold {
        final WeakReference<Object> list;final int value;
        Threshold(Object list,int value){this.list=new WeakReference<>(list);this.value=value;}
    }
    public static void observeConfig(Object list,Object unused,int ignored,Object config) {
        try {
            Object base=PopularReplies.fullList(list);int value=((Number)field(config,"b")).intValue();
            synchronized(THRESHOLDS){THRESHOLDS.removeIf(t->t.list.get()==null||t.list.get()==base);THRESHOLDS.add(new Threshold(base,value));}
        }catch(Exception e){Log.w("Haiagaru","Native filter configuration",e);}
    }
    public static void register(Object fragment) {
        OWNERS.removeIf(ref -> ref.get()==null||ref.get()==fragment);
        OWNERS.add(new WeakReference<>(fragment));
        try { Activity activity=(Activity)call(fragment,"getActivity",new Class<?>[0]);if(activity!=null)updateOptions(activity); }
        catch(Exception ignored){}
    }
    private static final class State {
        Object rawSnapshot, appliedSnapshot;
        WeakReference<HaiagaruDialog> dialog=new WeakReference<>(null);
        String[] dialogLabels;
        int[] dialogIndices;
        int extra;
        int pendingFilterMask;
        boolean[] pendingFilters = new boolean[4];
        int[] counts = new int[7];
        Set<Integer> aa = new HashSet<>(), mine = new HashSet<>(), replies = new HashSet<>();
        Exception failure;
    }
    private static State state(Object adapter) { synchronized(STATES) { return STATES.computeIfAbsent(adapter, k -> new State()); } }
    private static Object field(Object o, String n) throws Exception { return ResponseArchiveEntry.field(o,n); }
    private static Object call(Object o, String n, Class<?>[] types, Object... args) throws Exception {
        Method m=o.getClass().getMethod(n,types); m.setAccessible(true); return m.invoke(o,args);
    }
    private static boolean legacy(Object a) { return a.getClass().getName().equals("o.m9ExternalSyntheticLambda1"); }
    private static boolean middle(Object a) { return a.getClass().getName().equals("o.listener"); }
    private static Object model(Object a) throws Exception { return field(a,legacy(a)?"Y":middle(a)?"L":"k"); }
    private static Object adapter(Object fragment) throws Exception {
        View root=(View)call(fragment,"getView",new Class<?>[0]);
        View list=FilterPosition.findList(root);
        if(list==null)throw new IllegalStateException("Response list missing");
        Object a=call(list,"getAdapter",new Class<?>[0]);
        if(a instanceof HeaderViewListAdapter)a=((HeaderViewListAdapter)a).getWrappedAdapter();
        if(a==null || !(legacy(a)||middle(a)||a.getClass().getName().equals("o.zzacr")))
            throw new IllegalStateException("Response adapter unavailable");
        FRAGMENTS.put(a,new WeakReference<>(fragment)); return a;
    }
    private static boolean[] selected(Object a) throws Exception {
        boolean[] result=new boolean[7]; Object m=model(a);
        if(legacy(a)) {
            String[] keys={"c","a","e","b"};
            for(int i=0;i<4;i++)result[i]=(Boolean)field(field(m,keys[i]),"mValue");
        } else {
            Object flow=field(m,middle(a)?"k":"w");
            Object value=call(flow,middle(a)?"c":"d",new Class<?>[0]);
            Matcher matches=Pattern.compile("FilterBarState\\(selected=(true|false),").matcher(value.toString());
            int i=0; while(matches.find()&&i<4)result[i++]=Boolean.parseBoolean(matches.group(1));
            if(i!=4)throw new IllegalStateException("Filter states changed");
        }
        State selection=state(a);
        for(int i=0;i<4;i++)if((selection.pendingFilterMask&(1<<i))!=0) {
            if(result[i]==selection.pendingFilters[i])selection.pendingFilterMask&=~(1<<i);
            else result[i]=selection.pendingFilters[i];
        }
        int mask=selection.extra;
        for(int i=4;i<7;i++)result[i]=(mask&(1<<(i-4)))!=0;
        return result;
    }
    private static void toggle(Object a,int which) throws Exception {toggle(a,which,false);}
    private static void toggle(Object a,int which,boolean reset) throws Exception {
        Object fragment=FRAGMENTS.containsKey(a)?FRAGMENTS.get(a).get():null;
        if(fragment==null)for(WeakReference<Object> ref:new ArrayList<>(OWNERS)) {
            Object owner=ref.get();if(owner==null)continue;
            try {if(adapter(owner)==a){fragment=owner;break;}}catch(Exception ignored){}
        }
        if(fragment!=null)FilterPosition.beforeFragment(fragment);
        if(which>=4) {
            State s=state(a);s.extra^=1<<(which-4);
            try { rebuild(a);if(s.failure!=null)throw s.failure; } catch(Exception e) { s.extra^=1<<(which-4);refresh(a);throw e; }
        } else if(legacy(a)) {
            if(!reset&&which==0 && fragment!=null && PopularReplies.enabledFor(context(a))) { PopularReplies.show(fragment);refresh(a);return; }
            Object value=field(model(a),new String[]{"c","a","e","b"}[which]);
            Field f=value.getClass().getDeclaredField("mValue");f.setAccessible(true);f.setBoolean(value,!f.getBoolean(value));
            call(value,"c",new Class<?>[0]);
        } else {
            Object m=model(a);ClassLoader loader=a.getClass().getClassLoader();
            Class<?> kind=loader.loadClass(middle(a)?"o.setLastGoodStreamIdokhttp":"o.isAtLeastLollipop");
            Object value=Enum.valueOf((Class)kind,KINDS[which]);
            if(middle(a)) {
                Class<?> callback=loader.loadClass("o.listener$setContentView$ComponentActivity");
                Constructor<?> ctor=callback.getDeclaredConstructor(Object.class);ctor.setAccessible(true);
                call(ctor.newInstance(m),"invoke",new Class<?>[]{Object.class},value);
            } else call(m,"b",new Class<?>[]{kind},value);
        }
        refresh(a);
    }
    private static void rebuild(Object a) throws Exception {
        if(legacy(a)||middle(a))call(a,legacy(a)?"c":"b",new Class<?>[]{boolean.class},false);
        else {
            Object snapshot=state(a).rawSnapshot; if(snapshot==null)return;
            ClassLoader loader=a.getClass().getClassLoader();Class<?> callback=loader.loadClass("o.zzcbx");
            Object unit=loader.loadClass("o.zzbwq").getField("INSTANCE").get(null);
            Object action=Proxy.newProxyInstance(loader,new Class<?>[]{callback},(proxy,m,args)-> {
                if(m.getName().equals("invoke"))return unit;
                if(m.getName().equals("hashCode"))return System.identityHashCode(proxy);
                if(m.getName().equals("equals"))return proxy==args[0];return "HaiagaruFilterRefresh";
            });
            call(a,"b",new Class<?>[]{snapshot.getClass(),callback},snapshot,action);
        }
    }
    private static Context context(Object a) throws Exception {
        if(!legacy(a)&&!middle(a))return (Context)field(a,"e");
        return ((android.view.LayoutInflater)field(a,legacy(a)?"q":"s")).getContext();
    }
    private static int number(Object r) throws Exception { return ((Number)field(r,r.getClass().getName().equals("o.BouncyCastleSocketAdapterCompanion")?"l":"o")).intValue(); }
    private static int[] anchors(Object r) throws Exception { return (int[])field(r,legacyResponse(r)?"d":middleResponse(r)?"a":"b"); }
    private static boolean legacyResponse(Object r){return r.getClass().getName().equals("o.processAdDisplayErrorPostbackForUserError");}
    private static boolean middleResponse(Object r){return r.getClass().getName().equals("o.BouncyCastleSocketAdapterCompanion");}
    private static boolean response(Object r){return r!=null&&(legacyResponse(r)||middleResponse(r)||r.getClass().getName().equals("o.KeJ11"));}
    private static Object rowResponse(Object row) throws Exception {
        if(row==null)return null;
        if(response(row))return row;
        String key=row.getClass().getName().equals("o.r8lambdakfayx3RllpZib32UTPpeXfQjw")?"j":
                row.getClass().getName().equals("o.setSocketokhttp")?"j":
                row.getClass().getName().equals("o.zzadi$_init_lambda2")?"o":null;
        if(key!=null){Object r=field(row,key);return response(r)?r:null;}
        return null;
    }
    private static Object index(Object a) throws Exception {
        if(legacy(a))return call(field(a,"M"),"b",new Class<?>[0]);
        if(!middle(a))return call(a,"a",new Class<?>[0]);
        Object m=model(a);
        Method get=m.getClass().getMethod("e",m.getClass());
        return get.invoke(null,m);
    }
    private static void collect(Object a,List<?> source) throws Exception {
        State s=state(a);
        // A reset changes selection only, not the full response list or its counts.
        // No extra predicate is active now; retain the counts until the next normal
        // update/dialog opening instead of reparsing every post during native callbacks.
        if(resetting()&&s.extra==0)return;
        int[] counts=new int[7];Set<Integer> aa=new HashSet<>(),mine=new HashSet<>(),replies=new HashSet<>();
        if(source.isEmpty()){s.counts=counts;s.aa=aa;s.mine=mine;s.replies=replies;s.failure=null;return;}
        Object idx=index(a);
        String indexMethod=legacy(a)?"d":middle(a)?"a":"b";
        for(Object r:source)if(response(r)) {
            int no=number(r);
            if(( ((Number)call(idx,indexMethod,new Class<?>[]{int.class},no)).intValue()&8)!=0)mine.add(no);
            if(Boolean.TRUE.equals(call(r,legacy(a)?"e":middle(a)?"i":"g",new Class<?>[0])))aa.add(no);
        }
        for(Object r:source)if(response(r)) {
            int no=number(r);
            for(int anchor:anchors(r))if(anchor<no&&mine.contains(anchor)){replies.add(no);break;}
        }
        Context c=context(a);int threshold=popularThreshold(a,c,source);
        Map<Integer,Integer> direct=Collections.emptyMap();
        if(PopularReplies.enabledFor(c)) {
            Map<Integer,String> posts=new LinkedHashMap<>();
            for(Object r:source)if(response(r))posts.put(number(r),android.text.Html.fromHtml((String)field(r,legacy(a)?"c":middle(a)?"d":"h")).toString());
            direct=PopularReplyRules.count(posts);
        }
        // Counts are per response, even when a response contains several links or anchors.
        for(Object r:source)if(response(r)) {
            int references=PopularReplies.enabledFor(c)?direct.getOrDefault(number(r),0):((Number)field(r,legacy(a)?"r":middle(a)?"p":"s")).intValue();
            if(references>=threshold)counts[0]++;
            String[] urls;
            if(legacy(a)||middle(a))urls=(String[])call(r,"d",new Class<?>[0]);
            else urls=(String[])r.getClass().getClassLoader().loadClass("o.KeJ11$RemoteActionCompatParcelizer")
                    .getMethod("a",String.class).invoke(null,field(r,"h"));
            boolean[] kinds=new boolean[3];
            for(String url:urls) {
                ClassLoader loader=r.getClass().getClassLoader();Object movie;
                Class<?> movieType=loader.loadClass(legacy(a)?"o.r0ExternalSyntheticLambda3":middle(a)?"o.MK":"o.zzbuc");
                movie=movieType.getMethod(legacy(a)?"b":middle(a)?"d":"d",String.class).invoke(null,url);
                if(movie!=null)kinds[2]=true;
                else if(legacy(a))kinds[(Boolean)loader.loadClass("o.r8lambdaeAQm29xfY6v7mMntOfLRXKh8x10").getMethod("c",CharSequence.class).invoke(null,url)?1:0]=true;
                else if(!middle(a)) {
                    Object helper=loader.loadClass("o.ApiApiOptionsNotRequiredOptions").getField("IconCompatParcelizer").get(null);
                    kinds[(Boolean)call(helper,"e",new Class<?>[]{CharSequence.class},url)?1:0]=true;
                } else kinds[isImage226(loader,url)?1:0]=true;
            }
            for(int i=0;i<3;i++)if(kinds[i])counts[i+1]++;
        }
        counts[4]=aa.size();counts[5]=mine.size();counts[6]=replies.size();
        s.aa=aa;s.mine=mine;s.replies=replies;s.counts=counts;s.failure=null;
    }
    private static boolean isImage226(ClassLoader loader,String url) throws Exception {
        return (Boolean)loader.loadClass("o.getColorFilter").getMethod("d",CharSequence.class).invoke(null,url);
    }
    private static int popularThreshold(Object a,Context c,List<?> source) throws Exception {
        if(PopularReplies.enabledFor(c))return Math.max(1,Math.min(9999,c.getSharedPreferences("io.github.areteruhiro.chmate.haiagaru.ui-config",0).getInt("popularReplyThreshold",3)));
        if(legacy(a)||middle(a)) {
            Class<?> prefs=a.getClass().getClassLoader().loadClass("jp.syoboi.a2chMate.Prefs");
            Field f=prefs.getDeclaredField(legacy(a)?"M":"n");f.setAccessible(true);Object option=f.get(null);
            Method get=prefs.getDeclaredMethod(legacy(a)?"c":"d",f.getType());get.setAccessible(true);
            return ((Number)get.invoke(null,option)).intValue();
        }
        synchronized(THRESHOLDS){for(Threshold t:THRESHOLDS)if(t.list.get()==source)return t.value;}
        throw new IllegalStateException("Native popularity threshold unavailable");
    }
    public static Object modernSnapshot(Object a,Object snapshot) {
        try {
            State s=state(a);
            if(snapshot==s.appliedSnapshot)snapshot=s.rawSnapshot;
            s.rawSnapshot=snapshot;
            collect(a,PopularReplies.fullList(field(snapshot,"j")));
            List<?> rows=(List<?>)field(snapshot,"b");
            List<?> filtered=filterCollected(a,rows);
            if(filtered==rows){s.appliedSnapshot=snapshot;return snapshot;}
            Method copy=snapshot.getClass().getMethod("d",snapshot.getClass(),List.class);
            Object result=copy.invoke(null,snapshot,filtered);s.appliedSnapshot=result;return result;
        }catch(Exception e){state(a).failure=e;Log.e("Haiagaru","Modern extra filters",e);return snapshot;}
    }
    private static List<?> filterCollected(Object a,List<?> rows) throws Exception {
            State s=state(a);if(s.extra==0){refresh(a);return rows;}
            ArrayList<Object> kept=new ArrayList<>();Set<Integer> seen=new HashSet<>();
            for(Object row:rows) {
                Object r=rowResponse(row);
                if(r==null){kept.add(row);continue;}
                int no=number(r);
                if(((s.extra&1)!=0&&s.aa.contains(no))||((s.extra&2)!=0&&s.mine.contains(no))||((s.extra&4)!=0&&s.replies.contains(no)))
                    if(seen.add(no))kept.add(row);
            }
            refresh(a);return kept;
    }
    public static ArrayList<?> filterRows(Object a,List<?> rows) {
        if(!QuickFilterOptions.counts()&&!QuickFilterOptions.extras())return rows instanceof ArrayList?(ArrayList<?>)rows:new ArrayList<>(rows);
        try {
            Object source=field(a,legacy(a)?"K":middle(a)?"J":"o");
            List<?> full=PopularReplies.fullList(source);collect(a,full);
            List<?> kept=filterCollected(a,rows);return kept instanceof ArrayList?(ArrayList<?>)kept:new ArrayList<>(kept);
        }catch(Exception e){state(a).failure=e;Log.e("Haiagaru","Extra quick filters unavailable",e);return rows instanceof ArrayList?(ArrayList<?>)rows:new ArrayList<>(rows);}
    }
    public static void legacyRows(Object a) {
        try {String key=legacy(a)?"z":"g";Field f=a.getClass().getDeclaredField(key);f.setAccessible(true);
            Object rows=f.get(a);if(rows instanceof List)f.set(a,filterRows(a,(List<?>)rows));
        }catch(Exception e){Log.e("Haiagaru","Extra legacy filters",e);}
    }
    private static String[] labels(Object a) {
        int[] counts=state(a).counts;String[] result=new String[7];
        for(int i=0;i<7;i++)result[i]=QuickFilterOptions.counts()?counts[i]+"\n"+LABELS[i]:LABELS[i];return result;
    }
    public static boolean reset(Object fragment){
        RESETTING.set(true);
        try{Object a=adapter(fragment);State selection=state(a);selection.extra=0;
            if(legacy(a)){
                Object changed=null,m=model(a);
                for(String key:new String[]{"c","a","e","b"}){
                    Object value=field(m,key);Field bit=value.getClass().getDeclaredField("mValue");bit.setAccessible(true);
                    if(bit.getBoolean(value)){bit.setBoolean(value,false);changed=value;}
                }
                selection.pendingFilterMask=0;
                // All flags are final before notifying the native adapter: avoid rebuilding
                // a long thread once for each flag plus once for the extra filters.
                if(changed!=null)call(changed,"c",new Class<?>[0]);else rebuild(a);
            }else{
                boolean[] on=selected(a);for(int i=0;i<4;i++)if(on[i]){toggle(a,i,true);selection.pendingFilters[i]=false;selection.pendingFilterMask|=1<<i;}
                rebuild(a);
            }
            refresh(a);return true;
        }catch(Exception error){Log.w("Haiagaru","Reset filters",error);return false;}finally{RESETTING.remove();}
    }
    private static int[] visibleIndices(){int n=0;for(int i=0;i<7;i++)if(QuickFilterOptions.visible(i))n++;int[] result=new int[n];for(int i=0,j=0;i<7;i++)if(QuickFilterOptions.visible(i))result[j++]=i;return result;}
    public static boolean open(Object fragment) {
        try {
            Object a=adapter(fragment);Activity activity=(Activity)call(fragment,"getActivity",new Class<?>[0]);
            if(activity==null||activity.isFinishing())return false;
            if(QuickFilterOptions.bottom())return showBottomPanel(activity,a);
            collect(a,PopularReplies.fullList(field(a,legacy(a)?"K":middle(a)?"J":"o")));
            int[] indices=visibleIndices();boolean[] all=selected(a);String[] fullNames=labels(a);
            boolean[] checked=new boolean[indices.length];String[] names=new String[indices.length];
            for(int i=0;i<indices.length;i++){checked[i]=all[indices[i]];names[i]=fullNames[indices[i]];}
            HaiagaruDialog d=new HaiagaruDialog.Builder(activity).setTitle("レスのフィルター")
                    .setMultiChoiceItems(names,checked,(dialog,which,on)->{
                        int index=indices[which];try {toggle(a,index);
                            if(index<4 && !(index==0 && PopularReplies.enabledFor(activity))) {
                                State selection=state(a);selection.pendingFilters[index]=on;
                                selection.pendingFilterMask|=1<<index;
                            }
                            boolean[] next=selected(a);for(int i=0;i<names.length;i++)((HaiagaruDialog)dialog).getListView().setItemChecked(i,next[indices[i]]);}
                        catch(Exception e){Log.e("Haiagaru","Extra filter toggle",e);((HaiagaruDialog)dialog).getListView().setItemChecked(which,!on);Toast.makeText(activity,"フィルターを変更できませんでした",Toast.LENGTH_SHORT).show();}
                    }).setNeutralButton("設定",(dialog,which)->QuickFilterOptions.showToolbarSettings(activity)).setNegativeButton("閉じる",null).show();
            state(a).dialog=new WeakReference<>(d);state(a).dialogLabels=names;state(a).dialogIndices=indices;
            // Native filter actions finish asynchronously. Reading their StateFlow
            // directly after invoke() can still return the previous check state.
            // Watch only while this dialog is visible, and redraw only changes.
            android.os.Handler handler=new android.os.Handler(android.os.Looper.getMainLooper());
            boolean[] observed=checked.clone();
            Runnable sync=new Runnable() {
                @Override public void run() {
                    if(!d.isShowing())return;
                    try {
                        boolean[] next=selected(a);
                        for(int i=0;i<observed.length;i++)if(observed[i]!=next[indices[i]]) {
                            observed[i]=next[indices[i]];d.getListView().setItemChecked(i,next[indices[i]]);
                        }
                    }catch(Exception e){Log.w("Haiagaru","Filter selection update",e);}
                    handler.postDelayed(this,100);
                }
            };
            d.setOnDismissListener(dialog->{handler.removeCallbacks(sync);state(a).pendingFilterMask=0;});
            handler.postDelayed(sync,100);
            d.getListView().setOnItemLongClickListener((parent,v,pos,id)->{
                if(indices[pos]!=2)return false;
                d.dismissForNavigation(()->{if(!imageList(a))Toast.makeText(activity,"画像一覧を開けませんでした",Toast.LENGTH_SHORT).show();});
                return true;
            });
            return true;
        }catch(Exception e){Log.w("Haiagaru","Extended filter dialog",e);return false;}
    }
    private static boolean imageList(Object a) {return QuickFilterToolbar.openImageList(a);}
    public static View unwrap(View view) {return view instanceof Bar?((Bar)view).original:view;}
    public static View legacyRowOwner(Object owner,View row) {return legacyRow(row,owner);}
    public static void bindHolder(Object owner,Object holder,View row) {bind(row,owner);}
    public static View wrap(View original) {
        if(original==null||original instanceof Bar)return original;
        WeakReference<Bar> ref=BARS.get(original);Bar old=ref==null?null:ref.get();if(old!=null)return old;
        Bar bar=new Bar(original);BARS.put(original,new WeakReference<>(bar));return bar;
    }
    public static View legacyRow(View original,Object a) {
        if(original==null)return null;
        try {if(a.getClass().getName().equals("o.listener$setContentView"))a=field(a,"c");}
        catch(Exception e){Log.w("Haiagaru","Filter row owner",e);return original;}
        View raw=unwrap(original);Object binding=raw.getTag(0x7f0a0106);
        if(legacy(a)&&(binding==null||!binding.getClass().getName().equals("o.j4")))return original;
        Bar bar=(Bar)wrap(raw);bar.bind(a);bar.setTag(raw.getTag());return bar;
    }
    public static void bind(View original,Object owner) {
        try {Object a=owner.getClass().getName().equals("o.listener$setContentView")?field(owner,"c"):owner;
            Bar bar=(Bar)wrap(original);bar.bind(a);
        }catch(Exception e){Log.w("Haiagaru","Extended filter row",e);}
    }
    private static void refresh(Object a) {
        for(WeakReference<Bar> ref:new ArrayList<>(BARS.values())) {Bar bar=ref.get();if(bar!=null&&bar.adapter.get()==a)bar.post(bar::update);}
        State s=state(a);HaiagaruDialog d=s.dialog.get();
        if(d!=null&&d.isShowing())new android.os.Handler(android.os.Looper.getMainLooper()).post(()->{
            if(!d.isShowing())return;
            try {String[] full=labels(a);String[] names=new String[s.dialogIndices.length];for(int i=0;i<names.length;i++)names[i]=full[s.dialogIndices[i]];System.arraycopy(names,0,s.dialogLabels,0,names.length);
                ArrayAdapter<String> adapter=(ArrayAdapter<String>)d.getListView().getAdapter();
                adapter.setNotifyOnChange(false);adapter.clear();adapter.addAll(names);adapter.notifyDataSetChanged();
                boolean[] checked=selected(a);for(int i=0;i<names.length;i++)d.getListView().setItemChecked(i,checked[s.dialogIndices[i]]);
                d.refresh();
            }catch(Exception e){Log.w("Haiagaru","Filter counts update",e);}
        });
    }
    private static final class Bar extends LinearLayout {
        final View original;final HorizontalScrollView scroll;final ToggleButton[] buttons=new ToggleButton[7];
        WeakReference<Object> adapter=new WeakReference<>(null);
        Bar(View original) {
            super(original.getContext());this.original=original;setOrientation(VERTICAL);
            setLayoutParams(new android.widget.AbsListView.LayoutParams(-1,-2));
            original.setVisibility(GONE);addView(original,new LinearLayout.LayoutParams(-1,0));
            scroll=new HorizontalScrollView(getContext()){
                float downX,downY;
                @Override public boolean dispatchTouchEvent(android.view.MotionEvent event){
                    int action=event.getActionMasked();
                    if(action==android.view.MotionEvent.ACTION_DOWN){downX=event.getX();downY=event.getY();getParent().requestDisallowInterceptTouchEvent(true);}
                    else if(action==android.view.MotionEvent.ACTION_MOVE)getParent().requestDisallowInterceptTouchEvent(Math.abs(event.getX()-downX)>=Math.abs(event.getY()-downY));
                    // Dispatch here (not OnTouchListener): filter buttons consume DOWN themselves.
                    boolean handled=super.dispatchTouchEvent(event);
                    if(action==android.view.MotionEvent.ACTION_UP||action==android.view.MotionEvent.ACTION_CANCEL)getParent().requestDisallowInterceptTouchEvent(false);
                    return handled;
                }
            };scroll.setHorizontalScrollBarEnabled(false);
            LinearLayout choices=new LinearLayout(getContext());
            for(int i=0;i<7;i++){final int index=i;ToggleButton b=new ToggleButton(getContext());buttons[i]=b;b.setAllCaps(false);styleButton(b);
                b.setMinHeight((int)(48*getResources().getDisplayMetrics().density));
                b.setOnClickListener(v->{Object a=adapter.get();if(a!=null)try{boolean requested=b.isChecked();toggle(a,index);if(index<4&&!(index==0&&PopularReplies.enabledFor(getContext()))){State state=state(a);state.pendingFilters[index]=requested;state.pendingFilterMask|=1<<index;}update();}catch(Exception e){Log.e("Haiagaru","Filter row toggle",e);update();}});
                if(i==2)b.setOnLongClickListener(v->{Object a=adapter.get();return a!=null&&imageList(a);});
                b.setTextSize(14);b.setPadding((int)(14*getResources().getDisplayMetrics().density),0,(int)(14*getResources().getDisplayMetrics().density),0);
                LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(-2,-2);params.setMargins(2,0,2,0);choices.addView(b,params);}
            scroll.addView(choices);addView(scroll,new LinearLayout.LayoutParams(-1,-2));
        }
        void bind(Object a){adapter=new WeakReference<>(a);update();}
        void update(){Object a=adapter.get();if(a==null)return;try{boolean[] on=selected(a);String[] names=labels(a);
            for(int i=0;i<7;i++){buttons[i].setTextOn(names[i]);buttons[i].setTextOff(names[i]);buttons[i].setChecked(on[i]);buttons[i].setText(names[i]);}
            for(int i=4;i<7;i++)buttons[i].setVisibility(QuickFilterOptions.visible(i)?VISIBLE:GONE);
            boolean stock=!QuickFilterOptions.counts()&&!QuickFilterOptions.extras();
            boolean hidden=Haiagaru.compactQuickFilters()||QuickFilterOptions.bottom();
            original.setVisibility(stock&&!hidden?VISIBLE:GONE);
            original.setLayoutParams(new LinearLayout.LayoutParams(-1,stock&&!hidden?-2:0));
            scroll.setVisibility(hidden||stock?GONE:VISIBLE);
        }catch(Exception e){Log.w("Haiagaru","Filter row state",e);}}
    }
    private ExtraQuickFilters(){}
}
