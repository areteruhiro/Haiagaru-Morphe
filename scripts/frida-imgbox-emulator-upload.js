// Explicit live test: invoke uploadOneImgboxFixture() only with user authorization.
// Sends one generated 64x64 PNG through the installed 191 uploader wrapper.
// Never reads user media, API keys, session values or foreign account identities.
globalThis.uploadOneImgboxFixture = function () {
    Java.perform(function () {
        const app = Java.use('android.app.ActivityThread').currentApplication();
        if (String(app.getPackageName()) !== 'jp.co.airfront.android.a2chMate.haiagaru.uploadhosts191')
            throw Error('Isolated upload-hosts 191 verification app required');
        const Looper = Java.use('android.os.Looper');
        if (Looper.myLooper() !== null && Looper.myLooper().equals(Looper.getMainLooper()))
            throw Error('Network test must run off the main thread');
        const client = Java.use('app.morphe.extension.chmate.MediaUploadClient');
        const readBody = client.body.overload('java.net.HttpURLConnection', 'boolean');
        readBody.implementation = function (connection, sent) {
            const url = connection.getURL();
            console.log('HTTP ' + connection.getRequestMethod() + ' ' + url.getHost() + url.getPath()
                + ' -> ' + connection.getResponseCode() + ' (mediaSent=' + sent + ')');
            return readBody.call(this, connection, sent);
        };
        const prefs = app.getSharedPreferences('haiagaru_upload_hosts', 0);
        const hadHost = prefs.contains('imageHost'), oldHost = prefs.getString('imageHost', 'native');
        const hadAdult = prefs.contains('imgboxAdult'), oldAdult = prefs.getBoolean('imgboxAdult', false);
        const file = Java.use('java.io.File').$new(app.getCacheDir(), 'haiagaru-imgbox-synthetic-test.png');
        const Bitmap = Java.use('android.graphics.Bitmap');
        const bitmap = Bitmap.createBitmap(64, 64, Java.use('android.graphics.Bitmap$Config').ARGB_8888.value);
        let output = null;
        try {
            bitmap.eraseColor(-12290134);
            output = Java.use('java.io.FileOutputStream').$new(file);
            if (!bitmap.compress(Java.use('android.graphics.Bitmap$CompressFormat').PNG.value, 100, output))
                throw Error('Fixture PNG encoding failed');
            output.close(); output = null;
            if (!prefs.edit().putString('imageHost', 'imgbox').putBoolean('imgboxAdult', false).commit())
                throw Error('Cannot set temporary upload destination');
            console.log('BEGIN one synthetic PNG (' + file.length() + ' bytes), no automatic retry');
            const response = Java.use('app.morphe.extension.chmate.Haiagaru')
                .uploadLegacyImage(Java.array('java.lang.Object', [file]));
            const data = response.getClass().getField('d').get(response);
            const link = String(data.getClass().getField('d').get(data));
            if (!/^https:\/\/images\d*\.imgbox\.com\/[A-Za-z0-9/_.-]+\.png$/.test(link))
                throw Error('No valid native PNG result; do not resend');
            globalThis.lastImgboxFixtureUrl = link;
            console.log('PASS Android 191 uploader native response: ' + link);
        } catch (error) {
            console.log('FAIL Android upload; no retry: ' + error);
        } finally {
            if (output !== null) output.close();
            bitmap.recycle(); file.delete();
            const restore = prefs.edit();
            if (hadHost) restore.putString('imageHost', oldHost); else restore.remove('imageHost');
            if (hadAdult) restore.putBoolean('imgboxAdult', oldAdult); else restore.remove('imgboxAdult');
            restore.commit();
            readBody.implementation = null;
            console.log('Temporary fixture removed; upload settings restored; probe detached');
        }
    });
};
