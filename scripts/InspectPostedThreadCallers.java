import com.android.tools.smali.dexlib2.Opcodes;
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import java.util.*;
import java.util.zip.ZipFile;

/** Focused read-only call-site inspection; no full APK decompilation. */
public class InspectPostedThreadCallers {
    public static void main(String[] args) throws Exception {
        try (ZipFile zip = new ZipFile(args[0])) {
            for (var entry : Collections.list(zip.entries())) {
                if (!entry.getName().matches("classes[0-9]*\\.dex")) continue;
                try (var in = zip.getInputStream(entry)) {
                    for (var cls : new DexBackedDexFile(Opcodes.getDefault(), java.nio.ByteBuffer.wrap(in.readAllBytes())).getClasses()) {
                        for (var method : cls.getMethods()) {
                            if (method.getImplementation() == null) continue;
                            var code = new ArrayList<Instruction>();
                            method.getImplementation().getInstructions().forEach(code::add);
                            for (int i=0;i<code.size();i++) {
                                if (!(code.get(i) instanceof ReferenceInstruction ref)
                                        || !ref.getReference().toString().contains(args[1])) continue;
                                System.out.println(method + " @" + i);
                                for (int j=Math.max(0,i-6);j<Math.min(code.size(),i+4);j++) {
                                    var ins=code.get(j);
                                    String detail=ins instanceof ReferenceInstruction r?r.getReference().toString()
                                        :ins instanceof WideLiteralInstruction n?Long.toString(n.getWideLiteral()):"";
                                    if(ins instanceof FiveRegisterInstruction r) detail="{"+r.getRegisterC()+","+r.getRegisterD()+","+r.getRegisterE()+","+r.getRegisterF()+","+r.getRegisterG()+"} "+detail;
                                    else if(ins instanceof TwoRegisterInstruction r) detail="v"+r.getRegisterA()+",v"+r.getRegisterB()+" "+detail;
                                    else if(ins instanceof OneRegisterInstruction r) detail="v"+r.getRegisterA()+" "+detail;
                                    System.out.println(j+" "+ins.getOpcode()+" "+detail);
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
