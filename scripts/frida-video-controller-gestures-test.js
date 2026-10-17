// Emulator verification package only. Fake playback, restore all changed preferences.
Java.perform(function () {
    const app = Java.use('android.app.ActivityThread').currentApplication();
    if (String(app.getPackageName()) !== 'jp.co.airfront.android.a2chMate.haiagaru.archivemenu191')
        throw Error('Isolated emulator package required');
    const A = Java.use('app.morphe.extension.chmate.VideoViewerActivity');
    const P = Java.use('android.preference.PreferenceManager').getDefaultSharedPreferences(app);
    const H = app.getSharedPreferences('io.github.areteruhiro.chmate.haiagaru.ui-config', 0);
    const nativeKeys = ['gesture','gestureLength','gestureBack','gestureForward','gestureTop','gestureBottom',
        'gestureRefresh','gestureFullScreen','gestureSetting','gestureCloseTab','haiagaruGestureNgEdit'];
    const extraKeys = ['videoViewerEnabled','videoSwipeBrightness','videoSwipeVolume','videoDoubleTapSeek'];
    function snapshot(p, keys) { return keys.map(k => {
        const v=p.getAll().get(k); return [k,v===null?null:String(v)];
    }); }
    const before = snapshot(P,nativeKeys), extra = snapshot(H,extraKeys);
    function restore(p, rows) {
        const e=p.edit(); for (const [k,v] of rows) {
            if(v===null)e.remove(k);
            else if(v==='true'||v==='false')e.putBoolean(k,v==='true');
            else e.putInt(k,parseInt(v,10));
        } e.commit();
    }
    let actions=[], viewer=null, checks=0;
    function check(ok,label){if(!ok)throw Error(label);checks++;console.log('PASS '+label);}
    A.load.implementation=function(){this.prepared.value=false;};
    A.performVideoGesture.implementation=function(key){actions.push(String(key));console.log('ACTION '+key);};
    const G=Java.use('app.morphe.extension.chmate.VideoViewerGestures');
    G.complete.implementation=function(code){console.log('COMPLETE '+code);return this.complete(code);};
    const I=Java.use('android.content.Intent'),U=Java.use('android.net.Uri');
    H.edit().putBoolean('videoViewerEnabled',true).putBoolean('videoSwipeBrightness',false)
        .putBoolean('videoSwipeVolume',false).putBoolean('videoDoubleTapSeek',false).commit();
    const edit=P.edit().putBoolean('gesture',true).putInt('gestureLength',40);
    for(const key of nativeKeys.slice(2))edit.putInt(key,key==='gestureTop'?4:0);
    edit.commit();
    Java.scheduleOnMainThread(function(){
        const intent=I.$new('android.intent.action.VIEW',U.parse('https://i.imgur.com/A54Xv8k.mp4'));
        intent.setClassName(app,'app.morphe.extension.chmate.VideoViewerActivity').addFlags(0x10000000);
        app.startActivity(intent);
    });
    function main(fn){Java.perform(()=>Java.scheduleOnMainThread(fn));}
    const M=Java.use('android.view.MotionEvent'),C=Java.use('android.os.SystemClock');
    function swipe(){
        const v=viewer.video.value,p=Java.array('int',[0,0]);v.getLocationOnScreen(p);
        const y=Number(p[1])+v.getHeight()*.45, w=v.getWidth(),t=Number(C.uptimeMillis());
        for(const [action,x,dt] of [[0,w*.8,0],[2,w*.6,60],[2,w*.3,120],[1,w*.2,180]]){
            const event=M.obtain(t,t+dt,action,x,y,0);viewer.dispatchTouchEvent(event);event.recycle();
        }
    }
    setTimeout(()=>main(()=>{
        const T=Java.use('android.app.ActivityThread'),R=Java.use('android.app.ActivityThread$ActivityClientRecord');
        for(const value of T.currentActivityThread().mActivities.value.values().toArray()){
            const record=Java.cast(value,R),raw=record.activity.value;
            if(!raw||String(raw.getClass().getName())!=='app.morphe.extension.chmate.VideoViewerActivity')continue;
            const a=Java.cast(raw,A);
            if(a.isFinishing()||!a.foreground.value)continue;
            console.log('active viewer='+a+' focusBeforeShow='+a.hasWindowFocus());
            viewer=Java.retain(a);a.controls.value.show(0);
        }
    }),1200);
    setTimeout(()=>main(()=>{
        try{
            check(viewer!==null,'viewer opened without network');
            check(viewer.controls.value.isShowing(),'controls visible');
            console.log('activityFocus='+viewer.hasWindowFocus()+' controllerFocus='+viewer.controls.value.hasWindowFocus());
            check(!viewer.hasWindowFocus()&&viewer.controls.value.hasWindowFocus(),'controller owns focus (reported condition)');
            swipe();
            setTimeout(()=>main(()=>{try {
            check(actions.length===1&&actions[0]==='gestureTop','native swipe works with controls visible');
            P.edit().putBoolean('gesture',false).commit();swipe();
            check(actions.length===1,'global OFF still prevents gesture');
            P.edit().putBoolean('gesture',true).commit();
            viewer.foreground.value=false;swipe();check(actions.length===1,'background viewer cannot execute gesture');
            viewer.foreground.value=true;viewer.controls.value.hide();
            setTimeout(()=>main(()=>{
                try{swipe();
                    setTimeout(()=>main(()=>{try {
                    check(actions.length===2,'hidden controls still permit native swipe');
                    console.log('PASS total='+checks);
                    }finally{restore(P,before);restore(H,extra);viewer.finish();}}),350);
                }catch(error){restore(P,before);restore(H,extra);viewer.finish();throw error;}
            }),300);
            }catch(error){console.log('FAIL '+error);restore(P,before);restore(H,extra);viewer.finish();throw error;}}),350);
        }catch(error){console.log('FAIL '+error);restore(P,before);restore(H,extra);if(viewer)viewer.finish();throw error;}
    }),2200);
});
