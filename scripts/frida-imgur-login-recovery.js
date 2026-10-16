// Fresh isolated emulator package only. No passwords, Google cookies or real authentication requests.
Java.perform(function () {
    Java.deoptimizeEverything();
    const app = Java.use('android.app.ActivityThread').currentApplication();
    if (String(app.getPackageName()) !== 'jp.co.airfront.android.a2chMate.haiagaru.imgurrecovery191') throw Error('Isolated recovery191 required');
    const H = Java.use('app.morphe.extension.chmate.ImgurWebUpload');
    const Activity = Java.use('app.morphe.extension.chmate.ImgurWebLoginActivity');
    const Web = Java.use('android.webkit.WebView');
    const prefs = H.prefs(app), had = prefs.contains('enabled'), original = prefs.getBoolean('enabled', false);
    let mode = 'fail', checks = 0, uploadNavigation = 0, activity = null;
    const load = Web.loadUrl.overload('java.lang.String');
    load.implementation = function (url) { if (String(url) === 'https://imgur.com/upload') uploadNavigation++; };
    H.checkAccount.implementation = function () {
        checks++;
        if (mode === 'fail') throw Java.use('java.io.IOException').$new('Fixture: Google login alone is not Imgur');
        return 'FixtureAccount';
    };
    function cleanup() {
        H.checkAccount.implementation = null; load.implementation = null;
        const e = prefs.edit(); if (had) e.putBoolean('enabled', original); else e.remove('enabled'); e.commit();
        if (activity) activity.finish();
        console.log('Login fixture restored; no credentials or real authentication requests');
    }
    function later(action) { setTimeout(function () { Java.scheduleOnMainThread(function () { try { action(); } catch (e) { cleanup(); throw e; } }); }, 500); }
    function popup() {
        const root = Java.cast(activity.windows.value.get(0), Web);
        const handler = Java.use('android.os.Handler').$new(Java.use('android.os.Looper').getMainLooper());
        const message = Java.use('android.os.Message').obtain.overload('android.os.Handler').call(Java.use('android.os.Message'), handler);
        const transport = Java.use('android.webkit.WebView$WebViewTransport').$new(root); message.obj.value = transport;
        if (!root.getWebChromeClient().onCreateWindow(root, false, true, message)) throw Error('Popup blocked');
        const child = transport.getWebView();
        child.getWebViewClient().onPageStarted(child, 'https://accounts.google.com/', null);
        if (root.getVisibility() !== 0 || root.getImportantForAccessibility() !== 4) throw Error('Opener visibility/accessibility regression');
        return root;
    }
    Java.scheduleOnMainThread(function () {
        prefs.edit().putBoolean('enabled', false).commit();
        const intent = Java.use('android.content.Intent').$new(app, Activity.class); intent.addFlags(0x10000000); app.startActivity(intent);
        later(function () {
            Java.choose(Activity.$className, { onMatch: found => { activity = Java.retain(found); return 'stop'; }, onComplete: () => {} });
            if (!activity) throw Error('No login activity');
            popup(); activity.checkLoginCompletion(true);
            later(function () {
                if (activity.windows.value.size() !== 2 || prefs.getBoolean('enabled', true)) throw Error('Google-only login was treated as Imgur login');
                console.log('PASS incomplete Imgur session preserves Google window and does not enable uploads');
                mode = 'success'; activity.checkLoginCompletion(true);
                later(function () {
                    if (activity.windows.value.size() !== 1 || uploadNavigation !== 1 || prefs.getBoolean('enabled', true)) throw Error('Manual return verification failed: windows=' + activity.windows.value.size() + ', uploadNavigation=' + uploadNavigation + ', enabled=' + prefs.getBoolean('enabled', true) + ', checks=' + checks + ', checking=' + activity.checkingCompletion.value + ', status=' + activity.status.value.getText());
                    console.log('PASS confirmed Imgur session returns without closing the app or enabling uploads implicitly');
                    const root = popup(), before = checks;
                    root.getWebViewClient().onPageFinished(root, 'https://imgur.com/signin');
                    if (checks !== before) throw Error('Initial sign-in page mistaken for callback');
                    root.getWebViewClient().onPageFinished(root, 'https://imgur.com/signin/google');
                    later(function () {
                        if (activity.windows.value.size() !== 1 || uploadNavigation !== 2 || prefs.getBoolean('enabled', true)) throw Error('Callback recovery failed');
                        console.log('PASS verified callback restores opener; default sign-in page does not finish OAuth');
                        popup(); activity.verify();
                        later(function () {
                            if (activity.windows.value.size() !== 1 || !prefs.getBoolean('enabled', false)) throw Error('Explicit enable does not return from provider window');
                            console.log('PASS explicit verify enables uploads only after Imgur account verification and closes provider popup');
                            cleanup();
                        });
                    });
                });
            });
        });
    });
});
