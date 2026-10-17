/* Actual tablet ViewPager with local blank pages, no real thread URL or posting. */
'use strict';
Java.perform(function () {
    const app=Java.use('android.app.ActivityThread').currentApplication();
    if(String(app.getPackageName())!=='jp.co.airfront.android.a2chMate.haiagaru.responsearchive226')throw Error('Isolated 226 app required');
    const Tablet=Java.use('jp.syoboi.a2chMate.activity.TabletHomeActivity');
    const Intent=Java.use('android.content.Intent');
    Java.scheduleOnMainThread(()=>app.startActivity(Intent.$new(app,Tablet.class).addFlags(0x10000000)));
    setTimeout(()=>Java.perform(()=>Java.choose(Tablet.$className,{
        onMatch:function(activity){
            if(!activity.getWindow().getDecorView().isShown())return;
            const host=Java.retain(activity);
            Java.scheduleOnMainThread(function(){
                function field(obj,name){const f=obj.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(obj);}
                const panes=field(host,'r');
                const pane=Java.retain(Java.cast(Java.use('java.lang.reflect.Array').get(panes,1),Java.use('o.QWA')));
                const model=Java.retain(Java.cast(field(pane,'a'),Java.use('o.GNk')));
                const flow=Java.retain(Java.cast(field(model,'d'),Java.use('o.zzbc')));
                const pager=Java.retain(Java.cast(field(pane,'b'),Java.use('androidx.viewpager.widget.ViewPager')));
                const Ff=Java.use('o.Ff'), Page=Java.use('o.Kjv'), Bundle=Java.use('android.os.Bundle');
                const added=[], marker='HAIAGARU_TAB_FIXTURE_'+Date.now();
                const previous=Number(Java.cast(flow.c(),Ff).a.value);
                let checks=0;
                function state(){return Java.cast(flow.c(),Ff);}
                function check(ok,label){if(!ok)throw Error(label);checks++;console.log('PASS '+label);}
                function later(fn){setTimeout(()=>Java.perform(()=>Java.scheduleOnMainThread(fn)),600);}
                function make(n){const b=Bundle.$new();b.putString('android.intent.extra.TITLE',marker+n);const p=Page.$new(3,b);added.push(String(p.h.value));return p;}
                function open(p){model.d.overload('o.Kjv','o.fWG','boolean').call(model,p,Java.use('o.fWG').values()[1],true);}
                function selected(p,label){
                    const s=state(), selection=Number(s.a.value), pageIndex=pager.getCurrentItem();
                    check(selection>=0&&String(Java.cast(s.b.value.get(selection),Page).h.value)===String(p.h.value),label+' model ID');
                    check(pageIndex===selection,label+' ViewPager position');
                    check(!Java.use('o.QWA').h(pane),label+' guard released');
                }
                const p1=Java.retain(make(1)), p2=Java.retain(make(2)), p3=Java.retain(make(3));
                open(p1);
                later(()=>{selected(p1,'first open');open(p2);
                later(()=>{selected(p2,'second open');model.d.overload('int').call(model,Number(state().a.value));
                later(()=>{check(Number(state().a.value)===-1,'closing selected tab does not select another through feedback');open(p3);
                later(()=>{selected(p3,'new tab after close');
                    const pos=model.d.overload('o.Kjv').call(model,p1);
                    model.d.overload('int').call(model,pos);
                later(()=>{selected(p3,'removing earlier tab keeps same page');
                    // Remove only fixture pages, retaining every pre-existing native page.
                    function clean(){const s=state();for(let i=s.b.value.size()-1;i>=0;i--){
                        if(added.indexOf(String(Java.cast(s.b.value.get(i),Page).h.value))>=0){model.d.overload('int').call(model,i);later(clean);return;}}
                        if(previous>=0&&previous<s.b.value.size())model.c(previous);
                        console.log('COMPLETE 226 native tablet '+checks+' checks (local blank pages, not gesture input)');
                    }
                    clean();
                });});});});});
            });
            return 'stop';
        },onComplete:function(){}
    })),2000);
});
