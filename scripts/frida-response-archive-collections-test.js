/* Android persistence checks; isolated test packages only, no network or real DAT. */
'use strict';
Java.perform(function () {
    const app = Java.use('android.app.ActivityThread').currentApplication();
    const pkg = String(app.getPackageName());
    if (!/^jp\.co\.airfront\.android\.a2chMate\.haiagaru\.responsearchive(191|226|242)$/.test(pkg))
        throw Error('Isolated response archive app required');
    const A = Java.use('app.morphe.extension.chmate.ResponseArchive');
    const E = Java.use('app.morphe.extension.chmate.ResponseArchiveEntry');
    const File = Java.use('java.io.File'), Atomic = Java.use('android.util.AtomicFile');
    const marker = 'collection-fixture-' + Java.use('java.util.UUID').randomUUID().toString();
    const url = 'https://example.invalid/test/read.cgi/fixture/1234567890/42?fixture=' + marker;
    const entry = E.$new(url, '合成元スレ', 42, '名前', '日時 ID:a+/', '<b>保存確認</b><br>line2');
    const legacy = A.file(app, url);
    const root = File.$new(app.getFilesDir(), 'haiagaru-response-archive/threads');
    const created = [];
    let checks = 0;
    function check(ok, label) { if (!ok) throw Error(label); checks++; console.log('PASS ' + label); }
    function rejects(fn, label) { let failed = false; try { fn(); } catch (_) { failed = true; } check(failed, label); }
    try {
        check(!legacy.exists(), 'unique synthetic response');
        const one = A.createCollection(app, '  保存用スレA ' + marker + '  ');
        created.push(one);
        const two = A.createCollection(app, '保存用スレB ' + marker);
        created.push(two);
        const id = String(one.id.value), id2 = String(two.id.value);
        check(id !== id2, 'multiple independent destinations');
        check(String(A.readCollection(app,id).title.value) === '保存用スレA ' + marker, 'name trimmed and persisted');
        const f1 = File.$new(File.$new(root,id), String(legacy.getName()));
        const f2 = File.$new(File.$new(root,id2), String(legacy.getName()));
        A.saveToCollection(app,id,entry);
        check(f1.exists() && !f2.exists() && !legacy.exists(), 'only selected destination saved');
        A.saveToCollection(app,id2,entry);
        check(String(A.read(f2).body.value) === String(entry.body.value), 'body preserved in second destination');
        A.saveToCollection(app,id,entry);
        check(Array.from(f1.getParentFile().listFiles()).filter(f=>String(f.getName())===String(f1.getName())).length === 1, 'same response deduplicated');
        const backup = File.$new(String(f1.getPath()) + '.bak');
        check(f1.renameTo(backup), 'simulate interrupted write');
        check(String(A.read(f1).body.value) === String(entry.body.value) && f1.exists(), 'atomic recovery within collection');
        Atomic.$new(f1).delete();
        check(!f1.exists() && f2.exists(), 'deleting one copy preserves other destination');
        A.save(app,entry);
        check(legacy.exists(), 'legacy archive retained');
        check(A.collections(app).size() >= 2, 'collection browser lists destinations');
        rejects(()=>A.createCollection(app,'   '), 'blank title rejected');
        rejects(()=>A.createCollection(app,'x'.repeat(201)), 'overlong title rejected');
        for (const bad of ['../outside','',null,'00000000-0000-0000-0000-000000000000'])
            rejects(()=>A.saveToCollection(app,bad,entry), 'missing/invalid destination rejected: ' + bad);
        check(legacy.exists() && f2.exists(), 'invalid saves leave existing data intact');
        console.log('COMPLETE ' + pkg.slice(-3) + ' ' + checks + ' collection persistence checks');
    } finally {
        // Only paths created and owned by this invocation are removed.
        for (const collection of created) {
            const dir = File.$new(root,String(collection.id.value));
            for (const child of Array.from(dir.listFiles() || [])) {
                if (!child.delete()) throw Error('Cannot clean fixture: ' + child);
            }
            if (!dir.delete()) throw Error('Cannot clean fixture directory');
        }
        Atomic.$new(legacy).delete();
    }
});
