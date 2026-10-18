// Synthetic catalog checks. Temporarily change and restore verification-app preferences.
Java.perform(function(){
 const app=Java.use('android.app.ActivityThread').currentApplication(),pkg=app.getPackageName().toString();
 if(!/\.haiagaru\.(uploadhosts191|responsearchive242)$/.test(pkg))throw Error('Verification app only');
 Java.scheduleOnMainThread(function(){try{
 const old=pkg.endsWith('191');
 let catalog;
 if(old) catalog=Java.use('o.r8lambdafLXKIgI8H4VR9SponZBKnK7_9gE').a.overload().call(Java.use('o.r8lambdafLXKIgI8H4VR9SponZBKnK7_9gE'));
 else {
 const owner=Java.use('jp.syoboi.a2chMate.fragment.ResMenuDialogFragment').class;
 for(const cls of [owner,...owner.getDeclaredClasses()])for(const method of cls.getDeclaredMethods())if(method.getParameterTypes().length===0 && method.getReturnType().getName().toString()==='jp.syoboi.a2chMate.feature.toolbar.ToolbarDefault' && (method.getModifiers()&8)!==0){method.setAccessible(true);catalog=method.invoke(null,Java.array('java.lang.Object',[]));break;}
 if(!catalog)throw Error('Native catalog method missing');
 }
 const fields=catalog.getClass().getDeclaredFields();
 let list=null;for(const f of fields){if(f.getType().getName().toString().includes('List')||f.getType().getName().toString().includes('ArrayList')){f.setAccessible(true);list=Java.cast(f.get(catalog),Java.use('java.util.List'));}}
 if(!list && old) list=Java.cast(catalog,Java.use('java.util.List'));
 if(!list)throw Error('List missing');
 console.log('CATALOG '+pkg+' size='+list.size());
 for(const obj of list.toArray()){
  let parts=[];for(const f of obj.getClass().getDeclaredFields()){if(f.getType().getName().toString()==='int'){f.setAccessible(true);const n=f.getInt(obj);let label='';try{label=app.getString(n).toString();}catch(e){}parts.push(f.getName()+'='+n+(label?':'+label:''));}}
  console.log('ROW '+parts.join(' '));
 }
 const R=Java.use('app.morphe.extension.chmate.ResponseMenuSettings');
 const prefs=app.getSharedPreferences('resMenuDialog',0), saved=prefs.getString('toolbarSettings',null);
 let checks=0;function check(ok,label){if(!ok)throw Error(label);checks++;console.log('PASS '+label);}
 function key(obj){const f=obj.getClass().getDeclaredField(old?'c':'a');f.setAccessible(true);return f.getInt(obj);}
 const ids=[0x7e484b01,0x7e484a01,0x7e484a03,75,76,0x7e000077];
 ids.forEach(id=>check(list.toArray().filter(x=>key(x)===id).length===1,'catalog contains '+id));
 list.add(list.get(0));R.catalog(catalog);check(new Set(list.toArray().map(key)).size===list.size(),'dedup and repeated catalog');
 check(list.toArray().filter(x=>key(x)>=70&&key(x)<=74).length===5,'five share slots retained');
 try {
 const data={buttons:[{id:35,visible:false},...ids.map(id=>({id,visible:true}))]};
 prefs.edit().putString('toolbarSettings',JSON.stringify(data)).commit();
 ids.forEach(id=>check(R.visible(id),'read marker off independent '+id));
 ids.forEach(id=>{data.buttons.find(x=>x.id===id).visible=false;prefs.edit().putString('toolbarSettings',JSON.stringify(data)).commit();check(!R.visible(id),'individual off '+id);});
 } finally {const editor=prefs.edit();if(saved===null)editor.remove('toolbarSettings');else editor.putString('toolbarSettings',saved);editor.commit();}
 console.log('COMPLETE '+checks+' runtime checks');
 }catch(e){console.log('FAIL '+e.stack);}});
});
