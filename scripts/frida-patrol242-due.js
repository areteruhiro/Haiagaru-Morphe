// Verification package only: make the registered periodic WorkSpec due now.
Java.perform(function () {
    const app = Java.use('android.app.ActivityThread').currentApplication();
    if (String(app.getPackageName()) !== 'jp.co.airfront.android.a2chMate.haiagaru.patrol242') throw Error('Verification app only');
    const SQL = Java.use('android.database.sqlite.SQLiteDatabase');
    const path = app.getNoBackupFilesDir().getAbsolutePath() + '/androidx.work.workdb';
    const db = SQL.openDatabase.overload('java.lang.String','android.database.sqlite.SQLiteDatabase$CursorFactory','int').call(SQL,path,null,0);
    const rows = db.rawQuery('SELECT id,worker_class_name,state,last_enqueue_time,initial_delay FROM workspec',null);
    while(rows.moveToNext()) console.log('WORKSPEC ' + rows.getString(0) + ' ' + rows.getString(1) + ' state=' + rows.getInt(2) + ' enqueue=' + rows.getLong(3) + ' delay=' + rows.getLong(4));
    rows.close();
    db.execSQL("UPDATE workspec SET last_enqueue_time=last_enqueue_time-3600000 WHERE worker_class_name='jp.syoboi.a2chMate.autocheck.PeriodicAutoCheckWork' AND state=0");
    db.close(); console.log('PERIODIC_DUE_NOW');
    const native = SQL.openDatabase.overload('java.lang.String','android.database.sqlite.SQLiteDatabase$CursorFactory','int').call(SQL,app.getDatabasePath('roidon.sqlite').getAbsolutePath(),null,0);
    const saved=native.rawQuery("SELECT name,res_count,server_res_count,state,flag FROM bookmarks WHERE name='patrolfixture'",null);
    while(saved.moveToNext()) console.log('BOOKMARK '+saved.getString(0)+' res='+saved.getInt(1)+' server='+saved.getInt(2)+' state='+saved.getInt(3)+' flag='+saved.getInt(4));
    saved.close(); native.close();
});
