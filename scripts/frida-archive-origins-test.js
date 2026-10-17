// Synthetic records only in the dedicated archive verification application.
setImmediate(function(){ Java.perform(function(){
    const app=Java.use('android.app.ActivityThread').currentApplication();
    const pkg=String(app.getPackageName());
    if(!/\.haiagaru\.responsearchive(191|226|242)$/.test(pkg))throw Error('Dedicated archive verification app only');
    const O=Java.use('app.morphe.extension.chmate.ArchiveOrigins');
    const P=Java.use('app.morphe.extension.chmate.ArchiveOriginPaths');
    const F=Java.use('java.io.File'), Out=Java.use('java.io.FileOutputStream'), In=Java.use('java.io.FileInputStream');
    const J=Java.use('java.lang.String'), Long=Java.use('java.lang.Long');
    const B=Java.use('jp.syoboi.a2chMate.client.BBSUrlInfo');
    const A=Java.use('app.morphe.extension.chmate.HaiagaruBackupArchive');
    const Zip=Java.use('java.util.zip.ZipFile');
    const suffix=String(Date.now()),key='1792102288'; let checks=0;
    function check(ok,label){if(!ok)throw Error(label);checks++;console.log('PASS '+label);}
    function write(f,text){f.getParentFile().mkdirs();const out=Out.$new(f);try{out.write(J.$new(text).getBytes('UTF-8'));}finally{out.close();}}
    const model=B.$new('mi.5ch.io','news4vip',Long.valueOf(key),0,null,null);
    const nativeUrl=String(O.nativeUrlMethod());
    const url=String(model[nativeUrl]());
    check(/^https?:\/\/mi\.5ch.io\/test\/read.cgi\/news4vip\/1792102288\/$/.test(url),'native live thread URL unchanged');
    const map=Java.cast(O.LIVE.value,Java.use('java.util.Map'));
    const entries=map.entrySet().iterator();let name=null;
    while(entries.hasNext()){const entry=Java.cast(entries.next(),Java.use('java.util.Map$Entry'));if(String(entry.getValue())===url)name=String(entry.getKey());}
    check(name!==null,'constructor/URL observes native board and thread identity');
    const archiveRoot=O.archiveRoot(app),dir=F.$new(archiveRoot,'haiagaru-origin-fixture-'+suffix);
    dir.mkdirs();const dest=F.$new(dir,name),sourceDir=F.$new(app.getCacheDir(),'archive-origin-source-'+suffix),source=F.$new(sourceDir,name);
    const relative=String(P.relative(archiveRoot,dest));
    const metadata=F.$new(O.directory(app),String(P.hash(relative)));
    if(metadata.exists())throw Error('Refuse fixture collision');
    if(source.exists()||dest.exists())throw Error('Refuse existing DAT fixture path');
    write(source,'name<>mail<>date<>body<>fixture');write(dest,'name<>mail<>date<>body<>fixture');
    O.copied(source,dest); O.flush();
    check(String(O.read(app,relative).getString('url'))===url,'DAT archive stores exact original URL');
    check(String(O.read(app,relative).getString('kind'))==='captured','captured provenance retained');
    const fileModel=B.$new('mi.5ch.io','news4vip',Long.valueOf(key),0,null,String(dest.getPath()));
    check(String(fileModel[nativeUrl]()).startsWith('file:'),'native archive navigation remains file URL');
    const cache=F.$new(app.getCacheDir(),'archive-origin-backup-'+suffix).getCanonicalFile(); cache.mkdirs();
    const fakeFiles=Java.retain(F.$new(cache,'files')),fakeExternal=Java.retain(F.$new(cache,'external')),fakeCache=Java.retain(F.$new(cache,'cache'));
    fakeFiles.mkdirs();fakeExternal.mkdirs();fakeCache.mkdirs();
    const C=Java.registerClass({name:'app.haiagaru.test.OriginBackupContext'+suffix,superClass:Java.use('android.content.ContextWrapper'),methods:{
        $init:[{returnType:'void',argumentTypes:['android.content.Context'],implementation:function(base){this.$super.$init(base);}}],
        getFilesDir:function(){return F.$new(String(fakeFiles.getPath()));},getCacheDir:function(){return F.$new(String(fakeCache.getPath()));},
        getExternalFilesDir:[{returnType:'java.io.File',argumentTypes:['java.lang.String'],implementation:function(type){return F.$new(String(fakeExternal.getPath()));}}],
        getFileStreamPath:function(name){return F.$new(fakeFiles,name);},
        getSharedPreferences:function(name,mode){return app.getSharedPreferences('origin-fixture.'+suffix+'.'+name,mode);}
    }});
    const context=C.$new(app),fakeArchive=O.archiveRoot(context),fakeDat=F.$new(fakeArchive,relative);
    write(fakeDat,'synthetic DAT');O.write(context,relative,url,'captured');
    const fakeMeta=F.$new(O.directory(context),String(P.hash(relative)));
    let backup=null;
    function removeFixture(file){if(file.isDirectory()){const children=file.listFiles();if(children)for(let i=0;i<children.length;i++)removeFixture(children[i]);}file.delete();}
    try {
        backup=A.read(context,A.capture(context,128));
        const zip=Zip.$new(backup.file.value);
        try {
            check(zip.getEntry('archive/'+relative)!==null,'thread DAT in backup');
            check(zip.getEntry('archive-origins/'+P.hash(relative))!==null,'original URL metadata in same archive backup category');
        }finally{zip.close();}
        fakeDat.delete();fakeMeta.delete();
        A.restore(context,backup,128,false);
        check(fakeDat.isFile(),'thread archive restored');
        check(String(O.read(context,relative).getString('url'))===url,'exact original URL restored with archive');
        O.write(context,relative,'https://egg.5ch.io/test/read.cgi/android/'+key+'/','manual');
        check(String(O.read(context,relative).getString('kind'))==='manual','manual origin registered');
        check(O.read(context,'news4vip/title(1)['+key+']/news4vip_'+key+'.dat')===null,'legacy missing host not guessed');
        check(String(P.hash(relative))===String(P.hash(relative)),'identity independent of package/storage root');
    } finally {
        if(backup!==null)backup.close();removeFixture(cache);dest.delete();dir.delete();metadata.delete();source.delete();sourceDir.delete();
    }
    Java.scheduleOnMainThread(function(){Java.choose('android.app.Activity',{onMatch:function(a){if(a.isFinishing()||!a.hasWindowFocus())return;O.show(a);console.log('PASS original URL browser opened');return 'stop';},onComplete:function(){}});});
    console.log('PASS '+checks+' original URL and backup/restore checks (no MEGA network)');
});});
