// Native adapter + isolated SQLite fixture. No user bookmark or log is changed.
Java.perform(function () {
  Java.deoptimizeEverything();
  const app=Java.retain(Java.use('android.app.ActivityThread').currentApplication()),pkg=String(app.getPackageName());
  if(!/\.haiagaru\.(uploadhosts191|responsearchive226|responsearchive242)$/.test(pkg))throw Error('Verification app only');
  const D=Java.use('app.morphe.extension.chmate.DeleteReadThreads');
  const mid=pkg.endsWith('226'),legacy=pkg.endsWith('191');
  const File=Java.use('java.io.File'),SQLite=Java.use('android.database.sqlite.SQLiteDatabase');
  const path=Java.retain(File.$new(app.getCacheDir(),'haiagaru-read-delete-fixture-'+Date.now()+'.sqlite'));
  const pathString=String(path.getAbsolutePath());
  const Context=Java.registerClass({name:'app.morphe.test.ReadDeleteContext'+Date.now(),superClass:Java.use('android.content.ContextWrapper'),methods:{$init:[{returnType:'void',argumentTypes:['android.content.Context'],implementation:function(base){this.$super.$init(base);}}],getDatabasePath:[{returnType:'java.io.File',argumentTypes:['java.lang.String'],implementation:function(){return File.$new(pathString);}}]}});
  const context=Java.retain(Context.$new(app)),db=Java.retain(SQLite.openOrCreateDatabase.overload('java.io.File','android.database.sqlite.SQLiteDatabase$CursorFactory').call(SQLite,path,null));
  let checks=0;function check(ok,label){if(!ok)throw Error(label);checks++;console.log('PASS '+label);}
  db.execSQL('CREATE TABLE bookmarks (_id INTEGER PRIMARY KEY,name TEXT,created LONG,title TEXT,read_count INT,res_count INT,server_res_count INT)');
  for(const id of [201,202,203,204,205,206])db.execSQL('INSERT INTO bookmarks VALUES ('+id+',\'haiagaru-fixture\','+id+',\'fixture '+id+'\','+(id===202?9:id===206?0:10)+',10,10)');
  const Unsafe=Java.use('sun.misc.Unsafe'),uf=Unsafe.class.getDeclaredField('theUnsafe');uf.setAccessible(true);const unsafe=Java.cast(uf.get(null),Unsafe);
  const Model=Java.use(legacy?'o.MaxAdViewImplExternalSyntheticLambda2':mid?'o.verifyHostname':'o.zzauw');
  function row(id,total,read){const m=unsafe.allocateInstance(Model.class);for(const [n,v,isLong]of [[legacy?'e':mid?'b':'d',id,true],[legacy?'d':mid?'c':'a',total,false],[legacy?'b':mid?'e':'g',read,false]]){const f=Model.class.getDeclaredField(n);f.setAccessible(true);if(isLong)f.setLong(m,v);else f.setInt(m,v);}if(!legacy&&!mid){const w=unsafe.allocateInstance(Java.use('o.zzavl$read').class),f=w.getClass().getDeclaredField('a');f.setAccessible(true);f.set(w,m);return w;}return m;}
  Java.scheduleOnMainThread(function(){try{
    const rows=Java.use('java.util.ArrayList').$new();
    rows.add(row(201,10,0)); // Stale native read snapshot, but DB is fully read.
    rows.add(row(202,10,10)); // Previously read with one unread response: included.
    rows.add(row(203,11,11)); // Newly arrived unread response: also included.
    rows.add(row(205,0,0)); // Empty native count, valid local DB count.
    rows.add(row(206,10,0)); // Starred/patrol-only, never read: excluded by DB.
    rows.add(row(-1,10,10)); // Unopened thread.
    let adapter=Java.use('android.widget.ArrayAdapter').$new(app,17367043,rows);
    if(mid){
      const Handler=Java.registerClass({name:'app.morphe.test.ReadDeleteHandler'+Date.now(),implements:[Java.use('java.lang.reflect.InvocationHandler')],methods:{invoke:function(){return null;}}});
      const iface=Java.use('o.IPMiBroadcastReceiver1');
      const cb=Java.use('java.lang.reflect.Proxy').newProxyInstance(app.getClassLoader(),Java.array('java.lang.Class',[iface.class]),Handler.$new());
      adapter=Java.use('o.getWriteBytesTotal').$new(app,Java.cast(cb,iface));
      const visibleField=adapter.getClass().getDeclaredField('e');visibleField.setAccessible(true);visibleField.set(adapter,rows);
      const backing=Java.use('java.util.ArrayList').$new();backing.add(row(204,10,10));
      const backingField=adapter.getClass().getDeclaredField('b');backingField.setAccessible(true);backingField.set(adapter,backing);
    }
    const list=Java.use('android.widget.ListView').$new(app);list.addHeaderView(Java.use('android.view.View').$new(app));list.setAdapter(adapter);
    const found=Java.use('java.util.LinkedHashMap').$new();D.collectRows(list,found);
    const Long=Java.use('java.lang.Long');
    check(found.size()===5,'displayed local IDs collected through header wrapper');
    check(!found.containsKey(Long.valueOf(204)),'hidden backing rows excluded');
    check(!found.containsKey(Long.valueOf(-1)),'unopened rows excluded');
    const candidates=D.readCandidates(context,found);
    check(candidates.size()===4,'database includes all previously read threads, including unread');
    const Candidate=Java.use('app.morphe.extension.chmate.DeleteReadThreads$Candidate');
    const first=Java.cast(candidates.get(0),Candidate),last=Java.cast(candidates.get(3),Candidate);
    check(Number(first.id.value)===201,'stale native read count no longer hides fully-read thread');
    check(Number(last.id.value)===205,'database count handles empty native snapshot');
    check(D.stillRead(context,first),'read DB state revalidated');
    const partial=Java.cast(candidates.get(1),Candidate);
    check(Number(partial.id.value)===202&&partial.unread.value===1,'partially read thread included with unread count');
    check(Number(Java.cast(candidates.get(2),Candidate).id.value)===203,'new native unread response does not hide visited thread');
    db.execSQL('UPDATE bookmarks SET read_count=9 WHERE _id=201');
    check(D.stillRead(context,first),'remaining unread does not block requested deletion');
    db.execSQL('UPDATE bookmarks SET read_count=10,server_res_count=11 WHERE _id=201');
    check(D.stillRead(context,first),'new server response does not block requested deletion');
    db.execSQL('UPDATE bookmarks SET read_count=0 WHERE _id=201');
    check(!D.stillRead(context,first),'reset reading history blocks deletion');
    db.execSQL('UPDATE bookmarks SET server_res_count=10,name=\'other-board\' WHERE _id=201');
    db.execSQL('UPDATE bookmarks SET read_count=10 WHERE _id=201');
    check(!D.stillRead(context,first),'changed board identity blocks deletion');
    console.log('COMPLETE '+checks+' native-adapter/database checks '+pkg);
  }catch(e){console.log('FAIL '+e.stack);}finally{db.close();SQLite.deleteDatabase(File.$new(pathString));}});
});
