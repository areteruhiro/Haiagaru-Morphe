/** Android framework only: no Haiagaru or Frida, no media files or network. */
public class VerifyNativeMedia {
    public static void main(String[] args)throws Exception{
        if(args.length>1&&args[0].equals("frame")){
            android.media.MediaMetadataRetriever r=new android.media.MediaMetadataRetriever();
            try(java.io.FileInputStream input=new java.io.FileInputStream(args[1])){r.setDataSource(input.getFD(),0,new java.io.File(args[1]).length());android.graphics.Bitmap b=r.getScaledFrameAtTime(0,android.media.MediaMetadataRetriever.OPTION_CLOSEST,320,320);if(b==null)throw new AssertionError("no frame");System.out.println("PASS: video frame "+b.getWidth()+"x"+b.getHeight());b.recycle();}finally{r.release();}
            System.exit(0);
        }
        if(args.length>0&&args[0].equals("player")){android.os.Looper.prepare();android.media.MediaPlayer p=new android.media.MediaPlayer();p.release();System.out.println("PASS: framework MediaPlayer constructor/release");}
        else{android.media.MediaMetadataRetriever r=new android.media.MediaMetadataRetriever();r.release();System.out.println("PASS: framework MediaMetadataRetriever constructor/release");}
        System.exit(0);
    }
}
