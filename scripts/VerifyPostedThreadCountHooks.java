import com.android.tools.smali.dexlib2.Opcodes;
import com.android.tools.smali.dexlib2.Opcode;
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile;
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction;
import com.android.tools.smali.dexlib2.iface.reference.MethodReference;
import java.util.zip.ZipFile;

/** Validate actual generated DEX, without another full APK decompile. */
public final class VerifyPostedThreadCountHooks {
    public static void main(String[] args) throws Exception {
        int hooks = 0;
        String owner = args[1];
        boolean legacy = owner.equals("Lo/r8lambdaTb_p0z6z2AqSZIga1YhmAVmiTPk;")
                || owner.equals("Ljp/syoboi/a2chMate/fragment/HomeFragment$RemoteActionCompatParcelizer;");
        boolean wide = !legacy && !owner.equals("Lo/rS;");
        try (ZipFile zip = new ZipFile(args[0])) {
            var entries = zip.entries();
            while (entries.hasMoreElements()) {
                var entry = entries.nextElement();
                if (!entry.getName().matches("classes[0-9]*\\.dex")) continue;
                byte[] bytes;
                try (var in = zip.getInputStream(entry)) { bytes = in.readAllBytes(); }
                for (var cls : new DexBackedDexFile(Opcodes.getDefault(), java.nio.ByteBuffer.wrap(bytes)).getClasses()) {
                    if (cls.getType().startsWith("Lapp/morphe/extension/")) continue;
                    for (var method : cls.getMethods()) {
                        if (method.getImplementation() == null) continue;
                        var instructions = new java.util.ArrayList<com.android.tools.smali.dexlib2.iface.instruction.Instruction>();
                        method.getImplementation().getInstructions().forEach(instructions::add);
                        for (int i = 0; i < instructions.size(); i++) {
                            if (!(instructions.get(i) instanceof ReferenceInstruction ref)
                                    || !(ref.getReference() instanceof MethodReference call)
                                    || !call.getDefiningClass().equals("Lapp/morphe/extension/chmate/Haiagaru;")
                                    || !call.getName().startsWith("getPostedThreadDisplayCount")) continue;
                            if (!cls.getType().equals(owner)) throw new AssertionError("Unexpected hook " + cls.getType());
                            if (!legacy && !method.getReturnType().equals("Ljava/util/List;")) throw new AssertionError("Not display query");
                            if (!call.getReturnType().equals(wide ? "J" : "I")) throw new AssertionError("Wrong result type");
                            if (instructions.get(i + 1).getOpcode() != (wide ? Opcode.MOVE_RESULT_WIDE : Opcode.MOVE_RESULT))
                                throw new AssertionError("Wrong move-result");
                            if (instructions.get(i + 2).getOpcode() != (wide ? Opcode.INVOKE_DIRECT : Opcode.IPUT))
                                throw new AssertionError("Query limit destination changed");
                            hooks++;
                        }
                    }
                }
            }
        }
        if (hooks != 1) throw new AssertionError("Expected 1 display-limit hook, got " + hooks);
        System.out.println("PASS posted-thread display hook: " + owner);
    }
}
