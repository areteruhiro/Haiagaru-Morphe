/* Isolated packages; synthetic addresses only. No network requests or posting. */
Java.perform(function(){
 const app=Java.use('android.app.ActivityThread').currentApplication();
 const pkg=String(app.getPackageName());
 if(!/\.haiagaru\.boardedit(191|226|242)$/.test(pkg))throw Error('Verification package only');
 const legacy=pkg.endsWith('191');
 const E=Java.use('app.morphe.extension.chmate.ExternalBoardEditor');
 const B=Java.use('jp.syoboi.a2chMate.client.'+(legacy?'BBSUrlInfo$BoardID':'BoardID'));
 const I=Java.use('jp.syoboi.a2chMate.client.BBSUrlInfo');
 const P=Java.use('android.os.Parcel');
 const prefs=app.getSharedPreferences('haiagaru.external-board-addresses',0);
 const id='haiagaru-original.invalid%2Ftestboard';
 const original='https://haiagaru-original.invalid/testboard/';
 const target='https://haiagaru-destination.invalid/newboard/';
 let checks=0;
 function check(ok,label){if(!ok)throw Error('FAIL '+label);checks++;console.log('PASS '+label);}
 function reset(){const f=E.class.getDeclaredField('initialized');f.setAccessible(true);f.setBoolean(null,false);E.initialize(app);}
 function field(o,n){const f=o.getClass().getDeclaredField(n);f.setAccessible(true);return String(f.get(o));}
 function board(server,path,key){return B.$new.overload('java.lang.String','java.lang.String','java.lang.String','java.lang.String').call(B,server,path,key,'http://'+server+'/'+path+'/');}
 const bf=String(E.boardFields()).split(',');const inf=String(E.infoFields()).split(',');
 check(bf.length===4&&inf.length===2,'exact native field bindings');
 try{
  prefs.edit().putString('original:'+id,original).putString('target:'+id,target).commit();reset();
  const b=board('haiagaru-original.invalid','testboard',id);
  check(String(b.toString())===id,'old native board identity retained');
  check(field(b,bf[0])==='haiagaru-destination.invalid'&&field(b,bf[1])==='newboard','old identity resolves relocated host/path');
  check(field(b,bf[3])===target,'canonical URL relocated');
  const newId='haiagaru-destination.invalid%2Fnewboard';
  const b2=board('haiagaru-destination.invalid','newboard',newId);
  check(String(b2.toString())===id,'incoming new URL maps to old identity');
  check(b.equals(b2)&&b.hashCode()===b2.hashCode(),'native identity equality and hash preserved');
  const parcel=P.obtain();b.writeToParcel(parcel,0);parcel.setDataPosition(0);
  const creator=Java.cast(B.CREATOR.value,Java.use('android.os.Parcelable$Creator'));
  const restored=Java.cast(creator.createFromParcel(parcel),B);parcel.recycle();
  check(String(restored.toString())===id&&field(restored,bf[1])==='newboard','parcel roundtrip preserves relocated board');
  for(const host of ['haiagaru-original.invalid','haiagaru-destination.invalid']){
   const path=host.includes('original')?'testboard':'newboard';
   const info=I.$new.overload('java.lang.String','java.lang.String','java.lang.Long','int','java.lang.Integer','java.lang.String').call(I,
    host,path,Java.use('java.lang.Long').valueOf(1234567890),0,null,null);
   check(field(info,inf[0])==='haiagaru-destination.invalid'&&field(info,inf[1])==='newboard','native URL model remaps '+host);
   let urls=0;
   for(const method of I.class.getDeclaredMethods()){
    if(String(method.getName()).startsWith('haiagaruBoardAddress_'))continue;
    if(method.getParameterTypes().length!==0||String(method.getReturnType().getName())!=='java.lang.String'||(method.getModifiers()&8)!==0)continue;
    method.setAccessible(true);
    let s;try{s=String(method.invoke(info,Java.array('java.lang.Object',[])));}catch(e){continue;}
    if(!/^https?:\/\//.test(s))continue;
    if(!s.includes('haiagaru-'))continue;
    check(s.startsWith('https://haiagaru-destination.invalid/')&&!s.includes('/testboard/'),'native URL output '+String(method.getName()));urls++;
   }
   check(urls>0,'native transport URL getters exercised');
  }
  check(String(E.rewriteUrl(original+'dat/1234567890.dat'))===target+'dat/1234567890.dat','DAT path and scheme remap');
  check(String(E.rewriteUrl('http://haiagaru-original.invalid/test/read.cgi/testboard/1234567890/2'))==='https://haiagaru-destination.invalid/test/read.cgi/newboard/1234567890/2','thread links remap');
  check(String(E.rewriteUrl('https://unrelated.invalid/testboard/subject.txt'))==='https://unrelated.invalid/testboard/subject.txt','unrelated board unchanged');
  const ordinary=board('unrelated.invalid','other','unchanged');
  check(String(ordinary.toString())==='unchanged'&&field(ordinary,bf[1])==='other','unrelated native identity unchanged');
  // Save does not switch an active process to a different route mid-request.
  prefs.edit().putString('target:'+id,'https://third.invalid/third/').commit();
  check(field(board('haiagaru-original.invalid','testboard',id),bf[0])==='haiagaru-destination.invalid','routing snapshot stable until restart');
  console.log('PASS '+checks+' external board relocation checks');
 }finally{prefs.edit().remove('target:'+id).remove('original:'+id).commit();reset();}
});
