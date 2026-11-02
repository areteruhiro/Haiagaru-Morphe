package app.morphe.extension.chmate;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Data-only parsing shared by the UI and the background worker. */
final class ThreadWatchRules {
    private static final Pattern PATH=Pattern.compile("/test/read\\.cgi/([A-Za-z0-9_-]+)/([0-9]{9,11})(?:/.*)?");
    private static final Pattern ROW=Pattern.compile("^([0-9]{9,11})\\.dat<>(.*) \\(([0-9]{1,7})\\)\\s*$");
    static final class Target {
        final String url,subject,id;
        Target(String url,String subject,String id){this.url=url;this.subject=subject;this.id=id;}
    }
    static Target target(String value){
        try{
            URI u=new URI(value);String host=u.getHost();
            if(host==null||u.getUserInfo()!=null||u.getPort()!=-1||!("https".equals(u.getScheme())||"http".equals(u.getScheme())))return null;
            Matcher m=PATH.matcher(u.getPath());if(!m.matches())return null;
            String origin="https://"+host.toLowerCase(java.util.Locale.ROOT);
            return new Target(origin+"/test/read.cgi/"+m.group(1)+"/"+m.group(2)+"/",origin+"/"+m.group(1)+"/subject.txt",m.group(2));
        }catch(Exception invalid){return null;}
    }
    static Map<String,Integer> counts(String text){
        Map<String,Integer> out=new HashMap<>();
        if(text==null||text.trim().startsWith("<"))return out;
        for(String line:text.split("\n")){Matcher m=ROW.matcher(line);if(m.matches())out.put(m.group(1),Integer.parseInt(m.group(3)));}
        return out;
    }
    static int increase(int before,int after){return before>=0&&after>before?after-before:0;}
    private ThreadWatchRules(){}
}
