/* Saves/reopens only the new toggle in the isolated network191 test app. */
'use strict';
Java.perform(function () {
    const app = Java.use('android.app.ActivityThread').currentApplication();
    if (String(app.getPackageName()) !== 'jp.co.airfront.android.a2chMate.haiagaru.network191')
        throw Error('Isolated network191 app required');
    const H = Java.use('app.morphe.extension.chmate.Haiagaru');
    const prefs = app.getSharedPreferences('io.github.areteruhiro.chmate.haiagaru.ui-config', 0);
    const key = 'fast191UpdateConnect', existed = prefs.contains(key), saved = prefs.getBoolean(key, true);
    const Group = Java.use('android.view.ViewGroup'), Switch = Java.use('android.widget.Switch');
    const Activity = Java.use('android.app.Activity');
    const it = Java.use('android.app.ActivityThread').currentActivityThread().mActivities.value.values().iterator();
    let activity;
    while (it.hasNext()) {
        const r = Java.cast(it.next(), Java.use('android.app.ActivityThread$ActivityClientRecord'));
        if (!r.paused.value) activity = Java.retain(Java.cast(r.activity.value, Activity));
    }
    if (!activity) throw Error('No resumed activity');
    function check(ok, text) { if (!ok) throw Error(text); console.log('PASS ' + text); }
    function find(v) {
        if (Switch.class.isInstance(v)) {
            const s = Java.cast(v, Switch);
            if (/代替IP|alternative IP/.test(String(s.getText()))) return s;
        }
        if (Group.class.isInstance(v)) {
            const g = Java.cast(v, Group);
            for (let i = 0; i < g.getChildCount(); i++) { const s = find(g.getChildAt(i)); if (s) return s; }
        }
    }
    function inspect(fn) {
        setTimeout(function () { Java.perform(function () {
            let handled = false;
            Java.choose('android.app.AlertDialog', {
                onMatch: function (d) {
                    if (handled || !d.isShowing()) return;
                    const s = find(d.getWindow().getDecorView());
                    if (!s) return;
                    handled = true;
                    const dialog = Java.retain(d), toggle = Java.retain(s);
                    Java.scheduleOnMainThread(function () { fn(dialog, toggle); });
                }, onComplete: function () { check(handled, 'settings dialog contains 191 fallback toggle'); }
            });
        }); }, 1500);
    }
    Java.scheduleOnMainThread(function () { H.showSettingsDialog(activity); });
    inspect(function (d, s) {
        check(s.isChecked() === saved, 'initial saved state');
        s.setChecked(!saved); d.getButton(-1).performClick();
        check(!d.isShowing(), 'save closes dialog');
        check(prefs.getBoolean(key, saved) === !saved, 'toggle persisted');
        H.showSettingsDialog(activity);
        inspect(function (d2, s2) {
            check(s2.isChecked() === !saved, 'reopen uses persisted state');
            s2.setChecked(saved); d2.getButton(-1).performClick();
            check(prefs.getBoolean(key, !saved) === saved, 'original toggle restored via UI');
            if (!existed) prefs.edit().remove(key).commit();
            console.log('COMPLETE fallback toggle save/reopen/restore');
        });
    });
});
