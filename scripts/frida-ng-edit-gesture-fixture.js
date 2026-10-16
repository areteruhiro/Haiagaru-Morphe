// Run only against the disposable HaiagaruGesture191 clone. No NG entries are changed.
setTimeout(function () { Java.perform(function () {
    const Thread = Java.use('android.app.ActivityThread');
    const app = Thread.currentApplication();
    if (String(app.getPackageName()) !== 'jp.co.airfront.android.a2chMate.haiagaru.gesture191')
        throw Error('Disposable gesture191 package required');
    const prefs = Java.use('android.preference.PreferenceManager').getDefaultSharedPreferences(app);
    const key = 'haiagaruGestureNgEdit';
    const existed = prefs.contains(key), saved = prefs.getInt(key, 0);
    const H = Java.use('app.morphe.extension.chmate.NgEditGesture');
    let launches = 0, hooks = 0;
    const Activity = Java.use('android.app.Activity');
    const launch = Activity.startActivityForResult.overload('android.content.Intent', 'int');
    launch.implementation = function (intent, request) {
        if (intent.getComponent() !== null && String(intent.getComponent().getClassName()) === 'jp.syoboi.a2chMate.activity.NGEditorActivity') launches++;
        return launch.call(this, intent, request);
    };
    const onGesture = H.onGesture.overload('android.view.View', 'int');
    onGesture.implementation = function (view, sequence) {
        hooks++;
        return onGesture.call(H, view, sequence);
    };
    Java.scheduleOnMainThread(function () {
        try {
            let active = null;
            for (const item of Thread.currentActivityThread().mActivities.value.values().toArray()) {
                const record = Java.cast(item, Java.use('android.app.ActivityThread$ActivityClientRecord'));
                if (record.activity.value && record.activity.value.hasWindowFocus()) active = record.activity.value;
            }
            if (!active) throw Error('No focused activity');
            const View = Java.use('android.view.View');
            const Group = Java.use('android.view.ViewGroup');
            let gesture = null;
            function find(view) {
                if (String(view.getClass().getName()) === 'o.qgExternalSyntheticLambda1') { gesture = Java.cast(view, Java.use('o.qgExternalSyntheticLambda1')); return; }
                if (Group.class.isInstance(view)) {
                    const group = Java.cast(view, Group);
                    for (let i = 0; i < group.getChildCount() && gesture === null; i++) find(group.getChildAt(i));
                }
            }
            find(active.getWindow().getDecorView());
            if (!gesture) throw Error('Native gesture layout not found');
            prefs.edit().putInt(key, 0).commit();
            if (H.onGesture(gesture, 82824)) throw Error('Unassigned action consumed');
            prefs.edit().putInt(key, 82824).commit();
            if (H.onGesture(gesture, 82826)) throw Error('Unrelated gesture consumed');
            // Execute the native completion method, not just the extension helper.
            const detectorField = gesture.getClass().getDeclaredField('c');
            detectorField.setAccessible(true);
            const detector = detectorField.get(gesture);
            const sequenceField = detector.getClass().getDeclaredField('b');
            sequenceField.setAccessible(true);
            const oldSequence = sequenceField.getInt(detector);
            const Point = Java.use('android.graphics.PointF');
            try {
                sequenceField.setInt(detector, 82824);
                gesture.WU_(5, Point.$new(200, 200), Point.$new(400, 400));
            } finally { sequenceField.setInt(detector, oldSequence); }
            if (launches !== 1 || hooks < 3) throw Error('Completion did not open NG editor: launches=' + launches + ', hooks=' + hooks);
            console.log('PASS native completion opens NGEditorActivity; unassigned/unrelated gestures unchanged');
        } finally {
            const edit = prefs.edit();
            if (existed) edit.putInt(key, saved); else edit.remove(key);
            edit.commit();
        }
    });
}); }, 1500);
