Java.perform(function () {
    const app = Java.use('android.app.ActivityThread').currentApplication();
    if (!String(app.getPackageName()).endsWith('.videotouch')) throw Error('Test app only');
    const A = Java.use('app.morphe.extension.chmate.VideoViewerActivity');
    A.load.implementation = function () { this.prepared.value = true; };
    const V = Java.use('android.widget.VideoView');
    V.getDuration.implementation = function () { return 60000; };
    V.getCurrentPosition.implementation = function () { return 20000; };
    V.seekTo.overload('int').implementation = function (ms) { console.log('SEEK=' + ms); };
    const Listener = Java.use('app.morphe.extension.chmate.VideoTouchControls$1');
    const doubleTap = Listener.onDoubleTap.overload('android.view.MotionEvent');
    doubleTap.implementation = function (event) { console.log('DOUBLE callback'); return doubleTap.call(this, event); };
    const Intent = Java.use('android.content.Intent'), Uri = Java.use('android.net.Uri');
    Java.scheduleOnMainThread(function () {
        app.getSharedPreferences('io.github.areteruhiro.chmate.haiagaru.ui-config', 0).edit()
            .putBoolean('videoViewerEnabled', true).putBoolean('videoDoubleTapSeek', true).commit();
        Java.use('android.preference.PreferenceManager').getDefaultSharedPreferences(app).edit().putBoolean('gesture', false).commit();
        app.startActivity(Intent.$new(app, A.class).setData(Uri.parse('https://i.imgur.com/A54Xv8k.mp4')).addFlags(0x10000000));
    });
    setTimeout(function () { Java.perform(function () { Java.scheduleOnMainThread(function () {
        Java.choose('app.morphe.extension.chmate.VideoViewerActivity', { onMatch: function (a) {
            if (a.isFinishing() || !a.hasWindowFocus()) return;
            a.prepared.value = true;
            const v = a.video.value, p = Java.array('int', [0, 0]); v.getLocationOnScreen(p);
            console.log('TAP=' + (Number(p[0]) + v.getWidth() * .75) + ',' + (Number(p[1]) + v.getHeight() * .5));
        }, onComplete: function () {} });
    }); }); }, 1500);
});
