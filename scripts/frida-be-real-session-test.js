// Explicitly requested real BE authentication test. Does not call any posting method.
// Never print credentials, cookies, URLs, response bodies, or exception messages.
Java.perform(function(){
 const app=Java.use('android.app.ActivityThread').currentApplication();
 if(String(app.getPackageName())!=='jp.co.airfront.android.a2chMate.haiagaru.xpreviewfix191')throw Error('Wrong verification app');
 const B=Java.use('app.morphe.extension.chmate.BeAutoLogout');
 const prefs=app.getSharedPreferences('io.github.areteruhiro.chmate.haiagaru.ui-config',0);
 if(!prefs.getBoolean('beLogoutAfterThread',false))throw Error('BE automation disabled');
 const f=B.class.getDeclaredField('targets');f.setAccessible(true);
 const map=Java.cast(f.get(null),Java.use('java.util.Map'));
 const it=map.entrySet().iterator();let target=null,count=0;
 while(it.hasNext()){
  const e=Java.cast(it.next(),Java.use('java.util.Map$Entry'));
  const activity=Java.cast(e.getKey(),Java.use('android.app.Activity'));
  if(!activity.isFinishing()&&!activity.isDestroyed()&&activity.hasWindowFocus()){target=e.getValue();count++;}
 }
 if(count!==1)throw Error('Expected exactly one active 5ch thread editor; found '+count);
 const board=Java.retain(target);
 const Worker=Java.use(String(B.nativeWorker()));
 const native=Worker[String(B.nativeAction())].overload('[Ljava.lang.Object;');
 native.implementation=function(args){
  const result=native.call(this,args);
  if(result!==null && Java.use('java.lang.Exception').class.isInstance(result)){
   const error=Java.cast(result,Java.use('java.lang.Exception'));
   const text=String(error.getMessage()),type=String(error.getClass().getName());
   let category='other';
   if(text.indexOf('設定されていません')>=0)category='saved_credentials_missing';
   else if(text.indexOf('ログインエラー')>=0)category='authentication_response_rejected';
   else if(/SSL|Handshake|CertPath/.test(type))category='tls_failure';
   else if(/Timeout/.test(type))category='network_timeout';
   else if(/UnknownHost/.test(type))category='dns_failure';
   console.log('NATIVE_ERROR category='+category+' type='+type);
  }
  return result;
 };
 const Task=Java.registerClass({name:'app.morphe.test.RealBeSessionNoPost',implements:[Java.use('java.lang.Runnable')],methods:{run:function(){
  console.log('START real BE authentication only; no post');
  let ok=false;
  try {B.beforeSend(board);ok=true;console.log('PASS real automatic BE login returned successfully');}
  catch(e){
   let category='unclassified',type='unknown';
   try {
    let cause=e;
    if(e.cause)cause=e.cause;
    if(cause.getCause){for(let n=0;n<5;n++){const next=cause.getCause();if(next===null)break;cause=next;}}
    const text=String(cause);
    if(cause.getClass)type=String(cause.getClass().getName());
    if(text.indexOf('設定されていません')>=0)category='saved_credentials_missing';
    else if(text.indexOf('ログインエラー')>=0)category='authentication_response_rejected';
    else if(/SSL|Handshake|CertPath/.test(type))category='tls_failure';
    else if(/Timeout/.test(type))category='network_timeout';
    else if(/UnknownHost/.test(type))category='dns_failure';
    else if(/ClassNotFound|NoSuchMethod|IllegalAccess/.test(type))category='binding_failure';
   }catch(ignored){}
   console.log('FAIL automatic BE login category='+category+' exceptionType='+type);
  }
  finally {
   try{B.action(board,false);console.log('PASS native BE logout returned successfully');}
   catch(e){console.log('FAIL native BE logout; manually log out before replying');}
   console.log('COMPLETE no thread submitted; loginSuccess='+ok);
   board.$dispose();
  }
 }}});
 Java.use('java.lang.Thread').$new(Task.$new(),'Haiagaru-BE-no-post-test').start();
});
