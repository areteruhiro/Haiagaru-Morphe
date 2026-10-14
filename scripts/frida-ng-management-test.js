// Isolated HaiagaruNG191 test APK only. No runtime behavior replacements.
Java.perform(function () {
    var App = Java.use('android.app.ActivityThread').currentApplication();
    if (App.getPackageName().toString() !== 'jp.co.airfront.android.a2chMate.haiagaru.ngmanage191')
        throw new Error('Refusing to modify registrations outside isolated test app');
    var Store = Java.use('app.morphe.extension.chmate.NativeNgStore');
    var store = Store.$new(App, 'NAME');
    var entries = store.entries();
    if (entries.size() === 0) {
        store.update(null, store.item('(ﾜｯﾁｮｲ bbbb-zQ!9)', 9, Date.now(), 'synthetic.ng.test'));
        store.update(null, store.item('(ｽｯﾌﾟ dddd-zQ!9)', 9, Date.now(), 'synthetic.ng.test'));
        store.update(null, store.item('(ﾜｯﾁｮｲ aaaa-zQ!9)', 0, Date.now(), 'different.board'));
    }
    function activity(callback) {
        Java.choose('android.app.Activity', {
            onMatch: function (a) {
                if (!a.hasWindowFocus() || a.isFinishing()) return;
                var retained = Java.retain(a);
                Java.scheduleOnMainThread(function () { callback(retained); });
            }, onComplete: function () {}
        });
    }
    globalThis.showManager = function () {
        activity(function (a) { Java.use('app.morphe.extension.chmate.NgManagement').show(a); });
    };
    globalThis.showRegistration = function () {
        activity(function (a) {
            Java.use('app.morphe.extension.chmate.WacchoiSuffixNg').confirm
                .overload('android.content.Context', 'java.lang.String', 'java.lang.String')
                .call(Java.use('app.morphe.extension.chmate.WacchoiSuffixNg'), a,
                    '(ｽｯﾌﾟ Sd03-zQ!9)', 'synthetic.ng.test');
        });
    };
    globalThis.inspectTestRules = function () {
        Java.perform(function () {
            var items = store.entries();
            for (var i = 0; i < items.size(); i++) {
                var e = Java.cast(items.get(i), Java.use('app.morphe.extension.chmate.NativeNgStore$Entry'));
                console.log('[synthetic-only] ' + e.pattern.value + ' board=' + e.board.value + ' flags=' + e.flags.value);
            }
        });
    };
    showManager();
    console.log('Isolated test: showManager(), showRegistration(), inspectTestRules()');
});
