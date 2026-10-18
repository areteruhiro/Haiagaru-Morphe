Java.perform(function(){
 const app=Java.use('android.app.ActivityThread').currentApplication();
 if(!/haiagaru\.(uploadhosts191|responsearchive226|responsearchive242)$/.test(String(app.getPackageName())))throw Error('Test app only');
 const loader=Java.use('dalvik.system.DelegateLastClassLoader').$new('/data/local/tmp/live-settings-test.dex',app.getClassLoader());
 const Test=Java.ClassFactory.get(loader).use('app.morphe.extension.chmate.VerifyLiveSettingsAndroid');
 let found=false;
 Java.choose('android.app.Activity',{onMatch(a){if(found||a.isFinishing()||!a.getWindow().getDecorView().isShown())return;
 found=true;const activity=Java.retain(a);Java.scheduleOnMainThread(function(){try{console.log(String(Test.run(activity)));Test.runSearch(activity);}catch(e){console.log('FAIL '+e.stack);}});
 },onComplete(){if(!found)console.log('FAIL no visible activity');}});
});
