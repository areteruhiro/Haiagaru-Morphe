Java.perform(function () {
    const app = Java.use('android.app.ActivityThread').currentApplication();
    if (String(app.getPackageName()) !== 'jp.co.airfront.android.a2chMate.haiagaru.patrol242') throw Error('Verification app only');
    const SQL = Java.use('android.database.sqlite.SQLiteDatabase');
    const path = app.getDatabasePath('roidon.sqlite').getAbsolutePath();
    const db = SQL.openDatabase.overload('java.lang.String','android.database.sqlite.SQLiteDatabase$CursorFactory','int').call(SQL, path, null, 1);
    const rows = db.rawQuery("SELECT name,sql FROM sqlite_master WHERE type='table'", null);
    while (rows.moveToNext()) console.log('TABLE ' + rows.getString(0) + ' ' + rows.getString(1));
    rows.close(); db.close();
    for (const name of ['o.TTRewardVideoActivity8','jp.syoboi.a2chMate.autocheck.AutoCheckWork','jp.syoboi.a2chMate.prefs.Prefs']) {
        const C = Java.use(name);
        console.log('CLASS ' + name);
        for (const m of C.class.getDeclaredMethods()) console.log(String(m));
    }
});
