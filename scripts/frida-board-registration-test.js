// An in-memory database only; never modify the user's registered boards.
Java.perform(function () {
 const app=Java.use('android.app.ActivityThread').currentApplication();
 if(!/\.haiagaru\.(uploadhosts191|responsearchive226|responsearchive242)$/.test(String(app.getPackageName())))throw Error('Verification package only');
 const loader=Java.use('dalvik.system.DelegateLastClassLoader').$new('/data/local/tmp/board-registration-fix.dex',app.getClassLoader());
 const factory=Java.ClassFactory.get(loader);
 const Activity=factory.use('app.morphe.extension.chmate.ExternalBoardEditorActivity');
 const Unsafe=Java.use('sun.misc.Unsafe');const uf=Unsafe.class.getDeclaredField('theUnsafe');uf.setAccessible(true);
 const object=Java.cast(uf.get(null),Unsafe).allocateInstance(Activity.class);
 const editor=Java.cast(object,Activity);
 const DB=Java.use('android.database.sqlite.SQLiteDatabase');const db=DB.create.overload('android.database.sqlite.SQLiteDatabase$CursorFactory').call(DB,null);
 let count=0;function check(ok,label){if(!ok)throw Error(label);count++;console.log('PASS '+label);}
 try {
  db.execSQL('CREATE TABLE boards (_id INTEGER PRIMARY KEY,name TEXT,title TEXT,server TEXT,is_external INTEGER)');
  db.execSQL("INSERT INTO boards VALUES (1,'bbs.eddibb.cc%2Fliveedge','Edge imported','bbs.eddibb.cc',0),(2,'android','Android','egg.5ch.io',0),(3,'custom.invalid%2Ftest','Flagged external','custom.invalid',1)");
  const external=editor.boards(db,false),all=editor.boards(db,true);
  check(external.size()===2,'unflagged imported board and flagged board both appear');
  check(all.size()===3,'BBSMENU switch exposes all registered boards');
  const Board=factory.use('app.morphe.extension.chmate.ExternalBoardEditorActivity$Board');
  let found=null;for(let i=0;i<external.size();i++){const row=Java.cast(external.get(i),Board);if(String(row.id.value)==='bbs.eddibb.cc%2Fliveedge')found=row;}
  check(found!==null,'imported Edge keeps native identity');
  check(String(found.original.value)==='https://bbs.eddibb.cc/liveedge/','imported Edge URL reconstructed');
  const values=Java.use('android.content.ContentValues').$new();values.put.overload('java.lang.String','java.lang.String').call(values,'title','Renamed Edge');
  check(db.update('boards',values,'_id=? AND name=?',Java.array('java.lang.String',['1','bbs.eddibb.cc%2Fliveedge']))===1,'zero-flag board name can be updated');
  check(db.update('boards',values,'_id=? AND name=?',Java.array('java.lang.String',['1','other-board']))===0,'identity mismatch cannot be updated');
  const c=db.rawQuery('SELECT name,is_external FROM boards WHERE _id=1',null);try{c.moveToFirst();check(String(c.getString(0))==='bbs.eddibb.cc%2Fliveedge'&&c.getInt(1)===0,'native identity and flag unchanged');}finally{c.close();}
  console.log('COMPLETE '+count+' board registration checks on '+app.getPackageName());
 }finally{db.close();}
});
