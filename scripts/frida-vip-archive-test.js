// Existing isolated 242 app only. Read-only URL/identity/HTTP tests; never post.
Java.perform(function(){
 const app=Java.use('android.app.ActivityThread').currentApplication();
 if(String(app.getPackageName())!=='jp.co.airfront.android.a2chMate.haiagaru.responsearchive242')throw Error('Test 242 package required');
 const Runnable=Java.registerClass({name:'app.morphe.test.VipArchiveCheck'+Date.now(),implements:[Java.use('java.lang.Runnable')],methods:{run:function(){try{
  let n=0;function check(ok,label){if(!ok)throw Error(label);n++;console.log('PASS '+label);}
  const Info=Java.use('app.morphe.extension.chmate.ArchivedThreadImporter$ThreadInfo');
  const info=Info.parse('http://ex14.vip2ch.com/test/read.cgi/news4ssnip/1464424549/');
  check(info!==null&&info.vip.value&&!info.talk.value,'VIP thread classified independently of Talk/5ch');
  const board=String(info.cacheBoardId());
  check(board.length>0&&board.indexOf('/')<0,'native cache board identity resolved');
  console.log('CACHE BOARD '+board);
  const R=Java.use('app.morphe.extension.chmate.ArchivedThreadImporter$ArchiveRoute');
  const Format=Java.use('app.morphe.extension.chmate.ArchivedThreadImporter$RouteFormat');
  const route=R.$new(Format.valueOf('DAT'),'https://{$host}/{$bbs}/kako/{$key:0:4}/{$key:0:5}/{$key}.dat');
  check(String(route.resolve(info))==='https://ex14.vip2ch.com/news4ssnip/kako/1464/14644/1464424549.dat','actual importer route expands correctly');
  const I=Java.use('app.morphe.extension.chmate.ArchivedThreadImporter');
  const bytes=I.fetchArchivedDat(null,info);
  const valid=I.validateDat(bytes);
  check(valid.length===28468,'Android HTTPS returns expected validated DAT');
  const text=String(Java.use('java.lang.String').$new(valid,'MS932'));
  check(text.trim().split('\n').length===49,'Android DAT contains 49 response lines');
  console.log('PASS '+n+' Android VIP archive checks');
 }catch(error){console.log('FAIL '+error.stack);}}}});
 Java.use('java.lang.Thread').$new(Runnable.$new()).start();
});
