package app.morphe.extension.chmate;

import android.content.*;
import java.lang.reflect.*;
import java.util.*;

/** Synthetic native row contracts only; no live model/DB mutation. */
public final class PopularModesAndroidTest {
 static int checks;
 static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);checks++;}
 static Field field(Class<?> c,String n)throws Exception{Field f=c.getDeclaredField(n);f.setAccessible(true);return f;}
 static Object alloc(Class<?> c)throws Exception{Class<?> u=Class.forName("sun.misc.Unsafe");Field f=u.getDeclaredField("theUnsafe");f.setAccessible(true);return u.getMethod("allocateInstance",Class.class).invoke(f.get(null),c);}
 static Object call(Class<?> c,String n,Class<?>[] types,Object...args)throws Exception{Method m=c.getDeclaredMethod(n,types);m.setAccessible(true);return m.invoke(null,args);}
 public static String run(Context app)throws Exception {
  checks=0;String version=app.getPackageManager().getPackageInfo(app.getPackageName(),0).versionName;boolean old=version.contains("191"),middle=version.contains("226");ClassLoader loader=app.getClassLoader();
  Class<?> popular=Class.forName("app.morphe.extension.chmate.PopularReplies",true,loader),nativeRows=Class.forName("app.morphe.extension.chmate.PopularNativeRows",true,loader);
  SharedPreferences p=app.getSharedPreferences("io.github.areteruhiro.chmate.haiagaru.ui-config",0);Map<String,?> saved=p.getAll();String[] keys={"popularReplyPopup","popularReplyDirect","popularReplyThreshold","popularReplyStandardThreshold"};
  List snapshots=(List)field(nativeRows,"snapshots").get(null);List previous; synchronized(snapshots){previous=new ArrayList(snapshots);}
  try {
   p.edit().remove(keys[1]).putBoolean(keys[0],true).commit();
   check((Boolean)call(popular,"directFor",new Class<?>[]{Context.class},app),"legacy combined ON migrates to direct mode");
   p.edit().putBoolean(keys[0],false).commit();check((Boolean)call(popular,"directFor",new Class<?>[]{Context.class},app),"popup OFF does not disable migrated direct mode");
   for(boolean direct:new boolean[]{false,true})for(boolean popup:new boolean[]{false,true}){
    p.edit().putBoolean(keys[0],popup).putBoolean(keys[1],direct).commit();
    check((Boolean)call(popular,"directFor",new Class<?>[]{Context.class},app)==direct,"independent direct "+direct+"/"+popup);
    check((Boolean)call(popular,"enabledFor",new Class<?>[]{Context.class},app)==popup,"independent popup "+direct+"/"+popup);
   }
   Class<?> response=Class.forName(old?"o.processAdDisplayErrorPostbackForUserError":middle?"o.BouncyCastleSocketAdapterCompanion":"o.KeJ11",true,loader);
   Class<?> row=Class.forName(old?"o.r8lambdakfayx3RllpZib32UTPpeXfQjw":middle?"o.setSocketokhttp":"o.zzadi$_init_lambda2",true,loader);
   List<Object> source=new ArrayList<>(),rows=new ArrayList<>();
   for(int no=1;no<=105;no++){
    Object r=alloc(response);field(response,middle?"l":"o").setInt(r,no);field(response,old?"r":middle?"p":"s").setInt(r,no==1?3:0);source.add(r);
    Object item=alloc(row);field(row,old||middle?"j":"o").set(item,r);int depth=no==1||no==104?0:1;
    if(old)field(row,"i").setShort(item,(short)depth);else field(row,middle?"h":"j").setInt(item,depth);rows.add(item);
   }
   Object adapter=null,scope=null,config=null;
   if(old||middle){
    Class<?> type=Class.forName(old?"o.m9ExternalSyntheticLambda1":"o.listener",true,loader);adapter=alloc(type);field(type,old?"G":"M").setInt(adapter,1);field(type,old?"I":"D").setInt(adapter,3);
    call(nativeRows,"captureLegacy",new Class<?>[]{Object.class,List.class},adapter,rows);
   }else{
    Class<?> ct=Class.forName("o.IObjectWrapperStub",true,loader),st=Class.forName("o.ScopeUtil",true,loader);config=alloc(ct);scope=alloc(st);field(ct,"b").setInt(config,3);field(st,"a").setBoolean(scope,false);
    call(nativeRows,"config",new Class<?>[]{Object.class},config);call(nativeRows,"mode",new Class<?>[]{Object.class},Class.forName("o.zzacn",true,loader).getMethod("valueOf",String.class).invoke(null,"Tree"));call(nativeRows,"captureModern",new Class<?>[]{List.class,Object.class},rows,scope);
   }
   Set<?> selected=(Set<?>)call(nativeRows,"select",new Class<?>[]{List.class,int.class},source,3);
   check(selected.size()==103,"native tree returns all 103 matching responses, not one root");check(!selected.contains(104)&&!selected.contains(105),"small conversation excluded");
   if(old||middle){field(adapter.getClass(),old?"G":"M").setInt(adapter,0);call(nativeRows,"captureLegacy",new Class<?>[]{Object.class,List.class},adapter,rows);}
   else{
    field(scope.getClass(),"a").setBoolean(scope,true);call(nativeRows,"config",new Class<?>[]{Object.class},config);call(nativeRows,"mode",new Class<?>[]{Object.class},Class.forName("o.zzacn",true,loader).getMethod("valueOf",String.class).invoke(null,"Tree"));call(nativeRows,"captureModern",new Class<?>[]{List.class,Object.class},rows,scope);
    check(((Set<?>)call(nativeRows,"select",new Class<?>[]{List.class,int.class},source,3)).size()==103,"filter selection must not change Tree/Normal display mode");
    call(nativeRows,"config",new Class<?>[]{Object.class},config);call(nativeRows,"mode",new Class<?>[]{Object.class},Class.forName("o.zzacn",true,loader).getMethod("valueOf",String.class).invoke(null,"Normal"));call(nativeRows,"captureModern",new Class<?>[]{List.class,Object.class},rows,scope);
   }
   selected=(Set<?>)call(nativeRows,"select",new Class<?>[]{List.class,int.class},source,3);check(selected.size()==1&&selected.contains(1),"native flat mode counts qualifying roots only");
   p.edit().putBoolean(keys[1],false).putInt(keys[3],5).putInt(keys[2],3).commit();check((Integer)call(popular,"selectThreshold",new Class<?>[]{int.class},7)==5,"standard threshold independent");
   p.edit().putBoolean(keys[1],true).commit();check((Integer)call(popular,"selectThreshold",new Class<?>[]{int.class},7)==3,"direct threshold independent");
   check(rows.size()==105&&source.size()==105,"source rows retained");
   return "PASS "+version+" "+checks+" popularity mode/native row checks";
  }finally{
   SharedPreferences.Editor e=p.edit();for(String k:keys){Object v=saved.get(k);if(v instanceof Boolean)e.putBoolean(k,(Boolean)v);else if(v instanceof Integer)e.putInt(k,(Integer)v);else e.remove(k);}e.commit();
   synchronized(snapshots){snapshots.clear();snapshots.addAll(previous);}
  }
 }
}
