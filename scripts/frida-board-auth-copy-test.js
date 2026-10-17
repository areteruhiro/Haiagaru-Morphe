/* Synthetic UI test only. Never submits authentication or a post. */
Java.perform(function () {
    const app = Java.use('android.app.ActivityThread').currentApplication();
    if (String(app.getPackageName()) !== 'jp.co.airfront.android.a2chMate.haiagaru.responsearchive226')
        throw Error('Fixed 226 verification package only');
    const Auth = Java.use('app.morphe.extension.chmate.ExternalBoardAuth');
    const Activity = Java.use('android.app.Activity');
    const WeakReference = Java.use('java.lang.ref.WeakReference');
    const field = Auth.class.getDeclaredField('resumed'); field.setAccessible(true);
    const current = Java.cast(field.get(null), WeakReference).get();
    if (current === null) throw Error('Open the 226 verification app first');
    const activity = Java.retain(Java.cast(current, Activity));
    const message = "検証用の書き込みエラー。認証コード'TEST226'を用いて認証してください。";
    const candidates = Auth.class.getDeclaredMethods(); let show;
    for (const method of candidates) {
        const parameters = method.getParameterTypes();
        if (parameters.length === 4 && String(parameters[0].getName()) === 'android.app.Activity'
            && [1,2,3].every(i => String(parameters[i].getName()) === 'java.lang.String')) show = method;
    }
    if (!show) throw Error('Error presentation method missing');
    show.setAccessible(true);
    Java.scheduleOnMainThread(function () {
        const StringType = Java.use('java.lang.String');
        show.invoke(null, Java.array('java.lang.Object', [activity, StringType.$new('https://board-auth-test.invalid/edge/'), StringType.$new(''), StringType.$new(message)]));
    });
    setTimeout(function () { Java.perform(function () {
        Java.choose('android.app.AlertDialog', {
            onMatch: function (dialog) {
                if (!dialog.isShowing()) return;
                const copy = dialog.getButton(-3);
                if (copy === null || String(copy.getText()) !== '認証コードをコピー') return;
                const retained = Java.retain(dialog);
                Java.scheduleOnMainThread(function () {
                    retained.getButton(-3).performClick();
                    if (!retained.isShowing()) throw Error('Copy unexpectedly dismissed error UI');
                    const manager = Java.cast(activity.getSystemService('clipboard'), Java.use('android.content.ClipboardManager'));
                    const clip = manager.getPrimaryClip();
                    if (clip === null || String(clip.getItemAt(0).getText()) !== 'TEST226')
                        throw Error('Copied code differs from synthetic test code');
                    console.log('PASS 226 synthetic code copy; dialog stays visible; no authentication/post sent');
                });
            }, onComplete: function () {}
        });
    }); }, 1000);
});
