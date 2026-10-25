// Own verification package, synthetic .invalid page only. Never operates a real challenge or submits a post.
Java.perform(function(){
 const app=Java.use('android.app.ActivityThread').currentApplication();
 if(String(app.getPackageName())!=='jp.co.airfront.android.a2chMate.haiagaru.settingsfix191')throw Error('Verification package only');
 Java.classFactory.loader=app.getClassLoader();
 Java.openClassFile('/data/local/tmp/haiagaru-board-challenge-fixture.dex').load();
 const Fixture=Java.use('app.morphe.extension.chmate.BoardChallengeWebViewFixture');
 const Auth=Java.use('app.morphe.extension.chmate.ExternalBoardAuthActivity');
 const Hub=Java.use('app.morphe.extension.chmate.ExternalBoardAuth');
 const prepare=Hub.prepareWebSession.overload('android.app.Activity','java.lang.String','java.lang.String','java.lang.Runnable');
 // Offline renderer test only: no real cookie jar or network bootstrap is used.
 prepare.implementation=function(a,board,url,ready){
  if(String(board)==='https://fixture.invalid/board/'){ready.run();return;}
  return prepare.call(this,a,board,url,ready);
 };
 const Callback=Java.registerClass({name:'app.morphe.fixture.ChallengeValueCallback'+Date.now(),implements:[Java.use('android.webkit.ValueCallback')],methods:{onReceiveValue:function(value){handle(String(value));}}});
 let checks=0,phase=0,attempts=0,auth,web,timer,home;
 const prefs=app.getSharedPreferences('haiagaru.external-board-auth',0),before=String(prefs.getAll());
 function check(ok,label){if(!ok)throw Error(label);checks++;console.log('PASS '+label);}
 function clean(){if(timer)clearInterval(timer);if(auth&&!auth.isFinishing())auth.finish();prepare.implementation=null;}
 function fail(e){console.log('FAIL '+(e.stack||e));clean();}
 function handle(raw){try{
  if(raw==='null'){if(++attempts>25)throw Error('Fixture DOM did not load');return;}
  const result=JSON.parse(JSON.parse(raw));
  if(phase===0){
   check(result.origin==='https://fixture.invalid'&&result.secure,'HTTPS base gives secure same-origin page');
   check(result.starts===0&&result.submits===0,'page load does not calculate or submit');
   check(result.workerSupported,'Web Worker available');phase=1;attempts=0;
   check(String(result.charset).toLowerCase()==='shift_jis'&&result.title==='Captcha認証','original CAPTCHA page and Shift_JIS form encoding retained');
   web.evaluateJavascript("document.getElementById('compute').click()",null);
  }else if(phase===1){
   if(result.worker!==14){if(++attempts>25)throw Error('Offline Worker failed');return;}
   check(result.starts===1&&result.worker===14,'manual fixture button executes original Worker');
   check(result.submits===0,'Worker completion does not submit');phase=2;attempts=0;
   web.evaluateJavascript("document.getElementById('confirm').click()",null);
  }else if(phase===2){
   if(result.submits!==1||result.blockedPost!==1){if(result.blockedPost<0||++attempts>25)throw Error('Offline script POST was not locally blocked');return;}
   check(result.submits===1&&result.starts===1,'fixture confirmation event retains original DOM behavior');
   check(result.blockedPost===1,'actual WebView fetch POST is locally blocked before network');
   check(Fixture.networkChecks(auth)===4,'default-off POST guard, explicit permission and cross-origin protection');
   check(String(prefs.getAll())===before,'fixture does not enable cookie sharing or change auth settings');
   phase=3;attempts=0;
   auth.reloadPage();
  }else if(phase===3){
   if(result.starts!==0||result.submits!==0){if(++attempts>25)throw Error('Original form not restored on redisplay');return;}
   check(result.origin==='https://fixture.invalid'&&result.title==='Captcha認証','redisplay preserves original form instead of GET to posting endpoint');
   phase=4;clean();
   setTimeout(()=>Java.perform(()=>Java.scheduleOnMainThread(()=>{try{
    check(Fixture.forgotten(home),'closing discards ephemeral original HTML');
    console.log('BOARD_CHALLENGE_FIXTURE_COMPLETE '+checks+' checks; no network or real authentication');
   }catch(e){fail(e);}})),2000);
  }
 }catch(e){fail(e);}}
 const records=Java.use('android.app.ActivityThread').currentActivityThread().mActivities.value.values().iterator();
 while(records.hasNext()){
  const record=Java.cast(records.next(),Java.use('android.app.ActivityThread$ActivityClientRecord'));
  const a=record.activity.value;
  if(a&&!a.isFinishing()&&String(a.getPackageName())===String(app.getPackageName())&&!String(a.getClass().getName()).includes('ExternalBoardAuthActivity'))home=Java.retain(a);
 }
 Java.scheduleOnMainThread(()=>{try{
  if(!home)throw Error('Home Activity unavailable');
  console.log(Fixture.open(home));
  timer=setInterval(()=>Java.perform(()=>Java.scheduleOnMainThread(()=>{try{
   if(!web)Java.choose(Auth.$className,{onMatch(a){if(!a.isFinishing()&&String(a.getIntent().getStringExtra('board'))==='https://fixture.invalid/board/'){auth=Java.retain(a);web=Java.retain(a.web.value);}},onComplete(){}});
   if(!web){if(++attempts>25)throw Error('Fixture Activity unavailable');return;}
   web.evaluateJavascript("window.fixtureReady?JSON.stringify({origin:location.origin,secure:isSecureContext,starts:window.starts,submits:window.submits,worker:window.workerValue,workerSupported:typeof Worker==='function',charset:document.characterSet,title:document.title,blockedPost:window.blockedPost}):null",Callback.$new());
  }catch(e){fail(e);}})),400);
 }catch(e){fail(e);}});
});
