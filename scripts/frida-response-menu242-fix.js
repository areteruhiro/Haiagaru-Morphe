// Proof for the ordinary 242 response menu, using its selected native response.
Java.perform(function(){
 const app=Java.use('android.app.ActivityThread').currentApplication();
 if(!/haiagaru\.(responsearchive242|archivemenu242)$/.test(String(app.getPackageName())))throw Error('Wrong package');
 const A=Java.use('app.morphe.extension.chmate.ResponseArchive');
 function field(o,n){for(let c=o.getClass();c!==null;c=c.getSuperclass()){try{let f=c.getDeclaredField(n);f.setAccessible(true);return f.get(o);}catch(e){}}throw Error('Field '+n);}
 const D=Java.use('jp.syoboi.a2chMate.fragment.ResMenuDialogFragment'),g=D.g.overload();
 function append(d,m){
  const p=d.getParentFragment(),response=field(field(p,'o'),'c'),board=field(field(p,'r'),'b');
  A.append(p,m,response,board);
  const menu=Java.cast(m,Java.use('android.view.Menu'));let count=0;
  menu.findItem(0x7e484a01).setTitle.overload('java.lang.CharSequence').call(menu.findItem(0x7e484a01),'レスを保存');
  const hi=menu.findItem(0x7e484b01);if(hi!==null)hi.setTitle.overload('java.lang.CharSequence').call(hi,'投稿者を強調');
  for(let i=0;i<menu.size();i++){const item=menu.getItem(i);if(item.getItemId()===0x7e484a01||item.getItemId()===0x7e484a03){count++;console.log('PASS native242 '+item.getItemId()+' '+item.getTitle());}}
  if(count!==2)throw Error('Missing archive actions');
 }
 g.implementation=function(){const m=g.call(this);append(this,m);return m;};
 const click=D.c.overload('jp.syoboi.a2chMate.fragment.ResMenuDialogFragment','o.TTPlayableLandingPageActivity15');
 click.implementation=function(d,item){
  if(A.handle(item)){d.dismiss();console.log('PASS ordinary242 archive click');return Java.use('o.zzbwq').INSTANCE.value;}
  return click.call(this,d,item);
 };
 Java.choose(D.$className,{onMatch:function(d){const held=Java.retain(d);Java.scheduleOnMainThread(function(){if(held.isAdded()){const m=g.call(held);append(held,m);}});},onComplete:function(){console.log('READY');}});
});
