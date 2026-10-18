package app.morphe.extension.chmate;

import android.app.Activity;
import android.content.SharedPreferences;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ArrayAdapter;
import java.util.concurrent.atomic.AtomicInteger;

/** Dedicated preferences and detached controls; no changes to real user settings. */
public final class VerifyLiveSettingsAndroid {
    public static void runSearch(Activity activity) {
        LinearLayout root=new LinearLayout(activity);root.setOrientation(1);
        SettingsSections nav=new SettingsSections(root,false);
        nav.defer(SettingsSections.Category.DISPLAY,"表示",()->{
            LinearLayout page=new LinearLayout(activity);
            LiveSettings.Toggle toggle=new LiveSettings.Toggle(activity);toggle.setText("検索テストの行間");page.addView(toggle);return page;
        });
        nav.defer(SettingsSections.Category.NG,"NG",()->{
            LinearLayout page=new LinearLayout(activity);EditText input=new EditText(activity);input.setText("秘密の行間");page.addView(input);return page;
        });
        nav.finish();android.widget.ScrollView scroll=new android.widget.ScrollView(activity);scroll.addView(root);
        android.app.AlertDialog dialog=new android.app.AlertDialog.Builder(activity).setView(scroll).create();dialog.show();
        try {
            java.lang.reflect.Field search=SettingsSections.class.getDeclaredField("search");search.setAccessible(true);
            ((EditText)search.get(nav)).setText("行間");
            root.postDelayed(()->{
                try {
                    java.lang.reflect.Field field=SettingsSections.class.getDeclaredField("results");field.setAccessible(true);
                    LinearLayout results=(LinearLayout)field.get(nav);
                    check(results.getChildCount()==1,"search displays only matching option, never private input");
                    check(((android.widget.TextView)results.getChildAt(0)).getText().toString().contains("検索テストの行間"),"result shows individual option label");
                    results.getChildAt(0).performClick();check(nav.goBack(),"result opens category and back returns to results");
                    check(nav.goBack()&&!nav.goBack(),"back from results clears query before closing");
                    android.util.Log.i("LiveSettingsTest","PASS interactive individual option search");
                }catch(Exception|AssertionError e){android.util.Log.e("LiveSettingsTest","FAIL search",e);}
                finally{dialog.dismiss();}
            },1500);
        }catch(Exception e){dialog.dismiss();throw new AssertionError(e);}
    }
    private static int checks;
    private static void check(boolean result,String name){if(!result)throw new AssertionError(name);checks++;android.util.Log.i("LiveSettingsTest","PASS "+name);}
    public static String run(Activity activity) {
        checks=0;
        SharedPreferences prefs=activity.getSharedPreferences("haiagaru.settings-verification",0);
        prefs.edit().clear().commit();
        try {
            AtomicInteger refreshed=new AtomicInteger(),external=new AtomicInteger();
            LiveSettings live=new LiveSettings(prefs,refreshed::incrementAndGet);
            LiveSettings.Toggle toggle=new LiveSettings.Toggle(activity);toggle.setChecked(true);
            toggle.setOnCheckedChangeListener((v,on)->external.incrementAndGet());
            live.toggle(toggle,"toggle");
            check(!prefs.contains("toggle"),"binding does not save initial toggle");
            toggle.setChecked(false);
            check(prefs.contains("toggle")&&!prefs.getBoolean("toggle",true),"toggle saved synchronously in memory");
            check(external.get()==1&&refreshed.get()==1,"existing toggle behavior preserved");
            toggle.setChecked(true);check(prefs.getBoolean("toggle",false),"toggle can be changed again");
            EditText number=new EditText(activity);number.setText("20");live.number(number,"count",0,100);
            number.setText("30");check(prefs.getInt("count",0)==30,"valid input immediately saved");
            number.setText("");check(prefs.getInt("count",0)==30,"empty editing state preserves stored value");
            number.setText("999");check(prefs.getInt("count",0)==30&&number.getError()!=null,"out of range rejected inline");
            number.setText("0");check(prefs.getInt("count",-1)==0&&number.getError()==null,"valid zero accepted and error cleared");
            LinearLayout root=new LinearLayout(activity);root.setOrientation(1);
            SettingsSections nav=new SettingsSections(root,false);
            AtomicInteger created=new AtomicInteger();
            LinearLayout[] page=new LinearLayout[1];
            nav.defer(SettingsSections.Category.DISPLAY,"行間 フィルタ",()->{created.incrementAndGet();page[0]=new LinearLayout(activity);page[0].addView(number);return page[0];});
            nav.defer(SettingsSections.Category.BACKUP,"バックアップ",()->{created.incrementAndGet();return new LinearLayout(activity);});
            nav.finish();check(created.get()==0,"opening index builds no category controls");
            nav.revealCategory(SettingsSections.Category.DISPLAY);
            check(created.get()==1&&page[0].getParent()==root,"only selected category created and attached");
            number.setText("45");check(prefs.getInt("count",0)==45,"edit remains live inside category");
            check(nav.goBack()&&page[0].getParent()==null,"back returns to index and detaches category");
            nav.revealCategory(SettingsSections.Category.DISPLAY);
            check(created.get()==1&&number.getText().toString().equals("45"),"return retains controls and value without rebuild");
            check(nav.goBack()&&!nav.goBack(),"second back leaves settings index");
            check(SettingsSections.matches("行間 フィルタ","行間"),"search works without building controls");
            LinearLayout searchable=new LinearLayout(activity);
            android.widget.TextView caption=new android.widget.TextView(activity);caption.setText("テスト行間");searchable.addView(caption);
            EditText secret=new EditText(activity);secret.setText("private-value");searchable.addView(secret);
            LiveSettings.Toggle option=new LiveSettings.Toggle(activity);option.setText("画像を自動保存");searchable.addView(option);
            android.widget.TextView description=new android.widget.TextView(activity);description.setText("説明だけの文章");searchable.addView(description);
            try {
                java.lang.reflect.Method index=SettingsSections.class.getDeclaredMethod("collectOptions",android.view.ViewGroup.class,java.util.List.class);
                index.setAccessible(true);java.util.List<Object> entries=new java.util.ArrayList<>();index.invoke(null,searchable,entries);
                check(entries.size()==2,"search indexes individual input labels and switches, not descriptions");
                java.lang.reflect.Field name=entries.get(0).getClass().getDeclaredField("name");name.setAccessible(true);
                java.lang.reflect.Field target=entries.get(0).getClass().getDeclaredField("target");target.setAccessible(true);
                check(name.get(entries.get(0)).equals("テスト行間")&&target.get(entries.get(0))==secret,"search targets input without indexing private value");
            } catch(ReflectiveOperationException e){throw new AssertionError(e);}
            return "PASS "+checks+" live settings and lazy navigation checks";
        } finally {prefs.edit().clear().apply();}
    }
}
