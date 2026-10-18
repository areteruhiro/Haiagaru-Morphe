package app.morphe.extension.chmate;
public class VerifyDirectVideoUrls {
 public static void main(String[]args){
  String[] yes={"https://example.com/a.mp4","https://cdn.example.net/video.webm?token=abc&x=1","http://example.com/a.M4V","https://example.com/a.mov","https://example.com/a.3gp","https://example.com/a.3gpp","https://i.imgur.com/b1W1vSz.mp4","https://video.twimg.com/test.mp4"};
  for(String url:yes)if(!DirectVideoUrls.supported(url)||DirectVideoUrls.extract(url).length!=1)throw new AssertionError(url);
  for(String url:new String[]{"file:///sdcard/a.mp4","javascript:a.mp4","https://example.com/watch?v=1","https://example.com/video.m3u8","https://example.com/a.mp4evil","https://example.com/a.jpg","https://user:pass@example.com/a.mp4","http://127.0.0.1/a.mp4","http://localhost/a.mp4","http://[::1]/a.mp4"})if(DirectVideoUrls.supported(url))throw new AssertionError(url);
  if(DirectVideoUrls.extract(yes[0]+" "+yes[0]).length!=1)throw new AssertionError("dedup");
  if(!DirectVideoUrls.extract("("+yes[1].replace("&","&amp;")+")")[0].equals(yes[1]))throw new AssertionError("query");
  StringBuilder many=new StringBuilder();for(int i=0;i<10;i++)many.append("https://example.com/").append(i).append(".mp4 ");
  if(DirectVideoUrls.extract(many.toString()).length!=8)throw new AssertionError("limit");
  System.out.println("PASS: 21 direct-video URL policy cases");
 }
}
