// Verify full native grid labels without changing menu actions or saved data.
Java.perform(function(){
 const app=Java.use('android.app.ActivityThread').currentApplication();
 if(String(app.getPackageName())!=='jp.co.airfront.android.a2chMate.haiagaru.archivemenu191')throw Error('Wrong package');
 Java.choose('o.m6f',{onMatch:function(a){const held=Java.retain(a);Java.scheduleOnMainThread(function(){
  const roots=Java.use('android.view.WindowManagerGlobal').getInstance().mViews.value;
  const Text=Java.use('android.widget.TextView'),Group=Java.use('android.view.ViewGroup');
  function visit(v){
   if(Text.class.isInstance(v)){const t=Java.cast(v,Text),s=String(t.getText());
    if(s==='このレスをアーカイブに保存'||s==='この人の書き込みを強調'){
     t.setMaxLines(8);t.setEllipsize(null);t.setSingleLine(false);
     const lp=t.getLayoutParams();lp.height.value=-2;t.setLayoutParams(lp);t.requestLayout();
     console.log('EXPANDED '+s);
    }
   }
   if(Group.class.isInstance(v)){const g=Java.cast(v,Group);for(let i=0;i<g.getChildCount();i++)visit(g.getChildAt(i));}
  }
  for(let i=0;i<roots.size();i++)visit(Java.cast(roots.get(i),Java.use('android.view.View')));
 });},onComplete:function(){}});
});
