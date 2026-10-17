// Isolated emulator only. Generated metadata, isolated preference file; NO HTTP or deletions.
Java.perform(function () {
    const app = Java.use('android.app.ActivityThread').currentApplication();
    if (String(app.getPackageName()) !== 'jp.co.airfront.android.a2chMate.haiagaru.uploadhosts191')
        throw Error('Isolated emulator upload-hosts app required');
    const testName = 'haiagaru.fixture-management-secret.' + Date.now();
    const Context = Java.registerClass({
        name: 'app.morphe.fixture.ManagementContext' + Date.now(),
        superClass: Java.use('android.content.ContextWrapper'),
        methods: { $init: [{
            returnType: 'void', argumentTypes: ['android.content.Context'],
            implementation: function (value) { this.$super.$init(value); }
        }], getSharedPreferences: [{
            returnType: 'android.content.SharedPreferences', argumentTypes: ['java.lang.String', 'int'],
            implementation: function (name, mode) { return app.getSharedPreferences(testName, 0); }
        }] }
    });
    const context = Context.$new(app);
    const Store = Java.use('app.morphe.extension.chmate.MediaUploadManagement');
    const History = Java.use('app.morphe.extension.chmate.NativeUploadHistory');
    const Json = Java.use('org.json.JSONObject');
    const Row = Java.use('o.onRewardedInterstitialAdDisplayed');
    const Request = Java.use('o.nm');
    let checks = 0, requests = 0;
    const blocked = Request.a.overload('[Ljava.lang.Object;');
    blocked.implementation = function () { requests++; throw Error('Test blocked native network request'); };
    function check(ok, label) { if (!ok) throw Error(label); checks++; console.log('PASS ' + label); }
    function result(slug) {
        return Json.$new(JSON.stringify({haiagaruProvider:'imgpile', link:'https://cdn.imgpile.com/f/Fixture.webp',
            haiagaruManagementUrl:'https://imgpile.com/m/' + slug}));
    }
    try {
        Store.remember(context, result('FixtureOne'));
        check(Store.entries(context).size() === 1, 'management metadata saved in Android preferences');
        Store.remember(context, result('FixtureOne'));
        check(Store.entries(context).size() === 1, 'same owner handle saved idempotently');
        Store.remember(context, result('FixtureTwo'));
        check(Store.entries(context).size() === 2, 'identical CDN URL retains distinct owner handles');
        Store.remember(context, Json.$new(JSON.stringify({haiagaruProvider:'imgbox',link:'https://images2.imgbox.com/ab/cd/Fixture_o.png',
            haiagaruManagementUrl:'https://imgbox.com/upload/edit/123/fixture-secret'})));
        check(Store.entries(context).size() === 3, 'imgbox management capability saved separately');
        Store.remember(context, Json.$new(JSON.stringify({haiagaruProvider:'imgbox',link:'https://images2.imgbox.com/ab/cd/Fixture_o.png',
            haiagaruManagementUrl:'https://evil.invalid/delete'})));
        check(Store.entries(context).size() === 3, 'untrusted management URL not saved');
        app.getSharedPreferences(testName,0).edit().putString('corrupt','not JSON').commit();
        check(Store.entries(context).size() === 3, 'corrupt preference ignored');
        const foreign = Row.$new('', '', 'https://images2.imgbox.com/ab/cd/Fixture_o.png', 'fixture-not-an-imgur-key', 1, null);
        check(History.delete(app, foreign) === false && requests === 0, 'foreign native deletion rejected before request construction');
        const unknown = Row.$new('', '', 'https://example.invalid/Fixture.png', 'fixture-not-an-imgur-key', 1, null);
        check(History.delete(app, unknown) === false && requests === 0, 'unknown server never gets Imgur deletion');
        console.log('PASS ' + checks + ' Android metadata/deletion-isolation tests; zero network requests');
    } finally {
        blocked.implementation = null;
        app.getSharedPreferences(testName,0).edit().clear().commit();
        console.log('Generated test preferences cleared; no user settings changed');
    }
});
