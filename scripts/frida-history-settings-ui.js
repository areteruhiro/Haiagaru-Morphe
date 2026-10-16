// Isolated 241 fixture: exercise the actual settings save listener without restarting.
Java.perform(function() {
    const app=Java.use('android.app.ActivityThread').currentApplication();
    if(String(app.getPackageName())!=='jp.co.airfront.android.a2chMate.haiagaru.count241')throw Error('Isolated 241 required');
    const Haiagaru=Java.use('app.morphe.extension.chmate.Haiagaru');
    const prefs=app.getSharedPreferences('io.github.areteruhiro.chmate.haiagaru.ui-config',0);
    const nativePrefs=Java.use('android.preference.PreferenceManager').getDefaultSharedPreferences(app);
    const key='postedThreadDisplayCount', nativeKey='postDataListCount';
    const had=prefs.contains(key), old=prefs.getInt(key,10);
    const nativeHad=nativePrefs.contains(nativeKey), nativeOld=nativePrefs.getInt(nativeKey,100);
    const extra = [
        [prefs,'unlimitedThreadHistory','bool'],[prefs,'recentThreadDisplayCount','int'],
        [prefs,'searchHistoryCount','int'],[nativePrefs,'autoDelete','int']
    ].map(([p,k,t])=>[p,k,t,p.contains(k),t==='bool'?p.getBoolean(k,false):p.getInt(k,0)]);
    let restarts=0, dialog=null;
    Haiagaru.restart.implementation=function(activity){restarts++;};
    const Alert=Java.use('android.app.AlertDialog');
    const Builder=Java.use('android.app.AlertDialog$Builder');
    Builder.show.implementation=function(){const result=this.show();dialog=Java.retain(result);return result;};
    const records=Java.use('android.app.ActivityThread').currentActivityThread().mActivities.value.values().iterator();
    let activity=null;
    while(records.hasNext()){
        const record=records.next(),field=record.getClass().getDeclaredField('activity');field.setAccessible(true);
        const candidate=Java.cast(field.get(record),Java.use('android.app.Activity'));
        if(!candidate.isFinishing()&&!candidate.isDestroyed())activity=Java.retain(candidate);
    }
    if(activity===null)throw Error('No Activity');
    function visit(view, output){
        if(Java.use('android.widget.TextView').class.isInstance(view))output.push(Java.cast(view,Java.use('android.widget.TextView')));
        if(Java.use('android.view.ViewGroup').class.isInstance(view)){
            const group=Java.cast(view,Java.use('android.view.ViewGroup'));
            for(let i=0;i<group.getChildCount();i++)visit(group.getChildAt(i),output);
        }
    }
    function cleanup(){
        if(dialog)dialog.dismiss();
        const e=prefs.edit();if(had)e.putInt(key,old);else e.remove(key);e.commit();
        const n=nativePrefs.edit();if(nativeHad)n.putInt(nativeKey,nativeOld);else n.remove(nativeKey);n.commit();
        for(const [p,k,t,had,value] of extra){const e=p.edit();if(!had)e.remove(k);else if(t==='bool')e.putBoolean(k,value);else e.putInt(k,value);e.commit();}
        Haiagaru.restart.implementation=null;Builder.show.implementation=null;
        console.log('Settings fixture preferences restored');
    }
    function run(index){Java.scheduleOnMainThread(function(){
        if(index===2){
            if(restarts!==2){cleanup();throw Error('Expected two refresh restarts, got '+restarts);}
            console.log('PASS settings changes request restart');cleanup();return;
        }
        const unlimited=index===0;dialog=null;
                Haiagaru.showSettingsDialog(activity);
        setTimeout(function(){Java.choose('android.app.AlertDialog',{
            onMatch:function(found){if(found.isShowing())dialog=Java.retain(found);},
            onComplete:function(){Java.scheduleOnMainThread(function(){try{
                if(!dialog)throw Error('No settings dialog');
                const views=[];visit(dialog.getWindow().getDecorView(),views);
                const toggle=views.find(v=>String(v.getText())==='書き込み履歴を無制限に保存');
                if(!toggle)throw Error('Unlimited switch missing');
                const sw=Java.cast(toggle,Java.use('android.widget.Switch'));sw.setChecked(unlimited);
                const edits=views.filter(v=>Java.use('android.widget.EditText').class.isInstance(v));
                // Locate the numeric input by its preceding label in the same settings layout.
                const label=views.find(v=>String(v.getText()).startsWith('書き込み履歴に残す件数'));
                const parent=Java.cast(label.getParent(),Java.use('android.view.ViewGroup'));
                let input=null;
                for(let i=parent.indexOfChild(label)+1;i<parent.getChildCount();i++){
                    if(Java.use('android.widget.EditText').class.isInstance(parent.getChildAt(i))){input=Java.cast(parent.getChildAt(i),Java.use('android.widget.EditText'));break;}
                }
                if(!input||input.isEnabled()===unlimited)throw Error('Unlimited input state incorrect');
                if(!unlimited)input.setText.overload('java.lang.CharSequence','android.widget.TextView$BufferType')
                    .call(input,Java.use('java.lang.String').$new('20'),Java.use('android.widget.TextView$BufferType').NORMAL.value);
                const browse=views.find(v=>String(v.getText())==='閲覧スレ履歴を無制限に保存（自動削除しない）');
                Java.cast(browse,Java.use('android.widget.Switch')).setChecked(unlimited);
                for(const [text,value] of [['閲覧スレ履歴の保存数',20],['最近見たスレの表示数',unlimited?0:20],['検索履歴の保存数',unlimited?0:20]]){
                    const title=views.find(v=>String(v.getText()).startsWith(text));
                    const group=Java.cast(title.getParent(),Java.use('android.view.ViewGroup'));
                    const edit=Java.cast(group.getChildAt(group.indexOfChild(title)+1),Java.use('android.widget.EditText'));
                    edit.setText.overload('java.lang.CharSequence','android.widget.TextView$BufferType').call(edit,Java.use('java.lang.String').$new(String(value)),Java.use('android.widget.TextView$BufferType').NORMAL.value);
                }
                dialog.getButton(-1).performClick();
                setTimeout(function(){Java.scheduleOnMainThread(function(){try{
                const expected=unlimited?2147483647:20;
                console.log('Saved post='+nativePrefs.getInt(nativeKey,-1)+', expected='+expected+', browse='+prefs.getBoolean('unlimitedThreadHistory',false)+', searches='+prefs.getInt('searchHistoryCount',-1));
                if(nativePrefs.getInt(nativeKey,-1)!==expected||Haiagaru.getPostHistoryRetentionCount()!==expected)
                    throw Error('Actual settings save failed');
                console.log('PASS settings UI unlimited='+unlimited+', saved='+expected);
                if(prefs.getBoolean('unlimitedThreadHistory',!unlimited)!==unlimited
                    ||prefs.getInt('recentThreadDisplayCount',-1)!==(unlimited?0:20)
                    ||prefs.getInt('searchHistoryCount',-1)!==(unlimited?0:20)
                    ||(!unlimited&&nativePrefs.getInt('autoDelete',-1)!==20))throw Error('Browsing/search settings save failed');
                console.log('PASS browsing/search settings UI unlimited='+unlimited+', finite=20');
                dialog.dismiss();run(index+1);
                }catch(error){cleanup();throw error;}});},250);
        }catch(error){cleanup();throw error;}});}});},250);
    });}
    nativePrefs.edit().putInt(nativeKey,100).commit();
    run(0);
});
