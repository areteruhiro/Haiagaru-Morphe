Java.perform(function () {
    const app=Java.use('android.app.ActivityThread').currentApplication();
    if(String(app.getPackageName()).indexOf('uploadhosts191')<0)throw Error('Emulator fixture app only');
    Java.openClassFile('/data/local/tmp/haiagaru-dat-row.dex').load();
    const File=Java.use('java.io.File');
    const folder=File.$new(app.getCacheDir(),'haiagaru-dat-test/dat');folder.mkdirs();
    const target=File.$new(folder,'fixture.dat');
    const input=Java.use('java.io.FileInputStream').$new('/data/local/tmp/haiagaru-dat-row-fixture.dat');
    const output=Java.use('java.io.FileOutputStream').$new(target);
    const buffer=Java.array('byte',new Array(65536).fill(0));let n;
    while((n=input.read(buffer))!==-1)output.write(buffer,0,n);input.close();output.close();
    const parser=Java.use('o.processAdapterInitializationPostback');
    const before=parser.b(target,1).size();
    Java.use('app.morphe.extension.chmate.RestoredDatRows').prepare(target);
    const after=parser.b(target,1).size();
    console.log('Native parser BEFORE='+before+' AFTER='+after+' bytes='+target.length());
    if(before!==360||after!==361||Number(target.length())!==104302)throw Error('Native DAT repair failed');
    console.log('PASS native parser retains erased response slot and numbering');
});
