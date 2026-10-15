// Synthetic fixture ONLY. Never attach this to a user's installed package.
Java.perform(function () {
    const app = Java.use('android.app.ActivityThread').currentApplication();
    const packageName = 'jp.co.airfront.android.a2chMate.haiagaru.restore191';
    if (app.getPackageName().toString() !== packageName) throw new Error('Isolated restore test package required');
    const File = Java.use('java.io.File');
    const String = Java.use('java.lang.String');
    const DB = Java.use('android.database.sqlite.SQLiteDatabase');
    const FOS = Java.use('java.io.FileOutputStream');
    const FIS = Java.use('java.io.FileInputStream');
    const Zip = Java.use('java.util.zip.ZipOutputStream');
    const Entry = Java.use('java.util.zip.ZipEntry');
    const helper = Java.use('app.morphe.extension.chmate.NativeRestore191');
    const root = File.$new(app.getCacheDir(), 'nativeRestoreFixture');
    root.mkdirs();
    const database = File.$new(root, 'fixture.sqlite');
    const zipFile = File.$new(root, 'fixture.zip');
    const buffer = Java.array('byte', Array(65536).fill(0));
    function check(condition, message) { if (!condition) throw new Error(message); }
    function field(object, name) {
        const f = object.getClass().getDeclaredField(name);
        f.setAccessible(true);
        return f.get(object);
    }
    function findFragment(callback) {
        let found = false;
        Java.choose('o.setFormat', {
            onMatch: function (f) { if (f.getActivity() !== null) { found = true; callback(f); return 'stop'; } },
            onComplete: function () { check(found, 'No live restore fragment'); }
        });
    }
    globalThis.openRestoreFixture = function () { Java.perform(function () {
        check(!database.exists(), 'Fixture already created; use a fresh isolated package');
        const db = DB.openDatabase(database.getPath(), null, 0x10000000);
        db.execSQL("CREATE TABLE boards (_id INTEGER PRIMARY KEY, board_order INTEGER DEFAULT 0, name TEXT NOT NULL, server TEXT NOT NULL, title TEXT NOT NULL, category TEXT NOT NULL, is_external INTEGER DEFAULT 0, flag INTEGER DEFAULT 0, updated INTEGER DEFAULT 0, access INTEGER DEFAULT 0, extra2 TEXT DEFAULT '')");
        db.execSQL("CREATE TABLE bookmarks (_id INTEGER PRIMARY KEY, bookmark_order INTEGER DEFAULT 0, name TEXT NOT NULL, created LONG NOT NULL, title TEXT NOT NULL, res_count INTEGER DEFAULT 0, server_res_count INTEGER DEFAULT 0, read_count INTEGER DEFAULT 0, read_state INTEGER DEFAULT 0, updated LONG DEFAULT 0, access LONG DEFAULT 0, flag INTEGER DEFAULT 0, state INTEGER DEFAULT 0, posted LONG DEFAULT 0)");
        db.execSQL("INSERT INTO boards (_id,name,server,title,category) VALUES (1,'android','egg.5ch.io','復元検証','検証')");
        db.execSQL("INSERT INTO bookmarks (_id,name,created,title,res_count) VALUES (1,'android',1790351854,'復元テストスレッド',1)");
        db.setVersion(19);
        db.close();
        const zip = Zip.$new(FOS.$new(zipFile));
        zip.putNextEntry(Entry.$new('databases/roidon.sqlite'));
        const input = FIS.$new(database);
        let count;
        while ((count = input.read(buffer)) !== -1) zip.write(buffer, 0, count);
        input.close(); zip.closeEntry();
        const data = String.$new('fixture<>mail<>2026/10/15 ID:fixture<> restore test<>fixture\n').getBytes('UTF-8');
        for (let i = 0; i < 1000; i++) {
            zip.putNextEntry(Entry.$new('dat/android/' + (1790351854 + i) + '.dat'));
            zip.write(data, 0, data.length); zip.closeEntry();
        }
        zip.close();
        Java.scheduleOnMainThread(function () {
            const intent = Java.use('android.content.Intent').$new();
            intent.setClassName(packageName, 'jp.syoboi.a2chMate.activity.BackupActivity');
            intent.setData(Java.use('android.net.Uri').fromFile(zipFile));
            intent.addFlags(0x10000000);
            app.startActivity(intent);
            console.log('Opened synthetic native ZIP restore: 1000 files');
        });
    }); };
    globalThis.reopenRestoreFixture = function () { Java.perform(function () {
        check(zipFile.isFile(), 'Synthetic ZIP is missing');
        app.getSharedPreferences('boot_prefs', 0).edit().remove('rdp').remove('haiagaru_native_restore_error').commit();
        Java.scheduleOnMainThread(function () {
            const intent = Java.use('android.content.Intent').$new();
            intent.setClassName(packageName, 'jp.syoboi.a2chMate.activity.BackupActivity');
            intent.setData(Java.use('android.net.Uri').fromFile(zipFile)); intent.addFlags(0x10000000);
            app.startActivity(intent);
        });
    }); };
    globalThis.markPreparedFixture = function () { Java.perform(function () {
        findFragment(function (fragment) {
            check(field(fragment, 'd') !== null, 'Preview not ready');
            check(fragment.getRetainInstance(), 'Fragment not retained');
            const directory = Java.cast(field(fragment, 'h'), File);
            check(directory.getCanonicalPath().toString().startsWith(app.getNoBackupFilesDir().getCanonicalPath().toString() + '/'), 'Expanded ZIP still in cache');
            const marker = File.$new(directory, 'resume-sentinel');
            check(marker.createNewFile(), 'Marker already present');
            console.log('PASS native preview parsed; retained fragment; sentinel created');
        });
    }); };
    globalThis.verifyResumedFixture = function () { Java.perform(function () {
        findFragment(function (fragment) {
            const marker = File.$new(Java.cast(field(fragment, 'h'), File), 'resume-sentinel');
            check(marker.exists(), 'Resume deleted prepared restoreTmp');
            check(field(fragment, 'd') !== null, 'Preview database lost');
            console.log('PASS resume reused prepared extraction without deleting it');
        });
    }); };
    globalThis.restoreFixture = function () { Java.perform(function () {
        findFragment(function (fragment) {
            check(field(fragment, 'd') !== null, 'Preview not ready');
            const thread = Java.use('o.setFormat$2').$new(fragment);
            const f = fragment.getClass().getDeclaredField('c'); f.setAccessible(true); f.set(fragment, thread);
            thread.start();
            console.log('Started ORIGINAL patched native restore worker; process should restart');
        });
    }); };
    globalThis.seedFailedInstall = function () { Java.perform(function () {
        const broken = File.$new(app.getNoBackupFilesDir(), 'restoreTmp/databases/roidon.sqlite');
        broken.getParentFile().mkdirs();
        const output = FOS.$new(broken); output.write(Java.array('byte', [1, 2, 3])); output.close();
        check(app.getSharedPreferences('boot_prefs', 0).edit().putString('rdp', broken.getPath()).commit(), 'seed pending restore');
        console.log('Seeded invalid synthetic pending DB; restart isolated app to verify preservation');
    }); };
    globalThis.verifySuccessfulInstall = function () { Java.perform(function () {
        const db = DB.openDatabase(app.getDatabasePath('roidon.sqlite').getPath(), null, 1);
        const cursor = db.rawQuery('SELECT title FROM bookmarks WHERE created=1790351854', null);
        check(cursor.moveToFirst() && cursor.getString(0).toString() === '復元テストスレッド', 'DB not installed');
        check(app.getSharedPreferences('boot_prefs', 0).getString('rdp', null) === null, 'Pending not cleared');
        check(!File.$new(app.getNoBackupFilesDir(), 'restoreTmp').exists(), 'Successful restore temp not cleaned');
        cursor.close(); db.close();
        console.log('PASS native restore installed fixture database after process restart and cleaned completed staging');
    }); };
    globalThis.verifyFailedInstall = function () { Java.perform(function () {
        const prefs = app.getSharedPreferences('boot_prefs', 0);
        check(prefs.getString('rdp', null) !== null, 'Lost pending restore on failure');
        check(prefs.getString('haiagaru_native_restore_error', null) !== null, 'Missing visible failure reason');
        check(File.$new(prefs.getString('rdp', null)).exists(), 'Deleted failed source or restoreTmp');
        const db = DB.openDatabase(app.getDatabasePath('roidon.sqlite').getPath(), null, 1);
        const c = db.rawQuery("SELECT title FROM bookmarks WHERE created=1790351854", null);
        check(c.moveToFirst() && c.getString(0).toString() === '復元テストスレッド', 'Previous DB damaged');
        c.close(); db.close();
        console.log('PASS failed startup DB install kept previous DB, source, pending path and error');
    }); };
    globalThis.clearSyntheticFailure = function () { Java.perform(function () {
        const prefs = app.getSharedPreferences('boot_prefs', 0);
        const injected = File.$new(app.getNoBackupFilesDir(), 'restoreTmp/databases/roidon.sqlite');
        check(injected.getPath().toString() === prefs.getString('rdp', null).toString()
            && Number(injected.length()) === 3, 'Not the injected three-byte failure fixture');
        check(prefs.edit().remove('rdp').remove('haiagaru_native_restore_error').commit(), 'clear fixture prefs');
        check(injected.delete(), 'remove injected fixture');
        injected.getParentFile().delete(); injected.getParentFile().getParentFile().delete();
        console.log('Removed only synthetic failure; restored fixture DB and original ZIP remain');
    }); };
    console.log('Isolated native restore fixture ready: openRestoreFixture(), markPreparedFixture(), verifyResumedFixture(), restoreFixture(), seedFailedInstall(), verifyFailedInstall()');
});
