package app.morphe.extension.chmate;
import android.app.Activity;
import android.widget.LinearLayout;
import android.widget.Switch;
/** Display-only options, independent of native filter states. */
final class QuickFilterOptions {
    static boolean counts(){return FeatureSwitches.enabled("quickFilterCounts",true);}
    static boolean extras(){return FeatureSwitches.enabled("quickFilterExtras",true);}
    static boolean bottom(){return FeatureSwitches.enabled("bottomQuickFilterPanel",false);}
    static void addSettings(LinearLayout root,Activity activity){
        add(root,activity,"quickFilterCounts","フィルターに件数を表示",true);
        add(root,activity,"quickFilterExtras","フィルターにAA・自分・返信を追加",true);
        add(root,activity,"bottomQuickFilterPanel","下部フィルターパネルを使う（外側タップで閉じる）",false);
    }
    private static void add(LinearLayout root,Activity activity,String key,String label,boolean initial){
        Switch toggle=new Switch(activity);toggle.setText(label);
        android.content.SharedPreferences prefs=activity.getSharedPreferences(WacchoiSearchOptions.PREFS,0);
        toggle.setChecked(prefs.getBoolean(key,initial));root.addView(toggle);
        toggle.setOnCheckedChangeListener((button,on)->{prefs.edit().putBoolean(key,on).apply();ExtraQuickFilters.updateOptions(activity);});
    }
    private QuickFilterOptions(){}
}
