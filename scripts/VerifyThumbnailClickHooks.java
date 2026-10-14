import com.android.tools.smali.dexlib2.Opcodes;
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile;
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction;
import com.android.tools.smali.dexlib2.iface.reference.MethodReference;
import java.util.zip.ZipFile;

public class VerifyThumbnailClickHooks {
    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("Provide the patched 242 APK");
        int hooks = 0;
        boolean helper = false;
        try (ZipFile zip = new ZipFile(args[0])) {
            var entries = zip.entries();
            while (entries.hasMoreElements()) {
                var entry = entries.nextElement();
                if (!entry.getName().matches("classes[0-9]*\\.dex")) continue;
                byte[] bytes;
                try (var input = zip.getInputStream(entry)) { bytes = input.readAllBytes(); }
                for (var cls : new DexBackedDexFile(Opcodes.getDefault(), java.nio.ByteBuffer.wrap(bytes)).getClasses()) {
                    if (cls.getType().equals("Lapp/morphe/extension/chmate/ThumbnailClickAttachments;")) helper = true;
                    for (var method : cls.getMethods()) {
                        if (method.getImplementation() == null) continue;
                        boolean projection = false;
                        for (var instruction : method.getImplementation().getInstructions()) {
                            if (!(instruction instanceof ReferenceInstruction ref)
                                    || !(ref.getReference() instanceof MethodReference call)) continue;
                            if (call.getDefiningClass().equals("Lo/KeJ11;") && call.getReturnType().equals("[Ljava/lang/CharSequence;"))
                                projection = true;
                            if (call.getDefiningClass().equals("Lapp/morphe/extension/chmate/ThumbnailClickAttachments;")
                                    && call.getName().equals("links")) {
                                if (!cls.getType().equals("Lo/zzbhe;") || !method.getName().equals("onClick") || !projection)
                                    throw new AssertionError("Unexpected thumbnail hook location");
                                hooks++;
                            }
                        }
                    }
                }
            }
        }
        if (!helper || hooks != 1) throw new AssertionError("helper=" + helper + ", hooks=" + hooks);
        System.out.println("PASS: 242 thumbnail click reconciliation follows native projection exactly once");
    }
}
