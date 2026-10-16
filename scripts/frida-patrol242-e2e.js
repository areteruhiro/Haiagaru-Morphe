/* Native periodic WorkManager -> bookmark/subject/DAT -> native image cache.
 * Does not call PatrolImages.onThread or fabricate DAT return objects. */
Java.perform(function () {
    const app = Java.use('android.app.ActivityThread').currentApplication();
    if (String(app.getPackageName()) !== 'jp.co.airfront.android.a2chMate.haiagaru.patrol242') throw Error('Verification app only');
    const P = Java.use('app.morphe.extension.chmate.PatrolImages');
    const H = Java.use('app.morphe.extension.chmate.Haiagaru');
    H.shouldHideAds(app); P.initialize(app);
    // Loopback fixture uses HTTP, not a public server. Keep production HTTPS
    // policy intact and exempt only the in-process test endpoint.
    const transport = Java.use('app.morphe.extension.chmate.HttpsTransport');
    const local = transport.isLocalThumbnailEndpoint.overload('java.lang.String','int');
    local.implementation = function(host,port) {
        return (String(host)==='127.0.0.1' && port===18769) || local.call(this,host,port);
    };
    const builder = Java.use('o.zzcvl$IconCompatParcelizer');
    const build = builder.b.overload();
    build.implementation = function() {
        if (String(this.h.value)==='127.0.0.1') this.j.value='http';
        return build.call(this);
    };
    const method = P.onThread.overload('java.lang.Object','java.lang.Object');
    method.implementation = function (result, board) {
        console.log('NATIVE_PATROL_CALLBACK board=' + board);
        method.call(this, result, board);
        console.log('NATIVE_PATROL_STATUS ' + P.status.value);
    };
    // Observe Worker completion using WM-WorkerWrapper logcat. Hooking the
    // inherited/bridge doWork dispatch can recurse on this emulator's ART.
    const check = Java.use('jp.syoboi.a2chMate.autocheck.AutoCheckWork').c.overload('o.getBackImage','java.util.List','boolean');
    check.implementation = function(client, bookmarks, download) {
        console.log('NATIVE_CHECK bookmarks=' + bookmarks + ' download=' + download);
        const result = check.call(this,client,bookmarks,download);
        console.log('NATIVE_CHECK_RESULT ' + result);
        return result;
    };
    const SQL = Java.use('android.database.sqlite.SQLiteDatabase');
    const db = SQL.openDatabase.overload('java.lang.String','android.database.sqlite.SQLiteDatabase$CursorFactory','int')
        .call(SQL, app.getDatabasePath('roidon.sqlite').getAbsolutePath(), null, 0);
    db.execSQL("INSERT OR REPLACE INTO boards(name,server,title,category,is_external,flag) VALUES('patrolfixture','127.0.0.1:18769','巡回検証専用','検証',1,0)");
    db.execSQL("INSERT OR REPLACE INTO bookmarks(name,created,title,res_count,server_res_count,read_count,flag,state,access) VALUES('patrolfixture',1792176000,'巡回通しテスト',0,0,0,1,0,1792176000000)");
    const rows = db.rawQuery("SELECT name,server FROM boards WHERE name='patrolfixture'", null);
    while (rows.moveToNext()) console.log('FIXTURE_BOARD ' + rows.getString(0) + ' ' + rows.getString(1));
    rows.close(); db.close();
    const prefs = app.getSharedPreferences('io.github.areteruhiro.chmate.haiagaru.ui-config',0);
    prefs.edit().putBoolean('patrolImagesEnabled',true).putBoolean('patrolImageFilterEnabled',true)
        .putString('patrolImageKeywords','巡回通しテスト').putString('patrolImageUrls','').putInt('patrolImageDestination',0).commit();
    const Prefs = Java.use('jp.syoboi.a2chMate.prefs.Prefs');
    Prefs.a.overload('o.ApiAnyClient$write','boolean').call(Prefs, Prefs.C(), true);
    Prefs.d.overload('o.ApiAnyClient$RemoteActionCompatParcelizer','int').call(Prefs, Prefs.as(), 15);
    Java.use('o.TTRewardVideoActivity8').e(app);
    console.log('PERIODIC_SCHEDULE_REGISTERED interval=15min checkWithDownload=true');
    setInterval(() => Java.perform(function () {
        console.log('STATUS ' + P.status.value);
        const bindings = String(P.bindings()).split(';');
        try {
            const manager = Java.use(bindings[2])[bindings[3]](app);
            const stream = manager[bindings[5]]('http://127.0.0.1:18769/patrol-e2e.png');
            console.log('E2E_CACHE firstByte=' + stream.read()); stream.close();
        } catch (_) {}
    }), 5000);
});
