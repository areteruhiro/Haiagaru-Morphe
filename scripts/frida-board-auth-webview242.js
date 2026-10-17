/* Read-only WebView diagnosis. No field values, Cookies, auth codes, or query strings. */
Java.perform(function () {
    const app = Java.use('android.app.ActivityThread').currentApplication();
    if (String(app.getPackageName()) !== 'jp.co.airfront.android.a2chMate.haiagaru.responsearchive242')
        throw Error('Verification 242 only');
    const Callback = Java.registerClass({
        name: 'app.morphe.verification.AuthPageInfo' + Date.now(),
        implements: [Java.use('android.webkit.ValueCallback')],
        methods: { onReceiveValue: function (value) { console.log('PAGE_FLAGS ' + String(value)); } }
    });
    Java.choose('android.webkit.WebView', {onMatch: function (view) {
        const retained = Java.retain(view);
        Java.scheduleOnMainThread(function () {
            if (!retained.isShown()) return;
            const settings = retained.getSettings();
            console.log('WEBVIEW js=' + settings.getJavaScriptEnabled() + '; dom=' + settings.getDomStorageEnabled()
                + '; ua=' + settings.getUserAgentString());
            retained.evaluateJavascript("JSON.stringify({ready:document.readyState,title:document.title,bodyLength:(document.body?.innerText||'').length,suspicious:/(不審|suspicious)/i.test(document.body?.innerText||''),denied:/(許可されていません|拒否|forbidden|access denied)/i.test(document.body?.innerText||''),turnstile:!!document.querySelector('[src*=turnstile],[src*=challenges.cloudflare],.cf-turnstile'),recaptcha:!!document.querySelector('[src*=recaptcha],.g-recaptcha'),forms:document.forms.length,inputs:Array.from(document.querySelectorAll('input')).map(x=>({type:x.type,name:x.name})),scripts:Array.from(document.scripts).filter(x=>x.src).map(x=>{try{let u=new URL(x.src);return u.origin+u.pathname}catch(e){return 'invalid'}})})", Callback.$new());
        });
    },onComplete: function () {}});
});
