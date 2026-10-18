package app.morphe.extension.chmate;

import android.app.Service;
import android.content.*;
import android.os.*;
import android.os.Process;
import java.io.*;
import java.util.concurrent.*;

/** Same-UID, non-exported decoder process contains native media crashes. */
public final class VideoFrameDecodeService extends Service {
    public static boolean decoderProcess(Context context){
        String name=null;
        if(Build.VERSION.SDK_INT>=28)name=android.app.Application.getProcessName();
        else try{android.app.ActivityManager manager=(android.app.ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);for(android.app.ActivityManager.RunningAppProcessInfo process:manager.getRunningAppProcesses())if(process.pid==Process.myPid()){name=process.processName;break;}}catch(RuntimeException ignored){}
        return name!=null&&name.equals(context.getPackageName()+":haiagaru_video_decoder");
    }
    static boolean blocked(Context context){
        SharedPreferences health=context.getSharedPreferences("haiagaru-video-decoder-health",0);
        if(health.getInt("decoderRevision",0)!=2){health.edit().putBoolean("unsafe",false).putInt("decoderRevision",2).commit();return false;}
        if(!health.getBoolean("unsafe",false))return false;
        // Native framework state can recover after reboot. Never permanently disable previews.
        long boot=System.currentTimeMillis()-SystemClock.elapsedRealtime();
        long previous=health.getLong("boot",0);
        if(previous==0||Math.abs(boot-previous)>120000){health.edit().putBoolean("unsafe",false).putLong("boot",boot).commit();return false;}
        return true;
    }
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final Messenger messenger=new Messenger(new Handler(Looper.getMainLooper(),message->{
        Bundle data=message.getData();Messenger reply=message.replyTo;
        worker.execute(()->{boolean ok=false;try{
            File root=new File(getCacheDir(),"haiagaru-video-frames").getCanonicalFile();
            File input=new File(data.getString("input")).getCanonicalFile(),output=new File(data.getString("output")).getCanonicalFile();
            if(!root.equals(input.getParentFile())||!root.equals(output.getParentFile())||!input.getName().startsWith("frame-")||!output.getName().startsWith("preview-"))throw new IOException("Invalid decoder path");
            ok=VideoFrameThumbnail.decodeFile(input,output,data.getBoolean("four"),data.getInt("minimum"),data.getInt("count"));
        }catch(Exception|OutOfMemoryError error){android.util.Log.w("HaiagaruMedia","Decoder failed",error);}
        try{Message result=Message.obtain();result.arg1=ok?1:0;reply.send(result);}catch(Exception ignored){}
        });return true;
    }));
    @Override public IBinder onBind(Intent intent){return messenger.getBinder();}
    @Override public void onDestroy(){worker.shutdownNow();super.onDestroy();}

    static boolean decode(Context context,File input,File output,boolean four,int minimum,int count)throws Exception{
        SharedPreferences health=context.getSharedPreferences("haiagaru-video-decoder-health",0);
        // A pending request surviving process death means a crash/timeout, not permission to retry indefinitely.
        if(blocked(context)){android.util.Log.w("HaiagaruMedia","Native decoder unavailable until reboot; keeping original poster/placeholder");return false;}
        if(!health.edit().putBoolean("unsafe",true).putLong("boot",System.currentTimeMillis()-SystemClock.elapsedRealtime()).commit())return false;
        CountDownLatch done=new CountDownLatch(1);boolean[] result={false},responded={false};
        Messenger receiver=new Messenger(new Handler(Looper.getMainLooper(),message->{result[0]=message.arg1==1;responded[0]=true;done.countDown();return true;}));
        ServiceConnection connection=new ServiceConnection(){
            public void onServiceConnected(ComponentName name,IBinder binder){try{Message request=Message.obtain();Bundle data=new Bundle();data.putString("input",input.getAbsolutePath());data.putString("output",output.getAbsolutePath());data.putBoolean("four",four);data.putInt("minimum",minimum);data.putInt("count",count);request.setData(data);request.replyTo=receiver;new Messenger(binder).send(request);}catch(Exception error){done.countDown();}}
            public void onServiceDisconnected(ComponentName name){done.countDown();}
            public void onBindingDied(ComponentName name){done.countDown();}
            public void onNullBinding(ComponentName name){done.countDown();}
        };
        boolean bound=false;
        try{
            bound=context.bindService(new Intent(context,VideoFrameDecodeService.class),connection,Context.BIND_AUTO_CREATE);
            if(!bound){health.edit().putBoolean("unsafe",false).commit();return false;}
            if(!done.await(45,TimeUnit.SECONDS)){android.util.Log.w("HaiagaruMedia","Decoder timed out; further attempts disabled");return false;}
            if(responded[0])health.edit().putBoolean("unsafe",false).commit();
            else android.util.Log.w("HaiagaruMedia","Decoder process died; further attempts disabled");
            return result[0]&&output.isFile()&&output.length()>0;
        }finally{if(bound)context.unbindService(connection);}
    }
}
