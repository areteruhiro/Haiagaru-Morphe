package app.morphe.extension.chmate;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.text.InputType;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Independent, initially disabled thumbnail experiment with board exclusions. */
final class ThreadListThumbnailSettings {
    static final String PREFS="haiagaru.thread-list-thumbnail",ENABLE="enabled",BOARDS="boards",SIZE="size_dp";
    static final String EXCLUDED="excluded_boards_v2",KNOWN_5CH="known_5ch_boards";
    static final int MIN_SIZE_DP=24,MAX_SIZE_DP=96,STEP_SIZE_DP=4,DEFAULT_SIZE_DP=44;
    static SharedPreferences prefs(Context context){return context.getSharedPreferences(PREFS,0);}
    static boolean enabled(Context context){return prefs(context).getBoolean(ENABLE,false);}
    static boolean selected(Context context,String board){
        if(!enabled(context))return false;
        migrate(context);
        return !prefs(context).getStringSet(EXCLUDED,Collections.emptySet()).contains(board);
    }
    /** Convert former opt-in site boards to exclusions once, without losing their selections. */
    private static synchronized void migrate(Context context){
        SharedPreferences saved=prefs(context);
        if(saved.contains(EXCLUDED))return;
        Set<String> excluded=new LinkedHashSet<>(),known=new LinkedHashSet<>();
        if(saved.contains(BOARDS)){
            Set<String> selected=saved.getStringSet(BOARDS,Collections.emptySet());
            for(String key:SiteBoardGateway.get(context).boards.keySet())
                if(!selected.contains("site:"+key))excluded.add("site:"+key);
            for(String key:selected)if(key.startsWith("5ch:"))known.add(key);
        }
        saved.edit().putStringSet(EXCLUDED,excluded).putStringSet(KNOWN_5CH,known).commit();
    }
    static synchronized void rememberFiveCh(Context context,ThreadListThumbnailRules.Board board){
        migrate(context);
        SharedPreferences saved=prefs(context);
        Set<String> known=new LinkedHashSet<>(saved.getStringSet(KNOWN_5CH,Collections.emptySet()));
        Set<String> excluded=new LinkedHashSet<>(saved.getStringSet(EXCLUDED,Collections.emptySet()));
        String key=board.settingsKey(),legacy=board.legacySettingsKey();
        boolean changed=known.remove(legacy);
        // The old key lacked a host; preserve its exclusion for the first
        // observed matching board, then use independent host/board keys.
        if(excluded.remove(legacy)){excluded.add(key);changed=true;}
        changed=known.add(key)||changed;
        if(changed)saved.edit().putStringSet(KNOWN_5CH,known).putStringSet(EXCLUDED,excluded).apply();
    }
    static int sizeDp(Context context){
        int stored=prefs(context).getInt(SIZE,DEFAULT_SIZE_DP);
        return Math.max(MIN_SIZE_DP,Math.min(MAX_SIZE_DP,stored));
    }

