// Read-only inspection. Attach to the already-open emulator app; never launch an activity.
Java.perform(function () {
    const app = Java.use('android.app.ActivityThread').currentApplication();
    if (String(app.getPackageName()) !== 'jp.co.airfront.android.a2chMate.haiagaru.uploadhosts191')
        throw Error('The isolated 191 emulator package is required');
    Java.scheduleOnMainThread(function () {
        const State = Java.use('app.morphe.extension.chmate.Haiagaru');
        const names = ['o.r8lambdaDC40CispcBM3LVwPP01tT_FjLc', 'o.setCornerTopRightRadius', 'o.zzbhp'];
        const TextView = Java.use('android.widget.TextView');
        const Group = Java.use('android.view.ViewGroup');
        const records = Java.use('android.app.ActivityThread').currentActivityThread().mActivities.value.values().toArray();
        const bodies = [];
        function visit(view) {
            if (names.indexOf(String(view.getClass().getName())) >= 0) {
                const text = Java.cast(view, TextView);
                bodies.push({shown:!!text.isShown(), selectable:!!text.isTextSelectable(),
                    callback:text.getCustomSelectionActionModeCallback() === null ? 'platform' : 'native',
                    selected:!!text.hasSelection(), selectionLength:Math.abs(text.getSelectionEnd()-text.getSelectionStart()),
                    length:text.length()});
            }
            if (Group.class.isInstance(view)) {
                const group = Java.cast(view, Group);
                for (let i=0;i<group.getChildCount();i++) visit(group.getChildAt(i));
            }
        }
        for (let i=0;i<records.length;i++) {
            const record=Java.cast(records[i],Java.use('android.app.ActivityThread$ActivityClientRecord'));
            if (!record.paused.value) visit(record.activity.value.getWindow().getDecorView());
        }
        console.log(JSON.stringify({enabled:!!State.directBodySelectionEnabled(),bodies:bodies}));
    });
});
