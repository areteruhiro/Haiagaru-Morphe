Java.perform(function () {
    const app=Java.use('android.app.ActivityThread').currentApplication();
    if(String(app.getPackageName())!=='jp.co.airfront.android.a2chMate.haiagaru')throw Error('Wrong application');
    Java.openClassFile('/data/local/tmp/haiagaru-dat-row-phone.dex').load();
    const File=Java.use('java.io.File');
    const dat=File.$new('/storage/emulated/0/Android/data/jp.co.airfront.android.a2chMate.haiagaru/files/2chMate/dat/android_1792102288.dat');
    const parser=Java.use('o.processAdapterInitializationPostback');
    console.log('BEFORE='+parser.b(dat,1).size());
    Java.use('app.morphe.extension.chmate.RestoredDatRows').prepare(dat);
    console.log('AFTER='+parser.b(dat,1).size());
    // Discard only the weak response-list cache; keep DB and read positions intact.
    const cache=parser.class.getDeclaredField('d');cache.setAccessible(true);cache.set(null,null);
    console.log('Response cache invalidated; original DAT retained in haiagaru-dat-recovery');
});
