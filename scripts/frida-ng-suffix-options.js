// Test app only. Opens the normal confirmation UI for a synthetic suffix.
// Never logs real response contents or NG patterns.
function showNgSuffixOptions() {
    Java.perform(function () {
        Java.choose('android.app.Activity', {
            onMatch: function (activity) {
                if (!activity.hasWindowFocus() || activity.isFinishing()) return;
                var retained = Java.retain(activity);
                Java.scheduleOnMainThread(function () {
                    Java.use('app.morphe.extension.chmate.WacchoiSuffixNg')
                        .confirm(retained, '(ｽｯﾌﾟ Sd03-zQ!9)');
                });
            }, onComplete: function () {}
        });
    });
}
function dumpNgSuffixOptions() {
    Java.perform(function () {
        Java.choose('jp.syoboi.a2chMate.ng.NGWord', {
            onMatch: function (store) {
                var entries = store.e();
                var matches = 0;
                for (var i = 0; i < entries.size(); i++) {
                    var item = entries.get(i);
                    if (item.toString().indexOf('zQ!9') < 0) continue;
                    matches++;
                    var fields = item.getClass().getDeclaredFields();
                    for (var j = 0; j < fields.length; j++) {
                        if (fields[j].getType().getName() !== 'short') continue;
                        fields[j].setAccessible(true);
                        console.log('[ng-options] flags=' + fields[j].getShort(item));
                    }
                }
                if (matches) console.log('[ng-options] synthetic entries=' + matches);
            }, onComplete: function () {}
        });
    });
}
console.log('Test-only: showNgSuffixOptions(), dumpNgSuffixOptions()');
