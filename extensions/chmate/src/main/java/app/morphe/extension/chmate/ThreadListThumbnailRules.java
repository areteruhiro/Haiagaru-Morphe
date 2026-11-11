package app.morphe.extension.chmate;

import java.net.URI;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/** Pure URL and opening-post rules for the opt-in thread-list experiment. */
final class ThreadListThumbnailRules {
    static final class Board {
        final String host, name;
        Board(String host,String name){this.host=host;this.name=name;}
        String threadUrl(String id){return "https://"+host+"/test/read.cgi/"+name+"/"+id+"/";}
    }
    private static final Pattern DIAGNOSTIC=Pattern.compile("^\\{server:([^,]+), name:([^,]+), created:");
    private static final Pattern NAME=Pattern.compile("[a-zA-Z0-9_-]{1,80}");
    private static final Pattern IMAGE=Pattern.compile("(?i)\\.(?:jpe?g|png|webp|gif|avif)$");

    static Board board(String address){
        if(address==null)return null;
        String value=address.trim().replace("%2F","/").replace("%2f","/");
        Matcher diagnostic=DIAGNOSTIC.matcher(value);
        if(diagnostic.find())value="https://"+diagnostic.group(1).trim()+"/"+diagnostic.group(2).trim()+"/";
        try{
            URI uri=URI.create(value.contains("://")?value:"https://"+value);
            String host=uri.getHost(),path=uri.getRawPath();
            if(!("https".equalsIgnoreCase(uri.getScheme())||"http".equalsIgnoreCase(uri.getScheme()))
                    ||host==null||uri.getUserInfo()!=null||uri.getPort()!=-1||uri.getRawQuery()!=null||uri.getRawFragment()!=null||path==null)return null;
            host=host.toLowerCase(Locale.ROOT);
            if(!(host.endsWith(".5ch.io")||host.endsWith(".5ch.net")))return null;
            Matcher thread=Pattern.compile("^/test/read\\.cgi/([a-zA-Z0-9_-]{1,80})/[0-9]{9,11}/?$",Pattern.CASE_INSENSITIVE).matcher(path);
            String name=thread.matches()?thread.group(1):path.replaceFirst("^/","").replaceFirst("/$","");
            if(!NAME.matcher(name).matches())return null;
            return new Board(host,name.toLowerCase(Locale.ROOT));
        }catch(IllegalArgumentException invalid){return null;}
    }

    static boolean publicImage(String address){
        try{
            URI u=URI.create(address);String host=u.getHost();
            if(!"https".equalsIgnoreCase(u.getScheme())||host==null||u.getUserInfo()!=null||u.getPort()!=-1
                    ||u.getRawPath()==null||u.getRawPath().length()>1024||u.getRawFragment()!=null)return false;
            host=host.toLowerCase(Locale.ROOT);
            return host.contains(".")&&!host.endsWith(".local")&&!host.endsWith(".localhost")&&!host.endsWith(".internal")
                    &&!host.endsWith(".test")&&!host.endsWith(".invalid")&&!host.endsWith(".onion")
                    &&!host.matches("[0-9.]+")&&!host.contains(":");
        }catch(IllegalArgumentException invalid){return false;}
    }

    static String firstFiveChImage(String html,String threadUrl){
        Document doc=Jsoup.parse(html,threadUrl);
        Element opening=doc.selectFirst(".thread .post:first-child .message, .post:first-child .message, .post:first-of-type .message, .post:first-child .post-content, dl.thread dd:first-of-type, .postMessage, .message, dd");
        if(opening==null)return null;
        for(Element link:opening.select("a[href], img[src]")){
            String url=link.hasAttr("href")?link.absUrl("href"):link.absUrl("src");
            if(!publicImage(url))continue;
            URI uri=URI.create(url);
            if(IMAGE.matcher(uri.getPath()).find()||"pbs.twimg.com".equalsIgnoreCase(uri.getHost())
                    &&uri.getRawQuery()!=null&&uri.getRawQuery().matches("(?i).*(?:format|name)=(?:jpe?g|png|webp|gif)(?:&.*)?"))return url;
        }
        return null;
    }
    private ThreadListThumbnailRules(){}
}
