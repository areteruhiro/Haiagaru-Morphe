package app.morphe.extension.chmate;
import java.util.*;import java.util.regex.*;

public final class SharedNgFalsePositiveTest {
    static int checks;static void ok(boolean value,String label){checks++;if(!value)throw new AssertionError(label);}
    public static void main(String[] args){
        String expression="左のネ|([チョンパヨ].*){5}";
        String report="以前の変更でこうありましたが、ChMate標準バックアップのreferenceファイルはパッケージ名を変更しててもオリジナルと同名にして参照も同様にしてほしいです 検証で他のパッケージ名のchmate部分のみを復活する際、元の名前で共通してたほうが都合がいいです パッケージ名変更時、ChMate標準バックアップが実際の標準設定ストアを参照するよう修正しました。XML名は「変更後のパッケージ名_preferences.xml」です。元のパッケージ名の場合は従来のXML名です。";
        ok(Pattern.compile(expression).matcher(report).find(),"reported prose really matches the supplied regex");
        ok(NgRegexSafety.overbroadKeyword(expression),"alternation does not make a broad branch specific");
        var rule=new SharedNgRules.Rule("a".repeat(64),"*","keyword",expression,"",1,false,1,1,true);
        ok(!rule.expressionMatches(report),"online rule no longer hides backup discussion");
        var index=new SharedNgIndex(List.of(rule),1,Set.of(),Set.of(),255);
        ok(!index.matches("5ch/all","ID","name",report),"indexed shared matching follows same policy");
        ok(SharedNgIndex.matchingRules(List.of(rule),1,Set.of(),Set.of(),255,"5ch/all","ID","name",report).isEmpty(),"NG explanation does not claim a skipped rule matched");
        for(String broad:List.of("([チョンパヨ].*){5}","(?s)([あいうえお].*){10}","^(?:[あいう].*){5,}$","普通の条件|([あいう].*){5}"))ok(NgRegexSafety.overbroadKeyword(broad),"narrow scatter family detected");
        for(String specific:List.of("パッケージ名","(?:スパム宣伝.*){5}","差別用語([あいう].*){5}","[あいう]{5}","(.)\\1{20}","(?:宣伝|広告){5}","\\Q([あいう].*){5}\\E","左のネ\\|([あいう].*){5}"))ok(!NgRegexSafety.overbroadKeyword(specific),"other regexes are preserved: "+specific);
        var specific=new SharedNgRules.Rule("b".repeat(64),"*","keyword","(?:スパム宣伝.*){5}","",1,false,1,1,true);
        ok(specific.expressionMatches("スパム宣伝".repeat(5)),"specific repeated phrase remains effective");
        ok(rule.value.equals(expression)&&rule.votes==1&&!rule.revoked,"original rule and votes are not rewritten");
        System.out.println("PASS "+checks+" shared-regex false-positive/compatibility checks");
    }
}
