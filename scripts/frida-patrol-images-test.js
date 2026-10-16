/* Isolated patrol packages only, synthetic native responses and loopback images. */
'use strict';
Java.perform(function () {
    const Task = Java.registerClass({
        name: 'app.morphe.test.PatrolImagesTest' + Date.now(),
        implements: [Java.use('java.lang.Runnable')],
        methods: {run: function () { try {
    let app = Java.use('android.app.ActivityThread').currentApplication();
    for (let retry = 0; !app && retry < 100; retry++) {
        Java.use('java.lang.Thread').sleep(100);
        app = Java.use('android.app.ActivityThread').currentApplication();
    }
    if (!app) throw Error('Application did not initialize');
    // Application object exists before native onCreate has initialized its storage singleton.
    Java.use('java.lang.Thread').sleep(3000);
    const pkg = String(app.getPackageName());
    if (!/^jp\.co\.airfront\.android\.a2chMate\.haiagaru\.patrol(191|226|242)$/.test(pkg))
        throw Error('Isolated patrol package required');
    const version = pkg.slice(-3);
    const H = Java.use('app.morphe.extension.chmate.Haiagaru');
    H.shouldHideAds(app);
    const P = Java.use('app.morphe.extension.chmate.PatrolImages'); P.initialize(app);
    console.log('BACKGROUND ' + (Java.use('android.os.Looper').myLooper() === null));
    const config = String(P.bindings()).split(';');
    let checks = 0;
    function check(ok, label) { if (!ok) throw Error(label); checks++; console.log('PASS ' + version + ' ' + label); }
    const Board = Java.use('jp.syoboi.a2chMate.client.BBSUrlInfo');
    const board = Board.$new.overload('java.lang.String','java.lang.String','java.lang.Long','int','java.lang.Integer','java.lang.String')
        .call(Board, 'example.invalid', 'patrolfixture', Java.use('java.lang.Long').valueOf(1234567890), 0, null, null);
    const names = {
        '191':['o.processAdDisplayErrorPostbackForUserError','o.processAdapterInitializationPostback','o.o1b$RemoteActionCompatParcelizer'],
        '226':['o.BouncyCastleSocketAdapterCompanion','o.AndroidSocketAdapterCompanionfactory1','o.setLoop$ComponentActivity'],
        '242':['o.KeJ11','o.KeJ12','o.AvailabilityException']
    }[version];
    const Response = Java.use(names[0]);
    const image = 'http://127.0.0.1:18769/fixture-' + version + '-' + Date.now() + '.png';
    const response = version === '242'
        ? Response.$new.overload('int','java.lang.String','java.lang.String','java.lang.String','java.lang.String','java.lang.String','short','[I','int')
            .call(Response,1,'名前','sage','2026/10/17',image,'',0,Java.array('int',[]),0)
        : Response.$new.overload('int','java.lang.String','java.lang.String','java.lang.String','java.lang.String')
            .call(Response,1,'名前','sage','2026/10/17',image);
    const list = Java.use(names[1]).$new();
    list.title.value = '巡回画像テスト ' + version;
    list.add(response);
    const result = version === '191' ? Java.use(names[2]).$new(list,board,false) : Java.use(names[2]).$new(list,board);
    const prefs = app.getSharedPreferences('io.github.areteruhiro.chmate.haiagaru.ui-config',0);
    const Cache = Java.use(config[2]); let manager;
    for (let retry=0; !manager && retry<100; retry++) {
        try { manager=Cache[config[3]](app); }
        catch(error) { if(retry===99) throw error; Java.use('java.lang.Thread').sleep(100); }
    }
    function cached() {
        try { const input = manager[config[5]](image); const first = input.read(); input.close(); return first === 137; }
        catch (_) { return false; }
    }
    prefs.edit().putBoolean('patrolImagesEnabled',false).putString('patrolImageKeywords','巡回画像テスト')
        .putString('patrolImageUrls','').putInt('patrolImageDestination',0).commit();
    P.onThread(result,board);
    check(!cached(),'OFF does not fetch');
    prefs.edit().putBoolean('patrolImagesEnabled',true).putString('patrolImageKeywords','does-not-match').commit();
    P.onThread(result,board);
    check(!cached(),'unmatched thread does not fetch');
    prefs.edit().putString('patrolImageKeywords','巡回画像テスト').commit();
    P.onThread(result,board);
    console.log('NATIVE_ATTACHMENTS '+JSON.stringify(Array.from(response[config[1]]()).map(String)));
    console.log('STATUS '+P.status.value);
    check(cached(),'unopened thread stored in actual native image cache');
    P.onThread(result,board);
    check(cached(),'repeat patrol reuses native image cache');
    prefs.edit().putInt('patrolImageDestination',1).commit();
    P.onThread(result,board);
    const Rules = Java.use('app.morphe.extension.chmate.PatrolImageRules');
    const folder = Rules.folder('巡回画像テスト '+version,'http://example.invalid/test/read.cgi/patrolfixture/1234567890/');
    // The callback's canonical URL is authoritative; find its stored folder mapping.
    const all = prefs.getAll().entrySet().iterator(); let actualFolder;
    while(all.hasNext()) {
        const entry = Java.cast(all.next(), Java.use('java.util.Map$Entry'));
        if(String(entry.getKey()).startsWith('patrolImageFolder.')) actualFolder=String(entry.getValue());
    }
    check(!!actualFolder,'stable thread folder persisted');
    const hash = String(Rules.hash(image));
    check(P.publicExists(actualFolder,hash),'public Download image published');
    P.onThread(result,board);
    check(P.publicExists(actualFolder,hash),'repeat public save does not lose image');
    console.log('FIXTURE_IMAGE '+image);
    prefs.edit().putBoolean('patrolImagesEnabled',false).commit();
    console.log('PASS '+checks+' native patrol image checks '+version);
        } catch (error) { console.log('FAIL '+error.stack); } }}
    });
    Java.use('java.lang.Thread').$new(Task.$new()).start();
});
