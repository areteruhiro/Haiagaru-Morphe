// Read-only board registration diagnostics in the fixed verification packages.
Java.perform(function () {
 const app=Java.use('android.app.ActivityThread').currentApplication();
 if(!/\.haiagaru\.(uploadhosts191|responsearchive226|responsearchive242)$/.test(String(app.getPackageName())))throw Error('Verification package only');
 const DB=Java.use('android.database.sqlite.SQLiteDatabase');
 const db=DB.openDatabase.overload('java.lang.String','android.database.sqlite.SQLiteDatabase$CursorFactory','int').call(DB,String(app.getDatabasePath('roidon.sqlite').getPath()),null,1);
 try {
  for(const sql of ['SELECT is_external,count(*) FROM boards GROUP BY is_external','SELECT name,server,category,is_external FROM boards WHERE is_external<>0 OR name LIKE "%2F%" LIMIT 12']) {
   const c=db.rawQuery(sql,null);
   try{while(c.moveToNext()){let row=[];for(let i=0;i<c.getColumnCount();i++)row.push(c.getColumnName(i)+'='+c.getString(i));console.log(row.join(' '));}}finally{c.close();}
  }
 }finally{db.close();}
});
