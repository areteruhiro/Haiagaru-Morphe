// Read-only exception tracing. No arguments, request bodies, tokens or cookies are logged.
Java.perform(function () {
    var Throwable = Java.use('java.lang.Throwable');
    var Npe = Java.use('java.lang.NullPointerException');
    var Method = Java.use('java.lang.reflect.Method');
    var getCause = Throwable.getCause.overload();
    function stack(error, label) {
        if (error === null) return;
        try {
            if (!Npe.class.isInstance(error)) return;
            console.log('[Talk242 NPE] ' + label);
            var frames = error.getStackTrace();
            for (var i = 0; i < frames.length; i++) console.log(' at ' + frames[i].toString());
        } catch (_) {}
    }
    getCause.implementation = function () {
        var result = getCause.call(this);
        stack(result, 'unwrapped cause');
        return result;
    };
    var InvocationError = Java.use('java.lang.reflect.InvocationTargetException');
    var reflectedCause = InvocationError.getCause.overload();
    reflectedCause.implementation = function () {
        var result = reflectedCause.call(this);
        stack(result, 'reflected cause');
        return result;
    };
    var invoke = Method.invoke.overload('java.lang.Object', '[Ljava.lang.Object;');
    invoke.implementation = function (target, args) {
        try {
            return invoke.call(this, target, args);
        } catch (error) {
            // Method identity is sufficient to map the generated DEX; never log arguments.
            console.log('[Reflection failure] ' + this.getDeclaringClass().getName() + '.' + this.getName());
            throw error;
        }
    };
    console.log('Talk242 read-only NPE trace ready');
});
