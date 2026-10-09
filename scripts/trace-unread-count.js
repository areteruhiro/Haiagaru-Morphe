// Controlled in-memory notification test. Restore the original count; never write the DB.
Java.perform(function () {
    const app = Java.use('android.app.ActivityThread').currentApplication();
    const version = app.getPackageManager().getPackageInfo(app.getPackageName(), 0).versionName.value;
    const legacy = version === '0.8.10.241';
    if (!legacy && version !== '0.8.10.242 dev') throw new Error('Unsupported test version: ' + version);
    const VM = Java.use('jp.syoboi.a2chMate.ui.threadlist.ThreadListViewModel');
    const State = Java.use(legacy ? 'o.zzazi' : 'o.zzcpr');
    const Board = Java.use(legacy ? 'o.hLn11$IconCompatParcelizer' : 'o.VN21$ComponentActivity');
    const setter = legacy ? 'b' : 'c';
    const compute = legacy ? 'c' : 'a';
    const getter = legacy ? 'b' : 'd';
    const MapType = Java.use('java.util.HashMap');
    const LongType = Java.use('java.lang.Long');
    let selected = false;
    function field(object, name) {
        const f = object.getClass().getDeclaredField(name);
        f.setAccessible(true);
        return f.get(object);
    }
    Java.choose(VM.$className, {
        onMatch: function (candidate) {
            if (selected) return;
            const boardObject = field(candidate, legacy ? 'z' : 'A');
            if (boardObject === null) return;
            const board = Java.cast(boardObject, Board);
            const counts = Java.cast(field(board, legacy ? 'a' : 'c'), MapType);
            const entries = counts.entrySet().iterator();
            while (entries.hasNext()) {
                const entry = Java.cast(entries.next(), Java.use('java.util.Map$Entry'));
                const count = parseInt(entry.getValue().toString());
                if (count < 1) continue;
                selected = true;
                const vm = Java.retain(candidate);
                const retainedBoard = Java.retain(board);
                const created = Java.cast(entry.getKey(), LongType).longValue();
                const state = Java.retain(Java.cast(field(vm, legacy ? 'M' : 'L'), State));
                console.log(version + ' BEFORE computed=' + VM[compute].overload(VM.$className).call(VM, vm) + ' toolbar=' + state[getter]());
                retainedBoard[setter].overload('long', 'int').call(retainedBoard, created, count - 1);
                setTimeout(function () {
                    Java.perform(function () {
                        try {
                            console.log('EVENT computed=' + VM[compute].overload(VM.$className).call(VM, vm) + ' toolbar=' + state[getter]());
                        } finally {
                            retainedBoard[setter].overload('long', 'int').call(retainedBoard, created, count);
                        }
                        setTimeout(function () {
                            Java.perform(function () {
                                console.log('RESTORED computed=' + VM[compute].overload(VM.$className).call(VM, vm) + ' toolbar=' + state[getter]());
                            });
                        }, 500);
                    });
                }, 800);
                break;
            }
        },
        onComplete: function () { if (!selected) console.log('No active board with a memory read count'); }
    });
});
