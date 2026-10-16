/* Isolated patched191 app + loopback failure fixtures only. Restores the option after each test. */
'use strict';
Java.perform(function () {
    const app = Java.use('android.app.ActivityThread').currentApplication();
    if (String(app.getPackageName()) !== 'jp.co.airfront.android.a2chMate.haiagaru.network191')
        throw Error('Isolated network191 app required');
    const H = Java.use('app.morphe.extension.chmate.Haiagaru');
    H.shouldHideAds(app);
    const Policy = Java.use('app.morphe.extension.chmate.NativeUpdatePolicy');
    const prefs = app.getSharedPreferences('io.github.areteruhiro.chmate.haiagaru.ui-config', 0);
    const key = 'fast191UpdateConnect';
    const System = Java.use('android.os.SystemClock');
    const Inet = Java.use('java.net.InetAddress');
    const lookup = Inet.getAllByName.overload('java.lang.String');
    const good = Inet.getByAddress.overload('[B').call(Inet, Java.array('byte', [127,0,0,1]));
    const bad = Inet.getByAddress.overload('[B').call(Inet, Java.array('byte', [127,0,0,2]));
    const parent = Java.use('o.getCredentials').d();
    const ClientBuilder = Java.use('o.r8lambdaz0gPFulMuhJ_LGn4qb5HDvuDsis$read');
    const Client = Java.use('o.r8lambdaz0gPFulMuhJ_LGn4qb5HDvuDsis');
    const Request = Java.use('o.r8lambda17vlhACr7B0IDgCdn1Q67LKhir8$setContentView');
    const Call = Java.use('o.putFloat');
    const Proxy = Java.use('java.net.Proxy');
    const runId = String(Date.now());
    let current = null, passed = 0;
    lookup.implementation = function (host) {
        if (!/^haiagaru-race-[a-z0-9-]+\.invalid$/.test(String(host))) return lookup.call(Inet, host);
        current.dnsOrder = current.test.order;
        return Java.array('java.net.InetAddress', current.test.order.map(x => x === 'good' ? good : bad));
    };
    const connect = Java.use('java.net.Socket').connect.overload('java.net.SocketAddress', 'int');
    connect.implementation = function (address, timeout) {
        let trace;
        const target = Java.cast(address, Java.use('java.net.InetSocketAddress'));
        if (current && [16881,16883].indexOf(target.getPort()) >= 0) {
            trace = {ip: String(target.getAddress().getHostAddress()),
                startMs: Number(System.elapsedRealtime()) - current.start, timeoutMs: timeout};
            current.attempts.push(trace);
            if (current.test.cancel && trace.ip === '127.0.0.2') current.call.a.overload().call(current.call);
        }
        try { return connect.call(this, address, timeout); }
        finally { if (trace) trace.endMs = Number(System.elapsedRealtime()) - current.start; }
    };
    const tasks = [
        {label:'on-bad-first-1', enabled:true, order:['bad','good'], race:true},
        {label:'off-bad-first', enabled:false, order:['bad','good'], slow:true},
        {label:'on-bad-first-2', enabled:true, order:['bad','good'], race:true},
        {label:'on-good-first', enabled:true, order:['good','bad']},
        {label:'media-excluded', enabled:true, order:['bad','good'], media:true, slow:true},
        {label:'single-route-unchanged', enabled:true, order:['bad'], slow:true, error:'java.net.SocketTimeoutException'},
        {label:'tls-still-verifies', enabled:true, order:['bad','good'], tls:true, error:'javax.net.ssl.SSLHandshakeException'},
        {label:'native-cancel', enabled:true, order:['bad','good'], cancel:true, error:'cancel'}
    ];
    function run(test) {
        const existed = prefs.contains(key), saved = prefs.getBoolean(key, true);
        let response, body = null, exception = null;
        const record = {test:test, start:Number(System.elapsedRealtime()), attempts:[]};
        current = record;
        try {
            prefs.edit().putBoolean(key, test.enabled).commit();
            if (H.fast191UpdateConnect() !== test.enabled) throw Error('Option did not apply');
            const builder = ClientBuilder.$new(parent);
            builder.u.value = Proxy.NO_PROXY.value;
            const client = Client.$new(builder);
            const request = Request.$new();
            const url = (test.tls ? 'https://' : 'http://') + 'haiagaru-race-' + test.label + '-' + runId
                + '.invalid:' + (test.tls ? '16883' : '16881') + (test.media ? '/image.jpg' : '/subject.txt');
            request.b.overload('java.lang.String').call(request, url);
            record.call = Call.$new(client, request.c.overload().call(request), false);
            response = record.call.e();
            body = String(Java.cast(response.c.value, Java.use('o.sExternalSyntheticLambda2')).e());
        } catch (error) { exception = error.$className || String(error); }
        finally {
            if (response) try { response.close(); } catch (_) {}
            const editor = prefs.edit();
            if (existed) editor.putBoolean(key,saved); else editor.remove(key);
            editor.commit();
        }
        const elapsedMs = Number(System.elapsedRealtime()) - record.start;
        const badAttempt = record.attempts.find(x => x.ip === '127.0.0.2');
        const goodAttempt = record.attempts.find(x => x.ip === '127.0.0.1');
        let ok = test.error ? exception !== null : body === 'ok' && exception === null;
        // Frida may wrap a Java exception as Error rather than exposing $className.
        if (test.error && test.error !== 'cancel') ok = ok &&
            (exception === test.error || exception.indexOf('Error: ' + test.error + ':') === 0);
        if (test.race) ok = ok && elapsedMs < 2000 && badAttempt && goodAttempt
            && goodAttempt.startMs >= 180 && goodAttempt.startMs < badAttempt.endMs;
        if (test.slow) ok = ok && elapsedMs >= 14000 && elapsedMs < 22000;
        if (test.tls || test.cancel) ok = ok && elapsedMs < 2000;
        if (ok) passed++;
        console.log(JSON.stringify({test:test.label, enabled:test.enabled, elapsedMs:elapsedMs,
            dnsOrder:record.dnsOrder, attempts:record.attempts, exception:exception, pass:Boolean(ok)}));
        current = null;
    }
    function next() {
        if (!tasks.length) { console.log('COMPLETE native191 TCP race ' + passed + '/8'); return; }
        const test = tasks.shift();
        setTimeout(function () { Java.perform(function () { run(test); next(); }); },300);
    }
    next();
});
