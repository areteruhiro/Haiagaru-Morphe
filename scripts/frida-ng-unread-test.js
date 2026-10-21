Java.perform(()=>{
 const app=Java.use('android.app.ActivityThread').currentApplication();if(!String(app.getPackageName()).endsWith('.settingsfix191'))throw Error('Test package required');
 Java.openClassFile('/data/local/tmp/haiagaru-ng-unread-tests.dex').load();
 console.log(Java.use('app.morphe.extension.chmate.HiddenNgUnreadAndroidTest').run(app));
});
