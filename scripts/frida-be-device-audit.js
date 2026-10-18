// Read-only feature audit. Never read credentials, cookies, or posting contents.
Java.perform(function(){
 const app=Java.use('android.app.ActivityThread').currentApplication();
 console.log('PACKAGE '+app.getPackageName());
 try {
  const B=Java.use('app.morphe.extension.chmate.BeAutoLogout');
  console.log('WORKER '+B.nativeWorker());
  console.log('ACTION '+B.nativeAction());
  console.log('BOARD_GETTER '+B.nativeBoardUrl());
  console.log('BE_AUTOMATION_ENABLED '+app.getSharedPreferences('io.github.areteruhiro.chmate.haiagaru.ui-config',0).getBoolean('beLogoutAfterThread',false));
  console.log('BE_SEND_GATE '+Boolean(B.beforeSend));
 }catch(e){console.log('FEATURE_UNAVAILABLE '+e);}
});
