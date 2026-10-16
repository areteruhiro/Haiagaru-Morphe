import com.android.tools.smali.dexlib2.Opcodes;
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile;
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction;
import com.android.tools.smali.dexlib2.iface.reference.MethodReference;
import java.util.zip.ZipFile;

/** Checks the injected hooks in complete patched APKs, not intermediate smali. */
public class VerifyFilterPositionHooks {
    public static void main(String[] args) throws Exception {
        for (String apk : args) {
            int captures = 0, registrations = 0;
            boolean helper = false, setting = false;
            try (var zip = new ZipFile(apk)) {
                var entries = zip.entries();
                while (entries.hasMoreElements()) {
                    var entry = entries.nextElement();
                    if (!entry.getName().matches("classes[0-9]*\\.dex")) continue;
                    byte[] bytes;
                    try (var in = zip.getInputStream(entry)) { bytes = in.readAllBytes(); }
                    for (var cls : new DexBackedDexFile(Opcodes.getDefault(), java.nio.ByteBuffer.wrap(bytes)).getClasses()) {
                        if (cls.getType().equals("Lapp/morphe/extension/chmate/FilterPosition;")) helper = true;
                        for (var method : cls.getMethods()) {
                            if (method.getName().equals("keepQuickFilterPosition")) setting = true;
                            if (method.getImplementation() == null || cls.getType().startsWith("Lapp/morphe/extension/")) continue;
                            for (var instruction : method.getImplementation().getInstructions()) {
                                if (!(instruction instanceof ReferenceInstruction ref)
                                        || !(ref.getReference() instanceof MethodReference target)
                                        || !target.getDefiningClass().equals("Lapp/morphe/extension/chmate/FilterPosition;")) continue;
                                if (target.getName().startsWith("before")) {
                                    captures++;
                                    String expected = apk.contains("191") ? "e" : apk.contains("226") ? "invoke"
                                            : apk.contains("241") ? "d" : "b";
                                    if (!method.getName().equals(expected)) throw new AssertionError("Unexpected capture: " + method);
                                }
                                if (target.getName().equals("register")) registrations++;
                            }
                        }
                    }
                }
            }
            if (!helper || !setting || captures != 1 || (!apk.contains("191") && registrations < 1))
                throw new AssertionError(apk + ": capture=" + captures + ", register=" + registrations + ", helper=" + helper + ", setting=" + setting);
            System.out.println("PASS " + apk + ": filter-specific capture=" + captures + ", registration=" + registrations);
        }
    }
}
