import com.android.tools.smali.dexlib2.Opcodes;
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile;
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction;
import com.android.tools.smali.dexlib2.iface.reference.MethodReference;
import java.nio.ByteBuffer;
import java.util.zip.ZipFile;

public final class VerifyPostEditorTextSize {
    public static void main(String[] args) throws Exception {
        for (String apk : args) {
            int hooks = 0;
            boolean extension = false, getter = false;
            try (ZipFile zip = new ZipFile(apk)) {
                var entries = zip.entries();
                while (entries.hasMoreElements()) {
                    var entry = entries.nextElement();
                    if (!entry.getName().matches("classes[0-9]*\\.dex")) continue;
                    var dex = new DexBackedDexFile(Opcodes.getDefault(),
                            ByteBuffer.wrap(zip.getInputStream(entry).readAllBytes()));
                    for (var cls : dex.getClasses()) {
                        String ext = "Lapp/morphe/extension/chmate/PostEditorTextSize;";
                        if (cls.getType().equals(ext)) extension = true;
                        for (var method : cls.getMethods()) {
                            if (cls.getType().equals("Lapp/morphe/extension/chmate/Haiagaru;")
                                    && method.getName().equals("postEditorTextSizeSp")) getter = true;
                            if (method.getImplementation() == null) continue;
                            for (var instruction : method.getImplementation().getInstructions()) {
                                if (instruction instanceof ReferenceInstruction ref
                                        && ref.getReference() instanceof MethodReference target
                                        && target.getDefiningClass().equals(ext)
                                        && target.getName().equals("apply")) {
                                    if (!cls.getType().equals("Ljp/syoboi/a2chMate/activity/ResEditActivity;")
                                            || !method.getName().equals("onResume"))
                                        throw new AssertionError("Hook outside posting activity: " + cls.getType());
                                    hooks++;
                                }
                            }
                        }
                    }
                }
            }
            if (!extension || !getter || hooks != 1) throw new AssertionError(apk + ": hooks=" + hooks);
            System.out.println("PASS " + apk);
        }
    }
}
