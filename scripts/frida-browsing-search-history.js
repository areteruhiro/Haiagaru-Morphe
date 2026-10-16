// Isolated emulator packages only. No network requests, posts or history deletion.
Java.perform(function () {
    const app = Java.use('android.app.ActivityThread').currentApplication();
    const pkg = String(app.getPackageName());
    if (!/^jp\.co\.airfront\.android\.a2chMate\.haiagaru\.(count(191|226|241)|imgur242)$/.test(pkg))
        throw Error('Isolated history fixture required');
    const version = String(app.getPackageManager().getPackageInfo(pkg, 0).versionName.value);
    const v = version.includes('191') ? '191' : version.includes('226') ? '226' : version.includes('241') ? '241' : '242';
    const own = app.getSharedPreferences('io.github.areteruhiro.chmate.haiagaru.ui-config', 0);
    const native = Java.use('android.preference.PreferenceManager').getDefaultSharedPreferences(app);
    const H = Java.use('app.morphe.extension.chmate.HistoryLimits');
    const snapshots = [];
    function remember(p, k, type) {
        snapshots.push([p, k, type, p.contains(k), type === 'int' ? p.getInt(k, 0) : type === 'bool' ? p.getBoolean(k, false) : p.getString(k, '')]);
    }
    remember(own, 'searchHistoryCount', 'int'); remember(own, 'recentThreadDisplayCount', 'int');
    remember(own, 'unlimitedThreadHistory', 'bool'); remember(native, 'searchRecent', 'string');
    remember(native, 'saveSearchHistory', 'bool');
    const owners = {
        '191': ['o.lrb$write', 'o.r8lambdaEBvvDaQDWIaS7WUoordU_4sxR3Y', 'b'],
        '226': ['o.hasNextProxy$ComponentActivity', 'o.TrustRootIndex', 'b'],
        '241': ['o.canScrollHorizontally$read', 'o.tul11', 'd'],
        '242': ['o.afterTextChanged$RemoteActionCompatParcelizer', 'o.initView', 'a']
    };
    const writer = Java.use(owners[v][0]);
    const writerMethod = writer.class.getDeclaredMethods();
    let write, read;
    for (const m of writerMethod) {
        const p = m.getParameterTypes();
        if (p.length === 1 && String(p[0].getName()) === 'java.lang.String' && String(m.getReturnType().getName()) === 'void')
            write = writer[String(m.getName())].overload('java.lang.String');
        if (p.length === 0 && String(m.getReturnType().getName()) === '[Ljava.lang.String;')
            read = writer[String(m.getName())].overload();
    }
    if (!write || !read) throw Error('Native search methods not found');
    const hooks = [];
    try {
        native.edit().putBoolean('saveSearchHistory', true).commit();
        for (const limit of [20, 0, 1]) {
            own.edit().putInt('searchHistoryCount', limit).commit();
            native.edit().putString('searchRecent', '').commit();
            for (let i = 0; i < 65; i++) write.call(writer, 'Haiagaru Search Fixture ' + i);
            let rows = read.call(writer);
            const expected = limit === 0 ? 65 : limit;
            if (rows.length !== expected) throw Error('Native search reader count: ' + rows.length + ' != ' + expected);
            write.call(writer, 'HAIAGARU SEARCH FIXTURE 64');
            rows = read.call(writer);
            if (rows.length !== expected || String(rows[0]) !== 'HAIAGARU SEARCH FIXTURE 64') throw Error('Search dedup/promotion failed');
            console.log('PASS ' + v + ' native search writer/reader, limit=' + limit + ', count=' + rows.length);
        }
        const oldSearch = String(native.getString('searchRecent', ''));
        native.edit().putBoolean('saveSearchHistory', false).commit();
        write.call(writer, 'Must not save');
        if (String(native.getString('searchRecent', '')) !== oldSearch) throw Error('Save OFF ignored');
        console.log('PASS ' + v + ' native search saving OFF');
        const list = Java.use('java.util.ArrayList').$new();
        for (let i = 0; i < 65; i++) list.add(Java.use('java.lang.Integer').valueOf(i));
        for (const limit of [20, 0]) {
            own.edit().putInt('recentThreadDisplayCount', limit).commit();
            if (H.limitRecentThreadRows(list).size() !== (limit === 0 ? 65 : 20)) throw Error('Recent result limit');
            if (v === '242') {
                const C = Java.use('o.initRenderFinish$_init_lambda3');
                const ctor = C.$init.overloads[0];
                const args = ctor.argumentTypes.map(t => {
                    if (t.className === 'long') return 50;
                    let found = null;
                    Java.choose(t.className, { onMatch: x => { found = Java.retain(x); return 'stop'; }, onComplete: () => {} });
                    return found;
                });
                const query = C.$new.apply(C, args);
                const field = C.class.getDeclaredField('b'); field.setAccessible(true);
                const actual = field.getLong(query);
                console.log('Recent ctor limit=' + actual + ', getter=' + H.getRecentThreadDisplayCountLong());
                if (String(actual) !== String(limit === 0 ? 2147483647 : 20)) throw Error('Native recent SQL ctor fixed cap remains');
            }
            console.log('PASS ' + v + ' recent display limit=' + limit);
        }
        // Stop finite cleanup at the first DB query. Unlimited must never reach it.
        let sqlCalls = 0;
        const SQLite = Java.use('android.database.sqlite.SQLiteDatabase');
        for (const name of ['rawQuery', 'rawQueryWithFactory', 'query']) {
            if (!SQLite[name]) continue;
            for (const method of SQLite[name].overloads) {
                method.implementation = function () { sqlCalls++; throw Java.use('java.lang.IllegalStateException').$new('Haiagaru cleanup test sentinel'); };
                hooks.push(method);
            }
        }
        const prune = Java.use(owners[v][1]);
        const method = prune[owners[v][2]].overload();
        let receiver = prune;
        const Modifier = Java.use('java.lang.reflect.Modifier');
        const reflected = prune.class.getDeclaredMethod(owners[v][2], Java.array('java.lang.Class', []));
        if (!Modifier.isStatic(reflected.getModifiers())) {
            receiver = null;
            Java.choose(owners[v][1], { onMatch: x => { receiver = Java.retain(x); return 'stop'; }, onComplete: () => {} });
            if (!receiver) throw Error('Native cleanup instance not initialized');
        }
        own.edit().putBoolean('unlimitedThreadHistory', true).commit();
        method.call(receiver);
        if (sqlCalls !== 0) throw Error('Unlimited cleanup reached database');
        console.log('PASS ' + v + ' native automatic cleanup disabled without opening DB cursor');
        own.edit().putBoolean('unlimitedThreadHistory', false).commit();
        try { method.call(receiver); } catch (e) { if (!String(e).includes('sentinel')) throw e; }
        if (sqlCalls === 0) throw Error('Finite native cleanup not reached');
        console.log('PASS ' + v + ' finite cleanup retains native query (intercepted, no deletion)');
    } finally {
        for (const h of hooks) h.implementation = null;
        for (const [p, k, type, had, value] of snapshots) {
            const e = p.edit();
            if (!had) e.remove(k); else if (type === 'int') e.putInt(k, value); else if (type === 'bool') e.putBoolean(k, value); else e.putString(k, value);
            e.commit();
        }
        console.log('History preferences restored; no network or posts');
    }
});
