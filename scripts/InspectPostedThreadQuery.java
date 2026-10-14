import com.android.tools.smali.dexlib2.Opcodes;
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import java.util.zip.ZipFile;

/** Read-only, focused inspection of the home recently-posted query. */
public final class InspectPostedThreadQuery {
    public static void main(String[] args) throws Exception {
        try (ZipFile zip = new ZipFile(args[0])) {
            var entries = zip.entries();
            while (entries.hasMoreElements()) {
                var entry = entries.nextElement();
                if (!entry.getName().matches("classes[0-9]*\\.dex")) continue;
                byte[] bytes;
                try (var in = zip.getInputStream(entry)) { bytes = in.readAllBytes(); }
                for (var cls : new DexBackedDexFile(Opcodes.getDefault(), java.nio.ByteBuffer.wrap(bytes)).getClasses()) {
                    if (!cls.getType().equals(args[1])) continue;
                    for (var method : cls.getMethods()) {
                        if (!method.getName().equals(args[2]) || method.getImplementation() == null) continue;
                        System.out.println(method);
                        int i = 0;
                        for (var ins : method.getImplementation().getInstructions()) {
                            String detail = ins instanceof ReferenceInstruction r ? r.getReference().toString()
                                : ins instanceof WideLiteralInstruction n ? Long.toString(n.getWideLiteral()) : "";
                            if (ins instanceof TwoRegisterInstruction r) detail = "v" + r.getRegisterA() + ",v" + r.getRegisterB() + " " + detail;
                            else if (ins instanceof OneRegisterInstruction r) detail = "v" + r.getRegisterA() + " " + detail;
                            System.out.println((i++) + " " + ins.getOpcode() + " " + detail);
                        }
                    }
                }
            }
        }
    }
}
