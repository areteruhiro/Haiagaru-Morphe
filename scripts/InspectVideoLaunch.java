import com.android.tools.smali.dexlib2.Opcodes;
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile;
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.zip.ZipFile;
public final class InspectVideoLaunch {
    public static void main(String[] args) throws Exception {
        try (var zip=new ZipFile(args[0])) {
            for(var entry:Collections.list(zip.entries())) {
                if(!entry.getName().matches("classes[0-9]*\\.dex")) continue;
                byte[] bytes; try(var in=zip.getInputStream(entry)){bytes=in.readAllBytes();}
                for(var cls:new DexBackedDexFile(Opcodes.getDefault(),ByteBuffer.wrap(bytes)).getClasses()) {
                    if(!cls.getType().equals("Lo/lo;"))continue;
                    for(var m:cls.getMethods()) {
                        if(!m.getParameterTypes().equals(List.of("Landroid/app/Activity;","I","Ljava/lang/String;","Z")))continue;
                        System.out.println(m);
                        for(var ins:m.getImplementation().getInstructions()) if(ins instanceof ReferenceInstruction r)System.out.println(ins.getOpcode()+" "+r.getReference());
                    }
                }
            }
        }
    }
}
