// No network, no login and no post. Replace only the logout worker with a test spy.
Java.perform(function(){
 const app=Java.use('android.app.ActivityThread').currentApplication();
 if(!/\.haiagaru\.(uploadhosts191|responsearchive226|responsearchive242|xpreviewfix191)$/.test(String(app.getPackageName())))throw Error('Verification package only');
 const loader=Java.use('dalvik.system.DelegateLastClassLoader').$new('/data/local/tmp/be-auto-test.dex',app.getClassLoader());
 const factory=Java.ClassFactory.get(loader),B=factory.use('app.morphe.extension.chmate.BeAutoLogout');
 const prefs=app.getSharedPreferences('io.github.areteruhiro.chmate.haiagaru.ui-config',0),key='beLogoutAfterThread';
 const old=prefs.contains(key)?prefs.getBoolean(key,false):null;
 let calls=0,logins=0,checks=0,failLogin=false,boardUrl='https://egg.5ch.io/android/';
 function check(ok,label){if(!ok)throw Error(label);checks++;console.log('PASS '+label);}
 const Worker=Java.registerClass({name:'app.morphe.test.BeLogoutSpy',methods:{
  $init:[{argumentTypes:['java.lang.Object'],implementation:function(_){this.$super.$init();}}],
  boardUrl:{returnType:'java.lang.String',argumentTypes:[],implementation:function(){return boardUrl;}},
  run:{returnType:'java.lang.Object',argumentTypes:['[Ljava.lang.Object;'],implementation:function(args){
   if(String(args[0])==='true'){logins++;return failLogin?Java.use('java.lang.Exception').$new('test rejection'):null;}
   check(String(args[0])==='false','native worker receives logout');calls++;return null;
  }}
 }});
 B.nativeWorker.implementation=function(){return 'app.morphe.test.BeLogoutSpy';};
 B.nativeAction.implementation=function(){return 'run';};
 B.nativeBoardUrl.implementation=function(){return 'boardUrl';};
 B.message.implementation=function(context,text){console.log('NOTICE '+text);};
 const targets= B.class.getDeclaredField('targets');targets.setAccessible(true);
 const map=Java.cast(targets.get(null),Java.use('java.util.Map'));
 const owner=Worker.$new(null);
 function target(){map.put(owner,owner);}
 function restore(){const edit=prefs.edit();if(old===null)edit.remove(key);else edit.putBoolean(key,old);edit.commit();}
 try{
  check(B.eligible(true,null,'https://egg.5ch.io/android/'),'5ch thread allowed');
  check(!B.eligible(false,null,'https://egg.5ch.io/android/'),'default off');
  check(!B.eligible(true,'https://egg.5ch.io/test/read.cgi/android/1/','https://egg.5ch.io/android/'),'reply excluded');
  check(!B.eligible(true,null,'https://bbs.eddibb.cc/liveedge/'),'other board excluded');
  check(!B.eligible(true,null,'https://mercury.bbspink.com/test/'),'pink is not 5ch BE');
  check(!B.eligible(true,null,'https://5ch.io.example.org/android/'),'lookalike host excluded');
  prefs.edit().putBoolean(key,false).commit();B.beforeSend(owner);check(logins===0,'disabled sends do not log in');
  prefs.edit().putBoolean(key,true).commit();boardUrl='https://bbs.eddibb.cc/liveedge/';
  B.beforeSend(owner);check(logins===0,'non-5ch sends do not log in');boardUrl='https://egg.5ch.io/android/';
  failLogin=true;let blocked=false;try{B.beforeSend(owner);}catch(e){blocked=true;}
  check(blocked&&logins===1,'authentication failure throws before submission');
  failLogin=false;B.beforeSend(owner);check(logins===2,'thread send waits for successful login');
  prefs.edit().putBoolean(key,false).commit();target();B.completed(owner,-1);
  check(calls===0&&map.containsKey(owner),'off does not log out');
  prefs.edit().putBoolean(key,true).commit();B.completed(owner,0);
  check(calls===0&&map.containsKey(owner),'cancel or error does not consume thread target');
  map.remove(owner);B.completed(owner,-1);check(calls===0,'ordinary reply has no registered target');
  target();B.completed(owner,-1);
  setTimeout(function(){Java.perform(function(){try{
   check(calls===1,'successful thread calls logout once');
   B.completed(owner,-1);check(calls===1,'duplicate completion ignored');
   console.log('COMPLETE '+checks+' BE thread login/logout checks (no HTTP)');
  }catch(e){console.log('FAIL '+e.stack);}finally{restore();}});},1500);
 }catch(e){restore();throw e;}
});
