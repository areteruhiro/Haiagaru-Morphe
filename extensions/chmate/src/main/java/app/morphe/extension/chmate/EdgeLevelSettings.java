package app.morphe.extension.chmate;
import android.app.Activity;
import android.content.SharedPreferences;
import android.widget.*;
import java.util.*;

final class EdgeLevelSettings {
    static void add(LinearLayout parent,Activity a){Button b=new Button(a);b.setText("エッヂの認証日・共有レベルによる自動NG（開発中）");parent.addView(b);b.setOnClickListener(v->show(a));}
    static void show(Activity a){LinearLayout box=new LinearLayout(a);box.setOrientation(1);int p=(int)(16*a.getResources().getDisplayMetrics().density);box.setPadding(p,p,p,p);SharedPreferences prefs=EdgeLevelNg.config();
        TextView help=new TextView(a);help.setText("エッヂ限定。公開スレで確認したレベルと認証日からの推定を分けて使用します。IDは掲示板・投稿日単位で照合し、不明なIDはNGにしません。通常NG登録・DATは変更しません。変更後はスレを開き直してください。");box.addView(help);
        toggle(box,a,"確認済みレベルが指定値以下のIDを非表示",EdgeLevelNg.OBSERVED,false);
        number(box,a,"確認済みレベルの閾値（この値以下をNG）","edgeLevelThreshold",5,0,9999);
        toggle(box,a,"認証日からの推定レベルで非表示（自己申告情報）",EdgeLevelNg.ESTIMATE,false);
        TextView estimate=new TextView(a);estimate.setText("認証日が共有され、確認済みレベルがないIDだけが推定の対象です。日付からの計算は実レベルを保証しません。参考公開ソースの23時間を初期値にしていますが、現在の稼働仕様は未確認です。必要に応じて以下の計算値を変更してください。共有された認証日は自己申告で、本人の認証日であることをサーバーは証明できません。推定NGは初期OFFです。");box.addView(estimate);
        number(box,a,"推定レベルの閾値（この値以下をNG）","edgeLevelEstimateThreshold",5,0,9999);
        number(box,a,"認証当日の推定レベル","edgeLevelInitial",1,0,9999);
        number(box,a,"1レベル増やす経過時間（時間）","edgeLevelHours",23,1,8760);
        toggle(box,a,"認証成功を確認した日時をこの端末に記録",EdgeLevelNg.RECORD,false);
        TextView auth=new TextView(a);auth.setText("Haiagaru内のエッヂ認証成功ページを確認できた時だけ記録します。既存Cookieを初めて見た日を認証日にしません。認証が別ブラウザで完了した場合や、記録前のCookieでは認証日は不明です。");box.addView(auth);
        Switch share=new Switch(a);share.setText("同意した利用者間で自分の認証日を共有");share.setChecked(EdgeLevelNg.option(EdgeLevelNg.CONSENT));box.addView(share);
        share.setOnCheckedChangeListener((v,on)->{if(!on){EdgeLevelNg.revoke();return;}
            if(EdgeLevelNg.option(EdgeLevelNg.CONSENT))return;share.setChecked(false);
            new HaiagaruDialog.Builder(a).setTitle("エッヂの認証日を共有しますか？")
              .setMessage("共有する項目：自分の公開ID、投稿日、認証を確認した日、投稿先スレURLとレス番号。日付以外の認証時刻、Cookie、JWT、認証コード、IPアドレスは送信しません。\n\nChMateが自分の投稿として記録したレスだけを自動送信し、GASで公開投稿との一致を確認します。認証日自体は自己申告として扱います。同意した全利用者へ配信され、任意の推定NGに使われます。認証日時の端末内記録もONにします。通常の個人NG送信同意とは別です。共有をOFFにすると新しい送信を停止し、接続先の変更時は再同意が必要です。")
              .setNegativeButton("同意しない",null).setPositiveButton("同意して共有",(d,w)->{EdgeLevelNg.local().edit().putBoolean("consentV1",true).putString("consentEndpoint",EdgeLevelNg.endpoint()).apply();prefs.edit().putBoolean(EdgeLevelNg.RECORD,true).apply();share.setChecked(true);EdgeLevelNg.schedule(false);}).show();
        });
        toggle(box,a,"レベル情報を自動同期", "edgeLevelAutoSync",true);
        number(box,a,"同期間隔（分・15〜1440）","edgeLevelIntervalMinutes",30,15,1440);
        TextView source=new TextView(a);source.setText("現在のクロール対象：\n"+EdgeLevelPolicy.SOURCE+"\nGAS側で取得をまとめます。各端末が同じスレを巡回する方式ではありません。");source.setTextIsSelectable(true);box.addView(source);
        Button exclusions=new Button(a);exclusions.setText("自動NGから除外するID");box.addView(exclusions);exclusions.setOnClickListener(v->{EditText input=new EditText(a);input.setMinLines(4);input.setText(android.text.TextUtils.join("\n",prefs.getStringSet("edgeLevelExceptions",Collections.emptySet())));new HaiagaruDialog.Builder(a).setTitle("エッヂレベルNGの除外ID（完全一致）").setView(input).setNegativeButton("戻る",null).setPositiveButton("保存",(d,w)->{Set<String> values=new HashSet<>();for(String line:input.getText().toString().split("\\s+")){String id=EdgeLevelPolicy.id(line);if(id!=null&&values.size()<500)values.add(id);}prefs.edit().putStringSet("edgeLevelExceptions",values).apply();}).show();});
        TextView state=new TextView(a);state.setText(EdgeLevelNg.status);box.addView(state);Button sync=new Button(a);sync.setText("レベル情報を今すぐ同期");box.addView(sync);sync.setOnClickListener(v->{EdgeLevelNg.schedule(true);state.setText("同期を開始しました。画面を開き直すと結果を表示します。");});
        ScrollView scroll=new ScrollView(a);scroll.addView(box);new HaiagaruDialog.Builder(a).setTitle("エッヂのレベル自動NG（開発中）").setView(scroll).setPositiveButton("閉じる",null).show();
    }
    private static void toggle(LinearLayout box,Activity a,String title,String key,boolean initial){Switch s=new Switch(a);s.setText(title);s.setChecked(EdgeLevelNg.config().getBoolean(key,initial));box.addView(s);s.setOnCheckedChangeListener((v,on)->{EdgeLevelNg.config().edit().putBoolean(key,on).apply();if(on)EdgeLevelNg.schedule(false);});}
    private static void number(LinearLayout box,Activity a,String title,String key,int initial,int min,int max){TextView label=new TextView(a);label.setText(title);box.addView(label);LinearLayout row=new LinearLayout(a);EditText text=new EditText(a);text.setSingleLine(true);text.setInputType(2);text.setText(Integer.toString(EdgeLevelNg.config().getInt(key,initial)));row.addView(text,new LinearLayout.LayoutParams(0,-2,1));Button save=new Button(a);save.setText("保存");row.addView(save);box.addView(row);save.setOnClickListener(v->{try{int n=Integer.parseInt(text.getText().toString());if(n<min||n>max)throw new IllegalArgumentException();EdgeLevelNg.config().edit().putInt(key,n).apply();}catch(Exception invalid){text.setError(min+"〜"+max+"で入力してください");}});}
    private EdgeLevelSettings(){}
}
