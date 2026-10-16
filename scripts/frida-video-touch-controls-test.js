// Isolated test APK only; fake playback avoids downloads/uploads. Restore media volume.
Java.perform(function () {
    const app = Java.use('android.app.ActivityThread').currentApplication();
    if (!String(app.getPackageName()).endsWith('.videotouch')) throw Error('Isolated app only');
    const A = Java.use('app.morphe.extension.chmate.VideoViewerActivity');
    const Video = Java.use('android.widget.VideoView');
    const I = Java.use('android.content.Intent'), Uri = Java.use('android.net.Uri');
    const prefs = app.getSharedPreferences('io.github.areteruhiro.chmate.haiagaru.ui-config', 0);
    const nativePrefs = Java.use('android.preference.PreferenceManager').getDefaultSharedPreferences(app);
    let count = 0, seeks = [];
    function check(ok, label) { if (!ok) throw Error(label); console.log('PASS ' + label); count++; }
    prefs.edit().putBoolean('videoViewerEnabled', true).putBoolean('videoYoutubeEnabled', true)
        .putBoolean('videoSwipeBrightness', true).putBoolean('videoSwipeVolume', true)
        .putBoolean('videoDoubleTapSeek', true).putInt('videoDoubleTapSeconds', 10).commit();
    function intent(url) { return I.$new('android.intent.action.VIEW', Uri.parse(url)); }
    let yt = intent('https://www.youtube.com/watch?v=M7lc1UVf-VE');
    A.prepareExternalIntent(yt);
    check(yt.getComponent() !== null && String(yt.getComponent().getClassName()).endsWith('YouTubeViewerActivity'), 'YouTube routes internally');
    prefs.edit().putBoolean('videoYoutubeEnabled', false).commit();
    yt = intent('https://youtu.be/M7lc1UVf-VE'); A.prepareExternalIntent(yt);
    check(yt.getComponent() === null, 'YouTube off preserves original');
    prefs.edit().putBoolean('videoYoutubeEnabled', true).commit();
    for (const url of ['https://youtube.com.evil.invalid/watch?v=M7lc1UVf-VE', 'https://youtube.com/playlist?list=123']) {
        const other = intent(url); A.prepareExternalIntent(other); check(other.getComponent() === null, 'unrelated URL untouched');
    }
    const explicit = intent('https://youtu.be/M7lc1UVf-VE').setClassName(app, 'other.Activity');
    A.prepareExternalIntent(explicit); check(String(explicit.getComponent().getClassName()) === 'other.Activity', 'explicit destination untouched');
    A.load.implementation = function () { this.prepared.value = true; };
    Video.getDuration.implementation = function () { return 60000; };
    Video.getCurrentPosition.implementation = function () { return 20000; };
    Video.seekTo.overload('int').implementation = function (ms) { seeks.push(ms); console.log('seekTo=' + ms); };
    Java.scheduleOnMainThread(function () {
        const target = intent('https://i.imgur.com/A54Xv8k.mp4'); A.prepareExternalIntent(target);
        check(target.getComponent() !== null, 'native video still internal');
        app.startActivity(target.addFlags(0x10000000));
    });
    setTimeout(function () { Java.perform(function () { Java.scheduleOnMainThread(function () {
        Java.choose('app.morphe.extension.chmate.VideoViewerActivity', {
            onMatch: function (a) {
                if (a.isFinishing() || a.isDestroyed()) return;
                const M = Java.use('android.view.MotionEvent'), clock = Java.use('android.os.SystemClock');
                const v = a.video.value, p = Java.array('int', [0, 0]); v.getLocationOnScreen(p);
                const x = Number(p[0]), y = Number(p[1]), w = v.getWidth(), h = v.getHeight();
                check(w > 0 && h > 200, 'video surface laid out');
                nativePrefs.edit().putBoolean('gesture', false).commit();
                function send(action, px, py, offset, down) {
                    const e = M.obtain(down, down + offset, action, px, py, 0);
                    a.dispatchTouchEvent(e); e.recycle();
                }
                function swipe(px) {
                    const t = Number(clock.uptimeMillis());
                    send(0, px, y + h * .7, 0, t); send(2, px, y + h * .4, 80, t); send(1, px, y + h * .4, 100, t);
                }
                const window = a.getWindow(), attributes = window.getAttributes(); attributes.screenBrightness.value = .5; window.setAttributes(attributes);
                swipe(x + w * .25);
                check(window.getAttributes().screenBrightness.value > .5, 'left swipe increases window brightness');
                const audio = Java.cast(a.getSystemService('audio'), Java.use('android.media.AudioManager'));
                const before = audio.getStreamVolume(3), max = audio.getStreamMaxVolume(3);
                try {
                    audio.setStreamVolume(3, Math.floor(max / 3), 0); const start = audio.getStreamVolume(3);
                    swipe(x + w * .75); check(audio.getStreamVolume(3) > start, 'right swipe increases media volume');
                } finally { audio.setStreamVolume(3, before, 0); }
                // Framework tap timing is checked separately with real adb input events.
                const math = Java.use('app.morphe.extension.chmate.VideoPlaybackMath');
                check(math.seekTarget(20000, 60000, 10) === 30000 && math.seekTarget(20000, 60000, -10) === 10000,
                    'relative seeking target bounds');
                a.touchControls.value.cancel();
                prefs.edit().putBoolean('videoSwipeBrightness', false).putBoolean('videoSwipeVolume', false)
                    .putBoolean('videoDoubleTapSeek', false).commit();
                const brightness = window.getAttributes().screenBrightness.value, oldSeeks = seeks.length;
                swipe(x + w * .25);
                check(window.getAttributes().screenBrightness.value === brightness, 'brightness off leaves window unchanged');
                check(seeks.length === oldSeeks, 'disabled gesture does not seek');
                console.log('PASS total=' + count);
            }, onComplete: function () {}
        });
    }); }); }, 2000);
});
