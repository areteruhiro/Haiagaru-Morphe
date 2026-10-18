Java.perform(function(){
 const app=Java.use('android.app.ActivityThread').currentApplication();
 const pkg=String(app.getPackageName());
 if(!/haiagaru\.(uploadhosts191|responsearchive226|responsearchive242)$/.test(pkg))throw Error('Test app only');
 const owner=pkg.includes('191')?'o.r8lambdaTb_p0z6z2AqSZIga1YhmAVmiTPk':pkg.includes('226')?'jp.syoboi.a2chMate.fragment.HomeFragment':'jp.syoboi.a2chMate.ui.home.HomeFragment';
 Java.choose(owner,{onMatch:function(f){const held=Java.retain(f);Java.scheduleOnMainThread(function(){try{
  if(!held.isAdded())return;
  // Native sort tab selection; never starts refresh or changes favorites.
  held[pkg.includes('242')?'d':'a'].overload('int').call(held,108);
  console.log('PASS native HOME selected favorite tab');
  if(!pkg.includes('242')){
   const af=held.getClass().getDeclaredField(pkg.includes('191')?'e':'i');af.setAccessible(true);
   const adapter=Java.cast(af.get(held),Java.use(pkg.includes('191')?'o.r8lambdaXZREaGE4vvLAA2ZOtsKBd3knAPA':'o.bea1'));
   const rows=Java.use('java.util.ArrayList').$new();
   for(let i=0;i<adapter.getCount();i++)rows.add(adapter.getItem(i));
   const filtered=Java.use('app.morphe.extension.chmate.FavoriteThreads').filter(rows);
   let threadCount=0;
   for(let i=0;i<rows.size();i++){const name=String(rows.get(i).getClass().getName());if(name==='o.Yy2$open'||name==='o.onAdDisplayed$setIcon')threadCount++;}
   if(threadCount!==filtered.size())throw Error('Unstarred native thread in favorites tab');
   console.log('PASS native displayed favorites only: '+threadCount+' threads');
  }
 }catch(e){console.log('FAIL '+e.stack);}});},onComplete:function(){}});
});
