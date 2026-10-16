/* Open the fixture's editor dialog first, in the isolated boardedit191 package. */
Java.perform(function(){
 const app=Java.use('android.app.ActivityThread').currentApplication();
 if(String(app.getPackageName())!=='jp.co.airfront.android.a2chMate.haiagaru.boardedit191')throw Error('Verification only');
 let selected;
 Java.choose('android.app.AlertDialog',{onMatch(d){if(d.isShowing()&&String(d.getContext().getPackageName())===String(app.getPackageName()))selected=Java.retain(d);},onComplete(){
  if(!selected)throw Error('Open external board edit dialog first');
  Java.scheduleOnMainThread(function(){
   const edits=[];let save;
   function walk(v){
    const name=String(v.getClass().getName());
    if(name==='android.widget.EditText')edits.push(Java.cast(v,Java.use('android.widget.EditText')));
    if(name==='android.widget.Button'&&String(Java.cast(v,Java.use('android.widget.Button')).getText())==='保存')save=Java.cast(v,Java.use('android.widget.Button'));
    if(Java.use('android.view.ViewGroup').class.isInstance(v)){
     const g=Java.cast(v,Java.use('android.view.ViewGroup'));for(let i=0;i<g.getChildCount();i++)walk(g.getChildAt(i));
    }
   }
   walk(selected.getWindow().getDecorView());if(edits.length!==2||!save)throw Error('Unexpected edit dialog');
   function text(v,s){Java.use('android.widget.TextView').setText.overload('java.lang.CharSequence').call(v,Java.use('java.lang.String').$new(s));}
   text(edits[1],'file:///invalid');save.performClick();
   if(edits[1].getError()===null)throw Error('Invalid URL accepted');console.log('PASS native save button rejects invalid URL');
   text(edits[0],'Renamed verification board');
   text(edits[1],'https://haiagaru-destination.invalid/newboard/');save.performClick();
   let attempts=0;
   function verify(){Java.perform(function(){
    const DB=Java.use('android.database.sqlite.SQLiteDatabase');
    const db=DB.openDatabase.overload('java.lang.String','android.database.sqlite.SQLiteDatabase$CursorFactory','int').call(DB,String(app.getDatabasePath('roidon.sqlite').getPath()),null,1);
    const c=db.rawQuery("SELECT title FROM boards WHERE name='haiagaru-original.invalid%2Ftestboard'",null);
    const saved=c.moveToFirst()&&String(c.getString(0))==='Renamed verification board';c.close();
    if(!saved){db.close();if(++attempts>20)throw Error('Title not saved');setTimeout(verify,500);return;}
    const b=db.rawQuery("SELECT count(*) FROM bookmarks WHERE name='haiagaru-original.invalid%2Ftestboard' AND created=1234567890",null);b.moveToFirst();
    if(b.getInt(0)!==1)throw Error('Bookmark identity lost');b.close();db.close();
    const prefs=app.getSharedPreferences('haiagaru.external-board-addresses',0);
    if(String(prefs.getString('target:haiagaru-original.invalid%2Ftestboard',''))!=='https://haiagaru-destination.invalid/newboard/')throw Error('Address not saved');
    console.log('PASS actual dialog saved title/address and preserved native bookmark identity');
   });}setTimeout(verify,500);
  });
 }});
});
