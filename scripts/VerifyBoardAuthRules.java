package app.morphe.extension.chmate;
public final class VerifyBoardAuthRules {
    private static int checks;
    private static void check(boolean ok,String label){if(!ok)throw new AssertionError(label);checks++;}
    private static void invalid(String value){try{BoardAuthRules.validate(value);throw new AssertionError("accepted unsafe URL");}catch(IllegalArgumentException expected){checks++;}}
    public static void main(String[] args){
        check(BoardAuthRules.origin("https://BBS.EDDIBB.CC:443/liveedge/").equals("https://bbs.eddibb.cc"),"origin normalization");
        check(BoardAuthRules.origin("https://board.example:8443/a/").equals("https://board.example:8443"),"port isolation");
        check(BoardAuthRules.preset("https://bbs.eddibb.cc/liveedge").equals("https://bbs.eddibb.cc/auth-code"),"Edge preset");
        check(BoardAuthRules.preset("https://bbs.eddibb.cc.evil.example/").isEmpty(),"no suffix confusion");
        check(BoardAuthRules.preset("https://other.example/").isEmpty(),"no guessed path for unknown boards");
        invalid("http://board.example/auth");invalid("javascript:alert(1)");invalid("file:///data/data/auth");
        invalid("https://user:password@board.example/auth");invalid("https://board.example/auth#secret");invalid(null);
        check(BoardAuthRules.required("認証が必要です"),"Japanese error");
        check(BoardAuthRules.required("認証コードを入力してください"),"code error");
        check(BoardAuthRules.required("Authentication Required"),"English error");
        check(!BoardAuthRules.required("書き込みに成功しました"),"success ignored");
        check(!BoardAuthRules.required(null),"null ignored");
        check(!BoardAuthRules.required("x".repeat(65537)+"認証コード"),"bounded scan");
        check(BoardAuthRules.suggested("https://bbs.eddibb.cc/liveedge/","認証が必要です").endsWith("/auth-code"),"known board fallback");
        check(BoardAuthRules.suggested("https://other.example/a/","認証が必要です").isEmpty(),"unknown board requires configuration");
        check(BoardAuthRules.suggested("https://board.example/a/","認証コード: https://board.example/check?x=1&amp;y=2").equals("https://board.example/check?x=1&y=2"),"same origin link");
        check(BoardAuthRules.suggested("https://board.example/a/","認証コード: https://evil.example/check").isEmpty(),"cross site suggestion rejected");
        check(BoardAuthRules.suggested("https://board.example/a/","認証コード: /auth-code").equals("https://board.example/auth-code"),"relative auth link");
        check(BoardAuthRules.suggested("https://board.example/a/","https://board.example/check").isEmpty(),"ordinary link ignored");
        check(BoardAuthRules.validate("https://board.example/auth?x=1").equals("https://board.example/auth?x=1"),"explicit configurable URL");
        System.out.println("PASS "+checks+" external-board auth policy checks");
    }
}
