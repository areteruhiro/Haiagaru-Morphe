let original,entry,host,checks=0;
function main(fn){return new Promise((resolve,reject)=>Java.perform(()=>Java.scheduleOnMainThread(()=>{try{resolve(fn());}catch(e){reject(Error(String(e.stack||e)));}})));}
function check(ok,label){if(!ok)throw Error(label);checks++;}
rpc.exports={test(){return main(()=>{
 const app=Java.use('android.app.ActivityThread').currentApplication();if(!/^jp\.co\.airfront\.android\.a2chMate\.haiagaru\.bindings(191|226|242)$/.test(String(app.getPackageName())))throw Error('isolated bindings required');
 host=Java.retain(Java.use('app.morphe.extension.chmate.Haiagaru').toolbarActivity(null));check(String(host.getClass().getName())==='jp.syoboi.a2chMate.activity.SettingActivity','native settings foreground');
 const root=Java.cast(host.findViewById(16908290),Java.use('android.widget.FrameLayout'));entry=root.findViewWithTag('haiagaru.settings.inline');check(entry!==null,'inline entry exists');entry=Java.retain(Java.cast(entry,Java.use('android.widget.LinearLayout')));
 original=Java.retain(root.getChildAt(0));check(!original.equals(entry),'native content retained');const params=Java.cast(entry.getLayoutParams(),Java.use('android.widget.FrameLayout$LayoutParams'));check((params.gravity.value&112)===48,'entry at top');
 check(Java.cast(entry.getParent(),Java.use('android.view.View')).equals(root),'same native window');check(String(Java.cast(entry.getChildAt(0),Java.use('android.widget.TextView')).getText())==='Haiagaru設定','entry label');
 check(entry.getBottom()<=original.getTop(),'entry does not cover native settings');check(entry.isShown()&&entry.isClickable()&&entry.isFocusable(),'entry accessible');
 const children=root.getChildCount(),margin=Java.cast(original.getLayoutParams(),Java.use('android.widget.FrameLayout$LayoutParams')).topMargin.value;
 const H=Java.use('app.morphe.extension.chmate.Haiagaru');H.onSettingsResume(host);H.onSettingsResume(host);
 check(root.getChildCount()===children,'resume does not duplicate entry');check(root.getChildAt(0).equals(original),'native/Compose view not detached');
 check(Java.cast(original.getLayoutParams(),Java.use('android.widget.FrameLayout$LayoutParams')).topMargin.value===margin,'resume does not increase reserved space');
 check(entry.performClick(),'entry click handled');return {checks,clicked:true,version:String(app.getPackageName()).slice(-3)};
 });},opened(){return main(()=>{const a=Java.use('app.morphe.extension.chmate.Haiagaru').toolbarActivity(null);check(String(a.getClass().getName()).endsWith('HaiagaruSettingsActivity'),'Haiagaru settings opened');return {checks,activity:String(a.getClass().getName())};});},returned(){return main(()=>{const a=Java.use('app.morphe.extension.chmate.Haiagaru').toolbarActivity(null);if(a===null||!String(a.getClass().getName()).endsWith('.SettingActivity'))return {ready:false};check(true,'back returns to native settings');check(Java.cast(a.findViewById(16908290),Java.use('android.widget.FrameLayout')).findViewWithTag('haiagaru.settings.inline')!==null,'entry retained after back');return {ready:true,checks};});}};
