package app.haiagaru.tests.edgecookie;
import android.app.Instrumentation;
import android.content.*;
import android.os.Bundle;
import dalvik.system.DexClassLoader;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;

/** Runs unmodified ChMate cookie classes in this isolated test app, with synthetic preferences only. */
public final class EdgeCookiePersistenceInstrumentation extends Instrumentation {
    private Bundle arguments;
    public void onCreate(Bundle args){arguments=args;start();}
    static void check(boolean ok){if(!ok)throw new AssertionError("Cookie persistence assertion failed");}
    public void onStart(){
        Bundle result=new Bundle();
        try{
            Context context=getTargetContext();
            File source=new File(context.getCacheDir(),"source.apk");
            source.delete();
            try(InputStream in=new FileInputStream(arguments.getString("source"));OutputStream out=new FileOutputStream(source)){
                source.setReadOnly();byte[] buffer=new byte[32768];int count;while((count=in.read(buffer))>=0)out.write(buffer,0,count);
            }
            ClassLoader loader=new DexClassLoader(source.getPath(),context.getCodeCacheDir().getPath(),null,context.getClassLoader());
            Class<?> jarClass=loader.loadClass("com.franmontiel.persistentcookiejar.PersistentCookieJar");
            Class<?> persistClass=loader.loadClass("com.franmontiel.persistentcookiejar.persistence.SharedPrefsCookiePersistor");
            Class<?> cacheClass=loader.loadClass("com.franmontiel.persistentcookiejar.cache.SetCookieCache");
            Method read=Arrays.stream(jarClass.getDeclaredMethods()).filter(m->m.getReturnType()==List.class&&m.getParameterTypes().length==1).findFirst().get();
            Class<?> urlClass=read.getParameterTypes()[0];
            Method urlParse=Arrays.stream(urlClass.getDeclaredMethods()).filter(m->Modifier.isStatic(m.getModifiers())&&m.getReturnType()==urlClass&&Arrays.equals(m.getParameterTypes(),new Class[]{String.class})).findFirst().get();urlParse.setAccessible(true);
            Object url=urlParse.invoke(null,"https://bbs.eddibb.cc/liveedge/");
            Method encode=Arrays.stream(persistClass.getDeclaredMethods()).filter(m->m.getReturnType()==String.class&&m.getParameterTypes().length==1&&m.getParameterTypes()[0]!=String.class).findFirst().get();
            Class<?> cookieClass=encode.getParameterTypes()[0];
            Method cookieParse=Arrays.stream(cookieClass.getDeclaredMethods()).filter(m->Modifier.isStatic(m.getModifiers())&&m.getReturnType()==cookieClass&&Arrays.equals(m.getParameterTypes(),new Class[]{urlClass,String.class})).findFirst().get();cookieParse.setAccessible(true);
            Object cookie=cookieParse.invoke(null,url,"migration_test=synthetic; Path=/");check(cookie!=null);
            SharedPreferences prefs=context.getSharedPreferences("synthetic_cookie_test",0);prefs.edit().clear().commit();
            Object persistor=persistClass.getConstructor(SharedPreferences.class).newInstance(prefs);
            Constructor<?> constructor=jarClass.getConstructors()[0];
            Object jar=constructor.newInstance(cacheClass.getConstructor().newInstance(),persistor);
            Method save=Arrays.stream(jarClass.getDeclaredMethods()).filter(m->m.getReturnType()==void.class&&Arrays.equals(m.getParameterTypes(),new Class[]{urlClass,List.class})).findFirst().get();
            save.invoke(jar,url,Collections.singletonList(cookie));
            check(((List<?>)read.invoke(jar,url)).size()==1);
            check(prefs.getAll().isEmpty()); // Native save intentionally omits session cookies.
            Method persist=persistClass.getDeclaredMethod(arguments.getString("persist"),Collection.class);
            persist.invoke(persistor,Collections.singletonList(cookie));check(prefs.getAll().size()==1);
            // Recreate both jar and persistor, as on a process restart; values must round-trip exactly.
            Object restored=constructor.newInstance(cacheClass.getConstructor().newInstance(),persistClass.getConstructor(SharedPreferences.class).newInstance(prefs));
            List<?> loaded=(List<?>)read.invoke(restored,url);check(loaded.size()==1);
            check(loaded.get(0).toString().startsWith("migration_test=synthetic;"));
            Object expired=cookieParse.invoke(null,url,"migration_test=synthetic; Path=/; Max-Age=0");
            save.invoke(restored,url,Collections.singletonList(expired));persist.invoke(persistor,Collections.singletonList(expired));
            Object cleared=constructor.newInstance(cacheClass.getConstructor().newInstance(),persistClass.getConstructor(SharedPreferences.class).newInstance(prefs));
            check(((List<?>)read.invoke(cleared,url)).isEmpty());
            prefs.edit().clear().commit();source.delete();
            result.putString("result","PASS: native session save, explicit persistence, restart round-trip and expiry; synthetic data only");finish(-1,result);
        }catch(Throwable error){result.putString("error",error.getClass().getName()+": "+(error.getCause()==null?"":error.getCause().getClass().getName()));finish(0,result);}
    }
}
