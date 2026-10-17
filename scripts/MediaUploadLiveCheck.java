package app.morphe.extension.chmate;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import javax.imageio.ImageIO;

/** Explicit opt-in ONLY: sends a generated 16x16 test PNG, never user media. */
public final class MediaUploadLiveCheck {
    public static void main(String[] args) throws Exception {
        if(args.length!=1 || !args[0].equals("--upload-generated-test-image-to-imgbox"))
            throw new IllegalArgumentException("Explicit live-upload flag is required");
        File fixture=Files.createTempFile("haiagaru-generated-upload-",".png").toFile();
        try {
            BufferedImage image=new BufferedImage(16,16,BufferedImage.TYPE_INT_RGB);
            for(int y=0;y<16;y++)for(int x=0;x<16;x++)image.setRGB(x,y,((x^y)&1)==0?0x4477aa:0xffffff);
            ImageIO.write(image,"png",fixture);
            var result=new MediaUploadClient().upload("imgbox",fixture,"image/png","",false);
            System.out.println("PASS generated imgbox upload: "+result.getString("link"));
        } finally { Files.deleteIfExists(fixture.toPath()); }
    }
}
