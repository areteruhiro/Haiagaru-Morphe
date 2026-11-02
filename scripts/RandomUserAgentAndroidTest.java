package app.morphe.extension.chmate;

import android.content.SharedPreferences;
import android.os.Looper;
import android.os.SystemClock;
import java.lang.reflect.*;
import java.net.*;
import java.util.*;

/** On-device header preparation only; never connects or submits a post. */
public final class RandomUserAgentAndroidTest {
    private static boolean rotating=true;
    private static int checks;
    private static void check(boolean ok,String label){if(!ok)throw new AssertionError(label);checks++;}
    private static String ua(Object headers)throws Exception{
        Field f=headers.getClass().getDeclaredField("namesAndValues");f.setAccessible(true);
        String[] pairs=(String[])f.get(headers);for(int i=0;i<pairs.length;i+=2)if("User-Agent".equalsIgnoreCase(pairs[i]))return pairs[i+1];
        throw new AssertionError("No User-Agent header");
    }
    private static HttpURLConnection connection()throws Exception{
        HttpURLConnection c=(HttpURLConnection)new URL("https://fixture.invalid/test/bbs.cgi").openConnection();
        c.setRequestMethod("POST");c.setDoOutput(true);
        c.setRequestProperty("Content-Type","application/x-www-form-urlencoded");
        c.setRequestProperty("Cookie","fixture=unchanged");
        return c;
    }
    public static void main(String[] args)throws Exception{
        Looper.prepareMainLooper();
        SharedPreferences prefs=(SharedPreferences)java.lang.reflect.Proxy.newProxyInstance(SharedPreferences.class.getClassLoader(),new Class<?>[]{SharedPreferences.class},(object,method,values)->{
            if(method.getName().equals("getBoolean"))return RandomUserAgentRotation.KEY.equals(values[0])?rotating:values[1];
            if(method.getName().equals("getString"))return values[1];
            if(method.getName().equals("getAll"))return Collections.emptyMap();
            throw new UnsupportedOperationException(method.getName());
        });
        ViewingUserAgent.init(prefs,BrowserUaTemplates.DEFAULT);
        // Load the native request and headers from this actual patched 191 APK.
        Class<?> requestClass=Class.forName("o.r8lambda17vlhACr7B0IDgCdn1Q67LKhir8");
        Field header=requestClass.getDeclaredField("a"),url=requestClass.getDeclaredField("f"),method=requestClass.getDeclaredField("c");
        header.setAccessible(true);url.setAccessible(true);method.setAccessible(true);
        Constructor<?> constructor=header.getType().getDeclaredConstructor(String[].class);constructor.setAccessible(true);
        Field unsafeField=sun.misc.Unsafe.class.getDeclaredField("theUnsafe");unsafeField.setAccessible(true);
        Object nativeRequest=((sun.misc.Unsafe)unsafeField.get(null)).allocateInstance(requestClass);
        Object nativeUrl=ExternalBoardAuth.parseUrl("https://fixture.invalid/test/bbs.cgi");
        url.set(nativeRequest,nativeUrl);method.set(nativeRequest,"POST");
        // Start away from a boundary so synchronous same-slot checks are unambiguous.
        SystemClock.sleep(5000-SystemClock.elapsedRealtime()%5000+100);
        String first=ViewingUserAgent.selected("https://fixture.invalid/test/bbs.cgi","POST");
        check(first!=null&&ViewingUserAgentRules.valid(first),"POST rotation enabled");
        check(first.equals(ViewingUserAgent.selected("https://fixture.invalid/a/dat/1793113633.dat","GET")),"reading and posting share slot");
        HttpURLConnection before=connection();ViewingUserAgent.prepare(before);
        check(before.getRequestProperty("User-Agent").equals(first),"java.net POST receives rotating UA");
        check(before.getRequestProperty("Cookie").equals("fixture=unchanged")&&before.getRequestProperty("Content-Type").equals("application/x-www-form-urlencoded"),"cookie and content type retained");
        Object original=constructor.newInstance((Object)new String[]{"User-Agent","Monazilla/1.00 2chMate/fixture Dalvik/old","X-Fixture","unchanged"});
        header.set(nativeRequest,original);ViewingUserAgent.prepareRequest(nativeRequest);
        String nativeFirst=ua(header.get(nativeRequest));
        check(nativeFirst.endsWith(first)&&nativeFirst.startsWith("Monazilla/1.00 2chMate/fixture "),"native 191 POST rotates suffix and retains client identity");
        check(header.get(nativeRequest)!=original&&ua(original).endsWith("Dalvik/old"),"native headers copied without mutating original");
        check(url.get(nativeRequest)==nativeUrl&&"POST".equals(method.get(nativeRequest)),"native URL and method unchanged");
        SystemClock.sleep(5100);
        String second=ViewingUserAgent.selected("https://fixture.invalid/test/bbs.cgi","POST");
        check(!first.equals(second),"UA changes after five seconds");
        check(before.getRequestProperty("User-Agent").equals(first),"prepared request stays unchanged");
        HttpURLConnection after=connection();ViewingUserAgent.prepare(after);check(after.getRequestProperty("User-Agent").equals(second),"next POST uses new UA");
        ViewingUserAgent.prepareRequest(nativeRequest);check(ua(header.get(nativeRequest)).endsWith(second),"next native POST uses new UA");
        rotating=false;check(ViewingUserAgent.selected("https://fixture.invalid/test/bbs.cgi","POST")==null,"OFF restores normal selection");
        before.disconnect();after.disconnect();
        System.out.println("PASS "+checks+" Android UA checks; native 191 and java.net GET/POST; 0 network requests");
    }
}
