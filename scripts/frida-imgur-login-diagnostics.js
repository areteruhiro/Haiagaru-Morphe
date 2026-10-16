// Only coarse UI state is returned. Never output page text, identities, credentials or URLs.
Java.perform(function () {
    const app = Java.use('android.app.ActivityThread').currentApplication();
    if (String(app.getPackageName()) !== 'jp.co.airfront.android.a2chMate.haiagaru.imgurrecovery191') throw Error('Recovery191 required');
    const Callback = Java.registerClass({
        name: 'app.haiagaru.fixture.LoginDiagnosticCallback',
        implements: [Java.use('android.webkit.ValueCallback')],
        methods: { onReceiveValue: { returnType: 'void', argumentTypes: ['java.lang.Object'], implementation: function (value) { console.log('Login state: ' + value); } } }
    });
    Java.scheduleOnMainThread(function () {
        Java.choose('app.morphe.extension.chmate.ImgurWebLoginActivity', {
            onMatch: function (activity) {
                if (activity.isFinishing() || activity.isDestroyed()) return;
                for (let i = 0; i < activity.windows.value.size(); i++) {
                    const web = Java.cast(activity.windows.value.get(i), Java.use('android.webkit.WebView'));
                    web.evaluateJavascript('(function(){var t=document.body?document.body.innerText:"";return JSON.stringify({google:location.hostname==="accounts.google.com",imgur:/(^|\\.)imgur\\.com$/.test(location.hostname),ready:document.readyState,waiting:/しばらくお待ち|Please wait|One moment/i.test(t),blocked:/disallowed_useragent|unsupported browser|このブラウザ|安全でない|couldn.t sign you in/i.test(t),signedIn:/すでに.*ログイン|already signed in/i.test(t),passwordField:!!document.querySelector("input[type=password]"),opener:!!window.opener,openerClosed:window.opener?window.opener.closed:null,visibility:document.visibilityState})})()', Callback.$new());
                }
            }, onComplete: function () {}
        });
    });
});
