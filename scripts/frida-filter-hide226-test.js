// Offline, isolated apps only. Exercises the real 226 ComposeView and its patched onMeasure.
Java.perform(function(){Java.scheduleOnMainThread(function(){
    const app=Java.use('android.app.ActivityThread').currentApplication();
    const pkg=String(app.getPackageName());
    if(!pkg.endsWith('.filterhide226')&&!pkg.endsWith('.titlespacing226'))throw Error('Isolated app only');
    const fixed=pkg.endsWith('.filterhide226');
    const H=Java.use('app.morphe.extension.chmate.Haiagaru');
    const Q=Java.use('app.morphe.extension.chmate.QuickFilterToolbar');
    const prefs=H.preferences(app);
    let activity=null;
    const records=Java.use('android.app.ActivityThread').currentActivityThread().mActivities.value.values().toArray();
    for(const r of records){const a=Java.cast(r,Java.use('android.app.ActivityThread$ActivityClientRecord')).activity.value;
        if(a&&a.hasWindowFocus()){activity=a;break;}}
    if(!activity)throw Error('No focused activity');
    const root=Java.use('android.widget.LinearLayout').$new(activity);root.setOrientation(1);
    const Row=Java.use('o.m2a');
    const row=Row.$new.overload('android.content.Context').call(Row,activity);
    const control=Row.$new.overload('android.content.Context').call(Row,activity);
    for(const v of [row,control]){
        v.setPadding(0,40,0,40);
        root.addView(v,Java.use('android.widget.LinearLayout$LayoutParams').$new(-1,-2));
    }
    activity.setContentView(root);
    let checks=0;
    function check(ok,msg){if(!ok)throw Error(msg);checks++;console.log('PASS '+msg);}
    function measure(v){v.forceLayout();v.measure(1073742284,0);return v.getMeasuredHeight();}
    setTimeout(function(){Java.perform(function(){Java.scheduleOnMainThread(function(){
        prefs.edit().putBoolean('compactQuickFilters',false).commit();
        const before=measure(row),other=measure(control);
        check(before>0&&other>0,'normal Compose content has nonzero height');
        check(row.getChildCount()>0,'Compose initialization remains active');
        prefs.edit().putBoolean('compactQuickFilters',true).commit();Q.prepareFilterRow(row);
        check(row.getLayoutParams().height.value===0,'filter LayoutParams height is zero');
        const hidden=measure(row);
        console.log('baseline='+before+' hidden='+hidden+' width='+row.getMeasuredWidth());
        if(!fixed){check(hidden>0,'REPRODUCED old ListView UNSPECIFIED measurement ignores height zero');
            console.log('BASELINE CHECKS='+checks);return;}
        check(hidden===0,'registered filter measured height is zero');
        check(row.getMeasuredWidth()===460,'width preserved');
        check(row.getChildCount()>0,'Compose tree retained while hidden');
        check(measure(control)===other,'unregistered Compose rows are unchanged');
        // Rebinding/ListView can replace LayoutParams. The final measurement must still collapse.
        // This fixture is hosted in a LinearLayout; keep its required LayoutParams subtype.
        row.setLayoutParams(Java.use('android.widget.LinearLayout$LayoutParams').$new(-1,-2));
        check(measure(row)===0,'hidden survives LayoutParams replacement');
        prefs.edit().putBoolean('compactQuickFilters',false).commit();Q.prepareFilterRow(row);
        check(measure(row)===before,'OFF restores original content height');
        check(!Q.shouldCollapseMeasuredFilterRow(row),'OFF removes collapse marker');
        prefs.edit().putBoolean('compactQuickFilters',true).commit();Q.prepareFilterRow(row);
        check(measure(row)===0,'ON again collapses after reuse');
        console.log('FILTER CHECKS='+checks);
    });});},700);
});});
