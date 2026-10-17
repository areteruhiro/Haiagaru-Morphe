package app.morphe.extension.chmate;

import java.io.*;
import java.net.*;
import java.nio.file.Files;

/** Live anonymous form/token check; deliberately refuses to open the media upload endpoint. */
public final class MediaUploadPreflight {
    private static final class BeforeMedia extends IOException {}
    public static void main(String[] args) throws Exception {
        File fixture=Files.createTempFile("haiagaru-upload-preflight-",".png").toFile();
        int[] requests={0};
        try {
            Files.write(fixture.toPath(),new byte[]{(byte)137,'P','N','G',13,10,26,10});
            MediaUploadClient client=new MediaUploadClient(url->{
                if(!url.getProtocol().equals("https")||!url.getHost().equals("imgbox.com")) throw new AssertionError("Unexpected host");
                if(url.getPath().equals("/upload/process")) throw new BeforeMedia();
                if(!url.getPath().equals("/") && !url.getPath().equals("/ajax/token/generate")) throw new AssertionError("Unexpected endpoint");
                requests[0]++;return (HttpURLConnection)url.openConnection();
            });
            try { client.upload("imgbox",fixture,"image/png","",false);throw new AssertionError("Media request was not blocked"); }
            catch(BeforeMedia expected) {
                if(requests[0]!=2) throw new AssertionError("Unexpected request count");
                System.out.println("PASS: live imgbox form + token; media endpoint blocked before opening (no image uploaded)");
            }
        } finally { Files.deleteIfExists(fixture.toPath()); }
    }
}
