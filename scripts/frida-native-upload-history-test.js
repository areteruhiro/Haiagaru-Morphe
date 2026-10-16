// Isolated verification packages only. No real uploads or server deletions.
Java.perform(function () {
    const app = Java.use('android.app.ActivityThread').currentApplication();
    const pkg = String(app.getPackageName());
    if (!/\.uploadhistory(191|226|242)$/.test(pkg)) throw Error('Isolated verification app required');
    const version = pkg.slice(-3), legacy = version === '191', modern = version === '242';
    const H = Java.use('app.morphe.extension.chmate.NativeUploadHistory');
    const rowType = legacy ? 'o.onRewardedInterstitialAdDisplayed' : modern ? 'o.hasResolution' : 'o.enB12';
    const modelType = legacy ? 'o.onRewardedInterstitialAdHidden' : modern
        ? 'jp.syoboi.chmate2.feature.screen.uploadlist.UploadListViewModel' : 'o.RFEndCardBackUpLayout2';
    const Row = Java.use(rowType), ArrayList = Java.use('java.util.ArrayList');
    const prefs = app.getSharedPreferences('haiagaru_upload_history', 0);
    let checks = 0, requests = 0, owner, cancelMode = false;
    function check(ok, label) { if (!ok) throw Error(label); checks++; console.log('PASS ' + label); }
    function field(obj, name) {
        const f = obj.getClass().getDeclaredField(name); f.setAccessible(true); return f.get(obj);
    }
    function setField(obj, name, value) {
        const f = obj.getClass().getDeclaredField(name); f.setAccessible(true); f.set(obj, value);
    }
    // Verify the real helper-to-native constructor/method binding while stopping at
    // the native request entry. No native network method is allowed to execute.
    const requestType = legacy ? 'o.nm' : modern
        ? 'jp.syoboi.chmate2.feature.screen.uploadlist.UploadListViewModel$IconCompatParcelizer$write'
        : 'o.RFEndCardBackUpLayout2$RemoteActionCompatParcelizer$ComponentActivity';
    const Request = Java.use(requestType);
    let bound = false;
    if (legacy) Request.a.overload('[Ljava.lang.Object;').implementation = function () {
        bound = true; return Java.use('java.lang.Exception').$new('fixture response');
    };
    else Request.invokeSuspend.implementation = function (unit) {
        bound = true;
        check(String(unit.getClass().getName()) === (modern ? 'o.zzbwq' : 'o.Ff11'), 'native coroutine Unit binding');
        return Java.use('java.lang.Boolean').TRUE.value;
    };
    const transportRow = Row.$new('', '', 'https://example.invalid/binding.png', 'fixture-binding', 1, null);
    check(H.delete(app, transportRow) === !legacy && bound, 'native synchronous delete binding (network blocked)');
    // Block every request at the shared transport boundary, even if the fixture safety check fails.
    H.delete.implementation = function (context, row) {
        const r = Java.cast(row, Row), key = String(r.key.value);
        if (!key.startsWith('fixture-')) throw Error('Non-fixture deletion blocked');
        requests++;
        if (cancelMode) Java.scheduleOnMainThread(function () {
            Java.choose('android.app.ProgressDialog', {onMatch: function (d) { if (d.isShowing()) d.cancel(); }, onComplete: function () {}});
        });
        return key !== 'fixture-fail';
    };
    const Intent = Java.use('android.content.Intent');
    const intent = Intent.$new();
    intent.setClassName(app, legacy ? 'jp.syoboi.a2chMate.imageupload.UploadListActivity'
        : 'jp.syoboi.chmate2.feature.screen.uploadlist.UploadListActivity');
    intent.addFlags(0x10000000); app.startActivity(intent);
    function findOwner(attempt) { Java.perform(function () {
        Java.choose(modelType, { onMatch: function (m) { owner = Java.retain(m); }, onComplete: function () {
            if (!owner) {
                if (attempt >= 15) throw Error('Native upload history owner not found');
                setTimeout(function () { findOwner(attempt + 1); }, 1000);
                return;
            }
            Java.scheduleOnMainThread(function () {
                const rows = ArrayList.$new();
                [['old', 'fixture-ok', 1000], ['new', 'fixture-fail', 3000], ['middle', '', 2000], ['deleted', 'fixture-deleted', 4000]]
                    .forEach(function (data, i) {
                        const row = Row.$new('', '', 'https://example.invalid/' + data[0] + '.png', data[1], data[2], null);
                        if (i === 3) row.flags.value = 1;
                        rows.add(row);
                    });
                const manager = field(owner, legacy ? 's' : modern ? 'j' : 'g');
                setField(manager, legacy ? 'f' : 'h', rows);
                prefs.edit().putBoolean('newestFirst', false).commit(); H.refresh(owner);
                const copy = ArrayList.$new.overload('java.util.Collection').call(ArrayList, rows);
                H.sort(copy); check(String(Java.cast(copy.get(0), Row).key.value) === 'fixture-ok', 'off preserves native order');
                prefs.edit().putBoolean('newestFirst', true).commit(); H.sort(copy);
                check(String(Java.cast(copy.get(0), Row).key.value) === 'fixture-deleted', 'descending sorts by timestamp');
                check(String(Java.cast(rows.get(0), Row).key.value) === 'fixture-ok', 'service order unchanged');
                H.refresh(owner);
                const activity = Java.use('app.morphe.extension.chmate.Haiagaru').toolbarActivity(owner);
                const Popup = Java.use('android.widget.PopupMenu');
                const menu = Popup.$new(activity, Java.use('android.view.View').$new(activity)).getMenu();
                H.addMenu(owner, menu); H.addMenu(owner, menu);
                check(menu.size() === 2, 'native menu actions added once');
                check(String(menu.getItem(0).getTitle()).includes('降順'), 'descending menu label');
                check(String(menu.getItem(1).getTitle()).includes('サーバーから一括削除'), 'server bulk deletion menu label');
                H.handle(owner, menu.getItem(0));
                check(!prefs.getBoolean('newestFirst', true) && !menu.getItem(0).isChecked(), 'menu toggle saves off and updates checkmark');
                H.handle(owner, menu.getItem(0));
                check(prefs.getBoolean('newestFirst', false) && menu.getItem(0).isChecked(), 'menu toggle saves descending');
                H.choose(activity, owner);
                console.log('SELECTION_READY checks=' + checks + '; eligible=2; requests=' + requests);
                setTimeout(function () { Java.perform(function () { Java.scheduleOnMainThread(function () {
                    Java.choose('android.app.AlertDialog', {onMatch: function (d) {
                        if (!d.isShowing() || d.getListView() === null) return;
                        check(d.getListView().getCount() === 2, 'deleted/keyless rows excluded from selection');
                        d.getButton(-3).performClick();
                        check(d.getListView().isItemChecked(0) && d.getListView().isItemChecked(1), 'select all checks both');
                        d.getButton(-3).performClick();
                        check(!d.getListView().isItemChecked(0) && !d.getListView().isItemChecked(1), 'toggle all clears selection');
                        d.getButton(-3).performClick(); d.getButton(-1).performClick();
                    }, onComplete: function () {}});
                }); }); }, 1000);
                setTimeout(function () { Java.perform(function () { Java.scheduleOnMainThread(function () {
                    Java.choose('android.app.AlertDialog', {onMatch: function (d) {
                        if (d.isShowing() && d.getListView() === null && d.getButton(-2) !== null) d.getButton(-1).performClick();
                    }, onComplete: function () {}});
                }); }); }, 2500);
                setTimeout(function () { Java.perform(function () { Java.scheduleOnMainThread(function () {
                    const current = Java.cast(field(manager, legacy ? 'f' : 'h'), ArrayList);
                    check(requests === 2, 'only two eligible mock requests executed');
                    check((Java.cast(current.get(0), Row).flags.value & 1) === 1, 'success marked deleted');
                    check((Java.cast(current.get(1), Row).flags.value & 1) === 0, 'failure unchanged');
                    check(current.size() === 4, 'all history rows retained');
                    const Service = Java.use(manager.getClass().getName());
                    const stored = Service[legacy ? 'd' : modern ? 'a' : 'c'].overload().call(Service);
                    check(stored.size() === 4 && (Java.cast(stored.get(0), Row).flags.value & 1) === 1,
                        'native writer and reader persist deleted flag without removing history');
                    const fresh = ArrayList.$new();
                    for (let i = 0; i < 3; i++) fresh.add(Row.$new('', '', 'https://example.invalid/cancel' + i + '.png', 'fixture-cancel' + i, 5000 + i, null));
                    setField(manager, legacy ? 'f' : 'h', fresh); H.refresh(owner);
                    cancelMode = true; requests = 0;
                    Java.choose('android.app.AlertDialog', {onMatch: function(d) { if (d.isShowing()) d.dismiss(); }, onComplete: function(){}});
                    H.start(activity, owner, fresh);
                }); }); }, 6500);
                setTimeout(function () { Java.perform(function () { Java.scheduleOnMainThread(function () {
                    const current = Java.cast(field(manager, legacy ? 'f' : 'h'), ArrayList);
                    check(requests === 1, 'cancellation prevents remaining requests');
                    check((Java.cast(current.get(0), Row).flags.value & 1) === 1, 'in-flight success persisted after cancellation');
                    check((Java.cast(current.get(1), Row).flags.value & 1) === 0, 'unprocessed row unchanged');
                    console.log('PASS ALL ' + checks + ' upload history checks ' + version);
                }); }); }, 11000);
            });
        }});
    }); }
    setTimeout(function () { findOwner(0); }, 1800);
});
