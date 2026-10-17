// Isolated synthetic preferences/files only; no MEGA or user NG changes.
Java.perform(function () {
    const app = Java.retain(Java.use('android.app.ActivityThread').currentApplication());
    if (String(app.getPackageName()) !== 'jp.co.airfront.android.a2chMate.haiagaru.backuptest191') throw Error('Dedicated test app required');
    const Snapshot = Java.use('app.morphe.extension.chmate.HaiagaruSyncSnapshot');
    const Archive = Java.use('app.morphe.extension.chmate.HaiagaruBackupArchive');
    const File = Java.use('java.io.File'), Json = Java.use('org.json.JSONObject');
    const prefix = 'advanced-ng-fixture-' + Date.now();
    const root = Java.retain(File.$new(app.getCacheDir(), prefix)); root.mkdirs();
    const rootPath = String(root.getAbsolutePath());
    const Context = Java.registerClass({
        name: 'app.haiagaru.test.AdvancedNgContext' + Date.now(),
        superClass: Java.use('android.content.ContextWrapper'),
        methods: {
            $init: [{returnType:'void', argumentTypes:['android.content.Context'], implementation:function(base){this.$super.$init(base);}}],
            getFilesDir: function(){return File.$new(rootPath);}, getCacheDir: function(){return File.$new(rootPath);},
            getExternalFilesDir: [{returnType:'java.io.File',argumentTypes:['java.lang.String'],implementation:function(type){return File.$new(rootPath);}}],
            getFileStreamPath: [{returnType:'java.io.File',argumentTypes:['java.lang.String'],implementation:function(name){return File.$new(rootPath + '/' + name);}}],
            getSharedPreferences: [{returnType:'android.content.SharedPreferences',argumentTypes:['java.lang.String','int'],implementation:function(name, mode){return app.getSharedPreferences(prefix + '.' + name, mode);}}]
        }
    });
    const context = Context.$new(app);
    const prefs = context.getSharedPreferences('haiagaru.programmable-ng', 0);
    const source = 'jp.co.airfront.android.a2chMate.original';
    const rules = 'fixture script: function(text){return text === "' + source + '";}\n板=https://example.org/board/\n有効=true';
    let checks = 0, backup = null;
    function check(ok, label){if(!ok)throw Error(label); checks++; console.log('PASS ' + label);}
    function capture(flags){return Snapshot.validate(Snapshot.captureMetadata(context, flags));}
    function clear(){prefs.edit().clear().commit();}
    function exact(){return String(prefs.getString('rules','')) === rules && prefs.getBoolean('enabled',false);}
    function cleanup(file){
        const path=String(file.getCanonicalPath()), base=String(root.getCanonicalPath());
        if(path!==base && !path.startsWith(base+'/'))throw Error('Outside fixture');
        if(file.isDirectory())for(const child of file.listFiles())cleanup(child);
        file.delete();
    }
    try {
        prefs.edit().putString('rules',rules).putBoolean('enabled',true).commit();
        const ng = capture(2); ng.put('sourcePackage',source);
        check(ng.has('programmableNg') && !ng.has('settings'),'NG-only includes advanced NG without general settings');
        clear(); Snapshot.restore(context,ng,2); check(exact(),'NG-only restore preserves script, flags and literal package name');
        backup=Archive.read(context,Archive.capture(context,2));
        clear(); Archive.restore(context,backup,2,false); check(exact(),'shared local/MEGA ZIP restore');
        clear(); Snapshot.mergeMissing(context,ng,2); check(exact(),'two-way imports absent preferences');
        prefs.edit().putString('rules','local').putBoolean('enabled',false).commit();
        Snapshot.mergeMissing(context,ng,2);
        check(String(prefs.getString('rules',''))==='local' && !prefs.getBoolean('enabled',true),'two-way retains existing local preferences');
        const legacy=Json.$new(ng.toString()); legacy.remove('programmableNg');
        Snapshot.restore(context,legacy,2); check(String(prefs.getString('rules',''))==='local','legacy NG-only does not erase rules');
        prefs.edit().putString('rules',rules).putBoolean('enabled',true).commit();
        const settings=capture(4); settings.put('sourcePackage',source);
        clear(); Snapshot.restore(context,settings,4); check(exact(),'settings-only preserves script literally across packages');
        const oldBoth=capture(6); oldBoth.remove('programmableNg');
        clear(); Snapshot.restore(context,oldBoth,2); check(exact(),'legacy combined backup can restore advanced NG via NG selection');
        prefs.edit().putBoolean('enabled',false).commit();
        const disabled=capture(2); clear(); Snapshot.restore(context,disabled,2);
        check(!prefs.getBoolean('enabled',true) && String(prefs.getString('rules',''))===rules,'disabled state and rules retained');
        clear(); const empty=capture(2); Snapshot.restore(context,empty,2);
        check(prefs.getAll().isEmpty(),'empty preferences round-trip');
        check(!capture(8).has('programmableNg'),'unselected NG excluded');
        console.log('ADVANCED_NG_BACKUP_PASS '+checks);
    } finally {if(backup!==null)backup.close();clear();cleanup(root);}
});
