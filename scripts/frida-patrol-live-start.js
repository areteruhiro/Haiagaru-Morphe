// Dedicated empty verification apps only. No posting and no synthetic Worker calls.
Java.perform(function(){
    const app=Java.use('android.app.ActivityThread').currentApplication();
    const pkg=String(app.getPackageName());
    const modern=pkg==='jp.co.airfront.android.a2chMate.haiagaru.responsearchive242';
    if(!modern && pkg!=='jp.co.airfront.android.a2chMate.haiagaru.responsearchive191')throw Error('Verification app required');
    const P=Java.use(modern?'jp.syoboi.a2chMate.prefs.Prefs':'jp.syoboi.a2chMate.Prefs');
    const fields=P.class.getDeclaredFields();let preferences=null;
    for(let n=0;n<fields.length;n++)if(String(fields[n].getType().getName())==='android.content.SharedPreferences'){
        fields[n].setAccessible(true);const value=fields[n].get(null);
        if(value!==null){preferences=Java.cast(value,Java.use('android.content.SharedPreferences'));break;}
    }
    if(preferences===null)throw Error('Native preferences unavailable');
    const minutes=modern?10:5;
    preferences.edit().putInt('reloadBookmarkInterval',minutes).commit();
    Java.use(modern?'o.TTRewardVideoActivity8':'o.mdExternalSyntheticLambda0').e(app);
    console.log('NATIVE_PATROL_SCHEDULE '+JSON.stringify({package:pkg,minutes:minutes,start:Date.now()}));
});
