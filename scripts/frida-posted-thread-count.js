// Isolated emulator fixture only. Synthetic bookmarks are removed in finally.
// Never posts to a board or changes postDataList / sent-history retention.
Java.perform(function() {
    const app=Java.use('android.app.ActivityThread').currentApplication();
    const pkg=app.getPackageName().toString(), version=String(app.getPackageManager().getPackageInfo(pkg,0).versionName.value);
    const legacy=version.indexOf('191')>=0||version.indexOf('226')>=0;
    if(!/^jp\.co\.airfront\.android\.a2chMate\.haiagaru\.(count(191|226|241)|imgur242)$/.test(pkg))
        throw new Error('Isolated count fixture required');
    const prefs=app.getSharedPreferences('io.github.areteruhiro.chmate.haiagaru.ui-config',0);
    const key='postedThreadDisplayCount', had=prefs.contains(key), previous=prefs.getInt(key,10);
    const SQLite=Java.use('android.database.sqlite.SQLiteDatabase');
    const db=SQLite.openDatabase.overload('java.lang.String','android.database.sqlite.SQLiteDatabase$CursorFactory','int')
        .call(SQLite,app.getDatabasePath('roidon.sqlite').getAbsolutePath(),null,0);
    const title='Haiagaru posted-count isolated fixture ', base=1900018400;
    const modern241=version==='0.8.10.241';
    const history=legacy?null:Java.use(modern241?'o.KP':'o.PackageSignatureVerifier').$new(Java.use('java.io.File').$new(app.getCacheDir(),'posted-count-unused-history.json'));
    const query=Java.use(legacy?(version.indexOf('191')>=0?'o.r8lambdaTb_p0z6z2AqSZIga1YhmAVmiTPk':'jp.syoboi.a2chMate.fragment.HomeFragment$RemoteActionCompatParcelizer'):(modern241?'o.rS':'o.queryPackageSignatureVerified'));
    try {
        for(let i=0;i<25;i++) {
            db.execSQL.overload('java.lang.String','[Ljava.lang.Object;').call(db,
                'INSERT INTO bookmarks(name,created,title,posted) VALUES(?,?,?,?)',
                Java.array('java.lang.Object',[Java.use('java.lang.String').$new('egg.5ch.io/android'),
                    Java.use('java.lang.Long').valueOf(base+i),Java.use('java.lang.String').$new(title+i),
                    Java.use('java.lang.Long').valueOf(Date.now()+i)]));
        }
        for(const setting of [10,20,0,3,1,10000]) {
            prefs.edit().putInt(key,setting).commit();
            console.log('Preference='+prefs.getInt(key,-1)+', getter='+Java.use('app.morphe.extension.chmate.Haiagaru').getPostedThreadDisplayCountLong());
            let rows;
            if(legacy) rows=(version.indexOf('191')>=0?query.a:query.e).overload('android.content.Context').call(query,app);
            else {
                const instance=query.$new();
                rows=(modern241?instance.b.overload('android.content.Context','o.KP')
                    :instance.d.overload('android.content.Context','o.PackageSignatureVerifier')).call(instance,app,history);
            }
            const expected=(setting===0?25:Math.min(setting,25))+1;
            if(rows.size()!==expected)throw new Error('Setting '+setting+': expected '+expected+' rows including header, got '+rows.size());
            console.log('PASS native '+version+' posted section setting='+setting+', displayed threads='+(rows.size()-1));
        }
    } finally {
        db.delete('bookmarks','title LIKE ? AND created >= ? AND created < ?',
            Java.array('java.lang.String',[title+'%',String(base),String(base+25)]));
        db.close();
        const editor=prefs.edit();if(had)editor.putInt(key,previous);else editor.remove(key);editor.commit();
        console.log('Synthetic bookmarks and original display setting restored');
    }
});
