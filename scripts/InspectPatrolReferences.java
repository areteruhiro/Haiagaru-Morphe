import com.android.tools.smali.dexlib2.Opcodes;
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile;
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction;
import java.util.*;
import java.util.zip.ZipFile;
public class InspectPatrolReferences {
    public static void main(String[] args) throws Exception {
        try(var zip=new ZipFile(args[0])) {
            for(var entry:Collections.list(zip.entries())) {
                if(!entry.getName().matches("classes[0-9]*\\.dex"))continue;
                try(var in=zip.getInputStream(entry)) {
                    for(var c:new DexBackedDexFile(Opcodes.getDefault(),java.nio.ByteBuffer.wrap(in.readAllBytes())).getClasses())
                        for(var m:c.getMethods()) {
                            if(m.getImplementation()==null)continue;
                            for(var i:m.getImplementation().getInstructions())
                                if(i instanceof ReferenceInstruction r && r.getReference().toString().contains(args[1]))
                                    System.out.println(m+" -> "+r.getReference());
                        }
                }
            }
        }
    }
}
