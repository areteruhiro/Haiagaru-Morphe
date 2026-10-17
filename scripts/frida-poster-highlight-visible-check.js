// Read-only installed-app check. No hooks, settings writes, body/ID logging, or network calls.
Java.perform(function(){
 const app=Java.use('android.app.ActivityThread').currentApplication();
 const pkg=String(app.getPackageName());
 if(!/^jp\.co\.airfront\.android\.a2chMate\.haiagaru\.(archivemenu191|responsearchive226|responsearchive242)$/.test(pkg))throw Error('Wrong verification package');
 const prefs=app.getSharedPreferences('haiagaru.poster-highlight',0);
 if(!prefs.getBoolean('enabled',false))throw Error('Highlight not enabled');
 if(JSON.parse(String(prefs.getString('rules','[]'))).length===0)throw Error('No saved highlight rules');
 const Spanned=Java.use('android.text.Spanned');
 const Tint=Java.use('app.morphe.extension.chmate.PosterHighlight$Tint');
 let highlighted=0,plain=0;
 Java.choose('android.widget.TextView',{
  onMatch:function(view){
   const held=Java.retain(view);
   Java.scheduleOnMainThread(function(){
    if(!held.isShown())return;
    const text=held.getText();if(!Spanned.class.isInstance(text))return;
    const spans=Java.cast(text,Spanned);
    if(spans.getSpans(0,text.length(),Tint.class).length>0)highlighted++;else plain++;
   });
  },
  onComplete:function(){Java.scheduleOnMainThread(function(){
   if(highlighted<1)throw Error('No visible highlighted text');
   if(plain<1)throw Error('No visible unhighlighted text');
   console.log('PASS '+pkg+' saved rule enabled; visible highlighted='+highlighted+', other='+plain);
  });}
 });
});
