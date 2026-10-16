// Isolated app only. Public sample playback, no authentication/upload/delete.
Java.perform(function(){
    const app=Java.use('android.app.ActivityThread').currentApplication();
    if(!String(app.getPackageName()).endsWith('.videooption191'))throw Error('Isolated app required');
    const A=Java.use('app.morphe.extension.chmate.VideoViewerActivity');
    const M=Java.use('android.view.MotionEvent'),Clock=Java.use('android.os.SystemClock');
    const p=Java.use('android.preference.PreferenceManager').getDefaultSharedPreferences(app);
    const actions=[];
    A.performVideoGesture.implementation=function(key){actions.push(String(key));return this.performVideoGesture(key);};
    function main(fn){Java.perform(function(){Java.scheduleOnMainThread(fn);});}
    function check(ok,msg){if(!ok)throw Error(msg);console.log('PASS '+msg);}
    function motion(a,swipe){
        const t=Clock.uptimeMillis();
        const steps=swipe?[[0,800,900,0],[2,600,900,40],[2,300,900,80],[1,200,900,120]]:[[0,700,900,0],[1,700,900,20]];
        for(const s of steps){const e=M.obtain(t,t+s[3],s[0],s[1],s[2],0);a.dispatchTouchEvent(e);e.recycle();}
    }
    main(function(){
        const T=Java.use('android.app.ActivityThread');let active=null;
        for(const item of T.currentActivityThread().mActivities.value.values().toArray()){
            const r=Java.cast(item,Java.use('android.app.ActivityThread$ActivityClientRecord'));
            if(r.activity.value&&r.activity.value.hasWindowFocus())active=r.activity.value;
        }
        Java.use('o.lo').b.overload('android.app.Activity','int','java.lang.String','boolean').call(Java.use('o.lo'),active,0,'https://i.imgur.com/A54Xv8k.mp4',true);
    });
    setTimeout(function(){main(function(){Java.choose('app.morphe.extension.chmate.VideoViewerActivity',{
        onMatch:function(found){
            if(found.isFinishing()||found.isDestroyed())return;
            const a=Java.retain(found);
            a.controls.value.hide();
            p.edit().putBoolean('gesture',true).putInt('gestureBack',4).putInt('gestureForward',0)
                .putInt('gestureTop',0).putInt('gestureBottom',0).putInt('gestureRefresh',0)
                .putInt('gestureFullScreen',0).putInt('gestureSetting',0).putInt('gestureCloseTab',0)
                .putInt('haiagaruGestureNgEdit',0).putInt('gestureLength',40).commit();
            motion(a,false);
            setTimeout(function(){main(function(){
                check(actions.length===0&&!a.isFinishing(),'tap does not execute gesture');
                p.edit().putBoolean('gesture',false).commit();motion(a,true);
                setTimeout(function(){main(function(){
                    check(actions.length===0&&!a.isFinishing(),'global gesture OFF leaves viewer open');
                    p.edit().putBoolean('gesture',true).putInt('gestureBack',0).putInt('gestureTop',4).commit();
                    a.controls.value.hide();motion(a,true);
                    setTimeout(function(){main(function(){
                        console.log('top actions='+JSON.stringify(actions)+' focus='+a.hasWindowFocus()+' closed='+a.gestures.value.closed.value);
                        check(actions.length===1&&actions[0]==='gestureTop','saved top gesture uses native recognition');
                        p.edit().putInt('gestureTop',0).putInt('gestureBack',4).commit();
                        a.controls.value.hide();motion(a,true);
                        setTimeout(function(){main(function(){
                            check(a.isFinishing()&&actions[1]==='gestureBack','back gesture closes viewer');
                            a.$dispose();
                        });},300);
                    });},300);
                });},300);
            });},300);
        },onComplete:function(){}
    });});},2000);
});
