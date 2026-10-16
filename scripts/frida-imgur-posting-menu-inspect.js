// Read-only diagnostics; never prints session/account values or uploads/deletes.
Java.perform(function () {
    const app = Java.use('android.app.ActivityThread').currentApplication();
    const H = Java.use('app.morphe.extension.chmate.ImgurWebUpload');
    console.log('Experimental patch supported=' + H.supported(app));
    const owner = 'o.p9ExternalSyntheticLambda6';
    function field(object, name) {
        for (let type = object.getClass(); type !== null; type = type.getSuperclass()) {
            for (const f of type.getDeclaredFields()) if (String(f.getName()) === name) {
                f.setAccessible(true); return f.get(object);
            }
        }
        throw Error('Missing field ' + name);
    }
    Java.choose('o.qm', {
        onMatch: function (popup) {
            if (!popup.isShowing()) return;
            const callback = field(popup, 'd');
            console.log('Showing popup callback=' + (callback === null ? 'null' : callback.getClass().getName()));
            const adapter = field(popup, 'i');
            if (adapter !== null) {
                const menu = Java.cast(field(adapter, 'a'), Java.use('android.view.Menu'));
                console.log('Showing menu customItem=' + (menu.findItem(0x48690041) !== null));
            }
        }, onComplete: function () {}
    });
    Java.choose(owner, {
        onMatch: function (fragment) {
            console.log('Posting fragment: attached=' + (fragment.getActivity() !== null));
            try {
                const method = fragment.getClass().getMethod('getActivity', Java.array('java.lang.Class', []));
                console.log('Reflective activity=' + (method.invoke(fragment, Java.array('java.lang.Object', [])) !== null));
            } catch (error) { console.log('Reflective activity error: ' + error); }
            const popup = field(fragment, 'p');
            if (popup !== null) console.log('Native popup exists');
        },
        onComplete: function () {}
    });
    const add = H.addPostingMenu.overload('java.lang.Object', 'android.view.Menu');
    add.implementation = function (fragment, menu) {
        const before = menu.size();
        const result = add.call(this, fragment, menu);
        console.log('Menu prepare before=' + before + ' after=' + menu.size()
            + ' customItem=' + (menu.findItem(0x48690041) !== null));
        return result;
    };
});
