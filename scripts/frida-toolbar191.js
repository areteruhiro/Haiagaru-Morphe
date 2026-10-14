// Read-only toolbar dialog tracing; does not log thread/post contents.
Java.perform(function () {
    var Dialog = Java.use('o.p8a');
    var create = Dialog.e.overload('java.lang.String',
        'o.r8lambdaElkXfNt4VbdvffL9Z700R6oMDo',
        'o.r8lambdaElkXfNt4VbdvffL9Z700R6oMDo', 'boolean');
    create.implementation = function (key, saved, defaults, size) {
        console.log('[toolbar191] create key=' + key);
        var frames = Java.use('java.lang.Exception').$new().getStackTrace();
        for (var i = 0; i < Math.min(frames.length, 10); i++) console.log(' at ' + frames[i]);
        return create.call(this, key, saved, defaults, size);
    };
    var dispatch = Java.use('o.r8lambdahIGIGCNpKpFqE0lgDli724UCuDM').e
        .overload('int', 'java.lang.Object');
    dispatch.implementation = function (id, value) {
        console.log('[toolbar191] dispatch id=' + id);
        return dispatch.call(this, id, value);
    };
    console.log('[toolbar191] trace ready');
});
