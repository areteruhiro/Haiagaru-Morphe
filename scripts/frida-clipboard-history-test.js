// Only a dedicated verification app. Never touches another app's clipboard history.
setImmediate(function () { Java.perform(function () {
    const app = Java.use('android.app.ActivityThread').currentApplication();
    if (!String(app.getPackageName()).match(/\.haiagaru\.responsearchive(191|226|242)$/)) throw Error('Verification app only');
    const H = Java.use('app.morphe.extension.chmate.ClipboardHistory');
    const DB = Java.use('android.database.sqlite.SQLiteDatabase');
    const db = Java.cast(H.store(app).getWritableDatabase(), DB);
    function count(sql) { const c=db.rawQuery(sql,null); try { c.moveToFirst(); return c.getInt(0); } finally { c.close(); } }
    function scalar(sql) { const c=db.rawQuery(sql,null); try { return c.moveToFirst() ? String(c.getString(0)) : null; } finally { c.close(); } }
    if (count("SELECT count(*) FROM clips WHERE body NOT IN ('copy A','copy B','captured text') AND body NOT GLOB 'limit [0-9]*'") !== 0
            || count("SELECT count(*) FROM categories WHERE name NOT IN ('分類A','分類B')") !== 0) throw Error('Non-test records: refusing destructive test');
    // Remove only synthetic leftovers from an interrupted run in the isolated verification app.
    db.delete('clips',null,null); db.delete('categories',null,null);
    let checks=0;
    function check(ok,label) { if(!ok)throw Error(label); checks++; console.log('PASS '+label); }
    const prefs=app.getSharedPreferences('haiagaru-clipboard',0), was=prefs.getBoolean('enabled',false);
    const Clip = Java.use('android.content.ClipData');
    const JString = Java.use('java.lang.String');
    function clip(text) { return Clip.newPlainText(JString.$new('test'), JString.$new(text)); }
    const Manager = Java.use('android.content.ClipboardManager');
    const manager=Java.cast(app.getSystemService('clipboard'),Manager);
    // Intercept only the system write; no real user clipboard is replaced during this test.
    const copy=Manager.setPrimaryClip.overload('android.content.ClipData'); let systemCopies=0;
    copy.implementation=function(c){systemCopies++;};
    prefs.edit().putBoolean('enabled',false).commit();
    H.setPrimaryClip(manager,clip('not-recorded'));
    check(count('SELECT count(*) FROM clips')===0,'off does not record');
    H.save(app,'copy A'); H.save(app,'copy B');
    check(count('SELECT count(*) FROM clips')===2,'stores separate text');
    H.changeCategoryNow(app,null,'分類A');
    db.execSQL("UPDATE clips SET category='分類A' WHERE body='copy A'");
    H.save(app,'copy A');
    check(count('SELECT count(*) FROM clips')===2,'duplicate copy deduplicated');
    check(scalar('SELECT body FROM clips ORDER BY copied DESC,id DESC LIMIT 1')==='copy A','recopy moves to top even in same millisecond');
    check(scalar("SELECT category FROM clips WHERE body='copy A'")==='分類A','duplicate preserves classification');
    H.changeCategoryNow(app,'分類A','分類B');
    check(scalar("SELECT category FROM clips WHERE body='copy A'")==='分類B','rename moves records');
    check(count("SELECT count(*) FROM categories WHERE name='分類A'")===0,'old classification removed');
    H.changeCategoryNow(app,'分類B',null);
    check(scalar("SELECT category FROM clips WHERE body='copy A'")==='','category removal keeps text unclassified');
    db.beginTransaction();
    try { for(let n=0;n<103;n++)H.save(app,'limit '+n); db.setTransactionSuccessful(); }
    finally { db.endTransaction(); }
    check(count('SELECT count(*) FROM clips')===100,'100 record limit');
    check(count("SELECT count(*) FROM clips WHERE body='limit 0'")===0,'oldest record pruned');
    db.delete('clips',null,null);
    prefs.edit().putBoolean('enabled',true).commit();
    H.setPrimaryClip(manager,clip('captured text'));
    H.setPrimaryClip(manager,clip(' '));
    H.setPrimaryClip(manager,clip('x'.repeat(10001)));
    const sensitive=clip('sensitive');
    const bundle=Java.use('android.os.PersistableBundle').$new(); bundle.putBoolean('android.content.extra.IS_SENSITIVE',true); sensitive.getDescription().setExtras(bundle);
    H.setPrimaryClip(manager,sensitive);
    H.setPrimaryClip(manager,Clip.newRawUri(JString.$new('test'),Java.use('android.net.Uri').parse('https://example.invalid/')));
    setTimeout(function(){Java.perform(function(){
        try {
            check(count('SELECT count(*) FROM clips')===1,'only eligible text recorded asynchronously');
            check(scalar('SELECT body FROM clips')==='captured text','exact text retained');
            check(systemCopies===6,'all original copy calls preserved');
            check(count("SELECT count(*) FROM clips WHERE instr(body,'text')>0")===1,'literal substring search');
            Java.scheduleOnMainThread(function(){
                Java.choose('android.app.Activity', {onMatch:function(a){
                    if(a.isFinishing() || !a.hasWindowFocus()) return;
                    const Menu=Java.use('android.widget.PopupMenu').$new(a,a.getWindow().getDecorView()).getMenu();
                    H.addPostingMenu(a,Menu); H.addPostingMenu(a,Menu);
                    check(Menu.size()===1,'posting entry deduplicates');
                    check(String(Menu.getItem(0).getTitle())==='コピー履歴・分類','posting entry label');
                    check(H.handlePostingMenu(a,Menu.getItem(0)),'posting entry opens history');
                    return 'stop';
                },onComplete:function(){}});
            });
        } finally {
            db.delete('clips',null,null); db.delete('categories',null,null);
            prefs.edit().putBoolean('enabled',was).commit(); copy.implementation=null;
        }
        console.log('PASS '+checks+' clipboard model/privacy checks; pending UI callback');
    });},1500);
}); });
