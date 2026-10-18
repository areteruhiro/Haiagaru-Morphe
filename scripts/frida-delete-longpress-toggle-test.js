// Isolated fragments/buttons; intercept show() so no user logs or dialogs are touched.
Java.perform(function () {
    Java.deoptimizeEverything();
    const app = Java.use('android.app.ActivityThread').currentApplication();
    const pkg = String(app.getPackageName());
    if (!/\.haiagaru\.(uploadhosts191|responsearchive226|responsearchive242)$/.test(pkg))
        throw Error('Verification packages only');
    const D = Java.use('app.morphe.extension.chmate.DeleteReadThreads');
    const H = Java.use('app.morphe.extension.chmate.Haiagaru');
    const prefs = app.getSharedPreferences('io.github.areteruhiro.chmate.haiagaru.ui-config', 0);
    const key = 'deleteReadThreadsLongPress', had = prefs.contains(key), old = prefs.getBoolean(key, true);
    Java.scheduleOnMainThread(function () {
        let checks = 0, shown = 0;
        function check(ok, label) { if (!ok) throw Error(label); checks++; console.log('PASS ' + label); }
        const show = D.show.overload('java.lang.Object');
        show.implementation = function () { shown++; };
        try {
            const Unsafe = Java.use('sun.misc.Unsafe');
            const f = Unsafe.class.getDeclaredField('theUnsafe'); f.setAccessible(true);
            const unsafe = Java.cast(f.get(null), Unsafe);
            const cls = pkg.endsWith('191') ? 'o.r8lambdaGCnF6WpW_bFarRe7yCX2B6KzQ'
                : pkg.endsWith('226') ? 'o.Yhp5' : 'jp.syoboi.a2chMate.ui.threadlist.ThreadListFragment';
            const fragment = unsafe.allocateInstance(Java.use(cls).class);
            const button = Java.use('android.view.View').$new(app); button.setId(5);
            prefs.edit().remove(key).commit();
            check(H.deleteReadThreadsLongPressEnabled(), 'default enabled');
            prefs.edit().putBoolean(key, false).commit();
            check(!H.deleteReadThreadsLongPressEnabled(), 'off preference read immediately');
            check(!D.longClick(fragment, button) && shown === 0, 'off delegates native long press');
            D.show(fragment);
            check(shown === 1, 'direct/gesture entry remains available while off');
            prefs.edit().putBoolean(key, true).commit();
            check(H.deleteReadThreadsLongPressEnabled(), 'on takes effect without restart');
            // Unsafe-allocated fragment has no Activity: native show exits without any data access.
            check(D.longClick(fragment, button), 'on consumes refresh long press');
            button.setId(9);
            check(!D.longClick(fragment, button), 'other toolbar action unchanged');
            check(!D.longClick(null, button), 'other screens ignored');
            check(!D.longClick(fragment, null), 'null button safe');
            console.log('PASS ' + checks + ' checks: ' + pkg);
        } finally {
            const edit = prefs.edit(); if (had) edit.putBoolean(key, old); else edit.remove(key);
            edit.commit(); show.implementation = null;
        }
    });
});
