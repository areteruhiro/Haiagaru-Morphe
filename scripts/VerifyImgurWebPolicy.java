import app.morphe.extension.chmate.ImgurWebPolicy;
import java.net.URI;
import java.net.http.*;

public final class VerifyImgurWebPolicy {
    private static int checks;
    private static void check(boolean value) { checks++; if (!value) throw new AssertionError("case " + checks); }
    public static void main(String[] args) throws Exception {
        check(ImgurWebPolicy.loginUrl("https://imgur.com/signin"));
        check(ImgurWebPolicy.loginUrl("https://accounts.google.com/o/oauth2/auth?client_id=test"));
        check(ImgurWebPolicy.googleLoginUrl("https://accounts.google.com/signin/v2/challenge/pwd"));
        for (String host : new String[]{"accounts.google.co.jp", "accounts.youtube.com"}) {
            check(ImgurWebPolicy.loginUrl("https://" + host + "/accounts/SetSID?fixture=1"));
            check(ImgurWebPolicy.googleLoginUrl("https://" + host + "/accounts/SetSID?fixture=1"));
            check(!ImgurWebPolicy.imgurLoginUrl("https://" + host + "/accounts/SetSID"));
            check(!ImgurWebPolicy.imgurLoginReturnUrl("https://" + host + "/accounts/SetSID"));
            for (String bad : new String[]{"http://" + host + "/", "https://" + host + ".evil/",
                    "https://evil@" + host + "/", "https://" + host + ":444/"})
                check(!ImgurWebPolicy.loginUrl(bad));
        }
        check(!ImgurWebPolicy.loginUrl("https://www.youtube.com/"));
        check(!ImgurWebPolicy.loginUrl("https://www.google.co.jp/"));
        check(!ImgurWebPolicy.googleLoginUrl("https://accounts.google.com.evil/"));
        check(ImgurWebPolicy.imgurLoginUrl("https://imgur.com/signin/google"));
        check(!ImgurWebPolicy.imgurLoginUrl("https://accounts.google.com/"));
        check(!ImgurWebPolicy.imgurLoginUrl("https://evil/"));
        check(!ImgurWebPolicy.imgurLoginReturnUrl("https://imgur.com/signin"));
        check(!ImgurWebPolicy.imgurLoginReturnUrl("https://imgur.com/signin/"));
        check(ImgurWebPolicy.imgurLoginReturnUrl("https://imgur.com/signin/google?code=fixture"));
        for (String bad : new String[]{"https://accounts.google.com.evil/", "https://google.com/",
                "https://evil@accounts.google.com/", "http://accounts.google.com/", "https://accounts.google.com:444/"})
            check(!ImgurWebPolicy.loginUrl(bad));
        for (String bad : new String[]{"http://imgur.com/signin", "https://imgur.com.evil/signin", "https://evil@imgur.com/", "https://imgur.com:444/", "javascript:alert(1)", "file:///x"})
            check(!ImgurWebPolicy.loginUrl(bad));
        check("https://s.imgur.com/desktop-assets/js/main.abc123.js".equals(ImgurWebPolicy.scriptUrl("<script src=https://s.imgur.com/desktop-assets/js/main.abc123.js>")));
        check(ImgurWebPolicy.scriptUrl("https://evil/s.imgur.com/main.foo.js") == null);
        check("0123456789abcde".equals(ImgurWebPolicy.webClientId("hH:()=>w,other:()=>x});var r=0,w=\"0123456789abcde\"")));
        check("0123456789abcde".equals(ImgurWebPolicy.webClientId("hH:()=>$w});var r=0,$w=\"0123456789abcde\"")));
        check(ImgurWebPolicy.webClientId("hH:()=>w,w=\"not-a-client-id\"") == null);
        check(ImgurWebPolicy.ownedBy(1, "user", 1, "user"));
        check(!ImgurWebPolicy.ownedBy(1, "user", 2, "user"));
        check(ImgurWebPolicy.ownedBy(1, "user", 0, "User"));
        check(!ImgurWebPolicy.ownedBy(1, "user", 0, ""));
        check(!ImgurWebPolicy.ownedBy(0, "user", 0, "user"));
        check(ImgurWebPolicy.mediaUrl("https://i.imgur.com/abc123.png"));
        check(ImgurWebPolicy.mediaUrl("https://i.imgur.com/abc123.mp4"));
        for (Object done : new Object[]{null, false, "false", "FALSE", 0, "null"}) check(!ImgurWebPolicy.processing(done));
        for (Object pending : new Object[]{true, "true", 1, "pending"}) check(ImgurWebPolicy.processing(pending));
        check(ImgurWebPolicy.imageId("abc123"));
        check(!ImgurWebPolicy.imageId("../secret"));
        check("https://i.imgur.com/Abc123m.jpg".equals(ImgurWebPolicy.thumbnailUrl("Abc123", "https://i.imgur.com/Abc123.png", false)));
        check("https://i.imgur.com/Video1m.jpg".equals(ImgurWebPolicy.thumbnailUrl("Video1", "https://i.imgur.com/Video1.mp4", false)));
        check(ImgurWebPolicy.thumbnailUrl("Video1", "https://i.imgur.com/Video1.mp4", true) == null);
        check(ImgurWebPolicy.thumbnailUrl("../secret", "https://i.imgur.com/Video1.mp4", false) == null);
        check(ImgurWebPolicy.thumbnailUrl("Video1", "https://evil/Video1.mp4", false) == null);
        check(ImgurWebPolicy.thumbnailUrl("Video1", "http://i.imgur.com/Video1.mp4", false) == null);
        check(ImgurWebPolicy.thumbnailUrl("Video1", "https://i.imgur.com/Other.mp4", false) == null);
        check(ImgurWebPolicy.thumbnailUrl(null, null, false) == null);
        for (String bad : new String[]{"http://i.imgur.com/a.jpg", "https://i.imgur.com.evil/a.jpg", "https://user@i.imgur.com/a.jpg", "https://i.imgur.com:443/a.jpg", "https://i.imgur.com/a/album", "https://i.imgur.com/a.svg"})
            check(!ImgurWebPolicy.mediaUrl(bad));
        if (args.length > 0 && args[0].equals("--live")) {
            HttpClient client = HttpClient.newHttpClient();
            String agent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/140.0.0.0 Safari/537.36";
            var page = client.send(HttpRequest.newBuilder(URI.create("https://imgur.com/upload")).header("User-Agent", agent).GET().build(), HttpResponse.BodyHandlers.ofString());
            String html = page.body();
            System.out.println("Public HTML status=" + page.statusCode() + " bytes=" + html.length());
            String script = ImgurWebPolicy.scriptUrl(html); check(script != null);
            String javascript = client.send(HttpRequest.newBuilder(URI.create(script)).GET().build(), HttpResponse.BodyHandlers.ofString()).body();
            check(ImgurWebPolicy.webClientId(javascript) != null);
            System.out.println("PASS: current public web config recognized (no login, no upload)");
        }
        System.out.println("PASS: Imgur web policy " + checks + " cases");
    }
}
