package app.morphe.extension.chmate;
import java.net.*;
import java.util.*;
public final class ImgboxCookieCheck {
    public static void main(String[] args) throws Exception {
        HttpURLConnection c=(HttpURLConnection)new URL("https://imgbox.com/").openConnection();
        c.setRequestProperty("User-Agent","Haiagaru/1.0 (Android media uploader)");
        CookieManager manager=new CookieManager(null,CookiePolicy.ACCEPT_ORIGINAL_SERVER);
        try {
            c.getInputStream().close();manager.put(c.getURL().toURI(),c.getHeaderFields());
            for(var entry:c.getHeaderFields().entrySet()) if("Set-Cookie".equalsIgnoreCase(entry.getKey()))
                for(String header:entry.getValue()) {
                    int eq=header.indexOf('='),semi=header.indexOf(';');
                    System.out.println("RESPONSE_COOKIE "+(eq<0?"unknown":header.substring(0,eq))+" attributes="+(semi<0?"none":header.substring(semi)));
                }
            for(HttpCookie cookie:manager.getCookieStore().getCookies())
                System.out.println(cookie.getName()+" length="+cookie.getValue().length()+" domain="+cookie.getDomain()+" version="+cookie.getVersion()+" path="+cookie.getPath());
            for(var entry:manager.get(new URI("https://imgbox.com/ajax/token/generate"),Collections.emptyMap()).entrySet())
                System.out.println("REQUEST_HEADER "+entry.getKey()+" values="+entry.getValue().size()+" hasRFC2965="+entry.getValue().toString().contains("$Version"));
        } finally {c.disconnect();}
    }
}
