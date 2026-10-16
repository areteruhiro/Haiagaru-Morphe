import com.android.tools.smali.dexlib2.Opcodes;
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile;
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.zip.ZipFile;
public class InspectGestureLabels {
 public static void main(String[] args) throws Exception {
  try(var zip=new ZipFile(args[0])) {for(var e:Collections.list(zip.entries())) {
   if(!e.getName().matches("classes[0-9]*\\.dex"))continue;
   byte[] bytes;try(var in=zip.getInputStream(e)){bytes=in.readAllBytes();}
   for(var cls:new DexBackedDexFile(Opcodes.getDefault(),ByteBuffer.wrap(bytes)).getClasses()) {
    boolean layout=false;for(var m:cls.getMethods())if(m.getName().equals("setOnGestureListener"))layout=true;
    if(!layout)continue;
    System.out.println("LAYOUT "+cls.getType());
    for(var m:cls.getMethods()) {System.out.println("METHOD "+m);if(m.getImplementation()!=null)for(var i:m.getImplementation().getInstructions())if(i instanceof ReferenceInstruction r)System.out.println("  "+i.getOpcode()+" "+r.getReference());}
   }
  }}
 }
}
