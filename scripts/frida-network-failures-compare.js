/* Native transport comparison using loopback fixtures only.
 * No trust overrides, preference writes, uploads, external URLs or timeout edits.
 * Native Call factory is used directly: the optional 191 whole-call limit is not invoked.
 */
'use strict';
Java.perform(function () {
    const app = Java.use('android.app.ActivityThread').currentApplication();
    const pkg = String(app.getPackageName());
    const is191 = pkg === 'jp.co.airfront.android.a2chMate.haiagaru.network191';
    if (!is191 && pkg !== 'jp.co.airfront.android.a2chMate.haiagaru.uploadhistory226')
        throw Error('Isolated verification app required');
    const version = is191 ? '191' : '226';
    const System = Java.use('android.os.SystemClock');
    const client = is191 ? Java.use('o.getCredentials').d() : Java.use('o.isAvailable').d.overload().call(Java.use('o.isAvailable'));
    const Builder = Java.use(is191 ? 'o.r8lambda17vlhACr7B0IDgCdn1Q67LKhir8$setContentView' : 'o.zzdA$IconCompatParcelizer');
    const Call = Java.use(is191 ? 'o.putFloat' : 'o.MediationBannerAdCallback');
    function make(url) {
        const builder = Builder.$new();
        builder.b.overload('java.lang.String').call(builder, url);
        const request = is191 ? builder.c.overload().call(builder) : builder.a.overload().call(builder);
        return Call.$new(client, request, false);
    }
    function getBody(response) {
        return is191 ? String(Java.cast(response.c.value, Java.use('o.sExternalSyntheticLambda2')).e())
            : String(Java.cast(response.d.value, Java.use('o.forPackage')).string());
    }
    function errors(error) {
        const chain = [];
        try {
            let current = Java.cast(error, Java.use('java.lang.Throwable'));
            for (let n = 0; current !== null && n < 6; n++) {
                chain.push(String(current.getClass().getName()));
                current = current.getCause();
            }
        } catch (_) { chain.push(error.$className || 'unknown'); }
        return chain;
    }
    let passed = 0;
    function run(label, url, expectFailure) {
        let response, headersMs = null, body = null, exception = null;
        const start = Number(System.elapsedRealtime());
        try {
            const call = make(url);
            response = is191 ? call.e() : call.c();
            headersMs = Number(System.elapsedRealtime()) - start;
            body = getBody(response);
        } catch (error) { exception = errors(error); }
        finally { if (response) { try { response.close(); } catch (_) {} } }
        const elapsedMs = Number(System.elapsedRealtime()) - start;
        const expectedClass = label === 'untrusted-certificate'
            ? 'javax.net.ssl.SSLHandshakeException' : 'java.net.SocketTimeoutException';
        const ok = expectFailure ? exception !== null && exception[0] === expectedClass
            && (label === 'untrusted-certificate' || (elapsedMs >= 14000 && elapsedMs < 22000))
            && (label !== 'http-body-stalled' || headersMs !== null) : body === 'ok';
        if (ok) passed++;
        console.log(JSON.stringify({version: version, test: label, elapsedMs: elapsedMs,
            headersMs: headersMs, exception: exception, pass: ok}));
    }
    const cases = [
        ['untrusted-certificate', 'https://127.0.0.1:16883/subject.txt'],
        ['tls-handshake-no-response', 'https://127.0.0.1:16884/subject.txt'],
        ['http-headers-no-response', 'http://127.0.0.1:16882/headers/subject.txt'],
        ['http-body-stalled', 'http://127.0.0.1:16882/body/subject.txt']
    ];
    const tasks = [['baseline', 'http://127.0.0.1:16882/subject.txt', false]];
    cases.forEach(function (test) {
        tasks.push([test[0], test[1], true]);
        tasks.push(['recovery-after-' + test[0], 'http://127.0.0.1:16882/subject.txt', false]);
    });
    function next() {
        if (!tasks.length) {
            console.log('COMPLETE ' + version + ' ' + passed + '/9');
            return;
        }
        const task = tasks.shift();
        setTimeout(function () {
            Java.perform(function () { run(task[0], task[1], task[2]); next(); });
        }, 100);
    }
    next();
});
