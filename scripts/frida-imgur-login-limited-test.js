// Apply only the candidate's exact HTTPS host policy to the isolated app, without clearing login.
Java.perform(function () {
    const app = Java.use('android.app.ActivityThread').currentApplication();
    if (String(app.getPackageName()) !== 'jp.co.airfront.android.a2chMate.haiagaru.imgurrecovery191') throw Error('Recovery191 required');
    const Policy = Java.use('app.morphe.extension.chmate.ImgurWebPolicy');
    const URI = Java.use('java.net.URI');
    function kind(url) {
        try {
            const uri = URI.$new(url), host = String(uri.getHost());
            if (String(uri.getScheme()) !== 'https' || uri.getRawUserInfo() !== null
                    || (uri.getPort() !== -1 && uri.getPort() !== 443)) return 0;
            if (host === 'imgur.com' || host === 'www.imgur.com') return 1;
            if (host === 'accounts.google.com' || host === 'accounts.google.co.jp' || host === 'accounts.youtube.com') return 2;
        } catch (_) {}
        return 0;
    }
    Policy.loginUrl.implementation = function (url) { return kind(url) !== 0; };
    Policy.googleLoginUrl.implementation = function (url) { return kind(url) === 2; };
    Policy.imgurLoginUrl.implementation = function (url) { return kind(url) === 1; };
    console.log('Limited exact-host HTTPS policy active; login data preserved, no credentials or uploads accessed.');
});
