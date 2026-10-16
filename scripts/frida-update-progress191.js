'use strict';
Java.perform(function () {
    const progress = Java.use('o.r8lambdahIGIGCNpKpFqE0lgDli724UCuDM').c.overload('int', 'int');
    const System = Java.use('java.lang.System');
    let calls = 0, durationNs = 0;
    progress.implementation = function (current, total) {
        calls++;
        const start = Number(System.nanoTime());
        try { return progress.call(this, current, total); }
        finally { durationNs += Number(System.nanoTime()) - start; }
    };
    setInterval(function () {
        console.log(JSON.stringify({ progressCallbacks: calls, callbackMs: durationNs / 1000000 }));
        calls = 0; durationNs = 0;
    }, 5000);
    console.log('Progress counter ready (no response or URL logging)');
});
