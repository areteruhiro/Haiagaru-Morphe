package app.morphe.extension.chmate;

import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.*;
import java.util.function.Function;
import java.util.regex.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.*;

/** Converts site records into the existing native subject/DAT protocol. */
final class SiteNativeData {
    static byte[] subject(List<SiteBoardProtocol.Topic> topics){
        StringBuilder out=new StringBuilder();for(SiteBoardProtocol.Topic t:topics){
            if(!t.id.matches("[1-9][0-9]{0,11}")||t.count<1||t.count>1000000)throw new IllegalArgumentException("Invalid topic");
            try{out.append(SiteThreadIds.nativeId(t.id));}catch(java.io.IOException invalid){throw new IllegalArgumentException(invalid);}
            out.append(".dat<>").append(header(t.title)).append(" (").append(t.count).append(")\n");
        }return wire(out.toString());
    }
    static byte[] dat(SiteBoardProtocol.Board board,SiteBoardProtocol.ThreadPage page,Function<String,String> media,Function<String,String> links){
        LinkedHashMap<String,Integer> numbers=new LinkedHashMap<>();int next=1;
        for(SiteBoardProtocol.Entry e:page.entries)if(numbers.put(e.original,next++)!=null)throw new IllegalArgumentException("Duplicate original number");
        StringBuilder out=new StringBuilder();
        for(SiteBoardProtocol.Entry e:page.entries){
            Document doc=Jsoup.parseBodyFragment(e.body);doc.outputSettings().prettyPrint(false);
            for(Element a:doc.select("a[href]")){String target=links.apply(a.attr("href"));if(target!=null)a.attr("href",target);}
            String body=doc.body().html().replace("\r\n","<br>").replace("\r","<br>").replace("\n","<br>");
            Pattern references=Pattern.compile("&gt;(?:&gt;|No\\.)([0-9]+)(?:-([0-9]+))?");Matcher matches=references.matcher(body);StringBuffer fixed=new StringBuffer();
            while(matches.find()){
                Integer begin=numbers.get(matches.group(1)),end=matches.group(2)==null?null:numbers.get(matches.group(2));
                String replacement=begin!=null&&(matches.group(2)==null||end!=null)?"&gt;&gt;"+begin+(end==null?"":"-"+end):matches.group();
                matches.appendReplacement(fixed,Matcher.quoteReplacement(replacement));
            }matches.appendTail(fixed);body=fixed.toString();
            for(String image:e.media){String target=media.apply(image);if(target!=null)body+="<br><a href=\""+Entities.escape(target)+"\">"+Entities.escape(target)+"</a>";}
            String date=e.date;if(!board.kind.equals("animanch"))date+=" No."+e.original;
            out.append(header(e.name)).append("<><>").append(header(date)).append("<>").append(body).append("<>");
            if(numbers.get(e.original)==1)out.append(header(page.title));out.append('\n');
        }return wire(out.toString());
    }
    /** Native quote numbers must be translated before submitting to board-global numbering sites. */
    static String originalQuotes(String body,SiteBoardProtocol.ThreadPage page){
        Matcher matcher=Pattern.compile(">>([1-9][0-9]{0,5})(?:-([1-9][0-9]{0,5}))?").matcher(body);StringBuffer out=new StringBuffer();
        while(matcher.find()){
            int n=Integer.parseInt(matcher.group(1)),end=matcher.group(2)==null?0:Integer.parseInt(matcher.group(2));String replace=matcher.group();
            if(n<=page.entries.size()&&(end==0||end<=page.entries.size()))replace=">>"+page.entries.get(n-1).original+(end==0?"":"-"+page.entries.get(end-1).original);
            matcher.appendReplacement(out,Matcher.quoteReplacement(replace));
        }matcher.appendTail(out);return out.toString();
    }
    private static String header(String text){return Entities.escape(text.replace('\r',' ').replace('\n',' '));}
    static byte[] wire(String input){
        Charset charset=Charset.forName("windows-31j");CharsetEncoder encoder=charset.newEncoder();
        if(encoder.canEncode(input))return input.getBytes(charset);
        StringBuilder out=new StringBuilder();for(int pos=0;pos<input.length();){int cp=input.codePointAt(pos);String letter=new String(Character.toChars(cp));
            if(encoder.canEncode(letter))out.append(letter);else out.append("&#x").append(Integer.toHexString(cp)).append(';');pos+=Character.charCount(cp);}
        return out.toString().getBytes(charset);
    }
    private SiteNativeData(){}
}
