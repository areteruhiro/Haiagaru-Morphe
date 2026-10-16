package app.morphe.extension.chmate;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.CookieManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.WeakHashMap;

/** Explicit HTTPS-origin opt-in; native HTTP never receives unrelated WebView cookies. */
public final class ExternalBoardAuth {
    private static volatile Context context;
    private static WeakReference<Activity> resumed = new WeakReference<>(null);
    private static final Map<Activity,String> targets = new WeakHashMap<>();
    private static final Map<Activity,WeakReference<android.view.View>> editors = new WeakHashMap<>();
    private static final Map<MenuItem,WeakReference<Activity>> menus = new WeakHashMap<>();
    private static final Map<Activity,String> prompted = new WeakHashMap<>();
    private static final int MENU = 0x48690061;
    private ExternalBoardAuth() {}
    static SharedPreferences prefs(Context c) { return c.getSharedPreferences("haiagaru.external-board-auth",0); }
    public static void initialize(Context c) { context=c.getApplicationContext(); }
    public static void resumed(Activity a) {
        if(resumed.get() instanceof ExternalBoardAuthActivity) prompted.remove(a);
        resumed=new WeakReference<>(a);
    }
    static Activity activity(Object owner) {
        if(owner instanceof Activity) return (Activity)owner;
        try { return (Activity)owner.getClass().getMethod("getActivity").invoke(owner); }
        catch(Exception ignored) { return null; }
    }
    public static void target(Object owner, String url) {
        if(url!=null && url.startsWith("http://")) url="https://"+url.substring(7);
        Activity a=activity(owner);
        if(a!=null && !BoardAuthRules.origin(url).isEmpty()) {
            targets.put(a,url);
            try { editors.put(a,new WeakReference<>((android.view.View)owner.getClass().getMethod("getView").invoke(owner))); }
            catch(Exception ignored) { }
        }
    }
    public static void addPostingMenu(Object owner, Menu menu) {
        Activity a=activity(owner);
        if(a==null || menu==null || menu.findItem(MENU)!=null) return;
        MenuItem item=menu.add(Menu.NONE,MENU,Menu.NONE,"外部板の認証ページ");
        menus.put(item,new WeakReference<>(a));
        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        item.setOnMenuItemClickListener(v->handlePostingMenu(owner,v));
    }
    public static boolean handlePostingMenu(Object owner, MenuItem item) {
        if(item==null || item.getItemId()!=MENU) return false;
        Activity a=activity(owner);
        if(a==null) return false;
        String board=targets.get(a);
        if(board==null) showBoards(a); else configure(a,board,"");
        return true;
    }
    public static boolean handlePostingActivityMenu(MenuItem item) {
        WeakReference<Activity> ref=menus.get(item);
        return ref!=null && ref.get()!=null && handlePostingMenu(ref.get(),item);
    }
    public static void addSettingsButton(LinearLayout layout, Activity a) {
        Button button=new Button(a);button.setAllCaps(false);button.setText("外部板の認証ページを設定・開く");
        layout.addView(button);button.setOnClickListener(v->showBoards(a));
    }
    private static void showBoards(Activity a) {
        new Thread(()->{
            ArrayList<String> urls=new ArrayList<>(), names=new ArrayList<>();
            try(SQLiteDatabase db=SQLiteDatabase.openDatabase(a.getDatabasePath("roidon.sqlite").getPath(),null,SQLiteDatabase.OPEN_READONLY);
                Cursor rows=db.rawQuery("SELECT name,server,title FROM boards WHERE is_external<>0 ORDER BY title",null)) {
                while(rows.moveToNext()) {
                    String id=java.net.URLDecoder.decode(rows.getString(0),"UTF-8");
                    String path=id.contains("/") ? id.substring(id.indexOf('/')+1) : id;
                    String url="https://"+rows.getString(1)+"/"+path+"/";
                    if(!BoardAuthRules.origin(url).isEmpty()) {urls.add(url);names.add(rows.getString(2)+"\n"+url);}
                }
                a.runOnUiThread(()->{
                    if(a.isFinishing()) return;
                    new AlertDialog.Builder(a).setTitle("認証する外部板")
                        .setItems(names.toArray(new String[0]),(d,i)->configure(a,urls.get(i),""))
                        .setNeutralButton("URLを指定",(d,w)->configure(a,"",""))
                        .setNegativeButton("閉じる",null).show();
                });
            } catch(Exception error) {
                a.runOnUiThread(()->configure(a,"",""));
            }
        },"Haiagaru-board-auth-list").start();
    }
    static void configure(Activity a,String board,String suggestion) {
        LinearLayout box=new LinearLayout(a);box.setOrientation(LinearLayout.VERTICAL);
        int pad=(int)(20*a.getResources().getDisplayMetrics().density);box.setPadding(pad,0,pad,0);
        EditText boardInput=new EditText(a);boardInput.setSingleLine(true);boardInput.setHint("板のURL（https://…/板名/）");boardInput.setText(board);box.addView(boardInput);
        String origin=BoardAuthRules.origin(board);
        EditText auth=new EditText(a);auth.setSingleLine(true);auth.setHint("認証ページのURL（https://…）");
        auth.setText(prefs(a).getString("url:"+origin,suggestion.isEmpty()?BoardAuthRules.preset(board):suggestion));box.addView(auth);
        AlertDialog dialog=new AlertDialog.Builder(a).setTitle("外部板の認証")
            .setMessage("サイト上で認証コードなどを入力してください。完了後は書き込み画面へ戻ります。投稿の自動再送信はしません。\n認証URLは板に応じて変更できます。")
            .setView(box).setNegativeButton("キャンセル",null)
            .setNeutralButton("連携を解除",(d,w)->{
                String key=BoardAuthRules.origin(boardInput.getText().toString());
                prefs(a).edit().remove("enabled:"+key).remove("url:"+key).apply();
            }).setPositiveButton("認証ページを開く",null).create();
        dialog.setOnShowListener(d->dialog.getButton(-1).setOnClickListener(v->{
            try {
                String b=BoardAuthRules.validate(boardInput.getText().toString());
                String u=BoardAuthRules.validate(auth.getText().toString());
                prefs(a).edit().putString("url:"+BoardAuthRules.origin(b),u).apply();
                a.startActivity(new Intent(a,ExternalBoardAuthActivity.class).putExtra("board",b).putExtra("auth",u));dialog.dismiss();
            }catch(IllegalArgumentException e){auth.setError(e.getMessage());}
        }));dialog.show();
    }
    /** Observe native error UI only; do not parse the draft or intercept a successful post. */
    public static void observe(CharSequence message) {
        if(message==null || !BoardAuthRules.required(message.toString())) return;
        String text=message.toString();
        new Handler(Looper.getMainLooper()).postDelayed(()->{
            Activity a=resumed.get(); if(a==null || a.isFinishing() || a instanceof ExternalBoardAuthActivity) return;
            WeakReference<android.view.View> editor=editors.get(a);
            if(editor==null || editor.get()==null || !editor.get().isShown()) return;
            String board=targets.get(a);if(board==null) return;
            if(text.equals(prompted.get(a))) return;
            prompted.put(a,text);
            String suggested=BoardAuthRules.suggested(board,text);
            new AlertDialog.Builder(a).setTitle("板の認証が必要です")
                .setMessage("アプリ内で認証ページを開きますか？ 認証後に投稿内容を確認して、再度書き込んでください。")
                .setNegativeButton("後で",null).setPositiveButton("認証ページ",(d,w)->configure(a,board,suggested)).show();
        },400);
    }
    public static Object parseCookie(Object url,String cookie) { return null; } // Bound at patch time.
    public static void received(Object url,List<?> cookies) {
        Context c=context;if(c==null || url==null) return;
        String address=url.toString(),origin=BoardAuthRules.origin(address);
        if(origin.isEmpty() || !prefs(c).getBoolean("enabled:"+origin,false)) return;
        try {
            CookieManager manager=CookieManager.getInstance();
            for(Object cookie:cookies) manager.setCookie(address,cookie.toString(),null);
        } catch(Exception ignored) { }
    }
    public static List<?> cookies(List<?> nativeCookies,Object url) {
        Context c=context;if(c==null || url==null) return nativeCookies;
        String address=url.toString(), origin=BoardAuthRules.origin(address);
        if(origin.isEmpty() || !prefs(c).getBoolean("enabled:"+origin,false)) return nativeCookies;
        try {
            String header=CookieManager.getInstance().getCookie(address);
            if(header==null || header.isEmpty()) return nativeCookies;
            LinkedHashMap<String,Object> web=new LinkedHashMap<>();
            for(String pair:header.split(";")) {
                Object parsed=parseCookie(url,pair.trim());
                if(parsed!=null) web.put(pair.trim().split("=",2)[0],parsed);
            }
            ArrayList<Object> result=new ArrayList<>();
            for(Object cookie:nativeCookies) if(!web.containsKey(cookie.toString().split("=",2)[0])) result.add(cookie);
            result.addAll(web.values());return result;
        } catch(Exception ignored) { return nativeCookies; }
    }
}
