package app.morphe.extension.chmate;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Arrays;

public final class VerifyArchiveRouteVariables {
    static int checks;
    static void check(boolean ok, String label) { if (!ok) throw new AssertionError(label); checks++; }
    static String resolve(String template) throws IOException {
        return ArchiveRouteVariables.resolve(template,"ex14","ex14.vip2ch.com","news4ssnip","1464424549","random");
    }
    static void invalid(String template) throws Exception {
        try { resolve(template); throw new AssertionError("Expected invalid template"); }
        catch(IOException expected) { checks++; }
    }
    public static void main(String[] args) throws Exception {
        String source="http://ex14.vip2ch.com/test/read.cgi/news4ssnip/1464424549/";
        String expected="https://ex14.vip2ch.com/news4ssnip/kako/1464/14644/1464424549.dat";
        check(ArchiveRouteVariables.isVipThread(source),"HTTP source supported");
        check(ArchiveRouteVariables.isVipThread(source.replace("http:","https:")),"HTTPS source supported");
        check(ArchiveRouteVariables.isVipThread(source+"l50?foo=1#last"),"suffix/query/fragment supported");
        check(ArchiveRouteVariables.isVipThread(source.replace("ex14", "EX14")),"host case insensitive");
        for(String url: Arrays.asList("",null,source.replace("ex14.vip2ch.com","ex14.vip2ch.com.evil.example"),
                source.replace("ex14.vip2ch.com","evil.example@ex14.vip2ch.com"),
                source.replace("ex14.vip2ch.com","ex14.vip2ch.com:443"),source.replace("news4ssnip","other"),
                source.replace("http:","file:"),source.replace("1464424549","abc")))
            check(!ArchiveRouteVariables.isVipThread(url),"unrelated URL excluded");
        check(resolve(ArchiveRouteVariables.VIP_TEMPLATE).equals(expected),"requested VIP archive URL exact");
        check(ArchiveRouteVariables.containsVariable("https://example.com/{$key:0:4}","key"),"slice-only route accepted");
        check(ArchiveRouteVariables.containsVariable("https://example.com/{$bbs:0:4}","bbs"),"board slice accepted");
        check(!ArchiveRouteVariables.containsVariable("https://example.com/{$keyother}","key"),"variable names exact");
        check(resolve("https://{$server}.5ch.io/{$bbs}/dat/{$key}.dat?x={$rand}")
                .equals("https://ex14.5ch.io/news4ssnip/dat/1464424549.dat?x=random"),"existing variables unchanged");
        check(resolve("https://{$host}/{$key:4:10}").equals("https://ex14.vip2ch.com/424549"),"arbitrary key slice");
        check(resolve("https://{$host}/{$bbs:0:4}").equals("https://ex14.vip2ch.com/news"),"board slice");
        check(ArchiveRouteVariables.resolve("https://example.com/?q={$bbs:0:3}","s","h","a/b","123","r")
                .equals("https://example.com/?q=a%2Fb"),"slice encoded after expansion");
        for(String token:Arrays.asList("{$unknown}","{$key:-1:4}","{$key:4:2}","{$key:0:99}","{$key:0:0}",
                "{$key:99999999999:2}","{$key:1}","{$key:a:2}")) invalid("https://example.com/"+token);
        invalid("http://example.com/{$key}");invalid("https://user@example.com/{$key}");
        if(args.length>0 && args[0].equals("--live")) {
            HttpURLConnection c=(HttpURLConnection)new URL(expected).openConnection();
            c.setConnectTimeout(6000);c.setReadTimeout(15000);c.setInstanceFollowRedirects(false);
            c.setRequestProperty("User-Agent","Mozilla/5.0 Haiagaru");
            try {
                check(c.getResponseCode()==200,"live archive HTTP 200");
                byte[] dat=c.getInputStream().readNBytes(16*1024*1024+1);
                check(dat.length<=16*1024*1024,"bounded DAT");
                String text=new String(dat,Charset.forName("MS932"));
                check(text.contains("<>")&&!text.trim().toLowerCase().startsWith("<!doctype"),"real DAT, not HTML");
                check(text.split("\n").length>1,"multiple response lines");
                System.out.println("LIVE DAT bytes="+dat.length+", response lines="+text.split("\n").length);
            } finally { c.disconnect(); }
        }
        System.out.println("PASS "+checks+" archive route variable/VIP checks");
    }
}
