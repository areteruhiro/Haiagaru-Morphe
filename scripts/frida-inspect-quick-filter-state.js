Java.perform(function () { Java.scheduleOnMainThread(function () {
    const Q = Java.use('app.morphe.extension.chmate.ExtraQuickFilters');
    const Map = Java.use('java.util.Map'), List = Java.use('java.util.List');
    let selected = null;
    for (const m of Q.class.getDeclaredMethods()) if (String(m.getReturnType().getName()) === '[Z') {
        selected = m; selected.setAccessible(true);
    }
    for (const f of Q.class.getDeclaredFields()) {
        if (String(f.getType().getName()) !== 'java.util.Map') continue;
        f.setAccessible(true); const map = Java.cast(f.get(null), Map);
        for (const key of map.keySet().toArray()) {
            if (String(key.getClass().getName()) !== 'o.listener') continue;
            const state = map.get(key);
            const value = selected.invoke(null, Java.array('java.lang.Object', [key]));
            const Array = Java.use('java.lang.reflect.Array');
            console.log('NATIVE_SELECTED=' + [0,1,2,3,4,5,6].map(i => Array.getBoolean(value,i)));
            for (const sf of state.getClass().getDeclaredFields()) {
                if (String(sf.getType().getName()) !== 'java.lang.ref.WeakReference') continue;
                sf.setAccessible(true); const obj = Java.cast(sf.get(state), Java.use('java.lang.ref.WeakReference')).get();
                if (!obj || String(obj.getClass().getName()) !== 'app.morphe.extension.chmate.HaiagaruDialog') continue;
                const dialog = Java.cast(obj, Java.use('app.morphe.extension.chmate.HaiagaruDialog'));
                const checked = dialog.getClass().getDeclaredField('checkedItems'); checked.setAccessible(true);
                console.log('COMPOSE_CHECKED=' + Java.cast(checked.get(dialog),List).toString());
                dialog.getListView().setItemChecked(2, true);
                console.log('FORCED_CHECKED=' + Java.cast(checked.get(dialog),List).toString());
            }
        }
    }
}); });
