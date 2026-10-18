package app.morphe.extension.chmate;
import java.nio.file.*;
import java.util.regex.*;
public class VerifyEdgeWacchoiSearch {
 static int checks;
 static void check(boolean b,String label){checks++;if(!b)throw new AssertionError(label);}
 public static void main(String[] args)throws Exception {
  String post="<div class=\"r-head\"><a href=\"/sdemo/b/e_e_liveedge/?hi=&amp;wc=L20+AbC1-x%2B%2F9\">name</a><span class=\"clid\">6Nd.uJWxg</span></div><div class=\"clmess\">quote <a href='?wc=L20+fake-FAKE'>fake</a></div>";
  String value=WacchoiSearch.edgeWacchoi(post);
  check(value.equals("AbC1-x+/9"),"encoded + and / preserved");
  check(WacchoiSearch.matchesEdgeWacchoi("AbC1",value,true),"upper full");
  check(!WacchoiSearch.matchesEdgeWacchoi("AbC1",value,false),"upper/lower isolation");
  check(WacchoiSearch.matchesEdgeWacchoi("x+/9",value,false),"lower full");
  check(!WacchoiSearch.matchesEdgeWacchoi("x+/9",value,true),"lower/upper isolation");
  check(WacchoiSearch.matchesEdgeWacchoi("Ab",value,true),"optional partial");
  check(!WacchoiSearch.matchesEdgeWacchoi("abc1",value,true),"case preserved");
  check(WacchoiSearch.matchesEdgeWacchoi("AbC1-x+/9",value,false),"full slip");
  check(WacchoiSearch.edgeWacchoi("<div class='clmess'><a href='?wc=fake-FAKE'>quote</a></div>").isEmpty(),"ignore body quote");
  check(WacchoiSearch.edgeWacchoi("<a href='?wc=%ZZ'>bad</a><div class='clmess'>body</div>").isEmpty(),"malformed escape");
  check(!WacchoiSearch.matchesEdgeWacchoi("AbC1","",true),"missing slip excluded");
  check(WacchoiSearch.matchesEdgeWacchoi("","",false),"empty query allowed");
  check(WacchoiSearch.matchesEdgeId("ID:6Nd.uJWxg","6Nd.uJWxg"),"ID mode preserved");
  int count=0;
  if(args.length>0) {
  String html=Files.readString(Path.of(args[0]));
  Matcher posts=Pattern.compile("(?is)<div\\s+class=['\"]post\\s+p([0-9]+)\\b[^'\"]*['\"][^>]*>").matcher(html);
  while(posts.find()) {String slip=WacchoiSearch.edgeWacchoi(html.substring(posts.end())); check(!slip.isEmpty(),"live slip post "+posts.group(1));count++;}
  check(count==50,"50 live continuation posts");
  }
  System.out.println("PASS "+checks+" Edge ID/upper/lower/encoding/body-exclusion checks including "+count+" real posts");
 }
}
