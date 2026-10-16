import com.android.tools.smali.dexlib2.Opcodes;
import com.android.tools.smali.dexlib2.Opcode;
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.MethodReference;
import java.util.*;
import java.util.zip.ZipFile;

/** Check final Android DEX wiring, including fallback branches and ArrayList verifier type. */
public class VerifyBrowsingSearchHooks {
    public static void main(String[] args) throws Exception {
        var counts = new HashMap<String, Integer>();
        try (var zip = new ZipFile(args[0])) {
            for (var entry : Collections.list(zip.entries())) {
                if (!entry.getName().matches("classes[0-9]*\\.dex")) continue;
                try (var in = zip.getInputStream(entry)) {
                    for (var cls : new DexBackedDexFile(Opcodes.getDefault(), java.nio.ByteBuffer.wrap(in.readAllBytes())).getClasses()) {
                        if (cls.getType().startsWith("Lapp/morphe/extension/")) continue;
                        for (var method : cls.getMethods()) {
                            if (method.getImplementation() == null) continue;
                            var body = new ArrayList<Instruction>(); method.getImplementation().getInstructions().forEach(body::add);
                            for (int i = 0; i < body.size(); i++) {
                                if (!(body.get(i) instanceof ReferenceInstruction r) || !(r.getReference() instanceof MethodReference m)
                                    || !m.getDefiningClass().equals("Lapp/morphe/extension/chmate/HistoryLimits;")) continue;
                                counts.merge(m.getName(), 1, Integer::sum);
                                switch (m.getName()) {
                                    case "recordSearchHistory", "unlimitedThreadHistory" -> {
                                        if (i != 0 || body.get(1).getOpcode() != Opcode.MOVE_RESULT
                                            || body.get(2).getOpcode() != Opcode.IF_EQZ || body.get(3).getOpcode() != Opcode.RETURN_VOID)
                                            throw new AssertionError("Unsafe entry guard " + method);
                                    }
                                    case "limitRecentThreadRows" -> {
                                        if (!m.getReturnType().equals("Ljava/util/ArrayList;")
                                            || body.get(i-1).getOpcode() != Opcode.MOVE_RESULT_OBJECT || body.get(i+1).getOpcode() != Opcode.MOVE_RESULT_OBJECT)
                                            throw new AssertionError("Wrong recent query wrapper " + method);
                                    }
                                    case "getRecentThreadDisplayCountLong" -> {
                                        if (!method.getName().equals("<init>") || body.get(i+1).getOpcode() != Opcode.MOVE_RESULT_WIDE
                                            || body.get(i+2).getOpcode() != Opcode.IPUT_WIDE) throw new AssertionError("Wrong recent query ctor");
                                    }
                                    default -> throw new AssertionError("Unexpected hook " + m);
                                }
                                System.out.println("PASS " + m.getName() + " in " + method);
                            }
                        }
                    }
                }
            }
        }
        String recent = args[1].equals("242") ? "getRecentThreadDisplayCountLong" : "limitRecentThreadRows";
        if (counts.size() != 3 || counts.getOrDefault(recent, 0) != 1 || counts.getOrDefault("recordSearchHistory", 0) != 1
            || counts.getOrDefault("unlimitedThreadHistory", 0) != 1) throw new AssertionError("Hook counts " + counts);
    }
}
