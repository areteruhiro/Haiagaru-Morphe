// Read-only: only presence booleans, never print saved values or cookies. No network.
Java.perform(function(){
 const app=Java.use('android.app.ActivityThread').currentApplication();
 if(String(app.getPackageName())!=='jp.co.airfront.android.a2chMate.haiagaru.xpreviewfix191')throw Error('Wrong app');
 const Prefs=Java.use('jp.syoboi.a2chMate.Prefs');
 const getter=Prefs.b.overload('o.m1b$ActivityResult');
 ['r','p','q','k'].forEach(function(name){
  const f=Prefs.class.getDeclaredField(name);f.setAccessible(true);
  const descriptor=Java.cast(f.get(null),Java.use('o.m1b$ActivityResult'));
  const value=getter.call(Prefs,descriptor);
  console.log('NATIVE_BE_FIELD '+name+' nonempty='+(value!==null&&String(value).length>0));
 });
 const B=Java.use('app.morphe.extension.chmate.BeAutoLogout');
 const field=B.class.getDeclaredField('targets');field.setAccessible(true);
 const map=Java.cast(field.get(null),Java.use('java.util.Map'));
 const it=map.values().iterator();let count=0;
 while(it.hasNext()){
  const board=Java.cast(it.next(),Java.use('jp.syoboi.a2chMate.client.BBSUrlInfo'));
  console.log('EDITOR_BOARD_ENGINE '+board.g.value);count++;
 }
 console.log('REGISTERED_THREAD_EDITORS '+count);
});
