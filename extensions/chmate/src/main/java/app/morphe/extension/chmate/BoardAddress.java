package app.morphe.extension.chmate;

import java.net.URI;
import java.util.Locale;

/** A board base URL, not a subject/DAT/thread URL. */
public final class BoardAddress {
    public final String url, server, path;
    private BoardAddress(String url, String server, String path) {
        this.url=url; this.server=server; this.path=path;
    }
    public static BoardAddress parse(String input) {
        try {
            URI u=new URI(input.trim());
            String scheme=u.getScheme()==null?"":u.getScheme().toLowerCase(Locale.ROOT);
            String host=u.getHost(); String path=u.getRawPath();
            if (!(scheme.equals("https") || scheme.equals("http")) || host==null
                    || u.getUserInfo()!=null || u.getRawQuery()!=null || u.getRawFragment()!=null
                    || (u.getPort()!=-1 && (u.getPort()<1 || u.getPort()>65535))) throw new Exception();
            if(path==null) throw new Exception();
            path=path.replaceAll("^/+|/+$", "");
            // Encoded path separators, dot segments and thread endpoints are not board names.
            String lower=path.toLowerCase(Locale.ROOT);
            if(path.isEmpty() || path.contains("//") || path.contains("\\") || path.contains("%")
                    || lower.contains("read.cgi") || lower.endsWith(".txt") || lower.endsWith(".dat")) throw new Exception();
            for(String part:path.split("/")) if(part.equals(".") || part.equals("..")) throw new Exception();
            String server=host.toLowerCase(Locale.ROOT)+(u.getPort()==-1?"":":"+u.getPort());
            return new BoardAddress(scheme+"://"+server+"/"+path+"/",server,path);
        } catch(Exception e) { throw new IllegalArgumentException("板のURLを http(s)://ホスト/板名/ の形式で入力してください（スレURLは指定できません）。"); }
    }
    public String key() { return server+"/"+path; }
    public String engine() {
        String host=URI.create(url).getHost();
        if(host.equals("talk.jp") || host.endsWith(".talk.jp")) return "talk";
        if(host.equals("5ch.io") || host.endsWith(".5ch.io") || host.endsWith(".5ch.net")
                || host.endsWith(".2ch.net") || host.endsWith(".2ch.sc")) return "5ch";
        if(host.equals("jbbs.shitaraba.net") || host.equals("jbbs.livedoor.jp")) return "shitaraba";
        return "external";
    }
}
