package app.morphe.extension.chmate;

import java.nio.file.*;
import java.util.*;

public class ProgrammableNgChainTest {
    private static int checks;
    private static final String BOARD = "https://egg.5ch.io/android/";
    private static ProgrammableNgScript.Input post(int n, String id, String text) {
        return post(n, id, text, BOARD, 1L, "body");
    }
    private static ProgrammableNgScript.Input post(int n, String id, String text, String board, long thread, String target) {
        Map<String,Object> values = new LinkedHashMap<>();
        values.put("target",target); values.put("boardUrl",board); values.put("threadId",thread);
        values.put("responseNumber",n); values.put("responseId",id);
        return new ProgrammableNgScript.Input(text,values);
    }
    private static ProgrammableNgRuleSet rules(String script) { return rules(script,"body"); }
    private static ProgrammableNgRuleSet rules(String script, String target) {
        return new ProgrammableNgRuleSet(List.of(new ProgrammableNgRuleSet.Rule("example","test",target,true,true,"",script)));
    }
    private static void check(boolean value,String label) { if(!value)throw new AssertionError(label); checks++; }
    private static void expect(ProgrammableNgRuleSet rules, List<ProgrammableNgScript.Input> posts, int... numbers) {
        Set<Integer> expected = new HashSet<>(); for(int n:numbers)expected.add(n);
        boolean[] result=rules.evaluate(posts);
        for(int i=0;i<result.length;i++)check(result[i]==expected.contains(posts.get(i).values.get("responseNumber")),
                "response "+posts.get(i).values.get("responseNumber")+" matched="+result[i]+" expected="+expected+" error="+rules.batchError+" / "+rules.rules.get(0).evaluator.error());
    }
    private static String predicate(boolean poster, boolean chain) {
        return "function(text,options){return {match:text.includes('NGワード'),samePoster:"+poster+",chainReplies:"+chain+"};}";
    }
    public static void main(String[] args) throws Exception {
        String sample=Files.readString(Path.of(args[0]));
        var posts=List.of(post(10,"A","NGワード"),post(20,"A","other"),post(30,"B",">>20"),
                post(40,"C",">>30"),post(50,"D","unrelated"),post(60,"B","another B"));
        expect(rules(sample),posts,10,20,30,40);
        expect(rules(predicate(true,false)),posts,10,20);
        expect(rules(predicate(false,true)),posts,10);
        expect(rules("function(text,options){return text.includes('NGワード');}"),posts,10);
        var direct=List.of(post(10,"A","NGワード"),post(20,"A","other"),post(30,"B",">>10"),post(40,"C",">>30"));
        expect(rules(predicate(false,true)),direct,10,30,40);
        var reversed=new ArrayList<>(posts); Collections.reverse(reversed);
        expect(rules(sample),reversed,10,20,30,40);
        expect(rules(sample),List.of(post(1,"","NGワード"),post(2,"","other"),post(3,"B",">>1")),1,3);
        expect(rules(sample),List.of(post(1,null,"NGワード"),post(2,null,"other")),1);
        expect(rules(sample),List.of(post(1," ","NGワード"),post(2," ","other")),1);
        for(String invalid:List.of("???","DELETED","CAP_USER","OVER1000"))
            expect(rules(sample),List.of(post(1,invalid,"NGワード"),post(2,invalid,"other")),1);
        expect(rules(sample),List.of(post(10,"A","NGワード"),post(20,"A","other",BOARD,2L,"body"),
                post(30,"B",">>10",BOARD,2L,"body")),10);
        expect(rules(sample),List.of(post(10,"A","NGワード"),post(20,"A","other","https://example.org/test/",1L,"body"),
                post(30,"B",">>10","https://example.org/test/",1L,"body")),10);
        expect(rules(sample),List.of(post(10,"A","NGワード"),post(20,"A","other"),post(30,"B","＞＞20"),
                post(40,"C",">>19-20"),post(50,"D",">>2,20、30"),post(60,"E",">>1-9999999")),10,20,30,40,50,60);
        expect(rules(sample),List.of(post(10,"A",">>20 NGワード"),post(20,"B",">>10"),post(30,"C",">>30")),10,20);
        expect(rules(sample),List.of(post(1,"A","NGワード"),post(2,"B",">>00000010")),1);
        var cached=rules(sample);
        expect(cached,List.of(post(10,"A","clean"),post(20,"A","other"),post(30,"B",">>20")));
        expect(cached,List.of(post(10,"A","clean"),post(20,"A","other"),post(30,"B",">>20"),post(40,"A","NGワード")),10,20,30,40);
        expect(cached,List.of(post(10,"A","clean"),post(20,"A","other"),post(30,"B",">>20")));
        expect(cached,posts,10,20,30,40);
        expect(cached,List.of(post(10,"A","NGワード"),post(20,"A","other"),post(30,"B",">>20"),post(40,"C",">>30"),post(70,"E",">>40")),10,20,30,40,70);
        var one=rules(predicate(true,false)).rules.get(0); var two=rules(predicate(false,true)).rules.get(0);
        two=new ProgrammableNgRuleSet.Rule("second",two.name,two.target,true,true,"",two.script);
        expect(new ProgrammableNgRuleSet(List.of(one,two)),posts,10,20);
        for(String malformed:List.of("'yes'","{match:1}","{match:true,samePoster:'true'}","{samePoster:true}","null")) {
            var invalid=rules("function(text,options){return "+malformed+";}"); expect(invalid,posts);
            check(invalid.rules.get(0).evaluator.error()!=null,"invalid return must be reported");
        }
        expect(rules("function(text,options){return {match:false,samePoster:true,chainReplies:true};}"),posts);
        var titles=List.of(post(1,"A","NGワード",BOARD,1,"title"));
        expect(rules("function(text,options){return true;}","title"),titles,1);
        var invalidTitle=rules(sample,"title"); expect(invalidTitle,titles);
        check(invalidTitle.rules.get(0).evaluator.error()!=null,"body-only directives must reject title rules");
        var decoded=ProgrammableNgRuleSet.decode(rules(sample).encode()); expect(decoded,posts,10,20,30,40);
        var scoped=new ProgrammableNgRuleSet(List.of(new ProgrammableNgRuleSet.Rule("scoped","board","body",true,false,"https://example.org/test/",sample)));
        expect(scoped,posts);
        var disabled=new ProgrammableNgRuleSet(List.of(new ProgrammableNgRuleSet.Rule("off","off","body",false,true,"",sample)));
        expect(disabled,posts);
        var large=new ArrayList<ProgrammableNgScript.Input>(); for(int i=1;i<=2000;i++)large.add(post(i,"same","NGワード"));
        boolean[] many=rules(sample).evaluate(large); check(many.length==2000&&many[0]&&many[1999],"2000 same-author matches remain bounded");
        var excessive=new ArrayList<ProgrammableNgScript.Input>(); for(int i=1;i<=400;i++)excessive.add(post(i,"A"+i,(i==1?"NGワード ":"")+">>1-9999999"));
        var bounded=rules(sample); boolean[] limited=bounded.evaluate(excessive);
        check(!bounded.batchError.isEmpty(),"dense reference graph reports limit");
        for(boolean match:limited)check(!match,"dense graph must fail open as a whole");
        expect(bounded,posts,10,20,30,40);
        System.out.println("PASS "+checks+" advanced NG checks, sample, compatibility, scope, cache changes, cycles and limits");
    }
}
