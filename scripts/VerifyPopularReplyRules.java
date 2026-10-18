package app.morphe.extension.chmate;
import java.util.*;
public class VerifyPopularReplyRules {
 public static void main(String[] args){
  Map<Integer,String> posts=new LinkedHashMap<>();
  posts.put(1,"OP");posts.put(2,">>1");posts.put(3,">>2");posts.put(4,">>2");
  Map<Integer,Integer> c=PopularReplyRules.count(posts);
  require(c.get(1)==1 && c.get(2)==2,"no transitive count");
  posts.put(5,">>1 >>1 >>1");c=PopularReplyRules.count(posts);
  require(c.get(1)==2,"same source counted once");
  posts.put(6,">>1-3 >>2,4");c=PopularReplyRules.count(posts);
  require(c.get(1)==3 && c.get(2)==3 && c.get(3)==1 && c.get(4)==1,"range and list dedup");
  posts.put(7,">>7 >>8 >>0 >>9999999");c=PopularReplyRules.count(posts);
  require(!c.containsKey(7)&&!c.containsKey(8)&&!c.containsKey(0),"self future missing");
  posts.put(8,"＞＞1");c=PopularReplyRules.count(posts);require(c.get(1)==4,"fullwidth");
  posts.put(9,">>2-9999999");c=PopularReplyRules.count(posts);require(c.get(8)==1,"huge range bounded");
  require(PopularReplyRules.count(Collections.emptyMap()).isEmpty(),"empty");
  System.out.println("PASS 7 direct-reply popularity cases");
 }
 static void require(boolean ok,String label){if(!ok)throw new AssertionError(label);}
}
