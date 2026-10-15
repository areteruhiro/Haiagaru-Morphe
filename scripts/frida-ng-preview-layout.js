// Synthetic 15-group/36-name fixture. Refuses to touch normal app registrations.
Java.perform(function () {
    const app = Java.use('android.app.ActivityThread').currentApplication();
    if (app.getPackageName().toString() !== 'jp.co.airfront.android.a2chMate.haiagaru.nglayout191')
        throw new Error('Isolated NG layout test package required');
    const store = Java.use('app.morphe.extension.chmate.NativeNgStore').$new(app, 'NAME');
    if (store.entries().size() === 0) {
        for (let group = 0; group < 15; group++) {
            const suffix = 'T' + String(group).padStart(3, '0');
            for (let member = 0; member < (group < 6 ? 3 : 2); member++)
                store.update(null, store.item('(ﾜｯﾁｮｲ ' + (1000 + member) + '-' + suffix + ')',
                    0x19, Date.now(), 'synthetic.ng.layout'));
        }
    }
    if (store.entries().size() !== 36) throw new Error('Expected isolated fixture of 36 registrations');
    globalThis.openLayoutFixture = function () {
        Java.choose('android.app.Activity', {
            onMatch: function (activity) {
                if (!activity.hasWindowFocus() || activity.isFinishing()) return;
                const a = Java.retain(activity);
                Java.scheduleOnMainThread(function () {
                    Java.use('app.morphe.extension.chmate.NgManagement').show(a);
                });
            }, onComplete: function () {}
        });
    };
    globalThis.inspectLayoutFixture = function (scrollToEnd) {
        Java.choose('android.app.AlertDialog', {
            onMatch: function (dialog) {
                if (!dialog.isShowing() || dialog.getListView() === null) return;
                const d = Java.retain(dialog);
                Java.scheduleOnMainThread(function () {
                    const list = d.getListView();
                    if (list.getFooterViewsCount() !== 1 || list.getCount() !== 16)
                        throw new Error('Expected 15 choices and one deletion-option footer');
                    const bounds = Java.use('android.graphics.Rect').$new();
                    d.getWindow().getDecorView().getWindowVisibleDisplayFrame(bounds);
                    for (const id of [-1, -2]) {
                        const button = d.getButton(id);
                        const xy = Java.array('int', [0, 0]);
                        button.getLocationOnScreen(xy);
                        if (!button.isShown() || !button.isEnabled() || xy[1] < bounds.top.value
                                || xy[1] + button.getHeight() > bounds.bottom.value)
                            throw new Error('Dialog action outside usable display');
                    }
                    if (scrollToEnd) list.setSelection(15);
                    console.log('PASS 15-group/36-name preview: both actions within display, native scroll list + footer');
                });
            }, onComplete: function () {}
        });
    };
    console.log('Isolated fixture ready: openLayoutFixture(), inspectLayoutFixture(true/false)');
});
