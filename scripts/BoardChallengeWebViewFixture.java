package app.morphe.extension.chmate;

import android.app.Activity;
import android.content.Intent;
import java.lang.reflect.Method;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;

/** Offline arithmetic/DOM test only. No site proof, CAPTCHA, account or real post is used. */
public final class BoardChallengeWebViewFixture {
    public static final String BOARD="https://fixture.invalid/board/";
    public static String token;
    public static String open(Activity activity)throws Exception {
        if(!activity.getPackageName().endsWith(".settingsfix191"))throw new AssertionError("Verification package only");
        String html="<!doctype html><html><head><meta http-equiv='Content-Type' content='text/html;charset=Shift_JIS'><title>Captcha認証</title></head>"
            +"<body><h2>通信しないオフライン表示テスト</h2><p>実際の掲示板には送信しません。</p>"
            +"<form method='POST' action='/test/bbs.cgi' id='form'><input type='hidden' name='MESSAGE' value='SYNTHETIC_DRAFT_ONLY'>"
            +"<button type='button' id='compute'>Workerの計算テスト</button><button id='confirm'>送信せずDOM確認</button></form><p id='status'>待機中</p>"
            +"<script>window.fixtureReady=true;window.starts=0;window.submits=0;window.workerValue=0;window.blockedPost=0;"
            +"document.getElementById('compute').onclick=function(){window.starts++;"
            +"const url=URL.createObjectURL(new Blob(['onmessage=function(e){postMessage(e.data*2)}'],{type:'text/javascript'}));"
            +"const w=new Worker(url);w.onmessage=function(e){window.workerValue=e.data;document.getElementById('status').textContent='Worker結果 '+e.data;w.terminate();URL.revokeObjectURL(url)};w.postMessage(7)};"
            +"document.getElementById('form').onsubmit=function(e){e.preventDefault();window.submits++;"
            +"fetch('/test/bbs.cgi?fixture=1',{method:'POST',body:'OFFLINE_FIXTURE_ONLY'}).then(r=>r.text()).then(t=>window.blockedPost=t.includes('送信を停止')?1:-1).catch(e=>window.blockedPost=-2);return false};</script></body></html>";
        ClassLoader loader=activity.getClassLoader();
        Class<?> sessions=Class.forName("app.morphe.extension.chmate.BoardChallengeSession",true,loader);
        Method remember=sessions.getDeclaredMethod("remember",String.class,String.class);remember.setAccessible(true);
        token=(String)remember.invoke(null,BOARD,html);
        if(token.isEmpty())throw new AssertionError("Fixture page rejected");
        activity.startActivity(new Intent(activity,Class.forName("app.morphe.extension.chmate.ExternalBoardAuthActivity",true,loader))
            .putExtra("board",BOARD).putExtra("challenge",token));
        return "STARTED offline Web Worker fixture";
    }
    public static boolean forgotten(Activity activity)throws Exception {
        Class<?> sessions=Class.forName("app.morphe.extension.chmate.BoardChallengeSession",true,activity.getClassLoader());
        Method find=sessions.getDeclaredMethod("find",String.class,String.class);find.setAccessible(true);
        return find.invoke(null,token,BOARD)==null;
    }
    public static int networkChecks(Activity activity)throws Exception {
        Class<?> type=activity.getClass();
        java.lang.reflect.Field field=type.getDeclaredField("web");field.setAccessible(true);
        WebView web=(WebView)field.get(activity);
        field=type.getDeclaredField("sendAllowed");field.setAccessible(true);
        java.util.concurrent.atomic.AtomicBoolean permission=(java.util.concurrent.atomic.AtomicBoolean)field.get(activity);
        boolean previous=permission.get();
        try {
            if(previous)throw new AssertionError("Sending must default to OFF");
            if(web.getWebViewClient().shouldInterceptRequest(web,new Request("https://fixture.invalid/test/bbs.cgi",true))==null)
                throw new AssertionError("Unapproved main-frame POST permitted");
            if(web.getWebViewClient().shouldInterceptRequest(web,new Request("https://fixture.invalid/test/bbs.cgi",false))==null)
                throw new AssertionError("Unapproved script POST permitted");
            permission.set(true);
            if(web.getWebViewClient().shouldInterceptRequest(web,new Request("https://fixture.invalid/test/bbs.cgi",true))!=null)
                throw new AssertionError("Approved same-origin POST blocked");
            if(web.getWebViewClient().shouldInterceptRequest(web,new Request("https://outside.invalid/test/bbs.cgi",true))==null)
                throw new AssertionError("Cross-origin POST permitted");
            return 4;
        } finally { permission.set(previous); }
    }
    static final class Request implements WebResourceRequest {
        final android.net.Uri url; final boolean main;
        Request(String url,boolean main){this.url=android.net.Uri.parse(url);this.main=main;}
        public android.net.Uri getUrl(){return url;}
        public boolean isForMainFrame(){return main;}
        public boolean isRedirect(){return false;}
        public boolean hasGesture(){return true;}
        public String getMethod(){return "POST";}
        public java.util.Map<String,String> getRequestHeaders(){return java.util.Collections.emptyMap();}
    }
}
