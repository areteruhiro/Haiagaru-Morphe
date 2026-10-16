/* After actual UI save + process restart; do not initialize/reset routes in this script. */
Java.perform(function(){
 const app=Java.use('android.app.ActivityThread').currentApplication();
 if(String(app.getPackageName())!=='jp.co.airfront.android.a2chMate.haiagaru.boardedit191')throw Error('Verification only');
 const E=Java.use('app.morphe.extension.chmate.ExternalBoardEditor');
 const B=Java.use('jp.syoboi.a2chMate.client.BBSUrlInfo$BoardID');
 const id='haiagaru-original.invalid%2Ftestboard';
 const b=B.$new.overload('java.lang.String','java.lang.String','java.lang.String','java.lang.String').call(B,'haiagaru-original.invalid','testboard',id,'http://haiagaru-original.invalid/testboard/');
 const f=b.getClass().getDeclaredField(String(E.boardFields()).split(',')[0]);f.setAccessible(true);
 if(String(f.get(b))!=='haiagaru-destination.invalid'||String(b.toString())!==id)throw Error('Saved route not loaded at application startup');
 const DB=Java.use('android.database.sqlite.SQLiteDatabase');
 const db=DB.openDatabase.overload('java.lang.String','android.database.sqlite.SQLiteDatabase$CursorFactory','int').call(DB,String(app.getDatabasePath('roidon.sqlite').getPath()),null,1);
 const c=db.rawQuery("SELECT title FROM boards WHERE name='haiagaru-original.invalid%2Ftestboard'",null);
 if(!c.moveToFirst()||String(c.getString(0))!=='Renamed verification board')throw Error('Saved title lost');c.close();
 const bm=db.rawQuery("SELECT count(*) FROM bookmarks WHERE name='haiagaru-original.invalid%2Ftestboard' AND created=1234567890",null);bm.moveToFirst();
 if(bm.getInt(0)!==1)throw Error('Bookmark lost');bm.close();db.close();
 console.log('PASS actual UI title/address save persists across restart, native route loaded, bookmark identity retained');
});
