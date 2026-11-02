let fixtureCount=20,reads=0,background=true,app,W,pref,nm;
const watchUrl='https://fixture.invalid/test/read.cgi/a/1793113633/';
function run(fn){return new Promise((resolve,reject)=>Java.perform(()=>{try{resolve(fn());}catch(e){reject(Error(String(e.stack||e)));}}));}
rpc.exports={
 setup(){return run(()=>{
  app=Java.use('android.app.ActivityThread').currentApplication();if(String(app.getPackageName())!=='jp.co.airfront.android.a2chMate.haiagaru.migration191242')throw Error('Isolated package required');
  W=Java.use('app.morphe.extension.chmate.ThreadWatch');pref=app.getSharedPreferences('haiagaru_thread_watch',0);if(!pref.getAll().isEmpty())throw Error('Existing registrations');
  nm=Java.cast(app.getSystemService('notification'),Java.use('android.app.NotificationManager'));
  W.fetch.implementation=function(url,cancel){reads++;background=background&&Java.use('android.os.Looper').myLooper()===null;return '1793113633.dat<>background fixture ('+fixtureCount+')\n';};
  pref.edit().putString(watchUrl,'{"count":-1,"token":"background-fixture"}').commit();W.schedule(app);return true;
 });},
 next(){return run(()=>{fixtureCount=22;return true;});},
 state(){return run(()=>{const item=Java.use('org.json.JSONObject').$new(pref.getString(watchUrl,'{}'));let notifications=0;for(const n of nm.getActiveNotifications())if(String(n.getTag())===watchUrl)notifications++;return {reads,background,count:item.optInt('count',-1),notifications};});},
 cleanup(){return run(()=>{pref.edit().remove(watchUrl).commit();W.schedule(app);nm.cancel.overload('java.lang.String','int').call(nm,watchUrl,0x48690031);W.fetch.implementation=null;return true;});}
};
