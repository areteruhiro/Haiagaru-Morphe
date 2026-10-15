import com.android.tools.smali.dexlib2.Opcodes;
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile;
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction;
import com.android.tools.smali.dexlib2.iface.reference.MethodReference;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.zip.ZipFile;

public final class VerifyWacchoiMenuOrderHooks {
    public static void main(String[] args) throws Exception {
        String owner = switch (args[1]) {
            case "191" -> "Lo/pa;";
            case "226" -> "Lo/setDomStorageEnabled;";
            case "241" -> "Lo/OnUserEarnedRewardListener;";
            case "242" -> "Lo/isDataValid;";
            default -> throw new IllegalArgumentException("version");
        };
        int count = 0;
        try (var zip = new ZipFile(args[0])) {
            for (var entry : Collections.list(zip.entries())) {
                if (!entry.getName().matches("classes[0-9]*\\.dex")) continue;
                byte[] bytes;
                try (var in = zip.getInputStream(entry)) { bytes = in.readAllBytes(); }
                for (var cls : new DexBackedDexFile(Opcodes.getDefault(), ByteBuffer.wrap(bytes)).getClasses()) {
                    if (!cls.getType().equals(owner)) continue;
                    for (var method : cls.getMethods()) {
                        if (method.getImplementation() == null) continue;
                        for (var instruction : method.getImplementation().getInstructions()) {
                            if (instruction instanceof ReferenceInstruction ref && ref.getReference() instanceof MethodReference call
                                && call.getDefiningClass().equals("Lapp/morphe/extension/chmate/WacchoiLongPressMenu;")
                                && call.getName().equals(args[1].equals("191") ? "reorderLegacyNameMenu" : "reorderNameSheet")) count++;
                        }
                    }
                }
            }
        }
        int expected = args[1].equals("191") ? 1 : 2;
        if (count != expected) throw new AssertionError("Wrong hook count: " + count);
        System.out.println("PASS Wacchoi menu order hooks: " + args[1] + " = " + count);
    }
}
