// Intercept the system queue: validate requests without downloading or touching user files.
Java.perform(function () {
    const app = Java.use('android.app.ActivityThread').currentApplication();
    if (String(app.getPackageName()) !== 'jp.co.airfront.android.a2chMate.haiagaru.responsearchive191')
        throw Error('Isolated verification package required');
    const Downloads = Java.use('app.morphe.extension.chmate.VideoDownloads');
    const DM = Java.use('android.app.DownloadManager');
    const Looper = Java.use('android.os.Looper');
    const URI = Java.use('android.net.Uri');
    const signed = 'https://i.imgur.com/A54Xv8k.mp4?haiagaru_test=signature';
    let checks = 0, requests = 0;
    function check(value, label) { if (!value) throw Error(label); checks++; }
    function field(object, name) {
        const f = object.getClass().getDeclaredField(name); f.setAccessible(true); return f.get(object);
    }
    const enqueue = DM.enqueue.overload('android.app.DownloadManager$Request');
    enqueue.implementation = function (request) {
        {
            requests++;
            check(Looper.myLooper() === null || !Looper.myLooper().equals(Looper.getMainLooper()), 'Queue runs off UI thread');
            check(String(field(request, 'mUri')) === signed, 'Exact signed URL retained');
            const destination = String(field(request, 'mDestinationUri'));
            check(destination.indexOf('/Download/Haiagaru/A54Xv8k-') >= 0, 'Public Haiagaru destination');
            check(destination.endsWith('.mp4') && destination.indexOf('signature') < 0, 'MP4 without query leakage');
            check(String(field(request, 'mMimeType')) === 'video/mp4', 'Video MIME');
            check(String(field(request, 'mNotificationVisibility')) === '1', 'Completion notification');
            check(Java.cast(field(request, 'mRequestHeaders'), Java.use('java.util.List')).size() === 0, 'No login cookies');
            console.log('PASS ' + checks + ' video download request checks');
        }
        return 424242;
    };
    Downloads.enqueue(app, signed);
    // This must be rejected before enqueue; it must not become a YouTube extraction request.
    Downloads.enqueue(app, 'https://www.youtube.com/watch?v=abcdefghijk');
    setTimeout(function () {
        Java.perform(function () {
            check(requests === 1, 'Only the supported direct video was queued');
            console.log('PASS ' + checks + ' total checks; no network/file writes');
            enqueue.implementation = null;
        });
    }, 3000);
});
