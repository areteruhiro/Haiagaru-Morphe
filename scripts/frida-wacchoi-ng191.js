// Diagnostic hook: restore the callback that 191's name popup bypasses.
Java.perform(function () {
    const menuExtension = Java.use('app.morphe.extension.chmate.WacchoiLongPressMenu');
    const suffixNg = Java.use('app.morphe.extension.chmate.WacchoiSuffixNg');
    const Fragment = Java.use('o.pa');
    let selectedQuery = null;
    let selectedActivity = null;
    const append = menuExtension.appendLegacyForResponse;
    append.implementation = function (fragment, menu, response) {
        const fields = response.getClass().getDeclaredFields();
        let text = '';
        for (let i = 0; i < fields.length; i++) {
            if (fields[i].getName().toString() === 'n' || fields[i].getName().toString() === 'q') {
                fields[i].setAccessible(true);
                text += ' ' + fields[i].get(response);
            }
        }
        selectedQuery = text;
        selectedActivity = Java.cast(fragment, Fragment).getActivity();
        console.log('Selected suffix: ' + suffixNg.suffix(text));
        return append.call(this, fragment, menu, response);
    };
    const popup = Java.use('o.MaxFullscreenAdImplc');
    const callback = popup.onItemClick;
    callback.implementation = function (adapter, view, index, id) {
        const item = Java.cast(adapter.getItemAtPosition(index), Java.use('android.view.MenuItem'));
        console.log('Native popup click: ' + item.getItemId());
        if (item.getItemId() === 76) {
            const stored = Java.cast(item, Java.use('o.r8lambda9uSHt4DumN_rfVPyMzyPRS1qXDU'));
            const listener = stored.a.value;
            if (listener) {
                this.dismiss();
                listener.onMenuItemClick(item);
                console.log('NG item listener dispatched');
                return;
            }
        }
        return callback.call(this, adapter, view, index, id);
    };
    console.log('191 NG popup diagnostic ready');
});
