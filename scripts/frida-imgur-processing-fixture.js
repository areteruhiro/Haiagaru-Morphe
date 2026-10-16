// Isolated emulator only. ALL Imgur connections/session are replaced. Never uploads to the Internet.
Java.perform(function () {
    Java.deoptimizeEverything();
    const app = Java.use('android.app.ActivityThread').currentApplication();
    if (String(app.getPackageName()) !== 'jp.co.airfront.android.a2chMate.haiagaru.imgurthumb191') throw Error('Isolated thumbnail191 required');
    const H = Java.use('app.morphe.extension.chmate.ImgurWebUpload');
    const Session = Java.use('app.morphe.extension.chmate.ImgurWebUpload$Session');
    const J = Java.use('java.lang.String');
    const Thumbnail = Java.use('app.morphe.extension.chmate.ImgurThumbnailLoader');
    Thumbnail.fetch.implementation=function(){return Java.use('android.graphics.Bitmap').createBitmap(96,64,Java.use('android.graphics.Bitmap$Config').ARGB_8888.value);};
    const prefs = H.prefs(app), key = 'processingWaitSeconds', had = prefs.contains(key), old = prefs.getInt(key, 120);
    const savedClip = Java.cast(app.getSystemService('clipboard'), Java.use('android.content.ClipboardManager')).getPrimaryClip();
    let posts = 0, polls = 0, mode = 'complete';
    const payload = {id:'Test123',account_id:123,account_url:'FixtureUser',deletehash:'FixtureOnly',type:'video/mp4',link:'https://i.imgur.com/Test123.mp4',processing:false};
    const Fake = Java.registerClass({
        name: 'app.morphe.test.ImgurProcessingConnection' + Date.now(),
        superClass: Java.use('java.net.HttpURLConnection'),
        methods: {
            $init:[{returnType:'void',argumentTypes:['java.net.URL'],implementation:function(url){this.$super.$init(url);}}],
            connect:{returnType:'void',argumentTypes:[],implementation:function(){}},
            disconnect:{returnType:'void',argumentTypes:[],implementation:function(){}},
            usingProxy:{returnType:'boolean',argumentTypes:[],implementation:function(){return false;}},
            getResponseCode:{returnType:'int',argumentTypes:[],implementation:function(){return 200;}},
            getOutputStream:{returnType:'java.io.OutputStream',argumentTypes:[],implementation:function(){return Java.use('java.io.ByteArrayOutputStream').$new();}},
            getInputStream:{returnType:'java.io.InputStream',argumentTypes:[],implementation:function(){
                const url=String(this.getURL()), path=String(this.getURL().getPath());
                let data;
                if(path==='/3/account/me') data={id:123,url:'FixtureUser'};
                else if(path.startsWith('/3/account/me/images/')) data=[payload,{id:'Pending',processing:true},{id:'Png123',link:'https://i.imgur.com/Png123.png',processing:false}];
                else if(path==='/3/upload'){posts++;data=Object.assign({},payload,{processing:true});}
                else if(path==='/3/image/Test123'){polls++;data=Object.assign({},payload,{processing:mode==='timeout',account_id:null,account_url:null,deletehash:null});}
                else throw Error('Unrecognized mock path: '+path);
                const bytes=J.$new(JSON.stringify({success:true,data:data})).getBytes('UTF-8');
                return Java.use('java.io.ByteArrayInputStream').$new(bytes);
            }}
        }
    });
    H.session.implementation=function(){return Session.$new('FixtureClient','FixtureCookie','FixtureAgent');};
    H.open.implementation=function(url,session){return Fake.$new(Java.use('java.net.URL').$new(url));};
    const file=Java.use('java.io.File').$new(app.getCacheDir(),'haiagaru-imgur-processing-mock.mp4');
    if(file.exists())throw Error('Existing fixture file, abort');
    let output=null;
    function cleanup(){
        H.session.implementation=null;H.open.implementation=null;Thumbnail.fetch.implementation=null;
        const e=prefs.edit();if(had)e.putInt(key,old);else e.remove(key);e.commit();
        if(output)output.close(); file.delete();
        const clipboard=Java.cast(app.getSystemService('clipboard'),Java.use('android.content.ClipboardManager'));
        if(savedClip)clipboard.setPrimaryClip(savedClip);else clipboard.clearPrimaryClip();
        console.log('Fixture cleaned; preferences/clipboard restored; no real uploads');
    }
    try {
        output=Java.use('java.io.FileOutputStream').$new(file);
        output.write(Java.array('byte',[0,0,0,12,102,116,121,112,105,115,111,109])); output.close(); output=null;
        const response=Java.use('o.r0ExternalSyntheticLambda13').class;
        prefs.edit().putInt(key,5).commit();
        H.upload(Java.array('java.lang.Object',[file]),response);
        if(posts!==1||polls!==1)throw Error('Completion must send once and GET once');
        console.log('PASS real upload wrapper waits for processing=false (one POST, one GET; all mocked)');
        const postBefore=posts,pollBefore=polls;
        prefs.edit().putInt(key,0).commit();
        try{H.upload(Java.array('java.lang.Object',[file]),response);throw Error('Expected timeout');}
        catch(error){if(!String(error).includes('処理待ち時間'))throw error;}
        if(posts!==postBefore+1||polls!==pollBefore)throw Error('Zero wait must not poll or resend');
        console.log('PASS zero wait does not poll/resend');
        mode='timeout';prefs.edit().putInt(key,3).commit();
        const before=posts;
        try{H.upload(Java.array('java.lang.Object',[file]),response);throw Error('Expected timeout');}
        catch(error){if(!String(error).includes('処理待ち時間'))throw error;}
        if(posts!==before+1)throw Error('Timeout must not resend');
        console.log('PASS pending processing times out without a repeated POST');
        const rows=H.uploads(0);
        if(rows.length()!==3)throw Error('Account list parsing');
        console.log('PASS account images list parsing (PNG, MP4, pending)');
    }catch(error){cleanup();throw error;}
    Java.scheduleOnMainThread(function(){
        const intent=Java.use('android.content.Intent').$new(app,Java.use('app.morphe.extension.chmate.ImgurUploadsActivity').class);
        intent.addFlags(0x10000000);intent.putExtra.overload('java.lang.String','boolean').call(intent,'show_uploads',true);app.startActivity(intent);
        setTimeout(function(){Java.perform(function(){
            Java.choose('app.morphe.extension.chmate.ImgurUploadsActivity',{
                onMatch:function(activity){const retained=Java.retain(activity);Java.scheduleOnMainThread(function(){retained.showUploads(0);});return 'stop';},onComplete:function(){}
            });
            setTimeout(function(){Java.perform(function(){Java.choose('app.morphe.extension.chmate.ImgurUploadsActivity',{
                onMatch:function(activity){const retained=Java.retain(activity);Java.scheduleOnMainThread(function(){try{
                    const group=retained.uploadsRows.value;
                    let enabled=0,disabled=0;
                    for(let i=0;i<group.getChildCount();i++){
                        const child=group.getChildAt(i);
                        if(!Java.use('android.widget.Button').class.isInstance(child))continue;
                        const b=Java.cast(child,Java.use('android.widget.Button'));
                        if(String(b.getText())!=='URLをコピー')continue;
                        if(b.isEnabled()){enabled++;b.performClick();}else disabled++;
                    }
                    if(enabled!==2||disabled!==1)throw Error('List copy button states');
                    const clipboard=Java.cast(app.getSystemService('clipboard'),Java.use('android.content.ClipboardManager'));
                    if(String(clipboard.getPrimaryClip().getItemAt(0).getText())!=='https://i.imgur.com/Png123.png')throw Error('Copied URL incorrect');
                    console.log('PASS native list UI: two ready URL-copy buttons, processing disabled; exact PNG URL copied');
                    // Posting overflow integration is checked separately by VerifyImgurPostingMenu.
                    // This uploads-screen fixture must not wrap the editor or add an upper toolbar.
                    retained.finish();cleanup();
                }catch(error){retained.finish();cleanup();throw error;}});return 'stop';},onComplete:function(){}
            });});},1500);
        });},1000);
    });
});
