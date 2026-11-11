package app.morphe.extension.chmate;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.widget.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;

/** Native search form. Reads only the already-public log; never expires its records. */
final class ReporterSearchUi {
    private static final ExecutorService WORKER = Executors.newSingleThreadExecutor();
    static void show(Activity activity, String initialId) {
        LinearLayout content = new LinearLayout(activity); content.setOrientation(LinearLayout.VERTICAL);
        int pad = (int)(16 * activity.getResources().getDisplayMetrics().density); content.setPadding(pad,pad,pad,pad);
        Spinner mode = new Spinner(activity);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(activity, android.R.layout.simple_spinner_dropdown_item,
                new String[]{"記者ID全文", "記者ID前半4桁", "記者ID後半4桁", "スレ名（部分一致）"});
        mode.setAdapter(adapter); content.addView(mode);
        EditText query = new EditText(activity); query.setSingleLine(true); query.setHint("記者ID／スレ名");
        if (initialId != null) query.setText(initialId); content.addView(query);
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ROOT);
        format.setTimeZone(TimeZone.getTimeZone("Asia/Tokyo")); format.setLenient(false);
        TextView help = new TextView(activity);
        help.setText("日本時間。ID検索は基準日時を含む更新周期内のみ検索します（前半: 木曜9時、後半: 毎日0時）。全文は両期間の共通部分です。スレ名検索は開始以上・終了未満。部分IDの一致だけでは同一人物とは限りません。"); content.addView(help);
        EditText start = new EditText(activity); start.setSingleLine(true); start.setHint("基準日時／スレ名の開始日時 yyyy-MM-dd HH:mm");
        start.setText(format.format(new Date())); content.addView(start);
        EditText end = new EditText(activity); end.setSingleLine(true); end.setHint("スレ名検索の終了日時 yyyy-MM-dd HH:mm");
        end.setText(format.format(new Date(System.currentTimeMillis()+3600000))); content.addView(end);
        Button search = new Button(activity); search.setText("検索"); content.addView(search);
        TextView status = new TextView(activity); content.addView(status);
        LinearLayout results = new LinearLayout(activity); results.setOrientation(LinearLayout.VERTICAL); content.addView(results);
        ScrollView scroll = new ScrollView(activity); scroll.addView(content);
        HaiagaruDialog dialog = new HaiagaruDialog.Builder(activity).setTitle("エッヂの過去スレ検索").setView(scroll).setNegativeButton("閉じる",null).create();
        final Future<?>[] pending = {null}; final boolean[] closed = {false};
        dialog.setOnDismissListener(d -> { closed[0] = true; if(pending[0]!=null)pending[0].cancel(true); });
        search.setOnClickListener(v -> {
            final int selected = mode.getSelectedItemPosition(); final String text = query.getText().toString();
            final long from, until;
            try {
                long anchor = parse(format,start.getText().toString());
                if(selected==3){from=anchor;until=parse(format,end.getText().toString());}
                else {long[] period=ReporterSearch.period(selected,anchor);from=period[0];until=period[1];}
                if(from>=until)throw new IllegalArgumentException();
            } catch(Exception error){status.setText("日時を yyyy-MM-dd HH:mm で入力してください。終了は開始より後にしてください。");return;}
            search.setEnabled(false); results.removeAllViews();
            status.setText(format.format(new Date(from))+" ～ "+format.format(new Date(until))+" 未満を検索中…");
            pending[0]=WORKER.submit(() -> {
                ReporterSearch.Result found=null; String failure=null;
                HttpURLConnection connection=null;
                try {
                    connection=(HttpURLConnection)new URL(ReporterSearch.SOURCE).openConnection();
                    connection.setConnectTimeout(15000);connection.setReadTimeout(30000);
                    if(connection.getResponseCode()!=200)throw new IOException("HTTP "+connection.getResponseCode());
                    try(Reader reader=new InputStreamReader(connection.getInputStream(),StandardCharsets.UTF_8)){
                        found=ReporterSearch.search(reader,selected,text,from,until);
                    }
                }catch(Exception error){failure=error.getMessage();}finally{if(connection!=null)connection.disconnect();}
                final ReporterSearch.Result answer=found;final String error=failure;
                activity.runOnUiThread(() -> {
                    if(closed[0]||activity.isFinishing())return;
                    search.setEnabled(true);
                    if(answer==null){status.setText("検索できませんでした: "+error);return;}
                    status.setText(format.format(new Date(from))+" ～ "+format.format(new Date(until))+" 未満\n"+answer.matches+"件（表示最大200件）。収集済みログのみが対象です。");
                    for(ReporterSearch.Row row:answer.rows){
                        Button item=new Button(activity);item.setAllCaps(false);
                        item.setText(row.title+"\n["+row.id+"★] "+format.format(new Date(row.created)));
                        item.setOnClickListener(w -> {
                            try{activity.startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(row.url)).setPackage(activity.getPackageName()));}
                            catch(Exception unavailable){Toast.makeText(activity,"スレを開けませんでした",Toast.LENGTH_LONG).show();}
                        });results.addView(item);
                    }
                });
            });
        });
        dialog.show();
    }
    private static long parse(SimpleDateFormat format,String value){
        java.text.ParsePosition position=new java.text.ParsePosition(0);Date date=format.parse(value,position);
        if(date==null||position.getIndex()!=value.length())throw new IllegalArgumentException();return date.getTime();
    }
    private ReporterSearchUi(){}
}
