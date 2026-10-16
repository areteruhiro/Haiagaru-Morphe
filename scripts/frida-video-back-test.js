// Isolated APK only. Playback/network loading is stubbed; Back handlers are the packaged code.
Java.perform(function () {
    const app = Java.use('android.app.ActivityThread').currentApplication();
    if (!String(app.getPackageName()).endsWith('.videoback')) throw Error('Isolated test app only');
    const A = Java.use('app.morphe.extension.chmate.VideoViewerActivity');
    const Y = Java.use('app.morphe.extension.chmate.YouTubeViewerActivity');
    const Web = Java.use('android.webkit.WebView');
    const Intent = Java.use('android.content.Intent'), Uri = Java.use('android.net.Uri');
    const View = Java.use('android.view.View');
    const p = app.getSharedPreferences('io.github.areteruhiro.chmate.haiagaru.ui-config', 0);
    p.edit().putBoolean('videoViewerEnabled', true).putBoolean('videoYoutubeEnabled', true).commit();
    A.load.implementation = function () { }; // Do not download test media.
    Web.loadDataWithBaseURL.implementation = function () { }; // No YouTube request needed.
    let activity, count = 0;
    function check(ok, name) {
        if (!ok) throw Error(name);
        console.log('PASS ' + name); count++;
    }
    function main(fn) { Java.perform(function () { Java.scheduleOnMainThread(fn); }); }
    const cases = [
        { youtube: false, shown: true },
        { youtube: false, shown: false },
        { youtube: true, fullscreen: false },
        { youtube: true, fullscreen: true }
    ];
    function run(i) {
        if (i === cases.length) { console.log('PASS ALL ' + count + ' viewer Back checks'); return; }
        const c = cases[i], cls = c.youtube ? Y : A;
        main(function () {
            const url = c.youtube ? 'https://youtu.be/M7lc1UVf-VE' : 'https://i.imgur.com/test.mp4';
            const intent = Intent.$new('android.intent.action.VIEW', Uri.parse(url));
            intent.setClassName(app, cls.$className).addFlags(0x10000000);
            app.startActivity(intent);
        });
        setTimeout(function () { main(function () {
            Java.choose(cls.$className, { onMatch: function (a) {
                if (a.isFinishing() || a.isDestroyed() || !a.hasWindowFocus()) return;
                activity = Java.retain(a);
            }, onComplete: function () {
                check(activity && !activity.isFinishing(), 'viewer opened ' + i);
                if (!c.youtube) {
                    activity.setStatus('');
                    check(activity.status.value.getVisibility() === 8, 'empty status takes no space ' + i);
                    const ctrl = activity.controls.value;
                    ctrl.setMediaPlayer(activity.video.value);
                    if (c.shown) ctrl.show(0); else ctrl.hide();
                    check(ctrl.isShowing() === c.shown, 'controller state ' + i);
                    if (c.shown) check(ctrl.findOnBackInvokedDispatcher() !== null, 'controller system Back dispatcher attached');
                } else {
                    // No extra hint TextView below the content frame.
                    check(activity.root.value.getChildCount() === 2, 'YouTube hint removed ' + i);
                    if (c.fullscreen) {
                        const fullscreen = View.$new(activity);
                        activity.web.value.getWebChromeClient().onShowCustomView(fullscreen, null);
                        check(activity.fullscreen.value !== null, 'YouTube fullscreen active');
                    }
                }
                setTimeout(function () {
                    // Host sends adb shell input keyevent 4 when this marker appears.
                    console.log('READY_SYSTEM_BACK ' + i);
                    setTimeout(function () { main(function () {
                        check(activity.isFinishing() || activity.isDestroyed(), 'single system Back closes viewer ' + i);
                        activity.$dispose(); activity = null; run(i + 1);
                    }); }, 2200);
                }, 400);
            }});
        }); }, 1500);
    }
    run(0);
});
