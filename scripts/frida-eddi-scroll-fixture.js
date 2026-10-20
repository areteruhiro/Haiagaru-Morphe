// UI-only regression fixture, restricted to isolated emulator packages.
Java.perform(function () {
    const Thread = Java.use('android.app.ActivityThread');
    const app = Thread.currentApplication();
    if (!/\.haiagaru\.(resumefix226|resumefix242|full191)$/.test(String(app.getPackageName())))
        throw Error('Regression package required');
    Java.scheduleOnMainThread(function () {
        const records = Thread.currentActivityThread().mActivities.value.values().toArray();
        const ActivityRecord = Java.use('android.app.ActivityThread$ActivityClientRecord');
        let controller = null;
        for (const record of records) {
            const activity = Java.cast(record, ActivityRecord).activity.value;
            if (!activity || !activity.hasWindowFocus()) continue;
            for (const field of activity.getClass().getDeclaredFields()) {
                if (String(field.getType().getName()) !== 'app.morphe.extension.chmate.EddiArchiveSearchUi') continue;
                field.setAccessible(true); controller = field.get(activity);
            }
        }
        if (!controller) throw Error('Open Edge archive search first');
        const cls = controller.getClass();
        let parse = null, display = null;
        for (const method of cls.getDeclaredMethods()) {
            const args = method.getParameterTypes();
            const result = String(method.getReturnType().getName());
            if (args.length === 1 && String(args[0].getName()) === 'java.lang.String' && result.endsWith('$SearchPage')) parse = method;
            if (args.length === 2 && String(args[0].getName()) === 'int' && String(args[1].getName()).endsWith('$SearchPage')) display = method;
        }
        if (!parse || !display) throw Error('Search model methods missing');
        let html = '<main id="threadContainer">検索結果 100件';
        for (let i = 1; i <= 50; i++) html += '<div class="thread"><a class="title" href="https://egg.5ch.io/test/read.cgi/android/1792102288/658">SCROLL-TEST-' + String(i).padStart(2, '0') + '</a><p class="date">2026-10-20</p></div>';
        parse.setAccessible(true); display.setAccessible(true);
        try {
            const page = parse.invoke(controller, Java.array('java.lang.Object', [Java.use('java.lang.String').$new(html)]));
            display.invoke(controller, Java.array('java.lang.Object', [Java.use('java.lang.Integer').valueOf(1), page]));
        } catch (e) {
            console.log('FIXTURE_ERROR ' + e);
            if (e.cause) console.log(String(e.cause));
            try { console.log(Java.cast(e, Java.use('java.lang.Throwable')).getCause()); } catch (_) {}
            throw e;
        }
        console.log('FIXTURE_READY rows=50');
    });
});
