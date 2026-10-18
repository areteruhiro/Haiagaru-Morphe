// Tests the real settings dialog; restores the one changed preference afterwards.
Java.perform(function(){
 const app=Java.use('android.app.ActivityThread').currentApplication();
 if(!String(app.getPackageName()).endsWith('haiagaru.responsearchive242'))throw Error('Test app only');
 const H=Java.use('app.morphe.extension.chmate.Haiagaru'),D=Java.use('android.app.AlertDialog');
 const V=Java.use('android.view.ViewGroup'),T=Java.use('android.widget.TextView'),E=Java.use('android.widget.EditText'),S=Java.use('android.widget.Switch');
 const prefs=app.getSharedPreferences('io.github.areteruhiro.chmate.haiagaru.ui-config',0);
 const had=prefs.contains('directBodySelection'),old=prefs.getBoolean('directBodySelection',false);
 let current=null,checks=0;
 function check(ok,label){if(!ok)throw Error(label);checks++;console.log('PASS '+label);}
 function walk(v,p){if(p(v))return v;if(V.class.isInstance(v)){const g=Java.cast(v,V);for(let i=0;i<g.getChildCount();i++){const r=walk(g.getChildAt(i),p);if(r)return r;}}return null;}
 function text(v){return T.class.isInstance(v)?String(Java.cast(v,T).getText()):'';}
 function later(f){setTimeout(()=>Java.perform(()=>Java.scheduleOnMainThread(()=>{try{f();}catch(e){console.log('FAIL '+e.stack);cleanup();}})),1000);}
 function cleanup(){const edit=prefs.edit();if(had)edit.putBoolean('directBodySelection',old);else edit.remove('directBodySelection');edit.apply();if(current)current.dismiss();}
 let found=false;
 Java.choose('android.app.Activity',{onMatch(a){if(found||a.isFinishing()||!a.getWindow().getDecorView().isShown())return;found=true;
 const activity=Java.retain(a);Java.scheduleOnMainThread(()=>{H.showSettingsDialog(activity);});
 later(()=>{Java.choose('android.app.AlertDialog',{onMatch(d){if(!d.isShowing())return;current=Java.retain(d);},onComplete(){
  if(!current)throw Error('No settings dialog');
  const root=current.getWindow().getDecorView();
  const positive=current.getButton(-1);
  check(positive===null||!positive.isShown(),'no save button');
  const card=walk(root,v=>text(v).startsWith('画面・文字の表示  ›'));
  check(card!==null,'renamed display category');card.performClick();
  later(()=>{
   const toggle=walk(root,v=>S.class.isInstance(v)&&text(v).startsWith('レス本文を直接選択'));
   check(toggle!==null,'category controls created on demand');Java.cast(toggle,S).setChecked(!old);
   check(prefs.getBoolean('directBodySelection',old)===!old,'real toggle saved without confirmation');
   const back=walk(root,v=>text(v)==='‹ 戻る');back.performClick();
   check(walk(root,v=>v.isShown()&&text(v).startsWith('画像・動画を見る  ›'))!==null,'back restores category index');
   const display=walk(root,v=>text(v).startsWith('画面・文字の表示  ›'));display.performClick();
   check(Java.cast(walk(root,v=>S.class.isInstance(v)&&text(v).startsWith('レス本文を直接選択')),S).isChecked()===!old,'saved value retained after navigation');
   console.log('COMPLETE '+checks+' real settings UI checks');cleanup();
  });
 }});});
 },onComplete(){if(!found)console.log('FAIL no activity');}});
});
