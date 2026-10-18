package app.morphe.extension.chmate;
import java.nio.file.*;
import java.nio.charset.Charset;
public final class VerifyDroppedThreadRules {
    static int checks;
    static void check(boolean b,String label){if(!b)throw new AssertionError(label);checks++;}
    public static void main(String[] args)throws Exception{
        String banner="<div class='stoplight stopred stopdone'>■ このスレッドは過去ログ倉庫に格納されています</div>";
        check(DroppedThreadRules.archived(banner),"official archived banner");
        check(DroppedThreadRules.archived("<div class=\"stopdone stoplight\"><b>このスレッドは</b>過去ログ倉庫に格納されています</div>"),"class order and child tags");
        check(!DroppedThreadRules.archived("<div class='stoplight stopdone'>もう書けません</div>"),"write stop alone is not archived");
        check(!DroppedThreadRules.archived("<div id='1' class='clear post'>quoted "+banner+"</div>"),"ignore quoted banner in post");
        check(!DroppedThreadRules.archived("<article class='post'>"+banner+"</article>"),"ignore article quote");
        check(!DroppedThreadRules.archived("HTTP 404 / Dat not found"),"404 text not evidence");
        check(!DroppedThreadRules.archived("<h1>Access denied</h1>"),"access failure not evidence");
        check(!DroppedThreadRules.archived(null),"missing data not evidence");
        DroppedThreadRules.Thread t=DroppedThreadRules.thread("https://greta.5ch.io/test/read.cgi/poverty/1792155688/");
        check(t!=null&&t.key.equals("poverty/1792155688"),"reported thread identity");
        check(t.url.equals("https://greta.5ch.io/test/read.cgi/poverty/1792155688/1"),"small first-response request");
        check(DroppedThreadRules.thread("http://greta.5ch.net/test/read.cgi/poverty/1792155688/l50").key.equals(t.key),"5ch domain aliases");
        for(String u:new String[]{"https://5ch.io.evil.invalid/test/read.cgi/poverty/1792155688/","https://evil.invalid/test/read.cgi/poverty/1792155688/","https://user@greta.5ch.io/test/read.cgi/poverty/1792155688/","https://greta.5ch.io:8443/test/read.cgi/poverty/1792155688/","https://itest.5ch.io/test/read.cgi/poverty/1792155688/","file:///test/read.cgi/poverty/1792155688/","https://greta.5ch.io/poverty/subject.txt"})check(DroppedThreadRules.thread(u)==null,"no external/invalid probing: "+u);
        check(DroppedThreadRules.subject("1792155688.dat<>title (4)\n1792155689.dat<>other (1)\n").size()==2,"subject IDs");
        check(DroppedThreadRules.subject("<html>1792155688.dat</html>").isEmpty(),"invalid subject ignored");
        if(args.length>0)check(DroppedThreadRules.archived(Files.readString(Path.of(args[0]),Charset.forName("MS932"))),"reported real archived HTML");
        System.out.println("PASS "+checks+" official DAT-drop evidence checks");
    }
}
