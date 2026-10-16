// Synthetic rules only, on isolated emulator apps. Every rule is removed in finally.
Java.perform(function () {
    const app = Java.use('android.app.ActivityThread').currentApplication();
    const pkg = app.getPackageName().toString();
    if (!/^jp\.co\.airfront\.android\.a2chMate\.haiagaru\.ngsuffix(191|226|241|242)$/.test(pkg))
        throw new Error('Isolated NG test package required');
    const Store = Java.use('app.morphe.extension.chmate.NativeNgStore');
    const JString = Java.use('java.lang.String');
    const ObjectArray = values => Java.array('java.lang.Object', values);
    globalThis.showWordScopeFixture = function () { Java.perform(function () {
        const adapter=Store.$new(app,'WORD');
        adapter.update(null,adapter.item('__HaiagaruScopeUiFixture__',57,1792108300000,'android'));
        Java.choose('android.app.Activity',{onMatch:function(a){
            if(a.hasWindowFocus()) { const keep=Java.retain(a); Java.scheduleOnMainThread(function(){Java.use('app.morphe.extension.chmate.NgManagement').show(keep);}); }
        },onComplete:function(){}});
    }); };
    globalThis.verifyWordScopeFixture = function () { Java.perform(function () {
        const adapter=Store.$new(app,'WORD'), before=adapter.entries(),after=Java.use('java.util.ArrayList').$new();
        let found=false;
        for(let i=0;i<before.size();i++) {
            const entry=Java.cast(before.get(i),Java.use('app.morphe.extension.chmate.NativeNgStore$Entry'));
            if(entry.pattern.value.toString()==='__HaiagaruScopeUiFixture__') {
                if(entry.board.value.toString()!=='@haiagaru:boards:out:android' || entry.flags.value!==57
                        || Number(entry.timestamp.value)!==1792108300000) throw new Error('UI changed flags/time or did not save exclusion');
                found=true;
            } else after.add(entry.nativeItem.value);
        }
        if(!found)throw new Error('WORD UI fixture missing');
        adapter.replace(before,after);
        console.log('PASS '+pkg+': WORD scope edited via real UI, flags/timestamp preserved; fixture removed');
    }); };
    globalThis.testNativeBoardScopes = function () { Java.perform(function () {
        for (const category of ['NAME', 'WORD']) {
            const adapter = Store.$new(app, category);
            const store = adapter.store.value;
            const methods = store.getClass().getDeclaredMethods();
            let lookup = null, compiled = null;
            for (let i=0;i<methods.length;i++) {
                const m=methods[i], p=m.getParameterTypes();
                if (p.length===2 && p[0].getName().toString()==='java.lang.String'
                        && m.getReturnType().getName().toString()==='jp.syoboi.a2chMate.ng.NGWord$Item') lookup=m;
                if (p.length===1 && p[0].getName().toString()==='java.lang.String'
                        && m.getReturnType().isInterface()) compiled=m;
            }
            if (lookup===null || compiled===null) throw new Error('Native board methods unavailable');
            lookup.setAccessible(true); compiled.setAccessible(true);
            const Board=Java.use(lookup.getParameterTypes()[1].getName().toString());
            const token=JString.$new('__HaiagaruBoardScopeFixture987654__');
            const scopes=[null,'android','@haiagaru:boards:in:android,poverty','@haiagaru:boards:out:android,poverty'];
            for (let s=0;s<scopes.length;s++) {
                const item=adapter.item(token,0x10,Date.now(),scopes[s]);
                try {
                    adapter.update(null,item);
                    for (const board of ['android','poverty','edge','future_board']) {
                        const expected=s===0 || s===1 && board==='android' || s===2 && ['android','poverty'].includes(board)
                            || s===3 && !['android','poverty'].includes(board);
                        let id=null;
                        if(pkg.endsWith('191')) id=Board.$new.overload('java.lang.String').call(Board,board);
                        else if(pkg.endsWith('242')) id=Board.$new(null,'fixture.invalid',board,'');
                        else {
                            const parsers=Board.class.getDeclaredMethods();
                            for(let k=0;k<parsers.length;k++) {
                                const m=parsers[k], p=m.getParameterTypes();
                                if((m.getModifiers()&8)!==0 && p.length===1 && p[0].getName().toString()==='java.lang.String'
                                        && m.getReturnType().getName().toString()===Board.class.getName().toString()) {
                                    m.setAccessible(true); id=m.invoke(null,ObjectArray([JString.$new(board)])); break;
                                }
                            }
                        }
                        if(id===null || id.toString().toString()!==board)throw new Error('Synthetic board parser unavailable');
                        const result=lookup.invoke(store,ObjectArray([token,id]));
                        if ((result!==null)!==expected) throw new Error(category+' direct filter failed '+s+'/'+board);
                        const matcher=compiled.invoke(store,ObjectArray([JString.$new(board)]));
                        const checks=matcher.getClass().getMethods();
                        let matched=false, count=0;
                        for(let j=0;j<checks.length;j++) {
                            const m=checks[j],p=m.getParameterTypes();
                            if(m.getReturnType().getName().toString()!=='boolean' || p.length!==1
                                    || p[0].getName().toString()!=='java.lang.CharSequence') continue;
                            m.setAccessible(true); count++;
                            const value=Java.cast(m.invoke(matcher,ObjectArray([token])),Java.use('java.lang.Boolean')).booleanValue();
                            if(value)matched=true;
                        }
                        if(!count || matched!==expected) throw new Error(category+' cached matcher failed '+s+'/'+board);
                    }
                } finally {
                    const before=adapter.entries(), after=Java.use('java.util.ArrayList').$new();
                    for(let i=0;i<before.size();i++) {
                        const entry=Java.cast(before.get(i),Java.use('app.morphe.extension.chmate.NativeNgStore$Entry'));
                        if(!entry.pattern.value.toString().startsWith('__HaiagaruBoardScopeFixture')) after.add(entry.nativeItem.value);
                    }
                    adapter.replace(before,after);
                }
            }
            console.log('PASS '+pkg+' '+category+': direct + cached NG, global/single/include/exclude/future boards');
        }
    }); };
    console.log('Ready: testNativeBoardScopes()');
});
