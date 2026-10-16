// Isolated fixture ONLY. Replace every Imgur session/connection; NEVER delete real server media.
Java.perform(function () {
    Java.deoptimizeEverything();
    const app = Java.use('android.app.ActivityThread').currentApplication();
    if (String(app.getPackageName()) !== 'jp.co.airfront.android.a2chMate.haiagaru.imgurthumb191') throw Error('Thumbnail/delete fixture package required');
    const H = Java.use('app.morphe.extension.chmate.ImgurWebUpload');
    const Session = Java.use('app.morphe.extension.chmate.ImgurWebUpload$Session');
    const Page = Java.use('app.morphe.extension.chmate.ImgurWebUpload$UploadPage');
    const J = Java.use('java.lang.String');
    let deletes = [], mode = 'success', accountId = 123, cancel = false, finished = false;
    const payload = [
        {id:'First',title:'Fixture first',link:'https://i.imgur.com/First.png',account_id:123},
        {id:'Second',title:'Fixture second',link:'https://i.imgur.com/Second.mp4',processing:true},
        {id:'Third',title:'Fixture third',link:'https://i.imgur.com/Third.mp4',type:'video/mp4'},
        {id:'Foreign',account_id:456,link:'https://i.imgur.com/Foreign.png'}
    ];
    const Fake = Java.registerClass({
        name:'app.haiagaru.fixture.DeleteConnection' + Date.now(),
        superClass:Java.use('java.net.HttpURLConnection'),
        methods:{
            $init:[{returnType:'void',argumentTypes:['java.net.URL'],implementation:function(url){this.$super.$init(url);}}],
            connect:{returnType:'void',argumentTypes:[],implementation:function(){}},
            disconnect:{returnType:'void',argumentTypes:[],implementation:function(){}},
            usingProxy:{returnType:'boolean',argumentTypes:[],implementation:function(){return false;}},
            getResponseCode:{returnType:'int',argumentTypes:[],implementation:function(){return 200;}},
            getInputStream:{returnType:'java.io.InputStream',argumentTypes:[],implementation:function(){
                const path=String(this.getURL().getPath()), method=String(this.getRequestMethod());
                let data;
                if(path==='/3/account/me') data={id:accountId,url:'FixtureAccount'};
                else if(path.startsWith('/3/account/me/images/')) data=payload;
                else if(path.startsWith('/3/image/') && method==='DELETE') {
                    const id=path.substring('/3/image/'.length); deletes.push(id);
                    if(mode==='timeout' && id==='Second') throw Java.use('java.net.SocketTimeoutException').$new('Fixture timeout');
                    if(mode==='cancel') cancel=true;
                    data=mode==='false' ? false : true;
                } else throw Error('Unexpected fixture request (network never used)');
                return Java.use('java.io.ByteArrayInputStream').$new(J.$new(JSON.stringify({success:true,data:data})).getBytes('UTF-8'));
            }}
        }
    });
    H.session.implementation=function(){return Session.$new('FixtureClient','FixtureCookie','FixtureAgent');};
    H.open.implementation=function(url,session){return Fake.$new(Java.use('java.net.URL').$new(url));};
    const Thumbnail=Java.use('app.morphe.extension.chmate.ImgurThumbnailLoader');
    const previewRequests=[];
    Thumbnail.fetch.implementation=function(url){
        previewRequests.push(String(url));
        const bitmap=Java.use('android.graphics.Bitmap').createBitmap(96,64,Java.use('android.graphics.Bitmap$Config').ARGB_8888.value);
        bitmap.eraseColor(-16744320);return bitmap;
    };
    const Cancellation=Java.registerClass({
        name:'app.haiagaru.fixture.DeleteCancellation' + Date.now(),
        implements:[Java.use('app.morphe.extension.chmate.ImgurWebUpload$DeleteCancellation')],
        methods:{cancelled:{returnType:'boolean',argumentTypes:[],implementation:function(){return cancel;}}}
    });
    const cancellation=Cancellation.$new();
    function cleanup(){if(finished)return;finished=true;H.session.implementation=null;H.open.implementation=null;Thumbnail.fetch.implementation=null;console.log('Fixture cleaned; ZERO real uploads/deletes, previews or account requests');}
    function check(ok,label){if(!ok)throw Error(label);console.log('PASS '+label);}
    function ids(values){return Java.array('java.lang.String',values);}
    try {
        const page=H.uploadPage(0);
        check(page.rows.value.length()===3,'authenticated list filters foreign ownership');
        const result=H.deleteUploads(page,ids(['First','First','Second']),cancellation);
        check(result.deleted.value===2 && result.unconfirmed.value===0 && deletes.join(',')==='First,Second','deduplicated selected IDs use one DELETE each');
        deletes=[];
        for(const selected of [['Unknown'],['../First'],[]]) {
            let rejected=false;try{H.deleteUploads(page,ids(selected),cancellation);}catch(_){rejected=true;}
            check(rejected && deletes.length===0,'invalid/out-of-page/empty selection rejected before DELETE');
        }
        accountId=456;let rejected=false;
        try{H.deleteUploads(page,ids(['First']),cancellation);}catch(_){rejected=true;}
        check(rejected && deletes.length===0,'account switch aborts without DELETE');accountId=123;
        cancel=true;const cancelled=H.deleteUploads(page,ids(['First','Second']),cancellation);
        check(cancelled.deleted.value===0 && cancelled.notAttempted.value===2 && deletes.length===0,'cancellation before start prevents all DELETEs');cancel=false;
        mode='timeout';const partial=H.deleteUploads(page,ids(['First','Second','Third']),cancellation);
        check(partial.deleted.value===1 && partial.unconfirmed.value===1 && partial.notAttempted.value===1
            && deletes.join(',')==='First,Second','timeout stops batch; no retry and remaining media untouched');
        deletes=[];mode='false';const denied=H.deleteUploads(page,ids(['First','Second']),cancellation);
        check(denied.deleted.value===0 && denied.unconfirmed.value===1 && denied.notAttempted.value===1
            && deletes.join(',')==='First','false acknowledgement is not counted as deleted');
        deletes=[];mode='cancel';const stopped=H.deleteUploads(page,ids(['First','Second']),cancellation);
        check(stopped.deleted.value===1 && stopped.notAttempted.value===1 && deletes.join(',')==='First','cancellation between items stops subsequent DELETEs');
        deletes=[];mode='success';cancel=false;
    } catch(error){cleanup();throw error;}
    let activity=null;
    function later(action){setTimeout(function(){Java.scheduleOnMainThread(function(){try{action();}catch(error){if(activity)activity.finish();cleanup();throw error;}});},1200);}
    // Find actual visible dialogs: nested Java calls from the Frida main-thread callback
    // can bypass other hooks on that thread. Do not rely on a Dialog.show hook here.
    let dialog=null;
    function visibleDialog(){
        let found=null;
        Java.choose('android.app.AlertDialog',{onMatch:function(d){if(d.isShowing()){found=Java.retain(d);return 'stop';}},onComplete:function(){}});
        return found;
    }
    Java.scheduleOnMainThread(function(){
        const intent=Java.use('android.content.Intent').$new(app,Java.use('app.morphe.extension.chmate.ImgurUploadsActivity').class);
        intent.addFlags(0x10000000);app.startActivity(intent);
        later(function(){
            Java.choose('app.morphe.extension.chmate.ImgurUploadsActivity',{onMatch:function(a){if(!a.isFinishing() && !a.isDestroyed()){activity=Java.retain(a);return 'stop';}},onComplete:function(){}});
            if(!activity)throw Error('No uploads activity');
            function inspectList(attempt) {
            const group=activity.uploadsRows.value, Check=Java.use('android.widget.CheckBox'),Button=Java.use('android.widget.Button');
            let all=null,choices=[],deleteButton=null,previews=[],webViews=0;
            function visit(child){
                if(Check.class.isInstance(child)){const checkbox=Java.cast(child,Check);if(String(checkbox.getText()).startsWith('このページ'))all=checkbox;else choices.push(checkbox);}
                else if(Button.class.isInstance(child)){const b=Java.cast(child,Button);if(String(b.getText()).startsWith('選択した投稿'))deleteButton=b;}
                if(Java.use('android.widget.ImageView').class.isInstance(child))previews.push(Java.cast(child,Java.use('android.widget.ImageView')));
                if(Java.use('android.webkit.WebView').class.isInstance(child))webViews++;
                if(Java.use('android.view.ViewGroup').class.isInstance(child)){
                    const nested=Java.cast(child,Java.use('android.view.ViewGroup'));
                    for(let i=0;i<nested.getChildCount();i++)visit(nested.getChildAt(i));
                }
            }
            visit(activity.findViewById(16908290));
            if ((!all || choices.length!==3 || !deleteButton || previews.filter(p=>String(p.getContentDescription())==='投稿サムネイル').length<2)
                    && attempt<8) { later(function(){inspectList(attempt+1);}); return; }
            check(!!all && choices.length===3 && !!deleteButton && !deleteButton.isEnabled(),
                'per-item and select-all checkboxes; initial delete disabled (choices='+choices.length+', children='+group.getChildCount()+')');
            check(webViews===0,'uploads screen is independent of login and contains no WebView');
            check(previews.length===3 && previews.filter(p=>String(p.getContentDescription())==='投稿サムネイル').length===2,
                'image/video preview views rendered; pending item retains placeholder');
            check(previewRequests.includes('https://i.imgur.com/Firstm.jpg') && previewRequests.includes('https://i.imgur.com/Thirdm.jpg')
                && !previewRequests.includes('https://i.imgur.com/Secondm.jpg'),'preview requests use only ready static thumbnails');
            all.performClick();check(choices.every(c=>c.isChecked()) && String(deleteButton.getText()).includes('3件'),'select all includes pending media');
            choices[1].performClick();check(!all.isChecked() && String(deleteButton.getText()).includes('2件'),'individual toggle updates count and select-all state');
            console.log('Fixture delete click enabled='+deleteButton.isEnabled()+', hasListener='+deleteButton.hasOnClickListeners()+', clicked='+deleteButton.performClick()+', generation='+activity.uploadsGeneration.value);
            later(function(){
            dialog=visibleDialog();
            check(dialog!==null && deletes.length===0,'confirmation appears before any server deletion (dialog='+(dialog!==null)+', deletes='+deletes.length+')');
            dialog.getButton(-2).performClick();check(deletes.length===0,'confirmation cancel sends no DELETE');
            deleteButton.performClick();
            later(function(){dialog=visibleDialog();if(!dialog)throw Error('Second confirmation missing');dialog.getButton(-1).performClick();
            later(function(){
                check(deletes.join(',')==='First,Third','confirmed selection deletes only checked media');
                dialog=visibleDialog();if(!dialog)throw Error('Result dialog missing');
                const message=Java.cast(dialog.findViewById(16908299),Java.use('android.widget.TextView'));
                check(String(message.getText()).includes('削除完了：2件'),'result dialog reports confirmed deletion count');
                dialog.dismiss();activity.finish();cleanup();
            });
            });
            });
            }
            inspectList(0);
        });
    });
});
