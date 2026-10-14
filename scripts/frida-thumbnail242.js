// Read-only thumbnail routing diagnostics. No post bodies or credentials.
Java.perform(function () {
    var Row = Java.use('o.zzbhe');
    var Thumbnail = Java.use('o.zzbes');
    var click = Row.onClick.overload('android.view.View');
    click.implementation = function (view) {
        console.log('[thumbnail] click class=' + view.getClass().getName());
        try {
            var thumb = Java.cast(view, Thumbnail);
            console.log('[thumbnail] loaded=' + (thumb.a.value !== null));
            var list = this.e.value;
            console.log('[thumbnail] selected=' + list.indexOf(view) + ' visible=' + list.size());
            var projection = this.mRes.value.i();
            console.log('[thumbnail] projected=' + projection.length);
        } catch (error) { console.log('[thumbnail] state error=' + error); }
        try { return click.call(this, view); }
        catch (error) { console.log('[thumbnail] click error=' + error); throw error; }
    };
    var Viewer = Java.use('o.TTRewardVideoActivity1');
    var open = Viewer.e.overload('android.app.Activity', 'int', 'android.view.View', 'java.util.ArrayList', 'int', 'java.util.ArrayList');
    open.implementation = function (activity, mode, view, urls, index, tags) {
        console.log('[thumbnail] viewer request items=' + urls.size() + ' index=' + index);
        try { return open.call(this, activity, mode, view, urls, index, tags); }
        catch (error) { console.log('[thumbnail] viewer error=' + error); throw error; }
    };
    console.log('[thumbnail] trace ready');
});
