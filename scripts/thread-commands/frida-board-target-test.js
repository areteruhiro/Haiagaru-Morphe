// Detached inputs only. Does not create threads, send posts or change real drafts.
Java.perform(function () {
    const app = Java.use('android.app.ActivityThread').currentApplication();
    const pkg = String(app.getPackageName());
    if (!/haiagaru\.(responsearchive226|responsearchive242)$/.test(pkg)) throw Error('Verification app only');
    const modern = pkg.endsWith('242');
    const loader = Java.use('dalvik.system.DelegateLastClassLoader').$new('/data/local/tmp/thread-command-board-fix.dex', app.getClassLoader());
    const C = Java.ClassFactory.get(loader).use('app.morphe.extension.chmate.ThreadCommands');
    const R = Java.use('app.morphe.extension.chmate.ThreadCommandRules');
    const Target = Java.use('jp.syoboi.a2chMate.client.BBSUrlInfo');
    let checks = 0;
    function check(ok, name) { if (!ok) throw Error(name); checks++; console.log('PASS ' + name); }
    for (const pair of [['egg.5ch.io', 'android', '5ch'], ['bbs.eddibb.cc', 'liveedge', 'edge'], ['hayabusa.open2ch.net', 'livejupiter', 'open2ch']]) {
        const target = Target.$new(pair[0], pair[1], null, 0, null, null);
        const threadMethod = modern ? 'A' : 'G', boardMethod = modern ? 'y' : 'v';
        check(target[threadMethod]() === null, 'new thread has no thread URL: ' + pair[2]);
        const url = C.postingUrl(target, true, threadMethod, boardMethod);
        check(String(R.profile(url)) === pair[2], 'board profile: ' + pair[2]);
        check(String(R.boardKey(url)).includes(pair[1]), 'board key: ' + pair[2]);
        check(C.postingUrl(target, false, threadMethod, boardMethod) === null, 'reply path unchanged: ' + pair[2]);
    }
    let found = false;
    Java.choose('android.app.Activity', { onMatch(a) {
        if (found || a.isFinishing() || !a.getWindow().getDecorView().isShown()) return;
        found = true; const activity = Java.retain(a);
        Java.scheduleOnMainThread(function () {
            try {
                const E = Java.use('android.widget.EditText'), S = Java.use('java.lang.String');
                const body = E.$new(activity), name = E.$new(activity);
                body.setText(S.$new('verification draft'), Java.use('android.widget.TextView$BufferType').EDITABLE.value);
                C.choose.overload('android.widget.EditText', 'android.widget.EditText', 'java.lang.String', 'java.lang.String', 'boolean').call(C, body, name, '5ch', 'https://egg.5ch.io/android/', true);
                Java.choose('android.app.AlertDialog', { onMatch(d) {
                    if (!d.isShowing() || d.getListView() === null) return;
                    const list = d.getListView();
                    check(list.getAdapter().getCount() >= 13, '5ch command menu populated');
                    d.dismiss();
                }, onComplete() {} });
                const prefs = app.getSharedPreferences('haiagaru_thread_commands', 0);
                const key = 'board.egg.5ch.io/android/.0';
                const previous = prefs.contains(key) ? String(prefs.getString(key, null)) : null;
                prefs.edit().remove(key).commit();
                const dialog = Java.retain(C.edit.overload('android.widget.EditText', 'android.widget.EditText', 'java.lang.String', 'java.lang.String', 'int').call(C, body, name, '5ch', 'https://egg.5ch.io/android/', 0));
                const retainedBody = Java.retain(body);
                setTimeout(function () { Java.perform(function () { Java.scheduleOnMainThread(function () { try {
                    dialog.getButton(-3).performClick();
                    dialog.getButton(-1).performClick();
                    const result = String(Java.cast(retainedBody.getText(), Java.use('java.lang.Object')).toString());
                    check(result.startsWith('!extend:checked:vvvvv:1000:512\n'), 'command inserted: ' + JSON.stringify(result));
                    check(result.split('\n').length === 4 && result.endsWith('verification draft'), 'three lines inserted, draft preserved');
                    dialog.dismiss();
                    console.log('COMPLETE ' + checks + ' native board target and insertion checks');
                } catch(e) { console.log('FAIL ' + e.stack); } finally {
                    dialog.dismiss();
                    const editor = prefs.edit();
                    if (previous === null) editor.remove(key); else editor.putString(key, previous);
                    editor.commit();
                } }); }); }, 4000);
            } catch(e) { console.log('FAIL ' + e.stack); }
        });
    }, onComplete() { if (!found) console.log('FAIL no visible activity'); } });
});
