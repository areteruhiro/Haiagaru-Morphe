// Test packages only. No favorite registration, database writes or network refresh.
Java.perform(function () {
 const app=Java.use('android.app.ActivityThread').currentApplication();
 const pkg=String(app.getPackageName());
 if(!/haiagaru\.(uploadhosts191|responsearchive226|responsearchive242)$/.test(pkg))throw Error('Verification package only');
 const version=pkg.includes('191')?'191':pkg.includes('226')?'226':'242';
 Java.scheduleOnMainThread(function(){try{
  let checks=0;
  function check(ok,label){if(!ok)throw Error(label);checks++;console.log('PASS '+label);}
  const F=Java.use('app.morphe.extension.chmate.FavoriteThreads');
  const list=Java.use('java.util.ArrayList').$new();
  const name=version==='191'?'o.onAdDisplayed$setIcon':version==='226'?'o.Yy2$open':'o.getAvailabilityStatus$r8lambdawJ5MHcSJed_CjC7r4OWD0UxyJsQ';
  const cls=Java.use(name).class;
  const Unsafe=Java.use('sun.misc.Unsafe');const uf=Unsafe.class.getDeclaredField('theUnsafe');uf.setAccessible(true);
  const unsafe=Java.cast(uf.get(null),Unsafe);
  let owner=cls,field=null;
  while(owner!==null){try{field=owner.getDeclaredField(version==='191'?'b':version==='226'?'c':'h');break;}catch(e){owner=owner.getSuperclass();}}
  check(field!==null,'rating binding exists');field.setAccessible(true);
  for(const rating of [0,1,5,-1]){const row=unsafe.allocateInstance(cls);if(version==='242')field.setInt(row,rating);else field.setByte(row,rating);list.add(row);}
  const filtered=F.filter(list);
  check(filtered.size()===2,'includes every positive star level, excludes zero and negative');
  check(list.size()===4,'source snapshot unchanged');
  const home=Java.use('java.lang.Object').$new();
  F.selected(home,108);check(F.filterFor(home,list).size()===2,'favorite tab filters');
  F.selected(home,101);check(F.filterFor(home,list).size()===4,'unread tab unchanged');
  F.selected(home,103);check(F.filterFor(home,list).size()===4,'history tab unchanged');
  check(F.nativeMode(108)===102&&F.nativeMode(101)===101&&F.nativeMode(103)===103,'only new tab mapped');
  // Model construction is checked separately via native screen; avoid unattached fragment lifecycle.
  console.log('COMPLETE '+version+' '+checks+' favorite selection checks');
 }catch(e){console.log('FAIL '+e.stack);}});
});
