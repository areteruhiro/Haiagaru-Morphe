import com.android.tools.smali.dexlib2.Opcodes;
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile;
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction;
import java.util.*;
import java.util.zip.ZipFile;

public final class InspectPostingMenu {
    public static void main(String[] args) throws Exception {
        for (String apk : args) try (var zip = new ZipFile(apk)) {
            System.out.println("APK " + apk);
            for (var entry : Collections.list(zip.entries())) {
                if (!entry.getName().matches("classes[0-9]*\\.dex")) continue;
                byte[] bytes; try (var input = zip.getInputStream(entry)) { bytes = input.readAllBytes(); }
                for (var cls : new DexBackedDexFile(Opcodes.getDefault(), java.nio.ByteBuffer.wrap(bytes)).getClasses()) {
                    if (!cls.getType().startsWith("Ljp/syoboi/a2chMate/feature/resedit/ResEditFragment;")
                            && !cls.getType().equals("Lo/p9ExternalSyntheticLambda6;")) continue;
                    System.out.println("CLASS " + cls.getType() + " super=" + cls.getSuperclass());
                    for (var method : cls.getMethods()) {
                        System.out.println("METHOD " + method);
                        if (method.getImplementation() == null) continue;
                        for (var ins : method.getImplementation().getInstructions()) {
                            if (ins instanceof ReferenceInstruction r) System.out.println(ins.getOpcode() + " " + r.getReference());
                        }
                    }
                }
            }
        }
    }
}
