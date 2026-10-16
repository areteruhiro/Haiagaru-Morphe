package app.morphe.extension.chmate;

public final class VerifyPatrolImageRules {
    private static int checks;
    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message); checks++;
    }
    public static void main(String[] args) {
        String url = "https://egg.5ch.io/test/read.cgi/android/1792102288/";
        check(PatrolImageRules.eligible(false, "任意のスレ", url, "", ""), "Filter off includes every native patrol thread");
        check(!PatrolImageRules.eligible(true, "任意のスレ", url, "", ""), "Filter on with no conditions does not fetch");
        check(PatrolImageRules.eligible(true, "対象のスレ", url, "対象", ""), "Filter on narrows native patrol threads");
        check(PatrolImageRules.matches("ChMate質問スレ", url, "別のキーワード\nchmate\n", ""), "Multiple keywords, OR, case insensitive");
        check(PatrolImageRules.matches("質問スレ", url, "", "https://egg.5ch.io/test/read.cgi/android/1792102288/l50"), "Response URL matches thread");
        check(PatrolImageRules.matches("質問スレ", url, "", "http://egg.5ch.io/test/read.cgi/android/1792102288/167#x"), "Scheme and response suffix ignored");
        check(!PatrolImageRules.matches("質問スレ", url, " \n\r\n", " \n"), "Empty rules never match everything");
        check(!PatrolImageRules.matches("質問スレ", url, "別スレ", "1792102289"), "Unrelated rules do not match");
        check(PatrolImageRules.matches("質問スレ", url, "別スレ", "1792102288"), "URL fragment independent of title");
        check(PatrolImageRules.imageUrl("https://i.imgur.com/a.jpg?x=1"), "JPEG query");
        check(PatrolImageRules.imageUrl("https://example.com/A.PNG"), "Uppercase extension");
        check(PatrolImageRules.imageUrl("https://pbs.twimg.com/media/abc?format=png&name=orig"), "Twimg format query");
        check(!PatrolImageRules.imageUrl("https://example.com/a.mp4"), "Video excluded");
        check(!PatrolImageRules.imageUrl("file:///a.jpg"), "File URL excluded");
        check(!PatrolImageRules.imageUrl("https://user:password@example.com/a.jpg"), "Embedded credentials excluded");
        check(!PatrolImageRules.imageUrl("https://example.com/noextension"), "Unknown page excluded");
        String folder = PatrolImageRules.folder("../a/b:*?\\c", url);
        check(!folder.contains("/") && !folder.contains("\\") && !folder.startsWith("."), "Safe folder");
        check(!PatrolImageRules.folder("same", url).equals(PatrolImageRules.folder("same", url + "other")), "Same title different threads isolated");
        check(PatrolImageRules.hash(url).equals(PatrolImageRules.hash(url)), "Stable persistent dedup name");
        check(PatrolImageRules.hash(url).length() == 64, "Full SHA256 image key");
        check(PatrolImageRules.threadUrl("https://talk.jp/boards/operation/1736158664/49").equals("talk.jp/boards/operation/1736158664/"), "Talk response link normalized");
        System.out.println("PASS " + checks + " patrol image policy checks");
    }
}
