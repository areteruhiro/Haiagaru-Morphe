package app.morphe.extension.chmate;

import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.regex.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.*;
import org.jsoup.safety.Safelist;

/** Independent public-page adapters. Native state and network I/O belong to the gateway. */
final class SiteBoardProtocol {
    static final class Board {
        final String kind,url,host,path,thread;
        Board(String kind,String url,String host,String path,String thread){this.kind=kind;this.url=url;this.host=host;this.path=path;this.thread=thread;}
        String threadUrl(String id){
            if(!id.matches("[1-9][0-9]{0,11}"))throw new IllegalArgumentException("Invalid thread");
            if(kind.equals("animanch"))return AnimanchProtocol.ORIGIN+"/board/"+id+"/";
            if(kind.equals("futaba"))return url+"res/"+id+".htm";
            return "https://bakusai.com/thr_res/"+path+"tid="+id+"/";
        }
        String indexUrl(){return kind.equals("futaba")?url+"futaba.php?mode=cat":url;}
    }
    static Board address(String raw)throws IOException{
        try{
            URI u=URI.create(raw.trim());String host=u.getHost(),path=u.getRawPath();
            if(!"https".equalsIgnoreCase(u.getScheme())||host==null||u.getUserInfo()!=null||u.getPort()!=-1)throw new IllegalArgumentException();
            host=host.toLowerCase(Locale.ROOT);
            if(host.equals("bbs.animanch.com")&&u.getRawQuery()==null){
                String thread=AnimanchProtocol.threadId(raw.trim());if(!(path.equals("/")||path.matches("/category[1-9][0-9]{0,3}/")||thread!=null))throw new IllegalArgumentException();
                String category=thread==null?path:"/";
                return new Board("animanch",AnimanchProtocol.ORIGIN+category,host,category,thread);
            }
            if(host.matches("[a-z0-9-]+\\.2chan\\.net")){
                Matcher m=Pattern.compile("/([a-zA-Z0-9_-]+)/(?:futaba\\.(?:htm|php)|res/([1-9][0-9]{0,11})\\.htm)?").matcher(path);
                if(!m.matches())throw new IllegalArgumentException();
                if(u.getRawQuery()!=null&&!u.getRawQuery().equals("mode=cat"))throw new IllegalArgumentException();
                return new Board("futaba","https://"+host+"/"+m.group(1)+"/",host,m.group(1),m.group(2));
            }
            if(host.equals("bakusai.com")&&u.getRawQuery()==null){
                Matcher m=Pattern.compile("/thr_(?:tl|res)/(acode=[0-9]{1,3}/ctgid=[0-9]{1,6}/bid=[0-9]{1,8}/)(?:tid=([1-9][0-9]{0,11})/)?(?:p=[1-9][0-9]{0,2}/)?(?:tp=[0-9]+/)?").matcher(path);
                if(!m.matches())throw new IllegalArgumentException();
                return new Board("bakusai","https://bakusai.com/thr_tl/"+m.group(1),host,m.group(1),m.group(2));
            }
        }catch(RuntimeException invalid){/* Reject arbitrary URLs and encoded traversal. */}
        throw new IOException("爆サイ・ふたばの板URL、またはあにまんのURLを指定してください");
    }
    static final class Topic {
        final String id,title;final int count;
        Topic(String id,String title,int count){this.id=id;this.title=title;this.count=count;}
    }
    static final class Entry {
        final String original,name,date,body;final List<String> media;
        Entry(String original,String name,String date,String body,List<String> media){this.original=original;this.name=name;this.date=date;this.body=body;this.media=Collections.unmodifiableList(new ArrayList<>(media));}
    }
    static final class ThreadPage {
        final String title;final List<Entry> entries;
        ThreadPage(String title,List<Entry> entries){this.title=title;this.entries=Collections.unmodifiableList(new ArrayList<>(entries));}
    }
    private static final Safelist SAFE=new Safelist().addTags("a","br","b","strong","i","em","s","del","blockquote","p","span")
            .addAttributes("a","href").addProtocols("a","href","http","https");
    static List<Topic> topics(Board board,String html)throws IOException{
        ArrayList<Topic> out=new ArrayList<>();
        if(board.kind.equals("animanch")){
            for(AnimanchProtocol.Topic t:AnimanchProtocol.catalogue(html))out.add(new Topic(t.id,t.title,t.replies));return out;
        }
        Document doc=Jsoup.parse(html,board.indexUrl());LinkedHashMap<String,Topic> unique=new LinkedHashMap<>();
        if(board.kind.equals("futaba")){
            for(Element cell:doc.select("#cattable td")){
                Element link=cell.selectFirst("a[href]");if(link==null)continue;
                String id=threadIn(board,link.absUrl("href"));if(id==null)continue;
                Element count=null;for(Element candidate:cell.select("font"))if(candidate.text().trim().matches("[0-9]{1,6}")){count=candidate;break;}if(count==null)continue;
                String raw=count.text().trim();if(!raw.matches("[0-9]{1,6}"))continue;
                Element text=cell.selectFirst("small");String title=text==null?"No."+id:text.text().trim();
                if(title.isEmpty())title="No."+id;
                unique.putIfAbsent(id,new Topic(id,title,Integer.parseInt(raw)+1));
            }
        }else{
            for(Element link:doc.select("#threadListField a[href*=/thr_res/], .thr_list a[href*=/thr_res/]")){
                String id=threadIn(board,link.absUrl("href"));if(id==null||unique.containsKey(id))continue;
                Element heading=link.selectFirst(".imgbody .title, .ttTitle .title");
                boolean cards=heading!=null;
                String title=cards?(heading==null?"":heading.text()):link.hasAttr("title")?link.attr("title"):link.text();if(title.trim().isEmpty())continue;
                Element group=link.closest("table");if(group==null)group=link.closest("li");
                Element count=cards?link.selectFirst(".thrimg_comment_space + span, .thrlis_comment_space + span"):group==null?null:group.selectFirst(".comment_count_area > span:last-child");
                if(count==null)continue;String raw=count.text().trim();
                if(!raw.matches("(?:[0-9]{1,6}|[0-9]{1,3}(?:,[0-9]{3}){1,2})"))continue;
                int replies=Integer.parseInt(raw.replace(",",""));if(replies>999999)continue;
                unique.put(id,new Topic(id,title.trim(),replies+1));
            }
        }
        if(unique.isEmpty())throw new IOException("対応するスレ一覧がありません（認証・一覧形式を確認してください）");
        out.addAll(unique.values());return out;
    }
    static ThreadPage thread(Board board,String id,String html)throws IOException{
        if(board.kind.equals("animanch")){
            AnimanchProtocol.Page page=AnimanchProtocol.thread(html,board.threadUrl(id));ArrayList<Entry> entries=new ArrayList<>();
            for(AnimanchProtocol.Reply r:page.replies)entries.add(new Entry(Integer.toString(r.number),r.name,r.date,r.html,r.images));
            return new ThreadPage(page.title,entries);
        }
        Document doc=Jsoup.parse(html,board.threadUrl(id));doc.outputSettings().prettyPrint(false);
        return board.kind.equals("futaba")?futaba(board,id,doc):bakusai(board,id,doc);
    }
    private static ThreadPage bakusai(Board board,String id,Document doc)throws IOException{
        ThreadPage result=bakusaiSlice(board,id,doc);int expected=0;
        for(Entry row:result.entries)if(!row.original.equals(Integer.toString(expected++)))throw new IOException("爆サイのレスが未取得です");
        if(bakusaiCount(doc)+1!=result.entries.size())throw new IOException("爆サイのレスが未取得です");return result;
    }
    static int bakusaiCount(Document doc)throws IOException{
        Element count=doc.selectFirst(".res-count, #res0_whole .thrlis_comment_space + span");if(count==null||!count.text().matches("[0-9]{1,5}"))throw new IOException("爆サイのレス数を確認できません");return Integer.parseInt(count.text());
    }
    static ThreadPage bakusaiSlice(Board board,String id,Document doc)throws IOException{
        return bakusaiSlice(board,id,doc,true);
    }
    static ThreadPage bakusaiSlice(Board board,String id,Document doc,boolean requireOpening)throws IOException{
        Element title=doc.selectFirst("#title_thr"),start=doc.getElementById("threadBody");
        Element identity=doc.selectFirst(".thr_id");
        // Normal mobile boards place #threadBody inside #res0_body too.
        // Its presence must not select the desktop layout without #title_thr.
        boolean mobile=title==null;
        if(mobile){title=doc.selectFirst("#res0_whole [itemprop=headline]");start=doc.selectFirst("#res0_body .entry-content");if(start==null)start=doc.selectFirst("#res0_body #div_box");if(start==null)start=doc.getElementById("res0_body");identity=doc.selectFirst("#res0_whole .thr_rotundate");}
        if(title==null||requireOpening&&start==null||identity==null||!identity.text().matches(".*\\b"+Pattern.quote(id)+"\\b.*"))throw new IOException("爆サイのスレ本文を取得できません");
        // Older mobile pages omit the opening article. The first page must still supply it.
        ArrayList<Entry> out=new ArrayList<>();if(start!=null)out.add(entry(board,"0","スレ本文",text(doc,mobile?"#res0_whole .posts":".titleheader .posts"),start));
        for(Element row:doc.select("#res_list > .res_list_article[id], #res_list > li.res_block[id]")){
            Matcher m=Pattern.compile(mobile?"res([1-9][0-9]{0,5})_block":"res([1-9][0-9]{0,5})").matcher(row.id());if(!m.matches())continue;
            Element body=row.selectFirst(mobile?".res_body":".resbody");if(body==null)throw new IOException("レス本文が欠落しています");
            out.add(entry(board,m.group(1),text(row,mobile?".name_goodbad_box > .name":"dd.name"),text(row,"[itemprop=commentTime]"),body));
        }
        return new ThreadPage(title.text(),out);
    }
    private static ThreadPage futaba(Board board,String id,Document doc)throws IOException{
        Element root=doc.selectFirst(".thre[data-res="+id+"]");if(root==null)throw new IOException("ふたばのスレ本文がありません（過去ログ・認証を確認してください）");
        ArrayList<Entry> out=new ArrayList<>();Element main=root.clone();main.select("table").remove();
        out.add(futabaEntry(board,main));if(!out.get(0).original.equals(id))throw new IOException("別のスレ本文です");
        for(Element row:root.select("td.rtd"))out.add(futabaEntry(board,row));
        Set<String> seen=new HashSet<>();for(Entry e:out)if(!seen.add(e.original))throw new IOException("ふたばのレス番号が重複しています");
        Element sub=main.selectFirst(".csb");String title=sub==null?"":sub.text();
        if(title.isEmpty()||title.equals("無題")){Element body=main.selectFirst("blockquote");title=body==null?"No."+id:body.text();}
        if(title.length()>120)title=title.substring(0,120);return new ThreadPage(title,out);
    }
    private static Entry futabaEntry(Board board,Element row)throws IOException{
        Element number=row.selectFirst(".cno"),body=row.selectFirst("blockquote");
        if(number==null||body==null)throw new IOException("ふたばのレス本文・番号が欠落しています");
        Matcher m=Pattern.compile("No\\.([1-9][0-9]{0,11})").matcher(number.text());if(!m.find())throw new IOException("レス番号が不正です");
        Entry text=entry(board,m.group(1),text(row,".cnm"),text(row,".cnw"),body);ArrayList<String> media=new ArrayList<>();
        for(Element a:row.select("a[href]")){String url=a.absUrl("href");if(media(board,url)&&!media.contains(url))media.add(url);}
        return new Entry(text.original,text.name,text.date,text.body,media);
    }
    private static Entry entry(Board board,String number,String name,String date,Element body){
        Element safe=body.clone();LinkedHashSet<String> media=new LinkedHashSet<>();
        for(Element image:safe.select("img")){
            String raw=image.hasAttr("data-original")?image.attr("data-original"):image.attr("src");String url=resolve(board.url,raw);
            if(media(board,url))media.add(url);
        }
        safe.select("script,style,iframe,form,input,button,img").remove();
        for(Element popup:safe.select("[data-tipso]"))popup.removeAttr("data-tipso");
        for(Element a:safe.select("a[href]")){String url=resolve(board.url,a.attr("href"));if(url==null)a.removeAttr("href");else a.attr("href",url);}
        return new Entry(number,name,date,Jsoup.clean(safe.html(),board.url,SAFE,new Document.OutputSettings().prettyPrint(false)),new ArrayList<>(media));
    }
    static String threadIn(Board board,String url){
        try{Board parsed=address(url);return sameBoard(board,parsed)?parsed.thread:null;}catch(IOException invalid){return null;}
    }
    static boolean sameBoard(Board board,Board other){return board.kind.equals(other.kind)&&(board.url.equals(other.url)||board.kind.equals("animanch")||board.kind.equals("bakusai")&&board.path.replaceFirst("^acode=[0-9]+/","").equals(other.path.replaceFirst("^acode=[0-9]+/","")));}
    static String resolve(String base,String link){
        try{URI u=URI.create(base).resolve(link);return "https".equalsIgnoreCase(u.getScheme())&&u.getHost()!=null&&u.getUserInfo()==null&&u.getPort()==-1?u.toASCIIString():null;}catch(RuntimeException invalid){return null;}
    }
    private static boolean media(Board board,String url){
        try{URI u=URI.create(url);String host=u.getHost(),path=u.getPath();if(host==null||!"https".equals(u.getScheme())||u.getPort()!=-1)return false;
            return board.kind.equals("bakusai")?host.equals("img2.bakusai.com")&&!path.endsWith("dummyM.gif")
                    :host.endsWith(".2chan.net")&&path.startsWith("/"+board.path+"/src/");
        }catch(RuntimeException invalid){return false;}
    }
    private static String text(Element parent,String selector){Element e=parent.selectFirst(selector);return e==null?"":e.text();}
    private SiteBoardProtocol(){}
}
