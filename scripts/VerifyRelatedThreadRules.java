package app.morphe.extension.chmate;
public class VerifyRelatedThreadRules {
 static int checks;static void check(boolean b,String s){if(!b)throw new AssertionError(s);checks++;}
 public static void main(String[] args){
  String title="chmate morphe、更新ｗｗｗｗｗｗｗｗｗ";
  String candidate="chmateのスレッド保持数最大1001←これwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwww";
  long id=1792291793L,next=1792294432L;
  check(RelatedThreadRules.score(title,id,candidate,next)>0,"reported title change");
  check(RelatedThreadRules.score(title,id,"天気について語るスレ",next)==0,"unrelated");
  check(RelatedThreadRules.score("雑談 更新",id,"雑談 更新",id)==0,"self excluded");
  check(RelatedThreadRules.score(title,id,candidate,id-1)==0,"older excluded");
  check(RelatedThreadRules.score(title,id,candidate,id+8*86400)==0,"stale excluded");
  check(RelatedThreadRules.score("更新 総合",id,"更新 スレ",next)==0,"generic words excluded");
  check(RelatedThreadRules.score("ChMate part12 [abc★]",id,"chmate Part13 [xyz★]",next)==100,"part reporter normalized");
  check(RelatedThreadRules.source("https://bbs.eddibb.cc/test/read.cgi/liveedge/"+id+"/")!=null,"source");
  check(RelatedThreadRules.source("https://evil.example/test/read.cgi/liveedge/"+id+"/")==null,"host scoped");
  check(RelatedThreadRules.source("https://bbs.eddibb.cc@evil.example/test/read.cgi/liveedge/"+id+"/")==null,"userinfo");
  String subject=next+".dat<>"+candidate+" (1)\n"+(next+1)+".dat<>別の話題 (99)\n"+next+".dat<>"+candidate+" (2)\n";
  var rows=RelatedThreadRules.candidates(subject,title,id);
  check(rows.size()==1&&rows.get(0).id==next,"subject unique candidates");
  check(rows.get(0).title.equals(candidate),"response count stripped");
  check(!RelatedThreadRules.listed(subject,id),"missing source");
  check(RelatedThreadRules.listed(subject+id+".dat<>"+title+" (100)\n",id),"live source");
  check(RelatedThreadRules.candidates("<html>Error</html>",title,id).isEmpty(),"invalid subject");
  System.out.println("PASS "+checks+" related thread rules");
 }
}
