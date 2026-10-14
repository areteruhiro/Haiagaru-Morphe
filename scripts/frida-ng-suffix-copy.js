// Isolated emulator test. Invokes production code without replacing it.
// Never prints existing clipboard contents or private NG patterns.
Java.perform(function () {
    var app = Java.use('android.app.ActivityThread').currentApplication();
    if (app.getPackageName().toString() !== 'jp.co.airfront.android.a2chMate.haiagaru.ngmanage191')
        throw new Error('Isolated test package required');
    var store = Java.use('app.morphe.extension.chmate.NativeNgStore').$new(app, 'NAME');
    var before = store.entries().size();
    var invoked = false;
    Java.choose('android.app.Activity', {
        onMatch: function (a) {
            if (invoked || !a.hasWindowFocus() || a.isFinishing()) return;
            invoked = true;
            var activity = Java.retain(a);
            Java.scheduleOnMainThread(function () {
                var clipboard = Java.cast(activity.getSystemService('clipboard'), Java.use('android.content.ClipboardManager'));
                var previous = clipboard.getPrimaryClip();
                if (previous) previous = Java.retain(previous);
                Java.use('app.morphe.extension.chmate.WacchoiSuffixNg').confirm
                    .overload('android.content.Context', 'java.lang.String')
                    .call(Java.use('app.morphe.extension.chmate.WacchoiSuffixNg'), activity, '(ｽｯﾌﾟ Sd03-Kuom)');
                setTimeout(function () {
                    Java.perform(function () {
                        Java.scheduleOnMainThread(function () {
                            var value = clipboard.getPrimaryClip().getItemAt(0).getText().toString();
                            console.log('[suffix-copy] exact=' + (value === '-Kuom|'));
                            console.log('[suffix-copy] NG rows unchanged=' + (before === store.entries().size()));
                            Java.choose('jp.syoboi.a2chMate.activity.NGEditorActivity', {
                                onMatch: function (editor) {
                                    console.log('[suffix-copy] editor focused=' + editor.hasWindowFocus()
                                        + ' ngType=' + editor.getIntent().getIntExtra('ngType', -1));
                                }, onComplete: function () {}
                            });
                            if (previous) clipboard.setPrimaryClip(previous);
                            else clipboard.clearPrimaryClip();
                        });
                    });
                }, 2500);
            });
        }, onComplete: function () {}
    });
});
