Java.perform(function(){
    const app=Java.use('android.app.ActivityThread').currentApplication();
    const pkg=String(app.getPackageName());
    if(!/\.haiagaru\.responsearchive(191|242)$/.test(pkg))throw Error('Verification app required');
    const SQL=Java.use('android.database.sqlite.SQLiteDatabase');
    const path=String(app.getNoBackupFilesDir().getAbsolutePath())+'/androidx.work.workdb';
    const db=SQL.openDatabase.overload('java.lang.String','android.database.sqlite.SQLiteDatabase$CursorFactory','int').call(SQL,path,null,1);
    try{
        const cursor=db.rawQuery("SELECT id,worker_class_name,state,interval_duration,flex_duration,initial_delay,last_enqueue_time,period_count FROM workspec WHERE worker_class_name LIKE 'jp.syoboi.a2chMate.autocheck.%' ORDER BY last_enqueue_time DESC",null);
        try{while(cursor.moveToNext()){
            const row={package:pkg,now:Date.now(),id:String(cursor.getString(0)),worker:String(cursor.getString(1)),state:cursor.getInt(2),interval:Number(cursor.getLong(3)),flex:Number(cursor.getLong(4)),delay:Number(cursor.getLong(5)),enqueued:Number(cursor.getLong(6)),periodCount:cursor.getInt(7)};
            console.log('NATIVE_PATROL_WORKSPEC '+JSON.stringify(row));
        }}finally{cursor.close();}
    }finally{db.close();}
});
