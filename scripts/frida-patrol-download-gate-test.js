// No network or database changes; restore every preference touched by this test.
Java.perform(function () {
    const app=Java.use('android.app.ActivityThread').currentApplication();
    if(!app || !/\.haiagaru\.(uploadhosts191|responsearchive226|responsearchive242)$/.test(String(app.getPackageName())))
        throw Error('Fixed verification package required');
    const P=Java.use('app.morphe.extension.chmate.PatrolImages');P.initialize(app);
    const prefs=app.getSharedPreferences('io.github.areteruhiro.chmate.haiagaru.ui-config',0);
    const keys=['patrolImagesEnabled','patrolImageFilterEnabled','patrolImageKeywords','patrolImageUrls'];
    const before=keys.map(k=>({exists:prefs.contains(k),value:k==='patrolImageKeywords'||k==='patrolImageUrls'
        ? String(prefs.getString(k,'')):prefs.getBoolean(k,false)}));
    let count=0;
    try {
        for(const enabled of [false,true])for(const filter of [false,true])
            for(const words of ['', ' \n ', 'keyword'])for(const urls of ['', 'https://example.invalid/thread/'])
                for(const nativeFlag of [false,true]) {
                    prefs.edit().putBoolean(keys[0],enabled).putBoolean(keys[1],filter)
                        .putString(keys[2],words).putString(keys[3],urls).commit();
                    const expected=nativeFlag||(enabled&&(!filter||words.trim()!==''||urls!==''));
                    if(P.needsDat(nativeFlag)!==expected)throw Error('Download gate mismatch');
                    count++;
                }
        console.log('PASS '+count+' patrol download gate combinations (no HTTP)');
    } finally {
        const e=prefs.edit();keys.forEach((k,i)=>{
            if(!before[i].exists)e.remove(k);
            else if(typeof before[i].value==='boolean')e.putBoolean(k,before[i].value);
            else e.putString(k,before[i].value);
        });e.commit();
    }
});
