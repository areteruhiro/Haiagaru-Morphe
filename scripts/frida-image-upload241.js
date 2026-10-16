// Read-only upload tracing. Does not log image bytes, URIs, credentials or request bodies.
Java.perform(function () {
    var Task = Java.use('o.GNk35');
    var upload = Task.c.overload();
    var active = 0;
    function failure(error) {
        try {
            var frames = error.getStackTrace();
            console.log('[upload241] exception class: ' + error.getClass().getName());
            for (var i = 0; i < frames.length; i++) console.log(' at ' + frames[i]);
        } catch (_) {
            console.log('[upload241] exception (no Java stack available)');
        }
    }
    upload.implementation = function () {
        active++;
        console.log('[upload241] start');
        try {
            var result = upload.call(this);
            console.log('[upload241] completed; result null=' + (result === null));
            return result;
        } catch (error) {
            failure(error);
            throw error;
        } finally {
            active--;
        }
    };
    var Method = Java.use('java.lang.reflect.Method');
    var invoke = Method.invoke.overload('java.lang.Object', '[Ljava.lang.Object;');
    invoke.implementation = function (receiver, args) {
        if (active) console.log('[upload241] reflection: ' + this.getDeclaringClass().getName() + '.' + this.getName());
        try {
            return invoke.call(this, receiver, args);
        } catch (error) {
            if (active) failure(error);
            throw error;
        }
    };
    console.log('[upload241] read-only tracing ready');
});
