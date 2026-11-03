package app.morphe.extension.chmate;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.view.View;
import android.widget.*;
import java.util.*;
import java.util.concurrent.*;

/** A read-only list of refresh registrations, reached from the stock history popup. */
public final class RefreshTargetHistory {
    private static final int TAG=0x48690e11;
    private static final ExecutorService IO=Executors.newSingleThreadExecutor();
    static final class Entry {
        final String key,label,url;
        Entry(String key,String label,String url){this.key=key;this.label=label;this.url=url;}
    }
    public static void attach(PopupWindow popup){
        View original=popup.getContentView();if(original==null)return;
        try{
            Context context=original.getContext();
            Button button=original.findViewWithTag(Integer.valueOf(TAG));
            if(button==null){
                if(original.isAttachedToWindow())return;
                LinearLayout wrapper=new LinearLayout(context);wrapper.setOrientation(LinearLayout.VERTICAL);
                button=new Button(context);button.setTag(Integer.valueOf(TAG));button.setAllCaps(false);
                wrapper.addView(button,new LinearLayout.LayoutParams(-1,-2));
                wrapper.addView(original,new LinearLayout.LayoutParams(-1,0,1));
                popup.setContentView(wrapper);
                button.setOnClickListener(v->{
                    Activity activity=Haiagaru.toolbarActivity(null);if(activity==null)return;
                    popup.dismiss();show(activity);
                });
            }
            button.setText("更新対象のスレ（"+RefreshTargets.selected(context).size()+"）");
        }catch(Exception error){android.util.Log.w("Haiagaru","Refresh history entry unavailable",error);}
    }
    static List<Entry> load(Context context,Set<String> keys)throws Exception{
        ArrayList<Entry> result=new ArrayList<>();
        String version=context.getPackageManager().getPackageInfo(context.getPackageName(),0).versionName;
        try(SQLiteDatabase db=SQLiteDatabase.openDatabase(context.getDatabasePath("roidon.sqlite").getAbsolutePath(),null,SQLiteDatabase.OPEN_READONLY)){
            for(String key:keys){
                String[] parts=key.split("\\n",-1);if(parts.length!=2)continue;
                long id;try{id=Long.parseLong(parts[1]);if(id<=0)continue;}catch(NumberFormatException invalid){continue;}
                String title=parts[1],boardTitle=parts[0],server=null;
                try(Cursor row=db.rawQuery("SELECT bookmarks.title, boards.title, boards.server FROM bookmarks LEFT JOIN boards ON boards.name=bookmarks.name WHERE bookmarks.name=? AND bookmarks.created=? LIMIT 1",new String[]{parts[0],parts[1]})){
                    if(row.moveToFirst()){
                        if(!row.isNull(0)&&!row.getString(0).isEmpty())title=row.getString(0);
                        if(!row.isNull(1)&&!row.getString(1).isEmpty())boardTitle=row.getString(1);
                        server=row.getString(2);
                    }
                }
                String url=null;try{url=url(context,version,parts[0],id,server);}catch(Exception unavailable){/* Keep registration visible. */}
                result.add(new Entry(key,title+"\n"+boardTitle+(url==null?"（保存情報を確認できません）":""),url));
            }
        }
        java.text.Collator order=java.text.Collator.getInstance(Locale.JAPANESE);
        result.sort((a,b)->{int n=order.compare(a.label,b.label);return n==0?a.key.compareTo(b.key):n;});return result;
    }
    private static String url(Context context,String version,String key,long id,String server)throws Exception{
        boolean old=version.contains("191"),middle=version.contains("226");
        ClassLoader loader=context.getClassLoader();
        Class<?> boardType=Class.forName(old?"jp.syoboi.a2chMate.client.BBSUrlInfo$BoardID":"jp.syoboi.a2chMate.client.BoardID",false,loader);
        Object board;
        if(old)board=boardType.getConstructor(String.class).newInstance(key);
        else if(middle)board=boardType.getMethod("b",String.class).invoke(null,key);
        else{
            Class<?> companion=Class.forName("jp.syoboi.a2chMate.client.BoardID$RemoteActionCompatParcelizer",false,loader);
            board=companion.getMethod("c",String.class).invoke(null,key);
        }
        if(server==null||server.isEmpty())server=(String)NativeFields.find(boardType,old?"a":middle?"d":"c").get(board);
        if(server==null||server.isEmpty())return null;
        Class<?> infoType=Class.forName("jp.syoboi.a2chMate.client.BBSUrlInfo",false,loader);
        String path=(String)NativeFields.find(boardType,old?"d":middle?"b":"d").get(board);
        int kind;
        if(old)kind=((Number)infoType.getMethod("e",String.class).invoke(null,server)).intValue();
        else if(middle)kind=((Number)infoType.getMethod("d",String.class).invoke(null,server)).intValue();
        else{
            kind=((Number)infoType.getMethod("c",String.class).invoke(null,server)).intValue();
        }
        Object info=infoType.getConstructor(String.class,String.class,Long.class,int.class,Integer.class,String.class).newInstance(server,path,Long.valueOf(id),kind,null,null);
        String url=(String)infoType.getMethod(old?"d":middle?"e":"c",int.class).invoke(info,1);
        Uri uri=Uri.parse(url);return ("http".equals(uri.getScheme())||"https".equals(uri.getScheme()))&&uri.getHost()!=null?url:null;
    }
    static void show(Activity activity){
        Set<String> keys=RefreshTargets.selected(activity);
        if(keys.isEmpty()){
            new HaiagaruDialog.Builder(activity).setTitle("更新対象のスレ").setMessage("登録はありません。スレタイトルのメニューで「更新対象に含む」をオンにすると、ここに並びます。").setPositiveButton("閉じる",null).show();return;
        }
        IO.execute(()->{
            try{
                List<Entry> entries=load(activity.getApplicationContext(),keys);
                activity.runOnUiThread(()->{
                    if(activity.isFinishing()||activity.isDestroyed())return;
                    String[] labels=new String[entries.size()];for(int i=0;i<labels.length;i++)labels[i]=entries.get(i).label;
                    new HaiagaruDialog.Builder(activity).setTitle("更新対象のスレ（"+entries.size()+"）").setItems(labels,(dialog,index)->{
                        String url=entries.get(index).url;
                        if(url==null){Toast.makeText(activity,"保存された板情報を取得できませんでした",Toast.LENGTH_LONG).show();return;}
                        activity.startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(url)).setClassName(activity,"jp.syoboi.a2chMate.activity.ResListActivity"));
                    }).setNegativeButton("閉じる",null).show();
                });
            }catch(Exception error){activity.runOnUiThread(()->{if(!activity.isFinishing())Toast.makeText(activity,"更新対象の一覧を読み込めませんでした",Toast.LENGTH_LONG).show();});android.util.Log.w("Haiagaru","Refresh history unavailable",error);}
        });
    }
    private RefreshTargetHistory(){}
}
