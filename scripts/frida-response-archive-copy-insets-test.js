// Isolated emulator app only. Synthetic responses, no archive writes or network.
Java.perform(function () {
    const app = Java.use('android.app.ActivityThread').currentApplication();
    if (String(app.getPackageName()) !== 'jp.co.airfront.android.a2chMate.haiagaru.archivemenu191')
        throw Error('Isolated archive verification app required');
    Java.deoptimizeEverything(); // Ensure framework Builder/clipboard hooks are not bypassed by ART inlining.
    const A = Java.use('app.morphe.extension.chmate.ResponseArchive');
    const E = Java.use('app.morphe.extension.chmate.ResponseArchiveEntry');
    const Activity = Java.use('android.app.Activity'), Group = Java.use('android.view.ViewGroup');
    const Text = Java.use('android.widget.TextView'), Button = Java.use('android.widget.Button');
    let checks = 0, copied = null;
    function check(ok, label) { if (!ok) throw Error(label); checks++; console.log('PASS ' + label); }
    function current() {
        const records = Java.use('android.app.ActivityThread').currentActivityThread().mActivities.value.values().iterator();
        while (records.hasNext()) {
            const row = Java.cast(records.next(), Java.use('android.app.ActivityThread$ActivityClientRecord'));
            if (!row.paused.value) return Java.cast(row.activity.value, Activity);
        }
        throw Error('No resumed Activity');
    }
    function walk(v, fn) {
        fn(v);
        if (Group.class.isInstance(v)) {
            const g = Java.cast(v, Group);
            for (let i = 0; i < g.getChildCount(); i++) walk(g.getChildAt(i), fn);
        }
    }
    const entry = E.$new('https://example.invalid/test/read.cgi/a/1234567890/271', 'コピー検証', 271,
        '名無しさん＠お腹いっぱい。 (ﾜｯﾁｮｲ 0785-kAAU)', '2026/10/17(土) 21:31:19.27 ID:YpY9PPzg0',
        'ss速報の過去ログ対応ありがとうございます<br>二行目 &amp; &lt;test&gt;', 'sage');
    const expected = '271 名無しさん＠お腹いっぱい。 (ﾜｯﾁｮｲ 0785-kAAU) sage 2026/10/17(土) 21:31:19.27 ID:YpY9PPzg0\n\n'
        + 'ss速報の過去ログ対応ありがとうございます\n二行目 & <test>';
    check(String(A.copyText(entry)) === expected, 'header/body formatting and HTML decoding');
    check(String(A.copyText(entry)).indexOf('example.invalid') < 0, 'URL not mixed into body copy');
    const encoded = A.json(entry);
    check(String(A.decode(encoded).mail.value) === 'sage', 'mail survives archive JSON roundtrip');
    encoded.remove('mail');
    check(String(A.decode(encoded).mail.value) === '', 'old archives without mail still open');
    let dialog = null;
    Java.scheduleOnMainThread(function () {
        const a = current();
        A.detail(a, entry, null, 0);
        Java.choose('android.app.AlertDialog', {
            onMatch: function (candidate) {
                if (dialog !== null || !candidate.isShowing()) return;
                let own = false;
                walk(candidate.getWindow().getDecorView(), function (v) {
                    if (Text.class.isInstance(v) && String(Java.cast(v, Text).getText()) === 'コピー検証') own = true;
                });
                if (own) dialog = Java.retain(candidate);
            }, onComplete: function () {}
        });
        check(dialog !== null && dialog.isShowing(), 'saved-response detail opens');
        let copyButton = null, url = false;
        walk(dialog.getWindow().getDecorView(), function (v) {
            if (Text.class.isInstance(v) && String(Java.cast(v, Text).getText()) === 'URLをコピー') url = true;
            if (Button.class.isInstance(v) && String(Java.cast(v, Text).getText()) === 'ヘッダー＋本文をコピー')
                copyButton = Java.cast(v, Button);
        });
        check(copyButton !== null && url, 'body-copy and existing URL-copy both available');
        const clipboard = Java.cast(a.getSystemService('clipboard'), Java.use('android.content.ClipboardManager'));
        const previous = clipboard.getPrimaryClip();
        const history = app.getSharedPreferences('haiagaru-clipboard', 0);
        const hadHistory = history.contains('enabled'), historyEnabled = history.getBoolean('enabled', false);
        history.edit().putBoolean('enabled', false).commit();
        try {
            copyButton.performClick();
            copied = String(clipboard.getPrimaryClip().getItemAt(0).getText());
            check(copied === expected, 'copy button sends full header and body to clipboard');
        } finally {
            if (previous !== null) clipboard.setPrimaryClip(previous); else clipboard.clearPrimaryClip();
            const edit = history.edit();
            if (hadHistory) edit.putBoolean('enabled', historyEnabled); else edit.remove('enabled');
            edit.commit();
        }
        check(dialog.isShowing(), 'copy does not close the saved response');
        dialog.dismiss();
        const intent = Java.use('android.content.Intent').$new();
        intent.setClassName(app, 'jp.syoboi.a2chMate.activity.ArchiveListActivity');
        a.startActivity(intent);
    });
    setTimeout(function () { Java.perform(function () { Java.scheduleOnMainThread(function () {
        const a = current();
        check(String(a.getClass().getName()) === 'jp.syoboi.a2chMate.activity.ArchiveListActivity', 'native archive list opened');
        A.onActivityResumed(a);
        const archiveActivity = Java.retain(a);
        setTimeout(function () { Java.perform(function () { Java.scheduleOnMainThread(function () {
        const a = Java.cast(archiveActivity, Activity);
        const button = Java.cast(a.findViewById(0x7e484a02), Button);
        check(button !== null && button.isShown(), 'archive switcher visible');
        const parent = Java.cast(button.getParent(), Java.use('android.view.View'));
        const insets = parent.getRootWindowInsets();
        const xy = Java.array('int', [0, 0]); button.getLocationInWindow(xy);
        check(xy[1] >= insets.getSystemWindowInsetTop(), 'button below actual status bar');
        A.onActivityResumed(a);
        // Independently exercise inset changes, without changing system UI or stored settings.
        const fake = Java.use('android.widget.LinearLayout').$new(a);
        const b = Button.$new(a); fake.addView(b);
        const Insets = Java.use('android.view.WindowInsets$Builder');
        const Edge = Java.use('android.graphics.Insets');
        const safe = Insets.$new().setSystemWindowInsets(Edge.of(0, 72, 0, 0)).build();
        A.updateArchiveButtonInsets(fake, b, safe);
        const Params = Java.use('android.widget.LinearLayout$LayoutParams');
        check(Java.cast(b.getLayoutParams(), Params).topMargin.value === 72, 'edge-to-edge top inset applied');
        A.updateArchiveButtonInsets(fake, b, safe);
        check(Java.cast(b.getLayoutParams(), Params).topMargin.value === 72, 'repeated insets do not accumulate');
        const zero = Insets.$new().setSystemWindowInsets(Edge.of(0, 0, 0, 0)).build();
        A.updateArchiveButtonInsets(fake, b, zero);
        check(Java.cast(b.getLayoutParams(), Params).topMargin.value === 0, 'top margin resets after inset change');
        console.log('COMPLETE ' + checks + ' archive copy/inset checks');
        }); }); }, 500);
    }); }); }, 1800);
});
