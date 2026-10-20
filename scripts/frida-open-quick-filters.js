// Opens the real filter dialog in an isolated emulator test app without replacing its views.
Java.perform(function () {
    const app = Java.use('android.app.ActivityThread').currentApplication();
    const pkg = String(app.getPackageName());
    if (!/\.haiagaru\.(full191|resumefix226|resumefix242)$/.test(pkg))
        throw Error('Emulator regression package required');
    const name = pkg.endsWith('191') ? 'o.pa' : 'jp.syoboi.a2chMate.ui.reslist.ResListFragment';
    Java.scheduleOnMainThread(function () {
        let opened = false;
        const filters = Java.use('app.morphe.extension.chmate.ExtraQuickFilters');
        const fields = filters.class.getDeclaredFields();
        for (const field of fields) {
            if (String(field.getType().getName()) !== 'java.util.ArrayList') continue;
            field.setAccessible(true);
            const list = Java.cast(field.get(null), Java.use('java.util.ArrayList'));
            for (let i = 0; i < list.size() && !opened; i++) {
                const item = list.get(i);
                if (!Java.use('java.lang.ref.WeakReference').class.isInstance(item)) continue;
                const owner = Java.cast(item, Java.use('java.lang.ref.WeakReference')).get();
                if (!owner || String(owner.getClass().getName()) !== name) continue;
                const fragment = Java.cast(owner, Java.use(name));
                if (fragment.getView() && fragment.getActivity() && fragment.getActivity().hasWindowFocus())
                    opened = filters.open(fragment);
            }
        }
        console.log('OPEN_FILTERS=' + opened + ' package=' + pkg);
    });
});
