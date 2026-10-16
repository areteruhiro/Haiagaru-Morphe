import com.android.tools.smali.dexlib2.Opcodes;
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile;
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.zip.ZipFile;

public class VerifyExternalWebSearch {
 public static void main(String[] args) throws Exception {
  try(var zip=new ZipFile(args[0])) {
   boolean found=false;
   for(var e:Collections.list(zip.entries())) {
    if(!e.getName().matches("classes[0-9]*\\.dex"))continue;
    byte[] bytes;try(var in=zip.getInputStream(e)){bytes=in.readAllBytes();}
    for(var c:new DexBackedDexFile(Opcodes.getDefault(),ByteBuffer.wrap(bytes)).getClasses()) {
     if(!c.getType().equals("Lo/lo;"))continue;
     for(var m:c.getMethods())if(m.getName().equals("c")&&m.getParameterTypes().equals(List.of("Landroid/content/Context;","Ljava/lang/String;"))) {
      var instructions=new ArrayList<com.android.tools.smali.dexlib2.iface.instruction.Instruction>();m.getImplementation().getInstructions().forEach(instructions::add);
      if(!(instructions.get(0) instanceof ReferenceInstruction r)||!r.getReference().toString().contains("ExternalWebSearch;->open"))throw new AssertionError("Missing entry hook");
      if(instructions.get(1).getOpcode()!=com.android.tools.smali.dexlib2.Opcode.RETURN_VOID)throw new AssertionError("Native for-result path still executed");
      found=true;
     }
    }
   }
   if(!found)throw new AssertionError("Missing native launcher");System.out.println("PASS native web search entry bypasses for-result path");
  }
 }
}
