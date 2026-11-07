package app.morphe.extension.chmate;
import java.nio.file.*;import java.io.*;
public class SiteBoardProtocolTest {
 static int checks;static void ok(boolean b,String s){if(!b)throw new AssertionError(s);checks++;}
 interface Op{void run()throws Exception;}static void bad(Op f)throws Exception{try{f.run();throw new AssertionError("must reject");}catch(IOException expected){checks++;}}
 public static void main(String[]a)throws Exception{
  var bk=SiteBoardProtocol.address("https://bakusai.com/thr_res/acode=1/ctgid=137/bid=5071/tid=13601530/");
  ok(bk.thread.equals("13601530"),"bakusai thread link");ok(bk.url.endsWith("bid=5071/"),"stable board identity");var bp=SiteBoardProtocol.thread(bk,"13601530",Files.readString(Path.of(a[0])));ok(bp.entries.size()==17,"OP plus all 16 replies, skip hidden template");ok(bp.entries.get(1).original.equals("1"),"original numbering preserved");ok(bp.entries.get(7).body.contains("削除"),"deleted slot retained");
  var bt=SiteBoardProtocol.topics(bk,Files.readString(Path.of(a[1])));ok(!bt.isEmpty(),"bakusai list");ok(bt.stream().anyMatch(t->t.id.equals("13601530")&&t.count>=17),"same board count");
  var fb=SiteBoardProtocol.address("https://dat.2chan.net/t/res/380645.htm");ok(fb.thread.equals("380645"),"futaba thread link");var fp=SiteBoardProtocol.thread(fb,"380645",Files.readString(Path.of(a[2])));ok(fp.entries.size()==1&&fp.entries.get(0).original.equals("380645"),"futaba OP-only thread");ok(fp.title.contains("そら豆"),"Shift-JIS decoded text");ok(fp.entries.get(0).media.size()==1,"original image, no thumb duplicate");var ft=SiteBoardProtocol.topics(fb,Files.readString(Path.of(a[3])));ok(ft.size()>10,"futaba catalogue");ok(ft.stream().anyMatch(t->t.id.equals("380645")&&t.count==1),"zero-reply OP counted once");
  var fr=SiteBoardProtocol.thread(fb,"380363",Files.readString(Path.of(a[4])));ok(fr.entries.size()>2,"futaba replies");ok(fr.entries.stream().map(e->e.original).distinct().count()==fr.entries.size(),"board-global numbers unique");
  for(String x:new String[]{"http://bakusai.com/thr_tl/acode=1/ctgid=137/bid=5071/","https://evil.test/t/","https://dat.2chan.net.evil.test/t/","https://user@dat.2chan.net/t/","https://dat.2chan.net/t/../t/","https://dat.2chan.net/t/?x=y","https://bakusai.com/img_nl/acode=1/ctgid=137/"})bad(()->SiteBoardProtocol.address(x));
  bad(()->SiteBoardProtocol.thread(fb,"380645","<html>Forbidden</html>"));bad(()->SiteBoardProtocol.thread(bk,"99999",Files.readString(Path.of(a[0]))));bad(()->SiteBoardProtocol.topics(fb,"<html>認証が必要です</html>"));
  System.out.println("PASS "+checks+" independent site-address/list/thread/media checks; bakusai="+bp.entries.size()+" futaba="+fr.entries.size()+" catalogue="+ft.size());
 }
}
