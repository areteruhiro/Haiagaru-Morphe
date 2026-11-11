package app.morphe.extension.chmate;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.text.InputType;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Independent, initially disabled selection for the experimental thumbnail reader. */
final class ThreadListThumbnailSettings {
    static final String PREFS="haiagaru.thread-list-thumbnail",ENABLE="enabled",BOARDS="boards",SIZE="size_dp";
    static final int MIN_SIZE_DP=24,MAX_SIZE_DP=96,STEP_SIZE_DP=4,DEFAULT_SIZE_DP=44;
    static SharedPreferences prefs(Context context){return context.getSharedPreferences(PREFS,0);}
    static boolean enabled(Context context){return prefs(context).getBoolean(ENABLE,false);}
    static boolean selected(Context context,String board){return enabled(context)&&prefs(context).getStringSet(BOARDS,Collections.emptySet()).contains(board);}
    static int sizeDp(Context context){
        int stored=prefs(context).getInt(SIZE,DEFAULT_SIZE_DP);
        return Math.max(MIN_SIZE_DP,Math.min(MAX_SIZE_DP,stored));
    }

    static void addSettings(LinearLayout layout,Activity activity){
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
        Button boards=new Button(activity);boards.setText("画像を表示する対象の板を選択");layout.addView(boards);
        boards.setOnClickListener(v->showTargets(activity));
        Button five=new Button(activity);five.setText("5chの対象板をURLで追加");layout.addView(five);
        five.setOnClickListener(v->addFiveCh(activity));
        TextView help=new TextView(activity);
        help.setText("初期OFF。選択した板だけ、1レス目の画像を表示します。画像の大きさは24〜96dpの範囲で調整でき、一覧を再表示すると反映されます。5chは板URLから追加できます。ふたば・あにまんは登録済みの板から選択してください。取得には通信が発生し、画像のないスレや取得できない画像は通常表示のままです。");
        layout.addView(help);
    }

    private static void showTargets(Activity activity){
        Set<String> current=new LinkedHashSet<>(prefs(activity).getStringSet(BOARDS,Collections.emptySet()));
        List<String> keys=new ArrayList<>(),labels=new ArrayList<>();
        SiteBoardGateway gateway=SiteBoardGateway.get(activity);
        List<String> registered=new ArrayList<>(gateway.boards.keySet());Collections.sort(registered);
        for(String key:registered){
            SiteBoardProtocol.Board board=gateway.boards.get(key);
            if(board==null||!(board.kind.equals("futaba")||board.kind.equals("animanch")))continue;
            keys.add("site:"+key);labels.add((board.kind.equals("futaba")?"ふたば":"あにまん")+"："+gateway.names.get(key));
        }
        List<String> five=new ArrayList<>();for(String value:current)if(value.startsWith("5ch:"))five.add(value);
        Collections.sort(five);
        for(String value:five){keys.add(value);labels.add("5ch："+value.substring(4));}
        if(keys.isEmpty()){
            new AlertDialog.Builder(activity).setMessage("登録済みのふたば・あにまんの板がありません。板を登録するか、5chの板URLを追加してください。").setPositiveButton("閉じる",null).show();return;
        }
        boolean[] choices=new boolean[keys.size()];for(int i=0;i<choices.length;i++)choices[i]=current.contains(keys.get(i));
        new AlertDialog.Builder(activity).setTitle("スレ一覧画像表示の対象板")
                .setMultiChoiceItems(labels.toArray(new String[0]),choices,(d,i,checked)->choices[i]=checked)
                .setNegativeButton("キャンセル",null)
                .setPositiveButton("保存",(d,w)->{
                    for(int i=0;i<choices.length;i++)if(choices[i])current.add(keys.get(i));else current.remove(keys.get(i));
                    prefs(activity).edit().putStringSet(BOARDS,current).apply();ThreadListThumbnail.refresh();
                }).show();
    }

    private static void addFiveCh(Activity activity){
        EditText url=new EditText(activity);url.setSingleLine(true);url.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_URI);
        url.setHint("https://egg.5ch.io/android/");
        AlertDialog dialog=new AlertDialog.Builder(activity).setTitle("5chの板URLを追加").setMessage("板URLまたはスレURLを入力してください。板単位で有効にします。")
                .setView(url).setNegativeButton("キャンセル",null).setPositiveButton("追加",null).create();
        dialog.setOnShowListener(v->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(button->{
            ThreadListThumbnailRules.Board board=ThreadListThumbnailRules.board(url.getText().toString());
            if(board==null){url.setError("5chの板URLを指定してください");return;}
            Set<String> boards=new LinkedHashSet<>(prefs(activity).getStringSet(BOARDS,Collections.emptySet()));
            boards.add("5ch:"+board.name);prefs(activity).edit().putStringSet(BOARDS,boards).apply();ThreadListThumbnail.refresh();
            Toast.makeText(activity,"5ch："+board.name+" を追加しました",Toast.LENGTH_SHORT).show();dialog.dismiss();
        }));dialog.show();
    }
    private ThreadListThumbnailSettings(){}
}
