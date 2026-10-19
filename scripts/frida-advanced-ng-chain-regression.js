// Isolated emulator packages only; evaluate synthetic posts through the shipped controller/JS engine.
Java.perform(function () { Java.scheduleOnMainThread(function () {
    const app = Java.use('android.app.ActivityThread').currentApplication();
    const pkg = String(app.getPackageName());
    if (!/^jp\.co\.airfront\.android\.a2chMate\.haiagaru\.(full191|resumefix226|resumefix242)$/.test(pkg))
        throw Error('Isolated NG regression package required');
    let checks = 0;
    function check(ok, label) { if (!ok) throw Error(label + '\n' + Controller.status()); checks++; console.log('PASS ' + label); }
    const Controller = Java.use('app.morphe.extension.chmate.ProgrammableNgController');
    const Rule = Java.use('app.morphe.extension.chmate.ProgrammableNgRuleSet$Rule');
    const ArrayList = Java.use('java.util.ArrayList'), HashMap = Java.use('java.util.HashMap');
    const Integer = Java.use('java.lang.Integer');
    const prefs = app.getSharedPreferences('haiagaru.programmable-ng', 0);
    const oldEnabled = prefs.getBoolean('enabled', false), hadEnabled = prefs.contains('enabled');
    const oldRules = prefs.getString('rules', ''), hadRules = prefs.contains('rules');
    const oldBoard = Controller.lastBoardUrl.value;
    const ns = 'app.morphe.test.ng.t' + Date.now();
    const is191 = pkg.endsWith('191'), is226 = pkg.endsWith('226');
    const fields = is191 ? {o:'int',h:'java.lang.String',c:'java.lang.String',n:'java.lang.String',j:'java.lang.String',q:'java.lang.String'}
        : is226 ? {l:'int',j:'java.lang.String',d:'java.lang.String',m:'java.lang.String',g:'java.lang.String',t:'java.lang.String'}
        : {o:'int',g:'java.lang.String',h:'java.lang.String',n:'java.lang.String',j:'java.lang.String',p:'java.lang.String'};
    const Row = Java.registerClass({name: ns + '.' + (is191 ? 'LegacyPost' : is226 ? 'BouncyCastleSocketAdapterCompanion' : 'KeJ11'), fields:fields});
    const number = is226 ? 'l' : 'o', id = is191 ? 'h' : is226 ? 'j' : 'g', body = is191 ? 'c' : is226 ? 'd' : 'h';
    function row(n, who, text) {
        const r = Row.$new(); Object.keys(fields).forEach(k => { if(fields[k] !== 'int')r[k].value = ''; });
        r[number].value=n; r[id].value=who; r[body].value=text; return r;
    }
    const posts=[row(10,'A','NGワード'),row(20,'A','other'),row(30,'B','&gt;&gt;20'),row(40,'C','&gt;&gt;30'),row(50,'D','unrelated')];
    const all=ArrayList.$new(); posts.forEach(r=>all.add(r));
    const Filtered=Java.registerClass({name:ns+'.Filtered',superClass:ArrayList,fields:{mBase:'java.util.List'}});
    const filtered=Filtered.$new(); filtered.mBase.value=all; filtered.add(posts[2]);
    const sample=Controller.templateRule(4), list=ArrayList.$new(); list.add(sample);
    const script=String(sample.script.value), BOARD='https://egg.5ch.io/android/';
    function save(rule, enabled) { const rules=ArrayList.$new(); rules.add(rule); Controller.save(enabled,rules); }
    try {
        check(String(sample.target.value)==='body' && script.indexOf('chainReplies')>=0,'built-in author/reply template uses API v2');
        save(sample,true);
        if (is191 || is226) {
            const State=Java.registerClass({name:ns+'.State',fields:{[is191?'f':'j']:'java.lang.String'}});
            const Owner=Java.registerClass({name:ns+'.Owner',fields:{[is191?'M':'E']:'java.lang.Object',[is191?'K':'J']:'java.util.List'}});
            const state=State.$new(); state[is191?'f':'j'].value=BOARD;
            const owner=Owner.$new(); owner[is191?'M':'E'].value=state; owner[is191?'K':'J'].value=filtered;
            function prepare(){if(is191)Controller.prepareLegacyResponses(owner);else Controller.prepareModernResponses(owner);}
            function flags(r){return is191?Controller.mergeLegacyResponseFlags(owner,r,0x400):Controller.mergeModernResponseFlags(owner,r,0x400);}
            prepare();
            posts.forEach((r,i)=> {const f=flags(r);check((f&0x400)!==0,'preserve native flags '+i);check(((f&16)!==0)===(i<4),'full source author/reply result '+i);});
            check(String(Controller.preview(sample)).indexOf('5件中 4件')>=0,'preview includes the whole unfiltered thread and expanded matches');
            posts[0][body].value='clean';prepare();
            posts.forEach((r,i)=>check((flags(r)&16)===0,'rebuild clears stale match '+i));
            posts[0][body].value='NGワード';prepare();check((flags(posts[3])&16)!==0,'rebuild restores descendants after new seed');
            const scoped=Rule.$new('scoped','board','body',true,false,BOARD,script);save(scoped,true);
            state[is191?'f':'j'].value='https://example.org/other/';prepare();
            check((flags(posts[0])&16)===0,'scoped rule does not borrow a previous tab board');
            state[is191?'f':'j'].value=BOARD;
            save(sample,false);prepare();check((flags(posts[0])&16)===0,'master off removes advanced matches');
            const api=Rule.$new('api','api','body',true,true,'','function(t,o){return o.apiVersion === 2;}');
            save(api,true);prepare();check((flags(posts[4])&16)!==0,'controller passes apiVersion=2');
        } else {
            const Result=Java.registerClass({name:ns+'.Result',fields:{a:'java.util.List',d:'java.lang.String'}});
            const result=Result.$new();result.a.value=filtered;result.d.value=BOARD;
            const original=HashMap.$new();original.put(Integer.valueOf(50),Integer.valueOf(0x400));
            function apply(){Controller.bindModernResponses(result);Controller.pendingModernResponses(filtered);return Controller.applyPendingModernResponseFlags(original);}
            function flag(map,n){const v=map.get(Integer.valueOf(n));return v===null?0:Java.cast(v,Integer).intValue();}
            let output=apply();
            posts.forEach((r,i)=>check(((flag(output,r[number].value)&16)!==0)===(i<4),'full source author/reply result '+i));
            check(flag(output,50)===0x400,'preserve native flags');
            check(original.size()===1 && flag(original,10)===0,'native shared NG map is not mutated');
            check(String(Controller.preview(sample)).indexOf('5件中 4件')>=0,'preview counts expanded full-thread matches');
            save(sample,false);output=apply();check(output.size()===1 && flag(output,10)===0,'master off removes overlay without stale flags');
            const scoped=Rule.$new('scoped','board','body',true,false,BOARD,script);save(scoped,true);
            Controller.lastBoardUrl.value=BOARD;result.d.value='https://example.org/other/';output=apply();
            check(output.size()===1,'exact DAT board prevents cross-tab rule leakage');
            result.d.value=BOARD;save(sample,true);posts[0][body].value='clean';output=apply();
            check(output.size()===1,'removing seed removes ID and reply expansion despite predicate cache');
            posts[0][body].value='NGワード';output=apply();check((flag(output,40)&16)!==0,'new seed restores cached descendants');
            const api=Rule.$new('api','api','body',true,true,'','function(t,o){return o.apiVersion === 2;}');
            save(api,true);output=apply();check((flag(output,50)&16)!==0,'controller passes apiVersion=2');
        }
        const Fragment=Java.use(is191?'o.pa':'jp.syoboi.a2chMate.ui.reslist.ResListFragment');
        check(Fragment.$new()!==null,'native thread Fragment still verifies');
        console.log('PASS '+checks+' advanced NG controller checks: '+pkg);
    } finally {
        const edit=prefs.edit();if(hadEnabled)edit.putBoolean('enabled',oldEnabled);else edit.remove('enabled');
        if(hadRules)edit.putString('rules',oldRules);else edit.remove('rules');edit.commit();Controller.reload();
        Controller.lastBoardUrl.value=oldBoard;
    }
}); });
