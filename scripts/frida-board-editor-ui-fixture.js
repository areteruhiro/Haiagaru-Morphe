Java.perform(function(){
 const app=Java.use('android.app.ActivityThread').currentApplication();
 if(String(app.getPackageName())!=='jp.co.airfront.android.a2chMate.haiagaru.boardedit191')throw Error('Isolated 191 verification package only');
 const DB=Java.use('android.database.sqlite.SQLiteDatabase');
 const db=DB.openDatabase.overload('java.lang.String','android.database.sqlite.SQLiteDatabase$CursorFactory','int').call(DB,String(app.getDatabasePath('roidon.sqlite').getPath()),null,0);
 db.execSQL("INSERT OR IGNORE INTO boards(name,server,title,category,is_external) VALUES ('haiagaru-original.invalid%2Ftestboard','haiagaru-original.invalid','編集テスト板','Haiagaru verification',1)");
 db.execSQL("INSERT OR IGNORE INTO bookmarks(name,created,title) VALUES ('haiagaru-original.invalid%2Ftestboard',1234567890,'編集テストスレ')");
 db.close();console.log('PASS isolated board/bookmark fixtures created');
});
