// Isolated verification app only. No threads, NG rules or posts are modified.
Java.perform(function () {
    const app = Java.use('android.app.ActivityThread').currentApplication();
    if (String(app.getPackageName()) !== 'jp.co.airfront.android.a2chMate.haiagaru.hiddenng191') throw Error('Verification app only');
    const H = Java.use('app.morphe.extension.chmate.Haiagaru');
    const Menu = Java.use('app.morphe.extension.chmate.HiddenNgMenu');
    const Popup = Java.use('android.widget.PopupMenu');
    const View = Java.use('android.view.View');
    const prefs = app.getSharedPreferences('io.github.areteruhiro.chmate.haiagaru.ui-config', 0);
    const key = 'hideShowHiddenNgMenu', had = prefs.contains(key), saved = prefs.getBoolean(key, false);
    const thread = Java.use('android.app.ActivityThread').currentActivityThread();
    const it = thread.mActivities.value.values().iterator();
    let activity, checks = 0;
    while (it.hasNext()) {
        const record = Java.cast(it.next(), Java.use('android.app.ActivityThread$ActivityClientRecord'));
        if (!record.paused.value) activity = Java.retain(record.activity.value);
    }
    if (!activity) throw Error('No foreground verification activity');
    function check(ok, name) { if (!ok) throw Error(name); checks++; console.log('PASS ' + name); }
    Java.scheduleOnMainThread(function () {
        try {
            prefs.edit().remove(key).commit();
            check(!H.hideShowHiddenNgMenu(), 'default off');
            check(Menu.itemId() === 2131362442, '191 patched resource ID');
            const menu = Popup.$new(activity, View.$new(activity)).getMenu();
            const JString = Java.use('java.lang.String');
            const add = menu.add.overload('int', 'int', 'int', 'java.lang.CharSequence');
            const target = add.call(menu, 0, Menu.itemId(), 0, JString.$new('非表示NGを表示 (2)'));
            const other = add.call(menu, 0, 123, 1, JString.$new('NG編集'));
            Menu.apply(menu); check(target.isVisible(), 'off preserves native visible action');
            prefs.edit().putBoolean(key, true).commit();
            Menu.apply(menu);
            check(!target.isVisible(), 'on hides target');
            check(other.isVisible() && menu.size() === 2, 'other action and menu structure unchanged');
            target.setVisible(true); Menu.apply(menu);
            check(!target.isVisible(), 'native menu reprepare remains hidden');
            prefs.edit().putBoolean(key, false).commit();
            target.setVisible(true); Menu.apply(menu);
            check(target.isVisible(), 'turning off restores native visible result');
            target.setVisible(false); Menu.apply(menu);
            check(!target.isVisible(), 'off does not expose action hidden by native conditions');
            Menu.apply(null); check(true, 'null menu safe');
            const empty = Popup.$new(activity, View.$new(activity)).getMenu();
            prefs.edit().putBoolean(key, true).commit(); Menu.apply(empty);
            check(empty.size() === 0, 'missing target safe');
            console.log('PASS ALL ' + checks + ' hidden-NG menu checks');
        } finally {
            const editor = prefs.edit();
            if (had) editor.putBoolean(key, saved); else editor.remove(key);
            editor.commit();
        }
    });
});
