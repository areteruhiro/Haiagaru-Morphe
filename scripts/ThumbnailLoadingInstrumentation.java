package app.haiagaru.tests.thumbnail242;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.os.*;
import android.view.*;
import java.lang.reflect.*;
import java.util.concurrent.*;

/** Isolated 242 regression: exercise real native loading callbacks without ART hooks/network. */
public final class ThumbnailLoadingInstrumentation extends Instrumentation {
 private boolean baseline; private int checks;
 private Field field(Class<?> c,String name)throws Exception{Field f=c.getDeclaredField(name);f.setAccessible(true);return f;}
 private void check(boolean value,String label){if(!value)throw new AssertionError(label);checks++;}
 @Override public void onCreate(Bundle args){super.onCreate(args);baseline=args!=null&&"true".equals(args.getString("baseline"));start();}
 @Override public void onStart(){Bundle result=new Bundle();try{
  Context app=getTargetContext();Intent launch=app.getPackageManager().getLaunchIntentForPackage(app.getPackageName());
  launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);Activity activity=startActivitySync(launch);waitForIdleSync();
  FutureTask<Void> test=new FutureTask<>(()->{runChecks(activity);return null;});runOnMainSync(test);test.get(15,TimeUnit.SECONDS);
  result.putString("result","PASS "+checks+" native thumbnail checks; baseline="+baseline);finish(Activity.RESULT_OK,result);
 }catch(Throwable error){result.putString("error",android.util.Log.getStackTraceString(error));finish(Activity.RESULT_CANCELED,result);}}
 private void runChecks(Activity activity)throws Exception{
  ClassLoader loader=activity.getClassLoader();Class<?> image=loader.loadClass("o.zzbes"),bitmap=loader.loadClass("o.VN22"),state=loader.loadClass("o.zzbes$write");
  Object view=image.getConstructor(Context.class).newInstance(activity);
  Bitmap pixels=Bitmap.createBitmap(16,16,Bitmap.Config.ARGB_8888);pixels.eraseColor(Color.GREEN);
  String url="https://example.invalid/haiagaru-thumbnail.png";
  Object decoded=bitmap.getConstructor(Bitmap.class,String.class).newInstance(pixels,url);
  Field held=field(image,"a"),byUser=field(image,"mDownloadByUser");
  // onPreExecute only needs its owning ImageView; the task/URL network path is never run.
  Class<?> callback=loader.loadClass("o.zzbes$_init_lambda1"),unsafeClass=Class.forName("sun.misc.Unsafe");
  Object unsafe=field(unsafeClass,"theUnsafe").get(null);
  Object cb=unsafeClass.getMethod("allocateInstance",Class.class).invoke(unsafe,callback);field(callback,"b").set(cb,view);
  Method before=callback.getDeclaredMethod("e");before.setAccessible(true);
  held.set(view,decoded);byUser.setBoolean(view,false);before.invoke(cb);
  check(baseline?held.get(view)==null:held.get(view)==decoded,"automatic onPreExecute bitmap retention");
  if(baseline)return;
  for(int i=0;i<100;i++){before.invoke(cb);check(held.get(view)==decoded,"repeated loading notification "+i);}
  check(!pixels.isRecycled()&&pixels.getPixel(0,0)==Color.GREEN,"decoded pixels retained through loading notifications");
  byUser.setBoolean(view,true);before.invoke(cb);check(held.get(view)==null,"manual retry clears old bitmap");
  byUser.setBoolean(view,false);before.invoke(cb);check(held.get(view)==null,"initial load remains native");
  Method setState=image.getDeclaredMethod("c",state);setState.setAccessible(true);
  held.set(view,decoded);Object error=loader.loadClass("o.zzbes$write$read").getConstructor(String.class).newInstance("fixture error");
  setState.invoke(view,error);check(held.get(view)==null,"completed error clears old bitmap");
  held.set(view,decoded);image.getMethod("setImageUrl",String.class).invoke(view,url+"?changed");
  check(held.get(view)==null,"changed URL clears old bitmap");
 }
}
