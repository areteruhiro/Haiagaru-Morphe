package app.morphe.extension.chmate;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.widget.*;
import java.io.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.json.JSONArray;

/** Foreground-only checks. No alarms, background service or MEGA dependency. */
final class LocalAutoBackup {
    private static final String PREFS="haiagaru.local-auto-backup";
    private static final AtomicBoolean REGISTERED=new AtomicBoolean(), RUNNING=new AtomicBoolean();
    private static long lastAttempt;
    private static SharedPreferences prefs(Context c){return c.getSharedPreferences(PREFS,0);}
    static void initialize(Context c){
        if(!(c instanceof Application)||!REGISTERED.compareAndSet(false,true))return;
        android.os.Handler handler=new android.os.Handler(android.os.Looper.getMainLooper());
        ForegroundBackupChecks checks=new ForegroundBackupChecks(new ForegroundBackupChecks.Queue(){
            public void post(Runnable task,long delay){handler.postDelayed(task,delay);}
            public void remove(Runnable task){handler.removeCallbacks(task);}
        },()->maybeRun(c));
        ((Application)c).registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks(){
            public void onActivityStarted(Activity a){checks.started();}
            public void onActivityStopped(Activity a){checks.stopped();}
            public void onActivityCreated(Activity a,Bundle b){}
            public void onActivityResumed(Activity a){}
            public void onActivityPaused(Activity a){}
            public void onActivitySaveInstanceState(Activity a,Bundle b){}
            public void onActivityDestroyed(Activity a){}
        });
    }
    static boolean due(long now,long last,int hours){
        return last<=0||now<last||now-last>=Math.max(1,hours)*3600000L;
    }
    private static void maybeRun(Context c){
        SharedPreferences p=prefs(c);long now=System.currentTimeMillis();
        if(!p.getBoolean("enabled",false)||p.getString("tree","").isEmpty()
                ||!due(now,p.getLong("last",0),p.getInt("hours",24)))return;
        // Avoid repeated failures while opening settings/pickers or returning from another app.
        long uptime=android.os.SystemClock.elapsedRealtime();
        synchronized(LocalAutoBackup.class){if(lastAttempt!=0&&uptime-lastAttempt<300000)return;lastAttempt=uptime;}
        run(c);
    }
    static void addButton(LinearLayout parent,Activity a){
        Button button=new Button(a);button.setText("ローカル自動バックアップ");
        button.setOnClickListener(v->show(a));parent.addView(button);
    }
    static void show(Activity a){
        SharedPreferences p=prefs(a);
        LinearLayout body=new LinearLayout(a);body.setOrientation(1);
        int pad=(int)(16*a.getResources().getDisplayMetrics().density);body.setPadding(pad,pad,pad,pad);
        TextView note=new TextView(a);note.setText("ChMateを開いたときと、開いている間は約1分ごとに確認し、前回の成功から指定間隔を過ぎていれば保存します。バックグラウンド・終了中は新しいバックアップを開始しません。開始済みの保存は続行します。\n"
                +"ZIPは暗号化されません。Cookie・認証情報などを含める場合は保存先の管理に注意してください。"
                +"アプリ専用フォルダ外の、端末内フォルダを選んでください。大量のログを含めると時間と空き容量が必要です。");body.addView(note);
        Switch enabled=new Switch(a);enabled.setText("ローカル自動バックアップを有効にする");enabled.setChecked(p.getBoolean("enabled",false));body.addView(enabled);
        TextView destination=new TextView(a);destination.setText("保存先："+folderLabel(p.getString("tree","")));body.addView(destination);
        Button folder=new Button(a);folder.setText("保存先フォルダを選ぶ");folder.setOnClickListener(v->{
            a.startActivity(new Intent(a,HaiagaruLocalBackupActivity.class).putExtra(HaiagaruLocalBackupActivity.EXTRA_MODE,HaiagaruLocalBackupActivity.MODE_FOLDER));
        });body.addView(folder);
        enabled.setOnCheckedChangeListener((v,on)->{
            if(on&&p.getString("tree","").isEmpty()){enabled.setChecked(false);Toast.makeText(a,"先に保存先フォルダを選んでください",Toast.LENGTH_LONG).show();return;}
            p.edit().putBoolean("enabled",on).apply();
        });
        EditText hours=number(body,a,"実行間隔（時間、1〜8760）",p.getInt("hours",24));
        EditText keep=number(body,a,"保持件数（1〜100）",p.getInt("keep",3));
        LiveSettings live=new LiveSettings(p,()->{});live.number(hours,"hours",1,8760);live.number(keep,"keep",1,100);
        TextView retention=new TextView(a);retention.setText("保持件数を超えた分は、保存成功後に古い順で完全削除します。この機能が同じ保存先へ作ったZIPだけが対象です。他のファイルは削除しません。");body.addView(retention);
        int flags=p.getInt("categories",HaiagaruSyncSnapshot.DEFAULT);
        for(int i=0;i<HaiagaruMegaSync.CATEGORY_BITS.length;i++){
            int bit=HaiagaruMegaSync.CATEGORY_BITS[i];CheckBox check=new CheckBox(a);
            check.setText(HaiagaruMegaSync.CATEGORY_NAMES[i]);check.setChecked((flags&bit)!=0);body.addView(check);
            check.setOnCheckedChangeListener((v,on)->{int old=p.getInt("categories",HaiagaruSyncSnapshot.DEFAULT);p.edit().putInt("categories",on?old|bit:old&~bit).apply();});
        }
        TextView status=new TextView(a);long last=p.getLong("last",0);
        status.setText("前回成功："+(last==0?"未実行":BackupTime.display(last))+"\n"+p.getString("status",""));body.addView(status);
        Button now=new Button(a);now.setText("今すぐバックアップ");now.setOnClickListener(v->run(a.getApplicationContext()));body.addView(now);
        ScrollView scroll=new ScrollView(a);scroll.addView(body);
        AlertDialog dialog=new AlertDialog.Builder(a).setTitle("ローカル自動バックアップ").setView(scroll).setNegativeButton("戻る",null).create();
        Runnable refresh=()->{
            if(a.isFinishing()||a.isDestroyed())return;
            destination.setText("保存先："+folderLabel(p.getString("tree","")));
            long latest=p.getLong("last",0);
            status.setText("前回成功："+(latest==0?"未実行":BackupTime.display(latest))+"\n"+p.getString("status",""));
        };
        SharedPreferences.OnSharedPreferenceChangeListener listener=(changed,key)->{
            if(key==null||key.equals("tree")||key.equals("last")||key.equals("status"))a.runOnUiThread(refresh);
        };
        dialog.setOnShowListener(d->{p.registerOnSharedPreferenceChangeListener(listener);refresh.run();});
        dialog.setOnDismissListener(d->p.unregisterOnSharedPreferenceChangeListener(listener));
        dialog.show();
    }
    private static EditText number(LinearLayout body,Activity a,String label,int value){
        TextView text=new TextView(a);text.setText(label);body.addView(text);
        EditText input=new EditText(a);input.setInputType(2);input.setText(Integer.toString(value));body.addView(input);return input;
    }
    static String folderLabel(String value){
        if(value==null||value.isEmpty())return "未選択";
        try{
            Uri tree=Uri.parse(value);
            // getTreeDocumentId already decodes UTF-8 once; do not URL-decode it a second time.
            String id=DocumentsContract.getTreeDocumentId(tree);
            if("com.android.externalstorage.documents".equals(tree.getAuthority())){
                if(id.startsWith("primary:"))return "内部ストレージ/"+id.substring("primary:".length());
                return id.replaceFirst(":","/");
            }
            return id;
        }catch(Exception ignored){return Uri.decode(value);}
    }
    static synchronized void setFolder(Context c,Uri tree)throws IOException{
        if(tree==null)throw new IOException("保存先フォルダが選択されていません");
        SharedPreferences p=prefs(c);String selected=tree.toString();
        if(selected.equals(p.getString("tree","")))return;
        // Keep the old folder's tracked files untouched; never prune across a folder change.
        if(!p.edit().putString("tree",selected).remove("files").remove("last").remove("status").commit())
            throw new IOException("保存先の設定を保存できませんでした");
    }
    private static void run(Context c){
        if(!RUNNING.compareAndSet(false,true))return;
        new Thread(()->{
            File snapshot=null;Uri created=null;boolean complete=false;
            SharedPreferences p=prefs(c);
            try{
                String destination=p.getString("tree","");if(destination.isEmpty())throw new IOException("保存先フォルダを選んでください");
                int flags=p.getInt("categories",HaiagaruSyncSnapshot.DEFAULT)&HaiagaruSyncSnapshot.ALL;
                if(flags==0)throw new IOException("バックアップする項目を選んでください");
                Uri tree=Uri.parse(destination);
                snapshot=HaiagaruBackupArchive.capture(c,flags);
                Uri parent=DocumentsContract.buildDocumentUriUsingTree(tree,DocumentsContract.getTreeDocumentId(tree));
                created=DocumentsContract.createDocument(c.getContentResolver(),parent,"application/zip",
                        "haiagaru-auto-"+android.os.Build.MODEL.replaceAll("[^A-Za-z0-9_-]","_")+"-"+BackupTime.filename(System.currentTimeMillis())+".zip");
                if(created==null)throw new IOException("保存ファイルを作成できません");
                try(InputStream input=new FileInputStream(snapshot);OutputStream output=c.getContentResolver().openOutputStream(created,"w")){
                    if(output==null)throw new IOException("保存先を開けません");
                    if(HaiagaruBackupArchive.copy(input,output)!=snapshot.length())throw new IOException("保存サイズが一致しません");
                }
                complete=true;
                synchronized(LocalAutoBackup.class){
                    if(!destination.equals(p.getString("tree","")))return;
                    JSONArray files=new JSONArray(p.getString("files","[]"));files.put(created.toString());
                    // Persist ownership before pruning so a failed deletion can be retried safely.
                    if(!p.edit().putString("files",files.toString()).putLong("last",System.currentTimeMillis()).putString("status","バックアップ成功").commit())
                        throw new IOException("バックアップは保存されましたが管理記録を保存できません（古いファイルは削除しません）");
                    int excess=files.length()-Math.max(1,p.getInt("keep",3));JSONArray retained=new JSONArray();
                    for(int i=0;i<files.length();i++){
                        String value=files.getString(i);boolean deleted=false;
                        if(i<excess)try{Uri old=Uri.parse(value);
                            if(tree.getAuthority().equals(old.getAuthority())&&DocumentsContract.getTreeDocumentId(tree).equals(DocumentsContract.getTreeDocumentId(old))){
                                try(android.database.Cursor cursor=c.getContentResolver().query(old,new String[]{DocumentsContract.Document.COLUMN_DISPLAY_NAME},null,null,null)){
                                    if(cursor!=null&&cursor.moveToFirst()&&cursor.getString(0).startsWith("haiagaru-auto-"))
                                        deleted=DocumentsContract.deleteDocument(c.getContentResolver(),old);
                                }
                            }
                        }catch(Exception ignored){}
                        if(!deleted)retained.put(value);
                    }
                    p.edit().putString("files",retained.toString()).apply();
                }
            }catch(Exception error){p.edit().putString("status","バックアップ失敗："+error.getMessage()).apply();android.util.Log.w("Haiagaru","Local auto backup failed",error);}
            finally{
                if(snapshot!=null)snapshot.delete();
                if(!complete&&created!=null)try{DocumentsContract.deleteDocument(c.getContentResolver(),created);}catch(Exception ignored){}
                RUNNING.set(false);
            }
        },"Haiagaru-local-auto-backup").start();
    }
}
