setImmediate(function(){Java.perform(function(){
    const app=Java.use('android.app.ActivityThread').currentApplication();
    if(!/\.haiagaru\.responsearchive(191|226|242)$/.test(String(app.getPackageName())))throw Error('Verification app required');
    const loader=Java.use('dalvik.system.DexClassLoader').$new('/data/local/tmp/haiagaru-x-cache-test.dex',String(app.getCodeCacheDir().getPath()),null,Java.use('java.lang.ClassLoader').getSystemClassLoader().getParent());
    const test=loader.loadClass('app.morphe.extension.chmate.CacheTest');
    try { console.log(String(test.getMethod('run',Java.array('java.lang.Class',[Java.use('android.content.Context').class])).invoke(null,Java.array('java.lang.Object',[app])))); }
    catch(error) { console.log('FAIL '+error);if(error.cause)console.log('CAUSE '+error.cause);throw error; }
});});
