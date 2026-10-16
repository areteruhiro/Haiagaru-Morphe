// Isolated rendering fixture. No posts, account requests or persistent preference changes.
setTimeout(function(){Java.perform(function () {
    const app=Java.use('android.app.ActivityThread').currentApplication();
    const pkg=String(app.getPackageName());
    const version=pkg.match(/\.haiagaru\.autoscroll(226|241|242)$/);
    if(!version)throw Error('Isolated auto-scroll fixture package required');
    const profiles={
        '226':['o.getChannel','b','e','o.addConnectException','b','bv','o.getUrl','c'],
        '241':['o.QWA11','c','d','o.getPageMargin','a','bs','o.removeNonIABCustomConsent','e'],
        '242':['o.AdChoicesView','b','a','o.onTextChanged','a','bq','o.getLinkUri','b']
    };
    const p=profiles[version[1]], Theme=Java.use(p[3]), theme=Java.retain(Theme[p[4]](app));
    const original=theme[p[5]].value, color=-2039584; // opaque #e0e0e0 on gray/night backgrounds
    theme[p[5]].value=color;
    const fragment=Java.use('jp.syoboi.a2chMate.fragment.RoidonListFragment');
    let composeType=version[1]==='242'?'o.r7ExternalSyntheticLambda0':version[1]==='241'?'o.getBCode':null,callbackType;
    if(composeType)for(const method of Java.use(composeType).class.getDeclaredMethods())
        if(String(method.getName())==='setContent')callbackType=String(method.getParameterTypes()[0].getName());
    for(const field of fragment.class.getDeclaredFields()) {
        const type=field.getType();
        for(const method of type.getDeclaredMethods())if(String(method.getName())==='setContent' && method.getParameterTypes().length===1) {
            composeType=String(type.getName()); callbackType=String(method.getParameterTypes()[0].getName());
        }
    }
    if(!composeType)throw Error('Native ComposeView not found');
    const Icons=Java.use(p[0]), iconOverload=Icons[p[1]].overloads.find(o=>o.argumentTypes.length===2);
    const composerType=iconOverload.argumentTypes[0].className;
    const Unit=Java.use(iconOverload.returnType.className);
    const unit=Unit[version[1]==='226'?'c':'INSTANCE'].value;
    let calls=0;
    setTimeout(function(){Java.scheduleOnMainThread(function(){
            let active=null;
            const records=Java.use('android.app.ActivityThread').currentActivityThread().mActivities.value.values().toArray();
            for(const item of records){const record=Java.cast(item,Java.use('android.app.ActivityThread$ActivityClientRecord'));
                if(record.activity.value && !record.activity.value.isFinishing())active=record.activity.value;}
            if(!active)throw Error('Foreground activity not found');
            const Layout=Java.use('android.widget.LinearLayout'), Text=Java.use('android.widget.TextView');
            const Params=Java.use('android.widget.LinearLayout$LayoutParams');
            const root=Layout.$new(active);root.setOrientation(1);root.setBackgroundColor(-16777216);root.setPadding(24,100,24,24);
            const title=Text.$new(active);title.setText.overload('java.lang.CharSequence').call(title,Java.use('java.lang.String').$new('自動スクロール −／＋ 表示検証 '+version[1]));title.setTextColor(color);root.addView(title);
            for(const background of [-12566464,-16777216]){
                const row=Layout.$new(active);row.setBackgroundColor(background);row.setPadding(16,24,16,24);root.addView(row);
                const label=Text.$new(active);label.setText.overload('java.lang.CharSequence').call(label,Java.use('java.lang.String').$new(background===-16777216?'夜':'灰'));label.setTextColor(color);row.addView(label,Params.$new(140,150));
                for(const action of [p[1],p[2]]){
                    const Callback=Java.registerClass({name:'app.haiagaru.fixture.ScrollColor'+version[1]+action+Math.abs(background),implements:[Java.use(callbackType)],methods:{invoke:{returnType:'java.lang.Object',argumentTypes:['java.lang.Object','java.lang.Object'],implementation:function(composer,changed){Icons[action].overload(composerType,'int').call(Icons,Java.cast(composer,Java.use(composerType)),0);calls++;return Java.cast(unit,Java.use('java.lang.Object'));}}}});
                    const view=Java.use(composeType).$new(active);view.setContent(Callback.$new());row.addView(view,Params.$new(180,150));
                }
            }
            active.setContentView(root);console.log('Fixture displayed; no preference changes');
    });},2000);
    setTimeout(function(){Java.perform(function(){console.log('Rendered native icon calls: '+calls);if(calls<4)console.log('FAIL missing native icon rendering');});},10000);
    rpc.exports={cleanup:function(){Java.perform(function(){theme[p[5]].value=original;});}};
});},2000);
