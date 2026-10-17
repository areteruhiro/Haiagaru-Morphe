// Detached editors in the isolated emulator app only; never posts or changes a real draft.
Java.perform(function () {
    const app = Java.use('android.app.ActivityThread').currentApplication();
    if (String(app.getPackageName()) !== 'jp.co.airfront.android.a2chMate.haiagaru.archivemenu191') throw Error('Verification app required');
    const C = Java.use('app.morphe.extension.chmate.ThreadCommands');
    const E = Java.use('android.widget.EditText'), G = Java.use('android.view.ViewGroup');
    const S = Java.use('java.lang.String'), B = Java.use('android.widget.TextView$BufferType').EDITABLE.value;
    const prefs = app.getSharedPreferences('haiagaru_thread_commands', 0);
    const keys = ['board.egg.5ch.io/android/.13', 'board.egg.5ch.io/android/.2', 'board.egg.5ch.io/be/.2', 'board.hayabusa.open2ch.net/livejupiter/.2'];
    const old = keys.map(k => prefs.contains(k) ? String(prefs.getString(k, null)) : null);
    let dialog = null, checks = 0;
    function check(ok, name) { if (!ok) throw Error(name); checks++; }
    function text(v) { return String(Java.cast(v.getText(), Java.use('java.lang.Object')).toString()); }
    function input(v) {
        if (E.class.isInstance(v)) return Java.cast(v, E);
        if (G.class.isInstance(v)) { const g = Java.cast(v, G); for (let i=0;i<g.getChildCount();i++) { const e=input(g.getChildAt(i)); if(e)return e; } }
        return null;
    }
    let found = false;
    Java.choose('android.app.Activity', {onMatch(a) {
        if(found || a.isFinishing() || !a.getWindow().getDecorView().isShown())return;
        found=true; const activity=Java.retain(a);
        function later(work) { return new Promise((resolve,reject)=>setTimeout(()=>Java.perform(()=>Java.scheduleOnMainThread(()=>{try{work();resolve();}catch(e){reject(e);}})),400)); }
        (async function () {
            let body, name, edit;
            try {
              await later(function () {
                const clear=prefs.edit(); keys.forEach(k=>clear.remove(k));clear.commit();
                body=Java.retain(E.$new(activity)); name=Java.retain(E.$new(activity));
                body.setText(S.$new('unchanged draft'),B);name.setText(S.$new('tester'),B);
                edit=C.edit.overload('android.widget.EditText','android.widget.EditText','java.lang.String','java.lang.String','int');
                dialog=Java.retain(edit.call(C,body,name,'5ch','https://egg.5ch.io/android/',13));
              });
              await later(function () {
                check(text(input(dialog.getWindow().getDecorView()))==='!donguri','name initial');
                dialog.getButton(-1).performClick();
                check(text(name)==='tester !donguri','name destination: '+JSON.stringify(text(name))+'; error='+String(input(dialog.getWindow().getDecorView()).getError()));check(text(body)==='unchanged draft','body untouched');
                dialog=Java.retain(edit.call(C,body,name,'5ch','https://egg.5ch.io/android/',2));
              });
              await later(function () {
                input(dialog.getWindow().getDecorView()).setText(S.$new('!custom:a\n!custom:b'),B);dialog.getButton(-1).performClick();
                check(text(body)==='!custom:a\n!custom:b\nunchanged draft','multiline insertion');
                check(String(prefs.getString(keys[1],''))==='!custom:a\n!custom:b','board save');
                dialog=Java.retain(edit.call(C,body,name,'5ch','https://egg.5ch.io/be/',2));
                check(text(input(dialog.getWindow().getDecorView()))==='','different board isolated');dialog.dismiss();
                dialog=Java.retain(edit.call(C,body,name,'open2ch','https://hayabusa.open2ch.net/livejupiter/',2));
              });
              await later(function () {
                dialog.getButton(-1).performClick();check(dialog.isShowing(),'placeholder blocked');
                input(dialog.getWindow().getDecorView()).setText(S.$new('!aku31'),B);dialog.getButton(-1).performClick();
                check(text(body).startsWith('!aku31\n'),'aku insertion');
                console.log('PASS '+checks+' installed Android command-dialog checks; no post sent');
              });
            } catch(e) {console.log('FAIL '+e.stack);}
            finally {
              await later(function () {
                if(dialog && dialog.isShowing())dialog.dismiss();
                const restore=prefs.edit();keys.forEach((k,i)=>old[i]===null?restore.remove(k):restore.putString(k,old[i]));restore.commit();
              });
            }
        })();
    },onComplete(){if(!found)console.log('FAIL no visible Activity');}});
});
