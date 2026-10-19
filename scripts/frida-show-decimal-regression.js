// Leaves one decimal field on an isolated test Activity for IME inspection; does not edit preferences.
Java.perform(function(){
    const app=Java.retain(Java.use('android.app.ActivityThread').currentApplication()),pkg=String(app.getPackageName());
    if(pkg!=='jp.co.airfront.android.a2chMate.haiagaru.full191')throw Error('Isolated full191 package required');
    const name='app.morphe.extension.chmate.HaiagaruSettingsActivity';
    function find(attempt) { Java.perform(function(){Java.scheduleOnMainThread(function(){
        const records=Java.use('android.app.ActivityThread').currentActivityThread().mActivities.value.values().iterator();
        const Record=Java.use('android.app.ActivityThread$ActivityClientRecord');let activity=null;
        while(records.hasNext()) {
            const a=Java.cast(records.next(),Record).activity.value;
            if(a!==null && String(a.getClass().getName())===name && a.mResumed.value && !a.isFinishing() && a.hasWindowFocus())
                activity=Java.cast(a,Java.use(name));
        }
        if(activity===null){
            if(attempt===0){const intent=Java.use('android.content.Intent').$new();intent.setClassName(pkg,name);intent.addFlags(0x10000000);app.startActivity(intent);}
            if(attempt<20)setTimeout(function(){find(attempt+1);},400);else console.log('FAIL settings Activity never became focused');return;
        }
        const model=Java.use('android.widget.LinearLayout').$new(activity);model.setOrientation(1);
        const JString=Java.use('java.lang.String');
        const label=Java.use('android.widget.TextView').$new(activity);label.setText(JString.$new('Decimal regression'));model.addView(label);
        const field=Java.use('android.widget.EditText').$new(activity);field.setSingleLine(true);field.setInputType(0x2002);
        field.setText(JString.$new('16.5'),Java.use('android.widget.TextView$BufferType').EDITABLE.value);model.addView(field);
        Java.use('app.morphe.extension.chmate.HaiagaruUi').showForm(activity,'Decimal regression',model);console.log('READY decimal Compose field');
    });}); }
    setTimeout(function(){find(0);},400);
});
