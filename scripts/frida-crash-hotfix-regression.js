// Run in emulator-5554's isolated 1.11.0 regression packages after installing the fixed APK.
Java.perform(function () { Java.scheduleOnMainThread(function () {
    const app = Java.use('android.app.ActivityThread').currentApplication();
    const pkg = String(app.getPackageName());
    if (!/^jp\.co\.airfront\.android\.a2chMate\.haiagaru\.(full191|resumefix226|resumefix242)$/.test(pkg))
        throw Error('Isolated regression package required');
    const name = pkg.endsWith('191') ? 'o.m9ExternalSyntheticLambda1'
        : pkg.endsWith('226') ? 'jp.syoboi.a2chMate.activity.ImagesViewer2Activity' : 'o.zzaW';
    const Type = Java.use(name);
    const Unsafe = Java.use('sun.misc.Unsafe');
    // Reflection alone does not force ART to verify method bodies. Allocation does.
    const instance = Java.cast(Unsafe.theUnsafe.value.allocateInstance(Type.class), Type);
    console.log('PASS ART allocation verifies ' + name);
    if (pkg.endsWith('242')) {
        let checked = false;
        try { instance.c(null, false); }
        catch (e) {
            if (String(e).indexOf('VerifyError') >= 0) throw e;
            checked = String(e).indexOf('NullPointerException') >= 0;
        }
        if (!checked) throw Error('Expected the native response null check');
        console.log('PASS native NG method executes its original response null check');
    }
    console.log('DONE ' + pkg);
}); });
