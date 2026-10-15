// Tests real 191 Menu objects, no user data and no production method replacement.
Java.perform(function () {
    const app = Java.use('android.app.ActivityThread').currentApplication();
    if (app.getPackageName().toString() !== 'jp.co.airfront.android.a2chMate.haiagaru.nglayout191')
        throw new Error('Isolated menu test app required');
    Java.scheduleOnMainThread(function () {
        const prefs = app.getSharedPreferences('io.github.areteruhiro.chmate.haiagaru.ui-config', 0);
        const prior = prefs.getBoolean('wacchoiNgFirstMenu', false);
        const helper = Java.use('app.morphe.extension.chmate.WacchoiLongPressMenu');
        const menu = Java.use('o.m6fExternalSyntheticLambda0').$new(app);
        const string = Java.use('java.lang.String');
        const rows = [[75, 'ﾜｯﾁｮｲで検索'], [76, 'ﾜｯﾁｮｲ下4桁をNG登録'],
            [101, 'NGName に追加'], [103, 'コピー'], [104, '取得済みのスレから検索'], [105, 'その他']];
        for (const row of rows) menu.add.overload('int', 'int', 'int', 'java.lang.CharSequence')
            .call(menu, 0, row[0], 0, string.$new(row[1]));
        const original = rows.map((_, i) => Java.retain(menu.getItem(i)));
        try {
            prefs.edit().putBoolean('wacchoiNgFirstMenu', false).commit();
            helper.reorderLegacyNameMenu(menu);
            for (let i = 0; i < rows.length; i++)
                if (!menu.getItem(i).equals(original[i])) throw new Error('OFF changed ordering');
            prefs.edit().putBoolean('wacchoiNgFirstMenu', true).commit();
            helper.reorderLegacyNameMenu(menu);
            const expected = [101, 76, 103, 104, 75, 105];
            for (let i = 0; i < expected.length; i++)
                if (menu.getItem(i).getItemId() !== expected[i]) throw new Error('ON order mismatch');
            for (let i = 0; i < rows.length; i++)
                if (!menu.findItem(rows[i][0]).equals(original[i])) throw new Error('Menu action replaced');
            if (menu.findItem(75).getTitle().toString() !== '必死チェッカーもどき(ﾜｯﾁｮｲ)')
                throw new Error('Search label mismatch');
            console.log('PASS native 191 Wacchoi menu: OFF preserved, ON requested order, IDs and action objects preserved');
        } finally {
            prefs.edit().putBoolean('wacchoiNgFirstMenu', prior).commit();
        }
    });
});
