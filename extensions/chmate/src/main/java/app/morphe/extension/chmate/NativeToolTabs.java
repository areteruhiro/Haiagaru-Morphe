package app.morphe.extension.chmate;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.net.Uri;
import android.view.View;
import android.widget.*;
import java.lang.reflect.*;

/** Optional tools in the native type-4 tab model; phone navigation is unchanged. */
public final class NativeToolTabs {
    public static final String KEY="nativeToolTabs";
    public static final String SETTINGS_KEY="nativeToolTabSettings", EDDI_KEY="nativeToolTabEddi";
    public static final String SEARCH_KEY="nativeToolTabSearch", CHECKER_KEY="nativeToolTabChecker", HISSI_KEY="nativeToolTabHissi", FULL_KEY="nativeToolTabWacchoi";
    private static final String PREFIX="haiagaru-tool://";
    private static final String FROM_TABLET="haiagaru.settings.fromTablet";
    private static java.lang.ref.WeakReference<Activity> settingsHost=new java.lang.ref.WeakReference<>(null);
    private NativeToolTabs(){}
    /** Kept unoptimized and changed only after all native wiring is verified by the patch. */
    public static boolean supported(){return false;}
    /** Filled with the original native constructor by the version-specific patch. */
    public static Object stockFragment(){throw new IllegalStateException("Native tab bridge not installed");}
    static boolean enabled(Activity activity){return supported()&&activity.getSharedPreferences(WacchoiSearchOptions.PREFS,0).getBoolean(KEY,false);}
    static boolean selected(Activity activity,String tool){
        String key="settings".equals(tool)?SETTINGS_KEY:"eddi".equals(tool)?EDDI_KEY:"search".equals(tool)?SEARCH_KEY:"checker".equals(tool)?CHECKER_KEY:"hissi".equals(tool)?HISSI_KEY:"wacchoi".equals(tool)?FULL_KEY:null;
        return key!=null&&activity.getSharedPreferences(WacchoiSearchOptions.PREFS,0).getBoolean(key,true);
    }
    public static boolean open(Activity activity,String tool){
        return open(activity,tool,null);
    }
    public static boolean open(Activity activity,String tool,Intent launch){
        if(activity==null||!enabled(activity)||!activity.getClass().getName().equals("jp.syoboi.a2chMate.activity.TabletHomeActivity"))return false;
        if(!selected(activity,tool))return false;
        Bundle args=new Bundle();args.putString("_data",PREFIX+tool);
        args.putString(Intent.EXTRA_TITLE,"settings".equals(tool)?"Haiagaru設定":"eddi".equals(tool)?"エッヂ過去ログ検索":"search".equals(tool)?"投稿を探す":"checker".equals(tool)?"投稿検索・結果":"hissi".equals(tool)?"必死チェッカーもどき":"ワッチョイ全文検索");
        if(launch!=null)try{args.putString(Intent.EXTRA_TEXT,new org.json.JSONObject().put("launch",launch.toUri(Intent.URI_INTENT_SCHEME)).toString());}catch(org.json.JSONException error){throw new IllegalArgumentException(error);}
        try{
            Method opener=null;
            for(Method method:activity.getClass().getDeclaredMethods()){
                Class<?>[] p=method.getParameterTypes();
                // 191 accepts ChMate's own Fragment base, not the AndroidX root used by 242.
                if(method.getReturnType()==void.class&&p.length==3&&p[0]!=Object.class&&p[0].isAssignableFrom(NativeToolFragment.class)&&p[1]==Bundle.class&&p[2]==boolean.class){
                    if(opener!=null)throw new IllegalStateException("Ambiguous native tab opener");
                    opener=method;
                }
            }
            if(opener!=null){opener.setAccessible(true);opener.invoke(activity,null,args,true);return true;}
        }catch(Exception error){android.util.Log.e("Haiagaru","Could not open native tool tab",error);}
        return false;
    }
    /** Native close APIs resolve the owning pane from this exact Fragment. */
    static boolean closeCurrentTab(Activity activity,Object fragment){
        if(activity==null||activity.isFinishing()||activity.isDestroyed()
                ||!activity.getClass().getName().equals("jp.syoboi.a2chMate.activity.TabletHomeActivity")
                ||!(fragment instanceof NativeToolFragment))return false;
        try{
            if(!marker(arguments(fragment)))return false;
            Class<?> nativeFragment=Class.forName("androidx.fragment.app.Fragment",false,activity.getClassLoader());
            String version=activity.getPackageManager().getPackageInfo(activity.getPackageName(),0).versionName;
            if("0.8.10.191 dev".equals(version)){
                // false preserves ChMate's locked-tab confirmation and native selection update.
                activity.getClass().getMethod("e",nativeFragment,boolean.class).invoke(activity,fragment,false);
                return true;
            }
            if("0.8.10.226 dev".equals(version)){
                Object model=tabletModel(activity,"e","o.mc");
                return Boolean.TRUE.equals(model.getClass().getMethod("a",nativeFragment,boolean.class)
                        .invoke(model,fragment,false));
            }
            if("0.8.10.242 dev".equals(version)){
                Object model=tabletModel(activity,"getValue","jp.syoboi.a2chMate.feature.tabletmode.TabletHomeViewModel");
                Object page=model.getClass().getMethod("b",nativeFragment).invoke(model,fragment);
                if(page==null||!marker(pageArgs(page)))return false;
                return Boolean.TRUE.equals(model.getClass().getMethod("d",page.getClass(),boolean.class)
                        .invoke(model,page,false));
            }
        }catch(Exception error){android.util.Log.w("Haiagaru","Could not close native tool tab",error);}
        return false;
    }
    private static Object tabletModel(Activity activity,String getter,String expectedType)throws ReflectiveOperationException{
        Field field=activity.getClass().getDeclaredField("g");field.setAccessible(true);
        Object lazy=field.get(activity);
        if(lazy==null)throw new IllegalStateException("Native tablet model is unavailable");
        Method value=lazy.getClass().getMethod(getter);value.setAccessible(true);
        Object model=value.invoke(lazy);
        if(model==null||!model.getClass().getName().equals(expectedType))
            throw new IllegalStateException("Native tablet model changed");
        return model;
    }
    /** Remember only the tablet that opened this settings Activity, never an unrelated window. */
    public static Intent settingsIntent(Activity activity){
        boolean tablet=activity.getClass().getName().equals("jp.syoboi.a2chMate.activity.TabletHomeActivity");
        if(tablet)settingsHost=new java.lang.ref.WeakReference<>(activity);
        return new Intent(activity,HaiagaruSettingsActivity.class).putExtra(FROM_TABLET,tablet);
    }
    public static void showChooser(Activity activity){
        Activity host=activity;
        if(activity instanceof HaiagaruSettingsActivity&&activity.getIntent().getBooleanExtra(FROM_TABLET,false))host=settingsHost.get();
        if(host==null||host.isFinishing()||host.isDestroyed()||!host.getClass().getName().equals("jp.syoboi.a2chMate.activity.TabletHomeActivity")){
            Toast.makeText(activity,"タブレット表示の本体画面からHaiagaru設定を開いてください",Toast.LENGTH_LONG).show();return;
        }
        if(!enabled(host)){Toast.makeText(activity,"先に「タブレットで追加画面を本体のスレタブで開く」をオンにしてください",Toast.LENGTH_LONG).show();return;}
        final Activity target=host;
        java.util.ArrayList<String> tools=new java.util.ArrayList<>(),labels=new java.util.ArrayList<>();
        if(selected(host,"settings")){tools.add("settings");labels.add("Haiagaru設定");}
        if(selected(host,"eddi")){tools.add("eddi");labels.add("エッヂ過去ログ検索");}
        if(selected(host,"search")){tools.add("search");labels.add("投稿を探す");}
        if(selected(host,"checker")){tools.add("checker");labels.add("投稿検索・結果（必死チェッカー／Kyodemo）");}
        if(selected(host,"hissi")){tools.add("hissi");labels.add("必死チェッカーもどき専用ビュワー");}
        if(selected(host,"wacchoi")){tools.add("wacchoi");labels.add("ワッチョイ全文検索");}
        if(tools.isEmpty()){Toast.makeText(activity,"基本設定でタブ表示する画面を選んでください",Toast.LENGTH_LONG).show();return;}
        new HaiagaruDialog.Builder(activity).setTitle("タブに追加する画面")
            .setItems(labels.toArray(new String[0]),(dialog,index)->{
                if(open(target,tools.get(index))){if(activity!=target)activity.finish();}
                else Toast.makeText(activity,"タブを開けませんでした。タブ表示設定を確認してください",Toast.LENGTH_LONG).show();
            }).setNegativeButton("キャンセル",null).show();
    }
    private static Bundle pageArgs(Object page)throws ReflectiveOperationException{
        for(Field f:page.getClass().getDeclaredFields())if(f.getType()==Bundle.class){f.setAccessible(true);return (Bundle)f.get(page);}
        return null;
    }
    static boolean marker(Bundle args){return tool(args)!=null;}
    private static String tool(Bundle args){String value=args==null?null:args.getString("_data");for(String name:new String[]{"settings","eddi","search","checker","hissi","wacchoi"})if((PREFIX+name).equals(value))return name;return null;}
    // Several tabs share the Activity state transaction: never retain a full 120KB result per tab.
    static String compactState(String value){
        if(value==null||value.length()<=8192)return value;
        try{
            org.json.JSONObject json=new org.json.JSONObject(value);json.remove("threads");json.remove("resultQuery");
            String compact=json.toString();if(compact.length()<=8192)return compact;
        }catch(org.json.JSONException ignored){}
        return "{\"query\":\"https://eddiarchive3rd.boy.jp/\",\"page\":1,\"scroll\":0,\"expanded\":false}";
    }
    /** Replaces only the native WebView-tab constructor, including restored tabs. */
    public static Object createFragment(Object page){
        try{
            if(marker(pageArgs(page)))return new NativeToolFragment();
            return stockFragment();
        }catch(ReflectiveOperationException error){throw new IllegalStateException("Native tab constructor failed",error);}
    }
    private static Bundle arguments(Object fragment){
        try{return (Bundle)fragment.getClass().getMethod("getArguments").invoke(fragment);}
        catch(Exception error){throw new IllegalStateException("Native tool arguments unavailable",error);}
    }
    abstract static class Content {
        final View view;
        Content(View view){this.view=view;}
        abstract void save(Bundle state);
        void resume(){}
        void pause(){}
        void close(){}
    }
    static Content content(Object fragment,Activity activity,Bundle restored){
        Bundle args=arguments(fragment);
        if(!marker(args))throw new IllegalStateException("Not a Haiagaru tool tab");
        if((PREFIX+"eddi").equals(args.getString("_data"))){
            Bundle state=restored==null?new Bundle():new Bundle(restored);
            if(!state.containsKey(EddiArchiveSearchUi.STATE_KEY)&&args.containsKey(Intent.EXTRA_TEXT))state.putString(EddiArchiveSearchUi.STATE_KEY,args.getString(Intent.EXTRA_TEXT));
            EddiArchiveSearchUi ui=EddiArchiveSearchUi.embed(activity,Uri.parse("https://eddiarchive3rd.boy.jp/"),url->openThread(activity,url),state,()->back(activity));
            ui.onStateSaved=value->args.putString(Intent.EXTRA_TEXT,compactState(value));
            Content content=new Content(ui.rootView()){
                @Override void save(Bundle out){Bundle temporary=new Bundle();ui.saveState(temporary);String value=compactState(temporary.getString(EddiArchiveSearchUi.STATE_KEY));if(value!=null)out.putString(EddiArchiveSearchUi.STATE_KEY,value);}
                @Override void close(){ui.saveState(null);ui.dispose();}
            };
            content.save(new Bundle());return content;
        }
        if("settings".equals(tool(args)))return new SettingsContent(activity,args,restored,fragment);
        return new SearchContent(activity,args,restored,tool(args));
    }
    private static final class SearchContent extends Content {
        private final Activity activity;private final Bundle args;private final String tool;
        private ToolScreenController controller;
        private boolean closed;
        private int renderVersion;
        SearchContent(Activity activity,Bundle args,Bundle restored,String tool){
            super(new FrameLayout(activity));this.activity=activity;this.args=args;this.tool=tool;
            Intent launch=new Intent();Bundle state=restored;
            try{
                org.json.JSONObject saved=new org.json.JSONObject(args.getString(Intent.EXTRA_TEXT,"{}"));
                if(saved.has("launch"))launch=Intent.parseUri(saved.getString("launch"),Intent.URI_INTENT_SCHEME);
                if(state==null&&saved.has("form")){state=new Bundle();state.putParcelable("haiagaru.searchForm",Intent.parseUri(saved.getString("form"),Intent.URI_INTENT_SCHEME));}
                if(state==null)state=new Bundle();
                if(saved.has("resultsKey"))state.putString("haiagaru.tab.results",saved.getString("resultsKey"));
                state.putInt("haiagaru.tab.listIndex",saved.optInt("listIndex"));state.putInt("haiagaru.tab.listOffset",saved.optInt("listOffset"));
                if(saved.has("url")){state.putString("haiagaru.checker.url",saved.getString("url"));state.putInt("haiagaru.checker.scroll",saved.optInt("scroll"));}
            }catch(Exception error){android.util.Log.w("Haiagaru","Tool tab state could not be restored",error);}
            if("wacchoi".equals(tool))launch.putExtra("haiagaru.wacchoi.hissi",true);
            if("hissi".equals(tool))launch.putExtra("haiagaru.native.hissiViewer",true);
            if(("checker".equals(tool)||"hissi".equals(tool))&&launch.getData()==null){showAddressForm();return;}
            create(launch,state);
        }
        private void render(View content){
            final int revision=++renderVersion;
            // A form action can replace its own Compose tree. Leave its input
            // coroutine before disposing the old screen, just as for dialogs.
            new android.os.Handler(android.os.Looper.getMainLooper()).post(()->{
                if(closed||revision!=renderVersion||activity.isFinishing()||activity.isDestroyed())return;
                FrameLayout host=(FrameLayout)view;SettingsTabUi.clear(host);host.addView(content,new FrameLayout.LayoutParams(-1,-1));
            });
        }
        private void create(Intent launch,Bundle state){
            if("checker".equals(tool)||"hissi".equals(tool))launch.putExtra("haiagaru.native.hissiViewer",true);
            if(controller!=null){controller.pause();controller.close();}
            controller=("checker".equals(tool)||"hissi".equals(tool))?new HissiMenuController(activity,launch,true,()->back(activity),this::render):
                new HissiWacchoiSearchController(activity,launch,true,()->back(activity),this::render);
            controller.create(state);
        }
        private void showAddressForm(){
            LinearLayout form=new LinearLayout(activity);form.setOrientation(1);
            EditText address=new EditText(activity);address.setHint("必死チェッカー／Kyodemoの投稿者ページURL");address.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_VARIATION_URI);form.addView(address);
            Button open=new Button(activity);open.setText("投稿者ページを開く");form.addView(open);
            open.setOnClickListener(v->{Uri uri=Uri.parse(address.getText().toString().trim());String host=uri.getHost();
                boolean allowedHost="hissi".equals(tool)?"hissi.org".equalsIgnoreCase(host):("hissi.org".equalsIgnoreCase(host)||"www.kyodemo.net".equalsIgnoreCase(host)||"kyodemo.net".equalsIgnoreCase(host));
                if(!("http".equals(uri.getScheme())||"https".equals(uri.getScheme()))||!allowedHost){address.setError("hissi".equals(tool)?"hissi.org のURLを入力してください":"必死チェッカーまたはKyodemoのURLを入力してください");return;}
                create(new Intent(Intent.ACTION_VIEW,uri),null);controller.resume();});
            if("hissi".equals(tool)){
                TextView heading=new TextView(activity);heading.setText("必死チェッカー · ChMateビュー");heading.setTextSize(18);form.addView(heading,0);
                render(ToolScreenUi.model(activity,"",form,()->back(activity),false));
            }else render(ToolScreenUi.model(activity,"",form,()->back(activity),false));
        }
        @Override void resume(){if(controller!=null)controller.resume();}
        @Override void pause(){if(controller!=null)controller.pause();}
        @Override void save(Bundle out){
            if(controller==null)return;
            controller.save(out);
            try{
                org.json.JSONObject snapshot=new org.json.JSONObject().put("launch",controller.getIntent().toUri(Intent.URI_INTENT_SCHEME));
                Intent form=out.getParcelable("haiagaru.searchForm");if(form!=null)snapshot.put("form",form.toUri(Intent.URI_INTENT_SCHEME));
                String resultsKey=out.getString("haiagaru.tab.results");if(ToolTabResults.valid(resultsKey))snapshot.put("resultsKey",resultsKey);
                snapshot.put("listIndex",out.getInt("haiagaru.tab.listIndex")).put("listOffset",out.getInt("haiagaru.tab.listOffset"));
                String url=out.getString("haiagaru.checker.url");if(url!=null)snapshot.put("url",url).put("scroll",out.getInt("haiagaru.checker.scroll"));
                args.putString(Intent.EXTRA_TEXT,snapshot.toString());
            }catch(org.json.JSONException error){android.util.Log.w("Haiagaru","Tool tab state could not be saved",error);}
        }
        @Override void close(){closed=true;renderVersion++;if(controller!=null){controller.pause();controller.close();controller=null;}SettingsTabUi.clear((FrameLayout)view);}
    }
    @SuppressWarnings("deprecation") private static void back(Activity activity){activity.onBackPressed();}
    private static boolean openThread(Activity activity,String url){
        if(!HissiLinkRouting.isThreadUrl(url))return false;
        Intent intent=new Intent(Intent.ACTION_VIEW,Uri.parse(url));intent.setClass(activity,OpenUrlActivity.class);
        if(Haiagaru.archiveThreadStandalone(activity))intent.putExtra("haiagaru.archive.return",true);
        activity.startActivity(intent);return true;
    }
    /** The native widgets retain their normal callbacks; no second Activity owns this tab. */
    private static final class SettingsContent extends Content {
        private static final String SELECTED="haiagaru.native.settings.category";
        final Activity activity;final Bundle args;final LinearLayout root,body;final TextView title;final ScrollView scroll;final FrameLayout categoryHost;final Object fragment;
        String selected;
        final String[] ids={"DISPLAY","SOCIAL","CHECKER","ARCHIVE","POSTING","UPLOAD","MEDIA","NG","HISTORY","BACKUP","GENERAL","ADVANCED"};
        final String[] labels={"画面と操作","リンクカード・X","IDチェッカー","過去ログ・保存レス","書き込む","画像・動画の投稿","画像・動画の表示","NG・ミュート","履歴","バックアップ","基本設定","通信・外部板"};
        SettingsContent(Activity activity,Bundle args,Bundle restored,Object fragment){
            super(new LinearLayout(activity));this.activity=activity;this.args=args;this.fragment=fragment;root=(LinearLayout)view;root.setOrientation(1);
            int padding=(int)(12*activity.getResources().getDisplayMetrics().density);root.setPadding(padding,padding,padding,padding);
            LinearLayout bar=new LinearLayout(activity);Button back=new Button(activity);back.setText("戻る");
            back.setOnClickListener(v->{if(selected!=null)show(null);else NativeToolTabs.back(activity);});bar.addView(back);
            title=new TextView(activity);title.setTextSize(20);bar.addView(title,new LinearLayout.LayoutParams(0,-2,1));root.addView(bar);
            scroll=new ScrollView(activity);scroll.setFillViewport(true);body=new LinearLayout(activity);body.setOrientation(1);scroll.addView(body);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
            categoryHost=new FrameLayout(activity);root.addView(categoryHost,new LinearLayout.LayoutParams(-1,0,1));
            show(restored==null?args.getString(Intent.EXTRA_TEXT):restored.getString(SELECTED,args.getString(Intent.EXTRA_TEXT)));
        }
        void show(String category){
            selected=null;for(String id:ids)if(id.equals(category))selected=id;
            body.removeAllViews();title.setText("Haiagaru設定");
            SettingsTabUi.clear(categoryHost);
            scroll.setVisibility(selected==null?View.VISIBLE:View.GONE);
            categoryHost.setVisibility(selected==null?View.GONE:View.VISIBLE);
            if(selected==null){
                for(int i=0;i<ids.length;i++){final String id=ids[i];Button button=new Button(activity);button.setText(labels[i]);button.setOnClickListener(v->show(id));body.addView(button,new LinearLayout.LayoutParams(-1,-2));}
                Button share=new Button(activity);share.setText("設定を共有（.haiagaru）");share.setOnClickListener(v->SettingsProfiles.share(activity));body.addView(share);
                Button load=new Button(activity);load.setText("設定ファイルを読み込む");load.setOnClickListener(v->{try{fragment.getClass().getMethod("openSettingsFile").invoke(fragment);}catch(Exception error){android.util.Log.e("Haiagaru","Settings picker unavailable",error);Toast.makeText(activity,"ファイル選択画面を開けませんでした",Toast.LENGTH_LONG).show();}});body.addView(load);
                TextView help=new TextView(activity);help.setText("タブ表示をオフにすると、次に開く追加画面は従来の別画面になります。開いているタブは自動では閉じません。");body.addView(help);
            }else{
                for(int i=0;i<ids.length;i++)if(ids[i].equals(selected))title.setText(labels[i]);
                // Keep Compose's vertical list bounded by the pane, never inside ScrollView.
                categoryHost.addView(SettingsTabUi.category(activity,selected),new FrameLayout.LayoutParams(-1,-1));
            }
            if(selected==null)args.remove(Intent.EXTRA_TEXT);else args.putString(Intent.EXTRA_TEXT,selected);
            scroll.post(()->scroll.scrollTo(0,0));
        }
        @Override void save(Bundle state){if(selected!=null)state.putString(SELECTED,selected);}
        @Override void close(){SettingsTabUi.clear(categoryHost);Haiagaru.settingsClosed();}
    }
}
