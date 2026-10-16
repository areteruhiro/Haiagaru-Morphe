// Dedicated test package only. No uploads or account access; native gesture recognition.
Java.perform(function () {
    const app = Java.use('android.app.ActivityThread').currentApplication();
    if (!String(app.getPackageName()).endsWith('.videooption191')) throw Error('Isolated app only');
    const A = Java.use('app.morphe.extension.chmate.VideoViewerActivity');
    const G = Java.use('app.morphe.extension.chmate.VideoViewerGestures');
    const P = Java.use('android.preference.PreferenceManager').getDefaultSharedPreferences(app);
    const M = Java.use('android.view.MotionEvent');
    const Clock = Java.use('android.os.SystemClock');
    const original = A.performVideoGesture.overload('java.lang.String');
    let actions = [];
    original.implementation = function(key) {
        actions.push(String(key));
        console.log('recognized=' + key);
        return original.call(this,key);
    };
    console.log('native recognizer=' + G.recognizerClassName());
    Java.scheduleOnMainThread(function () {
        let active = null;
        const T = Java.use('android.app.ActivityThread');
        for (const item of T.currentActivityThread().mActivities.value.values().toArray()) {
            const record = Java.cast(item, Java.use('android.app.ActivityThread$ActivityClientRecord'));
            if (record.activity.value && record.activity.value.hasWindowFocus()) active = record.activity.value;
        }
        if (!active) throw Error('No focused activity');
        // Native image/video click helper; normal app launch path, no shell Activity bypass.
        Java.use('o.lo').b.overload('android.app.Activity','int','java.lang.String','boolean').call(
            Java.use('o.lo'),active,0,'https://i.imgur.com/A54Xv8k.mp4',true);
    });
    setTimeout(function () { Java.perform(function () { Java.scheduleOnMainThread(function () {
        Java.choose('app.morphe.extension.chmate.VideoViewerActivity', {onMatch:function(a) {
            if(a.isFinishing()||a.isDestroyed())return;
            console.log('viewer focus='+a.hasWindowFocus()+' recognizer-null='+(a.gestures.value.recognizer.value===null));
            a.controls.value.hide();
            const g=a.gestures.value;
            if(g.recognizer.value===null)throw Error('No recognizer');
            P.edit().putBoolean('gesture',true).putInt('gestureLength',40).putInt('gestureBack',4).commit();
            // Native encoding: 4 is left. Exercise actual MotionEvents, not the completion handler.
            const t=Clock.uptimeMillis();
            for(const p of [[0,800,900,0],[2,600,900,40],[2,300,900,80],[1,200,900,120]]) {
                const e=M.obtain(t,t+p[3],p[0],p[1],p[2],0);
                a.dispatchTouchEvent(e);e.recycle();
            }
            setTimeout(function(){console.log('actions='+JSON.stringify(actions));if(!actions.includes('gestureBack'))throw Error('Back gesture failed');},500);
        },onComplete:function(){}});
    });});},3000);
});
