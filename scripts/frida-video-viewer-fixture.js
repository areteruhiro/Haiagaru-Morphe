// Read-only playback of the user-supplied public Imgur video. No upload/delete/auth inspection.
setTimeout(function () { Java.perform(function () {
    const Thread = Java.use('android.app.ActivityThread');
    const app = Thread.currentApplication();
    const H = Java.use('app.morphe.extension.chmate.Haiagaru');
    const Viewer = Java.use('app.morphe.extension.chmate.VideoViewerActivity');
    const Intent = Java.use('android.content.Intent'), Uri = Java.use('android.net.Uri');
    const prefs = H.preferences(app);
    const key = 'videoViewerEnabled', had = prefs.contains(key), saved = prefs.getBoolean(key, true);
    function check(ok, label) { if (!ok) throw Error(label); console.log('PASS ' + label); }
    function intent(url) { return Intent.$new('android.intent.action.VIEW', Uri.parse(url)); }
    const url = 'https://i.imgur.com/A54Xv8k.mp4';
    const prepare = Viewer.prepareExternalIntent.overload('android.content.Intent');
    prepare.implementation = function (target) {
        const before = target.getComponent();
        prepare.call(Viewer,target);
        console.log('Prepare hook action=' + target.getAction() + ', beforeExplicit=' + (before !== null)
                + ', afterExplicit=' + (target.getComponent() !== null) + ', enabled=' + H.videoViewerEnabled());
    };
    const start = Java.use('android.app.Activity').startActivityForResult.overload('android.content.Intent','int');
    start.implementation = function(target, code) {
        console.log('Launch action=' + target.getAction() + ', explicit=' + (target.getComponent() !== null));
        return start.call(this,target,code);
    };
    try {
        prefs.edit().putBoolean(key, true).commit();
        const target = intent(url); Viewer.prepareExternalIntent(target);
        check(target.getComponent() !== null && String(target.getComponent().getClassName()) === String(Viewer.class.getName()), 'enabled routes public video internally');
        prefs.edit().putBoolean(key, false).commit();
        const off = intent(url); Viewer.prepareExternalIntent(off);
        check(off.getComponent() === null, 'disabled leaves native destination unchanged');
        prefs.edit().putBoolean(key, true).commit();
        for (const value of ['https://i.imgur.com/Test.png', 'https://i.imgur.com.evil/Test.mp4', 'http://i.imgur.com/Test.mp4']) {
            const other = intent(value); Viewer.prepareExternalIntent(other);
            check(other.getComponent() === null, 'unrelated or invalid URL not intercepted');
        }
        const explicit = intent(url).setClassName(app, 'jp.syoboi.a2chMate.activity.WebViewActivity');
        Viewer.prepareExternalIntent(explicit);
        check(String(explicit.getComponent().getClassName()) === 'jp.syoboi.a2chMate.activity.WebViewActivity', 'explicit native destinations preserved');
        Java.scheduleOnMainThread(function () {
            prefs.edit().putBoolean(key,true).commit();
            console.log('Native click viewer enabled=' + H.videoViewerEnabled());
            let active = null;
            for (const item of Thread.currentActivityThread().mActivities.value.values().toArray()) {
                const record = Java.cast(item, Java.use('android.app.ActivityThread$ActivityClientRecord'));
                if (record.activity.value && record.activity.value.hasWindowFocus()) active = record.activity.value;
            }
            if (!active) throw Error('No focused activity');
            // The native 191 thumbnail click uses this same helper.
            Java.use('o.lo').b.overload('android.app.Activity','int','java.lang.String','boolean').call(
                    Java.use('o.lo'), active, 0, url, true);
            const restore = prefs.edit(); if (had) restore.putBoolean(key,saved); else restore.remove(key); restore.commit();
            console.log('Native video click invoked');
        });
    } finally {
        const edit = prefs.edit(); if (had) edit.putBoolean(key,saved); else edit.remove(key); edit.commit();
    }
    setTimeout(function () { Java.perform(function () { Java.scheduleOnMainThread(function () {
        Java.choose('app.morphe.extension.chmate.VideoViewerActivity', {
            onMatch:function(activity) {
                if (activity.isFinishing() || activity.isDestroyed()) return;
                const video=activity.video.value;
                console.log('Player prepared=' + activity.prepared.value + ', playing=' + video.isPlaying()
                    + ', durationMs=' + video.getDuration() + ', positionMs=' + video.getCurrentPosition());
            }, onComplete:function(){}
        });
    }); }); }, 5000);
}); }, 3000);
