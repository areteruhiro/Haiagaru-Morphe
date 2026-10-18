package app.morphe.extension.chmate;

import android.app.*;
import android.content.*;
import android.os.*;

/** A user-started search, independent of the search screen's lifecycle. */
public final class WacchoiSearchService extends Service {
    private static final String CHANNEL="haiagaru-wacchoi-search";
    private static final int NOTIFICATION=0x484704;
    static volatile WacchoiBackgroundJob current;
    private WacchoiBackgroundJob owned;
    private final Handler main=new Handler(Looper.getMainLooper());
    static void launch(android.app.Activity activity,Intent args) {
        args.setClass(activity,WacchoiSearchService.class);
        if(Build.VERSION.SDK_INT>=26)activity.startForegroundService(args);else activity.startService(args);
    }
    @Override public int onStartCommand(Intent intent,int flags,int startId) {
        if(intent==null){stopSelf();return START_NOT_STICKY;}
        if("stop".equals(intent.getAction())){if(current!=null)current.cancel();stopSelf();return START_NOT_STICKY;}
        if(current!=null && current.running){owned=current;foreground();return START_NOT_STICKY;}
        if("resume".equals(intent.getAction()) && current!=null && current.canResume()) {
            owned=current;owned.setListener(()->main.post(this::update));foreground();owned.resume();return START_NOT_STICKY;
        }
        if(current!=null)current.release();
        owned=new WacchoiBackgroundJob(this,()->main.post(this::update));current=owned;
        owned.rememberArguments(intent);foreground();owned.start(intent);return START_NOT_STICKY;
    }
    private void foreground(){
        NotificationManager manager=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        if(Build.VERSION.SDK_INT>=26)manager.createNotificationChannel(new NotificationChannel(CHANNEL,"ワッチョイ4桁検索",NotificationManager.IMPORTANCE_LOW));
        if(Build.VERSION.SDK_INT>=29)startForeground(NOTIFICATION,notification(),android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
        else startForeground(NOTIFICATION,notification());
    }
    private Notification notification(){
        int flags=PendingIntent.FLAG_UPDATE_CURRENT;
        if(Build.VERSION.SDK_INT>=23)flags|=PendingIntent.FLAG_IMMUTABLE;
        Intent returnToSearch=WacchoiSearchState.notificationArguments(owned==null?null:owned.searchArguments())
                .setClass(this,HissiWacchoiSearchActivity.class);
        PendingIntent open=PendingIntent.getActivity(this,NOTIFICATION,returnToSearch,flags);
        PendingIntent stop=PendingIntent.getService(this,NOTIFICATION,new Intent(this,WacchoiSearchService.class).setAction("stop"),flags);
        Notification.Builder builder=Build.VERSION.SDK_INT>=26?new Notification.Builder(this,CHANNEL):new Notification.Builder(this);
        boolean running=owned==null||owned.running;
        return builder.setSmallIcon(android.R.drawable.ic_menu_search).setContentTitle(running?"ワッチョイ4桁を検索中":"ワッチョイ4桁検索の結果")
                .setContentText(owned==null?"開始しています":owned.status).setContentIntent(open)
                .setOngoing(running).setAutoCancel(!running).addAction(android.R.drawable.ic_menu_close_clear_cancel,"停止",stop).build();
    }
    private void update(){
        if(owned==null)return;
        if(owned.running){((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).notify(NOTIFICATION,notification());}
        else {stopForeground(true);((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).notify(NOTIFICATION,notification());stopSelf();}
    }
    @Override public void onTimeout(int startId,int fgsType){if(owned!=null)owned.cancel();stopForeground(true);stopSelf();}
    @Override public void onDestroy(){
        main.removeCallbacksAndMessages(null);
        if(owned!=null && owned.running)owned.cancel();
        if(owned!=null)owned.setListener(()->{});
        super.onDestroy();
    }
    @Override public IBinder onBind(Intent intent){return null;}
}
