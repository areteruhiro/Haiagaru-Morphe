import com.android.tools.smali.dexlib2.Opcodes;
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile;
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction;
import com.android.tools.smali.dexlib2.iface.reference.StringReference;
import java.util.*;
import java.util.zip.ZipFile;
public class InspectBeThreadSend {
 public static void main(String[] args)throws Exception {
  for(String path:args)try(var zip=new ZipFile(path)) {
   System.out.println(path);
   for(var e:Collections.list(zip.entries()))if(e.getName().matches("classes[0-9]*\\.dex")) {
    byte[] bytes;try(var in=zip.getInputStream(e)){bytes=in.readAllBytes();}
    for(var c:new DexBackedDexFile(Opcodes.getDefault(),java.nio.ByteBuffer.wrap(bytes)).getClasses())for(var m:c.getMethods()) {
     if(m.getImplementation()==null)continue;
     boolean match=false;
     for(var i:m.getImplementation().getInstructions())if(i instanceof ReferenceInstruction r && r.getReference() instanceof StringReference s && s.getString().equals("新規スレッド作成"))match=true;
     if(match){System.out.println(m);for(var i:m.getImplementation().getInstructions())if(i instanceof ReferenceInstruction r)System.out.println("  "+i.getOpcode()+" "+r.getReference());}
    }
   }
  }
 }
}
