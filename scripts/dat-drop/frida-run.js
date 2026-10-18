// Run only on the explicitly selected emulator. Synthetic rows, no native history DB writes.
Java.perform(function(){
    function run(){
        const app=Java.use('android.app.ActivityThread').currentApplication();
        if(app===null){setTimeout(run,1000);return;}
        const name=String(app.getPackageName());
        if(!/^jp\.co\.airfront\.android\.a2chMate\.haiagaru(?:\.responsearchive(?:226|242))?$/.test(name))throw Error('Verification package only');
        Java.openClassFile('/data/local/tmp/haiagaru-dropped-threads-test.dex').load();
        try{
            const test=Java.use('app.morphe.extension.chmate.DroppedThreadsAndroidTest');
            console.log(String(test.run(app)));
        }catch(error){console.log('FAIL '+error);if(error.$h)console.log(Java.use('android.util.Log').getStackTraceString(error));}
    }
    setTimeout(run,3000);
});
