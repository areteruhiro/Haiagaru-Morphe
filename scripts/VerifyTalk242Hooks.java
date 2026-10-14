import com.android.tools.smali.dexlib2.Opcodes;
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile;
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction;
import com.android.tools.smali.dexlib2.iface.reference.MethodReference;
import java.util.zip.ZipFile;

public final class VerifyTalk242Hooks {
    public static void main(String[] args) throws Exception {
        int auth = 0, post = 0, normalize = 0;
        try (ZipFile zip = new ZipFile(args[0])) {
            var entries = zip.entries();
            while (entries.hasMoreElements()) {
                var entry = entries.nextElement();
                if (!entry.getName().matches("classes[0-9]*\\.dex")) continue;
                byte[] bytes;
                try (var input = zip.getInputStream(entry)) { bytes = input.readAllBytes(); }
                for (var cls : new DexBackedDexFile(Opcodes.getDefault(), java.nio.ByteBuffer.wrap(bytes)).getClasses()) {
                    for (var method : cls.getMethods()) {
                        if (method.getImplementation() == null) continue;
                        for (var instruction : method.getImplementation().getInstructions()) {
                            if (!(instruction instanceof ReferenceInstruction ref)
                                    || !(ref.getReference() instanceof MethodReference call)
                                    || !call.getDefiningClass().equals("Lapp/morphe/extension/chmate/TalkPostCompatibility;")) continue;
                            if (call.getName().equals("invoke")) {
                                if (cls.getType().equals("Lo/PAGSdk71;")) auth++;
                                if (cls.getType().equals("Lo/getTopCountDown;")) post++;
                            }
                            if (call.getName().equals("normalizeIntegrityState242")) {
                                if (!cls.getType().equals("Lo/GoogleApiSettingsBuilder;"))
                                    throw new AssertionError("unexpected integrity hook");
                                normalize++;
                            }
                        }
                    }
                }
            }
        }
        if (auth == 0 || post == 0 || auth + post != 9 || normalize == 0)
            throw new AssertionError("Missing hooks: " + auth + "/" + post + "/" + normalize);
        System.out.println("PASS: APK Talk242 auth=" + auth + " post=" + post + " integrity=" + normalize);
    }
}
