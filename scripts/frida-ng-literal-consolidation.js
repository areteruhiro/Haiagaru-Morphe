// Isolated emulator fixture only; never modifies a user's NG registrations.
Java.perform(function () {
    var app = Java.use('android.app.ActivityThread').currentApplication();
    if (app.getPackageName().toString() !== 'jp.co.airfront.android.a2chMate.haiagaru.ngliteral191')
        throw new Error('Isolated literal-NG test package required');
    var store = Java.use('app.morphe.extension.chmate.NativeNgStore').$new(app, 'NAME');
    globalThis.openLiteralNgFixture = function () {
        Java.perform(function () {
            if (store.entries().size() !== 0) throw new Error('Fixture requires empty isolated NGName store');
            store.update(null, store.item('(ﾜｯﾁｮｲ 8776-Kuom)', 0x19, Date.now(), 'android'));
            store.update(null, store.item('(ﾜｯﾁｮｲ 87f7-Kuom)', 0x19, Date.now(), 'android'));
            Java.choose('android.app.Activity', {
                onMatch: function (a) {
                    if (!a.hasWindowFocus() || a.isFinishing()) return;
                    var activity = Java.retain(a);
                    Java.scheduleOnMainThread(function () {
                        Java.use('app.morphe.extension.chmate.NgManagement').show(activity);
                    });
                }, onComplete: function () {}
            });
        });
    };
    globalThis.verifyLiteralNgResult = function () {
        Java.perform(function () {
            var entries = store.entries();
            if (entries.size() !== 1) throw new Error('Expected one consolidated fixture rule');
            var item = Java.cast(entries.get(0), Java.use('app.morphe.extension.chmate.NativeNgStore$Entry'));
            if (item.flags.value !== 0x29 || item.board.value.toString() !== 'android')
                throw new Error('Native flags or scope changed');
            // Native NG matcher uses CASE_INSENSITIVE; the suffix's (?-i:...) must win.
            var pattern = Java.use('java.util.regex.Pattern').compile
                .overload('java.lang.String', 'int').call(Java.use('java.util.regex.Pattern'), item.pattern.value, 2);
            var JString = Java.use('java.lang.String');
            if (!pattern.matcher(JString.$new('(ｽｯﾌﾟ Sd03-Kuom)')).find()
                    || pattern.matcher(JString.$new('(ﾜｯﾁｮｲ 8776-kuom)')).find())
                throw new Error('Consolidated suffix matching changed');
            console.log('PASS native literal NG consolidation: one regex, board/chain/hidden preserved, carrier and case checked');
        });
    };
    console.log('Isolated fixture: openLiteralNgFixture(), verifyLiteralNgResult()');
});
