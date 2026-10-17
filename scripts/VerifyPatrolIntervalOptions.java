import app.morphe.patches.chmate.PatrolIntervalOptions;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.w3c.dom.*;

public class VerifyPatrolIntervalOptions {
    static int checks;
    static void check(boolean value) { if(!value)throw new AssertionError("case "+checks);checks++; }
    static String array(String name, String... values) {
        StringBuilder out=new StringBuilder("<string-array name=\""+name+"\">");
        for(String value:values)out.append("<item>").append(value).append("</item>");
        return out.append("</string-array>").toString();
    }
    static Document parse(String text)throws Exception {
        return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)));
    }
    static String item(Document doc,int array,int index) {return ((Element)doc.getElementsByTagName("string-array").item(array)).getElementsByTagName("item").item(index).getTextContent();}
    public static void main(String[] args)throws Exception {
        String labels=array("labels","しない","15分","30分","1時間","3時間","6時間","24時間");
        String values=array("values","0","15","30","60","180","360","1440");
        Document doc=parse("<resources>"+labels+values+array("other","5","10")+"</resources>");
        List<String> names=PatrolIntervalOptions.expand(doc);
        check(names.equals(List.of("labels","values")));
        check(item(doc,0,1).equals("5分"));check(item(doc,0,2).equals("10分"));
        String[] expected={"0","5","10","15","30","60","180","360","1440"};
        for(int i=0;i<expected.length;i++)check(item(doc,1,i).equals(expected[i]));
        check(item(doc,2,0).equals("5"));check(item(doc,2,1).equals("10"));
        Document en=parse("<resources>"+array("labels","Never","15 min","30 min","1 hour","3 hours","6 hours","24 hours")+values+"</resources>");
        PatrolIntervalOptions.expandOverrides(en,names);
        check(item(en,0,1).equals("5 min"));check(item(en,0,2).equals("10 min"));check(item(en,0,3).equals("15 min"));
        Document escaped=parse("<resources>"+labels+array("values","\\0","\\15","\\30","\\60","\\180","\\360","\\1440")+"</resources>");
        PatrolIntervalOptions.expand(escaped);check(item(escaped,1,1).equals("\\5"));check(item(escaped,1,2).equals("\\10"));
        Document missing=parse("<resources>"+labels+"</resources>");
        try{PatrolIntervalOptions.expand(missing);throw new AssertionError();}catch(java.util.NoSuchElementException expectedFailure){}
        check(missing.getElementsByTagName("item").getLength()==7);
        Document bad=parse("<resources>"+labels+array("values","0","1","2","3","4","5","6")+"</resources>");
        try{PatrolIntervalOptions.expandOverrides(bad,names);throw new AssertionError();}catch(IllegalStateException expectedFailure){}
        check(bad.getElementsByTagName("item").getLength()==14);
        System.out.println("PASS "+checks+" patrol resource cases");
    }
}
