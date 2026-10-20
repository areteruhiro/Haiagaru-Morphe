// Check the installed Compose dialog state without changing app settings or showing a window.
Java.perform(function () {
    let tested = false;
    Java.choose('jp.syoboi.a2chMate.activity.TabletHomeActivity', {
        onMatch: function (activity) {
            const host = Java.retain(activity);
            Java.scheduleOnMainThread(function () {
                if (tested || host.isFinishing()) return;
                tested = true;
                const Builder = Java.use('app.morphe.extension.chmate.HaiagaruDialog$Builder');
                const List = Java.use('java.util.List');
                const selected = Java.array('boolean', [false, true]);
                const labels = Java.array('java.lang.CharSequence', ['画像', '動画']);
                const dialog = Builder.$new(host).setMultiChoiceItems(labels, selected, null).create();
                const field = dialog.getClass().getDeclaredField('checkedItems');
                field.setAccessible(true);
                const state = Java.cast(field.get(dialog), List);
                function check(index, expected, label) {
                    if (String(state.get(index)) !== String(expected)) throw Error(label);
                    console.log('PASS ' + label);
                }
                check(0, false, 'initial unchecked Compose state');
                check(1, true, 'initial checked Compose state');
                dialog.getListView().setItemChecked(0, true);
                check(0, true, 'check immediately reaches observable Compose state');
                dialog.getListView().setItemChecked(0, false);
                check(0, false, 'uncheck immediately reaches observable Compose state');
                selected[1] = false;
                dialog.refresh();
                check(1, false, 'model refresh reaches observable Compose state');
                dialog.dismiss();
                console.log('DONE dialog checkbox regression');
            });
        },
        onComplete: function () {}
    });
});
