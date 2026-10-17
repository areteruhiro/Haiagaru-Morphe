// Detached draft only: no posting, network requests or user draft edits.
Java.perform(function () {
    const app = Java.use('android.app.ActivityThread').currentApplication();
    if (!/\.haiagaru\.responsearchive(191|226|242)$/.test(String(app.getPackageName()))) throw Error('Verification app only');
    const loader = Java.use('dalvik.system.DexClassLoader').$new('/data/local/tmp/haiagaru-command-test.dex',
        String(app.getCodeCacheDir().getPath()), null, Java.use('java.lang.ClassLoader').getSystemClassLoader().getParent());
    const Commands = Java.ClassFactory.get(loader).use('app.morphe.extension.chmate.ThreadCommands');
    const Dialog = Java.use('android.app.AlertDialog'), Edit = Java.use('android.widget.EditText');
    const Group = Java.use('android.view.ViewGroup');
    const prefs = app.getSharedPreferences('haiagaru_thread_commands',0);
    const keys = ['5ch.0','custom.0'];
    const previous = keys.map(k => prefs.contains(k) ? String(prefs.getString(k,null)) : null);
    let current = null, checks = 0;
    function cleanup() {
        if(current)current.dismiss();
        const editor=prefs.edit();keys.forEach((k,i)=>previous[i]===null?editor.remove(k):editor.putString(k,previous[i]));editor.commit();
    }
    function later(work) {
        setTimeout(function(){Java.perform(function(){Java.scheduleOnMainThread(function(){
            try {work();}catch(error){console.log('FAIL '+error);cleanup();}
        });});},1500);
    }
    function text(edit) { return Java.cast(edit.getText(),Java.use('java.lang.Object')).toString(); }
    function check(ok, label) { if (!ok) throw Error(label); checks++; }
    function findEdit(view) {
        if(Edit.class.isInstance(view)) return Java.cast(view,Edit);
        if(Group.class.isInstance(view)) {
            const group=Java.cast(view,Group);
            for(let n=0;n<group.getChildCount();n++){const edit=findEdit(group.getChildAt(n));if(edit)return edit;}
        }
        return null;
    }
    Java.choose('android.app.Activity', {onMatch:function(activity){
        if(activity.isFinishing() || !activity.getWindow().getDecorView().isShown())return;
        const retained=Java.retain(activity);
        Java.scheduleOnMainThread(function(){
            try {
                prefs.edit().putString('5ch.0','!extend:checked:vvvvv:1000:512').remove('custom.0').commit();
                const body=Java.retain(Edit.$new(retained));body.setText(Java.use('java.lang.String').$new('untouched draft'),Java.use('android.widget.TextView$BufferType').EDITABLE.value);
                current=Java.retain(Commands.edit(body,'5ch',0));
                later(function(){
                let input=findEdit(current.getWindow().getDecorView());
                check(text(input).split('\n').length===3,'legacy default migrated to three lines: '+text(input));
                check((input.getInputType() & 0x20000)!==0,'multiline keyboard enabled');
                check((input.getImeOptions() & 0x40000000)!==0,'Enter remains newline');
                current.getButton(-1).performClick();
                check(text(body).split('\n').length===4,'three lines inserted before draft: '+JSON.stringify(text(body))+' error='+String(input.getError()));
                current=Java.retain(Commands.edit(body,'5ch',0));
                later(function(){
                current.getButton(-1).performClick();
                check(text(body).split('\n').length===7,'same command inserted again');
                check(text(body).endsWith('untouched draft'),'original draft preserved');
                current=Java.retain(Commands.edit(body,'custom',0));
                later(function(){
                input=findEdit(current.getWindow().getDecorView());
                input.setText(Java.use('java.lang.String').$new('!custom:a\n!custom:b'),Java.use('android.widget.TextView$BufferType').EDITABLE.value);
                current.getButton(-1).performClick();
                check(text(body).startsWith('!custom:a\n!custom:b\n'),'custom multiline inserted');
                current=Java.retain(Commands.edit(body,'custom',0));input=findEdit(current.getWindow().getDecorView());
                check(text(input)==='!custom:a\n!custom:b','multiline preference restored');
                current.dismiss();
                console.log('PASS '+checks+' Android command-dialog checks; no post sent');
                cleanup();
                });
                });
                });
            } catch(error){console.log('FAIL '+error);cleanup();throw error;}
        });
        return 'stop';
    },onComplete:function(){}});
});
