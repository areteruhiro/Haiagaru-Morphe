Java.perform(function(){
 const app=Java.use('android.app.ActivityThread').currentApplication();
 if(!/\.haiagaru\.(settingsfix191|resumefix226|resumefix242)$/.test(String(app.getPackageName())))throw Error('Verification package required');
 // Fixture isolates native evidence and never contacts a server.
 const status=Java.use('app.morphe.extension.chmate.DroppedThreadStatus');
 status.droppedUrl.implementation=function(){return false;};
 try {
  Java.openClassFile('/data/local/tmp/history-drop-regression-v2.dex').load();
  console.log(Java.use('app.morphe.extension.chmate.HistoryDropRegression').run(app));
 } catch(e){console.log('FAIL '+e.stack);if(e.$h)console.log(Java.use('android.util.Log').getStackTraceString(e));}
 finally {status.droppedUrl.implementation=null;}
});
