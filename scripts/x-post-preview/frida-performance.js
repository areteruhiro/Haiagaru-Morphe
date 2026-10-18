// Isolated source tests. No real requests, credentials, preferences or thread data.
Java.perform(function () {
    const app=Java.use('android.app.ActivityThread').currentApplication();
    if(!/\.haiagaru\.(uploadhosts191|responsearchive226|responsearchive242)$/.test(String(app.getPackageName())))throw Error('Verification app only');
    const bytes=Java.use('java.nio.file.Files').readAllBytes(Java.use('java.io.File').$new('/data/local/tmp/haiagaru-x-preview-performance.dex').toPath());
    const loader=Java.use('dalvik.system.InMemoryDexClassLoader').$new(
        Java.use('java.nio.ByteBuffer').wrap(bytes),Java.use('java.lang.ClassLoader').getSystemClassLoader().getParent());
    const types=Java.array('java.lang.Class',[Java.use('android.content.Context').class]);
    const args=Java.array('java.lang.Object',[app]);
    function status(name, attempts){
        const result=String(loader.loadClass('app.morphe.extension.chmate.'+name).getField('result').get(null));
        if(result==='pending' && attempts>0){setTimeout(()=>Java.perform(()=>status(name,attempts-1)),500);return;}
        console.log(result==='pending'?'FAIL '+name+' did not finish':result);
    }
    Java.scheduleOnMainThread(function(){try{
        const test=loader.loadClass('app.morphe.extension.chmate.PreviewTest');
        test.getMethod('run',types).invoke(null,args);
        const checks=test.getDeclaredField('checks');checks.setAccessible(true);
        console.log('PASS '+checks.getInt(null)+' Android preview regression checks');
        console.log('SAMPLE '+loader.loadClass('app.morphe.extension.chmate.CardRenderTest').getMethod('render',types).invoke(null,args));
        loader.loadClass('app.morphe.extension.chmate.PerformanceTest').getMethod('start',types).invoke(null,args);
        setTimeout(()=>Java.perform(()=>status('PerformanceTest',20)),500);
        loader.loadClass('app.morphe.extension.chmate.RetryTest').getMethod('start',types).invoke(null,args);
        setTimeout(()=>Java.perform(()=>status('RetryTest',4)),12500);
        Java.choose('jp.syoboi.a2chMate.activity.HomeActivity',{
            onMatch:function(activity){
                if(activity.isFinishing()||!activity.getWindow().getDecorView().isShown())return;
                const retained=Java.retain(activity);
                Java.scheduleOnMainThread(function(){
                    loader.loadClass('app.morphe.extension.chmate.PositionTest')
                        .getMethod('start',Java.array('java.lang.Class',[Java.use('android.app.Activity').class]))
                        .invoke(null,Java.array('java.lang.Object',[retained]));
                });
                setTimeout(()=>Java.perform(()=>status('PositionTest',12)),3000);
                return 'stop';
            },onComplete:function(){}
        });
    }catch(e){console.log('FAIL '+e);if(e.cause)console.log('CAUSE '+e.cause);}});
});
