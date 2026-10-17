/* Read-only runtime check. No authentication, posting, or clipboard access. */
Java.perform(function () {
    const app = Java.use('android.app.ActivityThread').currentApplication();
    if (String(app.getPackageName()) !== 'jp.co.airfront.android.a2chMate.haiagaru.responsearchive226')
        throw Error('Fixed 226 verification package only');
    const Rules = Java.use('app.morphe.extension.chmate.BoardAuthRules');
    if (String(Rules.code("認証コード'TEST226'を用いて認証してください")) !== 'TEST226')
        throw Error('Latest code parser missing');
    console.log('PASS latest auth code parser is installed');
    const Auth = Java.use('app.morphe.extension.chmate.ExternalBoardAuth');
    const ref = Auth.class.getDeclaredField('resumed'); ref.setAccessible(true);
    const activity = Java.cast(ref.get(null), Java.use('java.lang.ref.WeakReference')).get();
    if (activity === null) throw Error('Activity lifecycle hook not initialized');
    console.log('PASS activity lifecycle hook active: ' + activity.getClass().getName());
    const context = Auth.class.getDeclaredField('context'); context.setAccessible(true);
    if (context.get(null) === null) throw Error('Extension initialization hook not initialized');
    console.log('PASS extension initialization active');
});
