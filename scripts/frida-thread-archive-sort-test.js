Java.perform(function(){
 const app=Java.use('android.app.ActivityThread').currentApplication(),pkg=String(app.getPackageName());
 if(!/\.haiagaru\.(uploadhosts191|responsearchive226|responsearchive242)$/.test(pkg))throw Error('Verification package only');
 const legacy=pkg.endsWith('191'),mid=pkg.endsWith('226');
 const Item=Java.use(legacy?'o.r8lambda9_COK73qqUUFHkYp0uhRYVshnk':mid?'o.AndroidLogKt':'o.GNk16');
 const File=Java.use('java.io.File'), Sort=Java.use('app.morphe.extension.chmate.ThreadArchiveOrder');
 const p=app.getSharedPreferences('haiagaru.thread-archive-order',0),had=p.contains('sort'),old=p.getInt('sort',0);
 let checks=0;function check(ok,label){if(!ok)throw Error(label);checks++;console.log('PASS '+label);}
 try{
  function item(name){return Item.$new(File.$new('/nonexistent/'+name),null);}
  const a=item('a[1800000000]'),z=item('z[1700000000]'),unknown=item('board');
  function nativeCompare(x,y){return legacy?Java.use('o.m6d$2').$new(Java.use('o.m6d').$new(app)).compare(x,y):Java.use('o.setAdCreativeClickListener').b(x,y);}
  p.edit().putInt('sort',0).commit();check(!Sort.enabled(),'standard preserved');
  for(const mode of [1,2,3,4]){
   p.edit().putInt('sort',mode).commit();check(Sort.enabled(),'enabled '+mode);
   const list=Java.use('java.util.ArrayList').$new();list.add(unknown);list.add(z);list.add(a);Sort.sort(list);
   check(list.size()===3,'all entries retained '+mode);
   if(mode<=2)check(String(list.get(2))===String(unknown),'unknown last '+mode);
   if(mode<=2){check((Sort.compare(a,z)<0)===(mode===1),'creation order '+mode);
    if(!mid)check((nativeCompare(a,z)<0)===(mode===1),'native comparator '+mode);
   }
  }
  console.log('PASS '+checks+' Android archive sort checks '+pkg);
 }finally{const edit=p.edit();if(had)edit.putInt('sort',old);else edit.remove('sort');edit.commit();}
});
