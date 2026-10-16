/* Isolated verification packages only. Uses dummy Cookie values and never posts. */
Java.perform(function(){
 const app=Java.use('android.app.ActivityThread').currentApplication();
 if(!/\.haiagaru\.(?:patrol|auth)(191|226|242)$/.test(String(app.getPackageName()))) throw Error('Verification package only');
 const A=Java.use('app.morphe.extension.chmate.ExternalBoardAuth'); A.initialize(app);
 const jarClass=Java.use('com.franmontiel.persistentcookiejar.PersistentCookieJar');
 const methods=jarClass.class.getDeclaredMethods(); let loadName,urlName,saveName;
 for(const m of methods){const args=m.getParameterTypes();if(String(m.getReturnType().getName())==='java.util.List'&&args.length===1&&!String(m.getName()).startsWith('haiagaru')){loadName=String(m.getName());urlName=String(args[0].getName());}}
 if(!loadName)throw Error('Native cookie loader missing');
 for(const m of methods){const args=m.getParameterTypes();if(String(m.getReturnType().getName())==='void'&&args.length===2&&!String(m.getName()).startsWith('haiagaru')&&String(args[1].getName())==='java.util.List')saveName=String(m.getName());}
 const URL=Java.use(urlName);let urlParse;
 for(const m of URL.class.getDeclaredMethods()){const args=m.getParameterTypes();if((m.getModifiers()&8)!==0&&String(m.getReturnType().getName())===urlName&&args.length===1&&String(args[0].getName())==='java.lang.String'){urlParse=String(m.getName());break;}}
 if(!urlParse)throw Error('Native URL parser missing');
 const url=s=>URL[urlParse](s);
 const cache=Java.use('com.franmontiel.persistentcookiejar.cache.SetCookieCache').$new();
 app.getSharedPreferences('haiagaru.auth-test-cookies',0).edit().clear().commit();
 const persistorClass=Java.use('com.franmontiel.persistentcookiejar.persistence.SharedPrefsCookiePersistor');
 let persistor;
 for(const ctor of persistorClass.class.getDeclaredConstructors()){
  const args=ctor.getParameterTypes();if(args.length!==1)continue;
  if(String(args[0].getName())==='android.content.SharedPreferences')persistor=persistorClass.$new(app.getSharedPreferences('haiagaru.auth-test-cookies',0));
  else if(String(args[0].getName())==='android.content.Context')persistor=persistorClass.$new(app);
 }
 if(!persistor)throw Error('Native cookie persistor ctor missing');
 const jar=jarClass.$new(cache,persistor);
 const prefs=app.getSharedPreferences('haiagaru.external-board-auth',0);
 const key='enabled:https://haiagaru-auth-test.invalid';
 const manager=Java.use('android.webkit.CookieManager').getInstance();let checks=0;
 const check=(ok,label)=>{if(!ok)throw Error('FAIL '+label);checks++;console.log('PASS '+label);};
 function has(address){const list=jar[loadName](url(address));for(let i=0;i<list.size();i++)if(String(list.get(i)).startsWith('haiagaru_dummy_auth='))return true;return false;}
 Java.scheduleOnMainThread(function(){
  manager.setCookie('https://haiagaru-auth-test.invalid/auth-code','haiagaru_dummy_auth=verification-only; Secure; HttpOnly; Path=/private/');manager.flush();
 });
 setTimeout(()=>Java.perform(function(){
  prefs.edit().putBoolean(key,false).commit();
  check(!has('https://haiagaru-auth-test.invalid/private/post'),'OFF retains native jar behavior');
  prefs.edit().putBoolean(key,true).commit();
  check(has('https://haiagaru-auth-test.invalid/private/post'),'native jar receives opted-in WebView Cookie');
  check(!has('https://other-auth-test.invalid/private/post'),'other origin never receives Cookie');
  check(!has('http://haiagaru-auth-test.invalid/private/post'),'HTTP never receives secure auth Cookie');
  check(!has('https://haiagaru-auth-test.invalid/public/'),'WebView Cookie path respected');
  const replacement=A.parseCookie(url('https://haiagaru-auth-test.invalid/private/post'),'haiagaru_dummy_auth=rotated-verification; Secure; Path=/private/');
  const reply=Java.use('java.util.ArrayList').$new();reply.add(replacement);jar[saveName](url('https://haiagaru-auth-test.invalid/private/post'),reply);
  setTimeout(()=>Java.perform(function(){
   const header=manager.getCookie('https://haiagaru-auth-test.invalid/private/post');
   check(header!==null&&String(header).includes('rotated-verification'),'native reply Cookie rotation reaches WebView');
   prefs.edit().remove(key).commit();
   const empty=Java.use('java.util.ArrayList').$new();
   check(A.cookies(empty,url('https://haiagaru-auth-test.invalid/private/post')).size()===0,'unlink stops Cookie bridge');
   const expired=Java.use('java.util.ArrayList').$new();expired.add(A.parseCookie(url('https://haiagaru-auth-test.invalid/private/post'),'haiagaru_dummy_auth=; Max-Age=0; Secure; Path=/private/'));jar[saveName](url('https://haiagaru-auth-test.invalid/private/post'),expired);
   manager.setCookie('https://haiagaru-auth-test.invalid/private/','haiagaru_dummy_auth=; Max-Age=0; Path=/private/; Secure');manager.flush();
   console.log('PASS '+checks+' native authentication Cookie checks; no post sent');
  }),600);
 }),1000);
});
