package app.morphe.extension.chmate;

public final class ThreadWatchRulesTest {
    private static int checks;
    private static void check(boolean value){checks++;if(!value)throw new AssertionError("Check "+checks);}
    public static void main(String[] args){
        ThreadWatchRules.Target edge=ThreadWatchRules.target("http://bbs.eddibb.cc/test/read.cgi/liveedge/1793457978/123");
        check(edge!=null&&edge.url.equals("https://bbs.eddibb.cc/test/read.cgi/liveedge/1793457978/"));
        check(edge.subject.equals("https://bbs.eddibb.cc/liveedge/subject.txt"));
        check(ThreadWatchRules.target("https://egg.5ch.io/test/read.cgi/android/1793113633/l50")!=null);
        check(ThreadWatchRules.target("https://mercury.bbspink.com/test/read.cgi/test/1793113633/")!=null);
        for(String invalid:new String[]{"javascript:alert(1)","https://user@host/test/read.cgi/a/1793113633/","https://host:123/test/read.cgi/a/1793113633/","https://host/test/bbs.cgi","https://host/test/read.cgi/../1793113633/","https://host/test/read.cgi/a/notanid/",null})check(ThreadWatchRules.target(invalid)==null);
        java.util.Map<String,Integer> counts=ThreadWatchRules.counts("1793457978.dat<>title (with brackets) (83)\r\n1793113633.dat<>test (1000)\n1793113634.dat<>bad (abc)\n");
        check(counts.size()==2&&counts.get("1793457978")==83&&counts.get("1793113633")==1000);
        for(String invalid:new String[]{"<!doctype html><html>challenge</html>","<html>\n1793457978.dat<>fake (123)\n</html>","unavailable",null})check(ThreadWatchRules.counts(invalid).isEmpty());
        check(ThreadWatchRules.increase(-1,10)==0);
        check(ThreadWatchRules.increase(10,10)==0);
        check(ThreadWatchRules.increase(10,5)==0);
        check(ThreadWatchRules.increase(10,15)==5);
        System.out.println("ThreadWatchRules: "+checks+" passed");
    }
}
