import java.lang.reflect.*;
import java.util.*;

/** Native 226 HOME adapter test, no app/database/network and no user preferences. */
public final class VerifyHomeKeywords226Native {
 static int checks;
 static void check(boolean ok,String label){if(!ok)throw new AssertionError(label);checks++;System.out.println("PASS "+label);}
 static Object allocate(Class<?> type)throws Exception{
  Class<?> unsafe=Class.forName("sun.misc.Unsafe");Field f=unsafe.getDeclaredField("theUnsafe");f.setAccessible(true);
  return unsafe.getMethod("allocateInstance",Class.class).invoke(f.get(null),type);
 }
 static Field field(Class<?> type,String name)throws Exception{Field f=type.getDeclaredField(name);f.setAccessible(true);return f;}
 static Object row(String title)throws Exception{Class<?> type=Class.forName("o.Yy2$RemoteActionCompatParcelizer");Object row=allocate(type);field(type,"e").set(row,title);return row;}
 public static void main(String[] args){try{run();}catch(Throwable e){e.printStackTrace(System.out);System.exit(1);}}
 static void run()throws Exception{
  android.os.Looper.prepareMainLooper();
  Class<?> type=Class.forName("o.bea1");Object adapter=allocate(type);
  field(android.widget.BaseAdapter.class,"mDataSetObservable").set(adapter,new android.database.DataSetObservable());
  Class<?> rows=Class.forName("o.Yy2$CheckResult");
  @SuppressWarnings("unchecked") List<Object> source=(List<Object>)rows.getConstructor(int.class).newInstance(2);
  Object mate=row("ChMate example"),other=row("Other topic");source.add(mate);source.add(other);
  Class<?> keyword=Class.forName("jp.syoboi.a2chMate.data.Keyword");Object rule=keyword.getConstructor(long.class,int.class,String.class).newInstance(1L,8,"ChMate");
  List<Object> rules=new ArrayList<>();rules.add(rule);
  field(type,"g").set(adapter,source);field(type,"h").set(adapter,rules);field(type,"i").setBoolean(adapter,true);
  Method redraw=type.getMethod("b");
  for(int i=0;i<5;i++){
   redraw.invoke(adapter);
   check(field(type,"h").get(adapter)==rules,"redraw "+i+" retains keyword list");
   check(field(type,"i").getBoolean(adapter),"redraw "+i+" retains grouping mode");
   List<?> result=(List<?>)field(type,"d").get(adapter);
   check(result.size()==4&&result.get(1)==mate&&result.get(3)==other,"redraw "+i+" retains keyword groups and rows");
  }
  field(type,"h").set(adapter,Collections.emptyList());redraw.invoke(adapter);
  check(((List<?>)field(type,"d").get(adapter)).equals(source),"removing keywords clears groups");
  field(type,"h").set(adapter,rules);field(type,"i").setBoolean(adapter,false);redraw.invoke(adapter);
  check(((List<?>)field(type,"d").get(adapter)).equals(source),"grouping OFF retains normal rows");
  check(source.size()==2&&source.get(0)==mate,"source rows unchanged");
  System.out.println("PASS "+checks+" native 226 HOME keyword redraw checks");System.exit(0);
 }
}
