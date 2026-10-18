Java.perform(function(){
 const app=Java.use('android.app.ActivityThread').currentApplication();
 if(String(app.getPackageName())!=='jp.co.airfront.android.a2chMate.haiagaru.decoderfix191')throw Error('Dedicated verification app only');
 const H=Java.use('app.morphe.extension.chmate.Haiagaru'),O=Java.use('app.morphe.extension.chmate.WacchoiSearchOptions');
 const R=Java.use('app.morphe.extension.chmate.ResponseMenuSettings'),D=Java.use('android.app.AlertDialog');
 const G=Java.use('android.view.ViewGroup'),T=Java.use('android.widget.TextView'),S=Java.use('android.widget.Switch');
 const prefs=app.getSharedPreferences('io.github.areteruhiro.chmate.haiagaru.ui-config',0),actions=app.getSharedPreferences('haiagaru_response_actions',0);
 const keys=['wacchoiLowerMenu','wacchoiUpperMenu','wacchoiSearchEnabled'],ids=[0x7e000077,0x7e000078];
 const icons=app.getSharedPreferences('haiagaru_toolbar_icons',0),iconSaved=[icons.contains('visible.4'),icons.getBoolean('visible.4',false)];
 const saved=keys.map(k=>[k,prefs.contains(k),prefs.getBoolean(k,false)]),modes=ids.map(id=>[id,actions.contains('visibility.'+id),actions.getInt('visibility.'+id,0)]);
 function restore(){let e=prefs.edit();saved.forEach(r=>r[1]?e.putBoolean(r[0],r[2]):e.remove(r[0]));e.apply();e=actions.edit();modes.forEach(r=>r[1]?e.putInt('visibility.'+r[0],r[2]):e.remove('visibility.'+r[0]));e.apply();e=icons.edit();if(iconSaved[0])e.putBoolean('visible.4',iconSaved[1]);else e.remove('visible.4');e.apply();}
 function walk(v,label){if(T.class.isInstance(v)&&String(Java.cast(v,T).getText())===label)return v;if(G.class.isInstance(v)){let g=Java.cast(v,G);for(let i=0;i<g.getChildCount();i++){let x=walk(g.getChildAt(i),label);if(x)return x;}}return null;}
 function check(v,s){if(!v)throw Error(s);console.log('PASS '+s);}
 let found=false;
 Java.choose('android.app.Activity',{onMatch(a){if(found||a.isFinishing()||!a.getWindow().getDecorView().isShown())return;found=true;let activity=Java.retain(a);
 Java.scheduleOnMainThread(function(){let opened=[];try{
  prefs.edit().putBoolean('wacchoiSearchEnabled',true).apply();
  const iconClass=Java.use('o.r8lambda98incQ33GAiiY2082BMi9yDa3l0'),toolbar=Java.use('app.morphe.extension.chmate.ToolbarIcons');
  const flag=iconClass.class.getDeclaredField(String(toolbar.visibilityField()));flag.setAccessible(true);
  icons.edit().putBoolean('visible.4',true).apply();
  check(flag.getBoolean(iconClass.$new(0x7e000005,0,0,0,false)),'toolbar explicit ON restores native hidden flag');
  icons.edit().putBoolean('visible.4',false).apply();
  check(!flag.getBoolean(iconClass.$new(0x7e000005,0,0,0,true)),'toolbar explicit OFF overrides native visible flag');
  let layout=Java.use('android.widget.LinearLayout').$new(activity);O.addSettings(layout,activity);layout.getChildAt(0).performClick();
  Java.choose('android.app.AlertDialog',{onMatch(d){if(d.isShowing())opened.push(Java.retain(d));},onComplete(){
   try{
    let dialog=opened.find(d=>walk(d.getWindow().getDecorView(),'ワッチョイ長押しに下4桁検索を表示')!==null);check(!!dialog,'actual 4-digit settings dialog opens');
    ['下','上'].forEach((side,i)=>{let view=Java.cast(walk(dialog.getWindow().getDecorView(),'ワッチョイ長押しに'+side+'4桁検索を表示'),S);
      view.setChecked(false);check(!R.visible(ids[i]),side+' OFF reaches actual menu visibility');view.setChecked(true);check(R.visible(ids[i]),side+' ON reaches actual menu visibility');});
    dialog.dismiss();
    Java.use('app.morphe.extension.chmate.SettingsRestart').confirm(activity);
    Java.choose('android.app.AlertDialog',{onMatch(d){if(!d.isShowing())return;let cancel=d.getButton(-2);if(cancel&&String(cancel.getText())==='あとで'){
       check(String(d.getButton(-1).getText())==='今すぐ再起動','restart confirmation has explicit choices');let prompt=Java.retain(d);cancel.performClick();
       setTimeout(()=>Java.perform(()=>Java.scheduleOnMainThread(()=>{try{check(!prompt.isShowing(),'Later dismisses without restart');console.log('COMPLETE actual settings UI; original preferences restored');}catch(e){console.log('FAIL '+e.stack);prompt.dismiss();}finally{restore();}})),1500);
      }},onComplete(){restore();}});
   }catch(e){console.log('FAIL '+e.stack);restore();opened.forEach(d=>d.dismiss());}
  }});
 }catch(e){console.log('FAIL '+e.stack);restore();}});
 },onComplete(){if(!found)console.log('FAIL no visible activity');}});
});
