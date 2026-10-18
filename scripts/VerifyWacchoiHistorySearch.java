package app.morphe.extension.chmate;

public class VerifyWacchoiHistorySearch {
    public static void main(String[] args){
        String text="下4桁 Q8ld android\nID:HKq+ABFo0\nChMate 質問スレ\n動画の本文 https://example.com/a";
        String[] yes={"","Q8LD","q8ld android","質問 本文","id:hkq+abfo0","動画　本文","https://example.com/a","["};
        for(int i=0;i<yes.length-1;i++)if(!WacchoiHistorySearch.matches(text,yes[i]))throw new AssertionError(yes[i]);
        for(String no:new String[]{"missing","android missing","[","Q8.*"})if(WacchoiHistorySearch.matches(text,no))throw new AssertionError(no);
        if(WacchoiHistorySearch.matches(null,"something")||!WacchoiHistorySearch.matches(null,""))throw new AssertionError("null");
        System.out.println("PASS: 13 history search cases (literal, case folding, AND, full-width whitespace, empty/null)");
    }
}
