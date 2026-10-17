/* Native 226 close-state regression matrix. Isolated test app, in-memory pages only. */
'use strict';
Java.perform(function () {
    const app=Java.use('android.app.ActivityThread').currentApplication();
    if(String(app.getPackageName())!=='jp.co.airfront.android.a2chMate.haiagaru.responsearchive226')
        throw Error('Isolated 226 app required');
    const Model=Java.use('o.GNk'), State=Java.use('o.Ff'), Page=Java.use('o.Kjv');
    const List=Java.use('java.util.ArrayList'), Bundle=Java.use('android.os.Bundle');
    let checks=0;
    function check(ok,label){if(!ok)throw Error(label);checks++;}
    for(let size=1;size<=5;size++)for(let selected=0;selected<size;selected++)for(let removed=-1;removed<=size;removed++) {
        const pages=List.$new();for(let i=0;i<size;i++)pages.add(Page.$new(3,Bundle.$new()));
        const model=Model.$new(1,State.$new(pages,selected));
        const selectedId=String(Java.cast(pages.get(selected),Page).h.value);
        model.d.overload('int').call(model,removed);
        const flowField=model.getClass().getDeclaredField('d');flowField.setAccessible(true);
        const flow=Java.cast(flowField.get(model),Java.use('o.zzbc'));
        const state=Java.cast(flow.c(),State);
        const valid=removed>=0&&removed<size;
        const expected=!valid?selected:selected===removed?Math.min(removed,size-2):selected>removed?selected-1:selected;
        const label='size='+size+' selected='+selected+' removed='+removed;
        check(Number(state.a.value)===expected,'selection '+label+' expected='+expected+' actual='+state.a.value);
        check(state.b.value.size()===(valid?size-1:size),'count '+label);
        if(expected>=0&&selected!==removed)
            check(String(Java.cast(state.b.value.get(expected),Page).h.value)===selectedId,'preserves page identity '+label);
        if(valid)check(state.b.value.contains(pages.get(removed))===false,'removes requested page '+label);
    }
    console.log('COMPLETE '+checks+' native 226 tab-close checks; no network or persistent pages');
});
