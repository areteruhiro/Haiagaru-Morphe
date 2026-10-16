// Isolated emulator apps only; never changes the user's sent-history file or posts.
Java.perform(function() {
    const app=Java.use('android.app.ActivityThread').currentApplication();
    const pkg=String(app.getPackageName());
    if(!/^jp\.co\.airfront\.android\.a2chMate\.haiagaru\.(count(191|226|241)|imgur242)$/.test(pkg))throw Error('Isolated app required');
    const v191=pkg.endsWith('191'),v226=pkg.endsWith('226'),v241=pkg.endsWith('241'),legacy=v191||v226,key='postDataListCount';
    const prefs=Java.use('android.preference.PreferenceManager').getDefaultSharedPreferences(app);
    const had=prefs.contains(key), previous=prefs.getInt(key,100);
    const file=Java.use('java.io.File').$new(app.getCacheDir(),'haiagaru-retention-fixture.json');
    const fixturePath=String(file.getAbsolutePath());
    if(file.exists())throw Error('Fixture path already exists');
    const Post=Java.use(v191?'o.n7':v226?'o.setBorderWidth':'jp.syoboi.a2chMate.postdata.PostData');
    const Owner=Java.use(v191?'o.n9':v226?'o.getTileModeX':v241?'o.KP':'o.PackageSignatureVerifier');
    const Wrapped=legacy?Java.registerClass({
        name:'app.haiagaru.test.RetentionContext',superClass:Java.use('android.content.ContextWrapper'),
        methods:{$init:[{returnType:'void',argumentTypes:['android.content.Context'],implementation:function(base){this.$super.$init(base);}}],getFileStreamPath:[{returnType:'java.io.File',argumentTypes:['java.lang.String'],implementation:function(name){
            if(String(name)!=='postDataList.json')throw Error('Unexpected file');return Java.use('java.io.File').$new(fixturePath);
        }}]}
    }):null;
    const context=legacy?Wrapped.$new(app):null;
    const Haiagaru=Java.use('app.morphe.extension.chmate.Haiagaru');
    try {
        for(const count of [20,3,0,100,2147483647]) {
            prefs.edit().putInt(key,count).commit();
            if(Haiagaru.getPostHistoryRetentionCount()!==count)throw Error('Preference not read');
            const history=legacy?Owner.$new.overload('android.content.Context','int').call(Owner,context,count):Owner.$new(file);
            // Reflect the ArrayList to avoid Frida's field/method-name collisions.
            const field=Owner.class.getDeclaredField(v191?'d':v226?'b':v241?'a':'e');field.setAccessible(true);
            const list=Java.cast(field.get(history),Java.use('java.util.ArrayList'));list.clear();
            for(let i=0;i<25;i++) {
                const url='https://egg.5ch.io/test/read.cgi/android/'+(1900018500+i)+'/';
                let row;
                if(v191){row=Post.$new();row.l.value=url;row.j.value=Date.now();row.b.value='body';}
                else row=Post.$new(url,
                    'fixture','','','body','',Date.now(),Date.now(),'','','');
                (v191?history.d.overload('o.n7'):v226?history.c.overload('o.setBorderWidth'):v241?history.c.overload('jp.syoboi.a2chMate.postdata.PostData')
                    :history.b.overload('jp.syoboi.a2chMate.postdata.PostData')).call(history,row);
            }
            const expected=Math.min(count,25);
            if(list.size()!==expected)throw Error('Retention '+count+': got '+list.size());
            console.log('PASS retention '+pkg+' setting='+count+', actual='+list.size());
            file.delete();
        }
    } finally {
        const editor=prefs.edit();if(had)editor.putInt(key,previous);else editor.remove(key);editor.commit();
        file.delete();console.log('Retention fixture removed and original preference restored');
    }
});
