package app.morphe.extension.chmate;

import java.net.URI;
import java.text.Normalizer;
import java.util.*;
import java.util.regex.*;

/** Conservative title-only hints, never proof that a thread is a successor. */
public final class RelatedThreadRules {
    private static final Pattern URL = Pattern.compile("^/test/read\\.cgi/([A-Za-z0-9_]+)/([0-9]{9,11})/?(?:.*)?$");
    private static final Pattern WORD = Pattern.compile("[a-z][a-z0-9_-]{2,}|[\\p{IsHan}]{2,}|[ァ-ヶー]{3,}");
    private static final Pattern SUBJECT_ROW = Pattern.compile("^(\\d{9,11})\\.dat<>(.*?)(?: \\(\\d+\\))?$");
    private static final Set<String> STOP = new HashSet<>(Arrays.asList("part","thread","test","スレ","スレッド","更新","速報","質問","雑談","総合","実況"));
    public static final class Source {
        public final String board, origin; public final long id;
        Source(String b,String o,long i){board=b;origin=o;id=i;}
    }
    public static final class Row {
        public final long id; public final String title; public final int score;
        Row(long i,String t,int s){id=i;title=t;score=s;}
    }
    public static Source source(String url) {
        try {
            URI u=URI.create(url); Matcher m=URL.matcher(u.getPath());
            if(!"bbs.eddibb.cc".equalsIgnoreCase(u.getHost()) || u.getUserInfo()!=null
                    || u.getPort()!=-1 || !("http".equalsIgnoreCase(u.getScheme())||"https".equalsIgnoreCase(u.getScheme())) || !m.matches())return null;
            return new Source(m.group(1),"https://bbs.eddibb.cc",Long.parseLong(m.group(2)));
        } catch(RuntimeException e){return null;}
    }
    public static String clean(String title) {
        if(title==null)return "";
        String s=Normalizer.normalize(title,Normalizer.Form.NFKC).toLowerCase(Locale.ROOT);
        return s.replaceAll("\\s*\\[[^\\[\\]\\s]+★]$","")
                .replaceAll("w{2,}|草+", " ")
                .replaceAll("(?i)(?:part|その|★|☆)\\s*\\d+", " ")
                .replaceAll("[^\\p{L}\\p{N}]+"," ").trim();
    }
    private static Set<String> words(String title) {
        Set<String> out=new LinkedHashSet<>();Matcher m=WORD.matcher(clean(title));
        while(m.find()&&out.size()<32)if(!STOP.contains(m.group()))out.add(m.group());
        return out;
    }
    public static int score(String original,long originId,String title,long id) {
        if(id<=originId || id-originId>7*86400L || title==null||title.length()>2048)return 0;
        String a=clean(original),b=clean(title);
        if(a.isEmpty()||b.isEmpty())return 0;
        if(a.equals(b))return 100;
        Set<String> aw=words(original),bw=words(title);int common=0,weight=0;
        for(String word:aw)if(bw.contains(word)){common++;weight+=word.matches("[a-z].*")?word.length()*2:word.length();}
        // A distinctive Latin name (e.g. ChMate) can bridge a complete title change.
        if(common==0 || (common==1&&weight<8))return 0;
        return Math.min(95,30+weight*3+common*5+(id-originId<=86400?10:0));
    }
    public static List<Row> candidates(String subject,String title,long originId) {
        Map<Long,Row> rows=new HashMap<>();
        for(String line:subject.split("\\r?\\n")) {
            Matcher m=SUBJECT_ROW.matcher(line);
            if(!m.matches())continue;
            long id=Long.parseLong(m.group(1));String text=m.group(2);int score=score(title,originId,text,id);
            if(score>0)rows.put(id,new Row(id,text,score));
        }
        List<Row> out=new ArrayList<>(rows.values());out.sort(Comparator.comparingInt((Row r)->r.score).reversed().thenComparing(Comparator.comparingLong((Row r)->r.id).reversed()));
        return out.size()>30?new ArrayList<>(out.subList(0,30)):out;
    }
    public static boolean listed(String subject,long id){return Pattern.compile("(?m)^"+id+"\\.dat<>").matcher(subject).find();}
}
