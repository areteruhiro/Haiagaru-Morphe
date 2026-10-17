// Runtime proof of the post-return hook. Does not save responses or change native menu settings.
Java.perform(function(){
 const app=Java.use('android.app.ActivityThread').currentApplication();
 if(String(app.getPackageName())!=='jp.co.airfront.android.a2chMate.haiagaru.archivemenu191')throw Error('Wrong package');
 const Archive=Java.use('app.morphe.extension.chmate.ResponseArchive');
 const Dialog=Java.use('o.r8lambdafLXKIgI8H4VR9SponZBKnK7_9gE');
 const build=Dialog.c.overload();
 build.implementation=function(){const m=build.call(this);Archive.appendDialog(this,m);return m;};
 Java.choose(Dialog.$className,{onMatch:function(d){
  const held=Java.retain(d);
  Java.scheduleOnMainThread(function(){
   if(!held.isAdded())return;
   const f=held.getClass().getDeclaredField('b');f.setAccessible(true);
   const value=f.get(held);if(value===null)return;
   const adapter=Java.cast(value,Java.use('o.m6f')), menu=adapter.a.value;
   Archive.appendDialog(held,menu);adapter.notifyDataSetChanged();
   let count=0;
   for(let i=0;i<menu.size();i++){
    const item=menu.getItem(i);
    if(item.getItemId()===0x7e484a01||item.getItemId()===0x7e484a03){count++;console.log('PASS '+item.getItemId()+' '+item.getTitle());}
   }
   if(count!==2)throw Error('Archive menu items missing');
   console.log('RUNTIME_FIX_APPLIED');
  });
 },onComplete:function(){}});
});
