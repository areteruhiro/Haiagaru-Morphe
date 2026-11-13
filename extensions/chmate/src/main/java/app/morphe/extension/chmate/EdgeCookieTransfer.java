package app.morphe.extension.chmate;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.LinearLayout;
import java.io.IOException;
import java.util.Map;

/** Explicit, in-memory capture followed by user-confirmed import into the native cookie store. */
final class EdgeCookieTransfer {
    private static EdgeCookieReceiver active;
    private EdgeCookieTransfer(){}
    static void addSettingsButton(LinearLayout layout,Activity activity){
        Button button=new Button(activity);button.setAllCaps(false);button.setText("通常版ChMateからエッヂのCookieを移行");
        layout.addView(button);button.setOnClickListener(v->start(activity));
    }
    private static synchronized void start(Activity activity){
        if(active!=null){message(activity,"Cookie移行","既に受信画面が開いています。その画面を閉じてから再試行してください。");return;}
        if(ExternalBoardAuth.nativeJar()==null){message(activity,"移行前の準備","先にHaiagaruでエッヂの板を一度開き、Cookieの保存先を準備してください。その後、この設定へ戻ってください。");return;}
        try{active=new EdgeCookieReceiver();}catch(IOException error){message(activity,"Cookie移行","受信を開始できませんでした。少し待って再試行してください。");return;}
        EdgeCookieReceiver receiver=active;
        String instructions="1. 通常版ChMateの現在のProxy設定を控えます。\n"
                +"2. 通常版の「設定 → 実験的 → Proxyを使用する」をオンにし、Proxyを次に変更します。\n\n127.0.0.1:"+receiver.port()+"\n\n"
                +"3. 通常版でエッヂの板かスレを更新し、Haiagaruへ戻ります。移行中は通信エラーになります。\n"
                +"4. 受信後に「内容を確認」を押します。終了後は必ず通常版のProxy設定を元に戻してください。\n\n"
                +"待受は5分間です。通信は掲示板へ転送せず、投稿本文も取得しません。HTTPS通信やHTTPへ送られないCookieは取得できません。通常版が別端末の場合は利用できません。";
        HaiagaruDialog dialog=new HaiagaruDialog.Builder(activity).setTitle("エッヂCookieの移行・受信中")
                .setMessage(instructions).setNegativeButton("受信を終了",null).setPositiveButton("内容を確認",null).create();
        Handler handler=new Handler(Looper.getMainLooper());
        boolean[] busy={false};
        dialog.setOnDismissListener(d->{receiver.close();synchronized(EdgeCookieTransfer.class){if(active==receiver)active=null;} messageIfAlive(activity);});
        dialog.setOnShowListener(d->{
            dialog.getButton(-1).setEnabled(false);
            dialog.getButton(-1).setOnClickListener(v->{
                Map<String,String> snapshot=receiver.snapshot();if(snapshot==null)return;
                if(busy[0])return;busy[0]=true;
                confirm(activity,receiver,snapshot,dialog,busy);
            });
        });
        dialog.show();receiver.start();
        handler.post(new Runnable(){public void run(){
            if(activity.isDestroyed()||activity.isFinishing()){dialog.dismiss();return;}
            if(!dialog.isShowing())return;
            dialog.setMessage(instructions+"\n\n状態: "+receiver.status());
            dialog.getButton(-1).setEnabled(!busy[0]&&receiver.snapshot()!=null);
            if(receiver.expired()){receiver.close();return;}
            handler.postDelayed(this,750);
        }});
    }
    private static void confirm(Activity activity,EdgeCookieReceiver receiver,Map<String,String> cookies,HaiagaruDialog waiting,boolean[] busy){
        waiting.getButton(-1).setEnabled(false);
        boolean[] importing={false};
        String names=String.join("、",cookies.keySet());
        HaiagaruDialog confirmation=new HaiagaruDialog.Builder(activity).setTitle("受信したCookieを移植")
            .setMessage("対象: https://bbs.eddibb.cc/liveedge/\nCookie名: "+names+"\n\n"
                +"同名の既存Cookieを受信した値へ置き換えます。板別Cookieが有効ならエッヂの板だけに保存します。\n"
                +"元の有効期限・Domain・Pathは通信ヘッダーから取得できないため、同一ホスト・Path=/のCookieとして保持します。再起動後も保存しますが、サイト側の失効や接続元の制限は引き継げません。閲覧用のCloudflare Cookieは対象外です。")
            .setNegativeButton("戻る",null).setPositiveButton("このCookieを移植",(d,w)->{
                if(receiver.snapshot()==null){message(activity,"Cookie移行","受信内容の有効期限が切れました。受信をやり直してください。");return;}
                importing[0]=true;
                waiting.getButton(-1).setEnabled(false);
                new Thread(()->{
                    boolean success=false;
                    try{ExternalBoardAuth.importTransferredCookies(cookies);success=true;}
                    catch(Exception ignored){}
                    boolean done=success;
                    activity.runOnUiThread(()->{
                        if(activity.isFinishing()||activity.isDestroyed()){receiver.close();return;}
                        if(done){waiting.dismiss();message(activity,"Cookieを移植しました","通常版ChMateのProxy設定を元に戻してください。その後Haiagaruでエッヂを開けます。実際の認証状態や投稿の成功はサイト側の判定によります。");}
                        else{busy[0]=false;waiting.getButton(-1).setEnabled(receiver.snapshot()!=null);message(activity,"Cookieを移植できませんでした","認証画面や通信を閉じてから再試行してください。Haiagaruでエッヂを一度開くと、保存先が準備されます。");}
                    });
                },"Haiagaru-edge-cookie-import").start();
            }).create();
        confirmation.setOnDismissListener(d->{if(!importing[0])busy[0]=false;if(waiting.isShowing())waiting.getButton(-1).setEnabled(!busy[0]&&receiver.snapshot()!=null);});
        confirmation.show();
    }
    private static void messageIfAlive(Activity a){if(!a.isFinishing()&&!a.isDestroyed())android.widget.Toast.makeText(a,"通常版ChMateのProxy設定を元に戻してください",android.widget.Toast.LENGTH_LONG).show();}
    private static void message(Activity a,String title,String text){new HaiagaruDialog.Builder(a).setTitle(title).setMessage(text).setPositiveButton("閉じる",null).show();}
}
