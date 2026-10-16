// Verify the native binding inheritance adapter on the isolated test app.
Java.perform(function () {
    const app = Java.use('android.app.ActivityThread').currentApplication();
    if (String(app.getPackageName()) !== 'jp.co.airfront.android.a2chMate.haiagaru.commands191') throw Error('Verification app only');
    const C = Java.use('app.morphe.extension.chmate.ThreadCommands');
    C.field.implementation = function (object, name) {
        let cls = object.getClass();
        while (cls !== null) {
            try { const f = cls.getDeclaredField(name); f.setAccessible(true); return f.get(object); }
            catch (e) { cls = cls.getSuperclass(); }
        }
        throw Error('Missing native field ' + name);
    };
    Java.choose('o.p9ExternalSyntheticLambda6', {onMatch: function (fragment) {
        const f = Java.retain(fragment);
        Java.scheduleOnMainThread(function () { C.attach(f, f.getView()); console.log('Reattached native thread command UI'); });
    }, onComplete: function () {}});
});
