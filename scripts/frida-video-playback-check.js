// Replay the already-open user-supplied video, then inspect pause after Home. No upload/delete.
Java.perform(function () {
    let viewer = null;
    Java.scheduleOnMainThread(function () {
        Java.choose('app.morphe.extension.chmate.VideoViewerActivity', {
            onMatch:function(activity) {
                if (!activity.isFinishing() && !activity.isDestroyed() && activity.foreground.value)
                    viewer = Java.retain(activity);
            }, onComplete:function() {
                if (!viewer) throw Error('No focused video viewer');
                viewer.video.value.seekTo(0); viewer.video.value.start();
            }
        });
    });
    function inspect(label) {
        Java.perform(function () { Java.scheduleOnMainThread(function () {
            if (!viewer) return;
            console.log(label + ' playing=' + viewer.video.value.isPlaying()
                + ', positionMs=' + viewer.video.value.getCurrentPosition()
                + ', foreground=' + viewer.foreground.value);
        }); });
    }
    setTimeout(function(){inspect('Replay');}, 2500);
    setTimeout(function(){ Java.perform(function(){ Java.scheduleOnMainThread(function(){
        if (viewer) viewer.startActivity(Java.use('android.content.Intent').$new('android.intent.action.MAIN')
                .addCategory('android.intent.category.HOME'));
    }); }); }, 4000);
    setTimeout(function(){inspect('After Home');}, 8000);
});
