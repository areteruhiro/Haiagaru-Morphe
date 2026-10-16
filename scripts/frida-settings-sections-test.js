// Presentation tests in the isolated emulator APK. Never saves a setting.
Java.perform(function () {
    const app = Java.use('android.app.ActivityThread').currentApplication();
    if (String(app.getPackageName()) !== 'jp.co.airfront.android.a2chMate.haiagaru.commands191') throw Error('Verification APK only');
    const H = Java.use('app.morphe.extension.chmate.Haiagaru');
    const Activity = Java.use('android.app.Activity');
    const Group = Java.use('android.view.ViewGroup');
    const Text = Java.use('android.widget.TextView');
    const Edit = Java.use('android.widget.EditText');
    const Switch = Java.use('android.widget.Switch');
    const Spinner = Java.use('android.widget.Spinner');
    const prefs = app.getSharedPreferences('io.github.areteruhiro.chmate.haiagaru.ui-config', 0);
    const savedX = prefs.getBoolean('xPostPreviewEnabled', false);
    let checks = 0;
    function check(ok, name) { if (!ok) throw Error(name); checks++; console.log('PASS ' + name); }
    function walk(v, out) {
        out.push(v);
        if (Group.class.isInstance(v)) {
            const g = Java.cast(v, Group);
            for (let i = 0; i < g.getChildCount(); i++) walk(g.getChildAt(i), out);
        }
        return out;
    }
    function label(v) { return Text.class.isInstance(v) ? String(Java.cast(v, Text).getText()) : ''; }
    function setText(v, value) { Text.setText.overload('java.lang.CharSequence').call(v, Java.use('java.lang.String').$new(value)); }
    const thread = Java.use('android.app.ActivityThread').currentActivityThread();
    const it = thread.mActivities.value.values().iterator();
    let activity;
    while (it.hasNext()) {
        const record = Java.cast(it.next(), Java.use('android.app.ActivityThread$ActivityClientRecord'));
        const a = Java.cast(record.activity.value, Activity);
        if (!a.isFinishing() && !record.paused.value) activity = Java.retain(a);
    }
    if (!activity) throw Error('No resumed verification activity');
    Java.choose('android.app.AlertDialog', {
        onMatch: function (d) { if (d.isShowing()) { const old = Java.retain(d); Java.scheduleOnMainThread(() => old.dismiss()); } },
        onComplete: function () { Java.scheduleOnMainThread(function () { H.showSettingsDialog(activity); }); }
    });
    setTimeout(function () { Java.perform(function () {
        let handled = false;
        Java.choose('android.app.AlertDialog', {
            onMatch: function (candidate) {
                if (handled || !candidate.isShowing()) return;
                handled = true;
                const dialog = Java.retain(candidate);
                Java.scheduleOnMainThread(function () {
                    const views = walk(dialog.getWindow().getDecorView(), []);
                    const find = text => views.find(v => label(v) === text);
                    const search = Java.cast(views.find(v => Edit.class.isInstance(v) && String(Java.cast(v, Edit).getHint()).indexOf('設定を検索') >= 0), Edit);
                    const headers = views.filter(v => label(v).startsWith('▸ ') || label(v).startsWith('▾ '));
                    check(headers.length === 10, '10 categories');
                    check(headers.every(v => label(v).startsWith('▸ ')), 'initial categories collapsed');
                    const edits = views.filter(v => Edit.class.isInstance(v) && !v.equals(search));
                    const original = edits.map(v => label(v));
                    const spinners = views.filter(v => Spinner.class.isInstance(v)).map(v => Java.cast(v, Spinner));
                    const selections = spinners.map(v => v.getSelectedItemPosition());
                    const x = Java.cast(views.find(v => Switch.class.isInstance(v) && label(v).indexOf('X／Twitter') >= 0), Switch);
                    const originalX = x.isChecked(); x.setChecked(!originalX);
                    find('全て開く').performClick();
                    check(headers.every(v => label(v).startsWith('▾ ')), 'expand all');
                    setText(search, '行間');
                    const visible = headers.filter(v => v.isShown());
                    check(visible.length === 1 && label(visible[0]).indexOf('スレ・スレ一覧') >= 0, 'search finds spacing category');
                    visible[0].performClick(); check(label(visible[0]).startsWith('▸ '), 'search result can collapse');
                    setText(search, '存在しない設定xyz');
                    check(headers.every(v => !v.isShown()), 'no-result search');
                    find('クリア').performClick();
                    check(headers.every(v => v.isShown()), 'clear restores categories');
                    check(edits.every((v, i) => label(v) === original[i]), 'all field values retained');
                    check(spinners.every((v, i) => v.getSelectedItemPosition() === selections[i]), 'all spinner selections retained');
                    check(x.isChecked() !== originalX, 'unsaved toggle retained during navigation');
                    check(prefs.getBoolean('xPostPreviewEnabled', false) === savedX, 'navigation does not save');
                    const countLabel = views.findIndex(v => label(v).startsWith('書き込んだスレの表示数'));
                    const countField = Java.cast(views.slice(countLabel + 1).find(v => Edit.class.isInstance(v)), Edit);
                    const countText = String(countField.getText());
                    setText(countField, '-1');
                    dialog.getButton(-1).performClick();
                    check(dialog.isShowing(), 'invalid value does not dismiss settings');
                    check(countField.isShown(), 'invalid field category is revealed');
                    setText(countField, countText);
                    dialog.getButton(-2).performClick();
                    setTimeout(function () { Java.perform(function () { Java.scheduleOnMainThread(function () {
                        check(!dialog.isShowing(), 'cancel closes settings');
                        check(prefs.getBoolean('xPostPreviewEnabled', false) === savedX, 'cancel preserves saved preference');
                        H.showSettingsDialog(activity);
                        console.log('PASS ' + checks + ' settings UI checks; preferences not saved');
                    }); }); }, 2000);
                });
            }, onComplete: function () {}
        });
    }); }, 1800);
});
