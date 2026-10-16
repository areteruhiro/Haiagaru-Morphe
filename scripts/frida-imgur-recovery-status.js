// Read-only recovery verification. Never print identities, cookies, credentials or URL queries.
Java.perform(function () {
    const app = Java.use('android.app.ActivityThread').currentApplication();
    if (String(app.getPackageName()) !== 'jp.co.airfront.android.a2chMate.haiagaru.imgurrecovery191')
        throw Error('Isolated recovery191 required');
    const upload = Java.use('app.morphe.extension.chmate.ImgurWebUpload');
    console.log('Patch supported=' + upload.supported(app) + ', upload enabled=' + upload.enabled());
    Java.scheduleOnMainThread(function () {
        Java.choose('app.morphe.extension.chmate.ImgurWebLoginActivity', {
            onMatch: function (activity) {
                if (activity.isFinishing() || activity.isDestroyed()) return;
                const web = activity.web.value;
                const url = web.getUrl();
                const host = url === null ? '(none)' : Java.use('android.net.Uri').parse(url).getHost();
                console.log('Login window count=' + activity.windows.value.size()
                    + ', active host=' + host + ', page progress=' + web.getProgress()
                    + ', Google flow=' + activity.googleFlow.value);
            }, onComplete: function () {}
        });
    });
    try {
        upload.checkAccount();
        console.log('PASS Imgur account authenticated (identity not logged)');
    } catch (error) {
        console.log('Imgur account verification failed (response and identity not logged)');
    }
});
