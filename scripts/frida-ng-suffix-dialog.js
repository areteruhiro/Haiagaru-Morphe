// Only isolated suffix-dialog test packages; never attach to a user's normal app.
Java.perform(function () {
    const app = Java.use('android.app.ActivityThread').currentApplication();
    const pkg = app.getPackageName().toString();
    if (!/^jp\.co\.airfront\.android\.a2chMate\.haiagaru\.ngsuffix(191|226|241|242)$/.test(pkg))
        throw new Error('Isolated suffix test package required');
    function activity() {
        const thread = Java.use('android.app.ActivityThread').currentActivityThread();
        const records = thread.mActivities.value.values().toArray();
        let fallback = null;
        for (let i = 0; i < records.length; i++) {
            const f = records[i].getClass().getDeclaredField('activity'); f.setAccessible(true);
            const a = Java.cast(f.get(records[i]), Java.use('android.app.Activity'));
            if (!a.isFinishing()) {
                fallback = a;
                if (a.hasWindowFocus()) return a;
            }
        }
        if (fallback !== null) return fallback;
        throw new Error('No activity');
    }
    function fixtureRow() {
        const version = app.getPackageManager().getPackageInfo(pkg, 0).versionName.value.toString();
        const old = version === '0.8.10.226 dev';
        const builder = Java.use(old ? 'o.setDomStorageEnabled' : 'o.OnUserEarnedRewardListener')
            .$new(Java.cast(app, Java.use('android.app.Application')));
        Java.use('app.morphe.extension.chmate.WacchoiLongPressMenu').appendNameSheet(null, builder, null, '(ｽｯﾌﾟ Sd03-aabb)');
        const f = builder.getClass().getDeclaredField(old ? 'c' : 'b'); f.setAccessible(true);
        const items = Java.cast(f.get(builder), Java.use('java.util.List'));
        return items.get(0);
    }
    globalThis.showNativeMenu = function () { Java.scheduleOnMainThread(function () {
        const old = pkg.endsWith('226');
        if (!old && !pkg.endsWith('241')) throw new Error('226/241 only');
        const row = Java.retain(fixtureRow());
        const ui = Java.use(old ? 'o.setDatabaseEnabled' : 'o.MobileAdsInitProvider');
        const nativeMethod = ui.class.getDeclaredMethods().filter(m => {
            const p=m.getParameterTypes();
            return p.length===4 && p[0].getName().toString()===(old?'o.setDisplayZoomControls':'o.MuteThisAdListener');
        })[0];
        nativeMethod.setAccessible(true);
        const view=Java.use('androidx.compose.ui.platform.ComposeView').$new(activity());
        const setContent=view.getClass().getMethods().filter(m=>m.getName().toString()==='setContent' && m.getParameterTypes().length===1)[0];
        const callbackClass=setContent.getParameterTypes()[0];
        const Handler=Java.registerClass({
            name:'app.morphe.extension.chmate.fixture.SuffixContent'+(old?'226':'241'),
            implements:[Java.use('java.lang.reflect.InvocationHandler')],
            methods:{invoke:function(proxy,method,args){
                if(method.getName().toString()==='invoke') {
                    nativeMethod.invoke(null,Java.array('java.lang.Object',[row,null,args[0],Java.use('java.lang.Integer').valueOf(0)]));
                    return null;
                }
                if(method.getName().toString()==='toString') return Java.use('java.lang.String').$new('Suffix fixture');
                if(method.getName().toString()==='hashCode') return Java.use('java.lang.Integer').valueOf(123);
                return Java.use('java.lang.Boolean').FALSE.value;
            }}
        });
        const callback=Java.use('java.lang.reflect.Proxy').newProxyInstance(callbackClass.getClassLoader(),
            Java.array('java.lang.Class',[callbackClass]),Handler.$new());
        setContent.invoke(view,Java.array('java.lang.Object',[callback]));
        activity().setContentView.overload('android.view.View').call(activity(),view);
        console.log('Showing real native name-sheet row in isolated ComposeView');
    }); };
    globalThis.showSuffix = function (query) { Java.scheduleOnMainThread(function () {
        Java.use('app.morphe.extension.chmate.WacchoiSuffixNg').confirm.overload('android.content.Context','java.lang.String','java.lang.String')
            .call(Java.use('app.morphe.extension.chmate.WacchoiSuffixNg'), activity(), query || '(ｽｯﾌﾟ Sd03-aabb)', 'android');
        console.log('Opened real suffix registration dialog: ' + pkg);
    }); };
    globalThis.inspectSuffix = function () { Java.scheduleOnMainThread(function () {
        const store = Java.use('app.morphe.extension.chmate.NativeNgStore').$new(activity(), 'NAME');
        const entries = store.entries();
        for (let i=0; i<entries.size(); i++) {
            const item=Java.cast(entries.get(i), Java.use('app.morphe.extension.chmate.NativeNgStore$Entry'));
            console.log('Fixture NG: ' + item.pattern.value + ', flags=' + item.flags.value + ', board=' + item.board.value);
        }
    }); };
    globalThis.saveFixtureDialog = function (regexMode, append, suffix) { Java.scheduleOnMainThread(function () {
        const helper=Java.use('app.morphe.extension.chmate.WacchoiSuffixNg');
        helper.confirm.overload('android.content.Context','java.lang.String','java.lang.String')
            .call(helper,activity(),'(test 1234-'+suffix+')','android');
        setTimeout(function () { Java.perform(function () { Java.scheduleOnMainThread(function () {
        const wm=Java.use('android.view.WindowManagerGlobal').getInstance();
        const field=wm.getClass().getDeclaredField('mViews');field.setAccessible(true);
        const roots=Java.cast(field.get(wm),Java.use('java.util.List'));
        const root=Java.cast(roots.get(roots.size()-1),Java.use('android.view.View'));
        let ok=null, edit=null, regex=null; const spinners=[], checks=[];
        function scan(view) {
            const name=view.getClass().getName().toString();
            if(Java.use('android.widget.EditText').class.isInstance(view))edit=Java.cast(view,Java.use('android.widget.EditText'));
            if(Java.use('android.widget.Spinner').class.isInstance(view))spinners.push(Java.cast(view,Java.use('android.widget.Spinner')));
            if(Java.use('android.widget.TextView').class.isInstance(view)) {
                const text=Java.cast(view,Java.use('android.widget.TextView')).getText().toString();
                if(text==='OK')ok=view;
                if(text==='正規表現')regex=view;
                if(text==='連鎖'||text==='非表示（あぼーん扱いにしない）')checks.push(view);
            }
            if(Java.use('android.view.ViewGroup').class.isInstance(view)) {
                const group=Java.cast(view,Java.use('android.view.ViewGroup'));
                for(let i=0;i<group.getChildCount();i++)scan(group.getChildAt(i));
            }
        }
        scan(root);
        if(!ok||!edit||!regex||spinners.length!==2)throw new Error('Dialog controls not found');
        if(regexMode)regex.performClick();else checks.forEach(v=>v.performClick());
        if(append) {
            if(spinners[1].getCount()<2)throw new Error('No append target');
            spinners[1].setSelection(1);
        }
        console.log('Saving actual dialog: '+edit.getText()+', regex='+regexMode+', target='+spinners[1].getSelectedItemPosition());
        ok.performClick();
        }); }); }, 700);
    }); };
    globalThis.testLegacyPopupClick = function (showForTouch) { Java.scheduleOnMainThread(function () {
        if(!pkg.endsWith('191'))throw new Error('191 only');
        const a=activity(), helper=Java.use('app.morphe.extension.chmate.WacchoiLongPressMenu');
        const Host=Java.registerClass({name:'app.morphe.extension.chmate.fixture.SuffixClickHost191',
            fields:{owner:'android.app.Activity'},methods:{getActivity:{returnType:'android.app.Activity',argumentTypes:[],implementation:function(){return this.owner.value;}}}});
        const Callback=Java.registerClass({name:'app.morphe.extension.chmate.fixture.SuffixPopupConsumed191',
            implements:[Java.use('o.qm$IconCompatParcelizer')],methods:{Xf_:function(item){console.log('Native outer dispatcher consumed item '+item.getItemId());return true;},e:function(){return false;}}});
        const host=Host.$new();host.owner.value=a;
        const context=Java.use('app.morphe.extension.chmate.WacchoiLongPressMenu$SearchContext').$new();
        context.query.value='(test 1234-aabb)';context.nativeBoard.value='android';
        const menu=Java.use('o.m6fExternalSyntheticLambda0').$new.overload('android.content.Context').call(Java.use('o.m6fExternalSyntheticLambda0'),a);
        helper.append(menu,host,host,context);
        const adapter=Java.use('o.m6f').$new.overload('android.content.Context','android.view.Menu').call(Java.use('o.m6f'),a,menu);
        const popup=Java.use('o.qm').$new(a,adapter,Callback.$new());
        if (showForTouch) {
            const anchor=Java.use('android.widget.TextView').$new(a);
            anchor.setText(Java.use('java.lang.String').$new('191 suffix popup touch fixture'));
            anchor.setPadding(30,30,30,30);
            const frame=Java.use('android.widget.FrameLayout').$new(a);
            frame.addView.overload('android.view.View','android.view.ViewGroup$LayoutParams').call(frame,anchor,
                Java.use('android.widget.FrameLayout$LayoutParams').$new(650,180));
            a.setContentView.overload('android.view.View').call(a,frame);
            anchor.post(Java.registerClass({name:'app.morphe.extension.chmate.fixture.ShowSuffixPopup191',
                implements:[Java.use('java.lang.Runnable')],methods:{run:function(){popup.a.overload('android.view.View').call(popup,anchor);}}}).$new());
            console.log('Showing real qm popup; test actual touch on its row');return;
        }
        popup.onItemClick(null,null,0,0);
        console.log('Dispatched real qm popup click; inspect whether NGName追加 is visible');
    }); };
    globalThis.testLegacyRow = function () { Java.scheduleOnMainThread(function () {
        if(!pkg.endsWith('191'))throw new Error('191 only');
        const a=activity(), helper=Java.use('app.morphe.extension.chmate.WacchoiLongPressMenu');
        const Host=Java.registerClass({name:'app.morphe.extension.chmate.fixture.SuffixHost191',
            fields:{owner:'android.app.Activity'},methods:{getActivity:{returnType:'android.app.Activity',argumentTypes:[],implementation:function(){return this.owner.value;}}}});
        const host=Host.$new();host.owner.value=a;
        const context=Java.use('app.morphe.extension.chmate.WacchoiLongPressMenu$SearchContext').$new();
        context.query.value='(test 1234-aabb)';context.nativeBoard.value='android';
        const anchor=Java.use('android.widget.TextView').$new(a);
        const popup=Java.use('android.widget.PopupMenu').$new(a,anchor), menu=popup.getMenu();
        helper.append(menu,host,host,context);
        const item=menu.findItem(76);
        if(item===null)throw new Error('Missing suffix MenuItem');
        const adapter=Java.use('o.m6f').$new.overload('android.content.Context','android.view.Menu')
            .call(Java.use('o.m6f'),a,menu);
        const row=adapter.getView(0,null,Java.use('android.widget.FrameLayout').$new(a));
        if(!row.isLongClickable()||!row.performLongClick())throw new Error('Long press not consumed');
        console.log('PASS 191 real MenuItem -> bound Android row -> performLongClick; verify NG editor');
    }); };
    globalThis.testSuffixRowRecycling = function () { Java.scheduleOnMainThread(function () {
        const a=activity(), helper=Java.use('app.morphe.extension.chmate.WacchoiLongPressMenu');
        const menu=Java.use('o.m6fExternalSyntheticLambda0').$new.overload('android.content.Context')
            .call(Java.use('o.m6fExternalSyntheticLambda0'),a);
        const suffix=menu.add.overload('int','int','int','java.lang.CharSequence')
            .call(menu,0,76,0,Java.use('java.lang.String').$new('suffix'));
        menu.add.overload('int','int','int','java.lang.CharSequence')
            .call(menu,0,90,1,Java.use('java.lang.String').$new('unrelated'));
        const mapField=helper.class.getDeclaredField('SUFFIX_LONG_PRESS');mapField.setAccessible(true);
        const actions=Java.cast(mapField.get(null),Java.use('java.util.Map'));
        let longs=0, clicks=0, nativeClicks=0;
        const Action=Java.registerClass({name:'app.morphe.extension.chmate.fixture.RowLongAction',
            implements:[Java.use('java.lang.Runnable')],methods:{run:function(){longs++;}}});
        const Click=Java.registerClass({name:'app.morphe.extension.chmate.fixture.RowNativeClick',
            implements:[Java.use('android.view.View$OnClickListener')],methods:{onClick:function(v){nativeClicks++;}}});
        const ItemClick=Java.registerClass({name:'app.morphe.extension.chmate.fixture.RowParentClick',
            implements:[Java.use('android.widget.AdapterView$OnItemClickListener')],
            methods:{onItemClick:function(parent,view,position,id){if(position!==0)throw new Error('Wrong current position');clicks++;}}});
        actions.put(suffix,Action.$new());
        const adapter=Java.use('o.m6f').$new.overload('android.content.Context','android.view.Menu')
            .call(Java.use('o.m6f'),a,menu);
        const list=Java.use('android.widget.ListView').$new(a);list.setAdapter(adapter);
        list.setOnItemClickListener(ItemClick.$new());
        a.setContentView.overload('android.view.View').call(a,list);
        list.measure(1073742500,1073742500);list.layout(0,0,676,676);
        const row=list.getChildAt(0);if(row===null)throw new Error('No row');
        row.performClick();if(clicks!==1)throw new Error('Normal tap not forwarded exactly once');
        row.performLongClick();if(longs!==1||clicks!==1)throw new Error('Long press also clicked');
        helper.bindSuffixRow(adapter,0,row);row.performClick();
        if(clicks!==2)throw new Error('Rebind duplicated/lost click');
        helper.bindSuffixRow(adapter,1,row);
        if(row.isLongClickable()||row.isClickable()||row.hasOnClickListeners())throw new Error('Recycled row retained listeners');
        row.setOnClickListener(Click.$new());helper.bindSuffixRow(adapter,0,row);row.performClick();
        helper.bindSuffixRow(adapter,1,row);row.performClick();
        if(nativeClicks!==2)throw new Error('Native row listener overwritten');
        console.log('PASS normal click forwarding, long press isolation, rebind, recycling, native click preservation');
    }); };
    globalThis.testNativeLongPress = function () { Java.scheduleOnMainThread(function () {
        const version = app.getPackageManager().getPackageInfo(pkg, 0).versionName.value.toString();
        const old = version === '0.8.10.226 dev';
        const newest = version.indexOf('0.8.10.242') === 0;
        if (!old && !newest && version !== '0.8.10.241') throw new Error('226/241/242 gesture fixture only');
        const helper = Java.use('app.morphe.extension.chmate.WacchoiLongPressMenu');
        const builder = Java.use(old ? 'o.setDomStorageEnabled' : newest ? 'o.isDataValid' : 'o.OnUserEarnedRewardListener')
            .$new(Java.cast(app, Java.use('android.app.Application')));
        helper.appendNameSheet(null, builder, null, '(ｽｯﾌﾟ Sd03-aabb)');
        const itemsField = builder.getClass().getDeclaredField(old ? 'c' : newest ? 'a' : 'b'); itemsField.setAccessible(true);
        const items = Java.cast(itemsField.get(builder), Java.use('java.util.List'));
        if (items.size() !== 1) throw new Error('Expected only suffix fixture action');
        const row = items.get(0);
        const mapField = helper.class.getDeclaredField('SUFFIX_LONG_PRESS'); mapField.setAccessible(true);
        const actions = Java.cast(mapField.get(null), Java.use('java.util.Map'));
        let click = null;
        const fields = row.getClass().getDeclaredFields();
        for (let i=0; i<fields.length; i++) {
            fields[i].setAccessible(true); const value = fields[i].get(row);
            if (value !== null && actions.containsKey(value)) { click=value; break; }
        }
        if (click === null) throw new Error('Suffix callback missing');
        const owner = Java.use(old ? 'o.createFragmentContainer' : newest ? 'o.setOnScrollChangeListener' : 'o.onStart');
        const methods = owner.class.getDeclaredMethods();
        let simple = null;
        for(let i=0;i<methods.length;i++) {
            const p=methods[i].getParameterTypes();
            if(p.length === (old ? 5 : 6) && p[1].getName().toString()==='boolean'
                    && p[2].getName().toString()==='java.lang.String' && p[p.length-1].getName().toString()==='int') simple=methods[i];
        }
        if(simple===null) throw new Error('Native clickable unavailable');
        const modifierClass=simple.getParameterTypes()[0];
        const modifierFields=modifierClass.getDeclaredFields();
        let modifier=null;
        for(let i=0;i<modifierFields.length;i++) {
            modifierFields[i].setAccessible(true);
            if((modifierFields[i].getModifiers() & 8)!==0) {
                const value=modifierFields[i].get(null);
                if(value!==null && modifierClass.isInstance(value)) {modifier=value;break;}
            }
        }
        if(modifier===null) throw new Error('Modifier singleton unavailable');
        const args=[modifier,Java.use('java.lang.Boolean').TRUE.value,null];
        if(!old) args.push(null);
        args.push(click,Java.use('java.lang.Integer').valueOf(0));
        const combined=simple.invoke(null,Java.array('java.lang.Object',args));
        if(combined===null) throw new Error('No native modifier');
        let longClick=null;
        function visit(object,depth) {
            if(object===null || depth>4 || longClick!==null) return;
            const f=object.getClass().getDeclaredFields();
            for(let i=0;i<f.length;i++) {
                if((f[i].getModifiers()&8)!==0 || f[i].getType().isPrimitive()) continue;
                f[i].setAccessible(true); const value=f[i].get(object);
                if(value===null)continue;
                if(Java.use('java.lang.reflect.Proxy').isProxyClass(value.getClass()) && value.toString().toString()==='HaiagaruSuffixLongPress') {longClick=value;return;}
                if(value.getClass().getName().toString().startsWith('o.')) visit(value,depth+1);
            }
        }
        visit(combined,0);
        if(longClick===null) throw new Error('Native combined modifier has no suffix long callback');
        console.log('PASS actual native clickable hook -> combined long press: '+version);
        const callback=longClick.getClass().getInterfaces()[0];
        callback.getMethod('invoke',Java.array('java.lang.Class',[])).invoke(longClick,Java.array('java.lang.Object',[]));
        console.log('Invoked native stored long callback; verify NGName editor is foreground');
    }); };
    console.log('Ready: showSuffix(query), inspectSuffix(), testNativeLongPress(), showNativeMenu()');
});
