package app.haiagaru.tests.backupng;

import android.app.*;import android.content.*;import android.os.*;import java.io.*;import java.lang.reflect.*;import java.util.*;import java.util.zip.*;

/** Dedicated preference namespaces and cache files; never restore into user settings. */
public final class CanonicalBackupNgInstrumentation extends Instrumentation {
 private int checks;private Context app;private ClassLoader loader;private File root;private FixtureApplication fixture;
 private static final String ORIGINAL="jp.co.airfront.android.a2chMate_preferences";
 private void ok(boolean value,String label){checks++;if(!value)throw new AssertionError(label);}
 private Method method(Class<?> c,String name,Class<?>...types)throws Exception{Method m=c.getDeclaredMethod(name,types);m.setAccessible(true);return m;}
 public static final class FixtureApplication extends Application {
  Context base;String prefix;final Set<String> stores=new HashSet<>();
  @Override public String getPackageName(){return base.getPackageName();}
  @Override public SharedPreferences getSharedPreferences(String name,int mode){stores.add(prefix+name);return base.getSharedPreferences(prefix+name,mode);}
 }
 private void write(File file,String text)throws Exception{file.getParentFile().mkdirs();try(OutputStream out=new FileOutputStream(file)){out.write(text.getBytes("UTF-8"));}}
 private void backup()throws Exception{
  Class<?> helper=loader.loadClass("app.morphe.extension.chmate.NativeBackupPreferences");fixture=new FixtureApplication();fixture.base=app;fixture.prefix="haiagaru.tests.backup."+System.nanoTime()+".";
  root=new File(app.getCacheDir(),fixture.prefix);String pkg=app.getPackageName();File physical=new File(new File(root,pkg),"shared_prefs"),runtime=new File(physical,pkg+"_preferences.xml");
  write(runtime,"<map><string name=\"test-marker\">canonical</string><int name=\"test-number\" value=\"42\"/></map>");
  File source=(File)method(helper,"exportFile",File.class,String.class).invoke(null,physical,ORIGINAL+".xml");ok(source.equals(runtime),"export reads actual renamed-package XML");
  File staging=new File(root,"prefs"),canonical=(File)method(helper,"exportFile",File.class,String.class).invoke(null,staging,ORIGINAL+".xml");ok(canonical.getName().equals(ORIGINAL+".xml"),"export staging uses canonical original filename");staging.mkdirs();try(InputStream in=new FileInputStream(source);OutputStream out=new FileOutputStream(canonical)){byte[] buffer=new byte[4096];int n;while((n=in.read(buffer))!=-1)out.write(buffer,0,n);}
  File zip=new File(root,"fixture.zip");try(ZipOutputStream out=new ZipOutputStream(new FileOutputStream(zip))){out.putNextEntry(new ZipEntry("prefs/"+canonical.getName()));try(InputStream in=new FileInputStream(canonical)){byte[] b=new byte[4096];int n;while((n=in.read(b))!=-1)out.write(b,0,n);}out.closeEntry();}
  try(ZipFile archive=new ZipFile(zip)){ok(archive.size()==1&&archive.getEntry("prefs/"+ORIGINAL+".xml")!=null,"ZIP contains only the common canonical preference entry");}
  SharedPreferences prefs=(SharedPreferences)method(helper,"preferences",Context.class,String.class,int.class).invoke(null,fixture,ORIGINAL,0);
  ok(fixture.stores.contains(fixture.prefix+pkg+"_preferences"),"restore maps canonical reference to active runtime store");
  boolean legacy=pkg.endsWith("191");
  if(!legacy){
   Class<?> type=loader.loadClass(pkg.endsWith("226")?"o.getUtf8":"jp.syoboi.a2chMate.feature.backupandrestore.BackupPreviewViewModel");Class<?> unsafeType=Class.forName("sun.misc.Unsafe");Field singleton=null;for(Field f:unsafeType.getDeclaredFields())if(Modifier.isStatic(f.getModifiers())&&f.getType()==unsafeType){singleton=f;break;}singleton.setAccessible(true);Object unsafe=singleton.get(null),model=unsafeType.getMethod("allocateInstance",Class.class).invoke(unsafe,type);
   for(Field f:type.getDeclaredFields())if(f.getType()==Application.class){f.setAccessible(true);f.set(model,fixture);}
   Method restore=method(type,"e",type,File.class);restore.invoke(null,model,staging);
   ok(prefs.getString("test-marker","").equals("canonical")&&prefs.getInt("test-number",0)==42,"native XML restore writes renamed-package store");
   File old=new File(staging,pkg+"_preferences.xml");write(old,"<map><string name=\"test-marker\">legacy</string></map>");restore.invoke(null,model,staging);ok(prefs.getString("test-marker","").equals("canonical"),"native restore prefers canonical name if both exist");
   ok(canonical.delete(),"only fixture canonical XML removed");restore.invoke(null,model,staging);ok(prefs.getString("test-marker","").equals("legacy"),"native restore accepts same-package historical backup name");
  }else{ok(((File)method(helper,"restoreFile",File.class,String.class).invoke(null,staging,ORIGINAL+".xml")).equals(canonical),"191 canonical restore path resolves without running its full data-restoration worker");}
  ok(runtime.exists(),"runtime source file was neither renamed nor deleted");
 }
 private void regex()throws Exception{
  Class<?> rules=loader.loadClass("app.morphe.extension.chmate.SharedNgRules$Rule");Constructor<?> ctor=rules.getDeclaredConstructor(String.class,String.class,String.class,String.class,String.class,int.class,boolean.class,int.class,int.class,boolean.class);ctor.setAccessible(true);
  String expression="左のネ|([チョンパヨ].*){5}",text="パッケージ名を変更したパッケージのバックアップ。元のパッケージ名と変更後のパッケージ名、別のパッケージ名を共通にする。";
  ok(java.util.regex.Pattern.compile(expression).matcher(text).find(),"reported false-positive regex is reproduced");
  Object rule=ctor.newInstance(String.join("",Collections.nCopies(64,"a")),"*","keyword",expression,"",1,false,1,1,true);
  ok(!(Boolean)method(rules,"expressionMatches",String.class).invoke(rule,text),"shared broad-regex rule does not hide backup discussion");
  Field flag=rules.getDeclaredField("overbroadKeyword");flag.setAccessible(true);ok(flag.getBoolean(rule),"UI can explain why automatic application was skipped");
  Class<?> index=loader.loadClass("app.morphe.extension.chmate.SharedNgIndex");Constructor<?> make=index.getDeclaredConstructor(List.class,int.class,Set.class,Set.class,int.class);make.setAccessible(true);Object cache=make.newInstance(Collections.singletonList(rule),1,Collections.emptySet(),Collections.emptySet(),255);
  ok(!(Boolean)method(index,"matches",String.class,String.class,String.class,String.class).invoke(cache,"5ch/all","id","name",text),"indexed native filtering uses the same skip policy");
 }
 private void remove(File file)throws Exception{String base=root.getCanonicalPath(),path=file.getCanonicalPath();if(!path.equals(base)&&!path.startsWith(base+File.separator))throw new IOException("Fixture cleanup escaped root");if(file.isDirectory())for(File child:file.listFiles())remove(child);if(!file.delete())throw new IOException("Cannot delete fixture");}
 @Override public void onCreate(Bundle args){super.onCreate(args);start();}
 @Override public void onStart(){Bundle result=new Bundle();int code=Activity.RESULT_CANCELED;try{app=getTargetContext();loader=app.getClassLoader();app.startActivity(new Intent().setClassName(app.getPackageName(),"jp.syoboi.a2chMate.activity.HomeActivity").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));Thread.sleep(2000);waitForIdleSync();backup();regex();result.putString("result","PASS "+checks+" canonical backup/native restore/shared-NG false-positive checks; user settings unchanged");code=Activity.RESULT_OK;}catch(Throwable error){result.putInt("checksCompleted",checks);result.putString("error",android.util.Log.getStackTraceString(error));}finally{try{if(fixture!=null)for(String store:fixture.stores)app.getSharedPreferences(store,0).edit().clear().commit();if(root!=null&&root.exists())remove(root);}catch(Exception error){code=Activity.RESULT_CANCELED;result.putString("cleanupError",error.toString());}}finish(code,result);}
}
