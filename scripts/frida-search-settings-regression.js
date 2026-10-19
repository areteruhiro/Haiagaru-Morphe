// Opens only internal test screens and about:blank; restores settings and callbacks.
Java.perform(function () {
    const app=Java.use('android.app.ActivityThread').currentApplication(), pkg=String(app.getPackageName());
    if(!/^jp\.co\.airfront\.android\.a2chMate\.haiagaru\.(full191|resumefix226|resumefix242)$/.test(pkg))throw Error('Isolated regression package required');
    const name='app.morphe.extension.chmate.HissiWacchoiSearchActivity';
    Java.scheduleOnMainThread(function(){const intent=Java.use('android.content.Intent').$new();intent.setClassName(pkg,name);intent.addFlags(0x10000000);app.startActivity(intent);});
    setTimeout(function(){Java.perform(function(){let found=false;Java.choose(name,{onMatch:function(a){
        if(found||a.isFinishing())return;found=true;const activity=Java.retain(a);
        Java.scheduleOnMainThread(function(){
            let checks=0;function check(ok,label){if(!ok)throw Error(label);checks++;console.log('PASS '+label);}
            const prefs=app.getSharedPreferences('io.github.areteruhiro.chmate.haiagaru.ui-config',0),key='videoFrameThumbnailsEnabled';
            const had=prefs.contains(key),old=prefs.getBoolean(key,true),original=activity.composeChanged.value;
            const Counter=Java.registerClass({name:'app.morphe.test.SearchCounter'+Date.now(),implements:[Java.use('java.lang.Runnable')],
                fields:{delegate:'java.lang.Runnable',calls:'int'},methods:{run:function(){this.calls.value=this.calls.value+1;const delegate=this.delegate.value;if(delegate!==null)delegate.run();}}});
            const counter=Counter.$new();counter.delegate.value=original;
            try {
                check(original!==null,'search screen has a Compose update callback');
                activity.setComposeChanged(counter);activity.openZzzSearch('about:blank');
                check(activity.composeChanged.value!==null,'ZZZSearch keeps the update callback');
                check(activity.zzzDialog.value!==null,'local ZZZSearch viewer opens');
                activity.zzzDialog.value.dismiss();const before=counter.calls.value;activity.searchUiChanged();
                check(counter.calls.value>before,'search updates continue after closing ZZZSearch');
                const Compound=Java.use('android.widget.CompoundButton'),Group=Java.use('android.view.ViewGroup'),Edit=Java.use('android.widget.EditText');
                function controls(view,type,result){if(type.class.isInstance(view))result.push(Java.cast(view,type));if(Group.class.isInstance(view)){const group=Java.cast(view,Group);for(let i=0;i<group.getChildCount();i++)controls(group.getChildAt(i),type,result);}return result;}
                prefs.edit().putBoolean(key,false).commit();
                const Haiagaru=Java.use('app.morphe.extension.chmate.Haiagaru'),media=Haiagaru.settingsCategory(activity,'MEDIA');
                const toggle=controls(media,Compound,[]).find(v=>/動画からサムネイルを生成|Generate thumbnails from videos/.test(String(v.getText())));
                check(toggle!==undefined,'video frame thumbnail switch is reachable');
                check(!toggle.isChecked(),'saved false survives an upgrade');toggle.setChecked(true);
                check(prefs.getBoolean(key,false),'switch can re-enable video thumbnails');toggle.setChecked(false);
                check(!prefs.getBoolean(key,true),'switch can disable video thumbnails');
                const posting=Haiagaru.settingsCategory(activity,'POSTING');
                check(controls(posting,Edit,[]).some(v=>(v.getInputType()&0x200f)===0x2002),'custom posting size keeps decimal input flags');
                console.log('PASS '+checks+' ZZZSearch/settings regression checks: '+pkg);
            } finally {
                const dialog=activity.zzzDialog.value;if(dialog!==null)dialog.dismiss();activity.setComposeChanged(original);
                const edit=prefs.edit();if(had)edit.putBoolean(key,old);else edit.remove(key);edit.commit();activity.finish();
            }
        });return 'stop';},onComplete:function(){if(!found)console.log('FAIL search activity was not created');}});});},1800);
});
