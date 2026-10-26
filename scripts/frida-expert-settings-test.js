// Verification package only. Changes the mode flag temporarily; feature values remain untouched.
Java.perform(function(){
 const app=Java.use('android.app.ActivityThread').currentApplication();
 if(String(app.getPackageName())!=='jp.co.airfront.android.a2chMate.haiagaru.resumefix242')throw Error('Verification package only');
 Java.classFactory.loader=app.getClassLoader();
 const A=Java.use('app.morphe.extension.chmate.HaiagaruSettingsActivity'),E=Java.use('app.morphe.extension.chmate.ExpertSettings'),Toggle=Java.use('android.widget.Switch'),Group=Java.use('android.view.ViewGroup'),Spinner=Java.use('android.widget.Spinner');
 const prefs=app.getSharedPreferences('io.github.areteruhiro.chmate.haiagaru.ui-config',0),key='expertSettingsMode',had=prefs.contains(key),old=prefs.getBoolean(key,false);
 let activity,checks=0;
 function check(ok,label){if(!ok)throw Error(label);checks++;console.log('PASS '+label);}
 function state(){return String(prefs.getAll());}
 Java.choose(A.$className,{onMatch(a){if(!a.isFinishing())activity=Java.retain(a);},onComplete(){Java.scheduleOnMainThread(()=>{
  try{
   check(!!activity,'settings Activity available');
   prefs.edit().putBoolean(key,false).commit();const before=state(),root=E.create(activity),mode=Java.cast(root.getChildAt(0),Toggle),options=Java.cast(root.getChildAt(2),Group);
   check(!mode.isChecked()&&options.getVisibility()===8,'expert OFF hides controls');check(state()===before,'opening expert does not alter settings');
   mode.performClick();check(prefs.getBoolean(key,false)&&options.getVisibility()===0,'expert ON saves and displays categories');
   const choice=Java.cast(options.getChildAt(0),Spinner),body=Java.cast(options.getChildAt(1),Group);
   check(choice.getCount()===13,'12 categories plus filters');check(body.getChildCount()===1,'only selected category initialized');
   const n=choice.getCount()-1;choice.setSelection(n);choice.getOnItemSelectedListener().onItemSelected(choice,null,n,choice.getItemIdAtPosition(n));
   check(body.getChildCount()>7,'filter options available');mode.performClick();
   check(!prefs.getBoolean(key,true)&&options.getVisibility()===8,'expert OFF preserves editors but hides them');
   check(state()===before,'mode round-trip leaves all feature settings intact');
   console.log('EXPERT_SETTINGS_COMPLETE '+checks+' checks');
  }catch(e){console.log('FAIL '+e.stack);}
  finally{const edit=prefs.edit();if(had)edit.putBoolean(key,old);else edit.remove(key);edit.commit();console.log('EXPERT_MODE_RESTORED');}
 });}});
});
