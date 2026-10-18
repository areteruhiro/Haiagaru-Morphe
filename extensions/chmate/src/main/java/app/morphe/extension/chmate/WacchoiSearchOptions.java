package app.morphe.extension.chmate;

import android.app.*;
import android.content.*;
import android.widget.*;

final class WacchoiSearchOptions {
    static final String PREFS="io.github.areteruhiro.chmate.haiagaru.ui-config";
    static String menuKey(int id){return id==0x7e000078?"wacchoiUpperMenu":"wacchoiLowerMenu";}
    static void addSettings(LinearLayout layout,Activity a){
        Button button=new Button(a);button.setText("ワッチョイ4桁検索：バックグラウンド・表示設定");layout.addView(button);
        button.setOnClickListener(v->{
            LinearLayout panel=new LinearLayout(a);panel.setOrientation(1);int pad=(int)(16*a.getResources().getDisplayMetrics().density);panel.setPadding(pad,pad,pad,pad);
            SharedPreferences prefs=a.getSharedPreferences(PREFS,0),icons=a.getSharedPreferences(ToolbarIcons.PREFS,0);
            toggle(panel,a,prefs,"wacchoiBackgroundSearch","通知を表示してバックグラウンドでも検索する",false);
            toggle(panel,a,prefs,"showZzzSearch","ZZZSearch関連を表示（検索画面を開き直すと反映）",true);
            toggle(panel,a,prefs,"showEdgeSearch","エッヂ関連を表示（検索画面を開き直すと反映）",true);
            toggle(panel,a,prefs,"wacchoiSearchHistory","検索履歴と結果を保存する",true);
            Button limits=new Button(a);limits.setText("検索履歴・キャッシュの保持設定");panel.addView(limits);
            limits.setOnClickListener(w->WacchoiStorageLimits.show(a));
            Button clearHistory=new Button(a);clearHistory.setText("検索履歴・保存結果を削除");panel.addView(clearHistory);
            clearHistory.setOnClickListener(w->new AlertDialog.Builder(a).setTitle("検索履歴を削除")
                    .setMessage("保存済みの検索条件と結果を削除します。検索用ページキャッシュは残します。")
                    .setNegativeButton("キャンセル",null).setPositiveButton("削除",(d,i)->WacchoiHistory.clear(a,false)).show());
            Button clearCache=new Button(a);clearCache.setText("検索用ページキャッシュを削除");panel.addView(clearCache);
            clearCache.setOnClickListener(w->new AlertDialog.Builder(a).setTitle("キャッシュを削除")
                    .setMessage("検索用の取得ページを削除します。検索履歴・保存結果は残します。実行中の取得によって再作成される場合があります。")
                    .setNegativeButton("キャンセル",null).setPositiveButton("削除",(d,i)->WacchoiHistory.clear(a,true)).show());
            menuToggle(panel,a,prefs,0x7e000077,"ワッチョイ長押しに下4桁検索を表示");
            menuToggle(panel,a,prefs,0x7e000078,"ワッチョイ長押しに上4桁検索を表示");
            toggle(panel,a,icons,"visible.4","ツールバーの下4桁検索を表示（再起動後に反映）",false);
            toggle(panel,a,icons,"visible.5","ツールバーの上4桁検索を表示（再起動後に反映）",false);
            TextView note=new TextView(a);note.setText("ここで切り替えた表示設定は、他の画面の表示チェックより優先します（親の検索機能がオンの場合）。ツールバーの配置順はChMateのツールバー設定から変更できます。バックグラウンド検索は通知から停止・再表示できます。OSの省電力制御で遅れる場合があり、強制停止・プロセス終了後には自動再開しません。");panel.addView(note);
            ScrollView scroll=new ScrollView(a);scroll.addView(panel);
            new AlertDialog.Builder(a).setTitle("ワッチョイ4桁検索").setView(scroll).setPositiveButton("閉じる",null).show();
        });
    }
    private static void toggle(LinearLayout panel,Activity a,SharedPreferences prefs,String key,String label,boolean initial){
        Switch view=new Switch(a);view.setText(label);view.setChecked(prefs.getBoolean(key,initial));
        view.setOnCheckedChangeListener((v,on)->{prefs.edit().putBoolean(key,on).apply();
            if(prefs==a.getSharedPreferences(ToolbarIcons.PREFS,0))SettingsRestart.confirm(a);
            else SettingsEffects.changed(a,key,on);
        });panel.addView(view);
    }
    private static void menuToggle(LinearLayout panel,Activity a,SharedPreferences prefs,int id,String label){
        Switch view=new Switch(a);view.setText(label);view.setChecked(ResponseMenuSettings.visible(id));panel.addView(view);
        view.setOnCheckedChangeListener((v,on)->{
            prefs.edit().putBoolean(menuKey(id),on).apply();
            // Last edited display control wins; do not retain a contradictory override.
            a.getSharedPreferences("haiagaru_response_actions",0).edit().putInt("visibility."+id,on?1:2).apply();
            if(on&&!FeatureSwitches.enabled("wacchoiSearchEnabled",true))Toast.makeText(a,"先に「追加機能のオン／オフ一覧」でワッチョイ上／下4桁検索をオンにしてください",Toast.LENGTH_LONG).show();
        });
    }
}
