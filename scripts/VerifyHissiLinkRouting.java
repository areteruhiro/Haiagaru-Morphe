import app.morphe.extension.chmate.HissiLinkRouting;

/** Standalone regression check for links in Hissi's thread and response lists. */
public class VerifyHissiLinkRouting {
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    public static void main(String[] args) {
        for (String url : new String[] {
                "https://rosie.5ch.net/test/read.cgi/operatex/1600326394/l50",
                "http://rosie.5ch.net/test/read.cgi/operatex/1600326394/24",
                "https://egg.5ch.io/test/read.cgi/android/1781182873/3",
                "http://aoi.bbspink.com/test/read.cgi/3shuchaku/1427347314/19",
                "https://talk.jp/boards/newsplus/1789378339",
                "https://itest.5ch.io/egg/test/read.cgi/android/1781182873/",
        }) {
            check(HissiLinkRouting.isThreadUrl(url), "Expected ChMate thread: " + url);
        }
        for (String url : new String[] {
                "http://hissi.org/read.php/operatex/20200917/UlBjVy9TdkI.html",
                "https://hissi.org/",
                "https://5ch.io/",
                "https://evil5ch.io/test/read.cgi/android/1781182873/",
                "https://egg.5ch.io.example/test/read.cgi/android/1781182873/",
                "https://user@egg.5ch.io/test/read.cgi/android/1781182873/",
                "javascript:alert(1)",
                null,
        }) {
            check(!HissiLinkRouting.isThreadUrl(url), "Unexpected ChMate thread: " + url);
        }
        System.out.println("PASS: Hissi thread, response, itest, and non-thread links");
    }
}
