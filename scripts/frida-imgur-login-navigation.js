// Observe navigation decisions only. No passwords, cookies, request bodies or query strings.
Java.perform(function () {
    Java.deoptimizeEverything();
    const app = Java.use('android.app.ActivityThread').currentApplication();
    if (String(app.getPackageName()) !== 'jp.co.airfront.android.a2chMate.haiagaru.imgurrecovery191') throw Error('Recovery191 required');
    const Policy = Java.use('app.morphe.extension.chmate.ImgurWebPolicy');
    const allowed = Policy.loginUrl.overload('java.lang.String');
    allowed.implementation = function (url) {
        const result = allowed.call(this, url);
        if (!result) {
            try {
                const uri = Java.use('android.net.Uri').parse(url);
                console.log('BLOCKED scheme=' + uri.getScheme() + ', host=' + uri.getHost()
                    + ', blank=' + (String(url) === 'about:blank'));
            } catch (_) { console.log('BLOCKED invalid navigation (URL not logged)'); }
        }
        return result;
    };
    console.log('Navigation tracing ready. User may retry Google Next. No URL queries or credentials are logged.');
    Java.scheduleOnMainThread(function () {
        Java.choose('app.morphe.extension.chmate.ImgurWebLoginActivity', {
            onMatch: function (activity) {
                if (activity.isFinishing() || activity.isDestroyed()) return;
                const web = activity.web.value, url = web.getUrl();
                console.log('Current URL allowed=' + Policy.loginUrl(url));
            }, onComplete: function () {}
        });
    });
});
