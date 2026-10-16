// TEMPORARY isolated-package diagnostic. Never ship the unrestricted navigation policy.
// Clears this test app's WebView site data only; no browser/other application data is touched.
Java.perform(function () {
    Java.deoptimizeEverything();
    const app = Java.use('android.app.ActivityThread').currentApplication();
    if (String(app.getPackageName()) !== 'jp.co.airfront.android.a2chMate.haiagaru.imgurrecovery191') throw Error('Recovery191 required');
    const Uri = Java.use('android.net.Uri');
    const Policy = Java.use('app.morphe.extension.chmate.ImgurWebPolicy');
    Policy.loginUrl.implementation = function (url) {
        try {
            const uri = Uri.parse(url), scheme = String(uri.getScheme()), host = uri.getHost();
            const allow = String(url) === 'about:blank'
                || (scheme === 'https' || scheme === 'http') && host !== null && uri.getUserInfo() === null;
            console.log('Navigation scheme=' + scheme + ', host=' + host + ', allowed=' + allow);
            return allow;
        } catch (_) { return false; }
    };
    Policy.imgurLoginUrl.implementation = function (url) {
        try {
            const uri = Uri.parse(url), host = String(uri.getHost());
            return String(uri.getScheme()) === 'https' && uri.getUserInfo() === null
                && (uri.getPort() === -1 || uri.getPort() === 443)
                && (host === 'imgur.com' || host === 'www.imgur.com');
        } catch (_) { return false; }
    };
    const Callback = Java.registerClass({
        name: 'app.haiagaru.fixture.ClearedLoginCookies',
        implements: [Java.use('android.webkit.ValueCallback')],
        methods: { onReceiveValue: { returnType: 'void', argumentTypes: ['java.lang.Object'], implementation: function (_) {
            Java.scheduleOnMainThread(function () {
                Java.choose('app.morphe.extension.chmate.ImgurWebLoginActivity', {
                    onMatch: function (a) {
                        if (a.isFinishing() || a.isDestroyed()) return;
                        a.loginGeneration.value++; a.verificationGeneration.value++; a.uploadsGeneration.value++;
                        a.checkingCompletion.value = false; a.googleFlow.value = false;
                        const verifyField = a.getClass().getDeclaredField('verify'); verifyField.setAccessible(true);
                        Java.cast(verifyField.get(a), Java.use('android.widget.Button')).setEnabled(true);
                        while (a.windows.value.size() > 1) a.closePopup(a.web.value);
                        a.setChecked(false);
                        Java.use('app.morphe.extension.chmate.ImgurWebUpload').prefs(app).edit().putBoolean('enabled', false).apply();
                        a.uploadsView.value.setVisibility(8);
                        const web = a.web.value;
                        web.stopLoading(); web.clearCache(true); web.clearHistory(); web.clearFormData();
                        web.setVisibility(0); web.setImportantForAccessibility(0);
                        a.status.value.setText.overload('java.lang.CharSequence').call(a.status.value,
                            Java.use('java.lang.String').$new('検証中：サイトデータ削除済み。HTTP/HTTPSの移動制限を一時解除しています。Googleログインを操作してください。'));
                        web.loadUrl('https://imgur.com/signin');
                        console.log('PASS test-app WebView site data cleared; login reopened. No credentials read.');
                        return 'stop';
                    }, onComplete: function () {}
                });
            });
        } } }
    });
    Java.scheduleOnMainThread(function () {
        Java.use('android.webkit.WebStorage').getInstance().deleteAllData();
        const manager = Java.use('android.webkit.CookieManager').getInstance();
        manager.removeAllCookies(Callback.$new()); manager.flush();
    });
});
