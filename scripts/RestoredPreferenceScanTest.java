package app.morphe.extension.chmate;
import java.nio.file.*;import java.nio.charset.*;
public final class RestoredPreferenceScanTest {
 public static void main(String[] args)throws Exception{Path file=Files.createTempFile("restored-preferences", ".xml");int checks=0;String pkg="jp.co.airfront.android.a2chMate";
  try{for(String value:new String[]{"", "<map><string name='a'>ordinary settings</string></map>","日本語の設定"}){Files.writeString(file,value);if(RestoredPreferenceScan.needsParsing(file.toFile(),pkg))throw new AssertionError(value);checks++;}
   for(int offset:new int[]{0,1,8190,8191,8192,8193,16383}){Files.writeString(file,"x".repeat(offset)+pkg+"/files");if(!RestoredPreferenceScan.needsParsing(file.toFile(),pkg))throw new AssertionError("boundary "+offset);checks++;}
   for(String value:new String[]{"<string>&#106;p.co.airfront.android.a2chMate</string>","&amp;",pkg+".renamed", "prefix "+pkg+" suffix"}){Files.writeString(file,value);if(!RestoredPreferenceScan.needsParsing(file.toFile(),pkg))throw new AssertionError(value);checks++;}
   Files.write(file,"<map>wide encoding</map>".getBytes(StandardCharsets.UTF_16));if(!RestoredPreferenceScan.needsParsing(file.toFile(),pkg))throw new AssertionError("UTF16 fallback");checks++;
   Files.delete(file);if(!RestoredPreferenceScan.needsParsing(file.toFile(),pkg))throw new AssertionError("IO fallback");checks++;
   System.out.println("PASS "+checks+" preference pre-scan and parser-fallback checks");
  }finally{Files.deleteIfExists(file);}
 }
}
