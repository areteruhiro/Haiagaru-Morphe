/* Read-only diagnostics for stock ChMate 0.8.10.191 dev.
 * Does not log URLs, cookies, headers, response bodies or exception messages.
 * Attach by PID; trigger board/thread updates manually, then detach.
 * Timing includes instrumentation overhead and is not a packet capture.
 */
'use strict';
Java.perform(function () {
    const Thread = Java.use('java.lang.Thread');
    let sequence = 0;
    function emit(event, values) {
        console.log(JSON.stringify(Object.assign({ event: event, at: Date.now() }, values)));
    }
    function timed(klass, name, parameters, phase) {
        try {
            const method = Java.use(klass)[name].overload.apply(Java.use(klass)[name], parameters);
            method.implementation = function () {
                const args = Array.prototype.slice.call(arguments);
                const id = ++sequence;
                const start = Date.now();
                emit('begin', { id: id, phase: phase, thread: String(Thread.currentThread().getId()) });
                try {
                    const result = method.call.apply(method, [this].concat(args));
                    emit('end', { id: id, elapsedMs: Date.now() - start });
                    return result;
                } catch (error) {
                    emit('failed', { id: id, elapsedMs: Date.now() - start,
                        exception: error.$className || 'JavaException' });
                    throw error;
                }
            };
            emit('hook', { phase: phase });
        } catch (_) {
            emit('unavailable', { phase: phase });
        }
    }
    timed('java.net.InetAddress', 'getAllByName', ['java.lang.String'], 'dns');
    timed('java.net.Socket', 'connect', ['java.net.SocketAddress', 'int'], 'tcp-connect');
    timed('o.putFloat', 'e', [], 'okhttp-execute-to-headers');
    function snapshot() {
        Java.perform(function () {
            const entries = Thread.getAllStackTraces().entrySet().iterator();
            const stacks = [];
            while (entries.hasNext() && stacks.length < 30) {
                const entry = Java.cast(entries.next(), Java.use('java.util.Map$Entry'));
                const thread = Java.cast(entry.getKey(), Thread);
                const frames = Java.cast(entry.getValue(), Java.use('[Ljava.lang.StackTraceElement;'));
                const lines = [];
                for (let i = 0; i < frames.length; i++) lines.push(String(frames[i]));
                if (!lines.some(line => /o\.(getLabel|putFloat|getCredentials)|java\.net\.|com\.android\.org\.conscrypt\.|libcore\.io\./.test(line))) continue;
                stacks.push({ thread: String(thread.getId()), state: String(thread.getState()), frames: lines.slice(0, 32) });
            }
            emit('network-thread-stacks', { stacks: stacks });
        });
    }
    // Read an existing singleton only. Do not create a new client or change policy.
    try {
        const field = Java.use('o.getCredentials').class.getDeclaredField('d');
        field.setAccessible(true);
        const client = field.get(null);
        emit('native-client', { initialized: client !== null });
    } catch (_) { emit('native-client', { available: false }); }
    snapshot();
    setInterval(snapshot, 10000);
    emit('ready', {});
});
