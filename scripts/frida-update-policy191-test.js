// Isolated test app and local fixture only. Does not post or contact public servers.
Java.perform(function () {
    const app = Java.use('android.app.ActivityThread').currentApplication();
    if (String(app.getPackageName()) !== 'jp.co.airfront.android.a2chMate.haiagaru.network191')
        throw Error('Isolated network191 app required');
    const Policy = Java.use('app.morphe.extension.chmate.NativeUpdatePolicy');
    const H = Java.use('app.morphe.extension.chmate.Haiagaru');
    H.shouldHideAds(app); // establish context for this test app, no preference reset
    const prefs = app.getSharedPreferences('io.github.areteruhiro.chmate.haiagaru.ui-config', 0);
    const keys = ['optimize191UpdateProgress', 'update191TimeoutSeconds'];
    const old = keys.map(key => ({ key: key, present: prefs.contains(key),
        value: key === keys[0] ? prefs.getBoolean(key, true) : prefs.getInt(key, 0) }));
    let checks = 0, callbacks = 0, last = 0, total = 0;
    function check(ok, label) { if (!ok) throw Error(label); checks++; console.log('PASS ' + label); }
    const Listener = Java.registerClass({ name: 'app.morphe.test.UpdateProgressListener191',
        implements: [Java.use('o.r8lambdamHWy7omJVz7G9M7u2KSo5IaRp90')], methods: {
            c: function (current, count) { callbacks++; last = current; total = count; },
            e: function (call) { }
        } });
    const Client = Java.use('o.getCredentials').d();
    const Builder = Java.use('o.r8lambda17vlhACr7B0IDgCdn1Q67LKhir8$setContentView');
    const Call = Java.use('o.putFloat');
    const Timeout = Java.use('o.StringUtils');
    const Unit = Java.use('java.util.concurrent.TimeUnit');
    function callFor(url) {
        const builder = Builder.$new(); builder.b.overload('java.lang.String').call(builder, url);
        return Call.$new(Client, builder.c.overload().call(builder), false);
    }
    function timeout(call) { return Java.cast(call.l.value, Timeout); }
    function burst(enabled) {
        prefs.edit().putBoolean(keys[0], enabled).commit();
        const response = callFor('http://127.0.0.1:16881/burst/subject.txt').e();
        callbacks = 0; last = 0; total = 0;
        const stream = Java.use('o.getCredentials').d.overload(
            'o.r8lambda4sy1jIZDDj0VvqfE_7RbnJrDAgk', 'o.r8lambdamHWy7omJVz7G9M7u2KSo5IaRp90')
            .call(Java.use('o.getCredentials'), response, Listener.$new());
        const buffer = Java.array('byte', [0, 0, 0, 0]);
        let bytes = 0, count;
        try { while ((count = stream.read.overload('[B', 'int', 'int').call(stream, buffer, 0, 4)) !== -1) bytes += count; }
        finally { stream.close(); response.close(); }
        check(bytes === 4096, 'all body bytes retained, optimization=' + enabled);
        check(last === total && total > 0, 'final progress retained, optimization=' + enabled);
        return callbacks;
    }
    try {
        const ordinary = burst(false), optimized = burst(true);
        check(optimized < ordinary / 2, 'native progress callback coalescing: ' + ordinary + ' -> ' + optimized);
        prefs.edit().putInt(keys[1], 60).commit();
        check(Number(Policy.timeoutNanos('GET', 'https://example.invalid/board/subject.txt')) === 60000000000, 'board GET timeout');
        for (const url of ['https://example.invalid/a.jpg', 'https://example.invalid/a.mp4',
                'https://example.invalid/test/bbs.cgi', 'file:///board/subject.txt'])
            check(Number(Policy.timeoutNanos('GET', url)) === 0, 'excluded non-thread target');
        check(Number(Policy.timeoutNanos('POST', 'https://example.invalid/board/subject.txt')) === 0, 'posting excluded');
        const native = callFor('http://127.0.0.1:16881/slow/subject.txt');
        Policy.configure191(native);
        check(Number(timeout(native).B_()) === 60000000000, 'native timeout bridge applies 60 seconds');
        timeout(native).a.overload('long', 'java.util.concurrent.TimeUnit').call(timeout(native), 2, Unit.SECONDS.value);
        Policy.configure191(native);
        check(Number(timeout(native).B_()) === 2000000000, 'shorter existing timeout preserved');
        const start = Date.now(); let failed = false, response;
        try { response = native.e(); Java.cast(response.c.value, Java.use('o.sExternalSyntheticLambda2')).e(); }
        catch (_) { failed = true; }
        finally { if (response) { try { response.close(); } catch (_) {} } }
        const elapsed = Date.now() - start;
        check(failed && elapsed >= 1200 && elapsed < 6000, 'continuously receiving body interrupted by call timeout (' + elapsed + ' ms)');
        const next = callFor('http://127.0.0.1:16881/subject.txt');
        Policy.configure191(next);
        const ok = next.e();
        try { check(String(Java.cast(ok.c.value, Java.use('o.sExternalSyntheticLambda2')).e()) === 'ok', 'next update remains usable'); }
        finally { ok.close(); }
        const image = callFor('http://127.0.0.1:16881/image.jpg');
        Policy.configure191(image);
        check(Number(timeout(image).B_()) === 0, 'native media call unchanged');
        prefs.edit().putInt(keys[1], 0).commit();
        const disabled = callFor('http://127.0.0.1:16881/subject.txt');
        Policy.configure191(disabled);
        check(Number(timeout(disabled).B_()) === 0, 'timeout opt-out retains original policy');
        console.log('PASSED ' + checks + ' update policy checks');
    } finally {
        const editor = prefs.edit();
        old.forEach(item => {
            if (!item.present) editor.remove(item.key);
            else if (item.key === keys[0]) editor.putBoolean(item.key, item.value);
            else editor.putInt(item.key, item.value);
        }); editor.commit();
    }
});
