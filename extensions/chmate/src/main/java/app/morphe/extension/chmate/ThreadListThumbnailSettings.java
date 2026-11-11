package app.morphe.extension.chmate;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
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
    /** New exclusion settings start empty, even when the old opt-in list exists. */
    private static synchronized void migrate(Context context){
        SharedPreferences saved=prefs(context);
        if(saved.contains(EXCLUDED))return;
        Set<String> known=new LinkedHashSet<>(saved.getStringSet(KNOWN_5CH,Collections.emptySet()));
        if(saved.contains(BOARDS)){
            Set<String> selected=saved.getStringSet(BOARDS,Collections.emptySet());
            for(String key:selected)if(key.startsWith("5ch:"))known.add(key);
        }
        saved.edit().putStringSet(EXCLUDED,Collections.emptySet()).putStringSet(KNOWN_5CH,known).commit();
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
        size.setContentDescription("スレ一覧画像の大きさ");
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
        Button boards=new Button(activity);boards.setText("画像を表示しない板を選択（検索可）");layout.addView(boards);
        boards.setOnClickListener(v->showTargets(activity));
        TextView help=new TextView(activity);
        help.setText("機能は初期OFF。有効にすると使用中の板の1レス目の画像を表示します。板は初期状態ですべて表示対象です。画像を表示しない板だけチェックして除外できます。画像の大きさは24〜96dpのバーで変更できます。ふたば・あにまんは通常の掲示板設定で登録した板が対象です。5ch・エッヂは閲覧すると自動で対象・除外候補に追加されます。画像取得には通信が発生します。画像がないスレや取得に失敗したスレは通常表示です。");
        layout.addView(help);
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
            new AlertDialog.Builder(activity).setMessage("対象の板がまだありません。ふたば・あにまんは通常の掲示板設定で登録し、5ch・エッヂは板を閲覧すると自動で候補に追加されます。").setPositiveButton("閉じる",null).show();return;
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
        new AlertDialog.Builder(activity).setTitle("画像を表示しない板（チェックで除外）").setView(content)
                .setNegativeButton("キャンセル",null).setPositiveButton("保存",(d,w)->{
                    prefs(activity).edit().putStringSet(EXCLUDED,excluded).apply();ThreadListThumbnail.refresh();
                }).show();
    }

    private ThreadListThumbnailSettings(){}
}
