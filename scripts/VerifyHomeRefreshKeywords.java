import app.morphe.extension.chmate.HomeRefreshKeywords;
import java.util.*;

public final class VerifyHomeRefreshKeywords {
 static int checks;
 static void equal(Object expected,Object actual,String label){checks++;if(!Objects.equals(expected,actual))throw new AssertionError(label+": "+expected+" != "+actual);}
 public static void main(String[] args){
  Object first=new Object(),second=new Object();
  equal(null,HomeRefreshKeywords.refresh(first),"no snapshot leaves null unchanged");
  ArrayList<String> keywords=new ArrayList<>(Arrays.asList("ChMate","Android"));
  equal(keywords,HomeRefreshKeywords.remember(first,keywords),"normal native list preserved");
  keywords.clear();
  equal(Arrays.asList("ChMate","Android"),HomeRefreshKeywords.refresh(first),"refresh keeps defensive snapshot");
  equal(null,HomeRefreshKeywords.refresh(second),"separate HOME model does not inherit keywords");
  List<?> copy=HomeRefreshKeywords.refresh(first);copy.clear();
  equal(Arrays.asList("ChMate","Android"),HomeRefreshKeywords.refresh(first),"consumer mutation does not corrupt snapshot");
  for(int i=0;i<10;i++){List<?> current=HomeRefreshKeywords.refresh(first);HomeRefreshKeywords.remember(first,current);equal(Arrays.asList("ChMate","Android"),current,"repeated refresh "+i);}
  HomeRefreshKeywords.remember(first,Collections.emptyList());
  equal(Collections.emptyList(),HomeRefreshKeywords.refresh(first),"all keywords removed stays empty");
  HomeRefreshKeywords.remember(first,Arrays.asList("New"));
  equal(Arrays.asList("New"),HomeRefreshKeywords.refresh(first),"edited keywords replace snapshot");
  HomeRefreshKeywords.remember(first,null);equal(null,HomeRefreshKeywords.refresh(first),"native null when all keywords removed invalidates snapshot");
  equal(null,HomeRefreshKeywords.remember(null,null),"null owner unchanged");
  System.out.println("PASS "+checks+" HOME refresh keyword snapshot checks");
 }
}
