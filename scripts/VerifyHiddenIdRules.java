import app.morphe.extension.chmate.HiddenIdRules;

public final class VerifyHiddenIdRules {
    private static int checks;
    private static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
        checks++;
    }
    public static void main(String[] args) {
        for (String host : new String[]{"egg.5ch.io", "greta.5ch.net", "5ch.io", "EGG.5CH.IO"})
            check(HiddenIdRules.fiveCh("https://" + host + "/test/read.cgi/a/123/"), "5ch host " + host);
        for (String url : new String[]{"https://5ch.io.evil.test/", "https://fake5ch.io/", "https://5ch.io@evil.test/",
                "https://user@egg.5ch.io/", "file:///5ch.io/a", "https://talk.jp/boards/a/", "https://bbs.eddibb.cc/liveedge/",
                "https://d1ch.cc/edge/", "not a url", "https://", "ftp://egg.5ch.io/", "https://2ch.net/"})
            check(!HiddenIdRules.fiveCh(url), "reject other or ambiguous host " + url);
        check(!HiddenIdRules.fiveCh(null), "unknown origin is not 5ch");
        for (String id : new String[]{"Abc12345", "abcDEF+/0", "AAbbcCdDO", "aabbccddOP"})
            check(HiddenIdRules.regularId(id), "normal IDs including symbols");
        for (String id : new String[]{"", "???", "???0", "DELETED", "CAP_USER", "OVER1000", "ID:none", "短い", " abcdefgh"})
            check(!HiddenIdRules.regularId(id), "not regular ID " + id);
        for (String id : new String[]{null, "", "  ", "???", "???0", "???O", "???P"})
            check(HiddenIdRules.candidate(2, id, "名無しさん", "2026/10/17(土) 12:34:56"), "absent ID " + id);
        for (String id : new String[]{"Abc12345", "DELETED", "CAP_USER", "OVER1000", "anonymous", "0"})
            check(!HiddenIdRules.candidate(2, id, "名無しさん", "2026/10/17"), "not absent " + id);
        check(!HiddenIdRules.candidate(0, "", "", "2026/10/17"), "system number");
        check(!HiddenIdRules.candidate(3, "", "あぼーん", "あぼーん"), "deleted response");
        check(!HiddenIdRules.candidate(3, "", "管理人 ★", "2026/10/17"), "staff excluded");
        check(!HiddenIdRules.candidate(3, "", "", null), "missing date fails safe");
        for (int bits = 0; bits < 16; bits++) {
            boolean enabled=(bits&1)!=0, board=(bits&2)!=0, ids=(bits&4)!=0, candidate=(bits&8)!=0;
            int original=0x2040;
            check(HiddenIdRules.merge(enabled,board,ids,candidate,original)==
                    (bits==15 ? original|0x1008 : original), "enable/host/evidence/candidate matrix " + bits);
        }
        check(HiddenIdRules.merge(false,true,true,true,0x88)==0x88, "OFF keeps native NG");
        check(HiddenIdRules.merge(true,true,true,true,0x1008)==0x1008, "idempotent");
        System.out.println("PASS " + checks + " hidden-ID policy checks");
    }
}