    static void addSettings(LinearLayout layout,Activity activity){
        migrate(activity);
        Switch enabled=new Switch(activity);
        enabled.setText("スレ一覧画像表示（実験）");
        enabled.setChecked(enabled(activity));
        layout.addView(enabled,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));
        enabled.setOnCheckedChangeListener((v,checked)->{prefs(activity).edit().putBoolean(ENABLE,checked).apply();ThreadListThumbnail.refresh();});
        TextView sizeLabel=new TextView(activity);
        sizeLabel.setText("画像の大きさ："+sizeDp(activity)+"dp");
        layout.addView(sizeLabel);
        SeekBar size=new SeekBar(activity);
        size.setMax((MAX_SIZE_DP-MIN_SIZE_DP)/STEP_SIZE_DP);
        size.setProgress((sizeDp(activity)-MIN_SIZE_DP)/STEP_SIZE_DP);
        layout.addView(size,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));
        size.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            @Override public void onProgressChanged(SeekBar bar,int progress,boolean fromUser){
                sizeLabel.setText("画像の大きさ："+(MIN_SIZE_DP+progress*STEP_SIZE_DP)+"dp");
            }
            @Override public void onStartTrackingTouch(SeekBar bar){}
            @Override public void onStopTrackingTouch(SeekBar bar){
                int chosen=MIN_SIZE_DP+bar.getProgress()*STEP_SIZE_DP;
                prefs(activity).edit().putInt(SIZE,chosen).apply();
                ThreadListThumbnail.refresh();
            }
        });
        LinearLayout controls=new LinearLayout(activity);controls.setOrientation(LinearLayout.HORIZONTAL);
        Button minus=new Button(activity);minus.setText("－");controls.addView(minus,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1));
        Button plus=new Button(activity);plus.setText("＋");controls.addView(plus,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1));
        layout.addView(controls);
        minus.setOnClickListener(v->changeSize(activity,size,-1));plus.setOnClickListener(v->changeSize(activity,size,1));
        Button boards=new Button(activity);boards.setText("画像を追加しない板を選択（検索可）");layout.addView(boards);
        boards.setOnClickListener(v->showTargets(activity));
        Button five=new Button(activity);five.setText("5ch・エッヂの板をURLで登録");layout.addView(five);
        five.setOnClickListener(v->addFiveCh(activity));
        Button futaba=new Button(activity);futaba.setText("ふたば二次元裏（img / may）を登録");layout.addView(futaba);
        futaba.setOnClickListener(v->new AlertDialog.Builder(activity).setTitle("二次元裏の板を登録")
                .setItems(new String[]{"二次元裏（img）","二次元裏（may）"},(d,choice)->{
                    String address=choice==0?"https://img.2chan.net/b/":"https://may.2chan.net/b/";
                    try{SiteBoardGateway.get(activity).register(address,choice==0?"二次元裏（img）":"二次元裏（may）");
                        Toast.makeText(activity,"板を登録しました。『使用する』の設定をご確認ください",Toast.LENGTH_LONG).show();}
                    catch(java.io.IOException error){new AlertDialog.Builder(activity).setMessage(error.getMessage()).setPositiveButton("閉じる",null).show();}
                }).setNegativeButton("キャンセル",null).show());
        TextView help=new TextView(activity);
        help.setText("初期OFF。有効にすると使用中の板の1レス目の画像を表示します。不要な板は除外できます。画像の大きさは24〜96dp、バーと±ボタンで変更できます。ふたば・あにまんは登録済みの板に対応し、5ch・エッヂは閲覧した板も除外候補になります。画像取得には通信が発生します。画像がないスレや取得に失敗したスレは通常表示です。");
        layout.addView(help);
    }

    private static void changeSize(Activity activity,SeekBar bar,int delta){
        int progress=Math.max(0,Math.min(bar.getMax(),bar.getProgress()+delta));
        bar.setProgress(progress);
        prefs(activity).edit().putInt(SIZE,MIN_SIZE_DP+progress*STEP_SIZE_DP).apply();
        ThreadListThumbnail.refresh();
    }

    private static void showTargets(Activity activity){
        migrate(activity);
        Set<String> excluded=new LinkedHashSet<>(prefs(activity).getStringSet(EXCLUDED,Collections.emptySet()));
        List<String> keys=new ArrayList<>(),labels=new ArrayList<>();
        SiteBoardGateway gateway=SiteBoardGateway.get(activity);
        List<String> registered=new ArrayList<>(gateway.boards.keySet());Collections.sort(registered);
        for(String key:registered){
            SiteBoardProtocol.Board board=gateway.boards.get(key);
            if(board==null||!(board.kind.equals("futaba")||board.kind.equals("animanch")))continue;
            keys.add("site:"+key);labels.add((board.kind.equals("futaba")?"ふたば":"あにまん")+"："+gateway.names.get(key)+"（"+board.url+"）");
        }
        List<String> five=new ArrayList<>(prefs(activity).getStringSet(KNOWN_5CH,Collections.emptySet()));
        Collections.sort(five);
        for(String value:five){keys.add(value);labels.add("5ch・エッヂ："+value.substring(4));}
        if(keys.isEmpty()){
            new AlertDialog.Builder(activity).setMessage("登録済みの板がありません。ふたば・あにまんの板を登録するか、5ch・エッヂの板を開いてください。").setPositiveButton("閉じる",null).show();return;
        }
        LinearLayout content=new LinearLayout(activity);content.setOrientation(LinearLayout.VERTICAL);
        EditText query=new EditText(activity);query.setSingleLine(true);query.setHint("板名・URLで検索");content.addView(query);
        ScrollView scroll=new ScrollView(activity);LinearLayout rows=new LinearLayout(activity);rows.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(rows);content.addView(scroll,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,(int)(350*activity.getResources().getDisplayMetrics().density)));
        Runnable render=()->{
            rows.removeAllViews();String filter=query.getText().toString().toLowerCase(Locale.ROOT);
            for(int i=0;i<keys.size();i++){
                String key=keys.get(i),label=labels.get(i);
                if(!label.toLowerCase(Locale.ROOT).contains(filter))continue;
                CheckBox check=new CheckBox(activity);check.setText(label);check.setChecked(excluded.contains(key));
                check.setOnCheckedChangeListener((v,value)->{if(value)excluded.add(key);else excluded.remove(key);});rows.addView(check);
            }
        };
        query.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int start,int count,int after){}
            public void onTextChanged(CharSequence s,int start,int before,int count){render.run();}
            public void afterTextChanged(Editable text){}
        });render.run();
        new AlertDialog.Builder(activity).setTitle("画像を追加しない板（チェックで除外）").setView(content)
                .setNegativeButton("キャンセル",null).setPositiveButton("保存",(d,w)->{
                    prefs(activity).edit().putStringSet(EXCLUDED,excluded).apply();ThreadListThumbnail.refresh();
                }).show();
    }

    private static void addFiveCh(Activity activity){
        EditText url=new EditText(activity);url.setSingleLine(true);url.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_URI);
        url.setHint("https://bbs.eddibb.cc/liveedge/");
        AlertDialog dialog=new AlertDialog.Builder(activity).setTitle("5ch・エッヂの板を登録").setMessage("板URLまたはスレURLを入力してください。閲覧した板も自動的に除外候補へ追加されます。")
                .setView(url).setNegativeButton("キャンセル",null).setPositiveButton("追加",null).create();
        dialog.setOnShowListener(v->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(button->{
            ThreadListThumbnailRules.Board board=ThreadListThumbnailRules.board(url.getText().toString());
            if(board==null){url.setError("対応する5ch・エッヂの板URLを指定してください");return;}
            String key=board.settingsKey();rememberFiveCh(activity,board);
            Set<String> excluded=new LinkedHashSet<>(prefs(activity).getStringSet(EXCLUDED,Collections.emptySet()));
            excluded.remove(key);prefs(activity).edit().putStringSet(EXCLUDED,excluded).apply();ThreadListThumbnail.refresh();
            Toast.makeText(activity,"画像表示の対象に追加しました："+board.name,Toast.LENGTH_SHORT).show();dialog.dismiss();
        }));dialog.show();
    }
    private ThreadListThumbnailSettings(){}
}
