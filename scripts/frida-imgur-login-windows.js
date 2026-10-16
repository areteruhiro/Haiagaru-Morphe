// Login window UI regression only: no credentials/cookies are read and no upload occurs.
Java.perform(function() {
    const app=Java.use('android.app.ActivityThread').currentApplication();
    if(app.getPackageName().toString()!=='jp.co.airfront.android.a2chMate.haiagaru.imgur191')
        throw new Error('Isolated Imgur191 fixture required');
    Java.choose('app.morphe.extension.chmate.ImgurWebLoginActivity', {
        onMatch:function(activity) { Java.scheduleOnMainThread(function() {
            if(activity.isFinishing()||activity.isDestroyed())return;
            function field(name,type) {
                const f=activity.getClass().getDeclaredField(name);f.setAccessible(true);
                return Java.cast(f.get(activity),Java.use(type));
            }
            const windows=field('windows','java.util.ArrayList'), parent=field('web','android.webkit.WebView');
            const status=field('status','android.widget.TextView'), originalStatus=status.getText();
            if(windows.size()!==1)throw new Error('Close active login popup before running fixture');
            const expected=Java.use('android.webkit.WebSettings').getDefaultUserAgent(activity);
            if(parent.getSettings().getUserAgentString().toString()!==expected.toString())
                throw new Error('WebView UA was spoofed');
            const Handler=Java.use('android.os.Handler');
            const handler=Handler.$new.overload('android.os.Looper').call(Handler,Java.use('android.os.Looper').getMainLooper());
            const message=Java.use('android.os.Message').obtain.overload('android.os.Handler').call(Java.use('android.os.Message'),handler);
            const transport=Java.use('android.webkit.WebView$WebViewTransport').$new(parent);
            message.obj.value=transport;
            const chrome=parent.getWebChromeClient();
            if(chrome.onCreateWindow(parent,false,false,message)||windows.size()!==1)
                throw new Error('Non-user popup allowed');
            if(!chrome.onCreateWindow(parent,false,true,message)||windows.size()!==2)
                throw new Error('User popup not opened inside WebView');
            const child=transport.getWebView();
            if(child===null||parent.getVisibility()!==8)throw new Error('Popup not visible');
            const client=child.getWebViewClient();
            if(client.shouldOverrideUrlLoading.overload('android.webkit.WebView','java.lang.String')
                    .call(client,child,'https://accounts.google.com/'))throw new Error('Google navigation blocked');
            if(!client.shouldOverrideUrlLoading.overload('android.webkit.WebView','java.lang.String')
                    .call(client,child,'https://accounts.google.com.evil/'))throw new Error('Foreign navigation allowed');
            chrome.onCloseWindow(child);
            if(windows.size()!==1||parent.getVisibility()!==0)throw new Error('Popup close failed');
            status.setText(originalStatus);
            console.log('PASS default WebView UA, user-gesture popup, Google allowlist, foreign-host rejection, popup cleanup');
        }); },
        onComplete:function(){}
    });
});
