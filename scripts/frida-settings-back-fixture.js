// Run only against the disposable settingsback191 emulator package.
setTimeout(function () { Java.perform(function () {
    const Thread = Java.use('android.app.ActivityThread');
    const app = Thread.currentApplication();
    if (String(app.getPackageName()) !== 'jp.co.airfront.android.a2chMate.haiagaru.settingsback191') {
        throw Error('Disposable settings-back fixture package required');
    }
    let settings = null;
    let before = null;
    const H = Java.use('app.morphe.extension.chmate.Haiagaru');
    function preferences() { return H.preferences.overload('android.content.Context').call(H, app); }
    Java.scheduleOnMainThread(function () {
        let active = null;
        for (const item of Thread.currentActivityThread().mActivities.value.values().toArray()) {
            const record = Java.cast(item, Java.use('android.app.ActivityThread$ActivityClientRecord'));
            if (record.activity.value && !record.activity.value.isFinishing()) active = record.activity.value;
        }
        if (!active) throw Error('No foreground activity');
        H.showSettingsDialog(active);
        Java.choose('android.app.AlertDialog', {
            onMatch: function (dialog) {
                if (dialog.isShowing() && dialog.findViewById(16908313) !== null) {
                    settings = Java.retain(dialog);
                }
            },
            onComplete: function () {
                before = String(preferences().getAll());
                console.log('Settings opened: ' + (settings !== null && settings.isShowing()));
            }
        });
    });
    rpc.exports = {
        status: function () {
            return { showing: settings !== null && settings.isShowing(), unchanged: before === String(preferences().getAll()) };
        }
    };
    setTimeout(function () { Java.perform(function () {
        console.log('After Back: showing=' + (settings !== null && settings.isShowing())
            + ', preferencesUnchanged=' + (before === String(preferences().getAll())));
    }); }, 18000);
}); }, 3000);
