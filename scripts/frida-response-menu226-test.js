// Synthetic native catalog only. Restore settings; never post or delete user data.
Java.perform(function(){
 const app=Java.use('android.app.ActivityThread').currentApplication();
 if(String(app.getPackageName())!=='jp.co.airfront.android.a2chMate.haiagaru.responsearchive226')throw Error('Verification package only');
 const prefs=app.getSharedPreferences('resMenuDialog',0), saved=prefs.getString('toolbarSettings',null);
 let count=0;function check(ok,label){if(!ok)throw Error(label);count++;console.log('PASS '+label);}
 Java.scheduleOnMainThread(function(){try{
 const B=Java.use('o.m27'), C=Java.use('o.LyD'), R=Java.use('app.morphe.extension.chmate.ResponseMenuSettings');
 const builder=B.$new('',0,0);C.$new().invoke(builder);const list=builder.a.value;
 function ids(){return list.toArray().map(x=>Java.cast(x,Java.use('jp.syoboi.a2chMate.feature.toolbar.ToolbarButtonSpec')).c.value);}
 const custom=[0x7e484b01,0x7e484a01,0x7e484a03,75,76,0x7e000077];
 custom.forEach(id=>check(ids().filter(x=>x===id).length===1,'catalog includes '+id));
 list.add(list.get(0));R.catalog226(builder);check(new Set(ids()).size===list.size(),'repeated catalog removes duplicate IDs');
 const shares=list.toArray().map(x=>Java.cast(x,Java.use('jp.syoboi.a2chMate.feature.toolbar.ToolbarButtonSpec'))).filter(x=>x.c.value>=70&&x.c.value<=74);
 check(shares.length===5,'preserve five stock share slots');
 check(new Set(shares.map(x=>app.getString(x.e.value).toString())).size===5,'share slot labels distinguish destinations');
 const settings={buttons:[{id:35,visible:false},...custom.map(id=>({id:id,visible:true}))]};
 prefs.edit().putString('toolbarSettings',JSON.stringify(settings)).commit();
 custom.forEach(id=>check(R.visible(id),'read-marker off leaves action '+id+' on'));
 custom.forEach(id=>{settings.buttons.find(x=>x.id===id).visible=false;prefs.edit().putString('toolbarSettings',JSON.stringify(settings)).commit();check(!R.visible(id),'individual setting off '+id);});
 const settingsType=Java.use('o.IThreadPoolCallback$write');
 const native=settingsType.c(JSON.stringify(settings));const roundtrip=settingsType.c(native.d());
 check(roundtrip.a.value.size()===7,'native save and load retain all custom IDs');
 console.log('COMPLETE '+count+' response-menu226 checks');
 }catch(e){console.log('FAIL '+e.stack);}finally{const edit=prefs.edit();if(saved===null)edit.remove('toolbarSettings');else edit.putString('toolbarSettings',saved);edit.commit();}});
});
