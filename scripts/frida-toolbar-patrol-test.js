// Isolated native toolbar. Records callbacks, never starts a real patrol or deletes logs.
Java.perform(function () {
  Java.deoptimizeEverything();
  const app=Java.retain(Java.use('android.app.ActivityThread').currentApplication());
  const pkg=String(app.getPackageName());
  if(!/\.haiagaru\.(uploadhosts191|responsearchive226|responsearchive242)$/.test(pkg))throw Error('Verification app only');
  const version=pkg.slice(-3);
  const configs={
    '191':{toolbar:'o.r8lambdaAcLFFW49T1Yql_ZgYak67s2Jmds',info:'o.r8lambda98incQ33GAiiY2082BMi9yDa3l0',item:'o.r8lambda8tdvLujHLP2NtDIECDdS14CNq9s',theme:'o.r8lambda0m18vyepBPbBImKp0mAya80YXc8',themeField:'r',list:'g',layout:'b',pressed:'i',runnable:'b'},
    '226':{toolbar:'o.setAdInteractionListener',info:'o.PAGBannerAdLoadCallback',item:'o.PAGBannerAdInteractionListener',theme:'o.addConnectException',themeField:'u',list:'b',layout:'e',pressed:'p',runnable:'o'},
    '242':{toolbar:'o.zzcB',info:'o.zzcI',item:'o.zzcC',theme:'o.onTextChanged',themeField:'mTheme',list:'d',layout:'b',pressed:'mPressed',runnable:'mLongClickRunnable'}
  },cfg=configs[version];
  Java.scheduleOnMainThread(function(){try{
    let clicks=[],longs=[],checks=0;
    function check(ok,label){if(!ok)throw Error(label);checks++;console.log('PASS '+label);}
    const View=Java.use('android.view.View'),Toolbar=Java.use(cfg.toolbar);
    const toolbar=Toolbar.$new(app,null),dummy=Java.cast(toolbar.getChildAt(0),View);
    const Info=Java.use(cfg.info),Item=Java.use(cfg.item);
    function field(name){const f=Toolbar.class.getDeclaredField(name);f.setAccessible(true);return f;}
    const theme=Java.cast(field(cfg.themeField).get(toolbar),Java.use(cfg.theme));
    function item(id){const info=version==='191'?Info.$new(id,0,0,0,true):Info.$new(Java.use('jp.syoboi.a2chMate.feature.toolbar.ToolbarButtonSpec').$new(id,0,0,0,true));return Item.$new(toolbar,info,theme,0);}
    const refresh=item(5),other=item(9);
    const items=Java.cast(field(cfg.list).get(toolbar),Java.use('java.util.List'));items.add(refresh);items.add(other);
    const unique=Date.now();
    const Click=Java.registerClass({name:'app.morphe.test.PatrolClick'+unique,implements:[Java.use('android.view.View$OnClickListener')],methods:{onClick:function(v){clicks.push(v.getId());}}});
    const Long=Java.registerClass({name:'app.morphe.test.PatrolLong'+unique,implements:[Java.use('android.view.View$OnLongClickListener')],methods:{onLongClick:function(v){longs.push(v.getId());return true;},onLongClickUseDefaultHapticFeedback:function(){return false;}}});
    toolbar.setOnClickListener(Click.$new());toolbar.setOnLongClickListener(Long.$new());
    toolbar.layout(0,0,600,100);
    refresh[cfg.layout].overload('int','int','int','boolean').call(refresh,0,300,100,false);
    other[cfg.layout].overload('int','int','int','boolean').call(other,300,300,100,false);
    // Drain only this unattached test view's queue; never hook real app Views.
    const queueField=View.class.getDeclaredField('mRunQueue');queueField.setAccessible(true);
    function flush(){const raw=queueField.get(toolbar);if(raw===null)return;const q=Java.cast(raw,Java.use('android.view.HandlerActionQueue'));while(q.size()>0){const r=q.getRunnable(0);toolbar.removeCallbacks(r);r.run();}}
    const Event=Java.use('android.view.MotionEvent'),Clock=Java.use('android.os.SystemClock');
    function tap(x){const time=Clock.uptimeMillis();for(const action of [0,1]){const e=Event.obtain(time,time,action,x,50,0);toolbar.dispatchTouchEvent(e);e.recycle();}flush();}
    check(!dummy.isClickable()&&!dummy.isLongClickable(),'native shared dispatch child is not touch-consuming');
    Java.use('app.morphe.extension.chmate.DeleteReadThreads').bind(toolbar);
    check(!dummy.isClickable()&&!dummy.isLongClickable(),'compatibility bind does not mutate shared child');
    tap(150);check(clicks.length===1&&clicks[0]===5,'first refresh/patrol tap reaches native callback');
    check(dummy.getId()===5,'native refresh reuses child ID 5');
    // Reproduce 1.8.13 bind(): global layout finds this child and installs a listener.
    dummy.setOnLongClickListener(Long.$new());
    check(dummy.isLongClickable(),'old binding makes shared child consume touch');
    tap(150);check(clicks.length===1,'old binding reproduces second refresh/patrol tap swallowed');
    dummy.setOnLongClickListener(null);dummy.setLongClickable(false);
    tap(150);tap(150);check(clicks.length===3,'without child binding repeated refresh/patrol taps work');
    tap(450);check(clicks.length===4&&clicks[3]===9,'other toolbar action remains functional');
    field(cfg.pressed).set(toolbar,refresh);
    Java.cast(field(cfg.runnable).get(toolbar),Java.use('java.lang.Runnable')).run();
    check(longs.length===1&&longs[0]===5,'native long-press dispatch works without a child listener');
    tap(150);check(clicks.length===5&&clicks[4]===5,'refresh/patrol still works after native long press');
    check(!dummy.isClickable()&&!dummy.isLongClickable(),'shared child remains non-interactive');
    console.log('COMPLETE '+checks+' native toolbar patrol checks '+version);
  }catch(e){console.log('FAIL '+e.stack);}});
});
