package app.morphe.extension.chmate;

public final class VerifyVideoDownloadNames {
    static int checks;
    static void check(boolean value, String label) { if (!value) throw new AssertionError(label); checks++; }
    public static void main(String[] args) {
        String signed="https://video.twimg.com/path/example.mp4?token=secret&expires=123#fragment";
        String name=VideoDownloadNames.filename(signed);
        check(name.startsWith("example-"),"Original video basename");
        check(name.endsWith(".mp4"),"Playable extension");
        check(!name.contains("secret")&&!name.contains("token")&&!name.contains("fragment"),"Signed parameters excluded");
        check(!name.equals(VideoDownloadNames.filename(signed)),"Repeated saves use unique names");
        String[] unsafe={"https://example.com/../../evil.mp4", "https://example.com/%2e%2e%2fwrong%5cname.mp4",
            "https://example.com/%00%0a%0d.mp4", "https://example.com/%E5%8B%95%E7%94%BB.mp4", "https://example.com/a:b*.mp4",
            "https://example.com/", "not a URL", "", null};
        for(String value:unsafe)check(VideoDownloadNames.filename(value).matches("[A-Za-z0-9_-]+\\.mp4"),"Safe path: "+value);
        check(VideoDownloadNames.filename("https://example.com/"+"a".repeat(400)+".mp4").length()<=93,"Long basenames bounded");
        check(VideoDownloadNames.filename("https://example.com/UPPER.MP4").startsWith("UPPER-"),"Uppercase extension");
        System.out.println("PASS "+checks+" video filename tests");
    }
}
