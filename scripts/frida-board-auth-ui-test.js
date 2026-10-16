/* Synthetic native error, not a real post. Verification package only. */
Java.perform(function(){
 const app=Java.use('android.app.ActivityThread').currentApplication();
 if(String(app.getPackageName())!=='jp.co.airfront.android.a2chMate.haiagaru.patrol242')throw Error('Verification app only');
 const A=Java.use('app.morphe.extension.chmate.ExternalBoardAuth');
 const ref=A.class.getDeclaredField('resumed');ref.setAccessible(true);
 const activity=Java.cast(Java.cast(ref.get(null),Java.use('java.lang.ref.WeakReference')).get(),Java.use('android.app.Activity'));
 const root=activity.findViewById(16908290);
 const Owner=Java.registerClass({name:'app.morphe.verification.BoardAuthOwner'+Date.now(),methods:{
  getActivity:{returnType:'android.app.Activity',argumentTypes:[],implementation:function(){return activity;}},
  getView:{returnType:'android.view.View',argumentTypes:[],implementation:function(){return root;}}
 }});
 Java.scheduleOnMainThread(function(){
  A.target(Owner.$new(),'https://bbs.eddibb.cc/liveedge/');
  // Ff3 constructor is patched. Do not call observe() directly in this test.
  Java.use('o.Ff3').$new('認証が必要です。https://bbs.eddibb.cc/auth-code '+Date.now());
 });
 setTimeout(()=>Java.perform(function(){
  Java.choose('android.app.AlertDialog',{onMatch:function(d){
   if(!d.isShowing())return; const button=d.getButton(-1);
   if(button!==null&&String(button.getText())==='認証ページ') {
    console.log('PASS native exception reached authentication prompt');
    const retained=Java.retain(button);Java.scheduleOnMainThread(()=>retained.performClick());
   }
  },onComplete:function(){}});
 }),1200);
 setTimeout(()=>Java.perform(function(){
  Java.choose('android.app.AlertDialog',{onMatch:function(d){
   if(!d.isShowing())return; const button=d.getButton(-1);
   if(button!==null&&String(button.getText())==='認証ページを開く') {
    console.log('PASS configurable authentication URL dialog opened');
    // Leave the dialog visible for a real UI tap and screenshot inspection.
   }
  },onComplete:function(){}});
 }),3500);
});
