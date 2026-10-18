// Inspect current native UI without emitting any user-entered text.
Java.perform(function(){
 const app=Java.use('android.app.ActivityThread').currentApplication();
 if(String(app.getPackageName())!=='jp.co.airfront.android.a2chMate.haiagaru.xpreviewfix191')throw Error('Wrong app');
 const Activity=Java.use('android.app.Activity'),TV=Java.use('android.widget.TextView');
 Java.choose('jp.syoboi.a2chMate.activity.SettingActivity',{onMatch:function(a){
  if(a.isDestroyed()||a.isFinishing())return;
  const activity=Java.retain(a);
  Java.scheduleOnMainThread(function(){
   function walk(v){
    if(TV.class.isInstance(v)){
     const t=Java.cast(v,TV),s=String(t.getText());
     const labels=['BE','Be','5ch','2ch','ログイン','パスワード','メール','アカウント','保存'];
     const tags=labels.filter(x=>s.indexOf(x)>=0);
     if(tags.length)console.log('VISIBLE_LABEL tags='+tags.join(',')+' class='+v.getClass().getSimpleName());
     if(Java.use('android.widget.EditText').class.isInstance(v))console.log('EDIT_FIELD nonempty='+(s.length>0)+' passwordType='+((t.getInputType()&128)!==0));
    }
    if(Java.use('android.view.ViewGroup').class.isInstance(v)){
     const group=Java.cast(v,Java.use('android.view.ViewGroup'));
     for(let n=0;n<group.getChildCount();n++)walk(group.getChildAt(n));
    }
   }
   walk(activity.getWindow().getDecorView());activity.$dispose();
  });
 },onComplete:function(){}});
});
