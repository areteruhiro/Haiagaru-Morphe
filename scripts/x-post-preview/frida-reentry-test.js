/* Isolated source regression. No real downloads, browser launch, or app preference writes. */
Java.perform(function () {
    const app = Java.use('android.app.ActivityThread').currentApplication();
    if (!/^jp\.co\.airfront\.android\.a2chMate\.haiagaru\.(archivemenu191|responsearchive(191|226|242))$/.test(String(app.getPackageName())))
        throw Error('Isolated verification app required');
    const loader = Java.use('dalvik.system.DexClassLoader').$new(
        '/data/local/tmp/haiagaru-x-preview-reentry-test.dex', String(app.getCodeCacheDir().getPath()), null,
        Java.use('java.lang.ClassLoader').getSystemClassLoader().getParent());
    const test = loader.loadClass('app.morphe.extension.chmate.PreviewTest');
    Java.scheduleOnMainThread(function () {
        try {
            test.getMethod('run', Java.array('java.lang.Class', [Java.use('android.content.Context').class]))
                .invoke(null, Java.array('java.lang.Object', [app]));
            const checks = test.getDeclaredField('checks'); checks.setAccessible(true);
            console.log('PASS ' + checks.getInt(null) + ' Android X preview checks including native setText reentry');
        } catch (error) {
            console.log('FAIL ' + error);
            if (error.cause) console.log('CAUSE ' + error.cause);
            throw error;
        }
    });
});
