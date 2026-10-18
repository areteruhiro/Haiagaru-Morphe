package app.morphe.extension.chmate;
public class VerifyImgurDirectVideos {
 public static void main(String[]a){
  String[] cases={"https://i.imgur.com/b1W1vSz.mp4","https://i.imgur.com/H0i6Uim.mp4\ntest","https://i.imgur.com/eZSXGUv.mp4","https://i.imgur.com/YI6WgMX.mp4","https://i.imgur.com/s7qfiSe.mp4","https://i.imgur.com/aBc.mp4?x=1","(ttps://i.imgur.com/abc.mp4)"};
  for(String s:cases)if(ImgurDirectVideos.extract(s).length!=1)throw new AssertionError(s);
  for(String s:new String[]{"https://evil.example/i.imgur.com/a.mp4","https://i.imgur.com.evil.example/a.mp4","https://i.imgur.com/a.jpg","https://imgur.com/a/test","https://i.imgur.com/a.mp4evil"})if(ImgurDirectVideos.extract(s).length!=0)throw new AssertionError(s);
  if(ImgurDirectVideos.extract(cases[0]+"\n"+cases[0]).length!=1)throw new AssertionError("dedup");
  System.out.println("PASS: 13 Imgur direct MP4 extraction checks");
 }
}
