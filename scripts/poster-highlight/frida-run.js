Java.perform(function(){
    const app=Java.use('android.app.ActivityThread').currentApplication();
    if(!/^jp\.co\.airfront\.android\.a2chMate\.haiagaru\.responsearchive(191|226|242)$/.test(String(app.getPackageName())))
        throw Error('Isolated verification package required');
    const loader=Java.use('dalvik.system.DexClassLoader').$new('/data/local/tmp/haiagaru-poster-highlight.dex',
        String(app.getCodeCacheDir().getPath()),null,Java.use('java.lang.ClassLoader').getSystemClassLoader().getParent());
    Java.scheduleOnMainThread(function(){
        try {
            const clazz=loader.loadClass('app.morphe.extension.chmate.HighlightTest');
            const result=clazz.getMethod('run',Java.array('java.lang.Class',[Java.use('android.content.Context').class]))
                .invoke(null,Java.array('java.lang.Object',[app]));
            console.log(String(result));
        }catch(error){
            console.log('FAIL '+error);
            if(error.$h)console.log(Java.use('android.util.Log').getStackTraceString(error));
            throw error;
        }
    });
});
