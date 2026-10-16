import app.morphe.extension.chmate.BoardAddress;

public final class VerifyBoardAddress {
    static int checks;
    static void check(boolean ok,String label){if(!ok)throw new AssertionError(label);checks++;}
    static void bad(String url){try{BoardAddress.parse(url);throw new AssertionError("Accepted "+url);}catch(IllegalArgumentException good){checks++;}}
    public static void main(String[] args){
        BoardAddress b=BoardAddress.parse(" https://BBS.example.org/liveedge ");
        check(b.url.equals("https://bbs.example.org/liveedge/"),"canonical URL");
        check(b.server.equals("bbs.example.org"),"server");check(b.path.equals("liveedge"),"path");
        check(BoardAddress.parse("http://bbs.example.org:8080/a/b/").key().equals("bbs.example.org:8080/a/b"),"nested board and port");
        check(BoardAddress.parse("https://jbbs.shitaraba.net/test/123/").engine().equals("shitaraba"),"shitaraba");
        check(BoardAddress.parse("https://egg.5ch.io/android/").engine().equals("5ch"),"5ch");
        check(BoardAddress.parse("https://talk.jp/boards/operation/").engine().equals("talk"),"Talk");
        for(String url:new String[]{"file:///tmp/a/","javascript:alert(1)","https://a.example/","https://a.example/test/read.cgi/a/1234567890/",
                "https://user:pass@a.example/a/","https://a.example/a/?q=1","https://a.example/a/#x","https://a.example/a/../b/",
                "https://a.example/%2f/","https://a.example/a//b/","https://a.example/a/subject.txt","https://a.example:99999/a/"})bad(url);
        System.out.println("PASS "+checks+" board address checks");
    }
}
