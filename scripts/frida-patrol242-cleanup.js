// Disable only the isolated verification app's temporary patrol settings.
Java.perform(function(){
 const app=Java.use('android.app.ActivityThread').currentApplication();
 if(String(app.getPackageName())!=='jp.co.airfront.android.a2chMate.haiagaru.patrol242') throw Error('Verification app only');
 app.getSharedPreferences('io.github.areteruhiro.chmate.haiagaru.ui-config',0).edit().putBoolean('patrolImagesEnabled',false).commit();
 const Prefs=Java.use('jp.syoboi.a2chMate.prefs.Prefs');
 Prefs.d.overload('o.ApiAnyClient$RemoteActionCompatParcelizer','int').call(Prefs,Prefs.as(),0);
 const scheduler=Java.use('o.TTRewardVideoActivity8');scheduler.e(app);scheduler.b(app);
 console.log('VERIFICATION_PATROL_DISABLED');
});
