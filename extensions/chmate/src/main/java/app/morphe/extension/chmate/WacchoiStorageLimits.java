package app.morphe.extension.chmate;

import android.app.*;
import android.content.*;
import android.text.InputType;
import android.widget.*;

/** Zero means unlimited; transport response-size safety remains independent. */
final class WacchoiStorageLimits {
    static final String[] KEYS={"historyCount","historyMiB","historyFileMiB","historyRecords","historyCharacters","cacheMiB","cacheHours"};
    static final long[] DEFAULTS={20,32,2,2000,250000,16,24};
    static final String[] LABELS={"履歴の保持件数","履歴の合計容量（MiB）","履歴1件の容量（MiB）","履歴1件の結果数","履歴1件の本文等の合計文字数","ページキャッシュ容量（MiB）","ページキャッシュ再利用期間（時間）"};
    static long value(Context c,int index){
        return Math.max(0,c.getSharedPreferences(WacchoiSearchOptions.PREFS,0).getLong("wacchoiLimit."+KEYS[index],DEFAULTS[index]));
    }
    static long bound(Context c,int index){long n=value(c,index);return n==0?Long.MAX_VALUE:n;}
    static long scaled(Context c,int index,long unit){long n=bound(c,index);return n>Long.MAX_VALUE/unit?Long.MAX_VALUE:n*unit;}
    static boolean fresh(Context c,java.io.File file){return file.isFile()&&System.currentTimeMillis()-file.lastModified()<scaled(c,6,3600000L);}
    static void show(Activity a){
        LinearLayout panel=new LinearLayout(a);panel.setOrientation(1);
        TextView note=new TextView(a);note.setText("0は無制限（再利用期間は無期限）。無制限や大きい値は容量・メモリ使用量を増やします。上限を下げると次の保存時に古いデータから整理します。削除済み・省略済みの結果は戻りません。通信応答1件の安全上限2MiBは維持します。");panel.addView(note);
        EditText[] inputs=new EditText[KEYS.length];
        for(int i=0;i<inputs.length;i++){TextView label=new TextView(a);label.setText(LABELS[i]);panel.addView(label);EditText input=new EditText(a);input.setSingleLine(true);input.setInputType(InputType.TYPE_CLASS_NUMBER);input.setText(Long.toString(value(a,i)));panel.addView(input);inputs[i]=input;}
        ScrollView scroll=new ScrollView(a);scroll.addView(panel);
        AlertDialog dialog=new AlertDialog.Builder(a).setTitle("検索履歴・キャッシュの保持設定").setView(scroll).setNegativeButton("キャンセル",null).setPositiveButton("保存",null).create();
        dialog.setOnShowListener(v->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(w->{
            long[] values=new long[inputs.length];
            for(int i=0;i<values.length;i++)try{values[i]=Long.parseLong(inputs[i].getText().toString().trim());if(values[i]<0)throw new NumberFormatException();}catch(NumberFormatException e){inputs[i].setError("0以上の整数を入力してください");return;}
            SharedPreferences.Editor edit=a.getSharedPreferences(WacchoiSearchOptions.PREFS,0).edit();for(int i=0;i<values.length;i++)edit.putLong("wacchoiLimit."+KEYS[i],values[i]);edit.apply();dialog.dismiss();
        }));dialog.show();
    }
    private WacchoiStorageLimits(){}
}
