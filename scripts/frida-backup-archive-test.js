/* Synthetic private fixture only. Does not access MEGA or export real credentials. */
'use strict';
Java.perform(function () {
    const app = Java.use('android.app.ActivityThread').currentApplication();
    const pkg = String(app.getPackageName());
    if (!/^jp\.co\.airfront\.android\.a2chMate\.haiagaru\.backuptest(191|226|242)$/.test(pkg)) throw Error('Dedicated backup test APK required');
    const File = Java.use('java.io.File'), Out = Java.use('java.io.FileOutputStream');
    const In = Java.use('java.io.FileInputStream'), A = Java.use('app.morphe.extension.chmate.HaiagaruBackupArchive');
    const S = Java.use('app.morphe.extension.chmate.HaiagaruBackupSecrets');
    const Snapshot = Java.use('app.morphe.extension.chmate.HaiagaruSyncSnapshot');
    const Json = Java.use('org.json.JSONObject'), Zip = Java.use('java.util.zip.ZipFile');
    const ZipOut = Java.use('java.util.zip.ZipOutputStream'), Entry = Java.use('java.util.zip.ZipEntry');
    const root = File.$new(app.getCacheDir(), 'backup-fixture-' + Date.now()); root.mkdirs();
    const files = File.$new(root, 'files'), cache = File.$new(root, 'cache'), external = File.$new(root, 'external');
    files.mkdirs(); cache.mkdirs(); external.mkdirs();
    const Info = Java.use('android.content.pm.ApplicationInfo'), info = Info.$new(app.getApplicationInfo());
    info.dataDir.value = String(root.getPath());
    const Context = Java.registerClass({
        name: 'app.haiagaru.test.BackupContext' + Date.now(), superClass: Java.use('android.content.ContextWrapper'),
        methods: {
            $init: [{returnType:'void', argumentTypes:['android.content.Context'], implementation:function(base) { this.$super.$init(base); }}],
            getFilesDir: function () { return files; }, getCacheDir: function () { return cache; },
            getExternalFilesDir: [{returnType:'java.io.File', argumentTypes:['java.lang.String'], implementation:function(type) { return external; }}],
            getApplicationInfo: function () { return info; },
            getFileStreamPath: function (name) { return File.$new(files, name); },
            getSharedPreferences: function (name, mode) { return app.getSharedPreferences('backup-fixture.' + name, mode); }
        }
    });
    const context = Context.$new(app), nativeRoot = File.$new(external, '2chMate'); nativeRoot.mkdirs();
    let count = 0;
    function check(ok, label) { if (!ok) throw Error(label); count++; console.log('PASS ' + label); }
    function write(file, text) {
        file.getParentFile().mkdirs(); const out = Out.$new(file);
        try { out.write(Java.use('java.lang.String').$new(text).getBytes('UTF-8')); } finally { out.close(); }
    }
    function text(file) {
        const scanner = Java.use('java.util.Scanner').$new(file, 'UTF-8');
        try { scanner.useDelimiter('\\A'); return scanner.hasNext() ? String(scanner.next()) : ''; } finally { scanner.close(); }
    }
    function digest(file) {
        const md = Java.use('java.security.MessageDigest').getInstance('SHA-256'), input = In.$new(file);
        const buffer = Java.array('byte', Array(65536).fill(0));
        try { let n; while ((n = input.read(buffer)) !== -1) md.update(buffer, 0, n); } finally { input.close(); }
        return String(Java.use('android.util.Base64').encodeToString(md.digest(), 2));
    }
    function rejects(fn, label) { let rejected = false; try { fn(); } catch (e) { rejected = true; } check(rejected, label); }
    function removeFixture(file) {
        // Exact fixture root, canonical containment checked before every deletion.
        if (!String(file.getCanonicalPath()).startsWith(String(root.getCanonicalPath()) + '/') && !file.equals(root)) throw Error('Cleanup outside fixture');
        if (file.isDirectory()) for (const child of file.listFiles()) removeFixture(child);
        file.delete();
    }
    let backup = null;
    try {
        const dat = File.$new(nativeRoot, 'dat/fixture.dat'); dat.getParentFile().mkdirs();
        const large = Out.$new(dat), random = Java.use('java.util.Random').$new(191226242);
        const block = Java.array('byte', Array(65536).fill(0));
        try { for (let i = 0; i < 640; i++) { random.nextBytes(block); large.write(block); } } finally { large.close(); }
        const expected = digest(dat);
        const response = File.$new(files, 'haiagaru-response-archive/fixture.json'); write(response, '{"fixture":"response"}');
        const archive = File.$new(nativeRoot, 'archive/board/title(1)[123]/fixture.dat'); write(archive, 'native thread DAT');
        write(File.$new(archive.getParentFile(), 'fixture.idx'), 'native companion index');
        const posts = File.$new(files, 'postDataList.json'); write(posts, '[{"id":1},{"id":2}]');
        const memo = File.$new(nativeRoot, 'kakikomi.txt'); write(memo, 'first\nsecond\n');
        const font = File.$new(nativeRoot, 'font/fixture.ttf'); write(font, 'font fixture');
        const keywords = File.$new(files, 'keyword_group_list.json'); write(keywords, '[]');
        const uploads = File.$new(nativeRoot, 'upload.json'); write(uploads, '{"items":[]}');
        const flags = 16 + 8 + 64 + 128 + 256 + 512 + 4096;
        const captured = A.capture(context, flags);
        check(Number(captured.length()) > 32 * 1024 * 1024, 'ZIP itself exceeds former 32MiB total cap');
        backup = A.read(context, captured);
        check(backup.zipped.value, 'disk-backed ZIP accepted');
        const zip = Zip.$new(backup.file.value);
        try {
            check(zip.getEntry('dat/fixture.dat') !== null, 'DAT included');
            check(zip.getEntry('archive/board/title(1)[123]/fixture.idx') !== null, 'thread archive companion index included');
            check(zip.getEntry('responses/fixture.json') !== null, 'response archive included');
            check(zip.getEntry('font/fixture.ttf') !== null && zip.getEntry('keywords/data') !== null, 'font and keyword definitions included');
            check(zip.getEntry('uploads/data') !== null, 'native upload history opt-in included');
        } finally { zip.close(); }
        check(!backup.snapshot.value.has('authentication') && !backup.snapshot.value.has('deviceData'), 'credentials and device blobs excluded without opt-in');
        write(dat, 'changed local DAT'); write(response, '{"fixture":"changed"}');
        A.restore(context, backup, 256, false);
        check(digest(dat) === expected, '40MiB DAT exact streamed restore');
        check(text(response).indexOf('changed') >= 0, 'unselected response archive untouched');
        const recovery = File.$new(files, 'haiagaru-sync-recovery').listFiles();
        check(recovery.length > 0, 'full ZIP recovery copy created before mutation');
        const recoveryZip = Zip.$new(recovery[0]);
        check(recoveryZip.getEntry('dat/fixture.dat') !== null, 'recovery copy includes overwritten DAT'); recoveryZip.close();
        write(posts, '[{"id":2},{"id":3}]'); write(memo, 'first\n');
        A.restore(context, backup, 8 + 16, true);
        check(text(posts) === '[{"id":2},{"id":3},{"id":1}]', 'two-way posting history retains both devices and deduplicates');
        check(text(memo) === 'first\nsecond\n', 'two-way memo extension preserved');
        response.delete(); write(dat, 'keep local');
        A.restore(context, backup, 64 + 256, true);
        check(text(dat) === 'keep local', 'two-way DAT never overwrites existing file');
        check(text(response).indexOf('response') >= 0, 'two-way response adds missing file');
        const metadata = backup.snapshot.value.toString();
        function badZip(path) {
            const file = A.temporary(context), out = ZipOut.$new(Out.$new(file));
            try {
                out.putNextEntry(Entry.$new('snapshot.json')); out.write(Java.use('java.lang.String').$new(metadata).getBytes('UTF-8')); out.closeEntry();
                out.putNextEntry(Entry.$new(path)); out.write(Java.array('byte', [1,2,3])); out.closeEntry();
            } finally { out.close(); }
            return file;
        }
        for (const path of ['dat/../outside', 'dat//empty', 'dat/\\escape', '/absolute', 'unknown/file']) {
            const file = badZip(path); rejects(() => A.read(context, file), 'reject unsafe ZIP path: ' + path);
            check(!file.exists(), 'rejected private transfer cleaned');
        }
        const wrongCategory = badZip('responses/x.json');
        // This path is valid for the declared flags; do not treat it as an attack.
        const valid = A.read(context, wrongCategory); valid.close();
        check(!wrongCategory.exists(), 'prepared backup lifecycle cleans transfer');
        const legacy = A.temporary(context);
        write(legacy, String(Java.use('java.lang.String').$new(Snapshot.capture(context, 8), 'UTF-8')));
        const old = A.read(context, legacy); check(!old.zipped.value, 'legacy JSON remains readable'); old.close();
        const prefdir = File.$new(root, 'shared_prefs'); prefdir.mkdirs(); write(File.$new(prefdir, 'fixture_auth.xml'), 'fixture marker');
        const authPrefs = context.getSharedPreferences('fixture_auth', 0);
        authPrefs.edit().putString('token', 'SYNTHETIC-NOT-A-CREDENTIAL').commit();
        const auth = A.read(context, A.capture(context, 1024));
        try {
            check(auth.snapshot.value.getJSONObject('authentication').getJSONObject('preferences').has('fixture_auth'), 'explicit auth opt-in exports synthetic token');
            authPrefs.edit().remove('token').commit(); A.restore(context, auth, 1024, false);
            check(String(authPrefs.getString('token', '')) === 'SYNTHETIC-NOT-A-CREDENTIAL', 'explicit auth opt-in restores synthetic token');
        } finally { auth.close(); }
        const device = A.read(context, A.capture(context, 2048));
        try {
            S.validateDevice(context, device.snapshot.value, 2048);
            check(true, 'device-bound opt-in accepted for same installation');
            device.snapshot.value.getJSONObject('deviceData').put('installation', 'different-install');
            rejects(() => S.validateDevice(context, device.snapshot.value, 2048), 'device-bound blob refused on different installation');
        } finally { device.close(); }
        console.log('COMPLETE BACKUP ' + pkg.slice(-3) + ' ' + count + ' checks');
    } finally {
        if (backup !== null) backup.close();
        removeFixture(root);
        context.getSharedPreferences('fixture_auth', 0).edit().clear().commit();
    }
});
