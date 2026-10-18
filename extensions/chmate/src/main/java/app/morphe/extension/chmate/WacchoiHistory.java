package app.morphe.extension.chmate;

import android.app.*;
import android.content.*;
import android.util.AtomicFile;
import android.text.*;
import android.widget.*;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;

/** Private, bounded query/result snapshots. Reading a snapshot never starts a search. */
final class WacchoiHistory {
    private static final ExecutorService IO=Executors.newSingleThreadExecutor();
    private static volatile long generation;
    static final int MAX_FILE=Integer.MAX_VALUE-8; // Java JSON/byte-array representation ceiling, not retention policy.
    static File directory(Context c){return new File(c.getFilesDir(),"haiagaru-wacchoi-history");}
    static final class Session {
        final Context context;final JSONObject data;final JSONArray records=new JSONArray();final long epoch;
        boolean queued;long characters;
        Session(Context c,Intent args)throws Exception{
            context=c.getApplicationContext();epoch=generation;
            data=new JSONObject().put("id",UUID.randomUUID().toString()).put("time",System.currentTimeMillis())
                    .put("four",args.getStringExtra("four")).put("upper",args.getBooleanExtra("upper",false))
                    .put("edge",args.getBooleanExtra("edge",false)).put("boards",args.getStringExtra("boards"))
                    .put("from",args.getStringExtra("from")).put("to",args.getStringExtra("to"))
                    .put("limit",args.getIntExtra("limit",20)).put("status","開始しています");
        }
        synchronized void add(int kind,String a,String b,String c,String d){
            try{long size=(long)a.length()+b.length()+c.length()+d.length();
                if(records.length()<WacchoiStorageLimits.bound(context,3)&&characters+size<=WacchoiStorageLimits.bound(context,4)){records.put(new JSONObject().put("kind",kind).put("a",a).put("b",b).put("c",c).put("d",d));characters+=size;}
                else data.put("truncated",true);
            }catch(Exception e){report(e);}
        }
        synchronized void status(String value){try{data.put("status",value);}catch(Exception e){report(e);}save();}
        synchronized void save(){
            if(queued)return;queued=true;
            IO.execute(()->{
                synchronized(WacchoiHistory.class){
                    synchronized(this){queued=false;if(epoch!=generation)return;
                        try{
                            data.put("results",new JSONArray(records.toString()));
                            JSONArray out=data.getJSONArray("results");
                            byte[] bytes=data.toString().getBytes(StandardCharsets.UTF_8);
                            while(bytes.length>WacchoiStorageLimits.scaled(context,2,1024L*1024)&&out.length()>0){out.remove(out.length()-1);data.put("truncated",true);bytes=data.toString().getBytes(StandardCharsets.UTF_8);}
                            File dir=directory(context);if(!dir.isDirectory()&&!dir.mkdirs())throw new IOException("history directory");
                            AtomicFile file=new AtomicFile(new File(dir,data.getString("id")+".json"));
                            FileOutputStream stream=null;try{stream=file.startWrite();stream.write(bytes);file.finishWrite(stream);stream=null;}finally{if(stream!=null)file.failWrite(stream);}
                            trim(context,dir);
                        }catch(Exception error){report(error);}
                    }
                }
            });
        }
    }
    static Session begin(Context c,Intent args){
        try{if(!c.getSharedPreferences(WacchoiSearchOptions.PREFS,0).getBoolean("wacchoiSearchHistory",true))return null;
            Session session=new Session(c,args);session.save();return session;
        }catch(Exception error){report(error);return null;}
    }
    static List<JSONObject> read(Context c)throws Exception{
        ArrayList<JSONObject> rows=new ArrayList<>();
        for(File file:files(directory(c))){
            if(file.length()>MAX_FILE)continue;
            try(InputStream in=new AtomicFile(file).openRead();ByteArrayOutputStream out=new ByteArrayOutputStream()){
                byte[] block=new byte[8192];int count;long total=0;while((count=in.read(block))!=-1){total+=count;if(total>MAX_FILE)throw new IOException("history size");out.write(block,0,count);}
                rows.add(new JSONObject(out.toString("UTF-8")));
            }catch(Exception e){report(e);}
        }
        rows.sort((a,b)->Long.compare(b.optLong("time"),a.optLong("time")));return rows;
    }
    private static File[] files(File dir){File[] list=dir.listFiles(f->f.getName().matches("[a-f0-9-]{36}\\.json"));return list==null?new File[0]:list;}
    private static void trim(Context c,File dir){File[] list=files(dir);Arrays.sort(list,(a,b)->Long.compare(b.lastModified(),a.lastModified()));long bytes=0;for(int i=0;i<list.length;i++){bytes+=list[i].length();if(i>=WacchoiStorageLimits.bound(c,0)||bytes>WacchoiStorageLimits.scaled(c,1,1024L*1024))new AtomicFile(list[i]).delete();}}
    interface Selected{void open(JSONObject data);}
    static String recordText(JSONObject r){return r.optString("a")+"\n"+r.optString("b")+"\n"+r.optString("c")+"\n"+r.optString("d");}
    private static String searchText(JSONObject r,String label){StringBuilder text=new StringBuilder(label).append('\n').append(r.optString("from")).append('\n').append(r.optString("to"));JSONArray records=r.optJSONArray("results");if(records!=null)for(int i=0;i<records.length();i++){JSONObject record=records.optJSONObject(i);if(record!=null)text.append('\n').append(recordText(record));}return text.toString();}
    static void show(Activity a,Selected selected){IO.execute(()->{try{
        List<JSONObject> rows=read(a);String[] labels=new String[rows.size()],texts=new String[rows.size()];
        for(int i=0;i<rows.size();i++){JSONObject r=rows.get(i);labels[i]=(r.optBoolean("upper")?"上4桁 ":"下4桁 ")+r.optString("four")+"　"+(r.optBoolean("edge")?"エッヂ":r.optString("boards"))+"\n"+new SimpleDateFormat("yyyy/MM/dd HH:mm",Locale.JAPAN).format(new Date(r.optLong("time")))+"　"+r.optString("status");texts[i]=searchText(r,labels[i]);}
        a.runOnUiThread(()->{if(a.isFinishing()||a.isDestroyed())return;
            LinearLayout panel=new LinearLayout(a);panel.setOrientation(1);
            EditText input=new EditText(a);input.setSingleLine(true);input.setHint("4桁・板・ID・スレ名・保存本文を検索（空白区切りAND）");panel.addView(input);
            TextView count=new TextView(a);panel.addView(count);ListView list=new ListView(a);panel.addView(list,new LinearLayout.LayoutParams(-1,(int)(320*a.getResources().getDisplayMetrics().density)));
            ArrayList<JSONObject> visible=new ArrayList<>(rows);ArrayList<String> names=new ArrayList<>(Arrays.asList(labels));
            ArrayAdapter<String> adapter=new ArrayAdapter<>(a,android.R.layout.simple_list_item_1,names);list.setAdapter(adapter);count.setText("保存 "+rows.size()+"件");
            AlertDialog dialog=new AlertDialog.Builder(a).setTitle("4桁検索履歴・保存結果内検索").setView(panel).setNegativeButton("閉じる",null).create();
            list.setOnItemClickListener((p,v,i,id)->{JSONObject row=visible.get(i);dialog.dismiss();selected.open(row);});
            input.addTextChangedListener(new TextWatcher(){int revision;
                public void beforeTextChanged(CharSequence s,int start,int n,int after){}
                public void onTextChanged(CharSequence s,int start,int before,int n){}
                public void afterTextChanged(Editable value){final int ticket=++revision;String query=value.toString();IO.execute(()->{
                    ArrayList<JSONObject> found=new ArrayList<>();ArrayList<String> foundNames=new ArrayList<>();
                    for(int i=0;i<rows.size();i++)if(WacchoiHistorySearch.matches(texts[i],query)){found.add(rows.get(i));foundNames.add(labels[i]);}
                    a.runOnUiThread(()->{if(ticket!=revision||!dialog.isShowing()||a.isFinishing()||a.isDestroyed())return;visible.clear();visible.addAll(found);adapter.clear();adapter.addAll(foundNames);adapter.notifyDataSetChanged();count.setText("該当 "+found.size()+" / "+rows.size()+"件");});
                });}
            });dialog.show();});
    }catch(Exception e){report(e);}});}
    static void clear(Context c,boolean pages){IO.execute(()->{synchronized(WacchoiHistory.class){
        if(!pages){generation++;for(File file:files(directory(c)))new AtomicFile(file).delete();}
        else{for(File dir:new File[]{new File(c.getFilesDir(),"hissi-public-pages"),new File(c.getCacheDir(),"hissi-public-pages")}){File[] files=dir.listFiles(f->f.getName().matches("[a-f0-9]{64}\\.html"));if(files!=null)for(File file:files)file.delete();}}
    }});}
    private static void report(Exception e){android.util.Log.w("Haiagaru","Wacchoi search history",e);}
    private WacchoiHistory(){}
}
