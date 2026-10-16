Java.perform(function(){
 const app=Java.use('android.app.ActivityThread').currentApplication();
 if(String(app.getPackageName())!=='jp.co.airfront.android.a2chMate.haiagaru.patrol242') throw Error('Verification app only');
 const SQL=Java.use('android.database.sqlite.SQLiteDatabase');
 const db=SQL.openDatabase.overload('java.lang.String','android.database.sqlite.SQLiteDatabase$CursorFactory','int').call(SQL,app.getDatabasePath('roidon.sqlite').getAbsolutePath(),null,0);
 db.execSQL("UPDATE bookmarks SET state=state|2,server_res_count=2 WHERE name='patrolfixture'");
 db.close();console.log('NATIVE_DAT_DOWNLOAD_MARKED');
});
