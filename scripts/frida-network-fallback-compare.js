/* Isolated apps; cloned clients and synthetic DNS; loopback fixture only.
 * The host must DROP 127.0.0.2:16881 SYNs for this experiment.
 * Does not mutate shared client policy, preferences, trust, or native timeouts.
 */
'use strict';
Java.perform(function () {
    console.log('SETUP app');
    const pkg = String(Java.use('android.app.ActivityThread').currentApplication().getPackageName());
    const is191 = pkg === 'jp.co.airfront.android.a2chMate.haiagaru.network191';
    if (!is191 && pkg !== 'jp.co.airfront.android.a2chMate.haiagaru.uploadhistory226')
        throw Error('Isolated verification app required');
    const version = is191 ? '191' : '226';
    const fixtureRun = String(Date.now());
    const System = Java.use('android.os.SystemClock');
    const Inet = Java.use('java.net.InetAddress');
    const Proxy = Java.use('java.net.Proxy');
    const parent = is191 ? Java.use('o.getCredentials').d()
        : Java.use('o.isAvailable').d.overload().call(Java.use('o.isAvailable'));
    console.log('SETUP native client');
    const Client = Java.use(is191 ? 'o.r8lambdaz0gPFulMuhJ_LGn4qb5HDvuDsis' : 'o.zzdE');
    const Request = Java.use(is191 ? 'o.r8lambda17vlhACr7B0IDgCdn1Q67LKhir8$setContentView'
        : 'o.zzdA$IconCompatParcelizer');
    const Call = Java.use(is191 ? 'o.putFloat' : 'o.MediationBannerAdCallback');
    const good = Inet.getByAddress.overload('[B').call(Inet, Java.array('byte', [127, 0, 0, 1]));
    const bad = Inet.getByAddress.overload('[B').call(Inet, Java.array('byte', [127, 0, 0, 2]));
    let order = [], current = null, passed = 0;
    console.log('SETUP addresses');
    const lookup = Inet.getAllByName.overload('java.lang.String');
    lookup.implementation = function (host) {
        if (!/^haiagaru-fallback-[a-z0-9-]+\.invalid$/.test(String(host))) return lookup.call(Inet, host);
        if (current) current.dnsOrder = order.slice();
        return Java.array('java.net.InetAddress', order.map(function (item) {
            return item === 'good' ? good : bad;
        }));
    };
    console.log('SETUP DNS');
    const builder = is191 ? Java.use('o.r8lambdaz0gPFulMuhJ_LGn4qb5HDvuDsis$read').$new(parent)
        : parent.b.overload().call(parent);
    builder[is191 ? 'u' : 'x'].value = Proxy.NO_PROXY.value;
    const client = Client.$new(builder);
    console.log('SETUP cloned client');
    const connect = Java.use('java.net.Socket').connect.overload('java.net.SocketAddress', 'int');
    connect.implementation = function (address, timeout) {
        let trace = null;
        try {
            const addr = Java.cast(address, Java.use('java.net.InetSocketAddress'));
            if (current && addr.getPort() === 16881) {
                trace = {ip: String(addr.getAddress().getHostAddress()),
                    startMs: Number(System.elapsedRealtime()) - current.start, timeoutMs: timeout};
                current.attempts.push(trace);
            }
        } catch (_) {}
        try { return connect.call(this, address, timeout); }
        finally { if (trace) trace.endMs = Number(System.elapsedRealtime()) - current.start; }
    };
    function run(label, addresses) {
        order = addresses;
        current = {start: Number(System.elapsedRealtime()), attempts: []};
        const record = current;
        let response, body = null, error = null;
        try {
            const request = Request.$new();
            request.b.overload('java.lang.String').call(request,
                'http://haiagaru-fallback-' + label + '-' + fixtureRun + '.invalid:16881/subject.txt');
            const built = is191 ? request.c.overload().call(request) : request.a.overload().call(request);
            const call = Call.$new(client, built, false);
            response = is191 ? call.e() : call.c();
            body = is191 ? String(Java.cast(response.c.value, Java.use('o.sExternalSyntheticLambda2')).e())
                : String(Java.cast(response.d.value, Java.use('o.forPackage')).string());
        } catch (exception) { error = exception.$className || 'JavaException'; }
        finally { if (response) { try { response.close(); } catch (_) {} } }
        const elapsedMs = Number(System.elapsedRealtime()) - record.start;
        const ok = body === 'ok' && error === null;
        if (ok) passed++;
        console.log(JSON.stringify({version: version, test: label, order: addresses,
            elapsedMs: elapsedMs, dnsOrder: record.dnsOrder, attempts: record.attempts, error: error, pass: ok}));
        current = null;
    }
    const tasks = [
        ['healthy-only', ['good']],
        ['unresponsive-first-1', ['bad', 'good']],
        ['healthy-first', ['good', 'bad']],
        ['unresponsive-first-2', ['bad', 'good']],
        ['recovery', ['good']]
    ];
    function next() {
        if (!tasks.length) { console.log('COMPLETE ' + version + ' ' + passed + '/5'); return; }
        const task = tasks.shift();
        setTimeout(function () { Java.perform(function () { run(task[0], task[1]); next(); }); }, 300);
    }
    next();
});
