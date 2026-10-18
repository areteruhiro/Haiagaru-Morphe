import com.android.tools.smali.dexlib2.Opcodes;
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile;
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction;
import com.android.tools.smali.dexlib2.iface.reference.MethodReference;
import java.util.zip.ZipFile;

/** Ensure each final APK uses native long-click dispatch, not a child listener. */
public final class VerifyPatrolToolbarPatch {
    public static void main(String[] args) throws Exception {
        for (String path : args) verify(path);
    }
    private static void verify(String path) throws Exception {
        String extension = "Lapp/morphe/extension/chmate/DeleteReadThreads;";
        int dispatch = 0, bindCalls = 0, bindMethods = 0;
        try (var zip = new ZipFile(path)) {
            var entries = zip.entries();
            while (entries.hasMoreElements()) {
                var entry = entries.nextElement();
                if (!entry.getName().matches("classes[0-9]*\\.dex")) continue;
                byte[] bytes;
                try (var in = zip.getInputStream(entry)) { bytes = in.readAllBytes(); }
                for (var cls : new DexBackedDexFile(Opcodes.getDefault(), java.nio.ByteBuffer.wrap(bytes)).getClasses()) {
                    for (var method : cls.getMethods()) {
                        var impl = method.getImplementation();
                        if (impl == null) continue;
                        boolean bind = cls.getType().equals(extension) && method.getName().equals("bind");
                        if (bind) bindMethods++;
                        for (var ins : impl.getInstructions()) {
                            if (bind && !ins.getOpcode().name().equals("RETURN_VOID"))
                                throw new AssertionError(path + ": bind must be a no-op");
                            if (!(ins instanceof ReferenceInstruction ref) || !(ref.getReference() instanceof MethodReference call)) continue;
                            if (!call.getDefiningClass().equals(extension)) continue;
                            if (call.getName().equals("bind")) bindCalls++;
                            if (call.getName().equals("longClick") && method.getName().equals("onLongClick")) dispatch++;
                        }
                    }
                }
            }
        }
        if (dispatch != 1 || bindCalls != 0 || bindMethods != 1)
            throw new AssertionError(path + ": dispatch=" + dispatch + " bindCalls=" + bindCalls + " bindMethods=" + bindMethods);
        System.out.println("PASS " + path + ": native long-click hook, no bind caller, compatibility bind is a no-op");
    }
}
