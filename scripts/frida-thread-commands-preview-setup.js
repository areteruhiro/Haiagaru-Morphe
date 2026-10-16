// Scoped to the isolated emulator verification APK. Does not post anything.
Java.perform(function () {
    const app = Java.use('android.app.ActivityThread').currentApplication();
    if (String(app.getPackageName()) !== 'jp.co.airfront.android.a2chMate.haiagaru.commands191') throw Error('Verification app only');
    const prefs = app.getSharedPreferences('io.github.areteruhiro.chmate.haiagaru.ui-config', 0);
    const enabled = !prefs.getBoolean('xPostPreviewEnabled', false);
    prefs.edit().putBoolean('xPostPreviewEnabled', enabled).commit();
    Java.use('app.morphe.extension.chmate.EmojiFontFallback').refreshVisibleRows();
    console.log('X preview=' + enabled + '; native visible rows refreshed');
});
