// Observe a user's existing public response selection and verify the next Copy.
// Does not launch activities, change selection, or print text/clipboard contents.
Java.perform(function(){
 const Thread=Java.use('android.app.ActivityThread');
 const app=Thread.currentApplication();
 if(String(app.getPackageName())!=='jp.co.airfront.android.a2chMate.haiagaru.uploadhosts191')throw Error('Wrong package');
 let expected=null;
 const Group=Java.use('android.view.ViewGroup'), Text=Java.use('android.widget.TextView');
 function visit(v){
  if(String(v.getClass().getName())==='o.r8lambdaDC40CispcBM3LVwPP01tT_FjLc'){
   const t=Java.cast(v,Text);
   if(t.hasSelection()){
    const a=Math.min(t.getSelectionStart(),t.getSelectionEnd()),b=Math.max(t.getSelectionStart(),t.getSelectionEnd());
    expected=String(t.getText()).substring(a,b);
   }
  }
  if(Group.class.isInstance(v)){const g=Java.cast(v,Group);for(let i=0;i<g.getChildCount();i++)visit(g.getChildAt(i));}
 }
 Java.scheduleOnMainThread(function(){
  const records=Thread.currentActivityThread().mActivities.value.values().toArray();
  for(let i=0;i<records.length;i++){
   const r=Java.cast(records[i],Java.use('android.app.ActivityThread$ActivityClientRecord'));
   if(!r.paused.value)visit(r.activity.value.getWindow().getDecorView());
  }
  console.log(JSON.stringify({selectedBeforeCopy:expected!==null,selectionLength:expected===null?0:expected.length}));
 });
 const Clipboard=Java.use('android.content.ClipboardManager');const set=Clipboard.setPrimaryClip;
 set.implementation=function(clip){
  const result=set.call(this,clip);
  const actual=clip===null?'':String(clip.getItemAt(0).coerceToText(app));
  console.log(JSON.stringify({copyMatchesSelection:expected!==null&&actual===expected,copiedLength:actual.length}));
  return result;
 };
});
