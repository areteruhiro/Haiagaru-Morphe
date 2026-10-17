import com.android.tools.smali.dexlib2.Opcodes;
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.StringReference;
import java.util.*;
import java.util.zip.ZipFile;
public class InspectReadDelete {
 public static void main(String[] args)throws Exception {
  try(var zip=new ZipFile(args[0])) { for(var entry:Collections.list(zip.entries())) {
   if(!entry.getName().matches("classes[0-9]*\\.dex"))continue;
   byte[] bytes;try(var in=zip.getInputStream(entry)){bytes=in.readAllBytes();}
   for(var cls:new DexBackedDexFile(Opcodes.getDefault(),java.nio.ByteBuffer.wrap(bytes)).getClasses())
    for(var method:cls.getMethods()) { if(method.getImplementation()==null)continue;
     var body=new ArrayList<Instruction>();method.getImplementation().getInstructions().forEach(body::add);
     boolean hit=body.stream().anyMatch(i->i instanceof ReferenceInstruction r && r.getReference() instanceof StringReference s && (s.getString().contains("confirmDeleteThread") || s.getString().equals("deleteThread") || s.getString().equals("deleteByBoardAndCreated")));
     if(!hit)continue;
     System.out.println("METHOD "+method+" SUPER "+cls.getSuperclass());
     for(var ins:body)if(ins instanceof ReferenceInstruction r)System.out.println(ins.getOpcode()+" "+r.getReference());
    }
  }}
 }
}
