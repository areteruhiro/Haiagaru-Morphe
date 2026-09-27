import app.morphe.extension.chmate.KyodemoRouting;

/** Standalone checks for Kyodemo's board and ID-search URL mapping. */
public class VerifyKyodemoRouting {
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    public static void main(String[] args) {
        check("e_e_liveedge".equals(KyodemoRouting.boardSlug("bbs.eddibb.cc", "liveedge")),
                "Edge board");
        check("s_anime_11177".equals(KyodemoRouting.boardSlug(
                "jbbs.shitaraba.net", "anime/11177")), "Shitaraba board");
        check("m_tokyo".equals(KyodemoRouting.boardSlug("tokyo.machi.to", "tokyo")),
                "Machi BBS board");
        check("o_newsplus".equals(KyodemoRouting.boardSlug("hayabusa.open2ch.net", "newsplus")),
                "Open2ch board");
        check("v_news4ssr".equals(KyodemoRouting.boardSlug("ex14.vip2ch.com", "news4ssr")),
                "VIP service board");
        check("p_3shuchaku".equals(KyodemoRouting.boardSlug("aoi.bbspink.com", "3shuchaku")),
                "BBSPINK board");
        check("i_5chnewsplus".equals(KyodemoRouting.boardSlug("5chan.jp", "5ch_newsplus")),
                "ItsuMo ch board");
        check("e_y_yaruzatsu01".equals(KyodemoRouting.boardSlug(
                "yaruozatsudan.com", "yaruzatsu01")), "Yaruo board");
        check("https://5chan.jp/test/read.cgi/5ch_newsplus/1684812738/1/".equals(
                KyodemoRouting.sourceThreadUrl("5chan.jp", "5ch_newsplus",
                        "https://www.kyodemo.net/sdemo/r/i_5chnewsplus/1684812738/1")),
                "ItsuMo ch result post should open its original thread");
        check("https://jbbs.shitaraba.net/bbs/read.cgi/anime/11177/1707378532/".equals(
                KyodemoRouting.sourceThreadUrl("jbbs.shitaraba.net", "anime/11177",
                        "https://www.kyodemo.net/sdemo/r/s_anime_11177/1707378532/")),
                "Shitaraba result should use its original URL shape");
        check(KyodemoRouting.sourceThreadUrl("5chan.jp", "5ch_newsplus",
                "https://evil.example/sdemo/r/i_5chnewsplus/1684812738/") == null,
                "External lookalike result links must not be rewritten");
        check("https://www.kyodemo.net/sdemo/b/e_e_liveedge/?hi=op3na4RR%2F&key=1707378532&date=20240208"
                        .equals(KyodemoRouting.idSearchUrl("bbs.eddibb.cc", "liveedge",
                                "op3na4RR/", "1707378532", "20240208")),
                "ID must be safely encoded with board, thread, and date context");
        check(KyodemoRouting.boardSlug("talk.jp", "newsplus") == null,
                "Unsupported sites must not be given a false result");
        check(KyodemoRouting.boardSlug("evil.bbs.eddibb.cc", "liveedge") == null,
                "Lookalike hosts must not be accepted");
        check(KyodemoRouting.boardSlug("jbbs.shitaraba.net", "anime") == null,
                "Shitaraba needs both category and board number");
        System.out.println("PASS: Kyodemo board mapping and ID-search URLs");
    }
}
