import com.android.tools.smali.dexlib2.Opcodes;
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile;
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction;
import com.android.tools.smali.dexlib2.iface.reference.MethodReference;
import java.util.zip.ZipFile;

/** Confirms the popup bridge is present before native dispatch in actual APKs. */
public class VerifyWacchoiNgHooks {
    public static void main(String[] args) throws Exception {
        if (args.length == 0) throw new IllegalArgumentException("Provide patched APKs");
        for (String apk : args) {
            int hooks = 0;
            boolean helperPresent = false;
            try (ZipFile zip = new ZipFile(apk)) {
                var entries = zip.entries();
                while (entries.hasMoreElements()) {
                    var entry = entries.nextElement();
                    if (!entry.getName().matches("classes[0-9]*\\.dex")) continue;
                    byte[] bytes;
                    try (var input = zip.getInputStream(entry)) { bytes = input.readAllBytes(); }
                    for (var cls : new DexBackedDexFile(Opcodes.getDefault(), java.nio.ByteBuffer.wrap(bytes)).getClasses()) {
                        for (var method : cls.getMethods()) {
                            if (cls.getType().equals("Lapp/morphe/extension/chmate/WacchoiLongPressMenu;")
                                    && method.getName().equals("dispatchSuffixNgItem")) helperPresent = true;
                            if (!method.getName().equals("onItemClick") || method.getImplementation() == null) continue;
                            int bridgeAt = -1, nativeAt = -1, position = 0;
                            for (var instruction : method.getImplementation().getInstructions()) {
                                if (!(instruction instanceof ReferenceInstruction ref)
                                        || !(ref.getReference() instanceof MethodReference target)) continue;
                                if (target.getDefiningClass().equals("Lapp/morphe/extension/chmate/WacchoiLongPressMenu;")
                                        && target.getName().equals("dispatchSuffixNgItem")) {
                                    bridgeAt = position;
                                    hooks++;
                                }
                                if (target.getName().equals("onMenuItemClick")) nativeAt = position;
                                position++;
                            }
                            if (bridgeAt >= 0 && (nativeAt < 0 || nativeAt <= bridgeAt))
                                throw new AssertionError(apk + ": native popup missing or dispatched before bridge");
                        }
                    }
                }
            }
            if (!helperPresent || hooks != 1) throw new AssertionError(apk + ": helpers=" + helperPresent + ", hooks=" + hooks);
            System.out.println("PASS: " + apk + " popup callback bridge present exactly once");
        }
    }
}
