// Capture only generated code before its in-memory header is erased by the app.
// No request arguments, account values or tokens are read.
Java.perform(function () {
    var Loader = Java.use('dalvik.system.InMemoryDexClassLoader');
    var Output = Java.use('java.io.FileOutputStream');
    var count = 0;
    function dump(buffer) {
        try {
            var copy = Java.cast(buffer.duplicate(), Java.use('java.nio.ByteBuffer'));
            copy.position(0);
            var size = copy.remaining();
            if (size < 112 || size > 4194304) return;
            var bytes = Java.array('byte', new Array(size).fill(0));
            copy.get(bytes);
            if (bytes[0] !== 100 || bytes[1] !== 101 || bytes[2] !== 120 || bytes[3] !== 10) return;
            var path = '/data/user/0/jp.co.airfront.android.a2chMate.haiagaru.talk242/cache/talk242-generated-' + count++ + '.dex';
            var stream = Output.$new(path);
            try { stream.write(bytes); } finally { stream.close(); }
            console.log('[Code dump] ' + path + ' bytes=' + size);
        } catch (error) { console.log('Code dump unavailable: ' + error); }
    }
    Loader.$init.overloads.forEach(function (init) {
        init.implementation = function () {
            var buffers = arguments[0];
            if (init.argumentTypes[0].className === 'java.nio.ByteBuffer') dump(buffers);
            else for (var i = 0; i < buffers.length; i++) dump(buffers[i]);
            return init.apply(this, arguments);
        };
    });
    console.log('Generated Talk code capture ready');
});
