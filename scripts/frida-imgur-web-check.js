// Isolated test package only. No account credentials, cookies or uploads are logged/sent.
globalThis.openImgurLogin = function () {
    Java.perform(function () {
        var app = Java.use('android.app.ActivityThread').currentApplication();
        if (app.getPackageName().toString().indexOf('.haiagaru.imgur') === -1) throw new Error('Isolated test package required');
        Java.scheduleOnMainThread(function () {
            var intent = Java.use('android.content.Intent').$new(app,
                Java.use('app.morphe.extension.chmate.ImgurWebLoginActivity').class);
            intent.addFlags(0x10000000); app.startActivity(intent);
        });
    });
};
globalThis.inspectImgurLogin = function () {
    Java.perform(function () {
        Java.choose('app.morphe.extension.chmate.ImgurWebLoginActivity', {
            onMatch: function (activity) {
                Java.scheduleOnMainThread(function () {
                    var fields = activity.getClass().getDeclaredFields();
                    for (var i = 0; i < fields.length; i++) {
                        if (fields[i].getType().getName().toString() !== 'android.webkit.WebView') continue;
                        fields[i].setAccessible(true);
                        var view = Java.cast(fields[i].get(activity), Java.use('android.webkit.WebView'));
                        console.log('Login page progress=' + view.getProgress() + ', title=' + view.getTitle());
                    }
                });
            }, onComplete: function () {}
        });
    });
};
Java.perform(function () {
    var ActivityThread = Java.use('android.app.ActivityThread');
    var app = ActivityThread.currentApplication();
    var pkg = app.getPackageName().toString();
    if (pkg.indexOf('.haiagaru.imgur') === -1) throw new Error('Isolated Imgur test package required');
    var Upload = Java.use('app.morphe.extension.chmate.ImgurWebUpload');
    var Json = Java.use('org.json.JSONObject');
    var Cls = Java.use('java.lang.Class');
    var ver = app.getPackageManager().getPackageInfo(pkg, 0).versionName.value.toString();
    var response = ver.indexOf('191') !== -1 ? 'o.r0ExternalSyntheticLambda13'
        : ver.indexOf('226') !== -1 ? 'o.Ad' : ver.indexOf('241') !== -1 ? 'o.defaultzzb' : 'o.zzbtj';
    var data = Json.$new('{"id":"Test","deletehash":"TestDeleteHash","link":"https://i.imgur.com/Test.png","account_id":1,"account_url":"test"}');
    var result = Upload.nativeResponse(Cls.forName(response, true, app.getClassLoader()), data);
    if (result === null || result.toString().indexOf('https://i.imgur.com/Test.png') === -1)
        throw new Error('Native response link was not preserved');
    if (Upload.enabled()) throw new Error('Test mode unexpectedly enabled');
    console.log('PASS ' + ver + ': default disabled, native result constructor/link mapping');
    if (ver.indexOf('241') !== -1 || ver.indexOf('242') !== -1) {
        var StringType = Java.use('java.lang.String');
        var trim = Cls.forName('java.lang.String').getMethod('trim', Java.array('java.lang.Class', []));
        var value = ver.indexOf('241') !== -1
            ? Upload.invoke241(trim, StringType.$new(' test '), Java.array('java.lang.Object', []))
            : Upload.invoke242(trim, StringType.$new(' test '), Java.array('java.lang.Object', []));
        if (value.toString() !== 'test') throw new Error('Disabled native invocation changed');
        console.log('PASS ' + ver + ': disabled mode preserves original reflection result');
    }
    var Cookie = Java.use('android.webkit.CookieManager');
    var cookies = Cookie.getInstance().getCookie('https://api.imgur.com/');
    if (cookies !== null && cookies.toString().length > 0) {
        console.log('SKIP unauthenticated check: test WebView already has cookies');
    } else {
        try { Upload.checkAccount(); throw new Error('Unexpected authenticated account'); }
        catch (e) {
            if (String(e).indexOf('匿名送信は行いません') === -1) throw e;
            console.log('PASS ' + ver + ': missing session rejected before network');
        }
        var File = Java.use('java.io.File');
        var Output = Java.use('java.io.FileOutputStream');
        var testFile = File.$new(app.getCacheDir(), 'haiagaru-imgur-no-session-test');
        try {
            var output = Output.$new(testFile); output.write(1); output.close();
            try {
                Upload.upload(Java.array('java.lang.Object', [testFile]), Cls.forName(response, true, app.getClassLoader()));
                throw new Error('Unexpected upload without a session');
            } catch (e) {
                if (String(e).indexOf('匿名送信は行いません') === -1) throw e;
                console.log('PASS ' + ver + ': upload preflight reaches session rejection without sending media');
            }
        } finally { testFile.delete(); }
    }
});
