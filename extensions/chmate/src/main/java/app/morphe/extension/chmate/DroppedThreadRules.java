package app.morphe.extension.chmate;

import java.net.URI;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Positive archive evidence only: a missing subject entry or an HTTP error is not enough. */
final class DroppedThreadRules {
    private static final Pattern PATH=Pattern.compile("^/test/read\\.cgi/([A-Za-z0-9_]+)/([0-9]{9,11})(?:/.*)?$");
    private static final Pattern POST=Pattern.compile("(?is)<(?:div|article)\\b[^>]*(?:class=[\"'][^\"']*\\bpost\\b|id=[\"']1[\"'])");
    private static final Pattern BANNER=Pattern.compile("(?is)<div\\b[^>]*class=[\"'](?=[^\"']*\\bstoplight\\b)(?=[^\"']*\\bstopdone\\b)[^\"']*[\"'][^>]*>(.*?)</div>");
    private static final Pattern SUBJECT=Pattern.compile("(?m)^([0-9]{9,11})\\.dat<>");
    static final class Thread {
        final String origin,board,id,key,url;
        Thread(String origin,String board,String id){this.origin=origin;this.board=board;this.id=id;key=board+"/"+id;url=origin+"/test/read.cgi/"+board+"/"+id+"/1";}
    }
    static Thread thread(String url){
        try{
            URI u=URI.create(url);String host=u.getHost();
            if(host==null||u.getUserInfo()!=null||u.getPort()!=-1||!("http".equalsIgnoreCase(u.getScheme())||"https".equalsIgnoreCase(u.getScheme())))return null;
            host=host.toLowerCase(Locale.ROOT);
            // Native 5ch URLs only; never send an external board to a guessed server.
            if(!host.matches("[a-z0-9-]+\\.5ch\\.(?:io|net)"))return null;
            if(host.equals("itest.5ch.io")||host.equals("itest.5ch.net")||host.equals("kako.5ch.io")||host.equals("kako.5ch.net"))return null;
            Matcher m=PATH.matcher(u.getPath());if(!m.matches())return null;
            host=host.replaceFirst("\\.5ch\\.net$",".5ch.io");
            return new Thread("https://"+host,m.group(1),m.group(2));
        }catch(Exception ignored){return null;}
    }
    static boolean archived(String html){
        if(html==null)return false;
        Matcher first=POST.matcher(html);String header=first.find()?html.substring(0,first.start()):html;
        Matcher banner=BANNER.matcher(header);
        while(banner.find()){
            String text=banner.group(1).replaceAll("<[^>]*>","").replaceAll("\\s|&nbsp;","");
            if(text.contains("このスレッドは過去ログ倉庫に格納されています"))return true;
        }
        return false;
    }
    static Set<String> subject(String body){
        Set<String> ids=new HashSet<>();Matcher m=SUBJECT.matcher(body==null?"":body);
        while(m.find())ids.add(m.group(1));return ids;
    }
    private DroppedThreadRules(){}
}
