// Emulator-only, already-open app. Toggles twice and restores the user's initial mode. No HTTP or clipboard reads.
Java.perform(function () {
    const app=Java.use('android.app.ActivityThread').currentApplication();
    if(String(app.getPackageName())!=='jp.co.airfront.android.a2chMate.haiagaru.uploadhosts191')throw Error('Test 191 package required');
    const H=Java.use('app.morphe.extension.chmate.Haiagaru');
    const initial=!!H.directBodySelectionEnabled();
    let activity=null, checks=0;
    function check(ok,label){if(!ok)throw Error(label);checks++;console.log('PASS '+label);}
    function bodies(root,expected){
        const Group=Java.use('android.view.ViewGroup'), Text=Java.use('android.widget.TextView');
        let n=0;
        function walk(v){
            if(String(v.getClass().getName())==='o.r8lambdaDC40CispcBM3LVwPP01tT_FjLc'){
                const text=Java.cast(v,Text);n++;
                check(!!text.isTextSelectable()===expected,'visible body selectable matches toggle');
                if(expected)check(text.getCustomSelectionActionModeCallback()===null,'standard selection callback active');
            }
            if(Group.class.isInstance(v)){const g=Java.cast(v,Group);for(let i=0;i<g.getChildCount();i++)walk(g.getChildAt(i));}
        }
        walk(root);check(n>0,'response bodies present');
    }
    Java.scheduleOnMainThread(function(){
        const values=Java.use('android.app.ActivityThread').currentActivityThread().mActivities.value.values().toArray();
        for(let i=0;i<values.length;i++){
            const record=Java.cast(values[i],Java.use('android.app.ActivityThread$ActivityClientRecord'));
            if(!record.paused.value)activity=Java.retain(record.activity.value);
        }
        check(activity!==null,'foreground activity found');
        const res=app.getResources(), name=app.getPackageName();
        const title=res.getIdentifier('haiagaru_direct_copy','string',name);
        check(title!==0&&['長押しコピー','Long-press copy'].indexOf(String(res.getString(title)))>=0,'toolbar resource available');
        check(res.getIdentifier('haiagaru_direct_copy','drawable',name)!==0,'copy icon available');
        check(!!H.handleEdgeArchiveToolbarClick(activity,0x7e000004),'toolbar action handled');
        check(!!H.directBodySelectionEnabled()!==initial,'toolbar changed shared preference');
        setTimeout(function(){Java.scheduleOnMainThread(function(){
            try{bodies(activity.getWindow().getDecorView(),!initial);}
            finally{H.handleEdgeArchiveToolbarClick(activity,0x7e000004);}
            setTimeout(function(){Java.scheduleOnMainThread(function(){
                check(!!H.directBodySelectionEnabled()===initial,'initial mode restored');
                bodies(activity.getWindow().getDecorView(),initial);
                console.log('PASS '+checks+' Android toolbar/state checks; original mode restored');
            });},500);
        });},500);
    });
});
