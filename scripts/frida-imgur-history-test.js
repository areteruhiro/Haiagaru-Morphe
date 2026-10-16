// Isolated app only. No account API, upload, forum post or deletion requests.
Java.perform(function () {
    const app = Java.use('android.app.ActivityThread').currentApplication();
    if (!String(app.getPackageName()).endsWith('.imgurhistory191')) throw Error('Isolated test app only');
    const H = Java.use('app.morphe.extension.chmate.ImgurUploadHistory');
    const W = Java.use('app.morphe.extension.chmate.ImgurWebUpload');
    const P = app.getSharedPreferences('haiagaru_imgur_web', 0);
    W.init(app);
    const List = Java.use('java.util.ArrayList');
    const Row = Java.use('o.onRewardedInterstitialAdDisplayed');
    const JString = Java.use('java.lang.String');
    let checks = 0;
    function check(ok, title) { if (!ok) throw Error(title); console.log('PASS '+title); checks++; }
    function row(link, target) { return Row.$new(target, 'content://test/local', link, '', 1, null); }
    const link = 'https://i.imgur.com/Test123.png';
    const thread = 'https://example.com/test/read.cgi/test/1791989395/';
    app.getSharedPreferences('haiagaru_imgur_threads', 0).edit().clear().commit();
    P.edit().remove('nativeUploadHistory').remove('uploadThreadHistory').commit();
    check(H.nativeEnabled(app), 'native history defaults ON');
    check(!H.threadsEnabled(app), 'thread history defaults OFF');
    const list = List.$new();
    P.edit().putBoolean('nativeUploadHistory', false).commit();
    check(H.addNativeHistory(list, row(link,thread)) && list.size()===1, 'anonymous uploads unaffected by toggle');
    H.uploaded(link, 123);
    check(!H.addNativeHistory(list,row(link,thread)) && list.size()===1, 'logged-in native history OFF');
    P.edit().putBoolean('nativeUploadHistory',true).commit();
    H.uploaded(link,123);
    check(H.addNativeHistory(list,row(link,thread)) && list.size()===2, 'logged-in native history ON adds exactly one');
    check(H.threads(app,123).isEmpty(), 'no associations collected when OFF');
    P.edit().putBoolean('nativeUploadHistory',false).putBoolean('uploadThreadHistory',true).commit();
    H.uploaded(link,123);
    check(!H.addNativeHistory(list,row(link,thread)), 'thread recording independent of native history');
    check(H.threadUrl('https://example.com/test/read.cgi/a/1791989395') !== null, 'slashless thread allowed');
    check(H.threadUrl('https://example.com/boards/a/1791989395') !== null, 'Talk thread allowed');
    check(H.threadUrl('https://example.com/a/') === null, 'board URL rejected');
    check(H.threadUrl('file:///test/read.cgi/a/1791989395/') === null, 'local URL rejected');
    check(H.threadUrl('https://user:password@example.com/test/read.cgi/a/1791989395/') === null, 'credentials rejected');
    check(String(H.threadUrl(thread+'?secret=not-retained#test'))===thread, 'query and fragment not retained');
    setTimeout(function () { Java.perform(function () {
        check(H.threads(app,123).size()===1, 'worker persisted thread association');
        check(H.threads(app,456).isEmpty(), 'different account cannot see associations');
        const urls=Java.cast(H.threads(app,123).get(JString.$new(link)),Java.use('java.util.List'));
        check(urls.size()===1 && String(urls.get(0))===thread, 'exact media URL maps to source thread');
        P.edit().putBoolean('uploadThreadHistory',false).commit();
        check(H.threads(app,123).isEmpty(), 'OFF hides associations');
        console.log('HISTORY CHECKS='+checks);
    }); },1000);

    let evictions=0, cancels=0, badThread=false;
    function onMain() {
        const loop=Java.use('android.os.Looper').myLooper();
        return loop!==null && loop.equals(Java.use('android.os.Looper').getMainLooper());
    }
    const Call=Java.use('app.morphe.extension.isolated.hu0');
    const cancel=Call.cancel.overload();
    cancel.implementation=function(){cancels++;if(onMain())badThread=true;return cancel.call(this);};
    // Shaded names verified against this build's R8 mapping.txt.
    const Pool=Java.use('app.morphe.extension.isolated.he');
    const evict=Pool.b.overload();
    evict.implementation=function(){
        if(onMain())badThread=true;
        evictions++;return evict.call(this);
    };
    Java.scheduleOnMainThread(function(){
        const loader=Java.use('app.morphe.extension.chmate.ImgurThumbnailLoader').$new();
        const image=Java.use('android.widget.ImageView').$new(app);
        loader.load('https://i.imgur.com/LWkGnaIm.jpg',image);
        setTimeout(function(){Java.perform(function(){Java.scheduleOnMainThread(function(){
            loader.reset();loader.close();loader.close();
        });});},100);
        setTimeout(function(){Java.perform(function(){
            check(evictions===1 && !badThread,'idempotent close evicts pool off main thread');
            check(cancels>0 && !badThread,'in-flight cancellation off main thread');
            console.log('ALL CHECKS='+checks);
        });},2000);
    });
});
