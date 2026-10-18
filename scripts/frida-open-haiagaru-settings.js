Java.perform(function(){
 const app=Java.use('android.app.ActivityThread').currentApplication();
 if(!String(app.getPackageName()).endsWith('haiagaru.responsearchive242'))throw Error('Test app only');
 let found=false;
 Java.choose('android.app.Activity',{onMatch(a){
  if(found||a.isFinishing()||!a.getWindow().getDecorView().isShown())return;
  found=true;const activity=Java.retain(a);
  Java.scheduleOnMainThread(function(){Java.use('app.morphe.extension.chmate.Haiagaru').showSettingsDialog(activity);console.log('Opened actual settings');});
 },onComplete(){if(!found)console.log('No visible activity');}});
});
