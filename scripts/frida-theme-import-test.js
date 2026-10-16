/* Only isolated verification APKs. Uses app-cache fixtures, not user themes. */
'use strict';
setTimeout(() => Java.perform(function () {
    const app = Java.use('android.app.ActivityThread').currentApplication();
    if (!app || !/^jp\.co\.airfront\.android\.a2chMate\.haiagaru\.patrol(191|226|242)$/.test(String(app.getPackageName()))) {
        console.log('FAIL Isolated test app required'); return;
    }
    const T = Java.use('app.morphe.extension.chmate.ThemeImportActivity');
    const File = Java.use('java.io.File');
    const URI = Java.use('android.net.Uri');
    const StringClass = Java.use('java.lang.String');
    const Output = Java.use('java.io.FileOutputStream');
    const Input = Java.use('java.io.FileInputStream');
    const Batch = Java.use(T.copy.overloads[0].argumentTypes[2].className);
    function write(file, text) {
        file.getParentFile().mkdirs();
        const output = Output.$new(file);
        output.write.overload('[B').call(output, StringClass.$new(text).getBytes('UTF-8')); output.close();
    }
    function read(file) {
        const input = Input.$new(file), bytes = Java.array('byte', new Array(Number(file.length())).fill(0));
        input.read.overload('[B').call(input, bytes); input.close();
        return String(StringClass.$new(bytes, 'UTF-8'));
    }
    T.onCreate.overload('android.os.Bundle').implementation = function (state) {
        T.onCreate.overload('android.os.Bundle').call(this, state);
        const activity = Java.retain(this);
        const Worker = Java.registerClass({name: 'app.morphe.test.ThemeWorker' + Date.now(),
            implements: [Java.use('java.lang.Runnable')], methods: {run: function () {
                let checks = 0;
                function check(value, name) { if (!value) throw Error(name); checks++; console.log('PASS ' + name); }
                try {
                    const fixture = File.$new(app.getCacheDir(), 'theme-test-' + Date.now()); fixture.mkdirs();
                    const target = File.$new(fixture, 'target'); target.mkdirs();
                    const source = File.$new(fixture, 'source.txt');
                    const stage = File.$new(fixture, 'stage'); stage.mkdirs();
                    const batch = Batch.$new(stage);
                    const raw = '{\n// test comment\n textPrimaryColor:0xfffefefe\n}';
                    write(source, raw);
                    activity.copy(URI.fromFile(source), '検証.ml.txt', batch);
                    check(Number(batch.themes.value) === 1, 'ml-only theme accepted');
                    check(read(File.$new(stage, '検証.ml.txt')) === raw, 'original JSON bytes retained');
                    T.commit(batch, target);
                    check(read(File.$new(target, '検証.ml.txt')) === raw, 'native-layout destination written');
                    const batch2 = Batch.$new(stage);
                    write(source, '{textPrimaryColor:-1}');
                    activity.copy(URI.fromFile(source), '検証.ml.txt', batch2);
                    T.commit(batch2, target);
                    check(read(File.$new(target, '検証.ml.txt')) === '{textPrimaryColor:-1}', 'existing theme replaced');
                    const batch3 = Batch.$new(stage);
                    activity.copy(URI.fromFile(source), 'nested/別.night.txt', batch3);
                    T.commit(batch3, target);
                    check(File.$new(target, 'nested/別.night.txt').isFile(), 'one-level subfolder retained');
                    const version = String(app.getPackageName()).slice(-3);
                    const bindings = {
                        '191': ['o.lr', 'b', 'b'],
                        '226': ['o.ExchangeCodec', 'd', 'b'],
                        '242': ['o.onDialogBtnYes', 'a', 'c']
                    }[version];
                    const Loader = Java.use(bindings[0]);
                    const native = Loader.$new();
                    const found = Java.use('java.util.ArrayList').$new();
                    Loader[bindings[1]].overload('java.io.File', 'java.util.ArrayList', 'int').call(native, target, found, 1);
                    check(Number(found.size()) === 2, 'native theme list detects ml and nested night themes');
                    const parsed = Loader[bindings[2]].overload('android.content.Context', 'java.lang.String')
                        .call(native, app, String(File.$new(target, '検証.ml.txt').getAbsolutePath()));
                    check(parsed !== null, 'native theme parser loads imported ml theme');
                    const png = File.$new(fixture, 'pixel.png');
                    const pngOut = Output.$new(png);
                    pngOut.write.overload('[B').call(pngOut, Java.use('android.util.Base64').decode('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVQIHWP4z8DwHwAFgAI/ScLbtAAAAABJRU5ErkJggg==', 0));
                    pngOut.close();
                    const imageBatch = Batch.$new(stage);
                    activity.copy(URI.fromFile(png), 'nested/pixel.png', imageBatch);
                    T.commit(imageBatch, target);
                    check(Number(File.$new(target, 'nested/pixel.png').length()) === Number(png.length()), 'image asset retained');
                    write(source, 'invalid JSON');
                    let rejected = false;
                    try { activity.copy(URI.fromFile(source), '不正.day.txt', Batch.$new(stage)); } catch (e) { rejected = true; }
                    check(rejected, 'invalid JSON rejected before installation');
                    check(!File.$new(target, '不正.day.txt').exists(), 'invalid theme does not alter target');
                    write(source, '{textPrimaryColor:123}');
                    const rollback = Batch.$new(stage);
                    activity.copy(URI.fromFile(source), '検証.ml.txt', rollback);
                    activity.copy(URI.fromFile(source), 'blocked.day.txt', rollback);
                    File.$new(target, 'blocked.day.txt').mkdirs();
                    let failed = false;
                    try { T.commit(rollback, target); } catch (e) { failed = true; }
                    check(failed, 'write collision fails safely');
                    check(read(File.$new(target, '検証.ml.txt')) === '{textPrimaryColor:-1}', 'earlier replacement rolled back');
                    const safeStage = File.$new(fixture, 'safe-stage'); safeStage.mkdirs();
                    const safe = Batch.$new(safeStage);
                    activity.copy(URI.fromFile(source), '単独.ml.txt', safe);
                    T.prepareLayout(safe, '選択フォルダ', true);
                    T.commit(safe, target);
                    check(File.$new(target, '単独.ml/単独.ml.txt').isFile(), 'file import isolated from other themes for safe native deletion');
                    const folderStage = File.$new(fixture, 'folder-stage'); folderStage.mkdirs();
                    const folder = Batch.$new(folderStage);
                    activity.copy(URI.fromFile(source), '自作.day.txt', folder);
                    activity.copy(URI.fromFile(source), '別セット/自作.night.txt', folder);
                    activity.copy(URI.fromFile(png), '素材/pixel.png', folder);
                    T.prepareLayout(folder, '選択フォルダ', false);
                    T.commit(folder, target);
                    check(File.$new(target, '選択フォルダ/自作.day.txt').isFile(), 'folder root theme isolated');
                    check(File.$new(target, '別セット/自作.night.txt').isFile(), 'existing theme subfolder preserved');
                    check(File.$new(target, '選択フォルダ/素材/pixel.png').isFile(), 'nested image references preserved');
                    T.remove(fixture);
                    check(!fixture.exists(), 'test fixtures removed');
                    console.log('DONE ' + checks + ' theme runtime checks');
                } catch (e) { console.log('FAIL ' + e.stack); }
            }}});
        Java.use('java.lang.Thread').$new(Worker.$new()).start();
    };
    Java.scheduleOnMainThread(() => {
        const intent = Java.use('android.content.Intent').$new(app, T.class);
        intent.addFlags(0x10000000); app.startActivity(intent);
    });
}), 4000);
