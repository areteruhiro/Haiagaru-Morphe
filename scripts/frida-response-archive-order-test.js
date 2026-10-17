// Synthetic isolated collection only; restore sorting preferences and remove our own fixture.
Java.perform(function () {
    const app = Java.use('android.app.ActivityThread').currentApplication();
    if (String(app.getPackageName()) !== 'jp.co.airfront.android.a2chMate.haiagaru.archivemenu191')
        throw Error('Isolated emulator app required');
    const A = Java.use('app.morphe.extension.chmate.ResponseArchive');
    const E = Java.use('app.morphe.extension.chmate.ResponseArchiveEntry');
    const File = Java.use('java.io.File');
    const prefs = app.getSharedPreferences('haiagaru-response-archive', 0);
    const had = prefs.contains('sort'), before = prefs.getInt('sort', 0);
    const collection = A.createCollection(app, '並び順検証 ' + Date.now());
    const id = String(collection.id.value);
    const dir = File.$new(File.$new(app.getFilesDir(), 'haiagaru-response-archive/threads'), id);
    let checks = 0;
    function check(ok, label) { if (!ok) throw Error(label); checks++; console.log('PASS ' + label); }
    try {
        const dates = ['2026/10/17(土) 21:31:19.27 ID:a', '2026/10/15(木) 20:00:00 ID:b', '2026/10/16(金) 10:00:00.00 ID:c'];
        const files = [];
        for (let i = 0; i < 3; i++) {
            const e = E.$new('https://example.invalid/test/read.cgi/fixture/1234567890/' + (i + 1),
                '並び順検証', i + 1, '名前', dates[i], '合成本文');
            A.saveToCollection(app, id, e);
            const f = A.fileIn(dir, String(e.url.value)); files.push(f);
            const json = A.readJson(f, 8000000);
            check(Number(json.getLong('savedAt')) > 0, 'new save records timestamp ' + i);
            json.put.overload('java.lang.String', 'long').call(json, 'savedAt', (i + 1) * 1000);
            A.writeJson(f, json);
        }
        const expected = [[3,2,1], [1,2,3], [1,3,2], [2,3,1]];
        for (let mode = 0; mode < 4; mode++) {
            prefs.edit().putInt('sort', mode).commit();
            const ordered = Array.from(A.sortedArchiveFiles(app, dir)).map(f => Number(A.read(f).number.value));
            check(JSON.stringify(ordered) === JSON.stringify(expected[mode]), 'actual saved files mode ' + mode);
        }
        const legacy = A.readJson(files[0], 8000000); legacy.remove('savedAt');
        A.writeJson(files[0], legacy);
        check(files[0].setLastModified(500), 'legacy timestamp fixture');
        prefs.edit().putInt('sort', 1).commit();
        check(Number(A.read(A.sortedArchiveFiles(app, dir)[0]).number.value) === 1, 'old archive uses file timestamp');
        legacy.put.overload('java.lang.String','java.lang.Object').call(legacy,'date',Java.use('java.lang.String').$new('日時不明'));
        A.writeJson(files[0], legacy);
        for (const mode of [2,3]) {
            prefs.edit().putInt('sort', mode).commit();
            const sorted = A.sortedArchiveFiles(app, dir);
            check(Number(A.read(sorted[sorted.length-1]).number.value) === 1, 'unknown posting date last mode ' + mode);
        }
        console.log('COMPLETE ' + checks + ' archive order storage checks');
    } finally {
        const edit = prefs.edit(); if (had) edit.putInt('sort', before); else edit.remove('sort'); edit.commit();
        for (const child of Array.from(dir.listFiles() || [])) {
            const name = String(child.getName());
            if (name !== 'collection.json' && !/^[0-9a-f]{64}\.json(?:\.bak)?$/.test(name)) throw Error('Unexpected fixture file');
            if (!child.delete()) throw Error('Fixture cleanup failed');
        }
        if (!dir.delete()) throw Error('Fixture directory cleanup failed');
    }
    Java.scheduleOnMainThread(function () {
        const records = Java.use('android.app.ActivityThread').currentActivityThread().mActivities.value.values().iterator();
        let activity = null;
        while (records.hasNext()) {
            const row = Java.cast(records.next(), Java.use('android.app.ActivityThread$ActivityClientRecord'));
            if (!row.paused.value) activity = Java.cast(row.activity.value, Java.use('android.app.Activity'));
        }
        if (activity === null) throw Error('No resumed test activity');
        A.showSort(activity, null, 0);
        let found = false;
        Java.choose('android.app.AlertDialog', {
            onMatch: function (dialog) {
                if (found || !dialog.isShowing()) return;
                const list = dialog.getListView();
                if (list === null || list.getAdapter().getCount() !== 4
                        || String(list.getAdapter().getItem(0)).indexOf('保存日時') < 0) return;
                found = true;
                check(String(list.getAdapter().getItem(2)).indexOf('投稿日時') >= 0, 'sort picker shows saved and posted date modes');
                check(list.getCheckedItemPosition() === A.sortMode(app), 'sort picker reflects stored selection');
                dialog.dismiss();
            }, onComplete: function () {
                check(found, 'sort picker opens on Android');
                console.log('COMPLETE ' + checks + ' archive storage and sort UI checks');
            }
        });
    });
});
