// Isolated app only. Native callbacks and persistence; server page/preview are mocked.
Java.perform(function(){
    const app=Java.use('android.app.ActivityThread').currentApplication();
    if(!String(app.getPackageName()).endsWith('.imgurhistory191'))throw Error('Isolated app only');
    const W=Java.use('app.morphe.extension.chmate.ImgurWebUpload');
    const H=Java.use('app.morphe.extension.chmate.ImgurUploadHistory');
    const P=app.getSharedPreferences('haiagaru_imgur_web',0);
    W.init(app);
    P.edit().putBoolean('nativeUploadHistory',true).putBoolean('uploadThreadHistory',true).commit();
    const thread='https://example.com/test/read.cgi/test/1791989395/';
    const link='https://i.imgur.com/Fixture123.png';
    let checks=0;
    function check(ok,title){if(!ok)throw Error(title);console.log('PASS '+title);checks++;}
    const Native=Java.use('o.onRewardedInterstitialAdLoadFailed');
    const Callback=Java.use('o.onRewardedInterstitialAdLoadFailed$2');
    const Request=Java.use('o.onRewardedInterstitialAdLoadFailed$IconCompatParcelizer');
    const JObject=Java.use('org.json.JSONObject');
    const response=W.nativeResponse(Java.use('o.r0ExternalSyntheticLambda13').class,
        JObject.$new(JSON.stringify({id:'Fixture123',link:link,deletehash:'FixtureOnly'})));
    let manager;
    Java.scheduleOnMainThread(function(){
        manager=Native.a.overload('android.content.Context').call(Native,app);
        const request=Request.$new(thread,Java.use('android.net.Uri').parse('content://test/local'),Java.use('o.p2').$new(),null);
        const callback=Callback.$new(manager,null,request);
        const before=manager.f.value===null?0:manager.f.value.size();
        H.uploaded(link,123);
        callback.a.overload('java.lang.Object').call(callback,response);
        check(manager.f.value.size()===before+1,'real native success callback adds one history row');
        P.edit().putBoolean('nativeUploadHistory',false).commit();
        H.uploaded(link,123);
        callback.a.overload('java.lang.Object').call(callback,response);
        check(manager.f.value.size()===before+1,'real callback OFF preserves existing history');
    });
    const page=Java.use('app.morphe.extension.chmate.ImgurWebUpload$UploadPage').$new(123,
        Java.use('org.json.JSONArray').$new.overload('java.lang.String').call(Java.use('org.json.JSONArray'),
            JSON.stringify([{id:'Fixture123',link:link,title:'履歴連携テスト',processing:false}])));
    W.uploadPage.implementation=function(){return page;};
    Java.use('app.morphe.extension.chmate.ImgurThumbnailLoader').fetch.implementation=function(){return null;};
    setTimeout(function(){Java.perform(function(){
        const stored=Native.d();
        check(stored.size()>0,'native writer persisted upload history');
        let found=false;for(let i=0;i<stored.size();i++){
            const r=Java.cast(stored.get(i),Java.use('o.onRewardedInterstitialAdDisplayed'));
            if(String(r.link.value)===link)found=true;
        }
        check(found,'native reader returns logged-in media URL');
        Java.scheduleOnMainThread(function(){
            const intent=Java.use('android.content.Intent').$new();
            intent.setClassName(app,'app.morphe.extension.chmate.ImgurUploadsActivity');
            intent.addFlags(0x10000000);app.startActivity(intent);
        });
    });},2200);
    setTimeout(function(){Java.perform(function(){Java.scheduleOnMainThread(function(){
        Java.choose('app.morphe.extension.chmate.ImgurUploadsActivity',{onMatch:function(a){
            if(a.isFinishing()||a.isDestroyed())return;
            let note=false,button=false;
            function walk(v){
                if(Java.use('android.widget.TextView').class.isInstance(v)){
                    const text=String(Java.cast(v,Java.use('android.widget.TextView')).getText());
                    if(text.includes('掲示板への投稿完了は未確認'))note=true;
                    if(text===thread)button=true;
                }
                if(Java.use('android.view.ViewGroup').class.isInstance(v)){
                    const g=Java.cast(v,Java.use('android.view.ViewGroup'));
                    for(let i=0;i<g.getChildCount();i++)walk(g.getChildAt(i));
                }
            }
            walk(a.findViewById(16908290));
            check(note&&button,'source-thread note and button rendered in uploads list');
            console.log('NATIVE/UI CHECKS='+checks);
        },onComplete:function(){}});
    });});},4000);
});
