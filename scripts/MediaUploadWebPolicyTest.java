package app.morphe.extension.chmate;

public final class MediaUploadWebPolicyTest {
    public static void main(String[] args) {
        int n = 0;
        for (String url : new String[]{"https://imgpile.com/", "https://imgpile.com/m/Ab_1", "https://imgpile.com:443/?a=1"}) {
            if (!MediaUploadWebPolicy.page(url)) throw new AssertionError(url);n++;
        }
        for (String url : new String[]{"http://imgpile.com/", "https://imgpile.com.evil.test/", "https://evil.test/", "file:///sdcard/a", "content://images/1", "javascript:alert(1)", "https://user@imgpile.com/", "https://imgpile.com:444/", "https://imgpile.com\\@evil.test/", "https://cdn.imgpile.com/f/a.png", "", null}) {
            if (MediaUploadWebPolicy.page(url)) throw new AssertionError(url);n++;
        }
        if (!MediaUploadWebPolicy.media("https://cdn.imgpile.com/f/Ab-1.mp4")) throw new AssertionError("direct video");n++;
        for (String url : new String[]{"https://evil.test/f/a.png", "https://cdn.imgpile.com/f/a.png?token=secret", "https://imgpile.com/m/a", "http://cdn.imgpile.com/f/a.png"}) {
            if (MediaUploadWebPolicy.media(url)) throw new AssertionError(url);n++;
        }
        for (String url : new String[]{"https://sendvid.com/", "https://sendvid.com/?src=ixfp", "https://sendvid.com/help/faq", "https://sendvid.com/abcd1234"}) {
            if (!MediaUploadWebPolicy.page("sendvid", url)) throw new AssertionError(url);n++;
        }
        for (String url : new String[]{"https://imgpile.com/", "https://sendvid.com.evil.test/", "https://evil.test/", "http://sendvid.com/", "file:///sdcard/a.mp4", "https://user@sendvid.com/", "https://sendvid.com:444/", "javascript:alert(1)"}) {
            if (MediaUploadWebPolicy.page("sendvid", url)) throw new AssertionError(url);n++;
        }
        if (!MediaUploadWebPolicy.share("sendvid", "https://sendvid.com/abcd1234")) throw new AssertionError("public watch link");n++;
        for (String url : new String[]{"https://sendvid.com/abcd1234?secret=private", "https://sendvid.com/abcd1234#secret", "https://sendvid.com/abcd1234/delete", "https://sendvid.com/delete/abcd1234/private", "https://sendvid.com/embed/abcd1234", "https://sendvid.com/help/faq", "https://sendvid.com/", "https://sendvid.com/?src=ixfp", "https://sendvid.com/abcd1234/", "https://sendvid.com/%61bcd1234", "https://evil.test/abcd1234", "https://imgpile.com/abcd1234", "https://sendvid.com/users/sign_out", null}) {
            if (MediaUploadWebPolicy.share("sendvid", url)) throw new AssertionError(url);n++;
        }
        if (MediaUploadWebPolicy.page("imgpile", "https://sendvid.com/abcd1234")) throw new AssertionError("provider isolation");n++;
        if (!MediaUploadWebPolicy.home("sendvid").equals("https://sendvid.com/")) throw new AssertionError("clean home");n++;
        if (!MediaUploadWebPolicy.provider("evil").equals("imgpile")) throw new AssertionError("unknown provider fallback");n++;
        System.out.println("MediaUploadWebPolicy: " + n + " PASS");
    }
}
