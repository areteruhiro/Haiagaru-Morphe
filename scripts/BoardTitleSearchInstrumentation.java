package app.haiagaru.tests.opentabs;
import android.app.*;import android.content.*;import android.os.*;import android.view.*;import java.lang.reflect.*;import java.util.concurrent.*;

/** Exercise the actual 226 native board menu inflater and the guarded addition. */
public final class BoardTitleSearchInstrumentation extends Instrumentation {
 private int checks;private void ok(boolean value,String text){if(!value)throw new AssertionError(text);checks++;}
 @Override public void onCreate(Bundle args){super.onCreate(args);start();}
 @Override public void onStart(){Bundle out=new Bundle();try{
  Context app=getTargetContext();ClassLoader loader=app.getClassLoader();
  app.startActivity(new Intent().setClassName(app.getPackageName(),"jp.syoboi.a2chMate.activity.HomeActivity").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));Thread.sleep(1500);
  FutureTask<Void> task=new FutureTask<>(()->{
   Class<?> extension=loader.loadClass("app.morphe.extension.chmate.Haiagaru");Method activity=extension.getDeclaredMethod("toolbarActivity",Object.class);activity.setAccessible(true);Activity host=(Activity)activity.invoke(null,new Object[]{null});ok(host!=null,"resumed host");
   Object board=loader.loadClass("o.Yhp5").getConstructor().newInstance();
   Menu menu=(Menu)loader.loadClass("o.getStream").getConstructor(Context.class,byte.class).newInstance(host,(byte)0);
   Method inflate=board.getClass().getMethod("b",Menu.class,MenuInflater.class);inflate.invoke(board,menu,new MenuInflater(host));
   MenuItem search=menu.findItem(0x7f090100);ok(search!=null,"Search present in board title menu");ok(search.getTitle().toString().equals("検索"),"native Search label");ok(search.getIcon()!=null,"Search icon");
   inflate.invoke(board,menu,new MenuInflater(host));int count=0;for(int n=0;n<menu.size();n++)if(menu.getItem(n).getItemId()==0x7f090100)count++;ok(count==1,"no duplicate Search when menu already contains it");return null;
  });runOnMainSync(task);task.get(20,TimeUnit.SECONDS);out.putString("result","PASS "+checks+" actual 226 board menu checks");finish(Activity.RESULT_OK,out);
 }catch(Throwable error){out.putString("error",android.util.Log.getStackTraceString(error));finish(Activity.RESULT_CANCELED,out);}}
}
