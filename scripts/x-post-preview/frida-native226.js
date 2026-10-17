/* Native 226 TextView dispatcher with a seeded local preview. Post-link callback is mocked.
 * Intent construction is checked separately by PreviewTest's RecordingContext.
 * Diagnostic only: R8 can inline the callback, bypassing this Frida mock.
 * opened=0 with handled down/up is inconclusive, not proof of a broken link. */
Java.perform(function(){
    Java.deoptimizeEverything(); // Ensure the browser-launch mock is not bypassed by ART inlining.
    const app=Java.use('android.app.ActivityThread').currentApplication();
    if(String(app.getPackageName())!=='jp.co.airfront.android.a2chMate.haiagaru.responsearchive226')throw Error('Isolated app required');
    const prefs=app.getSharedPreferences('io.github.areteruhiro.chmate.haiagaru.ui-config',0);
    const old=prefs.getAll(), enabled=prefs.getBoolean('xPostPreviewEnabled',false);
    prefs.edit().putBoolean('xPostPreviewEnabled',true).commit();
    const A=Java.use('app.morphe.extension.chmate.XPostPreview');
    const Entry=Java.use('app.morphe.extension.chmate.XPostPreview$Entry');
    const entry=Entry.$new();entry.data.value=A.parse('{"id_str":"123","text":"ネイティブ確認用の本文","user":{"name":"確認","screen_name":"fixture"}}','123');
    const field=A.class.getDeclaredField('CACHE');field.setAccessible(true);
    const cache=Java.cast(field.get(null),Java.use('java.util.Map'));
    const previous=cache.put('123',entry);
    const start=Java.use('app.morphe.extension.chmate.XPostPreviewTouch$PostLink').onClick.overload('android.view.View');
    let opened=0;
    start.implementation=function(view){
        if(String(this.getURL())==='https://x.com/fixture/status/123'){
            opened++;return;
        }
        throw Error('Unexpected post link');
    };
    Java.scheduleOnMainThread(function(){
        try{
            const text=Java.use('o.TTVideoLandingPageLink2Activity8').$new.overload('android.content.Context','android.util.AttributeSet').call(Java.use('o.TTVideoLandingPageLink2Activity8'),app,null);
            const Buffer=Java.use('android.widget.TextView$BufferType');
            text.setText.overload('java.lang.CharSequence','android.widget.TextView$BufferType').call(text,
                Java.use('android.text.SpannableString').$new.overload('java.lang.CharSequence')
                    .call(Java.use('android.text.SpannableString'),Java.use('java.lang.String').$new('https://x.com/fixture/status/123')),Buffer.SPANNABLE.value);
            A.onBound(text);
            if(String(text.getText()).indexOf('ネイティブ確認用の本文')<0)throw Error('Cached preview not bound before measurement');
            text.measure(0x40000000|900,0x80000000|20000);text.layout(0,0,900,text.getMeasuredHeight());
            const offset=String(text.getText()).lastIndexOf('投稿を開く')+1,layout=text.getLayout(),line=layout.getLineForOffset(offset);
            const x=layout.getPrimaryHorizontal(offset)+text.getTotalPaddingLeft();
            const y=(layout.getLineTop(line)+layout.getLineBottom(line))/2+text.getTotalPaddingTop();
            const time=Java.use('android.os.SystemClock').uptimeMillis();
            const Event=Java.use('android.view.MotionEvent');
            const down=Event.obtain(time,time,0,x,y,0),up=Event.obtain(time,Number(time)+40,1,x,y,0);
            const downHandled=text.dispatchTouchEvent(down),upHandled=text.dispatchTouchEvent(up);
            if(!downHandled||!upHandled)
                throw Error('Native post-link dispatcher failed: opened='+opened+' down='+downHandled+' up='+upHandled+' context='+text.getContext().$className+' x='+x+' y='+y+' offset='+offset+' layout='+text.getLayout().getHeight());
            down.recycle();up.recycle();
            console.log(opened===1
                ? 'PASS native 226 cached binding and post-link touch dispatch; callback mocked'
                : 'INCONCLUSIVE native callback mock bypassed (possible R8 inlining); native down/up handled');
        }finally{
            start.implementation=null;
            if(previous===null)cache.remove('123');else cache.put('123',previous);
            const editor=prefs.edit();if(old.containsKey('xPostPreviewEnabled'))editor.putBoolean('xPostPreviewEnabled',enabled);else editor.remove('xPostPreviewEnabled');editor.commit();
        }
    });
});
