import com.android.tools.smali.dexlib2.Opcodes;
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile;
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction;
import com.android.tools.smali.dexlib2.iface.reference.StringReference;
import java.util.*;
import java.util.zip.ZipFile;

/** Read-only inspection of native auto-scroll icons and theme names. */
public final class InspectAutoScrollColors {
    public static void main(String[] args) throws Exception {
        for (String apk : args) try (var zip = new ZipFile(apk)) {
            System.out.println("APK " + apk);
            for (var entry : Collections.list(zip.entries())) {
                if (!entry.getName().matches("classes[0-9]*\\.dex")) continue;
                byte[] bytes; try (var input = zip.getInputStream(entry)) { bytes = input.readAllBytes(); }
                for (var cls : new DexBackedDexFile(Opcodes.getDefault(), java.nio.ByteBuffer.wrap(bytes)).getClasses()) {
                    for (var method : cls.getMethods()) {
                        if (method.getImplementation() == null) continue;
                        boolean target = false;
                        for (var ins : method.getImplementation().getInstructions()) {
                            if (ins instanceof ReferenceInstruction r && r.getReference() instanceof StringReference s
                                    && Set.of("Down", "Up", "textPrimaryColor").contains(s.getString())) target = true;
                        }
                        if (!target || !cls.getType().startsWith("Lo/")) continue;
                        if (method.getParameterTypes().equals(List.of("Ljava/io/File;"))) {
                            for (var candidate : cls.getMethods()) if (candidate.getParameterTypes().equals(List.of("Landroid/content/Context;")))
                                System.out.println("THEME_GETTER " + candidate + " flags=" + candidate.getAccessFlags());
                        }
                        System.out.println("METHOD " + method);
                        for (var ins : method.getImplementation().getInstructions()) {
                            if (ins instanceof ReferenceInstruction r) System.out.println(ins.getOpcode() + " " + r.getReference());
                        }
                    }
                }
            }
        }
    }
}
