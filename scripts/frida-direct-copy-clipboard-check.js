// Check only whether our selected public thread substring was copied; do not log clipboard text.
Java.perform(function(){Java.scheduleOnMainThread(function(){
 const app=Java.use('android.app.ActivityThread').currentApplication();
 if(String(app.getPackageName())!=='jp.co.airfront.android.a2chMate.haiagaru.uploadhosts191')throw Error('Wrong package');
 const manager=Java.cast(app.getSystemService('clipboard'),Java.use('android.content.ClipboardManager'));
 const clip=manager.getPrimaryClip();
 const value=clip===null?'':String(clip.getItemAt(0).coerceToText(app));
 console.log(JSON.stringify({expectedSelectionCopied:value==='自動',copiedLength:value.length}));
});});
